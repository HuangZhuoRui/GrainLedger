package com.vincent.grainledger.ui.screens.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.theme.horizontalFadingEdge
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 预算 Step 1: 支出大类与细项归属选择卡片 (EditBudgetCategoryStep)。
 *
 * 包含：
 * 1. 支出大类横向羽化滑轨选择与原生新建大类；
 * 2. 细项名称文本输入；
 * 3. 智能高频细项推荐标签横向滑轨。
 */
@Composable
fun EditBudgetCategoryStep(
    expenseCategories: List<BudgetCategory>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    detailName: String,
    onDetailNameChange: (String) -> Unit,
    recommendedDetails: List<String>,
    onOpenCreateCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 归属支出大类选择（支持原地新建分类）
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "选择支出大类",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                expenseCategories.forEach { category ->
                    val isSelected = (category.categoryName == selectedCategory)
                    val catColor = category.themeColor

                    Box(
                        modifier = Modifier
                            .clip(MiuixShapes.SmallSquircle)
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.18f) else MiuixTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                onCategorySelected(category.categoryName)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(catColor, CircleShape)
                            )
                            Text(
                                text = category.categoryName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catColor else MiuixTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 原地新建分类
                Box(
                    modifier = Modifier
                        .clip(MiuixShapes.SmallSquircle)
                        .background(MiuixBlue.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenCreateCategory)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建分类",
                            tint = MiuixBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "新建分类",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixBlue
                        )
                    }
                }
            }
        }

        // 细项名称输入与智能推荐气泡
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = detailName,
                onValueChange = onDetailNameChange,
                label = { Text(text = "预算细项名称") },
                placeholder = { Text(text = "例如：房租物业、一日三餐、水电燃气") },
                modifier = Modifier.fillMaxWidth(),
                shape = MiuixShapes.MediumSquircle,
                singleLine = true
            )

            // 智能推荐常用标签
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdge(14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                recommendedDetails.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(MiuixShapes.SmallSquircle)
                            .background(MiuixTheme.colorScheme.surfaceVariant)
                            .clickable { onDetailNameChange(preset) }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+ $preset",
                            fontSize = 11.5.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }
        }
    }
}
