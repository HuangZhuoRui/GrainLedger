package com.vincent.grainledger.ui.screens.bookkeeping.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.theme.horizontalFadingEdge
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记账 Step 2: 分类归属与预算信封/收入来源选择卡片 (BookkeepingCategoryStep)。
 *
 * 包含：
 * 1. 【支出模式】：大类滑轨、细项信封列表、实时预算消耗与赤字透视预测卡片；
 * 2. 【收入模式】：收入大类滑轨、款项名称输入、常用高频入账标签、多月份同步勾选卡片；
 * 3. 边缘羽化模糊平滑过渡 (horizontalFadingEdge) 与原地新建大类。
 */
@Composable
fun BookkeepingCategoryStep(
    isIncomeMode: Boolean,
    currentYear: Int,
    currentMonth: Int,
    availableMonths: List<Pair<Int, Int>>,
    selectedTargetMonths: Set<Pair<Int, Int>>,
    onTargetMonthsChange: (Set<Pair<Int, Int>>) -> Unit,
    incomeCategories: List<BudgetCategory>,
    expenseCategories: List<BudgetCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    currentCategoryExpenseItems: List<BudgetItem>,
    selectedDetail: String,
    onDetailSelected: (String) -> Unit,
    matchedBudgetItem: BudgetItem?,
    evaluatedAmount: Double,
    incomeDetailInput: String,
    onIncomeDetailChange: (String) -> Unit,
    activeThemeColor: Color,
    onOpenCreateCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 大类横向滑动胶囊与新建大类
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isIncomeMode) "选择收入类别" else "选择支出大类",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryListToUse = if (isIncomeMode) incomeCategories else expenseCategories
                categoryListToUse.forEach { cat ->
                    val isSelected = (cat.categoryName == selectedCategory)
                    val catColor = cat.themeColor

                    Box(
                        modifier = Modifier
                            .clip(MiuixShapes.SmallSquircle)
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.18f) else MiuixTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onCategorySelected(cat.categoryName)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(catColor, CircleShape)
                            )
                            Text(
                                text = cat.categoryName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catColor else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 原地新建分类
                Box(
                    modifier = Modifier
                        .clip(MiuixShapes.SmallSquircle)
                        .background(activeThemeColor.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenCreateCategory)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建分类",
                            tint = activeThemeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "新建分类",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeThemeColor
                        )
                    }
                }
            }
        }

        // 【支出】细项选择与实时预算透视卡片
        if (!isIncomeMode) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "选择预算细项信封",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                if (currentCategoryExpenseItems.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalFadingEdge(14.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentCategoryExpenseItems.forEach { item ->
                            val isSelected = (item.detailName == selectedDetail)
                            val itemBalance = item.balance

                            Box(
                                modifier = Modifier
                                    .clip(MiuixShapes.SmallSquircle)
                                    .background(
                                        if (isSelected) MiuixBlue.copy(alpha = 0.16f) else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    )
                                    .clickable {
                                        onDetailSelected(item.detailName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.detailName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "(余 ${MathFormulaEvaluator.formatAmount(itemBalance)} ¥)",
                                        fontSize = 10.5.sp,
                                        color = if (itemBalance < 0) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "当前大类暂无预算细项，记账将自动关联「日常支出」预算信封",
                            fontSize = 11.5.sp,
                            color = MiuixOrange
                        )
                    }
                }
            }

            // 实时预算消耗预测透视卡片
            if (matchedBudgetItem != null) {
                val currentAllocated = matchedBudgetItem.actualAllocated
                val currentSpent = matchedBudgetItem.actualSpent
                val currentBalance = matchedBudgetItem.balance
                val simulatedSpent = currentSpent + evaluatedAmount
                val simulatedBalance = currentAllocated - simulatedSpent

                val predictedProgress = if (currentAllocated > 0) (simulatedSpent / currentAllocated).toFloat() else if (simulatedSpent > 0) 1f else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = predictedProgress.coerceIn(0f, 1f),
                    animationSpec = MiuixAnimation.springFast(),
                    label = "预测进度动画"
                )

                val isOverbudget = simulatedBalance < 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isOverbudget) MiuixRed else MiuixBlue, CircleShape)
                                )
                                Text(
                                    text = "${matchedBudgetItem.detailName} 预算透视",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = if (isOverbudget) "⚠️ 消费后将赤字超支" else "额度充足",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverbudget) MiuixRed else MiuixGreen
                            )
                        }

                        // 进度条
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MiuixShapes.PillShape)
                                .background(MiuixTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(MiuixShapes.PillShape)
                                    .background(if (isOverbudget) MiuixRed else if (predictedProgress > 0.85f) MiuixOrange else MiuixGreen)
                            )
                        }

                        // 消费前后结余演变
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "当前结余 ${MathFormulaEvaluator.formatAmount(currentBalance)} ¥",
                                fontSize = 11.5.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )

                            Text(
                                text = "消费后 ➔ ${MathFormulaEvaluator.formatAmount(simulatedBalance)} ¥",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverbudget) MiuixRed else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 【收入】款项明细与同步月份
        if (isIncomeMode) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = incomeDetailInput,
                    onValueChange = onIncomeDetailChange,
                    label = { Text(text = "收入款项明细") },
                    placeholder = { Text(text = "例如：月度基本工资、年终奖金、外快") },
                    singleLine = true,
                    shape = MiuixShapes.MediumSquircle,
                    modifier = Modifier.fillMaxWidth()
                )

                // 常用收入标签
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalFadingEdge(14.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("工资薪金", "年终奖金", "兼职外快", "投资收益", "长辈红包", "理财分红", "二手出物", "报销入账").forEach { tag ->
                        val isSelected = (incomeDetailInput == tag)
                        Box(
                            modifier = Modifier
                                .clip(MiuixShapes.PillShape)
                                .background(
                                    if (isSelected) MiuixGreen.copy(alpha = 0.16f) else MiuixTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onIncomeDetailChange(tag) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MiuixGreen else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 同步月份卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "同步入账月份 (已选 ${selectedTargetMonths.size} 个月)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(
                                    onClick = {
                                        onTargetMonthsChange(setOf(Pair(currentYear, currentMonth)))
                                    }
                                ) {
                                    Text(text = "仅当月", fontSize = 11.sp, color = MiuixGreen)
                                }
                                TextButton(
                                    onClick = {
                                        val newSelection = if (selectedTargetMonths.size == availableMonths.size) {
                                            setOf(Pair(currentYear, currentMonth))
                                        } else {
                                            availableMonths.toSet()
                                        }
                                        onTargetMonthsChange(newSelection)
                                    }
                                ) {
                                    Text(
                                        text = if (selectedTargetMonths.size == availableMonths.size) "取消全选" else "全选",
                                        fontSize = 11.sp,
                                        color = MiuixGreen
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalFadingEdge(14.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableMonths.forEach { (mYear, mMonth) ->
                                val monthPair = Pair(mYear, mMonth)
                                val isSelected = selectedTargetMonths.contains(monthPair)
                                val isCurrent = (mYear == currentYear && mMonth == currentMonth)

                                Box(
                                    modifier = Modifier
                                        .clip(MiuixShapes.SmallSquircle)
                                        .background(
                                            if (isSelected) MiuixGreen.copy(alpha = 0.18f) else MiuixTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            val newSet = if (isSelected) {
                                                if (selectedTargetMonths.size > 1) selectedTargetMonths - monthPair else selectedTargetMonths
                                            } else {
                                                selectedTargetMonths + monthPair
                                            }
                                            onTargetMonthsChange(newSet)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "${mYear}年${mMonth}月${if (isCurrent) " (当月)" else ""}",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MiuixGreen else MiuixTheme.colorScheme.onSurface
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
