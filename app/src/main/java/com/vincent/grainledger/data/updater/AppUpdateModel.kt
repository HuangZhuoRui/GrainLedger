package com.vincent.grainledger.data.updater

import java.util.Locale

/**
 * 结构化更新日志解析模型。
 *
 * @property features 新增特性列表 (feat:)
 * @property fixes 修复问题列表 (fix:)
 * @property others 其他变更内容
 */
data class ParsedChangelog(
    val features: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
    val others: List<String> = emptyList()
) {
    val hasCategorized: Boolean
        get() = features.isNotEmpty() || fixes.isNotEmpty()

    companion object {
        fun parse(rawBody: String): ParsedChangelog {
            val features = mutableListOf<String>()
            val fixes = mutableListOf<String>()
            val others = mutableListOf<String>()

            val lines = rawBody.split('\n')
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                var content = line
                if (content.startsWith("- ") || content.startsWith("* ")) {
                    content = content.substring(2).trim()
                }

                val lower = content.lowercase(Locale.getDefault())
                if (lower.startsWith("tmp:") || lower.startsWith("tmp：") || lower.startsWith("tmp ") ||
                    lower.startsWith("temp:") || lower.startsWith("temp：") || lower.startsWith("temp ")) {
                    continue
                }

                when {
                    lower.startsWith("feat:") || lower.startsWith("feat：") -> {
                        features.add(content.substring(5).trim())
                    }
                    lower.startsWith("feat ") -> {
                        features.add(content.substring(5).trim())
                    }
                    lower.startsWith("feature:") || lower.startsWith("feature ") -> {
                        features.add(content.substring(lower.indexOf("feature") + 7).trim())
                    }
                    lower.startsWith("fix:") || lower.startsWith("fix：") -> {
                        fixes.add(content.substring(4).trim())
                    }
                    lower.startsWith("fix ") -> {
                        fixes.add(content.substring(4).trim())
                    }
                    lower.startsWith("bugfix:") || lower.startsWith("bugfix ") -> {
                        fixes.add(content.substring(lower.indexOf("bugfix") + 6).trim())
                    }
                    content.isNotEmpty() -> {
                        others.add(content)
                    }
                }
            }

            return ParsedChangelog(features, fixes, others)
        }
    }
}

/**
 * GitHub Release 附件实体。
 *
 * @property id 附件唯一标识
 * @property name 附件文件全名
 * @property size 文件字节大小
 * @property browserDownloadUrl 原始直接下载链接
 */
data class GitHubAsset(
    val id: Long,
    val name: String,
    val size: Long,
    val browserDownloadUrl: String
) {
    /**
     * 将文件大小格式化为 MB 单位文本。
     */
    val formattedSize: String
        get() {
            if (size <= 0L) return ""
            val sizeInMegaBytes = size.toDouble() / (1024.0 * 1024.0)
            return String.format(Locale.getDefault(), "%.1f MB", sizeInMegaBytes)
        }
}

/**
 * GitHub Release 发布版本实体。
 *
 * @property id 发布记录标识
 * @property tagName 标签名（例如 "v1.0.1"）
 * @property name 版本标题
 * @property body 更新说明与发布日志
 * @property publishedAt 发布时间文本
 * @property assets 包含的附件列表
 */
data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val assets: List<GitHubAsset>
) {
    /**
     * 获取 Android APK 附件，优先检索以 .apk 结尾的附件。
     */
    val androidAsset: GitHubAsset?
        get() = assets.firstOrNull { it.name.lowercase(Locale.getDefault()).endsWith(".apk") }

    /**
     * 获取 APK 下载链接。
     */
    val androidDownloadUrl: String?
        get() = androidAsset?.browserDownloadUrl

    /**
     * 获取结构化解析的更新日志。
     */
    val parsedChangelog: ParsedChangelog
        get() = ParsedChangelog.parse(body)
}

/**
 * 下载状态枚举。
 */
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELED
}

/**
 * 下载进度实时数据模型。
 *
 * @property receivedBytes 当前已下载字节
 * @property totalBytes 总字节数
 * @property progress 进度百分比 (0.0f ~ 1.0f)
 * @property speedBytesPerSecond 实时下载速率（字节/秒）
 * @property status 当前下载状态
 * @property errorMessage 异常错误信息
 */
data class DownloadProgress(
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val speedBytesPerSecond: Double = 0.0,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String = ""
) {
    /**
     * 格式化实时下载速率（如 "2.5 MB/s" 或 "512 KB/s"）。
     */
    val formattedSpeed: String
        get() {
            return when {
                speedBytesPerSecond >= 1024 * 1024 -> {
                    String.format(Locale.getDefault(), "%.1f MB/s", speedBytesPerSecond / (1024 * 1024))
                }
                speedBytesPerSecond >= 1024 -> {
                    String.format(Locale.getDefault(), "%.1f KB/s", speedBytesPerSecond / 1024)
                }
                else -> {
                    String.format(Locale.getDefault(), "%.0f B/s", speedBytesPerSecond)
                }
            }
        }
}

/**
 * 检查更新结果密封状态。
 */
sealed class UpdateCheckState {
    /**
     * 初始未触发状态。
     */
    object Idle : UpdateCheckState()

    /**
     * 正在拉取远端更新数据。
     */
    object Checking : UpdateCheckState()

    /**
     * 检测到可用新版本。
     *
     * @property release 最新发布版本详情
     * @property currentVersion 当前安装的应用版本
     * @property acceleratedDownloadUrl 高速代理镜像加速下载链接
     */
    data class HasUpdate(
        val release: GitHubRelease,
        val currentVersion: String,
        val acceleratedDownloadUrl: String
    ) : UpdateCheckState()

    /**
     * 当前已是最新版本。
     *
     * @property currentVersion 当前版本
     */
    data class AlreadyLatest(
        val currentVersion: String
    ) : UpdateCheckState()

    /**
     * 检查更新失败。
     *
     * @property message 错误说明
     */
    data class Error(
        val message: String
    ) : UpdateCheckState()
}
