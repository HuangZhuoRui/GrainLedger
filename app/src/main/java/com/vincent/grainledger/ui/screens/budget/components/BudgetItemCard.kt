package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 单个预算/收入条目卡片 (BudgetItemCard)。
 *
 * 智能感知大类收入/支出属性，展示精准对应的细项名称、单价/数量、注入/到账金额、已花金额与实时结余。
 *
 * @param budgetItem 预算实体
 * @param isIncome 是否为收入类细项
 * @param onEditClick 点击编辑回调
 * @param modifier 外部修饰符
 */
@Composable
fun BudgetItemCard(
    budgetItem: BudgetItem,
    isIncome: Boolean = false,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MiuixShapes.MediumSquircle
            )
            .clickable(onClick = onEditClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = budgetItem.detailName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                if (budgetItem.remark.isNotBlank()) {
                    Text(
                        text = "(${budgetItem.remark})",
                        fontSize = 11.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            val formattedQuantity = if (budgetItem.quantity % 1.0 == 0.0) budgetItem.quantity.toInt().toString() else budgetItem.quantity.toString()

            if (isIncome) {
                if (budgetItem.quantity > 1.0) {
                    Text(
                        text = "单价 ${MathFormulaEvaluator.formatAmount(budgetItem.unitPrice)} ¥ × $formattedQuantity",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "预估收入 ${MathFormulaEvaluator.formatAmount(budgetItem.totalPrice)} ¥",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                } else {
                    Text(
                        text = "预估收入: ${MathFormulaEvaluator.formatAmount(budgetItem.totalPrice)} ¥",
                        fontSize = 11.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            } else {
                if (budgetItem.quantity > 1.0) {
                    Text(
                        text = "单价 ${MathFormulaEvaluator.formatAmount(budgetItem.unitPrice)} ¥ × $formattedQuantity",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "预算总价 ${MathFormulaEvaluator.formatAmount(budgetItem.totalPrice)} ¥",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                } else {
                    Text(
                        text = "预算总价: ${MathFormulaEvaluator.formatAmount(budgetItem.totalPrice)} ¥",
                        fontSize = 11.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (isIncome) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "到账: ",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Text(
                            text = "+${MathFormulaEvaluator.formatAmount(budgetItem.actualAllocated)} ¥",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixGreen
                        )
                    }
                    Text(
                        text = "收款: ${budgetItem.funder}",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "剩余: ",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        AmountText(
                            amount = budgetItem.balance,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (budgetItem.balance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "已花 ${MathFormulaEvaluator.formatAmount(budgetItem.actualSpent)} ¥ / 注入 ${MathFormulaEvaluator.formatAmount(budgetItem.actualAllocated)} ¥",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        (if (isIncome) MiuixGreen else MiuixBlue).copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = if (isIncome) MiuixGreen else MiuixBlue,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
