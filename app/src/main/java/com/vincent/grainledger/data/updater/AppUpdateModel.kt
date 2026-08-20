package com.vincent.grainledger.data.updater

import java.util.Locale
import java.util.regex.Pattern

/**
 * 结构化更新日志解析模型。
 *
 * 智能识别 Conventional Commits 规范，自动归类展示：
 * - features: 新增特性 (feat:, feature:)
 * - fixes: 问题修复 (fix:, bugfix:, hotfix:)
 * - improvements: 优化重构与体验提升 (perf:, refactor:, style:)
 * - others: 文档与维护项 (docs:, chore:, test:, revert:)
 *
 * @property features 新增特性列表
 * @property fixes 修复问题列表
 * @property improvements 优化与重构列表
 * @property others 其他变更内容
 */
data class ParsedChangelog(
    val features: List<String> = emptyList(),
    val fixes: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val others: List<String> = emptyList()
) {
    val hasCategorized: Boolean
        get() = features.isNotEmpty() || fixes.isNotEmpty() || improvements.isNotEmpty() || others.isNotEmpty()

    companion object {
        // 正则表达式匹配: ^(?<type>feat|fix|perf|...)(?:[(](?<scope>[^)]+)[)])?[:：\s]\s*(?<desc>.+)$
        private val COMMIT_PATTERN = Pattern.compile(
            """^(?:[-*]\s*)?(feat|feature|fix|bugfix|hotfix|perf|refactor|style|docs|chore|test|revert)(?:\(([^)]+)\))?[:：\s]\s*(.+)$""",
            Pattern.CASE_INSENSITIVE
        )

        fun parse(rawBody: String): ParsedChangelog {
            val features = mutableListOf<String>()
            val fixes = mutableListOf<String>()
            val improvements = mutableListOf<String>()
            val others = mutableListOf<String>()

            val lines = rawBody.split('\n')
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) continue

                // 剔除 markdown 列表前导符
                var cleanLine = line
                if (cleanLine.startsWith("- ") || cleanLine.startsWith("* ")) {
                    cleanLine = cleanLine.substring(2).trim()
                }
                if (cleanLine.isEmpty()) continue

                val lower = cleanLine.lowercase(Locale.getDefault())
                if (lower.startsWith("tmp:") || lower.startsWith("tmp：") || lower.startsWith("tmp ") ||
                    lower.startsWith("temp:") || lower.startsWith("temp：") || lower.startsWith("temp ")) {
                    continue
                }

                val matcher = COMMIT_PATTERN.matcher(cleanLine)
                if (matcher.matches()) {
                    val type = matcher.group(1)?.lowercase(Locale.getDefault()) ?: ""
                    val scope = matcher.group(2)?.trim()
                    val description = matcher.group(3)?.trim() ?: ""

                    // 格式化输出: 如果有 scope 模块名，则自动加上 [模块] 前缀
                    val formattedItem = if (!scope.isNullOrBlank()) {
                        val scopeUpper = scope.uppercase(Locale.getDefault())
                        "[$scopeUpper] $description"
                    } else {
                        description
                    }

                    when (type) {
                        "feat", "feature" -> features.add(formattedItem)
                        "fix", "bugfix", "hotfix" -> fixes.add(formattedItem)
                        "perf", "refactor", "style" -> improvements.add(formattedItem)
                        "docs", "chore", "test", "revert" -> others.add(formattedItem)
                        else -> others.add(formattedItem)
                    }
                } else {
                    // 普通 markdown 列表行，剔除前导标识符
                    var cleanLine = line
                    if (cleanLine.startsWith("- ") || cleanLine.startsWith("* ")) {
                        cleanLine = cleanLine.substring(2).trim()
                    }
                    if (cleanLine.isNotEmpty()) {
                        others.add(cleanLine)
                    }
                }
            }

            return ParsedChangelog(
                features = features,
                fixes = fixes,
                improvements = improvements,
                others = others
            )
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

    /**
     * 格式化下载进度与体积（如 "12.5 MB / 28.0 MB (45%)"）。
     */
    val formattedSizeProgress: String
        get() {
            val currentMb = receivedBytes.toDouble() / (1024.0 * 1024.0)
            val totalMb = totalBytes.toDouble() / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                String.format(Locale.getDefault(), "%.1f MB / %.1f MB (%.0f%%)", currentMb, totalMb, progress * 100)
            } else {
                String.format(Locale.getDefault(), "%.1f MB", currentMb)
            }
        }

    /**
     * 格式化已下载体积与总大小（如 "12.5 MB / 28.0 MB"）。
     */
    val formattedDownloadedTotal: String
        get() {
            val currentMb = receivedBytes.toDouble() / (1024.0 * 1024.0)
            val totalMb = totalBytes.toDouble() / (1024.0 * 1024.0)
            return if (totalBytes > 0) {
                String.format(Locale.getDefault(), "%.1f MB / %.1f MB", currentMb, totalMb)
            } else {
                String.format(Locale.getDefault(), "%.1f MB", currentMb)
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
