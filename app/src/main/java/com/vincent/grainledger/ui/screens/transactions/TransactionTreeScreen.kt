package com.vincent.grainledger.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.control.MiuixMonthSelector
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.PageHeader
import com.vincent.grainledger.ui.screens.budget.CreateMonthDialog
import com.vincent.grainledger.ui.screens.transactions.components.TransactionDailyCard
import com.vincent.grainledger.ui.screens.transactions.components.TransactionMonthSummaryCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 每日账单与多维层级树状流水页面 (TransactionTreeScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，全响应式呈现 年 -> 月 -> 日 -> 交易明细 层级，
 * 实时同步展示每一笔消费与入账发生后的【具体剩余】与【类剩余】，支持收支类型智能筛选。
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
    var filterType by remember { mutableIntStateOf(0) } // 0: 全部, 1: 仅支出, 2: 仅收入

    // 区分支出与收入
    val expenseRecords = remember(transactionList) {
        transactionList.filter { it.amount < 0 }
    }
    val incomeRecords = remember(transactionList) {
        transactionList.filter { it.amount > 0 }
    }
    val totalExpense = remember(expenseRecords) {
        expenseRecords.sumOf { -it.amount }
    }
    val totalIncome = remember(incomeRecords) {
        incomeRecords.sumOf { it.amount }
    }

    // 根据筛选器过滤
    val displayedRecords = remember(filterType, transactionList, expenseRecords, incomeRecords) {
        when (filterType) {
            1 -> expenseRecords
            2 -> incomeRecords
            else -> transactionList
        }
    }

    // 按日期（天）降序分组
    val dayGroupMap = remember(displayedRecords) {
        displayedRecords.groupBy { it.day }
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
                    subtitle = "查看每日支出与入账明细及双剩余实时变化"
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

            // 3. 当月收支流水统计卡片
            item {
                TransactionMonthSummaryCard(
                    year = currentYear,
                    month = currentMonth,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    expenseCount = expenseRecords.size,
                    incomeCount = incomeRecords.size
                )
            }

            // 4. 收支分类切换过滤器
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MiuixShapes.MediumSquircle)
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 全部
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (filterType == 0) MiuixTheme.colorScheme.surface else Color.Transparent)
                            .clickable { filterType = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "全部 (${transactionList.size})",
                            fontSize = 13.sp,
                            fontWeight = if (filterType == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterType == 0) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }

                    // 仅支出
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (filterType == 1) MiuixRed.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { filterType = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "支出 (${expenseRecords.size})",
                            fontSize = 13.sp,
                            fontWeight = if (filterType == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterType == 1) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }

                    // 仅收入
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (filterType == 2) MiuixGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { filterType = 2 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "收入 (${incomeRecords.size})",
                            fontSize = 13.sp,
                            fontWeight = if (filterType == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (filterType == 2) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }

            // 5. 空状态或流水列表
            if (sortedDays.isEmpty()) {
                item {
                    EmptyStateView(
                        title = when (filterType) {
                            1 -> "本月暂无支出流水"
                            2 -> "本月暂无入账流水"
                            else -> "本月暂无记账流水"
                        },
                        message = "在看板页面点击“记一笔”开始记录吧"
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
            val isIncome = record.amount > 0
            val typeName = if (isIncome) "入账" else "支出"
            val formattedAmount = if (isIncome) "+¥${MathFormulaEvaluator.formatAmount(record.amount)}" else "-¥${MathFormulaEvaluator.formatAmount(record.absoluteAmount)}"
            ConfirmDialog(
                title = "删除${typeName}记录",
                message = "确定要删除【${record.categoryName} - ${record.detailName}】金额 ${formattedAmount} 的这笔${typeName}记录吗？删除后细项与大类结余将自动反算回补。",
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
