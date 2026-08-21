package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算 Step 2: 预算总额核算卡片 (EditBudgetAmountStep)。
 *
 * 包含：
 * 1. 预算总额实时核算看板（单价 × 数量/月数）；
 * 2. 单价/基准额输入框（支持即时算式计算）；
 * 3. 数量/月数步进增减调节器；
 * 4. 快捷算术工具栏 (+, -, ×, ÷, C, ⌫)。
 */
@Composable
fun EditBudgetAmountStep(
    totalBudgetCalculated: Double,
    unitPriceInput: String,
    onUnitPriceChange: (String) -> Unit,
    quantityInput: String,
    onQuantityChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                        onValueChange = onUnitPriceChange,
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
                                            onQuantityChange(if (next % 1.0 == 0.0) next.toInt().toString() else next.toString())
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
                                    onValueChange = onQuantityChange,
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
                                        onQuantityChange(if (next % 1.0 == 0.0) next.toInt().toString() else next.toString())
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
                                    onUnitPriceChange(unitPriceInput + op)
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
                            .clickable { onUnitPriceChange("") },
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
                                    onUnitPriceChange(unitPriceInput.dropLast(1))
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
