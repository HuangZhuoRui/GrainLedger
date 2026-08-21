package com.vincent.grainledger.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

// MIUIX 预设 8 种高颜值主题色板
private val PRESET_CATEGORY_COLORS = listOf(
    0xFF3482FFL, // 经典蓝
    0xFF34C759L, // 翡翠绿
    0xFFFF9500L, // 活力橙
    0xFFFF3B30L, // 珊瑚红
    0xFFAF52DEL, // 典雅紫
    0xFFFF2D55L, // 樱花粉
    0xFF5856D6L, // 靛蓝
    0xFF00C7BEL  // 青碧
)

/**
 * 快速创建分类弹窗 (QuickCreateCategoryDialog)。
 *
 * 专用于在“记一笔”或“加预算”弹窗中原地创建新大类，无需退出中断当前录入流：
 * 1. 记收入时：直接创建收入大类并选中；
 * 2. 记支出/加预算时：支持同时设定该新大类在当前月份的初始预算细项（信封名称与规划金额），完成分类与预算的无缝挂载。
 *
 * @param isIncomeCategory 是否为收入分类
 * @param currentYear 当前年份
 * @param currentMonth 当前月份
 * @param onSaveCategoryWithBudget 保存回调 (新分类实体, 初始细项名称, 初始预算额度)
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun QuickCreateCategoryDialog(
    isIncomeCategory: Boolean,
    currentYear: Int,
    currentMonth: Int,
    onSaveCategoryWithBudget: (
        newCategory: BudgetCategory,
        initialBudgetDetailName: String?,
        initialBudgetAmount: Double?
    ) -> Unit,
    onDismissRequest: () -> Unit
) {
    var categoryNameInput by remember { mutableStateOf("") }
    var selectedColorValue by remember {
        mutableLongStateOf(if (isIncomeCategory) 0xFF34C759L else PRESET_CATEGORY_COLORS.first())
    }

    // 支出模式下的初始预算设置
    var initialDetailName by remember { mutableStateOf("") }
    var initialBudgetAmountText by remember { mutableStateOf("") }

    val activeThemeColor = if (isIncomeCategory) MiuixGreen else MiuixBlue

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 12.dp)
                .clip(MiuixShapes.DialogSquircle)
                .background(MiuixTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部居中标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncomeCategory) "新建收入大类" else "新建支出大类与预算",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                // 1. 分类名称输入
                OutlinedTextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text(text = if (isIncomeCategory) "收入类别名称" else "支出大类名称") },
                    placeholder = { Text(text = if (isIncomeCategory) "例如：兼职外快、投资分红" else "例如：宠物生活、学习进修") },
                    singleLine = true,
                    shape = MiuixShapes.MediumSquircle,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. 主题色彩点选
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "选择大类主题色",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PRESET_CATEGORY_COLORS.forEach { colorValue ->
                            val color = Color(colorValue)
                            val isSelected = (selectedColorValue == colorValue)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorValue = colorValue }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MiuixTheme.colorScheme.onSurface, CircleShape)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 【支出类特有】设置该大类在当前月份的初始预算细项
                if (!isIncomeCategory) {
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
                            Text(
                                text = "关联 ${currentMonth}月份 初始预算信封",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "新支出大类需要至少一个预算项以供记账扣减，可直接在此设定：",
                                fontSize = 11.5.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )

                            OutlinedTextField(
                                value = initialDetailName,
                                onValueChange = { initialDetailName = it },
                                label = { Text("预算细项名称") },
                                placeholder = { Text("默认：日常支出 (或如 猫粮罐头)") },
                                singleLine = true,
                                shape = MiuixShapes.MediumSquircle,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = initialBudgetAmountText,
                                onValueChange = { initialBudgetAmountText = it },
                                label = { Text("初始规划预算额度 (选填，支持算式)") },
                                placeholder = { Text("留空默认为 0.00 ¥") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MiuixShapes.MediumSquircle,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. 底部操作按钮
                val isFormValid = categoryNameInput.isNotBlank()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val newCategory = BudgetCategory(
                                    categoryId = 0L,
                                    categoryName = categoryNameInput.trim(),
                                    isIncome = isIncomeCategory,
                                    themeColorValue = selectedColorValue
                                )

                                val detailName = if (initialDetailName.isNotBlank()) {
                                    initialDetailName.trim()
                                } else {
                                    "日常支出"
                                }

                                val budgetAmount = if (initialBudgetAmountText.isNotBlank()) {
                                    MathFormulaEvaluator.evaluate(initialBudgetAmountText)
                                } else {
                                    0.0
                                }

                                onSaveCategoryWithBudget(newCategory, detailName, budgetAmount)
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = if (isFormValid) activeThemeColor else MiuixTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (isIncomeCategory) "创建收入大类" else "创建并关联预算",
                            color = if (isFormValid) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
