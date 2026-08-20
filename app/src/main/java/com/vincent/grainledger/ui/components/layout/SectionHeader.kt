package com.vincent.grainledger.ui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 列表/内容分区小标题栏 (SectionHeader)。
 *
 * 用于页面内部各功能卡片区域的标题提示与右侧操作项（如“查看全部”、“共 N 项”）。
 *
 * @param title 分区标题（如 "分类预算分配"、"版本发布历史"）
 * @param modifier 外部修饰符
 * @param actionSlot 右侧操作或提示插槽（可选）
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionSlot: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )

        if (actionSlot != null) {
            actionSlot()
        }
    }
}
