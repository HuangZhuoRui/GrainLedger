package com.vincent.grainledger.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.card.BudgetEnvelopeCard
import com.vincent.grainledger.ui.components.card.CapitalBalanceCard
import com.vincent.grainledger.ui.components.card.IncomeEnvelopeCard
import com.vincent.grainledger.ui.components.layout.MonthPagerScaffold
import com.vincent.grainledger.ui.components.layout.SectionHeader
import com.vincent.grainledger.ui.screens.budget.CreateMonthDialog
import com.vincent.grainledger.ui.screens.dashboard.components.MonthlyAssetOverviewCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 综合看板主页面 (DashboardScreen)。
 *
 * 基于 MonthPagerScaffold 通用脚手架构建：
 * 顶部展示居中渐变缩放月份进度轴，下方大卡片承载月度核心资产总览、
 * 资金池配平健康状态、本月收入分类卡片与各支出预算信封列表。
 *
 * @param viewModel 全局主视图模型
 * @param onOpenBookkeeping 触发快速记账弹窗回调
 * @param onBudgetItemClick 点击具体预算项时的回调
 * @param modifier 外部修饰符
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenBookkeeping: () -> Unit,
    onBudgetItemClick: (BudgetItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val monthlyOverview by viewModel.monthlyOverview.collectAsState()
    val balanceCheckResult by viewModel.balanceCheckResult.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.categoryName } }

    var showCreateMonthDialog by remember { mutableStateOf(false) }

    MonthPagerScaffold(
        availableMonths = availableMonths,
        currentYear = currentYear,
        currentMonth = currentMonth,
        pageTitle = "余粮",
        subtitle = "个人预算信封与智能日常记账系统",
        onMonthSelected = { year, month ->
            viewModel.selectMonth(year, month)
        },
        onAddMonthClick = {
            showCreateMonthDialog = true
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenBookkeeping,
                containerColor = MiuixBlue,
                contentColor = Color.White,
                shape = MiuixShapes.MediumSquircle
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "快速记账",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "记一笔",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dialogs = {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 本月核心资产总览大卡片（含上月结余滚存）
            item {
                MonthlyAssetOverviewCard(
                    currentYear = targetYear,
                    currentMonth = targetMonth,
                    monthlyOverview = monthlyOverview
                )
            }

            // 2. 资金池配平健康状态卡片（对应草稿页）
            item {
                CapitalBalanceCard(balanceCheckResult = balanceCheckResult)
            }

            // 3. 本月收入来源分类概览（看板专属展示）
            if (monthlyOverview.incomeOverviewList.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "本月收入来源",
                        actionSlot = {
                            Text(
                                text = "共 ${monthlyOverview.incomeOverviewList.size} 类别",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    )
                }

                items(monthlyOverview.incomeOverviewList, key = { it.categoryName }) { incomeOverview ->
                    IncomeEnvelopeCard(
                        incomeOverview = incomeOverview,
                        categoryDefinition = categoryMap[incomeOverview.categoryName]
                    )
                }
            }

            // 4. 分类支出预算信封列表标题
            item {
                SectionHeader(
                    title = "分类支出预算信封",
                    actionSlot = {
                        Text(
                            text = "共 ${monthlyOverview.categoryOverviewList.size} 大类",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                )
            }

            // 5. 各大类支出信封卡片
            items(monthlyOverview.categoryOverviewList, key = { it.categoryName }) { categoryOverview ->
                BudgetEnvelopeCard(
                    categoryOverview = categoryOverview,
                    categoryDefinition = categoryMap[categoryOverview.categoryName],
                    onBudgetItemClick = onBudgetItemClick
                )
            }
        }
    }
}
