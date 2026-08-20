package com.vincent.grainledger.ui.components.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * MIUIX 风格月份胶囊选择条 (MiuixMonthSelector)。
 *
 * 支持水平流畅滑动，选中月份带有平滑弹性缩放与色彩渐变过渡，
 * 尾部支持一键弹出新建月份账本入口。
 *
 * @param availableMonthList 可选择的年月列表 (Pair<年份, 月份>)
 * @param currentYear 当前选中的年份
 * @param currentMonth 当前选中的月份
 * @param onMonthSelected 切换月份时的回调函数
 * @param onAddMonthClick 新增月份点击回调（若为 null 则不展示添加按钮）
 * @param modifier 外部修饰符
 */
@Composable
fun MiuixMonthSelector(
    availableMonthList: List<Pair<Int, Int>>,
    currentYear: Int,
    currentMonth: Int,
    onMonthSelected: (Int, Int) -> Unit,
    onAddMonthClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableMonthList.forEach { (year, month) ->
            val isSelected = (year == currentYear && month == currentMonth)

            val scaleRatio by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1.0f,
                animationSpec = MiuixAnimation.springBouncy(),
                label = "胶囊缩放"
            )

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MiuixBlue
                } else {
                    MiuixTheme.colorScheme.surface
                },
                animationSpec = MiuixAnimation.springSmooth(),
                label = "背景色渐变"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    Color.White
                } else {
                    MiuixTheme.colorScheme.onSurface
                },
                animationSpec = MiuixAnimation.springSmooth(),
                label = "文字颜色渐变"
            )

            Box(
                modifier = Modifier
                    .scale(scaleRatio)
                    .background(
                        color = backgroundColor,
                        shape = MiuixShapes.MediumSquircle
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onMonthSelected(year, month)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${year}年${month}月",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }

        if (onAddMonthClick != null) {
            Box(
                modifier = Modifier
                    .background(
                        color = MiuixTheme.colorScheme.surfaceVariant,
                        shape = MiuixShapes.MediumSquircle
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onAddMonthClick()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新增月份",
                        tint = MiuixBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "加月份",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixBlue
                    )
                }
            }
        }
    }
}
