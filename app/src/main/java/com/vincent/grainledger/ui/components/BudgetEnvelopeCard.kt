package com.vincent.grainledger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.CategoryOverview
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 分类预算信封卡片组件。
 *
 * 展示某个大类（如强制类、饮食类）的资金注入额度、实际已消费金额、实时结余额度与进度条。
 * 点击卡片可丝滑展开查看该大类下的每一个具体预算细项明细。
 *
 * @param categoryOverview 大类聚合数据
 * @param categoryDefinition 分类样式定义
 * @param onBudgetItemClick 点击具体预算项时的回调
 */
@Composable
fun BudgetEnvelopeCard(
    categoryOverview: CategoryOverview,
    categoryDefinition: BudgetCategory?,
    onBudgetItemClick: (BudgetItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themeColor = categoryDefinition?.themeColor ?: MiuixBlue

    val usageProgress = categoryOverview.usageRatio
    val animatedProgress by animateFloatAsState(
        targetValue = usageProgress.coerceIn(0.0f, 1.0f),
        animationSpec = MiuixAnimation.springFast(),
        label = "预算进度条动画"
    )

    val progressBarColor = when {
        usageProgress >= 1.0f -> MiuixRed
        usageProgress >= 0.85f -> MiuixOrange
        else -> MiuixGreen
    }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 头部：类别名称、图标与展开箭头
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
                        Text(
                            text = categoryOverview.categoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "已消费 ¥${MathFormulaEvaluator.formatAmount(categoryOverview.categoryActualSpent)} / 额度 ¥${MathFormulaEvaluator.formatAmount(categoryOverview.categoryActualAllocated)}",
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
                            text = "剩余额度",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Text(
                            text = "¥${MathFormulaEvaluator.formatAmount(categoryOverview.categoryBalance)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (categoryOverview.categoryBalance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
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

            // 进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MiuixShapes.PillShape)
                    .background(MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(MiuixShapes.PillShape)
                        .background(progressBarColor)
                )
            }

            // 展开折叠的具体细项列表
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOverview.budgetItemList.forEach { budgetItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = MiuixShapes.MediumSquircle
                                )
                                .clickable {
                                    onBudgetItemClick(budgetItem)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = budgetItem.detailName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (budgetItem.quantity > 1.0) {
                                        "单价 ¥${MathFormulaEvaluator.formatAmount(budgetItem.unitPrice)} × ${budgetItem.quantity}"
                                    } else {
                                        "预算总价 ¥${MathFormulaEvaluator.formatAmount(budgetItem.totalPrice)}"
                                    },
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "剩余 ¥${MathFormulaEvaluator.formatAmount(budgetItem.balance)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (budgetItem.balance < 0) MiuixRed else MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "已花 ¥${MathFormulaEvaluator.formatAmount(budgetItem.actualSpent)} / 加入 ¥${MathFormulaEvaluator.formatAmount(budgetItem.actualAllocated)}",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
