package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.theme.horizontalFadingEdge
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算 Step 3: 资金池注入规划、出资账户与备注卡片 (EditBudgetAllocationStep)。
 *
 * 包含：
 * 1. 实际注入资金额度设定（支持一键 100% 等同总额）；
 * 2. 注入比例动态进度条与全额/部分注入状态提示；
 * 3. 资金出处/扣款账户输入与快捷标签滑轨；
 * 4. 备注说明输入；
 * 5. 预算总览小结预览卡片。
 */
@Composable
fun EditBudgetAllocationStep(
    totalBudgetCalculated: Double,
    actualAllocatedInput: String,
    onActualAllocatedChange: (String) -> Unit,
    actualAllocatedEvaluated: Double,
    allocationRatio: Float,
    animatedRatio: Float,
    funder: String,
    onFunderChange: (String) -> Unit,
    remark: String,
    onRemarkChange: (String) -> Unit,
    selectedCategory: String,
    detailName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                            if (totalBudgetCalculated > 0.0) {
                                onActualAllocatedChange(MathFormulaEvaluator.formatAmount(totalBudgetCalculated))
                            } else {
                                onActualAllocatedChange("")
                            }
                        }
                    ) {
                        Text(text = "100% 等同总额", fontSize = 11.5.sp, color = MiuixBlue)
                    }
                }

                OutlinedTextField(
                    value = actualAllocatedInput,
                    onValueChange = onActualAllocatedChange,
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
                onValueChange = onFunderChange,
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
                    .horizontalFadingEdge(14.dp)
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
                            .clickable { onFunderChange(acc) }
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
            onValueChange = onRemarkChange,
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
