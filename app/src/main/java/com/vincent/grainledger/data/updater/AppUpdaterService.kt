package com.vincent.grainledger.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 软件检查更新与高速分发下载服务 (AppUpdaterService)。
 *
 * 集成 GitHub Releases 接口与自建高速代理镜像分发，
 * 支持自适应双通道版本检测、断点感知下载、实时速率测算与应用内无缝安装。
 */
class AppUpdaterService(
    private val repoOwner: String = "HuangZhuoRui",
    private val repoName: String = "GrainLedger"
) {

    companion object {
        private const val ACCELERATE_BASE_URL = "https://update.vincenthzr.org:8443"
        private const val CONNECT_TIMEOUT_MS = 8000
        private const val READ_TIMEOUT_MS = 15000
    }

    private val isDownloadCanceled = AtomicBoolean(false)

    /**
     * 取消当前进行中的下载任务。
     */
    fun cancelDownload() {
        isDownloadCanceled.set(true)
    }

    /**
     * 检查是否有新版本可用。
     *
     * @param currentVersion 当前应用版本（如 "1.0.0"）
     * @return 检查更新结果状态
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateCheckState = withContext(Dispatchers.IO) {
        val releases = fetchReleases()
        if (releases.isEmpty()) {
            return@withContext UpdateCheckState.Error("未能获取到版本发布信息，请稍后重试")
        }

        val latestRelease = releases.first()
        val hasNewVersion = isNewerVersion(latestRelease.tagName, currentVersion)

        if (hasNewVersion) {
            val directDownloadUrl = latestRelease.androidDownloadUrl ?: ""
            val acceleratedUrl = getAcceleratedDownloadUrl(directDownloadUrl)
            UpdateCheckState.HasUpdate(
                release = latestRelease,
                currentVersion = currentVersion,
                acceleratedDownloadUrl = acceleratedUrl
            )
        } else {
            UpdateCheckState.AlreadyLatest(currentVersion)
        }
    }

    /**
     * 拉取远端 Releases 列表（优先自建加速节点，失败则降级官方 GitHub API）。
     */
    private fun fetchReleases(): List<GitHubRelease> {
        // 1. 优先尝试自建高速节点
        try {
            val proxyUrlString = "$ACCELERATE_BASE_URL/api/$repoName/releases"
            val responseContent = executeHttpGet(proxyUrlString)
            if (!responseContent.isNullOrBlank()) {
                val parsedList = parseReleasesJson(responseContent)
                if (parsedList.isNotEmpty()) {
                    return parsedList
                }
            }
        } catch (_: Exception) {
            // 优雅降级
        }

        // 2. 降级直连 GitHub 官方接口
        try {
            val officialUrlString = "https://api.github.com/repos/$repoOwner/$repoName/releases"
            val responseContent = executeHttpGet(officialUrlString)
            if (!responseContent.isNullOrBlank()) {
                return parseReleasesJson(responseContent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyList()
    }

    /**
     * 执行原生 HTTP GET 请求。
     */
    private fun executeHttpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val targetUrl = URL(urlString)
            connection = (targetUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "GrainLedger-App/1.0")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * 解析 GitHub Releases JSON 数据。
     */
    private fun parseReleasesJson(jsonString: String): List<GitHubRelease> {
        val releaseList = mutableListOf<GitHubRelease>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val releaseObj = jsonArray.getJSONObject(i)
                val id = releaseObj.optLong("id", 0L)
                val tagName = releaseObj.optString("tag_name", "")
                val name = releaseObj.optString("name", "")
                val body = releaseObj.optString("body", "")
                val publishedAt = releaseObj.optString("published_at", "")

                val assetsArray = releaseObj.optJSONArray("assets") ?: JSONArray()
                val assetList = mutableListOf<GitHubAsset>()
                for (j in 0 until assetsArray.length()) {
                    val assetObj = assetsArray.getJSONObject(j)
                    val assetId = assetObj.optLong("id", 0L)
                    val assetName = assetObj.optString("name", "")
                    val assetSize = assetObj.optLong("size", 0L)
                    val downloadUrl = assetObj.optString("browser_download_url", "")
                    assetList.add(
                        GitHubAsset(
                            id = assetId,
                            name = assetName,
                            size = assetSize,
                            browserDownloadUrl = downloadUrl
                        )
                    )
                }

                releaseList.add(
                    GitHubRelease(
                        id = id,
                        tagName = tagName,
                        name = name,
                        body = body,
                        publishedAt = publishedAt,
                        assets = assetList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return releaseList
    }

    /**
     * 构建自建服务器加速下载链接。
     *
     * @param directUrl 原始 GitHub Releases 下载地址
     * @return 加速下载链接
     */
    fun getAcceleratedDownloadUrl(directUrl: String): String {
        if (directUrl.isBlank()) return directUrl
        val githubPrefix = "https://github.com/"
        return if (directUrl.startsWith(githubPrefix)) {
            val relativePath = directUrl.removePrefix(githubPrefix)
            "$ACCELERATE_BASE_URL/download/$relativePath"
        } else {
            directUrl
        }
    }

    /**
     * 比较远端版本与当前版本，判断是否存在更新。
     *
     * @param latestTagName 远端版本号（如 "v1.0.1"）
     * @param currentVersion 本地安装版本号（如 "1.0.0"）
     * @return 是否存在新版本
     */
    fun isNewerVersion(latestTagName: String, currentVersion: String): Boolean {
        return try {
            val cleanLatest = cleanVersionString(latestTagName)
            val cleanCurrent = cleanVersionString(currentVersion)

            val latestParts = cleanLatest.split('.').map { it.toIntOrNull() ?: 0 }
            val currentParts = cleanCurrent.split('.').map { it.toIntOrNull() ?: 0 }

            val maxSegments = maxOf(latestParts.size, currentParts.size)
            for (index in 0 until maxSegments) {
                val latestNumber = latestParts.getOrElse(index) { 0 }
                val currentNumber = currentParts.getOrElse(index) { 0 }
                if (latestNumber > currentNumber) return true
                if (latestNumber < currentNumber) return false
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun cleanVersionString(version: String): String {
        var cleaned = version.trim().lowercase(Locale.getDefault())
        if (cleaned.startsWith("v")) {
            cleaned = cleaned.substring(1)
        }
        // 将 '+' 替换为 '.' 便于比较细分构建号
        cleaned = cleaned.replace('+', '.')
        val hyphenIndex = cleaned.indexOf('-')
        if (hyphenIndex != -1) {
            cleaned = cleaned.substring(0, hyphenIndex)
        }
        return cleaned
    }

    /**
     * 下载 APK 文件到本地存储，并实时回调下载进度与速率。
     *
     * @param downloadUrl 下载地址
     * @param destinationFile 目标本地文件
     * @param onProgress 进度回调函数
     * @return 下载是否完全成功
     */
    suspend fun downloadApkFile(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (DownloadProgress) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        isDownloadCanceled.set(false)
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            destinationFile.parentFile?.mkdirs()

            var currentTargetUrl = URL(downloadUrl)
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                connection = (currentTargetUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("User-Agent", "GrainLedger-App/1.0")
                    instanceFollowRedirects = true
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == 307 || responseCode == 308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentTargetUrl = URL(location)
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val totalBytes = connection?.contentLengthLong ?: -1L
            inputStream = connection?.inputStream ?: return@withContext false
            outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalReceivedBytes = 0L

            var lastReportTime = System.currentTimeMillis()
            var bytesSinceLastReport = 0L

            onProgress(
                DownloadProgress(
                    receivedBytes = 0L,
                    totalBytes = totalBytes,
                    progress = 0f,
                    speedBytesPerSecond = 0.0,
                    status = DownloadStatus.DOWNLOADING
                )
            )

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isDownloadCanceled.get()) {
                    onProgress(
                        DownloadProgress(
                            receivedBytes = totalReceivedBytes,
                            totalBytes = totalBytes,
                            progress = 0f,
                            status = DownloadStatus.CANCELED
                        )
                    )
                    destinationFile.delete()
                    return@withContext false
                }

                outputStream.write(buffer, 0, bytesRead)
                totalReceivedBytes += bytesRead
                bytesSinceLastReport += bytesRead

                val currentTime = System.currentTimeMillis()
                val durationMs = currentTime - lastReportTime

                if (durationMs >= 300) {
                    val currentSpeed = (bytesSinceLastReport.toDouble() / durationMs.toDouble()) * 1000.0
                    val calculatedProgress = if (totalBytes > 0) {
                        (totalReceivedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    onProgress(
                        DownloadProgress(
                            receivedBytes = totalReceivedBytes,
                            totalBytes = totalBytes,
                            progress = calculatedProgress,
                            speedBytesPerSecond = currentSpeed,
                            status = DownloadStatus.DOWNLOADING
                        )
                    )

                    lastReportTime = currentTime
                    bytesSinceLastReport = 0L
                }
            }

            outputStream.flush()

            onProgress(
                DownloadProgress(
                    receivedBytes = totalReceivedBytes,
                    totalBytes = totalReceivedBytes,
                    progress = 1.0f,
                    speedBytesPerSecond = 0.0,
                    status = DownloadStatus.COMPLETED
                )
            )
            true
        } catch (e: Exception) {
            onProgress(
                DownloadProgress(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "下载异常中断"
                )
            )
            destinationFile.delete()
            false
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * 调起系统安装器安装已下载的 APK 文件。
     *
     * @param context 上下文
     * @param apkFile 已下载完成的 APK 文件
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
