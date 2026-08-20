package com.vincent.grainledger.ui.screens.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixRed
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 月度流水统计卡片 (TransactionMonthSummaryCard)。
 *
 * 聚合展示选定月份的总开销金额与记账总笔数。
 *
 * @param year 当前年份
 * @param month 当前月份
 * @param totalSpent 总开销金额
 * @param transactionCount 交易记录条数
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionMonthSummaryCard(
    year: Int,
    month: Int,
    totalSpent: Double,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${year}年${month}月 总开销",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = -totalSpent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixRed
                )
            }

            Text(
                text = "共记录 $transactionCount 笔账目",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}
