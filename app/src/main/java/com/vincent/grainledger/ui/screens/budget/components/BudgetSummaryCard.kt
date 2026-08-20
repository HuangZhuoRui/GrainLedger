package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixRed
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算统计汇总卡片 (BudgetSummaryCard)。
 *
 * 聚合展示当前月份的规划总预算、实际注入、本月已消费及剩余可用结余。
 *
 * @param budgetItemList 当月预算细项列表
 * @param modifier 外部修饰符
 */
@Composable
fun BudgetSummaryCard(
    budgetItemList: List<BudgetItem>,
    modifier: Modifier = Modifier
) {
    val totalBudget = budgetItemList.sumOf { it.totalPrice }
    val totalAllocated = budgetItemList.sumOf { it.actualAllocated }
    val totalSpent = budgetItemList.sumOf { it.actualSpent }
    val totalBalance = totalAllocated - totalSpent

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "规划总预算",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = totalBudget,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = "实际注入",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = totalAllocated,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixBlue
                )
            }

            Column {
                Text(
                    text = "已消费",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = totalSpent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixRed
                )
            }

            Column {
                Text(
                    text = "剩余结余",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = totalBalance,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (totalBalance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}
