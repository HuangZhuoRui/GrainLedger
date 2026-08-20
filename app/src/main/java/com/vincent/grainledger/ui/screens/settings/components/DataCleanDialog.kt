package com.vincent.grainledger.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixPurple
import com.vincent.grainledger.ui.theme.MiuixRed
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 数据清理类型枚举。
 */
enum class CleanTargetType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val themeColor: Color
) {
    TRANSACTIONS(
        title = "仅清空流水记录",
        description = "删除所有月份的支出与收入流水明细，已消费金额归零并恢复预算可用结余，保留所有预算细项规划与分类",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        themeColor = MiuixOrange
    ),
    BUDGETS(
        title = "仅清空预算规划",
        description = "删除所有月份的预算细项与分配额度，保留现有的预算分类体系与历史流水记录",
        icon = Icons.Default.AccountBalanceWallet,
        themeColor = MiuixPurple
    ),
    ALL(
        title = "彻底清空全部数据",
        description = "彻底删除所有月份预算、全部流水记录与自定义分类体系，恢复为空白账本状态",
        icon = Icons.Default.DeleteForever,
        themeColor = MiuixRed
    )
}

/**
 * 细分数据清理弹窗 (DataCleanDialog)。
 *
 * 支持用户精准选择仅清理流水、仅清理预算或清空全部数据。
 *
 * @param onConfirmClean 确认清理回调 (目标清理类型)
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun DataCleanDialog(
    onConfirmClean: (CleanTargetType) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedType by remember { mutableStateOf(CleanTargetType.TRANSACTIONS) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(26.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 标题与说明
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "数据清理",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "请选择需要清理的数据范围，此操作不可逆：",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                // 2. 选项列表
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CleanTargetType.entries.forEach { cleanType ->
                        val isSelected = selectedType == cleanType
                        val itemColor = cleanType.themeColor

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) itemColor.copy(alpha = 0.08f)
                                    else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) itemColor else MiuixTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedType = cleanType }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(itemColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cleanType.icon,
                                        contentDescription = null,
                                        tint = itemColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = cleanType.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) itemColor else MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = cleanType.description,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(itemColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3. 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "取消",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            onConfirmClean(selectedType)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = selectedType.themeColor)
                    ) {
                        Text(
                            text = "确认清理",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
