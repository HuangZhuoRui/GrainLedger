package com.vincent.grainledger.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算细项新增与编辑弹窗 (EditBudgetItemDialog)。
 *
 * 自动根据所选类别的【收入/支出】性质动态变换标题、输入项名称、说明占位符、提示横幅与保存按钮。
 *
 * @param targetItem 待编辑的预算项（若为 null 则代表新增）
 * @param year 当前年份
 * @param month 当前月份
 * @param categoryList 可供选择的分类列表
 * @param onSave 保存回调
 * @param onDelete 删除回调
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun EditBudgetItemDialog(
    targetItem: BudgetItem?,
    year: Int,
    month: Int,
    categoryList: List<BudgetCategory>,
    onSave: (BudgetItem) -> Unit,
    onDelete: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(targetItem?.categoryName ?: categoryList.firstOrNull()?.categoryName ?: "强制类") }
    var detailName by remember { mutableStateOf(targetItem?.detailName ?: "") }
    var unitPriceInput by remember { mutableStateOf(targetItem?.unitPrice?.toString() ?: "") }
    var quantityInput by remember { mutableStateOf(targetItem?.quantity?.toString() ?: "1") }
    var actualAllocatedInput by remember { mutableStateOf(targetItem?.actualAllocated?.toString() ?: "") }
    var funder by remember { mutableStateOf(targetItem?.funder ?: "默认账户") }
    var remark by remember { mutableStateOf(targetItem?.remark ?: "") }

    // 动态感知所选分类是否为收入类
    val selectedCategoryDef = remember(selectedCategory, categoryList) {
        categoryList.find { it.categoryName == selectedCategory }
    }
    val isIncome = selectedCategoryDef?.isIncome ?: false

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
                // 顶部标题（根据收入/支出动态切换）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (targetItem == null) {
                            if (isIncome) "新增收入规划项" else "新增支出预算项"
                        } else {
                            if (isIncome) "编辑收入项" else "编辑支出预算项"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIncome) MiuixGreen.copy(alpha = 0.15f) else MiuixBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isIncome) "+ 收入类" else "- 支出类",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) MiuixGreen else MiuixBlue
                        )
                    }
                }

                // 收入属性全局提示卡片
                if (isIncome) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💡 本项归属于【收入类】：保存后该金额将作为新增资金直接累加至当月资金池（总量）与可用结余中。",
                            fontSize = 12.sp,
                            color = MiuixGreen,
                            lineHeight = 17.sp
                        )
                    }
                }

                // 1. 归属大类选择（展示收入/支出类别标签）
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "选择归属类别",
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
                        categoryList.forEach { category ->
                            val isSelected = (category.categoryName == selectedCategory)
                            val catIsIncome = category.isIncome
                            Box(
                                modifier = Modifier
                                    .clip(MiuixShapes.SmallSquircle)
                                    .background(
                                        color = if (isSelected) {
                                            if (catIsIncome) MiuixGreen else MiuixBlue
                                        } else {
                                            MiuixTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .clickable {
                                        selectedCategory = category.categoryName
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = category.categoryName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                    )
                                    if (catIsIncome) {
                                        Text(
                                            text = "(入)",
                                            fontSize = 10.5.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MiuixGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 细项名称输入框
                OutlinedTextField(
                    value = detailName,
                    onValueChange = { detailName = it },
                    label = {
                        Text(
                            text = if (isIncome) "收入来源细项 (如 基本工资、绩效奖金、兼职报酬、理财收益)" else "预算细项名称 (如 房租物业、餐饮外卖、生活用品)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 3. 单价/金额与数量/频次（支持公式算式）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = unitPriceInput,
                        onValueChange = { unitPriceInput = it },
                        label = {
                            Text(
                                text = if (isIncome) "预计收入 (支持如 3000+500)" else "单价/基准额 (支持如 30+50)"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.3f),
                        shape = MiuixShapes.MediumSquircle,
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = {
                            Text(
                                text = if (isIncome) "预计频次/笔数" else "数量/月数"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f),
                        shape = MiuixShapes.MediumSquircle,
                        singleLine = true
                    )
                }

                // 4. 实际加入/到账额度
                OutlinedTextField(
                    value = actualAllocatedInput,
                    onValueChange = { actualAllocatedInput = it },
                    label = {
                        Text(
                            text = if (isIncome) {
                                "实际到账额度 (留空默认等同于预计收入，累加至总量)"
                            } else {
                                "实际加入额度 (资金注入，留空默认等同于总价预算)"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 5. 账户与渠道
                OutlinedTextField(
                    value = funder,
                    onValueChange = { funder = it },
                    label = {
                        Text(
                            text = if (isIncome) "入账收款账户 (如 工资卡、微信零钱、支付宝)" else "资金出处 / 扣款账户 (如 招商银行卡、微信零钱)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 6. 备注说明
                OutlinedTextField(
                    value = remark,
                    onValueChange = { remark = it },
                    label = {
                        Text(
                            text = if (isIncome) "收入说明备注 (选填)" else "支出说明备注 (选填)"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 7. 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (targetItem != null) {
                        Button(
                            onClick = {
                                onDelete(targetItem.itemId)
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(0.8f),
                            colors = ButtonDefaults.buttonColors(color = MiuixRed.copy(alpha = 0.15f))
                        ) {
                            Text(text = "删除", color = MiuixRed)
                        }
                    }

                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            val unitPrice = MathFormulaEvaluator.evaluate(unitPriceInput)
                            val quantity = quantityInput.toDoubleOrNull() ?: 1.0
                            val totalPrice = unitPrice * quantity
                            val actualAllocated = if (actualAllocatedInput.isNotEmpty()) {
                                MathFormulaEvaluator.evaluate(actualAllocatedInput)
                            } else {
                                totalPrice
                            }

                            if (detailName.isNotEmpty()) {
                                val newItem = BudgetItem(
                                    itemId = targetItem?.itemId ?: 0L,
                                    year = year,
                                    month = month,
                                    categoryName = selectedCategory,
                                    detailName = detailName,
                                    unitPrice = unitPrice,
                                    quantity = quantity,
                                    totalPrice = totalPrice,
                                    actualAllocated = actualAllocated,
                                    funder = funder,
                                    actualSpent = targetItem?.actualSpent ?: 0.0,
                                    balance = if (isIncome) actualAllocated else actualAllocated - (targetItem?.actualSpent ?: 0.0),
                                    remark = remark
                                )
                                onSave(newItem)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(
                            color = if (isIncome) MiuixGreen else MiuixBlue
                        )
                    ) {
                        Text(
                            text = if (isIncome) "保存收入" else "保存预算",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
