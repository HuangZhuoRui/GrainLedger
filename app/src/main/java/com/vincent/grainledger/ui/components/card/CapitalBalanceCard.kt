package com.vincent.grainledger.ui.components.card

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BalanceCheckResult
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 资金池配平健康状态卡片（对应 Excel《草稿页》）。
 *
 * 实时监测目标启动资金与所有预算项实际注入资金的差额，
 * 帮助用户在制定月度预算时确保资金完全平衡。
 *
 * @param balanceCheckResult 检查结果模型
 * @param modifier 外部修饰符
 */
@Composable
fun CapitalBalanceCard(
    balanceCheckResult: BalanceCheckResult,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when {
            balanceCheckResult.isBalanced -> MiuixGreen
            balanceCheckResult.isOverAllocated -> MiuixRed
            else -> MiuixBlue
        },
        animationSpec = MiuixAnimation.springSmooth(),
        label = "配平状态颜色"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(statusColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (balanceCheckResult.isBalanced) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "资金池配平校验",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = MiuixShapes.SmallSquircle
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            balanceCheckResult.isBalanced -> "已完美配平"
                            balanceCheckResult.isOverAllocated -> "超额分配"
                            else -> "待分配结余"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "目标资金池基准",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "¥${MathFormulaEvaluator.formatAmount(balanceCheckResult.targetBenchmarkFund)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "各项已分配",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "¥${MathFormulaEvaluator.formatAmount(balanceCheckResult.allocatedTotalFund)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = "当前配平差额",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "¥${MathFormulaEvaluator.formatAmount(balanceCheckResult.balanceDifference)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }
        }
    }
}
