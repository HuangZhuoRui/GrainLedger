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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.components.card.BudgetEnvelopeCard
import com.vincent.grainledger.ui.components.card.CapitalBalanceCard
import com.vincent.grainledger.ui.components.control.MiuixMonthSelector
import com.vincent.grainledger.ui.components.layout.PageHeader
import com.vincent.grainledger.ui.components.layout.SectionHeader
import com.vincent.grainledger.ui.screens.dashboard.components.MonthlyAssetOverviewCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 综合看板主页面 (DashboardScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，全响应式展示月度核心资产总览卡片、
 * 月份切换胶囊、资金池配平健康状态卡片以及各大类预算信封列表。
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
    val categoryMap = allCategories.associateBy { it.categoryName }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部标题栏
            item {
                PageHeader(
                    title = "余粮",
                    subtitle = "个人预算信封与智能日常记账系统"
                )
            }

            // 2. 月份快速切换条
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

            // 3. 本月核心资产总览大卡片
            item {
                MonthlyAssetOverviewCard(
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    monthlyOverview = monthlyOverview
                )
            }

            // 4. 资金池配平健康状态卡片（对应草稿页）
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CapitalBalanceCard(balanceCheckResult = balanceCheckResult)
                }
            }

            // 5. 大类预算信封列表标题
            item {
                SectionHeader(
                    title = "分类预算信封",
                    actionSlot = {
                        Text(
                            text = "共 ${monthlyOverview.categoryOverviewList.size} 大类",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                )
            }

            // 6. 各大类信封卡片
            items(monthlyOverview.categoryOverviewList, key = { it.categoryName }) { categoryOverview ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BudgetEnvelopeCard(
                        categoryOverview = categoryOverview,
                        categoryDefinition = categoryMap[categoryOverview.categoryName],
                        onBudgetItemClick = onBudgetItemClick
                    )
                }
            }
        }

        // 悬浮快速记账按钮 (Floating Action Button)
        FloatingActionButton(
            onClick = onOpenBookkeeping,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp),
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
    }
}
