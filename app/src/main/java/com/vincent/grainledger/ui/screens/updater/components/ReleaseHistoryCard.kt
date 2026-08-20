package com.vincent.grainledger.ui.screens.updater.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.data.updater.DownloadProgress
import com.vincent.grainledger.data.updater.GitHubRelease
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.control.ActionButtonRow
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.components.feedback.DownloadProgressPanel
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixPurple
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 历史版本展示卡片 (ReleaseHistoryCard)。
 *
 * 聚合展示单个 GitHub Release 的版本号、发布日期、分类纯文字更新日志、安装包体积与加速/正常下载双操作。
 *
 * @param release GitHub 发布版本模型
 * @param currentVersion 本地安装版本号
 * @param isDownloading 是否正在下载此版本
 * @param downloadProgress 下载进度流
 * @param onStartNormalDownload 启动正常直连下载回调
 * @param onStartAcceleratedDownload 启动加速镜像下载回调
 * @param onCancelDownload 取消下载回调
 * @param modifier 外部修饰符
 */
@Composable
fun ReleaseHistoryCard(
    release: GitHubRelease,
    currentVersion: String = BuildConfig.VERSION_NAME,
    isDownloading: Boolean,
    downloadProgress: DownloadProgress,
    onStartNormalDownload: (directUrl: String, fileName: String) -> Unit,
    onStartAcceleratedDownload: (acceleratedUrl: String, fileName: String) -> Unit,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val changelog = release.parsedChangelog
    val apkAsset = release.androidAsset
    val isCurrentInstalled = release.tagName.trim().removePrefix("v") == currentVersion.trim().removePrefix("v")

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 头部版本与日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = release.tagName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (isCurrentInstalled) {
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(
                            text = "当前运行",
                            color = MiuixGreen,
                            fontSize = 11.sp
                        )
                    }
                }

                if (release.publishedAt.isNotBlank()) {
                    val displayDate = if (release.publishedAt.length >= 10) release.publishedAt.substring(0, 10) else release.publishedAt
                    Text(
                        text = displayDate,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 2. 更新日志分类（纯文字展示，无图标）
            if (changelog.hasCategorized) {
                if (changelog.features.isNotEmpty()) {
                    ChangelogSection(
                        title = "新增特性",
                        accentColor = MiuixBlue,
                        items = changelog.features
                    )
                }
                if (changelog.fixes.isNotEmpty()) {
                    ChangelogSection(
                        title = "问题修复",
                        accentColor = MiuixGreen,
                        items = changelog.fixes
                    )
                }
                if (changelog.improvements.isNotEmpty()) {
                    ChangelogSection(
                        title = "优化改进",
                        accentColor = MiuixPurple,
                        items = changelog.improvements
                    )
                }
                if (changelog.others.isNotEmpty()) {
                    ChangelogSection(
                        title = "其他变更",
                        accentColor = MiuixTheme.colorScheme.onSurfaceSecondary,
                        items = changelog.others
                    )
                }
            } else if (release.body.isNotBlank()) {
                Text(
                    text = release.body,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

            // 3. 安装包体积与下载控制
            if (apkAsset != null) {
                Text(
                    text = "安装包体积: ${apkAsset.formattedSize}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                // 4. 下载进度或下载操作按钮
                if (isDownloading) {
                    DownloadProgressPanel(
                        downloadProgress = downloadProgress,
                        onCancelDownload = onCancelDownload
                    )
                } else {
                    ActionButtonRow(
                        primaryText = "加速下载",
                        onPrimaryClick = {
                            val directUrl = release.androidDownloadUrl ?: ""
                            val fileName = apkAsset.name
                            val acceleratedUrl = if (directUrl.startsWith("https://github.com/")) {
                                "https://update.vincenthzr.org:8443/download/${directUrl.removePrefix("https://github.com/")}"
                            } else {
                                directUrl
                            }
                            onStartAcceleratedDownload(acceleratedUrl, fileName)
                        },
                        secondaryText = "正常下载",
                        onSecondaryClick = {
                            val directUrl = release.androidDownloadUrl ?: ""
                            val fileName = apkAsset.name
                            onStartNormalDownload(directUrl, fileName)
                        }
                    )
                }
            }
        }
    }
}
