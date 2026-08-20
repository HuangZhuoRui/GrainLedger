package com.vincent.grainledger.ui.components.card

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.IncomeCategoryOverview
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收入分类与入账概览卡片 (IncomeEnvelopeCard)。
 *
 * 在综合看板中展示收入大类的累计入账金额、笔数，支持展开查看该分类下每一笔具体的收入流水。
 *
 * @param incomeOverview 收入大类汇总模型
 * @param categoryDefinition 分类定义
 * @param onTransactionClick 点击具体入账流水的事件回调
 * @param modifier 外部修饰符
 */
@Composable
fun IncomeEnvelopeCard(
    incomeOverview: IncomeCategoryOverview,
    categoryDefinition: BudgetCategory?,
    onTransactionClick: (TransactionRecord) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themeColor = categoryDefinition?.themeColor ?: MiuixGreen

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isExpanded = !isExpanded
            },
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 头部：收入分类名称、笔数与累计到账金额
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(themeColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(themeColor, CircleShape)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = incomeOverview.categoryName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MiuixGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "+ 收入类",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixGreen
                                )
                            }
                        }
                        Text(
                            text = if (incomeOverview.transactionCount > 0) {
                                "当月共入账 ${incomeOverview.transactionCount} 笔流水"
                            } else {
                                "本月暂无入账记录，点击右上角快速记入"
                            },
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "累计到账",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Text(
                            text = "+${MathFormulaEvaluator.formatAmount(incomeOverview.totalIncome)} ¥",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixGreen
                        )
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 展开后的单笔入账流水明细
            AnimatedVisibility(visible = isExpanded) {
                if (incomeOverview.transactionList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MiuixShapes.MediumSquircle
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "本月暂无该分类的入账记录",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        incomeOverview.transactionList.forEach { transaction ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = MiuixShapes.MediumSquircle
                                    )
                                    .clickable {
                                        onTransactionClick(transaction)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = transaction.detailName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${transaction.month}月${transaction.day}日 · 收款: ${transaction.funder}${if (transaction.remark.isNotBlank()) " (${transaction.remark})" else ""}",
                                        fontSize = 11.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }

                                Text(
                                    text = "+${MathFormulaEvaluator.formatAmount(transaction.amount)} ¥",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
