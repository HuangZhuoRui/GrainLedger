package com.vincent.grainledger.data.model

import androidx.compose.ui.graphics.Color

/**
 * 预算分类数据模型。
 *
 * 对应 Excel 表格中的类别维度（例如：强制类、饮食类、预留类、文具类、恋爱类、生活类、提升类以及收入类等）。
 * 每个大类包含分类名称、显示图标标识、主题色、展示排序权重以及收入/支出性质标识。
 *
 * @property categoryId 唯一数字标识
 * @property categoryName 分类显示名称（例如 "强制类"）
 * @property iconName 分类所使用的图标标识
 * @property themeColorValue 分类的主题色值
 * @property sortOrder 在列表中的展示排序优先级
 * @property isIncome 是否为收入类（true 代表收入类直接增加总资金量，false 代表支出类从资金池中扣除）
 */
data class BudgetCategory(
    val categoryId: Long = 0L,
    val categoryName: String,
    val iconName: String = "category_default",
    val themeColorValue: Long = 0xFF2A82E4,
    val sortOrder: Int = 0,
    val isIncome: Boolean = false
) {
    /**
     * 获取 Compose Color 类型的分类主题色。
     */
    val themeColor: Color
        get() = Color((themeColorValue and 0xFFFFFFFFL).toInt())

    companion object {
        /**
         * 系统预置分类列表，完全对应用户 Excel 账单中的所有分类。
         */
        val defaultCategories: List<BudgetCategory> = listOf(
            BudgetCategory(
                categoryId = 1L,
                categoryName = "强制类",
                iconName = "category_mandatory",
                themeColorValue = 0xFFE54D42, // 红色系，代表固定必支出项（学费、住宿、电费、日常吃等）
                sortOrder = 1,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 2L,
                categoryName = "饮食类",
                iconName = "category_food",
                themeColorValue = 0xFFFA8C16, // 橙色系，代表额外饮食（减肥吃等）
                sortOrder = 2,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 3L,
                categoryName = "预留类",
                iconName = "category_saving",
                themeColorValue = 0xFF52C41A, // 绿色系，代表存储金与备用金
                sortOrder = 3,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 4L,
                categoryName = "文具类",
                iconName = "category_stationery",
                themeColorValue = 0xFF1890FF, // 蓝色系，代表学习文具
                sortOrder = 4,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 5L,
                categoryName = "恋爱类",
                iconName = "category_love",
                themeColorValue = 0xFFEB2F96, // 粉色系，代表恋爱基金
                sortOrder = 5,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 6L,
                categoryName = "生活类",
                iconName = "category_life",
                themeColorValue = 0xFF722ED1, // 紫色系，代表日常生活品
                sortOrder = 6,
                isIncome = false
            ),
            BudgetCategory(
                categoryId = 7L,
                categoryName = "提升类",
                iconName = "category_growth",
                themeColorValue = 0xFF13C2C2, // 青色系，代表个人成长与考证（如四级）
                sortOrder = 7,
                isIncome = false
            )
        )
    }
}
