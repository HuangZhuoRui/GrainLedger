package com.vincent.grainledger.ui.screens.bookkeeping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.screens.bookkeeping.components.BookkeepingAmountStep
import com.vincent.grainledger.ui.screens.bookkeeping.components.BookkeepingAttributesStep
import com.vincent.grainledger.ui.screens.bookkeeping.components.BookkeepingBottomBar
import com.vincent.grainledger.ui.screens.bookkeeping.components.BookkeepingCategoryStep
import com.vincent.grainledger.ui.screens.bookkeeping.components.BookkeepingHeader
import com.vincent.grainledger.ui.screens.category.QuickCreateCategoryDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import com.vincent.grainledger.util.DateUtils
import com.vincent.grainledger.util.MathFormulaEvaluator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue

/**
 * MIUIX / HyperOS 风格卡片式横向滑动分步记一笔与修改流水弹窗 (BookkeepingDialog)。
 *
 * 模块化拆分架构：
 * 1. [BookkeepingHeader]：步骤指示胶囊与 3D 缩放横向滑动标题；
 * 2. [BookkeepingAmountStep]：支出/收入模式切换、金额输入、实时算式求解与快捷算术工具栏；
 * 3. [BookkeepingCategoryStep]：大类与细项选择、实时预算透视卡片、常用收入标签与多月同步；
 * 4. [BookkeepingAttributesStep]：日期居中滑轨、出资/收款账户、备注说明与记账核对预览；
 * 5. [BookkeepingBottomBar]：智能底部导航栏（支持分步导航、保存修改与快捷删除）；
 * 6. 支持全量修改与二次确认删除回退。
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
                // ==================== 1. 顶部步骤指示轴与动态标题（物理缩放与同向滑动过渡） ====================
                BookkeepingHeader(
                    pagerState = pagerState,
                    activeThemeColor = activeThemeColor,
                    isIncomeMode = isIncomeMode,
                    editingRecord = editingRecord,
                    onStepClick = { targetIndex ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetIndex, animationSpec = MiuixAnimation.springSmooth())
                        }
                    }
                )

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
                            // Step 1: 金额与算式
                            0 -> BookkeepingAmountStep(
                                isIncomeMode = isIncomeMode,
                                onIncomeModeChange = { income ->
                                    isIncomeMode = income
                                    if (!income) {
                                        val firstExpCat = expenseCategories.firstOrNull()?.categoryName
                                        if (firstExpCat != null) {
                                            selectedCategory = firstExpCat
                                            selectedDetail = ""
                                        }
                                    } else {
                                        val firstIncCat = incomeCategories.firstOrNull()?.categoryName
                                        if (firstIncCat != null) {
                                            selectedCategory = firstIncCat
                                        }
                                    }
                                },
                                amountInputText = amountInputText,
                                onAmountChange = { amountInputText = it },
                                evaluatedAmount = evaluatedAmount,
                                hasFormula = hasFormula,
                                activeThemeColor = activeThemeColor
                            )

                            // Step 2: 分类与预算/来源
                            1 -> BookkeepingCategoryStep(
                                isIncomeMode = isIncomeMode,
                                currentYear = currentYear,
                                currentMonth = currentMonth,
                                availableMonths = availableMonths,
                                selectedTargetMonths = selectedTargetMonths,
                                onTargetMonthsChange = { selectedTargetMonths = it },
                                incomeCategories = incomeCategories,
                                expenseCategories = expenseCategories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { cat ->
                                    selectedCategory = cat
                                    if (!isIncomeMode) {
                                        val firstDetail = budgetItemList.firstOrNull { it.categoryName == cat }?.detailName ?: ""
                                        selectedDetail = firstDetail
                                    }
                                },
                                currentCategoryExpenseItems = currentCategoryExpenseItems,
                                selectedDetail = selectedDetail,
                                onDetailSelected = { selectedDetail = it },
                                matchedBudgetItem = matchedBudgetItem,
                                evaluatedAmount = evaluatedAmount,
                                incomeDetailInput = incomeDetailInput,
                                onIncomeDetailChange = { incomeDetailInput = it },
                                activeThemeColor = activeThemeColor,
                                onOpenCreateCategory = { showCreateCategoryDialog = true }
                            )

                            // Step 3: 日期、出资账户与备注
                            2 -> BookkeepingAttributesStep(
                                currentYear = currentYear,
                                currentMonth = currentMonth,
                                maxDaysInMonth = maxDaysInMonth,
                                transactionDay = transactionDay,
                                onDaySelected = { transactionDay = it },
                                dateListState = dateListState,
                                isIncomeMode = isIncomeMode,
                                funder = funder,
                                onFunderChange = { funder = it },
                                remarkText = remarkText,
                                onRemarkChange = { remarkText = it },
                                selectedCategory = selectedCategory,
                                selectedDetail = selectedDetail,
                                incomeDetailInput = incomeDetailInput,
                                evaluatedAmount = evaluatedAmount,
                                activeThemeColor = activeThemeColor
                            )
                        }
                    }
                }

                // ==================== 3. 底部智能导航栏 ====================
                BookkeepingBottomBar(
                    currentPage = pagerState.currentPage,
                    isFinalStep = (pagerState.currentPage == 2),
                    isIncomeMode = isIncomeMode,
                    editingRecord = editingRecord,
                    evaluatedAmount = evaluatedAmount,
                    activeThemeColor = activeThemeColor,
                    selectedTargetMonthsCount = selectedTargetMonths.size,
                    onPrevStep = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = MiuixAnimation.springSmooth())
                        }
                    },
                    onNextStep = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = MiuixAnimation.springSmooth())
                        }
                    },
                    onCancel = onDismissRequest,
                    onDeleteClick = { showDeleteConfirmDialog = true },
                    onSubmit = {
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
                    }
                )
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
