package com.vincent.grainledger.ui.screens.budget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.screens.budget.components.EditBudgetAllocationStep
import com.vincent.grainledger.ui.screens.budget.components.EditBudgetAmountStep
import com.vincent.grainledger.ui.screens.budget.components.EditBudgetBottomBar
import com.vincent.grainledger.ui.screens.budget.components.EditBudgetCategoryStep
import com.vincent.grainledger.ui.screens.budget.components.EditBudgetHeader
import com.vincent.grainledger.ui.screens.category.QuickCreateCategoryDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue

/**
 * MIUIX / HyperOS 风格卡片式横向滑动加预算与编辑预算弹窗 (EditBudgetItemDialog)。
 *
 * 模块化拆分架构：
 * 1. [EditBudgetHeader]：步骤指示胶囊与 3D 缩放横向滑动标题；
 * 2. [EditBudgetCategoryStep]：支出大类选择、细项名称输入与智能高频推荐标签；
 * 3. [EditBudgetAmountStep]：单价基准、数量调节器、实时公式计算与快捷算术工具条；
 * 4. [EditBudgetAllocationStep]：资金池注入规划、出资账户、备注说明与预算总览；
 * 5. [EditBudgetBottomBar]：智能底部导航栏（支持分步导航、保存预算与快捷删除）。
 */
@Composable
fun EditBudgetItemDialog(
    targetItem: BudgetItem?,
    year: Int,
    month: Int,
    categoryList: List<BudgetCategory>,
    onSave: (BudgetItem) -> Unit,
    onDelete: (Long) -> Unit,
    onDismissRequest: () -> Unit,
    onSaveCategory: ((BudgetCategory) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

    val expenseCategories = remember(categoryList) {
        val list = categoryList.filter { !it.isIncome }
        if (list.isNotEmpty()) list else categoryList
    }

    var selectedCategory by remember {
        mutableStateOf(
            targetItem?.categoryName ?: expenseCategories.firstOrNull()?.categoryName ?: "强制类"
        )
    }
    var detailName by remember { mutableStateOf(targetItem?.detailName ?: "") }
    var unitPriceInput by remember {
        mutableStateOf(
            if (targetItem != null) MathFormulaEvaluator.formatAmount(targetItem.unitPrice) else ""
        )
    }
    var quantityInput by remember {
        mutableStateOf(
            if (targetItem != null) {
                if (targetItem.quantity % 1.0 == 0.0) targetItem.quantity.toInt().toString() else targetItem.quantity.toString()
            } else "1"
        )
    }
    var actualAllocatedInput by remember {
        mutableStateOf(
            if (targetItem != null) MathFormulaEvaluator.formatAmount(targetItem.actualAllocated) else ""
        )
    }
    var funder by remember { mutableStateOf(targetItem?.funder ?: "微信零钱") }
    var remark by remember { mutableStateOf(targetItem?.remark ?: "") }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    // 动态计算总价与注资金额
    val unitPriceEvaluated = remember(unitPriceInput) {
        MathFormulaEvaluator.evaluate(unitPriceInput)
    }
    val quantityEvaluated = remember(quantityInput) {
        quantityInput.toDoubleOrNull() ?: 1.0
    }
    val totalBudgetCalculated = remember(unitPriceEvaluated, quantityEvaluated) {
        unitPriceEvaluated * quantityEvaluated
    }
    val actualAllocatedEvaluated = remember(actualAllocatedInput, totalBudgetCalculated) {
        if (actualAllocatedInput.isNotBlank()) {
            MathFormulaEvaluator.evaluate(actualAllocatedInput)
        } else {
            totalBudgetCalculated
        }
    }

    val recommendedDetails = remember(selectedCategory) {
        getRecommendedDetailsForCategory(selectedCategory)
    }

    val allocationRatio = if (totalBudgetCalculated > 0) {
        (actualAllocatedEvaluated / totalBudgetCalculated).toFloat()
    } else 1f
    val animatedRatio by animateFloatAsState(
        targetValue = allocationRatio.coerceIn(0f, 1f),
        animationSpec = MiuixAnimation.springFast(),
        label = "注资比例动画"
    )

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
                EditBudgetHeader(
                    pagerState = pagerState,
                    targetItem = targetItem,
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
                            // Step 1: 支出分类与细项
                            0 -> EditBudgetCategoryStep(
                                expenseCategories = expenseCategories,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it },
                                detailName = detailName,
                                onDetailNameChange = { detailName = it },
                                recommendedDetails = recommendedDetails,
                                onOpenCreateCategory = { showCreateCategoryDialog = true }
                            )

                            // Step 2: 预算总额核算
                            1 -> EditBudgetAmountStep(
                                totalBudgetCalculated = totalBudgetCalculated,
                                unitPriceInput = unitPriceInput,
                                onUnitPriceChange = { unitPriceInput = it },
                                quantityInput = quantityInput,
                                onQuantityChange = { quantityInput = it }
                            )

                            // Step 3: 资金注入与出资账户
                            2 -> EditBudgetAllocationStep(
                                totalBudgetCalculated = totalBudgetCalculated,
                                actualAllocatedInput = actualAllocatedInput,
                                onActualAllocatedChange = { actualAllocatedInput = it },
                                actualAllocatedEvaluated = actualAllocatedEvaluated,
                                allocationRatio = allocationRatio,
                                animatedRatio = animatedRatio,
                                funder = funder,
                                onFunderChange = { funder = it },
                                remark = remark,
                                onRemarkChange = { remark = it },
                                selectedCategory = selectedCategory,
                                detailName = detailName
                            )
                        }
                    }
                }

                // ==================== 3. 底部智能导航栏 ====================
                EditBudgetBottomBar(
                    currentPage = pagerState.currentPage,
                    isFinalStep = (pagerState.currentPage == 2),
                    targetItem = targetItem,
                    totalBudgetCalculated = totalBudgetCalculated,
                    detailName = detailName,
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
                    onDeleteClick = {
                        if (targetItem != null) {
                            onDelete(targetItem.itemId)
                            onDismissRequest()
                        }
                    },
                    onSubmit = {
                        if (detailName.isNotBlank() && totalBudgetCalculated >= 0.0) {
                            val newItem = BudgetItem(
                                itemId = targetItem?.itemId ?: 0L,
                                year = year,
                                month = month,
                                categoryName = selectedCategory,
                                detailName = detailName.trim(),
                                unitPrice = unitPriceEvaluated,
                                quantity = quantityEvaluated,
                                totalPrice = totalBudgetCalculated,
                                actualAllocated = actualAllocatedEvaluated,
                                funder = funder,
                                actualSpent = targetItem?.actualSpent ?: 0.0,
                                balance = actualAllocatedEvaluated - (targetItem?.actualSpent ?: 0.0),
                                remark = remark.trim()
                            )
                            onSave(newItem)
                            onDismissRequest()
                        }
                    }
                )
            }

            // 快速新建分类弹窗
            if (showCreateCategoryDialog) {
                QuickCreateCategoryDialog(
                    isIncomeCategory = false,
                    currentYear = year,
                    currentMonth = month,
                    onSaveCategoryWithBudget = { newCat, initialDetail, initialAmount ->
                        onSaveCategory?.invoke(newCat)
                        selectedCategory = newCat.categoryName
                        if (!initialDetail.isNullOrBlank()) {
                            detailName = initialDetail
                        }
                        if (initialAmount != null && initialAmount > 0) {
                            unitPriceInput = MathFormulaEvaluator.formatAmount(initialAmount)
                        }
                        showCreateCategoryDialog = false
                    },
                    onDismissRequest = { showCreateCategoryDialog = false }
                )
            }
        }
    }
}

/**
 * 根据支出大类智能匹配常用细项预设推荐。
 */
private fun getRecommendedDetailsForCategory(categoryName: String): List<String> {
    return when {
        categoryName.contains("强制") || categoryName.contains("固定") || categoryName.contains("房") ->
            listOf("房租物业", "水电气网", "房贷车贷", "手机话费", "保险缴费", "宽带暖气")
        categoryName.contains("餐饮") || categoryName.contains("吃") || categoryName.contains("食") ->
            listOf("一日三餐", "外卖聚餐", "买菜生鲜", "咖啡饮品", "零食水果", "下午茶夜宵")
        categoryName.contains("交通") || categoryName.contains("出行") || categoryName.contains("车") ->
            listOf("地铁公交", "加油充电", "打车出行", "停车过路", "保养维修", "机票火车")
        categoryName.contains("刚需") || categoryName.contains("生活") || categoryName.contains("日常") ->
            listOf("日用百货", "纸品消耗", "服饰鞋包", "家庭买药", "个护美妆", "家政保洁")
        categoryName.contains("品质") || categoryName.contains("娱乐") || categoryName.contains("休闲") ->
            listOf("影音娱乐", "旅行度假", "聚会聚餐", "游戏充值", "运动健身", "数码外设")
        categoryName.contains("人情") || categoryName.contains("礼") || categoryName.contains("社交") ->
            listOf("红包礼金", "请客送礼", "长辈赡养", "随礼份子", "朋友聚会")
        categoryName.contains("储蓄") || categoryName.contains("投资") || categoryName.contains("理财") ->
            listOf("定投储蓄", "养老储备", "应急备用金", "教育基金")
        else ->
            listOf("日常开销", "应急备用", "周期扣款", "其他支出")
    }
}
