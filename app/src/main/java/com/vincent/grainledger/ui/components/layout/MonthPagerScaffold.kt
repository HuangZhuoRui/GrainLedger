package com.vincent.grainledger.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.absoluteValue

/**
 * 通用月份大卡片页面脚手架 (MonthPagerScaffold)。
 *
 * 为【看板】、【预算】、【流水】三大核心页面提供统一的顶级视觉与交互架构：
 * 1. 顶部自适应月份进度轴：当前选中月份最大且绝对居中，两侧月份等比缩小淡化；
 * 2. 屏幕边缘渐变羽化遮罩：两端平滑淡出，消除生硬截断割裂感；
 * 3. 线性缩放平移大卡片：左右滑动时当前卡片线性缩小、新卡片逐渐放大、归位刚好满尺寸；
 * 4. 一次修改，三大页面同步生效。
 *
 * @param availableMonths 可用月份列表 (Pair<年份, 月份>)
 * @param currentYear 当前年份
 * @param currentMonth 当前月份
 * @param pageTitle 页面主标题（如 "余粮 · 看板"、"预算规划"、"账单流水"）
 * @param subtitle 页面副标题说明
 * @param headerActionSlot 顶部标题栏右侧自定义操作插槽
 * @param onMonthSelected 切换月份回调
 * @param onAddMonthClick 新增月份点击回调
 * @param floatingActionButton 悬浮操作按钮插槽 (FAB)
 * @param dialogs 弹窗组件插槽
 * @param content 各月份卡片内部具体内容的渲染函数 (targetYear, targetMonth)
 */
@Composable
fun MonthPagerScaffold(
    availableMonths: List<Pair<Int, Int>>,
    currentYear: Int,
    currentMonth: Int,
    pageTitle: String,
    subtitle: String? = null,
    headerActionSlot: @Composable (() -> Unit)? = null,
    onMonthSelected: (Int, Int) -> Unit,
    onAddMonthClick: (() -> Unit)? = null,
    floatingActionButton: @Composable (() -> Unit)? = null,
    dialogs: @Composable (() -> Unit)? = null,
    content: @Composable (targetYear: Int, targetMonth: Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // 确保有可用的月份列表
    val safeMonths = remember(availableMonths) {
        if (availableMonths.isNotEmpty()) availableMonths else listOf(Pair(currentYear, currentMonth))
    }

    val currentMonthIndex = remember(safeMonths, currentYear, currentMonth) {
        val idx = safeMonths.indexOf(Pair(currentYear, currentMonth))
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = currentMonthIndex.coerceIn(0, safeMonths.size - 1)
    ) {
        safeMonths.size
    }

    val monthListState = rememberLazyListState()

    // 监听当前外部选择的月份变更，联动同步滑动 Pager
    LaunchedEffect(currentMonthIndex) {
        if (pagerState.currentPage != currentMonthIndex) {
            pagerState.animateScrollToPage(
                page = currentMonthIndex,
                animationSpec = MiuixAnimation.springSmooth()
            )
        }
    }

    // 监听 Pager 滑动结束，向外通知选中月份
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage in safeMonths.indices) {
            val (sYear, sMonth) = safeMonths[pagerState.settledPage]
            if (sYear != currentYear || sMonth != currentMonth) {
                onMonthSelected(sYear, sMonth)
            }
        }
    }

    // 联动顶部月份列表自动滚动至居中
    LaunchedEffect(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        monthListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = -((screenWidth.value / 2) - 60).toInt()
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp)
        ) {
            // 1. 顶部标题栏区域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pageTitle,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MiuixTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (headerActionSlot != null) {
                    headerActionSlot()
                }
            }

            // 2. 顶部自适应月份进度轴（带两端渐变羽化遮罩与居中缩放）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // 左侧边缘羽化淡出
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.Black),
                                startX = 0f,
                                endX = 54.dp.toPx()
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        // 右侧边缘羽化淡出
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Transparent),
                                startX = size.width - 54.dp.toPx(),
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                LazyRow(
                    state = monthListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = (screenWidth / 2) - 45.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(safeMonths.size) { index ->
                        val (year, month) = safeMonths[index]

                        // 计算当前项与 Pager 滚动进度的距离，驱动实时缩放与透明度
                        val distance by remember(pagerState) {
                            derivedStateOf {
                                val currentOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                                (currentOffset - index).absoluteValue
                            }
                        }

                        val scale = (1.0f - (distance * 0.22f)).coerceIn(0.75f, 1.0f)
                        val alpha = (1.0f - (distance * 0.45f)).coerceIn(0.38f, 1.0f)
                        val isCurrent = distance < 0.5f

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .clip(MiuixShapes.SmallSquircle)
                                .background(
                                    if (isCurrent) MiuixBlue.copy(alpha = 0.12f) else Color.Transparent
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = index,
                                            animationSpec = MiuixAnimation.springSmooth()
                                        )
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${year}年${month}月",
                                fontSize = if (isCurrent) 17.sp else 14.sp,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCurrent) MiuixBlue else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 右侧一键加月份快捷入口
                    if (onAddMonthClick != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(MiuixShapes.SmallSquircle)
                                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        onAddMonthClick()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "加月份",
                                        tint = MiuixBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "加月份",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 下方全屏独立大卡片与线性缩放切换容器 (Linear Scaling Page Transform)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                beyondViewportPageCount = 1,
                pageSpacing = 10.dp,
                userScrollEnabled = true
            ) { pageIndex ->
                val (targetYear, targetMonth) = safeMonths[pageIndex]

                // 计算线性缩放因子：当前页面退出时线性缩小至 0.92f，新页面放大至 1.0f 满尺寸
                val pageOffset by remember(pagerState) {
                    derivedStateOf {
                        val currentOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        (currentOffset - pageIndex).absoluteValue
                    }
                }

                val cardScale = (1.0f - (pageOffset * 0.08f)).coerceIn(0.92f, 1.0f)
                val cardAlpha = (1.0f - (pageOffset * 0.15f)).coerceIn(0.85f, 1.0f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = cardScale
                            scaleY = cardScale
                            alpha = cardAlpha
                        }
                ) {
                    content(targetYear, targetMonth)
                }
            }
        }

        // 4. 悬浮操作按钮 (FAB)
        if (floatingActionButton != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 24.dp)
            ) {
                floatingActionButton()
            }
        }

        // 5. 挂载弹窗插槽
        if (dialogs != null) {
            dialogs()
        }
    }
}
