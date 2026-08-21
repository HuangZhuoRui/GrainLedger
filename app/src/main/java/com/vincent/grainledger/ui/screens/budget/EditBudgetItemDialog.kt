package com.vincent.grainledger.ui.screens.budget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.screens.category.QuickCreateCategoryDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue

/**
 * MIUIX / HyperOS 风格卡片式横向滑动加预算与编辑预算弹窗 (EditBudgetItemDialog)。
 *
 * 核心创新：
 * 1. 采用 HorizontalPager 分步卡片流，支持手势 1:1 左右平滑滑动与按键导航；
 * 2. 具备与月份切换一致的物理弹簧阻尼、缩放 (0.92x~1.0x) 与渐变透明度 (0.5~1.0) 动效；
 * 3. 顶部步骤指示轴与动态标题/副标题随滑动联动；
 * 4. 三大卡片专注拆分：
 *    - Step 1: 支出大类与预算细项（支持原地新建大类与智能高频细项推荐）
 *    - Step 2: 预算总额核算（单价算式 + 数量/月数步进微调器 + 算术工具条）
 *    - Step 3: 资金注入规划、出资扣款账户与说明备注
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
                        listOf("1 细项", "2 核算", "3 注资").forEachIndexed { index, title ->
                            val isCurrent = (pagerState.currentPage == index)
                            val isCompleted = (pagerState.currentPage > index)

                            Box(
                                modifier = Modifier
                                    .clip(MiuixShapes.PillShape)
                                    .background(
                                        when {
                                            isCurrent -> MiuixBlue
                                            isCompleted -> MiuixBlue.copy(alpha = 0.25f)
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
                                            tint = MiuixBlue,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Text(
                                        text = title,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isCurrent -> Color.White
                                            isCompleted -> MiuixBlue
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
                            if (targetItem == null) "新增支出预算 · 细项归属" else "编辑支出预算 · 细项归属",
                            "选择归属支出大类与预算细项名称"
                        )
                        1 -> Pair(
                            "预算核算 · 基准与数量",
                            "设定单价基准与数量月数，实时核算预算总额"
                        )
                        else -> Pair(
                            "资金注入 · 账户与备注",
                            "规划实际注入资金额度、出资账户与备注"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = stepTitle,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "BudgetTitleAnimation"
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
                            // ==================== Step 1: 支出分类与细项 ====================
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // 归属支出大类选择（支持原地新建分类）
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "选择支出大类",
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
                                            expenseCategories.forEach { category ->
                                                val isSelected = (category.categoryName == selectedCategory)
                                                val catColor = category.themeColor

                                                Box(
                                                    modifier = Modifier
                                                        .clip(MiuixShapes.SmallSquircle)
                                                        .background(
                                                            if (isSelected) catColor.copy(alpha = 0.18f) else MiuixTheme.colorScheme.surfaceVariant
                                                        )
                                                        .clickable {
                                                            selectedCategory = category.categoryName
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
                                                            text = category.categoryName,
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
                                                    .background(MiuixBlue.copy(alpha = 0.12f))
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
                                                        tint = MiuixBlue,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "新建分类",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MiuixBlue
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 细项名称输入与智能推荐气泡
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = detailName,
                                            onValueChange = { detailName = it },
                                            label = { Text(text = "预算细项名称") },
                                            placeholder = { Text(text = "例如：房租物业、一日三餐、水电燃气") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MiuixShapes.MediumSquircle,
                                            singleLine = true
                                        )

                                        // 智能推荐常用标签
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            recommendedDetails.forEach { preset ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(MiuixShapes.SmallSquircle)
                                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                                    .clickable { detailName = preset }
                                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "+ $preset",
                                                        fontSize = 11.5.sp,
                                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ==================== Step 2: 预算总额核算 ====================
                            1 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Hero 预算总额与算式联动卡片
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 20.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // 顶部总预算看板
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "预算总额 (单价 × 数量)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                                )

                                                Text(
                                                    text = "${MathFormulaEvaluator.formatAmount(totalBudgetCalculated)} ¥",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MiuixBlue
                                                )
                                            }

                                            // 单价输入与数量调节行
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 单价/基准额（支持算式）
                                                OutlinedTextField(
                                                    value = unitPriceInput,
                                                    onValueChange = { unitPriceInput = it },
                                                    label = { Text(text = "单价/基准额") },
                                                    placeholder = { Text(text = "如 3200 或 100*30") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1.3f),
                                                    shape = MiuixShapes.MediumSquircle,
                                                    singleLine = true
                                                )

                                                // 数量/月数调节器（带 - / + 微调气泡）
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = "数量/月数",
                                                        fontSize = 11.sp,
                                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(48.dp)
                                                            .clip(MiuixShapes.MediumSquircle)
                                                            .background(MiuixTheme.colorScheme.surfaceVariant),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .clickable {
                                                                    val cur = quantityInput.toDoubleOrNull() ?: 1.0
                                                                    if (cur > 1.0) {
                                                                        val next = cur - 1.0
                                                                        quantityInput = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "减少",
                                                                tint = MiuixTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier.weight(1f),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            BasicTextField(
                                                                value = quantityInput,
                                                                onValueChange = { quantityInput = it },
                                                                textStyle = TextStyle(
                                                                    fontSize = 15.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MiuixTheme.colorScheme.onSurface,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                ),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                singleLine = true,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .clickable {
                                                                    val cur = quantityInput.toDoubleOrNull() ?: 1.0
                                                                    val next = cur + 1.0
                                                                    quantityInput = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "增加",
                                                                tint = MiuixTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // 快捷算术微工具条 (+, -, ×, ÷, C, ⌫)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                            .height(28.dp)
                                                            .clip(MiuixShapes.SmallSquircle)
                                                            .background(MiuixTheme.colorScheme.surfaceVariant)
                                                            .clickable {
                                                                if (unitPriceInput.isNotEmpty() && !unitPriceInput.endsWith("+") &&
                                                                    !unitPriceInput.endsWith("-") && !unitPriceInput.endsWith("*") && !unitPriceInput.endsWith("/")
                                                                ) {
                                                                    unitPriceInput += op
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = displayOp,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MiuixTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }

                                                // 清空 C
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .clip(MiuixShapes.SmallSquircle)
                                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                                        .clickable { unitPriceInput = "" },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "C",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                                    )
                                                }

                                                // 退格 ⌫
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(28.dp)
                                                        .clip(MiuixShapes.SmallSquircle)
                                                        .background(MiuixTheme.colorScheme.surfaceVariant)
                                                        .clickable {
                                                            if (unitPriceInput.isNotEmpty()) {
                                                                unitPriceInput = unitPriceInput.dropLast(1)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                        contentDescription = "退格",
                                                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // ==================== Step 3: 资金注入与出资账户 ====================
                            2 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 实际注入资金智能卡片
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
                                                    text = "实际注资金额 (资金池注入)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )

                                                TextButton(
                                                    onClick = {
                                                        actualAllocatedInput = if (totalBudgetCalculated > 0.0) {
                                                            MathFormulaEvaluator.formatAmount(totalBudgetCalculated)
                                                        } else ""
                                                    }
                                                ) {
                                                    Text(text = "100% 等同总额", fontSize = 11.5.sp, color = MiuixBlue)
                                                }
                                            }

                                            OutlinedTextField(
                                                value = actualAllocatedInput,
                                                onValueChange = { actualAllocatedInput = it },
                                                placeholder = {
                                                    Text(
                                                        text = "留空默认等同总预算 (${MathFormulaEvaluator.formatAmount(totalBudgetCalculated)} ¥)",
                                                        fontSize = 12.5.sp
                                                    )
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                shape = MiuixShapes.MediumSquircle,
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )

                                            // 注入进度条与状态提示
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(MiuixShapes.PillShape)
                                                    .background(MiuixTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(animatedRatio)
                                                        .fillMaxHeight()
                                                        .clip(MiuixShapes.PillShape)
                                                        .background(if (allocationRatio >= 1f) MiuixGreen else if (allocationRatio > 0.5f) MiuixBlue else MiuixOrange)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (actualAllocatedEvaluated == totalBudgetCalculated) "已全额预算注资" else "部分资金注入",
                                                    fontSize = 11.sp,
                                                    color = if (actualAllocatedEvaluated == totalBudgetCalculated) MiuixGreen else MiuixOrange
                                                )

                                                Text(
                                                    text = "注入额: ${MathFormulaEvaluator.formatAmount(actualAllocatedEvaluated)} ¥",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MiuixTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // 出资账户
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedTextField(
                                            value = funder,
                                            onValueChange = { funder = it },
                                            label = { Text(text = "资金出处 / 扣款账户 (可输入或点选)") },
                                            placeholder = { Text("例如：微信零钱、招商银行卡、美团月付") },
                                            singleLine = true,
                                            shape = MiuixShapes.MediumSquircle,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // 常用账户快捷点选胶囊
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

                                    // 备注说明
                                    OutlinedTextField(
                                        value = remark,
                                        onValueChange = { remark = it },
                                        placeholder = { Text("添加支出说明备注 (选填)...", fontSize = 12.5.sp) },
                                        singleLine = true,
                                        shape = MiuixShapes.MediumSquircle,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // 预算总览小结卡片
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
                                                text = "${selectedCategory} · ${detailName.ifBlank { "未命名细项" }}",
                                                fontSize = 12.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                                            )

                                            Text(
                                                text = "总预算: ${MathFormulaEvaluator.formatAmount(totalBudgetCalculated)} ¥",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MiuixBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== 3. 底部智能导航栏 ====================
                val actionBudgetAmountString = if (totalBudgetCalculated > 0.0) "${MathFormulaEvaluator.formatAmount(totalBudgetCalculated)} ¥" else ""
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
                        if (targetItem != null) {
                            Button(
                                onClick = {
                                    onDelete(targetItem.itemId)
                                    onDismissRequest()
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
                                .weight(if (targetItem != null && pagerState.currentPage == 0) 1.4f else 2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                        ) {
                            Text(
                                text = "下一步 ➔",
                                color = Color.White,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // 最终加入/保存预算按钮
                        Button(
                            onClick = {
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
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                color = if (detailName.isNotBlank()) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = if (targetItem == null) {
                                    if (actionBudgetAmountString.isNotEmpty()) "加入预算 ($actionBudgetAmountString)" else "加入预算"
                                } else {
                                    "保存预算"
                                },
                                color = if (detailName.isNotBlank()) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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

