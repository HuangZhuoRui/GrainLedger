package com.vincent.grainledger.ui.screens.bookkeeping

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.screens.category.QuickCreateCategoryDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.DateUtils
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * MIUIX / HyperOS 风格极简记一笔弹窗 (BookkeepingDialog)。
 *
 * 核心优化：
 * 1. 极简优雅头部：移除顶部关闭按钮与抽拉指示条；
 * 2. 支出 / 收入 物理弹簧阻尼分段切换胶囊；
 * 3. Hero 级超大金额输入区，支持动态算式实时评估与极简算术微工具条（+、-、×、÷、C、⌫）；
 * 4. 独创“实时预算消耗预测透视卡片”，输入金额时即时演算消费后结余与动态进度条推演；
 * 5. 视觉化大类气泡（支持原地一键新建分类与自动关联当月预算信封）与内嵌实时结余的细项胶囊；
 * 6. 收入模式快捷明细标签与多月份同步选择胶囊；
 * 7. 智能记账日期选择：支持当月 1~31 号全量自选 + 今天/昨天/前天 快速切换；
 * 8. 出资/收款账户支持自由自定义输入 + 常用快捷预设点选；
 * 9. 底部全宽沉浸式动态确认按钮。
 */
@Composable
fun BookkeepingDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val budgetItemList by viewModel.currentBudgetItems.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var isIncomeMode by remember { mutableStateOf(false) }
    var selectedTargetMonths by remember(currentYear, currentMonth) {
        mutableStateOf(setOf(Pair(currentYear, currentMonth)))
    }

    val maxDaysInMonth = remember(currentYear, currentMonth) {
        DateUtils.getDaysInMonth(currentYear, currentMonth)
    }
    val defaultToday = remember(currentYear, currentMonth) {
        DateUtils.getCurrentDay().coerceIn(1, maxDaysInMonth)
    }
    var transactionDay by remember(defaultToday) {
        mutableIntStateOf(defaultToday)
    }

    val expenseCategories = remember(allCategories) { allCategories.filter { !it.isIncome } }
    val incomeCategories = remember(allCategories) { allCategories.filter { it.isIncome } }

    var selectedCategory by remember(allCategories, isIncomeMode) {
        mutableStateOf(
            if (isIncomeMode) {
                incomeCategories.firstOrNull()?.categoryName ?: "工资薪金"
            } else {
                expenseCategories.firstOrNull()?.categoryName ?: "强制类"
            }
        )
    }
    var selectedDetail by remember { mutableStateOf("") }
    var incomeDetailInput by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("") }
    var funder by remember { mutableStateOf("微信零钱") }
    var remarkText by remember { mutableStateOf("") }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    val currentCategoryExpenseItems = remember(selectedCategory, budgetItemList, isIncomeMode) {
        if (!isIncomeMode) {
            budgetItemList.filter { it.categoryName == selectedCategory }
        } else {
            emptyList()
        }
    }

    val matchedBudgetItem = remember(selectedCategory, selectedDetail, currentCategoryExpenseItems) {
        currentCategoryExpenseItems.find { it.detailName == selectedDetail } ?: currentCategoryExpenseItems.firstOrNull()
    }

    if (!isIncomeMode && selectedDetail.isEmpty() && currentCategoryExpenseItems.isNotEmpty()) {
        selectedDetail = currentCategoryExpenseItems.first().detailName
    }

    val evaluatedAmount = remember(amountInputText) {
        MathFormulaEvaluator.evaluate(amountInputText)
    }
    val hasFormula = remember(amountInputText) {
        amountInputText.contains("+") || amountInputText.contains("-") ||
                amountInputText.contains("*") || amountInputText.contains("/")
    }

    val activeThemeColor = if (isIncomeMode) MiuixGreen else MiuixRed

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp)
                .clip(MiuixShapes.DialogSquircle)
                .background(MiuixTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncomeMode) "记一笔收入" else "记一笔支出",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MiuixShapes.PillShape)
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.PillShape)
                            .background(
                                if (!isIncomeMode) MiuixRed.copy(alpha = 0.14f) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isIncomeMode = false
                                val firstExpCat = expenseCategories.firstOrNull()?.categoryName
                                if (firstExpCat != null) {
                                    selectedCategory = firstExpCat
                                    selectedDetail = ""
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!isIncomeMode) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "支出消费",
                                fontSize = 13.5.sp,
                                fontWeight = if (!isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isIncomeMode) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.PillShape)
                            .background(
                                if (isIncomeMode) MiuixGreen.copy(alpha = 0.16f) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isIncomeMode = true
                                val firstIncCat = incomeCategories.firstOrNull()?.categoryName
                                if (firstIncCat != null) {
                                    selectedCategory = firstIncCat
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "收入入账",
                                fontSize = 13.5.sp,
                                fontWeight = if (isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
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
                            Text(
                                text = if (isIncomeMode) "入账金额" else "支出金额",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )

                            if (hasFormula) {
                                Box(
                                    modifier = Modifier
                                        .clip(MiuixShapes.SmallSquircle)
                                        .background(activeThemeColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "= ${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeThemeColor
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (amountInputText.isEmpty()) {
                                    Text(
                                        text = "0.00",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.35f)
                                    )
                                }
                                BasicTextField(
                                    value = amountInputText,
                                    onValueChange = { amountInputText = it },
                                    textStyle = TextStyle(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeThemeColor
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    cursorBrush = SolidColor(activeThemeColor),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Text(
                                text = " ¥",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeColor,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("+", "-", "*", "/").forEach { op ->
                                val displayOp = when (op) {
                                    "*" -> "×"
                                    "/" -> "÷"
                                    else -> op
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(MiuixShapes.SmallSquircle)
                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            if (amountInputText.isNotEmpty() && !amountInputText.endsWith("+") &&
                                                !amountInputText.endsWith("-") && !amountInputText.endsWith("*") && !amountInputText.endsWith("/")
                                            ) {
                                                amountInputText += op
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayOp,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(MiuixShapes.SmallSquircle)
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                                    .clickable { amountInputText = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "C",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(MiuixShapes.SmallSquircle)
                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        if (amountInputText.isNotEmpty()) {
                                            amountInputText = amountInputText.dropLast(1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "退格",
                                    tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (!isIncomeMode && matchedBudgetItem != null) {
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

                if (isIncomeMode) {
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
                                Text(
                                    text = "同步入账月份 (已选 ${selectedTargetMonths.size} 个月)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            selectedTargetMonths = setOf(Pair(currentYear, currentMonth))
                                        }
                                    ) {
                                        Text(text = "仅当月", fontSize = 11.5.sp, color = MiuixGreen)
                                    }
                                    TextButton(
                                        onClick = {
                                            selectedTargetMonths = if (selectedTargetMonths.size == availableMonths.size) {
                                                setOf(Pair(currentYear, currentMonth))
                                            } else {
                                                availableMonths.toSet()
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = if (selectedTargetMonths.size == availableMonths.size) "取消全选" else "全选",
                                            fontSize = 11.5.sp,
                                            color = MiuixGreen
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                                selectedTargetMonths = if (isSelected) {
                                                    if (selectedTargetMonths.size > 1) {
                                                        selectedTargetMonths - monthPair
                                                    } else {
                                                        selectedTargetMonths
                                                    }
                                                } else {
                                                    selectedTargetMonths + monthPair
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${mYear}年${mMonth}月${if (isCurrent) " (当月)" else ""}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MiuixGreen else MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isIncomeMode) "选择收入类别" else "选择支出类别与预算信封",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                        selectedCategory = cat.categoryName
                                        if (!isIncomeMode) {
                                            val firstDetail = budgetItemList.firstOrNull { it.categoryName == cat.categoryName }?.detailName ?: ""
                                            selectedDetail = firstDetail
                                        }
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

                        Box(
                            modifier = Modifier
                                .clip(MiuixShapes.SmallSquircle)
                                .background(activeThemeColor.copy(alpha = 0.12f))
                                .clickable { showCreateCategoryDialog = true }
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

                    if (!isIncomeMode) {
                        if (currentCategoryExpenseItems.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
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
                                                selectedDetail = item.detailName
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
                                    .padding(top = 2.dp)
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

                    if (isIncomeMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = incomeDetailInput,
                                onValueChange = { incomeDetailInput = it },
                                label = { Text(text = "收入款项明细 (可输入或点选)") },
                                placeholder = { Text(text = "例如：月度基本工资、年终奖金、外快") },
                                singleLine = true,
                                shape = MiuixShapes.MediumSquircle,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            .clickable { incomeDetailInput = tag }
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
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "记账日期",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            Text(
                                text = "${currentYear}年${currentMonth}月${transactionDay}日",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val today = DateUtils.getCurrentDay().coerceIn(1, maxDaysInMonth)
                            val yesterday = (today - 1).coerceAtLeast(1)
                            val beforeYesterday = (today - 2).coerceAtLeast(1)

                            listOf(
                                Pair("今天 (${today}日)", today),
                                Pair("昨天 (${yesterday}日)", yesterday),
                                Pair("前天 (${beforeYesterday}日)", beforeYesterday)
                            ).forEach { (label, day) ->
                                val isSelected = (transactionDay == day)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(MiuixShapes.SmallSquircle)
                                        .background(
                                            if (isSelected) activeThemeColor.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { transactionDay = day }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) activeThemeColor else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..maxDaysInMonth).forEach { day ->
                                val isSelected = (transactionDay == day)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) activeThemeColor else MiuixTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { transactionDay = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$day",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = funder,
                            onValueChange = { funder = it },
                            label = { Text(text = if (isIncomeMode) "收款账户" else "出资账户") },
                            placeholder = { Text("例如：微信零钱、招商银行卡") },
                            singleLine = true,
                            shape = MiuixShapes.MediumSquircle,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("微信零钱", "支付宝", "招商银行卡", "现金", "建设银行", "工商银行", "京东白条", "花呗").forEach { acc ->
                                val isSelected = (funder == acc)
                                Box(
                                    modifier = Modifier
                                        .clip(MiuixShapes.PillShape)
                                        .background(
                                            if (isSelected) MiuixBlue.copy(alpha = 0.16f) else MiuixTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { funder = acc }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = acc,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = remarkText,
                        onValueChange = { remarkText = it },
                        placeholder = { Text("添加说明备注 (选填)...", fontSize = 12.5.sp) },
                        singleLine = true,
                        shape = MiuixShapes.MediumSquircle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val actionAmountString = if (evaluatedAmount > 0.0) "${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥" else ""
                val buttonText = if (isIncomeMode) {
                    if (selectedTargetMonths.size > 1) {
                        "确认入账 $actionAmountString（同步 ${selectedTargetMonths.size} 个月）"
                    } else {
                        "确认入账 $actionAmountString"
                    }
                } else {
                    "记一笔支出 $actionAmountString"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (evaluatedAmount > 0.0) {
                                val finalDetail = if (isIncomeMode) {
                                    if (incomeDetailInput.isNotBlank()) incomeDetailInput.trim() else "日常入账"
                                } else {
                                    if (selectedDetail.isNotEmpty()) selectedDetail else "日常支出"
                                }

                                if (isIncomeMode) {
                                    val targetMonthsList = selectedTargetMonths.toList()
                                    viewModel.recordTransactionsMultiMonths(
                                        targetMonths = targetMonthsList,
                                        day = transactionDay,
                                        categoryName = selectedCategory,
                                        detailName = finalDetail,
                                        amount = evaluatedAmount,
                                        funder = funder,
                                        remark = remarkText
                                    )
                                } else {
                                    viewModel.recordTransaction(
                                        year = currentYear,
                                        month = currentMonth,
                                        day = transactionDay,
                                        categoryName = selectedCategory,
                                        detailName = finalDetail,
                                        amount = -evaluatedAmount,
                                        funder = funder,
                                        remark = remarkText
                                    )
                                }
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = if (evaluatedAmount > 0.0) activeThemeColor else MiuixTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = buttonText,
                            color = if (evaluatedAmount > 0.0) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showCreateCategoryDialog) {
                QuickCreateCategoryDialog(
                    isIncomeCategory = isIncomeMode,
                    currentYear = currentYear,
                    currentMonth = currentMonth,
                    onSaveCategoryWithBudget = { newCat, initialDetail, initialAmount ->
                        viewModel.saveCategory(newCat)
                        selectedCategory = newCat.categoryName
                        if (isIncomeMode) {
                            if (!initialDetail.isNullOrBlank()) {
                                incomeDetailInput = initialDetail
                            }
                        } else {
                            val detailName = if (!initialDetail.isNullOrBlank()) initialDetail else "日常支出"
                            val budgetAmount = initialAmount ?: 0.0
                            val newBudgetItem = BudgetItem(
                                itemId = 0L,
                                year = currentYear,
                                month = currentMonth,
                                categoryName = newCat.categoryName,
                                detailName = detailName,
                                unitPrice = budgetAmount,
                                quantity = 1.0,
                                totalPrice = budgetAmount,
                                actualAllocated = budgetAmount,
                                funder = funder,
                                actualSpent = 0.0,
                                balance = budgetAmount,
                                remark = "记账时新建大类并关联"
                            )
                            viewModel.saveBudgetItem(newBudgetItem)
                            selectedDetail = detailName
                        }
                        showCreateCategoryDialog = false
                    },
                    onDismissRequest = { showCreateCategoryDialog = false }
                )
            }
        }
    }
}
