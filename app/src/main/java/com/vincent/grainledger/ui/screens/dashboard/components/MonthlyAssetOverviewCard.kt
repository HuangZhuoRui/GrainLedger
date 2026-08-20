package com.vincent.grainledger.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.MonthlyOverview
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.AmountText
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 月度核心资产总览大卡片 (MonthlyAssetOverviewCard)。
 *
 * 展示当月剩余可用总结余、资金状态徽章、上月结余滚存提示、规划总价、资金池总量、本月入账与已消费核心指标。
 *
 * @param currentYear 当前年份
 * @param currentMonth 当前月份
 * @param monthlyOverview 月度汇总模型
 * @param modifier 外部修饰符
 */
@Composable
fun MonthlyAssetOverviewCard(
    currentYear: Int,
    currentMonth: Int,
    monthlyOverview: MonthlyOverview,
    modifier: Modifier = Modifier
) {
    val hasIncome = monthlyOverview.totalIncome > 0.0
    val hasRollover = monthlyOverview.rolloverFromPreviousMonth != 0.0

    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 22.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentYear}年${currentMonth}月 资产总览",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )

            val (badgeText, badgeColor) = when {
                monthlyOverview.totalBalance > 0.0 -> "资金充盈" to MiuixGreen
                monthlyOverview.totalBalance < 0.0 -> "资金赤字" to MiuixRed
                else -> "收支平衡" to MiuixBlue
            }

            StatusBadge(
                text = badgeText,
                color = badgeColor
            )
        }

        // 剩余可用结余核心大字
        Column(modifier = Modifier.padding(top = 10.dp, bottom = if (hasRollover) 6.dp else 12.dp)) {
            Text(
                text = "当前剩余可用总结余",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
            AmountText(
                amount = monthlyOverview.totalBalance,
                fontSize = 32.sp,
                symbolFontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (monthlyOverview.totalBalance >= 0) MiuixTheme.colorScheme.onSurface else MiuixRed
            )
        }

        // 上月结余滚存横幅
        if (hasRollover) {
            val isPositiveRollover = monthlyOverview.rolloverFromPreviousMonth > 0.0
            val bannerBg = if (isPositiveRollover) MiuixGreen.copy(alpha = 0.12f) else MiuixRed.copy(alpha = 0.12f)
            val bannerColor = if (isPositiveRollover) MiuixGreen else MiuixRed
            val bannerText = if (isPositiveRollover)
                "🌱 含上月结余滚存: +${MathFormulaEvaluator.formatAmount(monthlyOverview.rolloverFromPreviousMonth)} ¥（已自动结转至本月资金池）"
            else
                "⚠️ 含上月赤字结转: -${MathFormulaEvaluator.formatAmount(-monthlyOverview.rolloverFromPreviousMonth)} ¥（已自动结转至本月资金池）"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bannerBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = bannerText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = bannerColor
                )
            }
        }

        // 核心指标行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (hasRollover) 8.dp else 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "规划总价",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = monthlyOverview.totalPlannedBudget,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
            }

            Column {
                Text(
                    text = "资金池总量",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = monthlyOverview.totalActualAllocated,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixBlue
                )
            }

            if (hasIncome) {
                Column {
                    Text(
                        text = "本月入账",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    AmountText(
                        amount = monthlyOverview.totalIncome,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixGreen
                    )
                }
            }

            Column {
                Text(
                    text = "本月已消费",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                AmountText(
                    amount = monthlyOverview.totalActualSpent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixRed
                )
            }
        }
    }
}
