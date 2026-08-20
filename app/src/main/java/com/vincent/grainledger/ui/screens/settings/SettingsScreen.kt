package com.vincent.grainledger.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.control.SettingItemRow
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.components.layout.PageHeader
import com.vincent.grainledger.ui.components.layout.SectionHeader
import com.vincent.grainledger.ui.screens.settings.components.AppInfoCard
import com.vincent.grainledger.ui.screens.settings.components.DataBackupCard
import com.vincent.grainledger.ui.screens.settings.components.ThemeSettingCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 设置、主题偏好、检查更新与 Excel 导入导出管理页面 (SettingsScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，提供标准 Excel 文件的双向互通导入导出、重置初始数据、
 * 暗色模式切换、进入独立检查更新页面与关于信息。
 *
 * @param viewModel 全局主视图模型
 * @param onNavigateToUpdate 导航至独立检查更新页面回调
 * @param modifier 外部修饰符
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateToUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isProcessingFile by viewModel.isProcessingFile.collectAsState()
    val darkModePreference by viewModel.darkModePreference.collectAsState()

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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部标题栏
            item {
                PageHeader(
                    title = "设置与数据",
                    subtitle = "Excel 互通、软件更新、外观与数据备份管理"
                )
            }

            // 2. 软件检查更新分区
            item {
                SectionHeader(title = "软件更新")
            }

            item {
                MiuixSectionCard(
                    contentPadding = PaddingValues(0.dp)
                ) {
                    SettingItemRow(
                        title = "检查新版本",
                        subtitle = "当前版本 v$currentAppVersion • 点击查看历史更新与下载",
                        icon = Icons.Default.SystemUpdate,
                        iconTint = MiuixBlue,
                        onClick = onNavigateToUpdate
                    )
                }
            }

            // 3. 外观设置分区
            item {
                SectionHeader(title = "外观与主题")
            }

            item {
                ThemeSettingCard(
                    darkModePreference = darkModePreference,
                    onPreferenceChange = { pref ->
                        viewModel.setDarkModePreference(pref)
                    }
                )
            }

            // 4. 数据与备份管理分区
            item {
                SectionHeader(title = "数据与文件互通")
            }

            item {
                DataBackupCard(
                    isProcessingFile = isProcessingFile,
                    onImportClick = {
                        importFileLauncher.launch(arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel"
                        ))
                    },
                    onExportClick = {
                        exportFileLauncher.launch("余粮记账数据备份.xlsx")
                    },
                    onResetClick = {
                        showResetDialog = true
                    }
                )
            }

            // 5. 关于应用分区
            item {
                SectionHeader(title = "关于软件")
            }

            item {
                AppInfoCard()
            }
        }

        // 重置数据二次确认弹窗
        if (showResetDialog) {
            ConfirmDialog(
                title = "重置为默认数据",
                message = "确定要清空当前数据库中所有的预算与记账流水，并重新加载内置初始测试数据吗？此操作不可逆！",
                confirmText = "确认重置",
                onConfirm = {
                    viewModel.resetToDefault()
                    showResetDialog = false
                },
                onDismiss = {
                    showResetDialog = false
                }
            )
        }
    }
}
