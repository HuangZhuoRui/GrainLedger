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
import androidx.compose.material.icons.filled.Category
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
import com.vincent.grainledger.ui.screens.category.CategoryManagementDialog
import com.vincent.grainledger.ui.screens.settings.components.AppInfoCard
import com.vincent.grainledger.ui.screens.settings.components.DataBackupCard
import com.vincent.grainledger.ui.screens.settings.components.ThemeSettingCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixPurple
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 设置、分类管理、主题偏好、检查更新与 Excel 导入导出管理页面 (SettingsScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，提供标准 Excel 文件的双向互通导入导出、重置初始数据、
 * 预算分类全量管理、暗色模式切换、进入独立检查更新页面与关于信息。
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
    val allCategories by viewModel.allCategories.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showDataCleanDialog by remember { mutableStateOf(false) }
    var pendingCleanType by remember { mutableStateOf<com.vincent.grainledger.ui.screens.settings.components.CleanTargetType?>(null) }
    var showCategoryManagementDialog by remember { mutableStateOf(false) }
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
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部标题栏
            item {
                PageHeader(
                    title = "设置与数据",
                    subtitle = "分类管理、Excel 互通、软件更新、外观与数据备份"
                )
            }

            // 2. 预算分类管理分区
            item {
                SectionHeader(title = "预算与分类")
            }

            item {
                MiuixSectionCard(
                    contentPadding = PaddingValues(0.dp)
                ) {
                    SettingItemRow(
                        title = "预算大类管理",
                        subtitle = "自定义预算分类名称与主题色彩 • 共 ${allCategories.size} 大类",
                        icon = Icons.Default.Category,
                        iconTint = MiuixPurple,
                        onClick = { showCategoryManagementDialog = true }
                    )
                }
            }

            // 3. 软件检查更新分区
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

            // 4. 外观设置分区
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

            // 5. 数据与备份管理分区
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
                    },
                    onCleanDataClick = {
                        showDataCleanDialog = true
                    }
                )
            }

            // 6. 关于应用分区
            item {
                SectionHeader(title = "关于软件")
            }

            item {
                AppInfoCard()
            }
        }

        // 预算大类管理弹窗
        if (showCategoryManagementDialog) {
            CategoryManagementDialog(
                categoryList = allCategories,
                onSaveCategory = { newCategory, oldName ->
                    viewModel.saveCategory(newCategory, oldName)
                },
                onDeleteCategory = { categoryToDelete, deleteAssociated ->
                    viewModel.deleteCategory(categoryToDelete, deleteAssociated)
                },
                onDismissRequest = {
                    showCategoryManagementDialog = false
                }
            )
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

        // 细分数据清理选择弹窗
        if (showDataCleanDialog) {
            com.vincent.grainledger.ui.screens.settings.components.DataCleanDialog(
                onConfirmClean = { cleanType ->
                    showDataCleanDialog = false
                    pendingCleanType = cleanType
                },
                onDismissRequest = {
                    showDataCleanDialog = false
                }
            )
        }

        // 细分数据清理防误触二次确认弹窗
        pendingCleanType?.let { cleanType ->
            val (confirmTitle, confirmMessage) = when (cleanType) {
                com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.TRANSACTIONS ->
                    "清空流水记录" to "确定要清空所有月份的交易流水明细吗？\n所有支出与收入记录将被删除，各预算细项的已消费金额将归零并恢复可用结余！"
                com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.BUDGETS ->
                    "清空预算规划" to "确定要清空所有月份的预算细项规划吗？\n所有月份的预算分配项将被清空，现有的预算分类与历史流水记录将继续保留！"
                com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.ALL ->
                    "彻底清空全部数据" to "确定要彻底清空数据库中所有月份的预算规划、交易流水和自定义分类吗？\n此操作将完全抹除所有记录，恢复为空白账本状态且不可逆！"
            }

            ConfirmDialog(
                title = confirmTitle,
                message = confirmMessage,
                confirmText = "确认清除",
                confirmColor = cleanType.themeColor,
                onConfirm = {
                    when (cleanType) {
                        com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.TRANSACTIONS ->
                            viewModel.clearAllTransactions()
                        com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.BUDGETS ->
                            viewModel.clearAllBudgets()
                        com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.ALL ->
                            viewModel.clearAllData()
                    }
                    pendingCleanType = null
                },
                onDismiss = {
                    pendingCleanType = null
                }
            )
        }
    }
}
