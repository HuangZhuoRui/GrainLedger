package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算编辑/新增弹窗顶部步骤指示轴与动态标题组件 (EditBudgetHeader)。
 *
 * 核心动效：
 * 1. 顶部步骤指示胶囊具备 springBouncy 弹性缩放 (1.08x vs 0.94x) 与背景色彩渐变；
 * 2. 步骤标题采用与页面同向的左右横向滑移与 3D 缩放淡入淡出联动动效 (Slide + Scale + Fade)。
 */
@Composable
fun EditBudgetHeader(
    pagerState: PagerState,
    targetItem: BudgetItem?,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 步骤指示胶囊条 (1 / 2 / 3) 带物理弹性缩放与平滑过渡
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("1 细项", "2 核算", "3 注资").forEachIndexed { index, title ->
                val isCurrent = (pagerState.currentPage == index)
                val isCompleted = (pagerState.currentPage > index)

                val pillScale by animateFloatAsState(
                    targetValue = if (isCurrent) 1.08f else 0.94f,
                    animationSpec = MiuixAnimation.springBouncy(),
                    label = "BudgetPillScale"
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (isCurrent) 1f else if (isCompleted) 0.85f else 0.55f,
                    animationSpec = MiuixAnimation.springSmooth(),
                    label = "BudgetPillAlpha"
                )
                val pillBgColor by animateColorAsState(
                    targetValue = when {
                        isCurrent -> MiuixBlue
                        isCompleted -> MiuixBlue.copy(alpha = 0.22f)
                        else -> MiuixTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(220),
                    label = "BudgetPillBg"
                )

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                            this.alpha = pillAlpha
                        }
                        .clip(MiuixShapes.PillShape)
                        .background(pillBgColor)
                        .clickable { onStepClick(index) }
                        .padding(horizontal = 12.dp, vertical = 4.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MiuixBlue,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isCurrent -> Color.White
                                isCompleted -> MiuixBlue
                                else -> MiuixTheme.colorScheme.onSurfaceSecondary
                            }
                        )
                    }
                }
            }
        }

        // 步骤主副标题 (3D 缩放 + 淡入淡出动画，杜绝生硬与文字截断)
        val (stepTitle, stepSubtitle) = when (pagerState.currentPage) {
            0 -> Pair(
                if (targetItem == null) "新增支出预算 · 细项归属" else "编辑支出预算 · 细项归属",
                "选择归属支出大类与预算细项名称"
            )
            1 -> Pair(
                "预算核算 · 基准与数量",
                "设定单价基准与数量月数，实时核算预算总额"
            )
            else -> Pair(
                "资金注入 · 账户与备注",
                "规划实际注入资金额度、出资账户与备注"
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = Triple(pagerState.currentPage, stepTitle, stepSubtitle),
                transitionSpec = {
                    val isForward = targetState.first >= initialState.first
                    val slideInOffset = if (isForward) { width: Int -> (width * 0.40f).toInt() } else { width: Int -> -(width * 0.40f).toInt() }
                    val slideOutOffset = if (isForward) { width: Int -> -(width * 0.40f).toInt() } else { width: Int -> (width * 0.40f).toInt() }

                    (
                        (slideInHorizontally(
                            initialOffsetX = slideInOffset,
                            animationSpec = tween(280, easing = MiuixAnimation.MiuixDecelerateEasing)
                        ) + scaleIn(
                            initialScale = 0.90f,
                            animationSpec = tween(280, easing = MiuixAnimation.MiuixDecelerateEasing)
                        ) + fadeIn(
                            animationSpec = tween(240)
                        )) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = slideOutOffset,
                            animationSpec = tween(220, easing = MiuixAnimation.MiuixFluidEasing)
                        ) + scaleOut(
                            targetScale = 0.90f,
                            animationSpec = tween(220, easing = MiuixAnimation.MiuixFluidEasing)
                        ) + fadeOut(
                            animationSpec = tween(200)
                        ))
                    ).using(SizeTransform(clip = false))
                },
                contentAlignment = Alignment.Center,
                label = "BudgetHorizontalSlideScaleTitleAnimation"
            ) { (_, targetTitle, targetSubtitle) ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = targetTitle,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false
                    )

                    Text(
                        text = targetSubtitle,
                        fontSize = 11.5.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
