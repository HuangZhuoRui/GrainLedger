package com.vincent.grainledger.ui.screens.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.control.SettingItemRow
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed

/**
 * 数据备份与 Excel 互通管理卡片 (DataBackupCard)。
 *
 * 提供标准 Excel 格式账单导入、导出、细分数据清理及初始数据重置操作。
 *
 * @param isProcessingFile 是否正在进行文件 IO 处理
 * @param onImportClick 点击导入 Excel 回调
 * @param onExportClick 点击导出 Excel 回调
 * @param onResetClick 点击重置初始数据回调
 * @param onCleanDataClick 点击细分数据清理回调
 * @param modifier 外部修饰符
 */
@Composable
fun DataBackupCard(
    isProcessingFile: Boolean,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onResetClick: () -> Unit,
    onCleanDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixSectionCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            SettingItemRow(
                title = "从 Excel 文件导入账单",
                subtitle = "支持解析《账单.xlsx》并自动持久化数据",
                icon = Icons.Default.CloudDownload,
                iconTint = MiuixGreen,
                onClick = onImportClick,
                trailingSlot = if (isProcessingFile) {
                    {
                        CircularProgressIndicator(
                            color = MiuixGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )

            SettingItemRow(
                title = "导出数据为 Excel 账单",
                subtitle = "完整导出所有月份预算与流水至本地表格",
                icon = Icons.Default.Share,
                iconTint = MiuixBlue,
                onClick = onExportClick
            )

            SettingItemRow(
                title = "重置为默认初始数据",
                subtitle = "清空现有数据库并重新加载初始测试账目",
                icon = Icons.Default.Refresh,
                iconTint = MiuixBlue,
                onClick = onResetClick
            )

            SettingItemRow(
                title = "数据清理",
                subtitle = "提供细分清理：仅清流水、仅清预算或彻底清空",
                icon = Icons.Default.DeleteSweep,
                iconTint = MiuixRed,
                onClick = onCleanDataClick
            )
        }
    }
}
