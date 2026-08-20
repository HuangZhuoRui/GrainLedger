package com.vincent.grainledger.ui.components.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用基础分组卡片 (MiuixSectionCard)。
 *
 * 统一应用中各功能模块卡片的圆角弧度、外边距、内边距与背景样式。
 *
 * @param modifier 外部修饰符（默认带水平 16.dp 外边距）
 * @param cornerRadius 圆角半径（默认 20.dp）
 * @param contentPadding 内部内边距（默认 16.dp）
 * @param backgroundColor 背景颜色（默认遵循当前主题的卡片背景色）
 * @param onClick 点击事件回调（可选）
 * @param content 卡片内部内容插槽
 */
@Composable
fun MiuixSectionCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    backgroundColor: Color = MiuixTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        cornerRadius = cornerRadius
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}
