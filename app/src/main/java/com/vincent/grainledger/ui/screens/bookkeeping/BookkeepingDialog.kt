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
 * MIUIX 风格快速记账与入账弹窗 (BookkeepingDialog)。
 *
 * 支持【支出记账】与【收入入账】无缝切换，选择大类后自动联动该类下的预算细项，
 * 并在界面上实时展现当前具体预算项的剩余额度。金额输入支持公式计算（如 30+50、3000+500）。
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

    // 根据模式过滤分类（支出类 vs 收入类）
    val filteredCategories = remember(isIncomeMode, allCategories) {
        val matches = allCategories.filter { it.isIncome == isIncomeMode }
        if (matches.isNotEmpty()) matches else allCategories
    }

    var selectedCategory by remember {
        mutableStateOf(filteredCategories.firstOrNull()?.categoryName ?: "强制类")
    }
    var selectedDetail by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("") }
    var funder by remember { mutableStateOf(if (isIncomeMode) "银行卡" else "默认账户") }
    var remarkText by remember { mutableStateOf("") }

    // 模式切换时自动重置归属大类
    val currentCategoryItems = remember(selectedCategory, budgetItemList) {
        budgetItemList.filter { it.categoryName == selectedCategory }
    }

    // 默认选中该类下的第一个细项
    val matchedBudgetItem = remember(selectedCategory, selectedDetail, currentCategoryItems) {
        currentCategoryItems.find { it.detailName == selectedDetail } ?: currentCategoryItems.firstOrNull()
    }

    if (selectedDetail.isEmpty() && currentCategoryItems.isNotEmpty()) {
        selectedDetail = currentCategoryItems.first().detailName
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
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (!isIncomeMode) MiuixTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                isIncomeMode = false
                                val firstExpCat = allCategories.firstOrNull { !it.isIncome }?.categoryName
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(if (isIncomeMode) MiuixGreen.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable {
                                isIncomeMode = true
                                val firstIncCat = allCategories.firstOrNull { it.isIncome }?.categoryName
                                if (firstIncCat != null) {
                                    selectedCategory = firstIncCat
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
                        text = if (isIncomeMode) "归属收入分类" else "归属支出分类",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCategories.forEach { category ->
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
                                        val categoryItems = budgetItemList.filter { it.categoryName == category.categoryName }
                                        selectedDetail = categoryItems.firstOrNull()?.detailName ?: ""
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

                // 4. 选择对应细项
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isIncomeMode) "关联收入细项" else "关联支出细项",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    if (currentCategoryItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentCategoryItems.forEach { item ->
                                val isSelected = (item.detailName == selectedDetail)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) {
                                                (if (isIncomeMode) MiuixGreen else MiuixBlue).copy(alpha = 0.15f)
                                            } else {
                                                MiuixTheme.colorScheme.surfaceVariant
                                            },
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
                                        color = if (isSelected) (if (isIncomeMode) MiuixGreen else MiuixBlue) else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 细项状态卡片
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
                                        text = "细项: ${matchedBudgetItem.detailName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isIncomeMode) "计划到账: ¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.actualAllocated)}" else "实际加入额度: ¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.actualAllocated)}",
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isIncomeMode) "累计入账" else "当前实时剩余",
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                    Text(
                                        text = if (isIncomeMode) "+¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.actualAllocated)}" else "¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.balance)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncomeMode) MiuixGreen else if (matchedBudgetItem.balance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. 备注说明
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text(text = if (isIncomeMode) "收入来源与备注 (选填)" else "支出说明备注 (选填)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 6. 底部确认按钮
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
                                val finalDetail = if (selectedDetail.isNotEmpty()) {
                                    selectedDetail
                                } else {
                                    if (isIncomeMode) "日常收入" else "日常支出"
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
