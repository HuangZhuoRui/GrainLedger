package com.vincent.grainledger.ui.components.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用缺省空白占位视图 (EmptyStateView)。
 *
 * 用于在列表无数据（如无流水、无历史发布、无预算等）时向用户呈现友好提示。
 *
 * @param title 提示标题（如 "暂无记账流水"）
 * @param message 补充提示文本（如 "点击下方按钮记录第一笔开支吧"）
 * @param icon 提示图标
 * @param modifier 外部修饰符
 * @param actionSlot 底部引导操作插槽（可选）
 */
@Composable
fun EmptyStateView(
    title: String,
    message: String? = null,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier,
    actionSlot: @Composable (() -> Unit)? = null
) {
    MiuixSectionCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )

            if (!message.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 12.5.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

            if (actionSlot != null) {
                Spacer(modifier = Modifier.height(14.dp))
                actionSlot()
            }
        }
    }
}
