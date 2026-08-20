package com.vincent.grainledger.ui.screens.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 每日流水记录项组件 (TransactionItemRow)。
 *
 * 规范区分呈现单笔收入与支出流水：
 * 支出以红色展示负数消费与消费后双剩余（项余与类余）；
 * 收入以绿色展示正数入账与入账渠道。
 *
 * @param record 流水记录实体
 * @param onClick 点击记录项回调（如触发删除确认）
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionItemRow(
    record: TransactionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = !record.isExpense

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：分类标签、细项名称、收款/出资渠道与备注
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(
                    text = if (isIncome) "${record.categoryName} (入)" else record.categoryName,
                    color = if (isIncome) MiuixGreen else MiuixBlue,
                    fontSize = 11.sp
                )
                Text(
                    text = record.detailName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (record.funder.isNotBlank()) {
                    Text(
                        text = if (isIncome) "收款: ${record.funder}" else "出资: ${record.funder}",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
                if (record.remark.isNotBlank()) {
                    Text(
                        text = "• ${record.remark}",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }

        // 右侧：金额与双剩余
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isIncome) {
                    "+${MathFormulaEvaluator.formatAmount(record.amount)} ¥"
                } else {
                    "-${MathFormulaEvaluator.formatAmount(record.absoluteAmount)} ¥"
                },
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MiuixGreen else MiuixRed
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isIncome) "项额 ${MathFormulaEvaluator.formatAmount(record.itemRemaining)} ¥" else "项余 ${MathFormulaEvaluator.formatAmount(record.itemRemaining)} ¥",
                    fontSize = 10.5.sp,
                    color = if (isIncome) MiuixGreen else if (record.itemRemaining < 0) MiuixRed else MiuixGreen,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isIncome) "类额 ${MathFormulaEvaluator.formatAmount(record.categoryRemaining)} ¥" else "类余 ${MathFormulaEvaluator.formatAmount(record.categoryRemaining)} ¥",
                    fontSize = 10.5.sp,
                    color = if (isIncome) MiuixGreen else if (record.categoryRemaining < 0) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }
    }
}
