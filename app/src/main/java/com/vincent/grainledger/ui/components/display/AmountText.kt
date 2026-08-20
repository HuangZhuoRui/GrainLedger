package com.vincent.grainledger.ui.components.display

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 通用金额展示组件 (AmountText)。
 *
 * 规范化格式化并呈现货币符号与金额数值。
 *
 * @param amount 金额数值
 * @param modifier 外部修饰符
 * @param color 文本颜色（默认遵循当前主题 onSurface）
 * @param fontSize 主金额字体大小
 * @param symbolFontSize 货币符号字体大小
 * @param fontWeight 字重
 * @param showSign 是否强制显示正负号（如 "+100.00" / "-50.00"）
 */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.onSurface,
    fontSize: TextUnit = 16.sp,
    symbolFontSize: TextUnit = (fontSize.value * 0.75f).sp,
    fontWeight: FontWeight = FontWeight.Bold,
    showSign: Boolean = false
) {
    val isNegative = amount < 0
    val absAmount = BigDecimal(kotlin.math.abs(amount)).setScale(2, RoundingMode.HALF_UP).toPlainString()
    val prefix = when {
        showSign && amount > 0 -> "+"
        isNegative -> "-"
        else -> ""
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color
            )
        }
        Text(
            text = "¥",
            fontSize = symbolFontSize,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = absAmount,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color
        )
    }
}
