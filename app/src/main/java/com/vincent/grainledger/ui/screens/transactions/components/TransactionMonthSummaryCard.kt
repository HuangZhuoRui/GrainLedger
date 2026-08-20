package com.vincent.grainledger.ui.screens.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 月度收支流水综合统计卡片 (TransactionMonthSummaryCard)。
 *
 * 聚合展示选定月份的总支出、总入账、收支净结余与笔数分类统计。
 *
 * @param year 当前年份
 * @param month 当前月份
 * @param totalExpense 总支出金额（正数）
 * @param totalIncome 总入账金额（正数）
 * @param expenseCount 支出笔数
 * @param incomeCount 收入笔数
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionMonthSummaryCard(
    year: Int,
    month: Int,
    totalExpense: Double,
    totalIncome: Double,
    expenseCount: Int,
    incomeCount: Int,
    modifier: Modifier = Modifier
) {
    val totalCount = expenseCount + incomeCount
    val netBalance = totalIncome - totalExpense

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 顶部核心数据栏：总支出 与 总入账
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：总支出
                Column {
                    Text(
                        text = "${month}月 总支出",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "-${MathFormulaEvaluator.formatAmount(totalExpense)} ¥",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixRed
                    )
                }

                // 中间/右侧：总收入
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${month}月 总入账",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "+${MathFormulaEvaluator.formatAmount(totalIncome)} ¥",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixGreen
                    )
                }
            }

            // 底部细项统计与笔数
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "收支差额:",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = if (netBalance >= 0) "+${MathFormulaEvaluator.formatAmount(netBalance)} ¥" else "-${MathFormulaEvaluator.formatAmount(-netBalance)} ¥",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netBalance >= 0) MiuixGreen else MiuixRed
                    )
                }

                Text(
                    text = "共 $totalCount 笔 (支出 $expenseCount · 入账 $incomeCount)",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }
    }
}
