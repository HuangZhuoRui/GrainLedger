package com.vincent.grainledger.ui.components.display

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.vincent.grainledger.ui.theme.MiuixAnimation

/**
 * 通用数字平滑滚动计数器 (AnimatedCounter)。
 *
 * 当传入数字发生增减变化时，以垂直滑入滑出动效呈现数字变动。
 *
 * @param count 目标数字
 * @param modifier 外部修饰符
 * @param style 文字排版样式
 * @param color 字体颜色
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified
) {
    var oldCount by remember { mutableIntStateOf(count) }

    SideEffect {
        oldCount = count
    }

    Row(modifier = modifier) {
        val countString = count.toString()
        val oldCountString = oldCount.toString()

        for (i in countString.indices) {
            val oldChar = oldCountString.getOrNull(i)
            val newChar = countString[i]
            val char = if (oldChar == newChar) oldCountString[i] else countString[i]

            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically(animationSpec = MiuixAnimation.springSmooth()) { -it }
                            .togetherWith(slideOutVertically(animationSpec = MiuixAnimation.springSmooth()) { it })
                    } else {
                        slideInVertically(animationSpec = MiuixAnimation.springSmooth()) { it }
                            .togetherWith(slideOutVertically(animationSpec = MiuixAnimation.springSmooth()) { -it })
                    }
                },
                label = "AnimatedCounterDigit"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}
