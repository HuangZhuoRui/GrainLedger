package com.vincent.grainledger.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.data.updater.UpdateCheckState
import com.vincent.grainledger.ui.screens.updater.UpdateCheckDialog
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 设置、主题偏好、检查更新与 Excel 导入导出管理页面。
 *
 * 提供标准 Excel 文件的双向互通导入导出、重置初始数据、
 * 暗色模式切换、进入独立检查更新页面与关于信息。
 *
 * @param viewModel 全局主视图模型
 * @param onNavigateToUpdate 导航至独立检查更新页面回调
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val isProcessingFile by viewModel.isProcessingFile.collectAsState()
    val darkModePreference by viewModel.darkModePreference.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }

    val currentAppVersion = BuildConfig.VERSION_NAME

    // Excel 导入文件选择器
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importExcelData(inputStream)
                }
            } catch (_: Exception) {}
        }
    }

    // Excel 导出文件选择器
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.exportExcelData(outputStream)
                }
            } catch (_: Exception) {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部标题栏
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "设置与数据",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Excel 互通、软件更新、外观与数据备份管理",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 2. 软件检查更新分区
            item {
                Text(
                    text = "软件更新",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp
                ) {
                    SettingItemRow(
                        icon = Icons.Default.SystemUpdate,
                        iconBackgroundColor = MiuixBlue,
                        primaryTitle = "检查应用更新与发布历史",
                        secondaryTitle = "当前版本: v$currentAppVersion",
                        badgeText = "进入",
                        onClick = onNavigateToUpdate
                    )
                }
            }

            // 3. Excel 导入导出分区
            item {
                Text(
                    text = "Excel 账单互通",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingItemRow(
                            icon = Icons.Default.Share,
                            iconBackgroundColor = MiuixBlue,
                            primaryTitle = "一键导入 Excel 账单 (.xlsx)",
                            secondaryTitle = "支持解析《数据源》与《每日账单》",
                            onClick = {
                                importFileLauncher.launch(arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/octet-stream",
                                    "*/*"
                                ))
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 0.5.dp)
                        )

                        SettingItemRow(
                            icon = Icons.Default.CloudDownload,
                            iconBackgroundColor = MiuixGreen,
                            primaryTitle = "导出为标准 Excel 账单 (.xlsx)",
                            secondaryTitle = "包含完整综合看板、数据源与每日流水",
                            onClick = {
                                exportFileLauncher.launch("余粮账单_备份_${System.currentTimeMillis()}.xlsx")
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 0.5.dp)
                        )

                        SettingItemRow(
                            icon = Icons.Default.Refresh,
                            iconBackgroundColor = MiuixRed,
                            primaryTitle = "恢复初始预置数据",
                            secondaryTitle = "清空现有数据并重置为 2026年 8~12月示例",
                            onClick = {
                                showResetDialog = true
                            }
                        )
                    }
                }
            }

            // 4. 外观与主题模式
            item {
                Text(
                    text = "界面外观",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "主题显示模式",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val optionList = listOf(
                                Triple(null, "跟随系统", "自适应"),
                                Triple(false, "纯白浅色", "以白为主"),
                                Triple(true, "暗夜深色", "AMOLED")
                            )

                            optionList.forEach { (modeValue, title, tag) ->
                                val isSelected = (darkModePreference == modeValue)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant,
                                            shape = MiuixShapes.MediumSquircle
                                        )
                                        .clickable {
                                            viewModel.setDarkModePreference(modeValue)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tag,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. 关于应用
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MiuixBlue.copy(alpha = 0.15f), MiuixShapes.MediumSquircle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MiuixBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "余粮 GrainLedger",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "基于 MIUIX 规范与非线性物理动效构建 · v$currentAppVersion",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }
            }
        }

        // 处理中指示器
        if (isProcessingFile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MiuixBlue)
                        Text(text = "正在处理 Excel 账单，请稍候...", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // 重置数据确认弹窗
    if (showResetDialog) {
        OverlayDialog(
            show = true,
            onDismissRequest = { showResetDialog = false },
            title = "恢复初始数据确认"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "此操作将清空当前所有记账流水与自定义预算，并恢复为初始 2026年 8~12月的示范数据。该操作不可撤销，是否继续？",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showResetDialog = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            viewModel.resetToDefaults()
                            showResetDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = MiuixRed
                        )
                    ) {
                        Text(text = "确认重置", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 设置项单行通用组件。
 */
@Composable
private fun SettingItemRow(
    icon: ImageVector,
    iconBackgroundColor: Color,
    primaryTitle: String,
    secondaryTitle: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconBackgroundColor.copy(alpha = 0.12f),
                    shape = MiuixShapes.MediumSquircle
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconBackgroundColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = primaryTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = secondaryTitle,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                lineHeight = 16.sp
            )
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixBlue.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixBlue
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
