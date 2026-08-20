package com.vincent.grainledger.ui.components.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixBlue
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 双通道/多操作弹性按钮行 (ActionButtonRow)。
 *
 * 采用弹性均分宽度布局（Modifier.weight(1f)）并配置适宜的内部边距，
 * 彻底杜绝并排多按钮在不同屏幕尺寸下的挤压与文字截断问题。
 *
 * @param primaryText 主按钮文字（如 "加速下载"）
 * @param onPrimaryClick 主按钮点击回调
 * @param secondaryText 次按钮文字（如 "正常下载"）
 * @param onSecondaryClick 次按钮点击回调
 * @param modifier 外部修饰符
 * @param primaryColor 主按钮背景色
 * @param secondaryColor 次按钮背景色
 * @param spacing 按钮间距
 */
@Composable
fun ActionButtonRow(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryColor: Color = MiuixBlue,
    secondaryColor: Color = MiuixTheme.colorScheme.surfaceVariant,
    spacing: Dp = 10.dp
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        // 主操作按钮
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f),
            insideMargin = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(color = primaryColor)
        ) {
            Text(
                text = primaryText,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        // 次操作按钮
        Button(
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f),
            insideMargin = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(color = secondaryColor)
        ) {
            Text(
                text = secondaryText,
                fontSize = 13.sp,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
