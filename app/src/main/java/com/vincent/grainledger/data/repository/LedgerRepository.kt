package com.vincent.grainledger.data.repository

import android.content.Context
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.BalanceCheckResult
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.CategoryOverview
import com.vincent.grainledger.data.model.IncomeCategoryOverview
import com.vincent.grainledger.data.model.MonthlyOverview
import com.vincent.grainledger.data.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 余粮统一数据仓库 (带内存多级缓存与增量失效机制)。
 *
 * 核心架构：
 * 1. 读操作全量走内存极速缓存，0ms 瞬时响应，避免滑月/切 Tab/浏览时的重复 SQLite 查询与反复重算；
 * 2. 写操作（增删改流水、预算、分类、月份）精准执行局部增量失效与波及链式滚存失效，保证数据绝对准确 (SSOT)。
 */
class LedgerRepository(context: Context) {

    private val database = GrainLedgerDatabase.getInstance(context.applicationContext)
    private val categoryDao = database.categoryDao
    private val budgetItemDao = database.budgetItemDao
    private val transactionDao = database.transactionDao

    // ==================== 内存多级高速缓存 ====================
    private val categoryCache = AtomicReference<List<BudgetCategory>?>(null)
    private val availableMonthsCache = AtomicReference<List<Pair<Int, Int>>?>(null)
    private val budgetItemsCache = ConcurrentHashMap<Pair<Int, Int>, List<BudgetItem>>()
    private val transactionsCache = ConcurrentHashMap<Pair<Int, Int>, List<TransactionRecord>>()
    private val monthlyOverviewCache = ConcurrentHashMap<Pair<Int, Int>, MonthlyOverview>()
    private val balanceCheckCache = ConcurrentHashMap<Pair<Int, Int>, BalanceCheckResult>()

    // 内存数据版本触发流，用于状态变更通知
    private val _dataVersionFlow = MutableStateFlow(0L)
    val dataVersionFlow: StateFlow<Long> = _dataVersionFlow.asStateFlow()

    private fun notifyDataChanged() {
        _dataVersionFlow.value = System.currentTimeMillis()
    }

    /**
     * 全局清空所有内存缓存。
     */
    private fun invalidateAllCaches() {
        categoryCache.set(null)
        availableMonthsCache.set(null)
        budgetItemsCache.clear()
        transactionsCache.clear()
        monthlyOverviewCache.clear()
        balanceCheckCache.clear()
    }

    /**
     * 精准失效指定月份及后续受链式滚存波及月份的缓存。
     */
    private fun invalidateMonthAndFuture(year: Int, month: Int) {
        val monthKey = Pair(year, month)
        budgetItemsCache.remove(monthKey)
        transactionsCache.remove(monthKey)
        availableMonthsCache.set(null)

        // 历史发生变动，波及当前月及以后所有月份的链式滚存结果，予以精准失效
        monthlyOverviewCache.keys.filter {
            it.first > year || (it.first == year && it.second >= month)
        }.forEach {
            monthlyOverviewCache.remove(it)
        }

        // 资金池配平数据同步精准失效当前月及后续波及月份
        balanceCheckCache.keys.filter {
            it.first > year || (it.first == year && it.second >= month)
        }.forEach {
            balanceCheckCache.remove(it)
        }
    }

    /**
     * 获取所有预算分类列表（内存缓存加速）。
     */
    suspend fun getAllCategories(): List<BudgetCategory> = withContext(Dispatchers.IO) {
        categoryCache.get() ?: run {
            val loaded = categoryDao.getAllCategories()
            categoryCache.set(loaded)
            loaded
        }
    }

    /**
     * 保存或更新分类实体。
     */
    suspend fun saveCategory(category: BudgetCategory, oldName: String = ""): Long = withContext(Dispatchers.IO) {
        val resultId = if (category.categoryId > 0L) {
            categoryDao.updateCategory(oldName, category)
            category.categoryId
        } else {
            categoryDao.insertCategory(category)
        }
        // 分类变动影响全局信封与展示，全面刷新
        invalidateAllCaches()
        notifyDataChanged()
        resultId
    }

    /**
     * 删除指定分类，支持选择是否级联清理该分类下的所有预算细项与流水。
     */
    suspend fun deleteCategory(category: BudgetCategory, deleteAssociatedItems: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            if (deleteAssociatedItems) {
                db.delete(
                    GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
                    "${GrainLedgerDatabase.COL_BUDGET_CATEGORY} = ?",
                    arrayOf(category.categoryName)
                )
                db.delete(
                    GrainLedgerDatabase.TABLE_TRANSACTIONS,
                    "${GrainLedgerDatabase.COL_TRANS_CATEGORY} = ?",
                    arrayOf(category.categoryName)
                )
            }
            categoryDao.deleteCategory(category.categoryId, db)
        }
        invalidateAllCaches()
        notifyDataChanged()
        true
    }

    /**
     * 获取指定分类关联的预算细项数量。
     */
    suspend fun getCategoryUsageCount(categoryName: String): Int = withContext(Dispatchers.IO) {
        categoryDao.countBudgetItemsByCategory(categoryName)
    }

    /**
     * 获取指定月份的全部记账流水记录（按日期降序排列，内存缓存加速）。
     */
    suspend fun getTransactionsByMonth(year: Int, month: Int): List<TransactionRecord> = withContext(Dispatchers.IO) {
        val key = Pair(year, month)
        transactionsCache.computeIfAbsent(key) {
            transactionDao.getTransactionsByMonth(year, month)
        }
    }

    /**
     * 获取指定年月的全部预算项列表（内存缓存加速）。
     */
    suspend fun getBudgetItemsByMonth(year: Int, month: Int): List<BudgetItem> = withContext(Dispatchers.IO) {
        val key = Pair(year, month)
        budgetItemsCache.computeIfAbsent(key) {
            budgetItemDao.getBudgetItemsByMonth(year, month)
        }
    }

    /**
     * 保存或更新预算细项。
     */
    suspend fun saveBudgetItem(budgetItem: BudgetItem): Long = withContext(Dispatchers.IO) {
        val resultId = budgetItemDao.saveBudgetItem(budgetItem)
        invalidateMonthAndFuture(budgetItem.year, budgetItem.month)
        notifyDataChanged()
        resultId
    }

    /**
     * 批量保存预算细项。
     */
    suspend fun saveBudgetItems(budgetItems: List<BudgetItem>) = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            budgetItems.forEach { budgetItemDao.saveBudgetItem(it, db) }
        }
        val affectedMonths = budgetItems.map { Pair(it.year, it.month) }.distinct()
        affectedMonths.forEach { (y, m) -> invalidateMonthAndFuture(y, m) }
        notifyDataChanged()
    }

    /**
     * 删除指定的预算项。
     */
    suspend fun deleteBudgetItem(item: BudgetItem): Boolean = withContext(Dispatchers.IO) {
        val rowsAffected = budgetItemDao.deleteBudgetItem(item.itemId)
        if (rowsAffected > 0) {
            invalidateMonthAndFuture(item.year, item.month)
            notifyDataChanged()
            true
        } else {
            false
        }
    }

    /**
     * 根据主键删除预算项。
     */
    suspend fun deleteBudgetItem(itemId: Long): Boolean = withContext(Dispatchers.IO) {
        // 先读取以便精准知道年月
        val allBudgets = budgetItemDao.getAllBudgetItems()
        val target = allBudgets.find { it.itemId == itemId }
        val rowsAffected = budgetItemDao.deleteBudgetItem(itemId)
        if (rowsAffected > 0) {
            if (target != null) {
                invalidateMonthAndFuture(target.year, target.month)
            } else {
                invalidateAllCaches()
            }
            notifyDataChanged()
            true
        } else {
            false
        }
    }

    /**
     * 新建月份账本，支持从基准月份智能复制克隆预算细项结构。
     */
    suspend fun createMonth(
        targetYear: Int,
        targetMonth: Int,
        sourceYear: Int,
        sourceMonth: Int,
        copyBudget: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            if (copyBudget) {
                val sourceItems = budgetItemDao.getBudgetItemsByMonth(sourceYear, sourceMonth)
                sourceItems.forEach { item ->
                    val clonedItem = item.copy(
                        itemId = 0L,
                        year = targetYear,
                        month = targetMonth,
                        actualSpent = 0.0,
                        balance = item.actualAllocated
                    )
                    budgetItemDao.saveBudgetItem(clonedItem, db)
                }
            } else {
                // 若不复制，则为新月份插入一条默认分类的空预算项
                val firstCategory = categoryDao.getAllCategories().firstOrNull()?.categoryName ?: "强制类"
                val emptyItem = BudgetItem(
                    itemId = 0L,
                    year = targetYear,
                    month = targetMonth,
                    categoryName = firstCategory,
                    detailName = "初始预算",
                    unitPrice = 0.0,
                    quantity = 1.0,
                    totalPrice = 0.0,
                    actualAllocated = 0.0,
                    funder = "默认账户",
                    actualSpent = 0.0,
                    balance = 0.0,
                    remark = "新建月份初始项"
                )
                budgetItemDao.saveBudgetItem(emptyItem, db)
            }
        }
        invalidateMonthAndFuture(targetYear, targetMonth)
        notifyDataChanged()
        true
    }

    /**
     * 获取所有可用的年份与月份列表（内存缓存加速）。
     */
    suspend fun getAvailableMonths(): List<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        availableMonthsCache.get() ?: run {
            val budgetMonths = budgetItemDao.getAvailableMonths()
            val txMonths = transactionDao.getAvailableMonths()
            val combined = (budgetMonths + txMonths).distinct().sortedWith(Comparator { a, b ->
                if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
            })
            val result = if (combined.isNotEmpty()) {
                combined
            } else {
                val cal = java.util.Calendar.getInstance()
                listOf(Pair(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1))
            }
            availableMonthsCache.set(result)
            result
        }
    }

    /**
     * 新增一笔记账流水，并在数据库事务中自动联动计算【具体剩余】与【类剩余】，同时扣减对应预算项的结余。
     */
    suspend fun recordTransaction(transaction: TransactionRecord): Long = withContext(Dispatchers.IO) {
        val resultId = database.runInTransaction { db ->
            val isIncome = transaction.amount > 0

            val computedItemRemaining: Double
            val computedCategoryRemaining: Double

            if (isIncome) {
                // 收入记账：直接累加该收入大类当前月的入账总量
                val curTrans = transactionDao.getTransactionsByMonth(transaction.year, transaction.month)
                val curCatIncome = curTrans.filter { it.categoryName == transaction.categoryName && it.amount > 0 }.sumOf { it.amount }
                computedItemRemaining = transaction.amount
                computedCategoryRemaining = curCatIncome + transaction.amount
            } else {
                // 支出记账：联动更新对应预算项并扣减剩余
                val expenseAmount = transaction.absoluteAmount
                val existingItem = budgetItemDao.findBudgetItem(
                    transaction.year,
                    transaction.month,
                    transaction.categoryName,
                    transaction.detailName,
                    db
                )

                if (existingItem != null) {
                    val newSpent = existingItem.actualSpent + expenseAmount
                    val newBalance = existingItem.actualAllocated - newSpent
                    budgetItemDao.updateSpentAndBalance(existingItem.itemId, newSpent, newBalance, db)
                    computedItemRemaining = newBalance
                } else {
                    computedItemRemaining = -expenseAmount
                }

                // 重新统计该支出大类的全部结余
                val categoryItems = budgetItemDao.getBudgetItemsByMonth(transaction.year, transaction.month)
                    .filter { it.categoryName == transaction.categoryName }
                val totalAllocated = categoryItems.sumOf { it.actualAllocated }
                val totalSpent = categoryItems.sumOf { it.actualSpent } + (if (existingItem == null) expenseAmount else 0.0)
                computedCategoryRemaining = totalAllocated - totalSpent
            }

            val finalRecord = transaction.copy(
                itemRemaining = computedItemRemaining,
                categoryRemaining = computedCategoryRemaining
            )

            transactionDao.insertTransaction(finalRecord, db)
        }

        invalidateMonthAndFuture(transaction.year, transaction.month)
        notifyDataChanged()
        resultId
    }

    /**
     * 删除一笔交易流水，并在数据库事务中自动反算回补细项与大类的剩余金额。
     */
    suspend fun deleteTransaction(record: TransactionRecord): Boolean = withContext(Dispatchers.IO) {
        val success = database.runInTransaction { db ->
            val recordId = record.recordId
            val existingRecord = transactionDao.getTransactionsByMonth(record.year, record.month)
                .find { it.recordId == recordId }

            if (existingRecord != null) {
                // 若是支出流水，回退扣减的预算项消费
                if (existingRecord.amount < 0) {
                    val refundAmount = existingRecord.absoluteAmount
                    val targetItem = budgetItemDao.findBudgetItem(
                        existingRecord.year,
                        existingRecord.month,
                        existingRecord.categoryName,
                        existingRecord.detailName,
                        db
                    )
                    if (targetItem != null) {
                        val newSpent = (targetItem.actualSpent - refundAmount).coerceAtLeast(0.0)
                        val newBalance = targetItem.actualAllocated - newSpent
                        budgetItemDao.updateSpentAndBalance(
                            itemId = targetItem.itemId,
                            actualSpent = newSpent,
                            balance = newBalance,
                            db = db
                        )
                    }
                }

                // 删除流水
                transactionDao.deleteTransaction(recordId, db)
                true
            } else {
                false
            }
        }

        if (success) {
            invalidateMonthAndFuture(record.year, record.month)
            notifyDataChanged()
        }
        success
    }

    /**
     * 根据主键删除交易流水（重载方法）。
     */
    suspend fun deleteTransaction(recordId: Long): Boolean = withContext(Dispatchers.IO) {
        val allTxs = transactionDao.getAllTransactions()
        val targetRecord = allTxs.find { it.recordId == recordId }
        if (targetRecord != null) {
            deleteTransaction(targetRecord)
        } else {
            val rows = transactionDao.deleteTransaction(recordId)
            if (rows > 0) {
                invalidateAllCaches()
                notifyDataChanged()
                true
            } else {
                false
            }
        }
    }

    /**
     * 获取指定年月的月度综合汇总数据（内存缓存加速 + 增量失效）。
     */
    suspend fun getMonthlyOverview(year: Int, month: Int): MonthlyOverview = withContext(Dispatchers.IO) {
        val key = Pair(year, month)
        monthlyOverviewCache.computeIfAbsent(key) {
            computeMonthlyOverviewInternal(year, month)
        }
    }

    /**
     * 内部月度综合数据计算逻辑。
     */
    private fun computeMonthlyOverviewInternal(year: Int, month: Int): MonthlyOverview {
        val budgetItems = budgetItemDao.getBudgetItemsByMonth(year, month)
        val allCats = categoryDao.getAllCategories()
        val categoryMap = allCats.associateBy { it.categoryName }
        val transactions = transactionDao.getTransactionsByMonth(year, month)

        // 1. 仅支出类预算细项
        val expenseBudgetItems = budgetItems.filter { categoryMap[it.categoryName]?.isIncome != true }

        val totalPlanned = BigDecimal(expenseBudgetItems.sumOf { it.totalPrice }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalExpenseAllocated = BigDecimal(expenseBudgetItems.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 2. 收入来自当月流水中的真实入账
        val incomeTransactions = transactions.filter { it.amount > 0 }
        val totalIncome = BigDecimal(incomeTransactions.sumOf { it.amount }).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 3. 支出来自消费总额
        val totalSpent = BigDecimal(expenseBudgetItems.sumOf { it.actualSpent }).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 4. 历史各月链式累加结余滚存（一次性批量聚合，极速完成历史全程链式计算）
        val allBudgets = budgetItemDao.getAllBudgetItems()
        val allTxs = transactionDao.getAllTransactions()

        val budgetMonths = allBudgets.map { Pair(it.year, it.month) }
        val txMonths = allTxs.map { Pair(it.year, it.month) }
        val allChronologicalMonths = (budgetMonths + txMonths)
            .distinct()
            .sortedWith(Comparator { a, b ->
                if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
            })

        val priorMonths = allChronologicalMonths.filter {
            it.first < year || (it.first == year && it.second < month)
        }

        val budgetGroupMap = allBudgets.groupBy { Pair(it.year, it.month) }
        val txGroupMap = allTxs.groupBy { Pair(it.year, it.month) }

        var cumulativeRollover = 0.0
        for (pMonthKey in priorMonths) {
            val pBudgetItems = budgetGroupMap[pMonthKey] ?: emptyList()
            val pExpenseItems = pBudgetItems.filter { categoryMap[it.categoryName]?.isIncome != true }
            val pExpenseAllocated = pExpenseItems.sumOf { it.actualAllocated }
            val pTransactions = txGroupMap[pMonthKey] ?: emptyList()
            val pIncomeTotal = pTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val pSpentTotal = pExpenseItems.sumOf { it.actualSpent }

            // 该历史月总资金 = 基础分配 + 真实入账 + 来自更早历史月份的累积滚存
            val pTotalFunds = pExpenseAllocated + pIncomeTotal + cumulativeRollover
            // 该历史月期末结余，持续流转继承给下一个历史月份
            val pEndingBalance = pTotalFunds - pSpentTotal
            cumulativeRollover = if (pEndingBalance > 0.0) pEndingBalance else 0.0
        }

        val rolloverFromPreviousMonth = BigDecimal(cumulativeRollover).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 当月总资金池总量 = 支出预算基础分配 + 真实入账总额 + 历史全程累计滚存资金
        val totalActualAllocated = BigDecimal(totalExpenseAllocated + totalIncome + rolloverFromPreviousMonth).setScale(2, RoundingMode.HALF_UP).toDouble()
        // 当月可用总结余 = 总资金池总量 - 总消费支出
        val totalBalance = BigDecimal(totalActualAllocated - totalSpent).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 5. 按支出大类聚合信封卡片
        val categoryGroups = expenseBudgetItems.groupBy { it.categoryName }
        val categoryOverviewList = categoryGroups.map { (categoryName, items) ->
            val budgetTotal = BigDecimal(items.sumOf { it.totalPrice }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val allocatedTotal = BigDecimal(items.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val spentTotal = BigDecimal(items.sumOf { it.actualSpent }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val balanceTotal = BigDecimal(allocatedTotal - spentTotal).setScale(2, RoundingMode.HALF_UP).toDouble()
            CategoryOverview(
                categoryName = categoryName,
                categoryTotalBudget = budgetTotal,
                categoryActualAllocated = allocatedTotal,
                categoryActualSpent = spentTotal,
                categoryBalance = balanceTotal,
                isIncome = false,
                budgetItemList = items
            )
        }.sortedBy { categoryMap[it.categoryName]?.sortOrder ?: 99 }

        // 6. 按收入大类聚合入账概览（显示在看板页面）
        val incomeCategories = allCats.filter { it.isIncome }
        val incomeGroupMap = incomeTransactions.groupBy { it.categoryName }
        val incomeOverviewList = incomeCategories.map { cat ->
            val transList = incomeGroupMap[cat.categoryName] ?: emptyList()
            val sum = BigDecimal(transList.sumOf { it.amount }).setScale(2, RoundingMode.HALF_UP).toDouble()
            IncomeCategoryOverview(
                categoryName = cat.categoryName,
                totalIncome = sum,
                transactionCount = transList.size,
                transactionList = transList
            )
        }.sortedBy { categoryMap[it.categoryName]?.sortOrder ?: 99 }

        return MonthlyOverview(
            year = year,
            month = month,
            totalPlannedBudget = totalPlanned,
            totalActualAllocated = totalActualAllocated,
            totalActualSpent = totalSpent,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            rolloverFromPreviousMonth = rolloverFromPreviousMonth,
            categoryOverviewList = categoryOverviewList,
            incomeOverviewList = incomeOverviewList
        )
    }

    /**
     * 资金池配平健康检查（内存缓存加速 + 全动态联动）。
     *
     * 目标资金池基准为当月支出规划总额（总规划预算），
     * 各项已分配总额为当月所有支出大类实际注入分配之和，
     * 差额用于检验各细项实际注入金额是否与预算规划完全平衡。
     */
    suspend fun getBalanceCheck(year: Int, month: Int): BalanceCheckResult = withContext(Dispatchers.IO) {
        val key = Pair(year, month)
        balanceCheckCache[key] ?: run {
            val overview = getMonthlyOverview(year, month)
            val targetBenchmark = overview.totalPlannedBudget
            val allocatedTotal = BigDecimal(overview.categoryOverviewList.sumOf { it.categoryActualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val difference = BigDecimal(targetBenchmark - allocatedTotal).setScale(2, RoundingMode.HALF_UP).toDouble()

            val result = BalanceCheckResult(
                targetBenchmarkFund = targetBenchmark,
                allocatedTotalFund = allocatedTotal,
                balanceDifference = difference
            )
            balanceCheckCache[key] = result
            result
        }
    }

    /**
     * 清空所有数据并重新恢复默认初始账单数据。
     */
    suspend fun resetDatabaseToDefaults() = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            transactionDao.clearAllTransactions(db)
            budgetItemDao.clearAllBudgetItems(db)
            categoryDao.clearAllCategories(db)

            // 重新初始化写入预置数据
            database.seedInitialDatabaseData(db)
        }
        invalidateAllCaches()
        notifyDataChanged()
    }

    /**
     * 清空所有记账数据（清空所有月份预算、流水记录与自定义分类，并恢复默认空白分类体系）。
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            transactionDao.clearAllTransactions(db)
            budgetItemDao.clearAllBudgetItems(db)
            categoryDao.clearAllCategories(db)

            // 仅写入初始默认分类，不包含任何预算与流水
            database.seedDefaultCategories(db)
        }
        invalidateAllCaches()
        notifyDataChanged()
    }

    /**
     * 仅清空所有交易流水记录，并自动将所有预算细项的已消费金额归零、还原全部可用结余。
     */
    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            transactionDao.clearAllTransactions(db)
            val allBudgetItems = budgetItemDao.getAllBudgetItems()
            allBudgetItems.forEach { item ->
                budgetItemDao.updateSpentAndBalance(
                    itemId = item.itemId,
                    actualSpent = 0.0,
                    balance = item.actualAllocated,
                    db = db
                )
            }
        }
        invalidateAllCaches()
        notifyDataChanged()
    }

    /**
     * 仅清空所有月份的预算规划细项，保留分类体系与历史交易流水记录。
     */
    suspend fun clearAllBudgets() = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            budgetItemDao.clearAllBudgetItems(db)
        }
        invalidateAllCaches()
        notifyDataChanged()
    }
}
