package com.vincent.grainledger.ui.screens.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 新建月份账本弹窗 (CreateMonthDialog)。
 *
 * 支持选择目标年份与月份，并提供一键从当前月份复制克隆预算结构（智能继承）的能力。
 *
 * @param currentYear 当前已选年份
 * @param currentMonth 当前已选月份
 * @param availableMonths 当前已存在的全部月份列表 (Pair<年份, 月份>)
 * @param onCreateMonth 确认创建回调 (目标年份, 目标月份, 是否复制预算)
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun CreateMonthDialog(
    currentYear: Int,
    currentMonth: Int,
    availableMonths: List<Pair<Int, Int>>,
    onCreateMonth: (targetYear: Int, targetMonth: Int, copyBudget: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    // 默认目标年份为当前年份，若当前年份月份已满则默认下一年
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    // 默认目标月份优先推荐未创建的下一个月份
    val nextSuggestedMonth = remember(currentYear, currentMonth, availableMonths) {
        val next = currentMonth + 1
        if (next <= 12 && !availableMonths.contains(Pair(currentYear, next))) {
            next
        } else {
            (1..12).firstOrNull { !availableMonths.contains(Pair(currentYear, it)) } ?: 1
        }
    }
    var selectedMonth by remember { mutableIntStateOf(nextSuggestedMonth) }
    var copyBudgetFromCurrent by remember { mutableStateOf(true) }

    val yearList = remember(currentYear) {
        ((currentYear - 1)..(currentYear + 4)).toList()
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(MiuixTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MiuixBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "新建月份账本",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "选择目标年月，开启全新月份预算与记账周期",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                // 1. 年份选择栏
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "选择年份",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        yearList.forEach { yr ->
                            val isSelected = (yr == selectedYear)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant,
                                        shape = MiuixShapes.SmallSquircle
                                    )
                                    .clickable {
                                        selectedYear = yr
                                        val firstAvailable = (1..12).firstOrNull { !availableMonths.contains(Pair(yr, it)) }
                                        if (firstAvailable != null) {
                                            selectedMonth = firstAvailable
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "${yr}年",
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. 月份选择网格 (1~12月)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "选择月份",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    // 12 个月份按 4 列排布
                    val months = (1..12).toList()
                    months.chunked(4).forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEach { m ->
                                val isExisting = availableMonths.contains(Pair(selectedYear, m))
                                val isSelected = (m == selectedMonth) && !isExisting

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = when {
                                                isExisting -> MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                isSelected -> MiuixBlue
                                                else -> MiuixTheme.colorScheme.surfaceVariant
                                            },
                                            shape = MiuixShapes.SmallSquircle
                                        )
                                        .clickable(enabled = !isExisting) { selectedMonth = m }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${m}月",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isExisting -> MiuixTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                                isSelected -> Color.White
                                                else -> MiuixTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (isExisting) {
                                            Text(
                                                text = "已存在",
                                                fontSize = 9.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. 从当前月复制预算配置 Switch 开关
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 14.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MiuixGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "复制当前月预算规划",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "将 ${currentYear}年${currentMonth}月 的所有预算项克隆至新月份，支出清零",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }

                        Switch(
                            checked = copyBudgetFromCurrent,
                            onCheckedChange = { copyBudgetFromCurrent = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MiuixBlue
                            )
                        )
                    }
                }

                // 4. 上月结余自动滚存提示
                val isCurrentSelectionExisting = availableMonths.contains(Pair(selectedYear, selectedMonth))

                // 4. 提示信息
                if (isCurrentSelectionExisting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "⚠️ ${selectedYear}年${selectedMonth}月 账本已存在，请切换选择其他未开启的月份。",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            lineHeight = 17.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "🌱 资金智能结转：如果上个月有剩余未用完的结余资金，系统将自动作为【上月结余滚存】计入新月份的总资金池与可用结余供继续使用。",
                            fontSize = 12.sp,
                            color = MiuixGreen,
                            lineHeight = 17.sp
                        )
                    }
                }

                // 5. 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "取消", color = MiuixTheme.colorScheme.onSurface)
                    }

                    Button(
                        onClick = {
                            if (!isCurrentSelectionExisting) {
                                onCreateMonth(selectedYear, selectedMonth, copyBudgetFromCurrent)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        enabled = !isCurrentSelectionExisting,
                        colors = ButtonDefaults.buttonColors(
                            color = if (!isCurrentSelectionExisting) MiuixBlue else MiuixTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (!isCurrentSelectionExisting) "立即开启" else "月份已存在",
                            color = if (!isCurrentSelectionExisting) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
