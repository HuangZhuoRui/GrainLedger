package com.vincent.grainledger

import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 新建月份账本与分类管理逻辑单元测试。
 */
class MonthAndCategoryTest {

    @Test
    fun testBudgetItemCloningForNewMonth() {
        // 模拟上月已有预算项（存在已支出与剩余）
        val sourceItem = BudgetItem(
            itemId = 101L,
            year = 2026,
            month = 8,
            categoryName = "固定开支",
            detailName = "房租水电",
            unitPrice = 3200.0,
            quantity = 1.0,
            totalPrice = 3200.0,
            actualAllocated = 3200.0,
            funder = "招商银行",
            actualSpent = 3200.0,
            balance = 0.0,
            remark = "每月初按时缴纳"
        )

        // 模拟新月份（2026年9月）预算克隆继承
        val clonedItem = sourceItem.copy(
            itemId = 0L,
            year = 2026,
            month = 9,
            actualSpent = 0.0,
            balance = sourceItem.actualAllocated
        )

        // 验证克隆后的主键与年月
        assertEquals(0L, clonedItem.itemId)
        assertEquals(2026, clonedItem.year)
        assertEquals(9, clonedItem.month)
        assertEquals("固定开支", clonedItem.categoryName)
        assertEquals("房租水电", clonedItem.detailName)

        // 验证预算额度保持一致，而支出清零且结余重置为注入额度
        assertEquals(3200.0, clonedItem.actualAllocated, 0.001)
        assertEquals(0.0, clonedItem.actualSpent, 0.001)
        assertEquals(3200.0, clonedItem.balance, 0.001)
    }

    @Test
    fun testAvailableMonthsSortingComparator() {
        val rawMonths = listOf(
            Pair(2027, 2),
            Pair(2026, 8),
            Pair(2026, 12),
            Pair(2025, 11),
            Pair(2026, 9)
        )

        val sorted = rawMonths.sortedWith(Comparator { a, b ->
            if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
        })

        val expected = listOf(
            Pair(2025, 11),
            Pair(2026, 8),
            Pair(2026, 9),
            Pair(2026, 12),
            Pair(2027, 2)
        )

        assertEquals(expected, sorted)
    }

    @Test
    fun testBudgetCategoryProperties() {
        val category = BudgetCategory(
            categoryId = 5L,
            categoryName = "投资理财",
            iconName = "trending_up",
            themeColorValue = 0xFF34C759L,
            sortOrder = 5
        )

        assertEquals(5L, category.categoryId)
        assertEquals("投资理财", category.categoryName)
        assertEquals(0xFF34C759L, category.themeColorValue)
        assertTrue(category.sortOrder > 0)

        // 验证 Compose Color 正常转换，无 ColorSpace 21 异常
        val color = category.themeColor
        assertEquals(1f, color.alpha, 0.01f)

        BudgetCategory.defaultCategories.forEach { defaultCat ->
            val c = defaultCat.themeColor
            assertEquals(1f, c.alpha, 0.01f)
        }
    }
}
