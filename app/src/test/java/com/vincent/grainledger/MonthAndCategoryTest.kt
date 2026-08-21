package com.vincent.grainledger

import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        // 严格现金流：8月份入账 8000，当月消费 3000 -> 8月总结余 = 8000 - 3000 = 5000
        val m8Income = 8000.0
        val m8Spent = 3000.0
        val m8Balance = m8Income - m8Spent
        assertEquals(5000.0, m8Balance, 0.001)

        // 9月份：入账 0，上月结余滚存 5000 -> 总可用资金 5000，消费 1500 -> 总结余 3500
        val m9Income = 0.0
        val m9Rollover = m8Balance
        val m9TotalFunds = m9Income + m9Rollover // 0 + 5000 = 5000
        val m9Spent = 1500.0
        val m9Balance = m9TotalFunds - m9Spent // 5000 - 1500 = 3500

        assertEquals(5000.0, m9TotalFunds, 0.001)
        assertEquals(3500.0, m9Balance, 0.001)
    }

    @Test
    fun testStrictZeroIncomeNegativeBalance() {
        // 场景：某月无任何真实收入入账（0元），无上月滚存，但实际消费支出了 5413.50 元
        val income = 0.0
        val rollover = 0.0
        val spent = 5413.50

        val totalFunds = income + rollover
        val balance = totalFunds - spent

        assertEquals(0.0, totalFunds, 0.001)
        assertEquals(-5413.50, balance, 0.001)
        assertTrue(balance < 0.0) // 严格呈现为赤字负数
    }

    @Test
    fun testMultiMonthContinuousChainRollover() {
        // 模拟多月历史链条 (6月 -> 7月 -> 8月 -> 9月)，验证严格现金流全程继承
        var cumulativeRollover = 0.0

        // 6月：入账 8000，消费 3000 -> 期末结余 5000
        val m6Funds = 8000.0 + cumulativeRollover
        val m6Ending = m6Funds - 3000.0
        assertEquals(5000.0, m6Ending, 0.001)
        cumulativeRollover = m6Ending

        // 7月：入账 0，继承6月滚存 5000 -> 总资金 5000，消费 6000 -> 期末赤字 -1000
        val m7Funds = 0.0 + cumulativeRollover
        val m7Ending = m7Funds - 6000.0
        assertEquals(-1000.0, m7Ending, 0.001)
        cumulativeRollover = m7Ending

        // 8月：入账 8000，继承7月赤字 -1000 -> 总资金 7000，消费 2000 -> 期末结余 5000
        val m8Funds = 8000.0 + cumulativeRollover
        val m8Ending = m8Funds - 2000.0
        assertEquals(5000.0, m8Ending, 0.001)
        cumulativeRollover = m8Ending

        // 9月：入账 500，继承8月滚存 5000 -> 总资金 5500
        val m9Funds = 500.0 + cumulativeRollover
        assertEquals(5500.0, m9Funds, 0.001)
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

    @Test
    fun testGranularDataCleanOperations() {
        // 1. 模拟清空流水：预算细项的已消费清零，结余恢复为注入金额
        val itemWithSpent = BudgetItem(
            itemId = 1L,
            year = 2026,
            month = 8,
            categoryName = "餐饮",
            detailName = "午餐",
            unitPrice = 30.0,
            quantity = 30.0,
            totalPrice = 900.0,
            actualAllocated = 900.0,
            actualSpent = 450.0,
            balance = 450.0
        )

        val itemAfterCleanTransactions = itemWithSpent.copy(
            actualSpent = 0.0,
            balance = itemWithSpent.actualAllocated
        )

        assertEquals(0.0, itemAfterCleanTransactions.actualSpent, 0.001)
        assertEquals(900.0, itemAfterCleanTransactions.balance, 0.001)
        assertEquals(900.0, itemAfterCleanTransactions.actualAllocated, 0.001)

        // 2. 验证 CleanTargetType 枚举属性定义完备
        val types = com.vincent.grainledger.ui.screens.settings.components.CleanTargetType.entries
        assertEquals(3, types.size)
        assertTrue(types.any { it.name == "TRANSACTIONS" })
        assertTrue(types.any { it.name == "BUDGETS" })
        assertTrue(types.any { it.name == "ALL" })
    }

    @Test
    fun testDuplicateMonthCreationDisallowed() {
        val existingMonths = listOf(
            Pair(2026, 8),
            Pair(2026, 9),
            Pair(2026, 10),
            Pair(2026, 11),
            Pair(2026, 12)
        )

        // 尝试创建已存在的 2026年 8月 -> 应被判定为已存在
        val targetMonth = Pair(2026, 8)
        val isDuplicate = existingMonths.contains(targetMonth)
        assertTrue(isDuplicate)

        // 尝试创建未存在的 2027年 1月 -> 允许创建
        val newMonth = Pair(2027, 1)
        val isNewDuplicate = existingMonths.contains(newMonth)
        org.junit.Assert.assertFalse(isNewDuplicate)
    }

    @Test
    fun testMultiDimensionalGranularCleanFilter() {
        val tx1 = com.vincent.grainledger.data.model.TransactionRecord(1, 2026, 8, 1, "餐饮", "早餐", -15.0, 85.0, 85.0)
        val tx2 = com.vincent.grainledger.data.model.TransactionRecord(2, 2026, 8, 2, "交通", "地铁", -6.0, 94.0, 94.0)
        val tx3 = com.vincent.grainledger.data.model.TransactionRecord(3, 2026, 9, 1, "餐饮", "午餐", -30.0, 70.0, 70.0)
        val tx4 = com.vincent.grainledger.data.model.TransactionRecord(4, 2026, 9, 2, "工资", "薪水", 8000.0, 8000.0, 8000.0)

        val allTxs = listOf(tx1, tx2, tx3, tx4)

        // 场景 1：仅过滤清理 8月份且为 "餐饮" 的流水 -> tx1
        val targetMonths = setOf(Pair(2026, 8))
        val targetCategories = setOf("餐饮")
        val filtered = allTxs.filter { tx ->
            val monthMatch = targetMonths.contains(Pair(tx.year, tx.month))
            val catMatch = targetCategories.contains(tx.categoryName)
            monthMatch && catMatch
        }
        assertEquals(1, filtered.size)
        assertEquals(tx1.recordId, filtered[0].recordId)

        // 场景 2：全部月份，仅清理 "餐饮" 分类 -> tx1, tx3
        val allMonthsCategoryFilter = allTxs.filter { it.categoryName == "餐饮" }
        assertEquals(2, allMonthsCategoryFilter.size)

        // 场景 3：仅清理 9月份全部类别 -> tx3, tx4
        val monthOnlyFilter = allTxs.filter { it.year == 2026 && it.month == 9 }
        assertEquals(2, monthOnlyFilter.size)
    }

    @Test
    fun testSelectiveMonthIncomeSync() {
        // 场景：用户选择将一笔收入（如工资 8000元）同步至选定的部分月份（如 8月和 9月），但不同步至 10月
        val selectedMonths = listOf(Pair(2026, 8), Pair(2026, 9))
        val generatedRecords = selectedMonths.map { (y, m) ->
            com.vincent.grainledger.data.model.TransactionRecord(
                recordId = 0L,
                year = y,
                month = m,
                day = 10,
                categoryName = "工资薪金",
                detailName = "基本工资",
                amount = 8000.0,
                itemRemaining = 8000.0,
                categoryRemaining = 8000.0
            )
        }

        assertEquals(2, generatedRecords.size)
        assertEquals(Pair(2026, 8), Pair(generatedRecords[0].year, generatedRecords[0].month))
        assertEquals(Pair(2026, 9), Pair(generatedRecords[1].year, generatedRecords[1].month))
        // 验证 10月份未被污染
        assertFalse(generatedRecords.any { it.year == 2026 && it.month == 10 })
    }

    @Test
    fun testEmptyIncomeCategoryNotPollutingOtherMonths() {
        // 场景：系统中定义了 3 个收入分类，但 8 月仅有 "工资薪金" 入账，"兼职收入" 与 "理财" 无入账
        val allIncomeCategories = listOf(
            com.vincent.grainledger.data.model.BudgetCategory(1, "工资薪金", isIncome = true),
            com.vincent.grainledger.data.model.BudgetCategory(2, "兼职收入", isIncome = true),
            com.vincent.grainledger.data.model.BudgetCategory(3, "理财收益", isIncome = true)
        )
        val augTransactions = listOf(
            com.vincent.grainledger.data.model.TransactionRecord(1, 2026, 8, 5, "工资薪金", "月薪", 10000.0, 10000.0, 10000.0)
        )

        val txGroup = augTransactions.groupBy { it.categoryName }
        val displayedIncomeOverviews = allIncomeCategories.mapNotNull { cat ->
            val list = txGroup[cat.categoryName] ?: emptyList()
            if (list.isEmpty()) null else cat.categoryName
        }

        // 8月份仅展示存在流水的 "工资薪金"，不会将未入账的分类全部同步展开
        assertEquals(1, displayedIncomeOverviews.size)
        assertEquals("工资薪金", displayedIncomeOverviews[0])
    }
}
