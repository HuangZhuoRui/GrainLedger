package com.vincent.grainledger.ui.screens.updater.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 结构化更新日志分类展示块 (ChangelogSection)。
 *
 * 采用纯文字标记（【新增特性】、【问题修复】、【优化改进】）优雅排版，不包含图标。
 *
 * @param title 分类标题（如 "新增特性"）
 * @param accentColor 强调色
 * @param items 该分类下的条目列表
 * @param modifier 外部修饰符
 */
@Composable
fun ChangelogSection(
    title: String,
    accentColor: Color,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "【$title】",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        items.forEach { line ->
            Text(
                text = "• $line",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
