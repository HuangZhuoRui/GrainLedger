package com.vincent.grainledger.ui.screens.bookkeeping

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue

/**
 * MIUIX / HyperOS 风格卡片式横向滑动分步记一笔弹窗 (BookkeepingDialog)。
 *
 * 核心创新：
 * 1. 采用 HorizontalPager 分步卡片流，支持手势 1:1 左右平滑滑动与按键导航；
 * 2. 具备与月份切换一致的物理弹簧阻尼、缩放 (0.92x~1.0x) 与渐变透明度 (0.5~1.0) 动效；
 * 3. 顶部步骤指示轴与动态标题/副标题随滑动联动；
 * 4. 三大卡片专注拆分：
 *    - Step 1: 金额与实时算式评估
 *    - Step 2: 归属大类、预算信封透视（支出）或同步月份（收入），支持原地新建大类
 *    - Step 3: 记账日期（全量自选）、出资扣款账户与说明备注
 */
@Composable
fun BookkeepingDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
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

    // 3 步 Pager 状态
    val pagerState = rememberPagerState(initialPage = 0) { 3 }

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
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ==================== 1. 顶部步骤指示轴与动态标题 ====================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 步骤指示胶囊条 (1 / 2 / 3)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1 金额", "2 分类", "3 信息").forEachIndexed { index, title ->
                            val isCurrent = (pagerState.currentPage == index)
                            val isCompleted = (pagerState.currentPage > index)

                            Box(
                                modifier = Modifier
                                    .clip(MiuixShapes.PillShape)
                                    .background(
                                        when {
                                            isCurrent -> activeThemeColor
                                            isCompleted -> activeThemeColor.copy(alpha = 0.25f)
                                            else -> MiuixTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index, animationSpec = MiuixAnimation.springSmooth())
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isCompleted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = activeThemeColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isCurrent -> Color.White
                                            isCompleted -> activeThemeColor
                                            else -> MiuixTheme.colorScheme.onSurfaceSecondary
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 步骤主副标题 (动画切换)
                    val (stepTitle, stepSubtitle) = when (pagerState.currentPage) {
                        0 -> Pair(
                            if (isIncomeMode) "记一笔收入 · 设定金额" else "记一笔支出 · 设定金额",
                            "输入本次款项金额，支持即时算式计算"
                        )
                        1 -> Pair(
                            if (isIncomeMode) "归属分类 · 收入来源" else "归属分类 · 预算信封透视",
                            if (isIncomeMode) "选择收入类别与同步月份" else "选择支出大类，查看预算实时消耗与结余演变"
                        )
                        else -> Pair(
                            "记账属性 · 确认入账",
                            "选择记账日期、出资账户与备注说明"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = stepTitle,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "TitleAnimation"
                        ) { targetTitle ->
                            Text(
                                text = targetTitle,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = stepSubtitle,
                            fontSize = 11.5.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                // ==================== 2. 中部 HorizontalPager (物理阻尼与缩放) ====================
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val scale = 1f - (pageOffset * 0.08f).coerceIn(0f, 0.08f)
                    val alpha = 1f - (pageOffset * 0.45f).coerceIn(0f, 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        when (page) {
                            // ==================== Step 1: 金额与算式 ====================
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 支出 / 收入 物理弹簧分段胶囊
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MiuixShapes.PillShape)
                                            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 支出 Tab
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

                                        // 收入 Tab
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

                                    // Hero 金额输入卡片
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

                                            // 大字号输入框与货币符号
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

                                            // 快捷算术微工具条 (+, -, ×, ÷, C, ⌫)
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

                                                // 清空 C
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

                                                // 退格 ⌫
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
                                }
                            }

                            // ==================== Step 2: 分类与预算/来源 ====================
                            1 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
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

                                            // 原地新建分类
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
                                                onValueChange = { incomeDetailInput = it },
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
                                                                    selectedTargetMonths = setOf(Pair(currentYear, currentMonth))
                                                                }
                                                            ) {
                                                                Text(text = "仅当月", fontSize = 11.sp, color = MiuixGreen)
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
                                                                    fontSize = 11.sp,
                                                                    color = MiuixGreen
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

                            // ==================== Step 3: 日期、出资账户与备注 ====================
                            2 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. 记账日期自选
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

                                        // 今天/昨天/前天 快速定位
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

                                        // 当月 1 ~ maxDaysInMonth 全量自选滑轨
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

                                    // 2. 出资/收款账户
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedTextField(
                                            value = funder,
                                            onValueChange = { funder = it },
                                            label = { Text(text = if (isIncomeMode) "收款账户" else "出资扣款账户") },
                                            placeholder = { Text("例如：微信零钱、招商银行卡、美团月付") },
                                            singleLine = true,
                                            shape = MiuixShapes.MediumSquircle,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // 常用账户快捷胶囊
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

                                    // 3. 说明备注
                                    OutlinedTextField(
                                        value = remarkText,
                                        onValueChange = { remarkText = it },
                                        placeholder = { Text("添加说明备注 (选填)...", fontSize = 12.5.sp) },
                                        singleLine = true,
                                        shape = MiuixShapes.MediumSquircle,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // 4. 实时记账总览小结卡片
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${selectedCategory} · ${if (isIncomeMode) incomeDetailInput.ifBlank { "日常入账" } else selectedDetail.ifBlank { "日常支出" }}",
                                                fontSize = 12.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                                            )

                                            Text(
                                                text = "${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = activeThemeColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 3. 底部智能导航栏 ====================
                val actionAmountString = if (evaluatedAmount > 0.0) "${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥" else ""
                val isFinalStep = (pagerState.currentPage == 2)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pagerState.currentPage > 0) {
                        // 上一步按钮
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = MiuixAnimation.springSmooth())
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(text = "上一步", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    } else {
                        // 第 1 步时为取消按钮
                        Button(
                            onClick = onDismissRequest,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(text = "取消", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }

                    if (!isFinalStep) {
                        // 下一步按钮
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = MiuixAnimation.springSmooth())
                                }
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(color = activeThemeColor)
                        ) {
                            Text(
                                text = "下一步 ➔",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // 最终提交按钮
                        val submitButtonText = if (isIncomeMode) {
                            if (selectedTargetMonths.size > 1) {
                                "确认入账 $actionAmountString（同步 ${selectedTargetMonths.size} 个月）"
                            } else {
                                "确认入账 $actionAmountString"
                            }
                        } else {
                            "记一笔支出 $actionAmountString"
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
                                text = submitButtonText,
                                color = if (evaluatedAmount > 0.0) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 原地新建分类弹窗
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

