package com.vincent.grainledger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.util.MathFormulaEvaluator

/**
 * 丝滑非线性数字滚动展示组件。
 *
 * 结合 MIUI 非线性贝塞尔动画曲线与数字上下滚动过渡，
 * 当金额发生变化时呈现丝滑的动画效果。
 *
 * @param amountValue 需要展示的金额数值
 * @param prefixSymbol 前缀（默认 "¥"）
 * @param textStyle 文字样式
 * @param color 字体颜色
 * @param modifier 修饰符
 */
@Composable
fun AnimatedAmountDisplay(
    amountValue: Double,
    modifier: Modifier = Modifier,
    prefixSymbol: String = "¥",
    textStyle: TextStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    color: Color = Color.Unspecified
) {
    val formattedText = MathFormulaEvaluator.formatAmount(amountValue, 2)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefixSymbol.isNotEmpty()) {
            Text(
                text = prefixSymbol,
                style = textStyle.copy(fontSize = (textStyle.fontSize.value * 0.75f).sp),
                color = color
            )
        }

        AnimatedContent(
            targetState = formattedText,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = MiuixAnimation.springFast()
                ) { height -> height / 2 } + fadeIn(animationSpec = MiuixAnimation.springFast()))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = MiuixAnimation.springFast()
                        ) { height -> -height / 2 } + fadeOut(animationSpec = MiuixAnimation.springFast())
                    )
            },
            label = "金额滚动动画"
        ) { text ->
            Text(
                text = text,
                style = textStyle,
                color = color
            )
        }
    }
}
