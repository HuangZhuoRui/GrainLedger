package com.vincent.grainledger.ui.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.components.display.AmountText
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统计指标高亮卡片 (StatCard)。
 *
 * 用于仪表盘与预算总览中展示单个或成对的核心财务数值（如总预算、实际支出、结余）。
 *
 * @param title 指标标题
 * @param amount 指标金额数值
 * @param accentColor 强调高亮颜色
 * @param modifier 外部修饰符
 * @param subtitle 补充说明文本（可选）
 */
@Composable
fun StatCard(
    title: String,
    amount: Double,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        AmountText(
            amount = amount,
            color = accentColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
