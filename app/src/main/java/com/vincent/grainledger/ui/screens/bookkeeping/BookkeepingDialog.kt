package com.vincent.grainledger.ui.screens.bookkeeping

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.screens.category.QuickCreateCategoryDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.theme.horizontalFadingEdge
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
 * MIUIX / HyperOS 风格卡片式横向滑动分步记一笔与修改流水弹窗 (BookkeepingDialog)。
 *
 * 核心创新与体验升级：
 * 1. 采用 HorizontalPager 分步卡片流，支持手势 1:1 左右平滑滑动与按键导航；
 * 2. 具备与月份切换一致的物理弹簧阻尼、缩放 (0.92x~1.0x) 与渐变透明度 (0.5~1.0) 动效；
 * 3. 顶部步骤指示轴与标题采用同向左右横向滑动与物理缩放 (Slide + Scale + Fade) 联动过渡；
 * 4. 三大步骤卡片高度统一固定为 370.dp，左右切换坚实无跳变；
 * 5. 所有横向滑动区域（分类、标签、月份、日期、账户）边缘增加平滑渐变羽化模糊过渡 (horizontalFadingEdge)；
 * 6. 记账日期 1~31 号滑轨智能居中定位当日或已选日期；
 * 7. 支持传入 [editingRecord]，实现单笔收入/支出流水的直接修改与二次确认删除。
 */
@Composable
fun BookkeepingDialog(
    viewModel: MainViewModel,
    editingRecord: TransactionRecord? = null,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val budgetItemList by viewModel.currentBudgetItems.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var isIncomeMode by remember(editingRecord) {
        mutableStateOf(editingRecord?.let { !it.isExpense } ?: false)
    }
    var selectedTargetMonths by remember(currentYear, currentMonth, editingRecord) {
        mutableStateOf(
            if (editingRecord != null) setOf(Pair(editingRecord.year, editingRecord.month))
            else setOf(Pair(currentYear, currentMonth))
        )
    }

    val maxDaysInMonth = remember(currentYear, currentMonth) {
        DateUtils.getDaysInMonth(currentYear, currentMonth)
    }
    val defaultToday = remember(currentYear, currentMonth, editingRecord) {
        editingRecord?.day ?: DateUtils.getCurrentDay().coerceIn(1, maxDaysInMonth)
    }
    var transactionDay by remember(defaultToday) {
        mutableIntStateOf(defaultToday)
    }

    // 记账日期居中滑轨 State
    val dateListState = rememberLazyListState()

    // 当日期改变或弹窗展示时，平滑自动居中定位到选中日期
    LaunchedEffect(transactionDay, maxDaysInMonth) {
        val itemWidthPx = with(density) { 40.dp.toPx() } // 34dp item + 6dp spacing
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val dialogWidthPx = screenWidthPx * 0.94f - with(density) { 36.dp.toPx() }
        val halfContainerPx = dialogWidthPx / 2f
        val centerOffsetPx = (halfContainerPx - (itemWidthPx / 2f)).toInt()

        val targetIndex = (transactionDay - 1).coerceIn(0, maxDaysInMonth - 1)
        dateListState.animateScrollToItem(
            index = targetIndex,
            scrollOffset = -centerOffsetPx
        )
    }

    val expenseCategories = remember(allCategories) { allCategories.filter { !it.isIncome } }
    val incomeCategories = remember(allCategories) { allCategories.filter { it.isIncome } }

    var selectedCategory by remember(allCategories, isIncomeMode, editingRecord) {
        mutableStateOf(
            editingRecord?.categoryName ?: if (isIncomeMode) {
                incomeCategories.firstOrNull()?.categoryName ?: "工资薪金"
            } else {
                expenseCategories.firstOrNull()?.categoryName ?: "强制类"
            }
        )
    }
    var selectedDetail by remember(editingRecord) {
        mutableStateOf(editingRecord?.let { if (it.isExpense) it.detailName else "" } ?: "")
    }
    var incomeDetailInput by remember(editingRecord) {
        mutableStateOf(editingRecord?.let { if (!it.isExpense) it.detailName else "" } ?: "")
    }
    var amountInputText by remember(editingRecord) {
        mutableStateOf(
            if (editingRecord != null) {
                if (editingRecord.absoluteAmount % 1.0 == 0.0) editingRecord.absoluteAmount.toLong().toString()
                else String.format(java.util.Locale.US, "%.2f", editingRecord.absoluteAmount)
            } else ""
        )
    }
    var funder by remember(editingRecord) {
        mutableStateOf(editingRecord?.funder ?: "微信零钱")
    }
    var remarkText by remember(editingRecord) {
        mutableStateOf(editingRecord?.remark ?: "")
    }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                // ==================== 1. 顶部步骤指示轴与动态标题（物理缩放过渡） ====================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 步骤指示胶囊条 (1 / 2 / 3) 带物理弹性缩放与平滑过渡
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1 金额", "2 分类", "3 信息").forEachIndexed { index, title ->
                            val isCurrent = (pagerState.currentPage == index)
                            val isCompleted = (pagerState.currentPage > index)

                            val pillScale by animateFloatAsState(
                                targetValue = if (isCurrent) 1.08f else 0.94f,
                                animationSpec = MiuixAnimation.springBouncy(),
                                label = "StepPillScale"
                            )
                            val pillAlpha by animateFloatAsState(
                                targetValue = if (isCurrent) 1f else if (isCompleted) 0.85f else 0.55f,
                                animationSpec = MiuixAnimation.springSmooth(),
                                label = "StepPillAlpha"
                            )
                            val pillBgColor by animateColorAsState(
                                targetValue = when {
                                    isCurrent -> activeThemeColor
                                    isCompleted -> activeThemeColor.copy(alpha = 0.22f)
                                    else -> MiuixTheme.colorScheme.surfaceVariant
                                },
                                animationSpec = tween(220),
                                label = "StepPillBg"
                            )

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = pillScale
                                        scaleY = pillScale
                                        this.alpha = pillAlpha
                                    }
                                    .clip(MiuixShapes.PillShape)
                                    .background(pillBgColor)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index, animationSpec = MiuixAnimation.springSmooth())
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.5.dp),
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

                    // 步骤主副标题 (左右滑动 + 3D 缩放淡入淡出动画，杜绝生硬与文字截断)
                    val (stepTitle, stepSubtitle) = when (pagerState.currentPage) {
                        0 -> Pair(
                            if (editingRecord != null) {
                                if (isIncomeMode) "修改收入 · 设定金额" else "修改支出 · 设定金额"
                            } else {
                                if (isIncomeMode) "记一笔收入 · 设定金额" else "记一笔支出 · 设定金额"
                            },
                            "输入本次款项金额，支持即时算式计算"
                        )
                        1 -> Pair(
                            if (isIncomeMode) "归属分类 · 收入来源" else "归属分类 · 预算信封透视",
                            if (isIncomeMode) "选择收入类别与同步月份" else "选择支出大类，查看预算实时消耗与结余演变"
                        )
                        else -> Pair(
                            if (editingRecord != null) "修改属性 · 保存修改" else "记账属性 · 确认入账",
                            "选择记账日期、出资账户与备注说明"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = Triple(pagerState.currentPage, stepTitle, stepSubtitle),
                            transitionSpec = {
                                val isForward = targetState.first >= initialState.first
                                val slideInOffset = if (isForward) { width: Int -> (width * 0.40f).toInt() } else { width: Int -> -(width * 0.40f).toInt() }
                                val slideOutOffset = if (isForward) { width: Int -> -(width * 0.40f).toInt() } else { width: Int -> (width * 0.40f).toInt() }

                                (
                                    (slideInHorizontally(
                                        initialOffsetX = slideInOffset,
                                        animationSpec = tween(280, easing = MiuixAnimation.MiuixDecelerateEasing)
                                    ) + scaleIn(
                                        initialScale = 0.90f,
                                        animationSpec = tween(280, easing = MiuixAnimation.MiuixDecelerateEasing)
                                    ) + fadeIn(
                                        animationSpec = tween(240)
                                    )) togetherWith
                                    (slideOutHorizontally(
                                        targetOffsetX = slideOutOffset,
                                        animationSpec = tween(220, easing = MiuixAnimation.MiuixFluidEasing)
                                    ) + scaleOut(
                                        targetScale = 0.90f,
                                        animationSpec = tween(220, easing = MiuixAnimation.MiuixFluidEasing)
                                    ) + fadeOut(
                                        animationSpec = tween(200)
                                    ))
                                ).using(SizeTransform(clip = false))
                            },
                            contentAlignment = Alignment.Center,
                            label = "HorizontalSlideScaleTitleAnimation"
                        ) { (_, targetTitle, targetSubtitle) ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = targetTitle,
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Text(
                                    text = targetSubtitle,
                                    fontSize = 11.5.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // ==================== 2. 中部 HorizontalPager (固定统一高度 370.dp，彻底消除高度跳动) ====================
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(370.dp)
                ) { page ->
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                    val scale = 1f - (pageOffset * 0.08f).coerceIn(0f, 0.08f)
                    val alpha = 1f - (pageOffset * 0.45f).coerceIn(0f, 0.5f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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
                                        .fillMaxSize()
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
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. 记账日期自选（居中定位）
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

                                        // 当月 1 ~ maxDaysInMonth 全量自选滑轨（带两端羽化淡出 & 居中定位）
                                        LazyRow(
                                            state = dateListState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalFadingEdge(16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            items((1..maxDaysInMonth).toList()) { day ->
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

                                        // 常用账户快捷胶囊（带边缘羽化模糊）
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalFadingEdge(14.dp)
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
                        // 第 1 步时：编辑模式下提供删除，新增模式下提供取消
                        if (editingRecord != null) {
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = true
                                },
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(color = MiuixRed.copy(alpha = 0.15f))
                            ) {
                                Text(text = "删除", color = MiuixRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

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
                                .weight(if (editingRecord != null && pagerState.currentPage == 0) 1.4f else 2f)
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
                        // 最终提交/保存按钮
                        val submitButtonText = if (editingRecord != null) {
                            "保存修改 $actionAmountString"
                        } else if (isIncomeMode) {
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

                                    if (editingRecord != null) {
                                        val finalAmount = if (isIncomeMode) evaluatedAmount else -evaluatedAmount
                                        val updated = editingRecord.copy(
                                            year = currentYear,
                                            month = currentMonth,
                                            day = transactionDay,
                                            categoryName = selectedCategory,
                                            detailName = finalDetail,
                                            amount = finalAmount,
                                            funder = funder.ifBlank { "默认账户" },
                                            remark = remarkText.trim()
                                        )
                                        viewModel.updateTransaction(editingRecord, updated)
                                    } else if (isIncomeMode) {
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

            // 删除流水记录二次确认弹窗
            if (showDeleteConfirmDialog && editingRecord != null) {
                val isIncome = editingRecord.amount > 0
                val typeName = if (isIncome) "入账" else "支出"
                val formattedAmount = if (isIncome) "+${MathFormulaEvaluator.formatAmount(editingRecord.amount)} ¥" else "-${MathFormulaEvaluator.formatAmount(editingRecord.absoluteAmount)} ¥"
                ConfirmDialog(
                    title = "删除${typeName}记录",
                    message = "确定要删除【${editingRecord.categoryName} - ${editingRecord.detailName}】金额 ${formattedAmount} 的这笔${typeName}记录吗？删除后可用结余将自动反算回补。",
                    onConfirm = {
                        viewModel.deleteTransaction(editingRecord)
                        showDeleteConfirmDialog = false
                        onDismissRequest()
                    },
                    onDismiss = {
                        showDeleteConfirmDialog = false
                    }
                )
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

