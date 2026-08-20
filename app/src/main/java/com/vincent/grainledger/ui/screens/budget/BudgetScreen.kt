package com.vincent.grainledger.ui.screens.budget

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import com.vincent.grainledger.ui.components.MiuixMonthSelector
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算管理与规划页面。
 *
 * 对应 Excel 表格中的《数据源》全部行记录，按月份与大类结构化展示每一个预算细项的
 * 单价、数量、总价预算、实际加入资金、实际消费及当前结余，支持可视化新增、修改与删除。
 *
 * @param viewModel 全局视图模型
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "预算管理",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "规划每月开支细项与资金注入额度",
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

            // 3. 统计汇总头部卡片
            item {
                val totalBudget = budgetItemList.sumOf { it.totalPrice }
                val totalAllocated = budgetItemList.sumOf { it.actualAllocated }
                val totalSpent = budgetItemList.sumOf { it.actualSpent }
                val totalBalance = totalAllocated - totalSpent

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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "规划总预算",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            Text(
                                text = "¥${MathFormulaEvaluator.formatAmount(totalBudget)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            Text(
                                text = "实际加入(注入)",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            Text(
                                text = "¥${MathFormulaEvaluator.formatAmount(totalAllocated)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixBlue
                            )
                        }

                        Column {
                            Text(
                                text = "剩余总结余",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            Text(
                                text = "¥${MathFormulaEvaluator.formatAmount(totalBalance)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalBalance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 4. 按类别列出各项预算
            if (categoryGroupMap.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "本月暂无预算规划，点击右下角按钮添加",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            } else {
                categoryGroupMap.forEach { (categoryName, itemList) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 18.dp, end = 18.dp, top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = categoryName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            val categoryAllocatedTotal = itemList.sumOf { it.actualAllocated }
                            Text(
                                text = "注入合计: ¥${MathFormulaEvaluator.formatAmount(categoryAllocatedTotal)}",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }

                    items(itemList, key = { it.itemId }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingBudgetItem = item
                                        showEditDialog = true
                                    },
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.detailName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MiuixTheme.colorScheme.onSurface
                                            )
                                            if (item.funder != "默认账户" && item.funder.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            MiuixTheme.colorScheme.surfaceVariant,
                                                            MiuixShapes.SmallSquircle
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = item.funder,
                                                        fontSize = 10.sp,
                                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = if (item.quantity > 1.0) {
                                                "单价 ¥${MathFormulaEvaluator.formatAmount(item.unitPrice)} × ${item.quantity} = 预算 ¥${MathFormulaEvaluator.formatAmount(item.totalPrice)}"
                                            } else {
                                                "总额预算 ¥${MathFormulaEvaluator.formatAmount(item.totalPrice)}"
                                            },
                                            fontSize = 12.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "剩余 ¥${MathFormulaEvaluator.formatAmount(item.balance)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.balance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "已花 ¥${MathFormulaEvaluator.formatAmount(item.actualSpent)} / 加入 ¥${MathFormulaEvaluator.formatAmount(item.actualAllocated)}",
                                                fontSize = 11.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "编辑",
                                            tint = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 新增预算项悬浮按钮
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
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = "添加预算", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // 预算编辑弹窗
    if (showEditDialog) {
        EditBudgetItemDialog(
            targetItem = editingBudgetItem,
            year = currentYear,
            month = currentMonth,
            categoryList = allCategories,
            onSave = { newItem ->
                viewModel.saveBudgetItem(newItem)
            },
            onDelete = { itemId ->
                viewModel.deleteBudgetItem(itemId)
            },
            onDismissRequest = {
                showEditDialog = false
            }
        )
    }
}
