package com.vincent.grainledger.ui.screens.bookkeeping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
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
 * 看板快速记账与入账弹窗 (BookkeepingDialog)。
 *
 * 规范承载日常操作：
 * - 【记支出】：选择支出大类 -> 联动对应预算细项并展现当前剩余额度 -> 录入消费并扣减预算；
 * - 【记收入】：选择收入大类（来自分类管理）-> 录入单笔入账来源与金额 -> 直接累加至当月资金池（总量）与可用结余。
 *
 * @param viewModel 全局视图模型
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun BookkeepingDialog(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit
) {
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val budgetItemList by viewModel.currentBudgetItems.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var isIncomeMode by remember { mutableStateOf(false) }
    var transactionDay by remember {
        mutableIntStateOf(
            DateUtils.getCurrentDay().coerceIn(1, DateUtils.getDaysInMonth(currentYear, currentMonth))
        )
    }

    // 根据模式分别过滤分类
    val expenseCategories = remember(allCategories) { allCategories.filter { !it.isIncome } }
    val incomeCategories = remember(allCategories) { allCategories.filter { it.isIncome } }
    val activeCategories = if (isIncomeMode) incomeCategories else expenseCategories

    var selectedCategory by remember {
        mutableStateOf(expenseCategories.firstOrNull()?.categoryName ?: "强制类")
    }
    var selectedDetail by remember { mutableStateOf("") }
    var incomeDetailInput by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("") }
    var funder by remember { mutableStateOf("默认账户") }
    var remarkText by remember { mutableStateOf("") }

    // 支出模式下过滤当前大类的预算细项
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部：支出/收入分段切换胶囊
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MiuixShapes.MediumSquircle)
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 记支出按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (!isIncomeMode) MiuixTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!isIncomeMode) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "记一笔支出",
                                fontSize = 13.5.sp,
                                fontWeight = if (!isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isIncomeMode) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }

                    // 记收入按钮
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (isIncomeMode) MiuixGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
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
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "记一笔收入",
                                fontSize = 13.5.sp,
                                fontWeight = if (isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                                color = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }

                // 1. 日期选择条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceVariant, MiuixShapes.MediumSquircle)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isIncomeMode) MiuixGreen else MiuixBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${currentYear}年${currentMonth}月${transactionDay}日",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                    }

                    // 快捷切换日期的微调按钮
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                if (transactionDay > 1) transactionDay--
                            }
                        ) {
                            Text(
                                text = "前一天",
                                fontSize = 12.sp,
                                color = if (isIncomeMode) MiuixGreen else MiuixBlue
                            )
                        }
                        TextButton(
                            onClick = {
                                val maxDays = DateUtils.getDaysInMonth(currentYear, currentMonth)
                                if (transactionDay < maxDays) transactionDay++
                            }
                        ) {
                            Text(
                                text = "后一天",
                                fontSize = 12.sp,
                                color = if (isIncomeMode) MiuixGreen else MiuixBlue
                            )
                        }
                    }
                }

                // 2. 金额输入框（支持公式）
                OutlinedTextField(
                    value = amountInputText,
                    onValueChange = { amountInputText = it },
                    label = {
                        Text(
                            text = if (isIncomeMode) "入账金额 (支持如 3000+500 公式)" else "支出金额 (支持如 30+50 公式)"
                        )
                    },
                    placeholder = { Text(text = if (isIncomeMode) "例如: 3500.00" else "例如: 180.59") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 3. 选择归属大类
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isIncomeMode) "选择收入类别" else "选择支出大类",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    if (activeCategories.isEmpty()) {
                        Text(
                            text = if (isIncomeMode) "暂无收入分类，请在分类管理中创建收入类" else "暂无支出分类",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeCategories.forEach { category ->
                                val isSelected = (category.categoryName == selectedCategory)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) {
                                                if (isIncomeMode) MiuixGreen else MiuixBlue
                                            } else {
                                                MiuixTheme.colorScheme.surfaceVariant
                                            },
                                            shape = MiuixShapes.SmallSquircle
                                        )
                                        .clickable {
                                            selectedCategory = category.categoryName
                                            if (!isIncomeMode) {
                                                val categoryItems = budgetItemList.filter { it.categoryName == category.categoryName }
                                                selectedDetail = categoryItems.firstOrNull()?.detailName ?: ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = category.categoryName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 细项信息（支出联动预算细项，收入直接输入来源明细）
                if (isIncomeMode) {
                    OutlinedTextField(
                        value = incomeDetailInput,
                        onValueChange = { incomeDetailInput = it },
                        label = { Text(text = "收入来源明细 (如 8月基本工资、季度绩效、兼职报酬)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MiuixShapes.MediumSquircle,
                        singleLine = true
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "关联支出预算项",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
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
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSelected) MiuixBlue.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant,
                                                shape = MiuixShapes.SmallSquircle
                                            )
                                            .clickable {
                                                selectedDetail = item.detailName
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = item.detailName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // 预算项当前剩余额度提示卡片
                        if (matchedBudgetItem != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 14.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "当前预算项: ${matchedBudgetItem.detailName}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiuixTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "实际注入额度: ¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.actualAllocated)}",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "当前实时剩余",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                        Text(
                                            text = "¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.balance)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (matchedBudgetItem.balance < 0) MiuixRed else MiuixGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. 账户与渠道
                OutlinedTextField(
                    value = funder,
                    onValueChange = { funder = it },
                    label = { Text(text = if (isIncomeMode) "入账收款账户 (如 工资卡、微信零钱、支付宝)" else "扣款出资账户 (如 招商银行卡、微信零钱)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 6. 备注说明
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text(text = if (isIncomeMode) "收入说明备注 (选填)" else "支出说明备注 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 7. 底部确认按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            val evaluatedAmount = MathFormulaEvaluator.evaluate(amountInputText)
                            if (evaluatedAmount > 0.0) {
                                val finalDetail = if (isIncomeMode) {
                                    if (incomeDetailInput.isNotBlank()) incomeDetailInput.trim() else "日常入账"
                                } else {
                                    if (selectedDetail.isNotEmpty()) selectedDetail else "日常支出"
                                }
                                val finalAmount = if (isIncomeMode) evaluatedAmount else -evaluatedAmount
                                viewModel.recordTransaction(
                                    year = currentYear,
                                    month = currentMonth,
                                    day = transactionDay,
                                    categoryName = selectedCategory,
                                    detailName = finalDetail,
                                    amount = finalAmount,
                                    funder = funder,
                                    remark = remarkText
                                )
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            color = if (isIncomeMode) MiuixGreen else MiuixBlue
                        )
                    ) {
                        Text(
                            text = if (isIncomeMode) "确认入账" else "确认记账",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
