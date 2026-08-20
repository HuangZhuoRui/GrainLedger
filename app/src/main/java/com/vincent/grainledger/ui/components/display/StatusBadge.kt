package com.vincent.grainledger.ui.components.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixBlue

/**
 * 通用状态徽章组件 (StatusBadge)。
 *
 * 用于高亮呈现状态标签（如 "当前运行"、"正式发行版"、"超支"、"结余" 等）。
 *
 * @param text 徽章文字
 * @param color 主题强调色
 * @param modifier 外部修饰符
 * @param fontSize 字体大小
 * @param cornerRadius 圆角半径
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color = MiuixBlue,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.5.sp,
    cornerRadius: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
