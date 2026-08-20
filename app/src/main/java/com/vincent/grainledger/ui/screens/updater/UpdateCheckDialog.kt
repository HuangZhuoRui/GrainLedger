package com.vincent.grainledger.ui.screens.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.R
import com.vincent.grainledger.data.updater.DownloadProgress
import com.vincent.grainledger.data.updater.DownloadStatus
import com.vincent.grainledger.data.updater.GitHubRelease
import com.vincent.grainledger.data.updater.UpdateCheckState
import com.vincent.grainledger.ui.components.feedback.DownloadProgressPanel
import com.vincent.grainledger.ui.screens.updater.components.ChangelogSection
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixPurple
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * MIUIX 规范检查更新弹窗 (UpdateCheckDialog)。
 *
 * 展示检查中、发现新版本、自建高速镜像加速分发、实时下载进度与最新版本提示。
 *
 * @param checkState 当前检查更新状态
 * @param downloadProgress 当前下载进度状态
 * @param onStartDownload 触发高速下载回调
 * @param onCancelDownload 取消下载回调
 * @param onRetry 重新检查回调
 * @param onDismiss 关闭弹窗回调
 */
@Composable
fun UpdateCheckDialog(
    checkState: UpdateCheckState,
    downloadProgress: DownloadProgress,
    onStartDownload: (downloadUrl: String, fileName: String) -> Unit,
    onCancelDownload: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = {
            if (downloadProgress.status != DownloadStatus.DOWNLOADING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = downloadProgress.status != DownloadStatus.DOWNLOADING,
            dismissOnClickOutside = downloadProgress.status != DownloadStatus.DOWNLOADING,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (downloadProgress.status != DownloadStatus.DOWNLOADING) {
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(250)) + scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.9f),
                exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.9f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.90f)
                        .padding(horizontal = 16.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (checkState) {
                            is UpdateCheckState.Checking -> {
                                CheckingView()
                            }
                            is UpdateCheckState.HasUpdate -> {
                                HasUpdateView(
                                    release = checkState.release,
                                    currentVersion = checkState.currentVersion,
                                    acceleratedUrl = checkState.acceleratedDownloadUrl,
                                    downloadProgress = downloadProgress,
                                    onStartDownload = onStartDownload,
                                    onCancelDownload = onCancelDownload,
                                    onDismiss = onDismiss
                                )
                            }
                            is UpdateCheckState.AlreadyLatest -> {
                                AlreadyLatestView(
                                    currentVersion = checkState.currentVersion,
                                    onDismiss = onDismiss
                                )
                            }
                            is UpdateCheckState.Error -> {
                                ErrorView(
                                    errorMessage = checkState.message,
                                    onRetry = onRetry,
                                    onDismiss = onDismiss
                                )
                            }
                            is UpdateCheckState.Idle -> {
                                // 空闲状态
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 正在检查更新视图。
 */
@Composable
private fun CheckingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MiuixBlue,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "正在检查更新...",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "正在获取最新版本信息...",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
    }
}

/**
 * 发现新版本更新视图。
 */
@Composable
private fun HasUpdateView(
    release: GitHubRelease,
    currentVersion: String = "1.0.0",
    acceleratedUrl: String,
    downloadProgress: DownloadProgress,
    onStartDownload: (downloadUrl: String, fileName: String) -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = downloadProgress.status == DownloadStatus.DOWNLOADING
    val apkAsset = release.androidAsset
    val fileName = apkAsset?.name ?: "GrainLedger-${release.tagName}.apk"

    val animatedProgress by animateFloatAsState(
        targetValue = downloadProgress.progress,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "DownloadProgressAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // 头部标题与版本徽章
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "余粮 Logo",
                modifier = Modifier
                    .size(48.dp)
                    .clip(MiuixShapes.MediumSquircle)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "发现新版本",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(
                    text = "当前: v$currentVersion  ➔  最新: ${release.tagName}",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }

        if (apkAsset != null && apkAsset.formattedSize.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "安装包体积",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                Text(
                    text = apkAsset.formattedSize,
                    fontSize = 12.sp,
                    color = MiuixBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 更新日志内容
        Text(
            text = "更新内容：",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))

        val changelog = release.parsedChangelog
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (changelog.hasCategorized) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                }
            } else {
                Text(
                    text = if (release.body.isNotBlank()) release.body else "本次更新包含多项稳定性改进与体验优化。",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 下载进度显示或操作按钮
        if (isDownloading) {
            DownloadProgressPanel(
                downloadProgress = downloadProgress,
                onCancelDownload = onCancelDownload
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val targetDownloadUrl = if (acceleratedUrl.isNotBlank()) acceleratedUrl else (release.androidDownloadUrl ?: "")
                            onStartDownload(targetDownloadUrl, fileName)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiuixBlue)
                    ) {
                        Text(
                            text = "加速下载",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = {
                            val directUrl = release.androidDownloadUrl ?: ""
                            onStartDownload(directUrl, fileName)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "正常下载",
                            color = MiuixTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = "稍后再说", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
        }
    }
}

/**
 * 已是最新版本视图。
 */
@Composable
private fun AlreadyLatestView(
    currentVersion: String = "1.0.0",
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 软件官方精致 Logo
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "余粮 Logo",
            modifier = Modifier
                .size(60.dp)
                .clip(MiuixShapes.MediumSquircle)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "当前已是最新版本",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "余粮 v$currentVersion 运转良好",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(0.6f),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiuixBlue)
        ) {
            Text(text = "我知道了", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 检查失败或异常视图。
 */
@Composable
private fun ErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MiuixRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "检查失败",
                tint = MiuixRed,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "检查更新失败",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "取消", color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiuixBlue)
            ) {
                Text(text = "重试", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
