package com.vincent.grainledger.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.ui.theme.MiuixRed
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用二次确认弹窗 (ConfirmDialog)。
 *
 * 用于重要/危险操作确认（如删除流水、删除预算项、清除所有数据等）。
 *
 * @param title 弹窗标题（如 "确认删除"）
 * @param message 详细说明信息
 * @param onConfirm 确认回调
 * @param onDismiss 取消/关闭回调
 * @param confirmText 确认按钮文字（默认 "确认删除"）
 * @param confirmColor 确认按钮主题色（默认 MiuixRed 警示红）
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确认删除",
    confirmColor: Color = MiuixRed
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Text(
                    text = message,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 13.5.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = confirmColor)
                    ) {
                        Text(
                            text = confirmText,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
