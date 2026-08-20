package com.vincent.grainledger.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.control.MiuixMonthSelector
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.PageHeader
import com.vincent.grainledger.ui.screens.budget.CreateMonthDialog
import com.vincent.grainledger.ui.screens.transactions.components.TransactionDailyCard
import com.vincent.grainledger.ui.screens.transactions.components.TransactionMonthSummaryCard
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 每日账单与多维层级树状流水页面 (TransactionTreeScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，全响应式呈现 年 -> 月 -> 日 -> 交易明细 层级，
 * 实时同步展示每一笔消费发生后的【具体剩余】与【类剩余】。
 *
 * @param viewModel 全局主视图模型
 * @param modifier 外部修饰符
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
    var showCreateMonthDialog by remember { mutableStateOf(false) }

    // 按日期（天）降序分组
    val dayGroupMap = remember(transactionList) {
        transactionList.groupBy { it.day }
    }
    val sortedDays = remember(dayGroupMap) {
        dayGroupMap.keys.sortedDescending()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部标题
            item {
                PageHeader(
                    title = "账单流水",
                    subtitle = "查看每日支出明细与双剩余实时变化"
                )
            }

            // 2. 月份选择胶囊
            item {
                MiuixMonthSelector(
                    availableMonthList = availableMonths,
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    onMonthSelected = { year, month ->
                        viewModel.selectMonth(year, month)
                    },
                    onAddMonthClick = {
                        showCreateMonthDialog = true
                    }
                )
            }

            // 3. 当月流水统计卡片
            item {
                val totalSpent = transactionList.sumOf { it.amount }
                TransactionMonthSummaryCard(
                    year = currentYear,
                    month = currentMonth,
                    totalSpent = totalSpent,
                    transactionCount = transactionList.size
                )
            }

            // 4. 空状态或流水列表
            if (sortedDays.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "本月暂无记账流水",
                        message = "在看板页面点击“记一笔”记录第一笔日常开支吧"
                    )
                }
            } else {
                items(sortedDays, key = { it }) { day ->
                    val dayRecords = dayGroupMap[day] ?: emptyList()
                    TransactionDailyCard(
                        year = currentYear,
                        month = currentMonth,
                        day = day,
                        records = dayRecords,
                        onItemClick = { record ->
                            pendingDeleteRecord = record
                        }
                    )
                }
            }
        }

        // 删除记录二次确认弹窗
        if (pendingDeleteRecord != null) {
            val record = pendingDeleteRecord!!
            ConfirmDialog(
                title = "删除记账记录",
                message = "确定要删除【${record.categoryName} - ${record.detailName}】金额 ¥${record.amount} 的这笔支出记录吗？删除后细项与大类结余将自动反算回补。",
                onConfirm = {
                    viewModel.deleteTransaction(record)
                    pendingDeleteRecord = null
                },
                onDismiss = {
                    pendingDeleteRecord = null
                }
            )
        }
    }

    // 新建月份账本弹窗
    if (showCreateMonthDialog) {
        CreateMonthDialog(
            currentYear = currentYear,
            currentMonth = currentMonth,
            availableMonths = availableMonths,
            onCreateMonth = { targetYear, targetMonth, copyBudget ->
                viewModel.createMonth(targetYear, targetMonth, copyBudget)
                showCreateMonthDialog = false
            },
            onDismissRequest = {
                showCreateMonthDialog = false
            }
        )
    }
}
