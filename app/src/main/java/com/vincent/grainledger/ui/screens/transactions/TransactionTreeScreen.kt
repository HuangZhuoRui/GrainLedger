package com.vincent.grainledger.ui.screens.transactions

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.MonthPagerScaffold
import com.vincent.grainledger.ui.screens.budget.CreateMonthDialog
import com.vincent.grainledger.ui.screens.transactions.components.TransactionDailyCard
import com.vincent.grainledger.ui.screens.transactions.components.TransactionMonthSummaryCard
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 每日账单与多维层级树状流水页面 (TransactionTreeScreen)。
 *
 * 基于 MonthPagerScaffold 通用脚手架构建：
 * 顶部展示居中渐变缩放月份进度轴，下方大卡片承载月度收支汇总、
 * 收支筛选器与每日层级流水树。
 *
 * @param viewModel 全局主视图模型
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionTreeScreen(
    viewModel: MainViewModel,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val transactionsMap by viewModel.transactionsMap.collectAsState()

    var pendingDeleteRecord by remember { mutableStateOf<TransactionRecord?>(null) }
    var showCreateMonthDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableIntStateOf(0) } // 0: 全部, 1: 仅支出, 2: 仅收入

    MonthPagerScaffold(
        availableMonths = availableMonths,
        currentYear = currentYear,
        currentMonth = currentMonth,
        pageTitle = "账单流水",
        subtitle = "查看每日支出与入账明细及双剩余实时变化",
        isActive = isActive,
        onMonthSelected = { year, month ->
            viewModel.selectMonth(year, month)
        },
        onAddMonthClick = {
            showCreateMonthDialog = true
        },
        dialogs = {
            // 删除记录二次确认弹窗
            if (pendingDeleteRecord != null) {
                val record = pendingDeleteRecord!!
                val isIncome = record.amount > 0
                val typeName = if (isIncome) "入账" else "支出"
                val formattedAmount = if (isIncome) "+${MathFormulaEvaluator.formatAmount(record.amount)} ¥" else "-${MathFormulaEvaluator.formatAmount(record.absoluteAmount)} ¥"
                ConfirmDialog(
                    title = "删除${typeName}记录",
                    message = "确定要删除【${record.categoryName} - ${record.detailName}】金额 ${formattedAmount} 的这笔${typeName}记录吗？删除后可用结余将自动反算回补。",
                    onConfirm = {
                        viewModel.deleteTransaction(record)
                        pendingDeleteRecord = null
                    },
                    onDismiss = {
                        pendingDeleteRecord = null
                    }
                )
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
    ) { targetYear, targetMonth ->
        // 瞬间从全量预载内存缓存中读取当前目标月份的流水列表，0 延迟秒级呈现
        val monthTransactions = transactionsMap[Pair(targetYear, targetMonth)] ?: emptyList()

        // 区分支出与收入
        val expenseRecords = remember(monthTransactions) {
            monthTransactions.filter { it.amount < 0 }
        }
        val incomeRecords = remember(monthTransactions) {
            monthTransactions.filter { it.amount > 0 }
        }
        val totalExpense = remember(expenseRecords) {
            expenseRecords.sumOf { -it.amount }
        }
        val totalIncome = remember(incomeRecords) {
            incomeRecords.sumOf { it.amount }
        }

        // 根据筛选器过滤
        val displayedRecords = remember(filterType, monthTransactions, expenseRecords, incomeRecords) {
            when (filterType) {
                1 -> expenseRecords
                2 -> incomeRecords
                else -> monthTransactions
            }
        }

        // 按日期（天）降序分组
        val dayGroupMap = remember(displayedRecords) {
            displayedRecords.groupBy { it.day }
        }
        val sortedDays = remember(dayGroupMap) {
            dayGroupMap.keys.sortedDescending()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 当月收支流水统计卡片
            item {
                TransactionMonthSummaryCard(
                    year = targetYear,
                    month = targetMonth,
                    totalExpense = totalExpense,
                    totalIncome = totalIncome,
                    expenseCount = expenseRecords.size,
                    incomeCount = incomeRecords.size
                )
            }

            // 2. 收支分类切换过滤器（平滑滑动胶囊指示器，物理移动轨迹与零水波纹闪烁）
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(MiuixShapes.MediumSquircle)
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp)
                ) {
                    val containerWidth = maxWidth
                    val segmentCount = 3
                    val tabWidth = containerWidth / segmentCount

                    // 动画计算滑动胶囊的 X 轴偏移量（Spring 物理弹簧阻尼）
                    val animatedOffsetX by animateDpAsState(
                        targetValue = tabWidth * filterType,
                        animationSpec = MiuixAnimation.springSmooth(),
                        label = "filterIndicatorOffset"
                    )

                    // 动画计算滑动胶囊背景颜色
                    val activeCapsuleColor = when (filterType) {
                        1 -> MiuixRed.copy(alpha = 0.16f)
                        2 -> MiuixGreen.copy(alpha = 0.16f)
                        else -> MiuixTheme.colorScheme.surface
                    }

                    // 1. 底层平滑左右滑动的物理胶囊指示器
                    Box(
                        modifier = Modifier
                            .offset(x = animatedOffsetX)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(activeCapsuleColor)
                    )

                    // 2. 顶层无水波纹点击触发层与文本
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 全部 (0)
                        val isAllSelected = filterType == 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(MiuixShapes.SmallSquircle)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    filterType = 0
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "全部 (${monthTransactions.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAllSelected) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        // 仅支出 (1)
                        val isExpenseSelected = filterType == 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(MiuixShapes.SmallSquircle)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    filterType = 1
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "支出 (${expenseRecords.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpenseSelected) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        // 仅收入 (2)
                        val isIncomeSelected = filterType == 2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(MiuixShapes.SmallSquircle)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    filterType = 2
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "收入 (${incomeRecords.size})",
                                fontSize = 13.sp,
                                fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncomeSelected) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }
            }

            // 3. 空状态或流水列表
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
                        year = targetYear,
                        month = targetMonth,
                        day = day,
                        records = dayRecords,
                        onItemClick = { record ->
                            pendingDeleteRecord = record
                        }
                    )
                }
            }
        }
    }
}
