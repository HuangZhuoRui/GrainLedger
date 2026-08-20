package com.vincent.grainledger.ui.screens.updater

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.data.updater.DownloadProgress
import com.vincent.grainledger.data.updater.DownloadStatus
import com.vincent.grainledger.data.updater.GitHubRelease
import com.vincent.grainledger.data.updater.UpdateCheckState
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixPurple
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 独立的检查更新与版本发布历史页面 (UpdateScreen)。
 *
 * 提供应用版本检查、多通道下载（正常下载与加速下载）、
 * 实时下载进度展示与历史发布记录浏览。
 *
 * @param viewModel 全局视图模型
 * @param onBack 点击返回回调
 */
@Composable
fun UpdateScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentAppVersion = BuildConfig.VERSION_NAME
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadingTagName by viewModel.downloadingTagName.collectAsState()
    val releaseHistoryList by viewModel.releaseHistoryList.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchReleaseHistory()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部返回与页面标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "检查更新",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "版本状态与更新历史记录",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }

            // 2. 当前版本与检查卡片
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 22.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MiuixBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "软件更新",
                                tint = MiuixBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "余粮 GrainLedger",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MiuixBlue.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "当前版本 v$currentAppVersion",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixBlue
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MiuixGreen.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "正式发行版",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 检查更新按钮
                        Button(
                            onClick = {
                                showDialog = true
                                viewModel.checkForUpdates(currentAppVersion)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "检查更新",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "检查新版本",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. 历史版本发布记录分区标题
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "版本发布历史",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (isLoadingHistory) {
                        CircularProgressIndicator(
                            color = MiuixBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "共 ${releaseHistoryList.size} 个版本",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }

            // 4. 历史版本列表
            if (releaseHistoryList.isEmpty() && !isLoadingHistory) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        cornerRadius = 20.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无更多历史版本发布信息",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }
            } else {
                items(releaseHistoryList) { release ->
                    val isDownloading = (downloadingTagName == release.tagName && downloadProgress.status == DownloadStatus.DOWNLOADING)

                    ReleaseHistoryCard(
                        release = release,
                        currentVersion = currentAppVersion,
                        isDownloading = isDownloading,
                        downloadProgress = downloadProgress,
                        onStartNormalDownload = { directUrl, fileName ->
                            viewModel.startDownloadApk(
                                context = context,
                                downloadUrl = directUrl,
                                fileName = fileName,
                                tagName = release.tagName
                            )
                        },
                        onStartAcceleratedDownload = { acceleratedUrl, fileName ->
                            viewModel.startDownloadApk(
                                context = context,
                                downloadUrl = acceleratedUrl,
                                fileName = fileName,
                                tagName = release.tagName
                            )
                        },
                        onCancelDownload = {
                            viewModel.cancelDownload()
                        }
                    )
                }
            }
        }
    }

    // 检查更新弹窗
    if (showDialog && updateCheckState !is UpdateCheckState.Idle) {
        UpdateCheckDialog(
            checkState = updateCheckState,
            downloadProgress = downloadProgress,
            onStartDownload = { downloadUrl, fileName ->
                viewModel.startDownloadApk(context, downloadUrl, fileName)
            },
            onCancelDownload = {
                viewModel.cancelDownload()
            },
            onRetry = {
                viewModel.checkForUpdates(currentAppVersion)
            },
            onDismiss = {
                showDialog = false
                viewModel.resetUpdateState()
            }
        )
    }
}

/**
 * 历史版本展示卡片。
 */
@Composable
private fun ReleaseHistoryCard(
    release: GitHubRelease,
    currentVersion: String = BuildConfig.VERSION_NAME,
    isDownloading: Boolean,
    downloadProgress: DownloadProgress,
    onStartNormalDownload: (directUrl: String, fileName: String) -> Unit,
    onStartAcceleratedDownload: (acceleratedUrl: String, fileName: String) -> Unit,
    onCancelDownload: () -> Unit
) {
    val changelog = release.parsedChangelog
    val apkAsset = release.androidAsset
    val isCurrentInstalled = release.tagName.trim().removePrefix("v") == currentVersion.trim().removePrefix("v")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MiuixGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "当前运行",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixGreen
                            )
                        }
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
                    ChangelogCategorySection(
                        title = "新增特性",
                        accentColor = MiuixBlue,
                        items = changelog.features
                    )
                }
                if (changelog.fixes.isNotEmpty()) {
                    ChangelogCategorySection(
                        title = "问题修复",
                        accentColor = MiuixGreen,
                        items = changelog.fixes
                    )
                }
                if (changelog.others.isNotEmpty()) {
                    ChangelogCategorySection(
                        title = "优化改进",
                        accentColor = MiuixPurple,
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

            // 3. 安装包体积展示
            if (apkAsset != null) {
                Text(
                    text = "安装包体积: ${apkAsset.formattedSize}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                // 4. 下载进度或下载操作按钮
                if (isDownloading) {
                    // 下载中状态面板
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "正在下载: ${(downloadProgress.progress * 100).toInt()}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixBlue
                            )
                            Text(
                                text = downloadProgress.formattedSpeed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixGreen
                            )
                        }

                        LinearProgressIndicator(
                            progress = { downloadProgress.progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MiuixBlue,
                            trackColor = MiuixTheme.colorScheme.surface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = downloadProgress.formattedDownloadedTotal,
                                fontSize = 11.5.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )

                            Button(
                                onClick = onCancelDownload,
                                modifier = Modifier.height(28.dp),
                                insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(color = MiuixRed.copy(alpha = 0.85f))
                            ) {
                                Text(
                                    text = "取消",
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // 下载选项按钮行（优化内边距，确保文字永不截断）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 加速下载按钮
                        Button(
                            onClick = {
                                val directUrl = release.androidDownloadUrl ?: ""
                                val fileName = apkAsset.name
                                val acceleratedUrl = if (directUrl.startsWith("https://github.com/")) {
                                    "https://update.vincenthzr.org:8443/download/${directUrl.removePrefix("https://github.com/")}"
                                } else {
                                    directUrl
                                }
                                onStartAcceleratedDownload(acceleratedUrl, fileName)
                            },
                            modifier = Modifier.weight(1f),
                            insideMargin = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                        ) {
                            Text(
                                text = "加速下载",
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        // 正常下载按钮
                        Button(
                            onClick = {
                                val directUrl = release.androidDownloadUrl ?: ""
                                val fileName = apkAsset.name
                                onStartNormalDownload(directUrl, fileName)
                            },
                            modifier = Modifier.weight(1f),
                            insideMargin = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "正常下载",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 结构化日志分类展示块（纯文字展示，无图标）。
 */
@Composable
private fun ChangelogCategorySection(
    title: String,
    accentColor: Color,
    items: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "【$title】",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        items.forEach { line ->
            Text(
                text = "• $line",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
