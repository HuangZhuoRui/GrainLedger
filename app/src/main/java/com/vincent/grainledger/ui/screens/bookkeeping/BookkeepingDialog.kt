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
 * MIUIX 风格快速记账弹窗。
 *
 * 支持选择大类后自动联动该类下的预算细项，并在界面上实时展现当前具体预算项的剩余额度。
 * 金额输入支持公式计算（如 30+50），录入完成后自动精准计算双剩余并扣减预算。
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

    var transactionDay by remember { mutableIntStateOf(DateUtils.getCurrentDay().coerceIn(1, DateUtils.getDaysInMonth(currentYear, currentMonth))) }
    var selectedCategory by remember { mutableStateOf(allCategories.firstOrNull()?.categoryName ?: "强制类") }
    var selectedDetail by remember { mutableStateOf("") }
    var amountInputText by remember { mutableStateOf("") }
    var funder by remember { mutableStateOf("默认账户") }
    var remarkText by remember { mutableStateOf("") }

    // 过滤当前大类下的所有预算细项
    val currentCategoryItems = remember(selectedCategory, budgetItemList) {
        budgetItemList.filter { it.categoryName == selectedCategory }
    }

    // 默认选中该类下的第一个预算细项
    val matchedBudgetItem = remember(selectedCategory, selectedDetail, currentCategoryItems) {
        currentCategoryItems.find { it.detailName == selectedDetail } ?: currentCategoryItems.firstOrNull()
    }

    // 当大类变动时自动更新选中详情
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
                // 顶部标题
                Text(
                    text = "记一笔支出",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

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
                            tint = MiuixBlue,
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
                            Text(text = "前一天", fontSize = 12.sp, color = MiuixBlue)
                        }
                        TextButton(
                            onClick = {
                                val maxDays = DateUtils.getDaysInMonth(currentYear, currentMonth)
                                if (transactionDay < maxDays) transactionDay++
                            }
                        ) {
                            Text(text = "后一天", fontSize = 12.sp, color = MiuixBlue)
                        }
                    }
                }

                // 2. 金额输入框（支持公式）
                OutlinedTextField(
                    value = amountInputText,
                    onValueChange = { amountInputText = it },
                    label = { Text(text = "支出金额 (支持如 30+50 公式)") },
                    placeholder = { Text(text = "例如: 180.59") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 3. 选择归属大类
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "归属分类",
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
                        allCategories.forEach { category ->
                            val isSelected = (category.categoryName == selectedCategory)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant,
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

                // 4. 选择对应预算项（联动显示当前剩余额度）
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "关联预算细项",
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
                                        text = "实际加入额度: ¥${MathFormulaEvaluator.formatAmount(matchedBudgetItem.actualAllocated)}",
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

                // 5. 备注说明
                OutlinedTextField(
                    value = remarkText,
                    onValueChange = { remarkText = it },
                    label = { Text(text = "备注说明 (选填)") },
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
                                val finalDetail = if (selectedDetail.isNotEmpty()) selectedDetail else "日常支出"
                                // 支出金额记为负数
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
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                    ) {
                        Text(text = "确认记账", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
