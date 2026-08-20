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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算细项新增与编辑弹窗。
 *
 * 支持设定单价、数量、总价预算与实际加入（资金注入）额度，
 * 支持在单价或总额输入框中直接输入算式（例如 30+50、39+26+5+5）。
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

    OverlayDialog(
        show = true,
        onDismissRequest = onDismissRequest,
        title = if (targetItem == null) "新增预算项" else "编辑预算项"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. 归属分类选择
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "归属类别",
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
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant,
                                    shape = MiuixShapes.SmallSquircle
                                )
                                .clickable { selectedCategory = category.categoryName }
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

            // 2. 详情名称
            OutlinedTextField(
                value = detailName,
                onValueChange = { detailName = it },
                label = { Text(text = "预算细项名称 (如日常吃/学费)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MiuixShapes.MediumSquircle,
                singleLine = true
            )

            // 3. 单价与数量（两列排布）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = unitPriceInput,
                    onValueChange = { unitPriceInput = it },
                    label = { Text(text = "单价/定额") },
                    placeholder = { Text(text = "如 30 或 30+50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = { quantityInput = it },
                    label = { Text(text = "数量/天数") },
                    placeholder = { Text(text = "如 30 或 4.2") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )
            }

            // 4. 实际加入金额（资金分配）
            OutlinedTextField(
                value = actualAllocatedInput,
                onValueChange = { actualAllocatedInput = it },
                label = { Text(text = "实际加入(注入资金，默认等于总价)") },
                placeholder = { Text(text = "留空则自动按单价×数量计算") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = MiuixShapes.MediumSquircle,
                singleLine = true
            )

            // 5. 备注说明
            OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                label = { Text(text = "备注说明 (选填)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MiuixShapes.MediumSquircle,
                singleLine = true
            )

            // 6. 操作按钮栏
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
                                balance = actualAllocated - (targetItem?.actualSpent ?: 0.0),
                                remark = remark
                            )
                            onSave(newItem)
                            onDismissRequest()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                ) {
                    Text(text = "保存预算", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
