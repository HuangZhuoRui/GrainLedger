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
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.util.DateUtils
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 每日流水聚合卡片 (TransactionDailyCard)。
 *
 * 将属于同一日期的多笔消费聚合展示，并呈现当天支出小计。
 *
 * @param year 当前年份
 * @param month 当前月份
 * @param day 当前日期（日）
 * @param records 当日消费记录列表
 * @param onItemClick 点击单笔流水记录回调
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionDailyCard(
    year: Int,
    month: Int,
    day: Int,
    records: List<TransactionRecord>,
    onItemClick: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyTotalSpent = records.sumOf { it.amount }
    val dayOfWeek = DateUtils.getWeekDayName(year, month, day)

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 日期行头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${month}月${day}日",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dayOfWeek,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "支出小计: ",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    AmountText(
                        amount = -dailyTotalSpent,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixRed
                    )
                }
            }

            // 当日记录列表
            records.forEach { record ->
                TransactionItemRow(
                    record = record,
                    onClick = { onItemClick(record) }
                )
            }
        }
    }
}
