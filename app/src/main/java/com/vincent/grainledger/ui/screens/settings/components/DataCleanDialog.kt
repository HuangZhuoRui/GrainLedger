package com.vincent.grainledger.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
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
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixOrange
import com.vincent.grainledger.ui.theme.MiuixPurple
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
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
        title = "清理流水",
        description = "删除指定月份与分类下的流水明细，已消费金额归零并恢复结余",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        themeColor = MiuixOrange
    ),
    BUDGETS(
        title = "清理预算",
        description = "删除指定月份与分类下的预算规划细项，保留分类体系与流水",
        icon = Icons.Default.AccountBalanceWallet,
        themeColor = MiuixPurple
    ),
    ALL(
        title = "全部重置",
        description = "彻底删除所有月份预算、全部流水记录与自定义分类，重置为空白账本",
        icon = Icons.Default.DeleteForever,
        themeColor = MiuixRed
    )
}

/**
 * 细分数据清理弹窗 (DataCleanDialog)。
 *
 * 支持按【清理流水 / 清理预算 / 全部重置】进行维度细分，
 * 并支持自由筛选目标月份集合与目标分类集合。
 *
 * @param availableMonths 可用的全部年月列表
 * @param allCategories 当前全部预算分类列表
 * @param onConfirmCleanTransactions 确认清理流水 (目标月份集合, 目标分类集合) -> 若集合为 null 则代表全部
 * @param onConfirmCleanBudgets 确认清理预算 (目标月份集合, 目标分类集合) -> 若集合为 null 则代表全部
 * @param onConfirmCleanAll 确认彻底清空全部数据
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun DataCleanDialog(
    availableMonths: List<Pair<Int, Int>>,
    allCategories: List<BudgetCategory>,
    onConfirmCleanTransactions: (Set<Pair<Int, Int>>?, Set<String>?) -> Unit,
    onConfirmCleanBudgets: (Set<Pair<Int, Int>>?, Set<String>?) -> Unit,
    onConfirmCleanAll: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedType by remember { mutableStateOf(CleanTargetType.TRANSACTIONS) }

    // 月份多选状态：true 为全选所有月份
    var isAllMonths by remember { mutableStateOf(true) }
    var selectedMonths by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }

    // 分类多选状态：true 为全选所有分类
    var isAllCategories by remember { mutableStateOf(true) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    val isFilterValid = when (selectedType) {
        CleanTargetType.ALL -> true
        CleanTargetType.TRANSACTIONS, CleanTargetType.BUDGETS -> {
            val hasMonth = isAllMonths || selectedMonths.isNotEmpty()
            val hasCat = isAllCategories || selectedCategories.isNotEmpty()
            hasMonth && hasCat
        }
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 顶部标题
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "数据清理",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "精准筛选需要清理的范围，灵活重置账目数据",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                // 2. 清理类型切换分段卡片
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CleanTargetType.entries.forEach { cleanType ->
                        val isSelected = selectedType == cleanType
                        val itemColor = cleanType.themeColor

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) itemColor.copy(alpha = 0.12f)
                                    else MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) itemColor else MiuixTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedType = cleanType }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = cleanType.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) itemColor else MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = cleanType.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) itemColor else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 3. 细分过滤选项（仅在清理流水或清理预算时展示）
                if (selectedType != CleanTargetType.ALL) {
                    // A. 月份范围筛选
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "目标月份范围",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAllMonths) "全部月份" else "已选 ${selectedMonths.size} 个月",
                                fontSize = 11.5.sp,
                                color = selectedType.themeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "全部月份" 胶囊
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isAllMonths) selectedType.themeColor else MiuixTheme.colorScheme.surfaceVariant,
                                        shape = MiuixShapes.SmallSquircle
                                    )
                                    .clickable {
                                        isAllMonths = true
                                        selectedMonths = emptySet()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "全部月份",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAllMonths) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAllMonths) Color.White else MiuixTheme.colorScheme.onSurface
                                )
                            }

                            // 各独立月份胶囊
                            availableMonths.forEach { m ->
                                val isSelected = !isAllMonths && selectedMonths.contains(m)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) selectedType.themeColor else MiuixTheme.colorScheme.surfaceVariant,
                                            shape = MiuixShapes.SmallSquircle
                                        )
                                        .clickable {
                                            if (isAllMonths) {
                                                isAllMonths = false
                                                selectedMonths = setOf(m)
                                            } else {
                                                val nextSet = selectedMonths.toMutableSet()
                                                if (nextSet.contains(m)) {
                                                    nextSet.remove(m)
                                                    if (nextSet.isEmpty()) isAllMonths = true
                                                } else {
                                                    nextSet.add(m)
                                                    if (nextSet.size == availableMonths.size) {
                                                        isAllMonths = true
                                                        nextSet.clear()
                                                    }
                                                }
                                                selectedMonths = nextSet
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${m.first % 100}年${m.second}月",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // B. 分类范围筛选
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "目标分类范围",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAllCategories) "全部分类" else "已选 ${selectedCategories.size} 个类别",
                                fontSize = 11.5.sp,
                                color = selectedType.themeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "全部分类" 胶囊
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isAllCategories) selectedType.themeColor else MiuixTheme.colorScheme.surfaceVariant,
                                        shape = MiuixShapes.SmallSquircle
                                    )
                                    .clickable {
                                        isAllCategories = true
                                        selectedCategories = emptySet()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "全部分类",
                                    fontSize = 12.sp,
                                    fontWeight = if (isAllCategories) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAllCategories) Color.White else MiuixTheme.colorScheme.onSurface
                                )
                            }

                            // 各独立分类胶囊
                            allCategories.forEach { cat ->
                                val isSelected = !isAllCategories && selectedCategories.contains(cat.categoryName)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) selectedType.themeColor else MiuixTheme.colorScheme.surfaceVariant,
                                            shape = MiuixShapes.SmallSquircle
                                        )
                                        .clickable {
                                            if (isAllCategories) {
                                                isAllCategories = false
                                                selectedCategories = setOf(cat.categoryName)
                                            } else {
                                                val nextSet = selectedCategories.toMutableSet()
                                                if (nextSet.contains(cat.categoryName)) {
                                                    nextSet.remove(cat.categoryName)
                                                    if (nextSet.isEmpty()) isAllCategories = true
                                                } else {
                                                    nextSet.add(cat.categoryName)
                                                    if (nextSet.size == allCategories.size) {
                                                        isAllCategories = true
                                                        nextSet.clear()
                                                    }
                                                }
                                                selectedCategories = nextSet
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isSelected) Color.White else cat.themeColor,
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = cat.categoryName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MiuixTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // C. 影响范围说明卡片
                    val monthSummary = if (isAllMonths) "全部月份" else "${selectedMonths.size} 个指定月份"
                    val catSummary = if (isAllCategories) "全部分类" else "${selectedCategories.size} 个指定分类"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(selectedType.themeColor.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🎯 清理范围：$monthSummary × $catSummary",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = selectedType.themeColor
                            )
                            Text(
                                text = if (selectedType == CleanTargetType.TRANSACTIONS)
                                    "选定范围内的流水明细将被清空，对应预算项的实际消费将自动清零并恢复可用结余。"
                                else
                                    "选定范围内的预算规划细项将被清空，历史交易流水明细与分类体系将继续保留。",
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                } else {
                    // 全部重置模式警示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixRed.copy(alpha = 0.08f))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MiuixRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "彻底抹除所有账本数据",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixRed
                                )
                                Text(
                                    text = "将清空所有月份预算、全部交易流水与自定义分类，恢复为空白账本状态，操作不可撤销！",
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. 底部按钮
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
                            val targetMonths = if (isAllMonths) null else selectedMonths
                            val targetCats = if (isAllCategories) null else selectedCategories

                            when (selectedType) {
                                CleanTargetType.TRANSACTIONS -> onConfirmCleanTransactions(targetMonths, targetCats)
                                CleanTargetType.BUDGETS -> onConfirmCleanBudgets(targetMonths, targetCats)
                                CleanTargetType.ALL -> onConfirmCleanAll()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        enabled = isFilterValid,
                        colors = ButtonDefaults.buttonColors(
                            color = if (isFilterValid) selectedType.themeColor else MiuixTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (isFilterValid) "确认清理" else "请选择范围",
                            fontSize = 14.sp,
                            color = if (isFilterValid) Color.White else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
