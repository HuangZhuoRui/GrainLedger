package com.vincent.grainledger.ui.screens.budget

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
import com.vincent.grainledger.ui.components.control.MiuixMonthSelector
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.PageHeader
import com.vincent.grainledger.ui.screens.budget.components.BudgetCategoryGroup
import com.vincent.grainledger.ui.screens.budget.components.BudgetSummaryCard
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 预算管理与规划页面 (BudgetScreen)。
 *
 * 遵循单一数据源 (SSOT) 原则，全响应式展示当前月份的预算细项、
 * 按大类聚合分组卡片与统计汇总，支持可视化新增、编辑与删除。
 *
 * @param viewModel 全局视图模型
 * @param modifier 外部修饰符
 */
@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val budgetItemList by viewModel.currentBudgetItems.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.categoryName } }

    var editingBudgetItem by remember { mutableStateOf<BudgetItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    // 按大类对预算项进行分组
    val categoryGroupMap = remember(budgetItemList) {
        budgetItemList.groupBy { it.categoryName }
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
                    title = "预算管理",
                    subtitle = "规划每月开支细项与资金注入额度"
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
                    }
                )
            }

            // 3. 统计汇总头部卡片
            item {
                BudgetSummaryCard(budgetItemList = budgetItemList)
            }

            // 4. 大类分组预算列表
            if (budgetItemList.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "本月暂无预算规划",
                        message = "点击右下角按钮添加第一个预算细项吧"
                    )
                }
            } else {
                items(categoryGroupMap.keys.toList(), key = { it }) { categoryName ->
                    val items = categoryGroupMap[categoryName] ?: emptyList()
                    BudgetCategoryGroup(
                        categoryName = categoryName,
                        categoryDefinition = categoryMap[categoryName],
                        items = items,
                        onEditItem = { item ->
                            editingBudgetItem = item
                            showEditDialog = true
                        }
                    )
                }
            }
        }

        // 悬浮新增预算细项按钮
        FloatingActionButton(
            onClick = {
                editingBudgetItem = null
                showEditDialog = true
            },
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
                    contentDescription = "新增预算",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "加预算",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // 预算编辑/新增弹窗
    if (showEditDialog) {
        EditBudgetItemDialog(
            targetItem = editingBudgetItem,
            year = currentYear,
            month = currentMonth,
            categoryList = allCategories,
            onSave = { savedItem ->
                viewModel.saveBudgetItem(savedItem)
                showEditDialog = false
            },
            onDelete = { deletedItem ->
                viewModel.deleteBudgetItem(deletedItem)
                showEditDialog = false
            },
            onDismissRequest = {
                showEditDialog = false
            }
        )
    }
}
