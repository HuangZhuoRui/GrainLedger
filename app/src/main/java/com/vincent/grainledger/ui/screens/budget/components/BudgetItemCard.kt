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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 单个预算条目卡片 (BudgetItemCard)。
 *
 * 展示预算细项名称、单价/数量、注入金额、已花金额、实时结余，并支持点击编辑。
 *
 * @param budgetItem 预算实体
 * @param onEditClick 点击编辑回调
 * @param modifier 外部修饰符
 */
@Composable
fun BudgetItemCard(
    budgetItem: BudgetItem,
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

            Text(
                text = if (budgetItem.quantity > 1.0) {
                    "单价 ¥${budgetItem.unitPrice} × ${budgetItem.quantity} = 总价 ¥${budgetItem.totalPrice}"
                } else {
                    "预算总价: ¥${budgetItem.totalPrice}"
                },
                fontSize = 11.5.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
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
                    text = "已花 ¥${budgetItem.actualSpent} / 注入 ¥${budgetItem.actualAllocated}",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MiuixBlue.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MiuixBlue,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
