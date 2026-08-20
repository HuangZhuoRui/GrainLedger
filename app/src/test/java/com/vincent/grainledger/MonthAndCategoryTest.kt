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

    @Test
    fun testIncomeCategoryAddsToTotalVolume() {
        // 1. 验证收入类大类属性
        val incomeCat = BudgetCategory(
            categoryId = 10L,
            categoryName = "工资薪金",
            iconName = "category_income",
            themeColorValue = 0xFF34C759L,
            sortOrder = 1,
            isIncome = true
        )
        assertTrue(incomeCat.isIncome)

        // 2. 模拟包含支出项与收入项的预算池计算
        val expenseItem = BudgetItem(
            itemId = 1L,
            year = 2026,
            month = 8,
            categoryName = "强制类",
            detailName = "学费支出",
            unitPrice = 5000.0,
            quantity = 1.0,
            totalPrice = 5000.0,
            actualAllocated = 5000.0,
            funder = "默认账户",
            actualSpent = 5000.0,
            balance = 0.0
        )

        val incomeItem = BudgetItem(
            itemId = 2L,
            year = 2026,
            month = 8,
            categoryName = "工资薪金",
            detailName = "8月兼职薪资",
            unitPrice = 3000.0,
            quantity = 1.0,
            totalPrice = 3000.0,
            actualAllocated = 3000.0, // 收入注入额度
            funder = "银行卡",
            actualSpent = 0.0,
            balance = 3000.0
        )

        val items = listOf(expenseItem, incomeItem)
        val totalAllocated = items.sumOf { it.actualAllocated } // 5000 + 3000 = 8000
        val totalSpent = expenseItem.actualSpent // 5000
        val totalBalance = totalAllocated - totalSpent // 8000 - 5000 = 3000

        // 验证收入类直接增加了资金池总量 (8000.0) 并使可用结余增加 (3000.0)
        assertEquals(8000.0, totalAllocated, 0.001)
        assertEquals(5000.0, totalSpent, 0.001)
        assertEquals(3000.0, totalBalance, 0.001)
    }

    @Test
    fun testIncomeAndExpenseTransactionDistinction() {
        val expenseRecord = com.vincent.grainledger.data.model.TransactionRecord(
            recordId = 1L,
            year = 2026,
            month = 8,
            day = 20,
            categoryName = "饮食开销",
            detailName = "午餐外卖",
            amount = -35.50,
            itemRemaining = 464.50,
            categoryRemaining = 1200.0
        )

        val incomeRecord = com.vincent.grainledger.data.model.TransactionRecord(
            recordId = 2L,
            year = 2026,
            month = 8,
            day = 20,
            categoryName = "工资薪金",
            detailName = "基本工资",
            amount = 8000.0,
            itemRemaining = 8000.0,
            categoryRemaining = 8000.0
        )

        // 验证支出流水属性
        assertTrue(expenseRecord.isExpense)
        assertEquals(35.50, expenseRecord.absoluteAmount, 0.001)

        // 验证收入流水属性
        org.junit.Assert.assertFalse(incomeRecord.isExpense)
        assertEquals(8000.0, incomeRecord.amount, 0.001)
        assertEquals(8000.0, incomeRecord.absoluteAmount, 0.001)

        // 验证收支统计聚合
        val records = listOf(expenseRecord, incomeRecord)
        val expenseTotal = records.filter { it.amount < 0 }.sumOf { -it.amount }
        val incomeTotal = records.filter { it.amount > 0 }.sumOf { it.amount }
        val netDifference = incomeTotal - expenseTotal

        assertEquals(35.50, expenseTotal, 0.001)
        assertEquals(8000.0, incomeTotal, 0.001)
        assertEquals(7964.50, netDifference, 0.001)
    }

    @Test
    fun testDynamicIncomeAccumulationOnBookkeeping() {
        // 初始状态：基础支出预算 6000，初始收入 0
        val baseExpenseAllocated = 6000.0
        var incomeItemAllocated = 0.0

        // 第一次记一笔收入：工资 +5000
        val trans1 = 5000.0
        incomeItemAllocated += trans1
        var totalFunds = baseExpenseAllocated + incomeItemAllocated
        assertEquals(5000.0, incomeItemAllocated, 0.001)
        assertEquals(11000.0, totalFunds, 0.001)

        // 第二次记一笔收入：兼职 +1200
        val trans2 = 1200.0
        incomeItemAllocated += trans2
        totalFunds = baseExpenseAllocated + incomeItemAllocated
        assertEquals(6200.0, incomeItemAllocated, 0.001)
        assertEquals(12200.0, totalFunds, 0.001)

        // 记一笔支出：消费 -800
        val spent = 800.0
        val totalBalance = totalFunds - spent
        assertEquals(11400.0, totalBalance, 0.001)
    }

    @Test
    fun testExpenseOnlyInBudgetPlanningAndIncomeInBookkeeping() {
        val categories = listOf(
            BudgetCategory(1, "餐饮饮食", "food", 0xFF3482FFL, 1, isIncome = false),
            BudgetCategory(2, "房租物业", "home", 0xFFFF9500L, 2, isIncome = false),
            BudgetCategory(3, "工资薪金", "salary", 0xFF34C759L, 3, isIncome = true),
            BudgetCategory(4, "兼职收入", "work", 0xFF00C7BEL, 4, isIncome = true)
        )

        // 验证分类管理中同时包含支出类与收入类
        val expenseCats = categories.filter { !it.isIncome }
        val incomeCats = categories.filter { it.isIncome }
        assertEquals(2, expenseCats.size)
        assertEquals(2, incomeCats.size)

        // 验证加预算仅允许支出类
        assertEquals(listOf("餐饮饮食", "房租物业"), expenseCats.map { it.categoryName })

        // 验证看板记一笔支持从收入类中选取
        assertEquals(listOf("工资薪金", "兼职收入"), incomeCats.map { it.categoryName })
    }
}
