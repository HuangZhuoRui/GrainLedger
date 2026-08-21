package com.vincent.grainledger.ui.screens.bookkeeping.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.util.MathFormulaEvaluator
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 记账弹窗底部智能导航栏组件 (BookkeepingBottomBar)。
 *
 * 包含：
 * 1. 上一步 / 取消 / 删除 按钮（根据当前步骤与编辑模式动态切换）；
 * 2. 下一步 / 确认入账 / 记一笔支出 / 保存修改 动态主操作按钮；
 * 3. 动态算式金额在按钮文案中的实时格式化注入。
 */
@Composable
fun BookkeepingBottomBar(
    currentPage: Int,
    isFinalStep: Boolean,
    isIncomeMode: Boolean,
    editingRecord: TransactionRecord?,
    evaluatedAmount: Double,
    activeThemeColor: Color,
    selectedTargetMonthsCount: Int,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val actionAmountString = if (evaluatedAmount > 0.0) "${MathFormulaEvaluator.formatAmount(evaluatedAmount)} ¥" else ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (currentPage > 0) {
            // 上一步按钮
            Button(
                onClick = onPrevStep,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Text(text = "上一步", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        } else {
            // 第 1 步时：编辑模式下提供删除，新增模式下提供取消
            if (editingRecord != null) {
                Button(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .weight(0.8f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(color = MiuixRed.copy(alpha = 0.15f))
                ) {
                    Text(text = "删除", color = MiuixRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
            ) {
                Text(text = "取消", color = MiuixTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        }

        if (!isFinalStep) {
            // 下一步按钮
            Button(
                onClick = onNextStep,
                modifier = Modifier
                    .weight(if (editingRecord != null && currentPage == 0) 1.4f else 2f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(color = activeThemeColor)
            ) {
                Text(
                    text = "下一步 ➔",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // 最终提交/保存按钮
            val submitButtonText = if (editingRecord != null) {
                "保存修改 $actionAmountString"
            } else if (isIncomeMode) {
                if (selectedTargetMonthsCount > 1) {
                    "确认入账 $actionAmountString（同步 ${selectedTargetMonthsCount} 个月）"
                } else {
                    "确认入账 $actionAmountString"
                }
            } else {
                "记一笔支出 $actionAmountString"
            }

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    color = if (evaluatedAmount > 0.0) activeThemeColor else MiuixTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = submitButtonText,
                    color = if (evaluatedAmount > 0.0) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
