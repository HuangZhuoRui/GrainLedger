package com.vincent.grainledger.ui.screens.budget.components

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
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算大类分组卡片 (BudgetCategoryGroup)。
 *
 * 将属于同一大类（如“强制类”、“饮食类”）的预算细项包裹并展示大类统计汇总。
 *
 * @param categoryName 大类名称
 * @param categoryDefinition 分类样式定义
 * @param items 该大类下的预算细项列表
 * @param onEditItem 点击编辑预算细项回调
 * @param modifier 外部修饰符
 */
@Composable
fun BudgetCategoryGroup(
    categoryName: String,
    categoryDefinition: BudgetCategory?,
    items: List<BudgetItem>,
    onEditItem: (BudgetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = categoryDefinition?.themeColor ?: MiuixBlue
    val groupTotalBudget = items.sumOf { it.totalPrice }
    val groupTotalAllocated = items.sumOf { it.actualAllocated }
    val groupTotalSpent = items.sumOf { it.actualSpent }
    val groupTotalBalance = groupTotalAllocated - groupTotalSpent

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 大类头部标题与统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusBadge(
                        text = categoryName,
                        color = themeColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "共 ${items.size} 项",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                Text(
                    text = "注入 ¥${MathFormulaEvaluator.formatAmount(groupTotalAllocated)} / 结余 ¥${MathFormulaEvaluator.formatAmount(groupTotalBalance)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

            // 细项列表
            items.forEach { item ->
                BudgetItemCard(
                    budgetItem = item,
                    onEditClick = { onEditItem(item) }
                )
            }
        }
    }
}
