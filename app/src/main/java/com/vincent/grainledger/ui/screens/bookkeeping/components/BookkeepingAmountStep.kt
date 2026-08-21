package com.vincent.grainledger.ui.screens.bookkeeping.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记账 Step 1: 金额与算式输入步骤卡片 (BookkeepingAmountStep)。
 *
 * 包含：
 * 1. 支出 / 收入 物理胶囊切换器；
 * 2. 大字号金额输入与实时公式计算结果预览；
 * 3. 快捷算术工具条 (+, -, ×, ÷, C, ⌫)。
 */
@Composable
fun BookkeepingAmountStep(
    isIncomeMode: Boolean,
    onIncomeModeChange: (Boolean) -> Unit,
    amountInputText: String,
    onAmountChange: (String) -> Unit,
    evaluatedAmount: Double,
    hasFormula: Boolean,
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 支出 / 收入 物理弹簧分段胶囊
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MiuixShapes.PillShape)
                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 支出 Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MiuixShapes.PillShape)
                    .background(
                        if (!isIncomeMode) MiuixRed.copy(alpha = 0.14f) else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onIncomeModeChange(false)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (!isIncomeMode) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "支出消费",
                        fontSize = 13.5.sp,
                        fontWeight = if (!isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isIncomeMode) MiuixRed else MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 收入 Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MiuixShapes.PillShape)
                    .background(
                        if (isIncomeMode) MiuixGreen.copy(alpha = 0.16f) else Color.Transparent
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onIncomeModeChange(true)
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "收入入账",
                        fontSize = 13.5.sp,
                        fontWeight = if (isIncomeMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isIncomeMode) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }

        // Hero 金额输入卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncomeMode) "入账金额" else "支出金额",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )

                    if (hasFormula) {
                        Box(
                            modifier = Modifier
                                .clip(MiuixShapes.SmallSquircle)
                                .background(activeThemeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "= ${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeColor
                            )
                        }
                    }
                }

                // 大字号输入框与货币符号
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (amountInputText.isEmpty()) {
                            Text(
                                text = "0.00",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.35f)
                            )
                        }
                        BasicTextField(
                            value = amountInputText,
                            onValueChange = onAmountChange,
                            textStyle = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = activeThemeColor
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            cursorBrush = SolidColor(activeThemeColor),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = " ¥",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeThemeColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // 快捷算术微工具条 (+, -, ×, ÷, C, ⌫)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                                .height(30.dp)
                                .clip(MiuixShapes.SmallSquircle)
                                .background(MiuixTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    if (amountInputText.isNotEmpty() && !amountInputText.endsWith("+") &&
                                        !amountInputText.endsWith("-") && !amountInputText.endsWith("*") && !amountInputText.endsWith("/")
                                    ) {
                                        onAmountChange(amountInputText + op)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayOp,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 清空 C
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .clickable { onAmountChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }

                    // 退格 ⌫
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .clickable {
                                if (amountInputText.isNotEmpty()) {
                                    onAmountChange(amountInputText.dropLast(1))
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "退格",
                            tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
