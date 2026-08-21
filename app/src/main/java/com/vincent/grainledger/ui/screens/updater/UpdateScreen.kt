package com.vincent.grainledger.ui.screens.updater

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.data.updater.DownloadStatus
import com.vincent.grainledger.data.updater.UpdateCheckState
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.AppPageScaffold
import com.vincent.grainledger.ui.components.layout.SectionHeader
import com.vincent.grainledger.ui.screens.updater.components.CurrentVersionCard
import com.vincent.grainledger.ui.screens.updater.components.ReleaseHistoryCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.topFadingEdge
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 独立的检查更新与版本发布历史页面 (UpdateScreen)。
 *
 * 接入通用页面容器 AppPageScaffold，完美适配顶部状态栏沉浸、安全边距与返回导航，
 * 遵循单一数据源 (SSOT) 原则，提供版本检查、多通道下载与历史发布浏览。
 *
 * @param viewModel 全局视图模型
 * @param onBack 点击返回上一页回调
 */
@Composable
fun UpdateScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    // 监听系统返回键与手势，返回上一层设置页面
    BackHandler(onBack = onBack)

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

    AppPageScaffold(
        title = "检查更新",
        subtitle = "版本状态与更新历史记录",
        onBack = onBack,
        applyStatusBarPadding = true,
        applyNavigationBarPadding = true
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .topFadingEdge(20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 当前版本与检查卡片
            item {
                CurrentVersionCard(
                    currentVersion = currentAppVersion,
                    onCheckUpdateClick = {
                        showDialog = true
                        viewModel.checkForUpdates(currentAppVersion)
                    }
                )
            }

            // 2. 历史版本发布记录分区标题
            item {
                SectionHeader(
                    title = "版本发布历史",
                    actionSlot = {
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
                )
            }

            // 3. 历史版本列表
            if (releaseHistoryList.isEmpty() && !isLoadingHistory) {
                item {
                    EmptyStateView(
                        title = "暂无更多历史版本发布信息",
                        message = "已是最新版本或未联网获取到记录"
                    )
                }
            } else {
                items(releaseHistoryList, key = { it.tagName }) { release ->
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
