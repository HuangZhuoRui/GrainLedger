package com.vincent.grainledger.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.ui.components.dialog.ConfirmDialog
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算分类全量管理弹窗 (CategoryManagementDialog)。
 *
 * 展示当前所有大类、支持新增大类、编辑大类名称/主题色，以及删除大类。
 *
 * @param categoryList 分类列表
 * @param onSaveCategory 保存/更新分类回调 (新实体, 旧名称)
 * @param onDeleteCategory 删除分类回调 (待删实体, 是否级联删除关联细项)
 * @param onDismissRequest 关闭弹窗回调
 */
@Composable
fun CategoryManagementDialog(
    categoryList: List<BudgetCategory>,
    onSaveCategory: (newCategory: BudgetCategory, oldName: String) -> Unit,
    onDeleteCategory: (category: BudgetCategory, deleteAssociatedItems: Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var editingCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<BudgetCategory?>(null) }

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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MiuixBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "预算大类管理",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "自定义与规划专属预算大类及主题色彩",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                // 分类列表卡片列表 (高度限制自适应滚动)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryList, key = { it.categoryId }) { cat ->
                        val themeColor = cat.themeColor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MiuixShapes.MediumSquircle)
                                .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(themeColor)
                                )
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (cat.isIncome) MiuixGreen.copy(alpha = 0.15f) else MiuixTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (cat.isIncome) "+ 收入类" else "- 支出类",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (cat.isIncome) MiuixGreen else MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        editingCategory = cat
                                        showEditCategoryDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = MiuixBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        categoryToDelete = cat
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MiuixRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 底部操作按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            editingCategory = null
                            showEditCategoryDialog = true
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(color = MiuixBlue)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "添加新分类", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(0.8f),
                        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(text = "完成", color = MiuixTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

    // 编辑/新增分类子弹窗
    if (showEditCategoryDialog) {
        EditCategoryDialog(
            category = editingCategory,
            onSave = { savedCategory, oldName ->
                onSaveCategory(savedCategory, oldName)
                showEditCategoryDialog = false
            },
            onDismissRequest = {
                showEditCategoryDialog = false
            }
        )
    }

    // 删除分类确认弹窗
    if (categoryToDelete != null) {
        val target = categoryToDelete!!
        ConfirmDialog(
            title = "确认删除分类",
            message = "删除【${target.categoryName}】后，其关联的细项也将一并清理。是否继续？",
            confirmText = "确认删除",
            confirmColor = MiuixRed,
            onConfirm = {
                onDeleteCategory(target, true)
                categoryToDelete = null
            },
            onDismiss = {
                categoryToDelete = null
            }
        )
    }
}
