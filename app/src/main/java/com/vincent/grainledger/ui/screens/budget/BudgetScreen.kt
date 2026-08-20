package com.vincent.grainledger.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.TextButton
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
import com.vincent.grainledger.ui.components.feedback.EmptyStateView
import com.vincent.grainledger.ui.components.layout.MonthPagerScaffold
import com.vincent.grainledger.ui.screens.budget.components.BudgetCategoryGroup
import com.vincent.grainledger.ui.screens.budget.components.BudgetSummaryCard
import com.vincent.grainledger.ui.screens.category.CategoryManagementDialog
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 预算管理与规划页面 (BudgetScreen)。
 *
 * 基于 MonthPagerScaffold 通用脚手架构建：
 * 顶部展示居中渐变缩放月份进度轴与“分类管理”入口，下方大卡片承载支出预算汇总、
 * 各支出大类分组信封及细项列表。
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

    // 仅保留支出类预算项
    val expenseBudgetItems = remember(budgetItemList, categoryMap) {
        budgetItemList.filter { categoryMap[it.categoryName]?.isIncome != true }
    }

    var editingBudgetItem by remember { mutableStateOf<BudgetItem?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCreateMonthDialog by remember { mutableStateOf(false) }
    var showCategoryManagementDialog by remember { mutableStateOf(false) }

    // 按大类对预算项进行分组
    val categoryGroupMap = remember(expenseBudgetItems) {
        expenseBudgetItems.groupBy { it.categoryName }
    }

    MonthPagerScaffold(
        availableMonths = availableMonths,
        currentYear = currentYear,
        currentMonth = currentMonth,
        pageTitle = "预算规划",
        subtitle = "规划每月支出预算细项与资金注入额度",
        headerActionSlot = {
            TextButton(
                onClick = { showCategoryManagementDialog = true }
            ) {
                Text(
                    text = "分类管理",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixBlue
                )
            }
        },
        onMonthSelected = { year, month ->
            viewModel.selectMonth(year, month)
        },
        onAddMonthClick = {
            showCreateMonthDialog = true
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingBudgetItem = null
                    showEditDialog = true
                },
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
        },
        dialogs = {
            // 预算编辑/新增弹窗（仅展示支出大类）
            if (showEditDialog) {
                val expenseCategories = allCategories.filter { !it.isIncome }
                EditBudgetItemDialog(
                    targetItem = editingBudgetItem,
                    year = currentYear,
                    month = currentMonth,
                    categoryList = expenseCategories,
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

            // 分类全生命周期管理弹窗（包含支出类与收入类管理）
            if (showCategoryManagementDialog) {
                CategoryManagementDialog(
                    categoryList = allCategories,
                    onSaveCategory = { newCategory, oldName ->
                        viewModel.saveCategory(newCategory, oldName)
                    },
                    onDeleteCategory = { categoryToDelete, deleteAssociated ->
                        viewModel.deleteCategory(categoryToDelete, deleteAssociated)
                    },
                    onDismissRequest = {
                        showCategoryManagementDialog = false
                    }
                )
            }
        }
    ) { _, _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 统计汇总头部卡片
            item {
                BudgetSummaryCard(budgetItemList = expenseBudgetItems)
            }

            // 2. 大类分组预算列表
            if (expenseBudgetItems.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "本月暂无预算规划",
                        message = "点击右下角按钮添加第一个支出预算细项吧"
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
    }
}
