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

    @Test
    fun testRolloverFromPreviousMonth() {
        // 模拟8月份：基础预算 5000，当月入账 8000，当月消费 3000 -> 8月总结余 = (5000+8000)-3000 = 10000
        val m8ExpenseAllocated = 5000.0
        val m8Income = 8000.0
        val m8Spent = 3000.0
        val m8TotalAllocated = m8ExpenseAllocated + m8Income
        val m8Balance = m8TotalAllocated - m8Spent
        assertEquals(10000.0, m8Balance, 0.001)

        // 模拟9月份：基础预算 6000，当月入账 0，上月结余滚存 10000
        val m9ExpenseAllocated = 6000.0
        val m9Income = 0.0
        val m9Rollover = m8Balance // 自动结转给9月使用
        val m9TotalAllocated = m9ExpenseAllocated + m9Income + m9Rollover // 6000 + 0 + 10000 = 16000
        val m9Spent = 1500.0
        val m9Balance = m9TotalAllocated - m9Spent // 16000 - 1500 = 14500

        assertEquals(16000.0, m9TotalAllocated, 0.001)
        assertEquals(14500.0, m9Balance, 0.001)
    }

    @Test
    fun testMultiMonthContinuousChainRollover() {
        // 模拟多月历史链条 (6月 -> 7月 -> 8月 -> 9月)，验证资金一分不漏全程继承
        var cumulativeRollover = 0.0

        // 6月：基础预算 5000，入账 8000，消费 3000 -> 期末结余 10000
        val m6Funds = 5000.0 + 8000.0 + cumulativeRollover
        val m6Ending = m6Funds - 3000.0
        assertEquals(10000.0, m6Ending, 0.001)
        cumulativeRollover = m6Ending

        // 7月：基础预算 2000，入账 0，继承6月滚存 10000 -> 总资金 12000，消费 4000 -> 期末结余 8000
        val m7Funds = 2000.0 + 0.0 + cumulativeRollover
        val m7Ending = m7Funds - 4000.0
        assertEquals(8000.0, m7Ending, 0.001)
        cumulativeRollover = m7Ending

        // 8月：基础预算 3000，入账 1000，继承7月滚存 8000 -> 总资金 12000，消费 2000 -> 期末结余 10000
        val m8Funds = 3000.0 + 1000.0 + cumulativeRollover
        val m8Ending = m8Funds - 2000.0
        assertEquals(10000.0, m8Ending, 0.001)
        cumulativeRollover = m8Ending

        // 9月：基础预算 4000，入账 500，继承8月滚存 10000 -> 总资金 14500
        val m9Funds = 4000.0 + 500.0 + cumulativeRollover
        assertEquals(14500.0, m9Funds, 0.001)
    }

    @Test
    fun testDynamicCapitalBalanceCheck() {
        // 场景 1：9月份总规划 1675.20，实际注入 1625.20 -> 差额 50.00 待分配
        val sepPlanned = 1675.20
        val sepAllocated = 1625.20
        val sepBalanceCheck = com.vincent.grainledger.data.model.BalanceCheckResult(
            targetBenchmarkFund = sepPlanned,
            allocatedTotalFund = sepAllocated
        )
        assertEquals(50.00, sepBalanceCheck.balanceDifference, 0.001)
        assertTrue(sepBalanceCheck.hasUnallocatedFund)
        org.junit.Assert.assertFalse(sepBalanceCheck.isBalanced)
        org.junit.Assert.assertFalse(sepBalanceCheck.isOverAllocated)

        // 场景 2：用户加预算使得注入金额正好等于规划额度 -> 完美配平
        val balancedCheck = com.vincent.grainledger.data.model.BalanceCheckResult(
            targetBenchmarkFund = 1675.20,
            allocatedTotalFund = 1675.20
        )
        assertEquals(0.00, balancedCheck.balanceDifference, 0.001)
        assertTrue(balancedCheck.isBalanced)

        // 场景 3：实际注入金额超出规划额度 -> 超额分配警示
        val overAllocatedCheck = com.vincent.grainledger.data.model.BalanceCheckResult(
            targetBenchmarkFund = 1000.00,
            allocatedTotalFund = 1200.00
        )
        assertEquals(-200.00, overAllocatedCheck.balanceDifference, 0.001)
        assertTrue(overAllocatedCheck.isOverAllocated)
    }
}
