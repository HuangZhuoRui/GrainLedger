package com.vincent.grainledger.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.MiuixMonthSelector
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.DateUtils
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 每日账单与多维层级树状流水页面。
 *
 * 对应 Excel 表格中的《每日账单》与《综合查看》多维流水树：
 * 采用 年 -> 月 -> 日 -> 交易明细 的层级结构清晰呈现每一笔支出，
 * 并且实时精准展示每一笔消费发生后的【具体剩余】与【类剩余】。
 *
 * @param viewModel 全局主视图模型
 */
@Composable
fun TransactionTreeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val transactionList by viewModel.currentTransactions.collectAsState()

    var pendingDeleteRecord by remember { mutableStateOf<TransactionRecord?>(null) }

    // 按日期（天）进行层级分组
    val dayGroupMap = remember(transactionList) {
        transactionList.groupBy { it.day }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部标题
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "账单流水",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "查看每日支出明细与双剩余实时变化",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 2. 月份选择胶囊
            item {
                MiuixMonthSelector(
                    availableMonthList = availableMonths,
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    onMonthSelected = { year, month ->
                        viewModel.selectMonth(year, month)
                    }
                )
            }

            // 3. 当月流水统计卡片
            item {
                val totalSpent = transactionList.sumOf { it.amount }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${currentYear}年${currentMonth}月 总开销",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            Text(
                                text = "¥${MathFormulaEvaluator.formatAmount(-totalSpent)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixRed
                            )
                        }

                        Text(
                            text = "共记录 ${transactionList.size} 笔账目",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }

            // 4. 空状态提示
            if (dayGroupMap.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "本月暂无记账流水，去首页快速记一笔吧",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            } else {
                // 5. 按天分组的层级列表
                dayGroupMap.forEach { (day, dayTransactionList) ->
                    val dayTotalSpent = dayTransactionList.sumOf { it.amount }
                    val weekDayName = DateUtils.getWeekDayName(currentYear, currentMonth, day)

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentMonth}月${day}日 $weekDayName",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "当日支出: ¥${MathFormulaEvaluator.formatAmount(-dayTotalSpent)}",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }

                    items(dayTransactionList, key = { it.recordId }) { transaction ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pendingDeleteRecord = transaction
                                    },
                                cornerRadius = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MiuixBlue.copy(alpha = 0.12f),
                                                        MiuixShapes.SmallSquircle
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = transaction.categoryName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MiuixBlue
                                                )
                                            }

                                            Text(
                                                text = transaction.detailName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MiuixTheme.colorScheme.onSurface
                                            )
                                        }

                                        Text(
                                            text = "${MathFormulaEvaluator.formatAmount(transaction.amount)} 元",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (transaction.amount < 0) MiuixRed else MiuixGreen
                                        )
                                    }

                                    // 双剩余指标展示栏（具体剩余与类剩余）
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                MiuixShapes.SmallSquircle
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "具体剩余: ¥${MathFormulaEvaluator.formatAmount(transaction.itemRemaining)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "类剩余: ¥${MathFormulaEvaluator.formatAmount(transaction.categoryRemaining)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (transaction.remark.isNotEmpty()) {
                                        Text(
                                            text = "备注: ${transaction.remark}",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 删除账单确认弹窗
    if (pendingDeleteRecord != null) {
        val record = pendingDeleteRecord!!
        OverlayDialog(
            show = true,
            onDismissRequest = { pendingDeleteRecord = null },
            title = "删除账单记录"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "确定要删除「${record.detailName} (${MathFormulaEvaluator.formatAmount(record.amount)}元)」这笔记录吗？删除后将自动恢复对应的预算额度。",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { pendingDeleteRecord = null },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            viewModel.deleteTransaction(record.recordId)
                            pendingDeleteRecord = null
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixRed)
                    ) {
                        Text(text = "确认删除", color = Color.White)
                    }
                }
            }
        }
    }
}
