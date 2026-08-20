package com.vincent.grainledger.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

// MIUIX 预设 8 种高颜值主题色板
private val PRESET_CATEGORY_COLORS = listOf(
    0xFF3482FFL, // 经典蓝
    0xFF34C759L, // 翡翠绿
    0xFFFF9500L, // 活力橙
    0xFFFF3B30L, // 珊瑚红
    0xFFAF52DEL, // 典雅紫
    0xFFFF2D55L, // 樱花粉
    0xFF5856D6L, // 靛蓝
    0xFF00C7BEL  // 青碧
)

/**
 * 预算分类新增与编辑弹窗 (EditCategoryDialog)。
 *
 * @param category 待编辑的分类实体（若为 null 则代表新增分类）
 * @param onSave 保存回调 (新实体, 原分类名称)
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun EditCategoryDialog(
    category: BudgetCategory? = null,
    onSave: (newCategory: BudgetCategory, oldName: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val oldName = category?.categoryName ?: ""
    var categoryNameInput by remember { mutableStateOf(category?.categoryName ?: "") }
    var selectedColorValue by remember { mutableLongStateOf(category?.themeColorValue ?: PRESET_CATEGORY_COLORS.first()) }

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
                Text(
                    text = if (category == null) "新增预算分类" else "编辑分类信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

                // 1. 分类名称输入框
                OutlinedTextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text(text = "分类名称 (如 投资理财、宠物开支)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MiuixShapes.MediumSquircle,
                    singleLine = true
                )

                // 2. 主题颜色选择调色板
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "选择主题色彩",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PRESET_CATEGORY_COLORS.forEach { colorVal ->
                            val isSelected = (colorVal == selectedColorValue)
                            val composeColor = Color((colorVal and 0xFFFFFFFFL).toInt())

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(composeColor)
                                    .clickable { selectedColorValue = colorVal }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MiuixTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                            val trimmedName = categoryNameInput.trim()
                            if (trimmedName.isNotEmpty()) {
                                val newCategory = BudgetCategory(
                                    categoryId = category?.categoryId ?: 0L,
                                    categoryName = trimmedName,
                                    iconName = category?.iconName ?: "folder",
                                    themeColorValue = selectedColorValue,
                                    sortOrder = category?.sortOrder ?: 99
                                )
                                onSave(newCategory, oldName)
                                onDismissRequest()
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                    ) {
                        Text(text = "保存分类", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
