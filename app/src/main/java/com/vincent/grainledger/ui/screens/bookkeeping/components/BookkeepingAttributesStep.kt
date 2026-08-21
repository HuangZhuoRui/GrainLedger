package com.vincent.grainledger.ui.screens.bookkeeping.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.theme.horizontalFadingEdge
import com.vincent.grainledger.util.DateUtils
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记账 Step 3: 记账日期、出资/收款账户与说明备注卡片 (BookkeepingAttributesStep)。
 *
 * 包含：
 * 1. 记账日期选择：今日/昨日/前日快捷胶囊 + 1~31 号智能居中羽化滑轨；
 * 2. 账户选择：输入框 + 8 大主流高频账户快捷标签；
 * 3. 备注文本输入；
 * 4. 实时款项核算与明细小结卡片。
 */
@Composable
fun BookkeepingAttributesStep(
    currentYear: Int,
    currentMonth: Int,
    maxDaysInMonth: Int,
    transactionDay: Int,
    onDaySelected: (Int) -> Unit,
    dateListState: LazyListState,
    isIncomeMode: Boolean,
    funder: String,
    onFunderChange: (String) -> Unit,
    remarkText: String,
    onRemarkChange: (String) -> Unit,
    selectedCategory: String,
    selectedDetail: String,
    incomeDetailInput: String,
    evaluatedAmount: Double,
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 记账日期自选（居中定位）
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "记账日期",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                Text(
                    text = "${currentYear}年${currentMonth}月${transactionDay}日",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeThemeColor
                )
            }

            // 今天/昨天/前天 快速定位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val today = DateUtils.getCurrentDay().coerceIn(1, maxDaysInMonth)
                val yesterday = (today - 1).coerceAtLeast(1)
                val beforeYesterday = (today - 2).coerceAtLeast(1)

                listOf(
                    Pair("今天 (${today}日)", today),
                    Pair("昨天 (${yesterday}日)", yesterday),
                    Pair("前天 (${beforeYesterday}日)", beforeYesterday)
                ).forEach { (label, day) ->
                    val isSelected = (transactionDay == day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MiuixShapes.SmallSquircle)
                            .background(
                                if (isSelected) activeThemeColor.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onDaySelected(day) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) activeThemeColor else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 当月 1 ~ maxDaysInMonth 全量自选滑轨（带两端羽化淡出 & 居中定位）
            LazyRow(
                state = dateListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items((1..maxDaysInMonth).toList()) { day ->
                    val isSelected = (transactionDay == day)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) activeThemeColor else MiuixTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onDaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. 出资/收款账户
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = funder,
                onValueChange = onFunderChange,
                label = { Text(text = if (isIncomeMode) "收款账户" else "出资扣款账户") },
                placeholder = { Text("例如：微信零钱、招商银行卡、美团月付") },
                singleLine = true,
                shape = MiuixShapes.MediumSquircle,
                modifier = Modifier.fillMaxWidth()
            )

            // 常用账户快捷胶囊（带边缘羽化模糊）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("微信零钱", "支付宝", "招商银行卡", "现金", "建设银行", "工商银行", "京东白条", "花呗").forEach { acc ->
                    val isSelected = (funder == acc)
                    Box(
                        modifier = Modifier
                            .clip(MiuixShapes.PillShape)
                            .background(
                                if (isSelected) MiuixBlue.copy(alpha = 0.16f) else MiuixTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onFunderChange(acc) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = acc,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. 说明备注
        OutlinedTextField(
            value = remarkText,
            onValueChange = onRemarkChange,
            placeholder = { Text("添加说明备注 (选填)...", fontSize = 12.5.sp) },
            singleLine = true,
            shape = MiuixShapes.MediumSquircle,
            modifier = Modifier.fillMaxWidth()
        )

        // 4. 实时记账总览小结卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedCategory} · ${if (isIncomeMode) incomeDetailInput.ifBlank { "日常入账" } else selectedDetail.ifBlank { "日常支出" }}",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                Text(
                    text = "${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeThemeColor
                )
            }
        }
    }
}
