package com.vincent.grainledger.data.repository

import android.content.Context
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.BalanceCheckResult
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.CategoryOverview
import com.vincent.grainledger.data.model.MonthlyOverview
import com.vincent.grainledger.data.model.TransactionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 余粮统一数据仓库 (面向对象 DAO 数据库驱动版)。
 *
 * 基于 GrainLedgerDatabase 与 CategoryDao / BudgetItemDao / TransactionDao 构建，
 * 封装多表级联原子事务、双剩余自动实时核算、资金配平校验及 Excel 导入持久化。
 */
class LedgerRepository(context: Context) {

    private val database = GrainLedgerDatabase.getInstance(context.applicationContext)
    private val categoryDao = database.categoryDao
    private val budgetItemDao = database.budgetItemDao
    private val transactionDao = database.transactionDao

    // 内存数据版本触发流，用于状态变更通知
    private val _dataVersionFlow = MutableStateFlow(0L)
    val dataVersionFlow: StateFlow<Long> = _dataVersionFlow.asStateFlow()

    private fun notifyDataChanged() {
        _dataVersionFlow.value = System.currentTimeMillis()
    }

    /**
     * 获取所有预算分类列表（按排序序号升序排列）。
     */
    suspend fun getAllCategories(): List<BudgetCategory> = withContext(Dispatchers.IO) {
        categoryDao.getAllCategories()
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
     * 获取指定月份的全部记账流水记录（按日期降序排列）。
     */
    suspend fun getTransactionsByMonth(year: Int, month: Int): List<TransactionRecord> = withContext(Dispatchers.IO) {
        transactionDao.getTransactionsByMonth(year, month)
    }

    /**
     * 获取指定年月的全部预算项列表。
     *
     * @param year 指定年份
     * @param month 指定月份
     * @return 预算细项列表
     */
    suspend fun getBudgetItemsByMonth(year: Int, month: Int): List<BudgetItem> = withContext(Dispatchers.IO) {
        budgetItemDao.getBudgetItemsByMonth(year, month)
    }

    /**
     * 保存或更新预算细项。
     *
     * @param budgetItem 待保存的预算实体
     * @return 插入或更新后的数据库主键标识
     */
    suspend fun saveBudgetItem(budgetItem: BudgetItem): Long = withContext(Dispatchers.IO) {
        val resultId = budgetItemDao.saveBudgetItem(budgetItem)
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
        notifyDataChanged()
    }

    /**
     * 删除指定的预算项。
     */
    suspend fun deleteBudgetItem(itemId: Long): Boolean = withContext(Dispatchers.IO) {
        val rowsAffected = budgetItemDao.deleteBudgetItem(itemId)
        if (rowsAffected > 0) {
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
                // 若不复制，则为新月份插入一条默认分类的空预算项或直接保留空列表
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
        notifyDataChanged()
        true
    }

    /**
     * 获取所有可用的年份与月份列表。
     */
    suspend fun getAvailableMonths(): List<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        val budgetMonths = budgetItemDao.getAvailableMonths()
        val txMonths = transactionDao.getAvailableMonths()
        val combined = (budgetMonths + txMonths).distinct().sortedWith(Comparator { a, b ->
            if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
        })
        if (combined.isNotEmpty()) {
            combined
        } else {
            // 默认若为空则补充 2026 年 8~12 月
            (8..12).map { Pair(2026, it) }
        }
    }

    /**
     * 新增一笔记账流水，并在数据库事务中自动联动计算【具体剩余】与【类剩余】，同时扣减对应预算项的结余。
     *
     * @param transaction 待录入的交易记录
     * @return 新增流水主键标识
     */
    suspend fun recordTransaction(transaction: TransactionRecord): Long = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            // 1. 查找对应的预算项
            val matchedItem = budgetItemDao.findBudgetItem(
                transaction.year,
                transaction.month,
                transaction.categoryName,
                transaction.detailName,
                db
            )

            // 支出金额（负数，例如 -180.59）
            val amountDelta = transaction.amount
            val spentIncrease = -amountDelta // 扣减支出为增加消费

            val computedItemRemaining: Double
            if (matchedItem != null) {
                val newActualSpent = matchedItem.actualSpent + spentIncrease
                val newBalance = matchedItem.actualAllocated - newActualSpent
                computedItemRemaining = BigDecimal(newBalance).setScale(2, RoundingMode.HALF_UP).toDouble()

                // 更新预算细项已消费与结余
                budgetItemDao.updateSpentAndBalance(
                    itemId = matchedItem.itemId,
                    actualSpent = newActualSpent,
                    balance = newBalance,
                    db = db
                )
            } else {
                computedItemRemaining = 0.0
            }

            // 2. 计算大类剩余（该大类当前所有实际加入 - 该大类当前所有消费）
            val categoryItems = budgetItemDao.getBudgetItemsByMonth(transaction.year, transaction.month)
                .filter { it.categoryName == transaction.categoryName }

            val categoryTotalAllocated = categoryItems.sumOf { it.actualAllocated }
            val categoryTotalSpent = categoryItems.sumOf { it.actualSpent }
            val computedCategoryRemaining = BigDecimal(categoryTotalAllocated - categoryTotalSpent).setScale(2, RoundingMode.HALF_UP).toDouble()

            // 3. 写入流水表
            val recordToSave = transaction.copy(
                itemRemaining = computedItemRemaining,
                categoryRemaining = computedCategoryRemaining
            )
            val newId = transactionDao.insertTransaction(recordToSave, db)
            notifyDataChanged()
            newId
        }
    }

    /**
     * 删除指定的交易流水，并在数据库事务中回退预算细项中的已消费和结余。
     */
    suspend fun deleteTransaction(recordId: Long): Boolean = withContext(Dispatchers.IO) {
        database.runInTransaction { db ->
            val targetRecord = transactionDao.getTransactionById(recordId, db)
            if (targetRecord != null) {
                // 还原预算项
                val matchedItem = budgetItemDao.findBudgetItem(
                    targetRecord.year,
                    targetRecord.month,
                    targetRecord.categoryName,
                    targetRecord.detailName,
                    db
                )

                if (matchedItem != null) {
                    val restoredSpent = (matchedItem.actualSpent - (-targetRecord.amount)).coerceAtLeast(0.0)
                    val restoredBalance = matchedItem.actualAllocated - restoredSpent
                    budgetItemDao.updateSpentAndBalance(
                        itemId = matchedItem.itemId,
                        actualSpent = restoredSpent,
                        balance = restoredBalance,
                        db = db
                    )
                }

                // 删除流水
                transactionDao.deleteTransaction(recordId, db)
                notifyDataChanged()
                true
            } else {
                false
            }
        }
    }

    /**
     * 计算并获取指定年月的月度综合汇总数据（对应《综合查看》看板）。
     */
    suspend fun getMonthlyOverview(year: Int, month: Int): MonthlyOverview = withContext(Dispatchers.IO) {
        val budgetItems = getBudgetItemsByMonth(year, month)
        val categoryMap = getAllCategories().associateBy { it.categoryName }

        val totalPlanned = BigDecimal(budgetItems.sumOf { it.totalPrice }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalAllocated = BigDecimal(budgetItems.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 区分收入类分类与支出类分类
        val incomeItems = budgetItems.filter { categoryMap[it.categoryName]?.isIncome == true }
        val expenseItems = budgetItems.filter { categoryMap[it.categoryName]?.isIncome != true }

        val totalIncome = BigDecimal(incomeItems.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalSpent = BigDecimal(expenseItems.sumOf { it.actualSpent }).setScale(2, RoundingMode.HALF_UP).toDouble()
        // 当月可用结余 = 总资金量（基础预算注入 + 收入类金额） - 总支出消费
        val totalBalance = BigDecimal(totalAllocated - totalSpent).setScale(2, RoundingMode.HALF_UP).toDouble()

        // 按分类聚合
        val categoryGroups = budgetItems.groupBy { it.categoryName }
        val categoryOverviewList = categoryGroups.map { (categoryName, items) ->
            val catDef = categoryMap[categoryName]
            val isIncomeCat = catDef?.isIncome ?: false
            val budgetTotal = BigDecimal(items.sumOf { it.totalPrice }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val allocatedTotal = BigDecimal(items.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val spentTotal = BigDecimal(items.sumOf { it.actualSpent }).setScale(2, RoundingMode.HALF_UP).toDouble()
            val balanceTotal = if (isIncomeCat) {
                BigDecimal(allocatedTotal).setScale(2, RoundingMode.HALF_UP).toDouble()
            } else {
                BigDecimal(allocatedTotal - spentTotal).setScale(2, RoundingMode.HALF_UP).toDouble()
            }
            CategoryOverview(
                categoryName = categoryName,
                categoryTotalBudget = budgetTotal,
                categoryActualAllocated = allocatedTotal,
                categoryActualSpent = spentTotal,
                categoryBalance = balanceTotal,
                isIncome = isIncomeCat,
                budgetItemList = items
            )
        }.sortedBy { categoryMap[it.categoryName]?.sortOrder ?: 99 }

        MonthlyOverview(
            year = year,
            month = month,
            totalPlannedBudget = totalPlanned,
            totalActualAllocated = totalAllocated,
            totalActualSpent = totalSpent,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            categoryOverviewList = categoryOverviewList
        )
    }

    /**
     * 资金池配平健康检查（对应 Excel《草稿页》）。
     *
     * @param year 年份
     * @param month 月份
     * @param targetBenchmarkFund 初始基准金额（默认 10000.00）
     * @return 配平检查结果
     */
    suspend fun getBalanceCheck(year: Int, month: Int, targetBenchmarkFund: Double = 10000.0): BalanceCheckResult = withContext(Dispatchers.IO) {
        val budgetItems = getBudgetItemsByMonth(year, month)
        val allocatedTotal = BigDecimal(budgetItems.sumOf { it.actualAllocated }).setScale(2, RoundingMode.HALF_UP).toDouble()
        val difference = BigDecimal(targetBenchmarkFund - allocatedTotal).setScale(2, RoundingMode.HALF_UP).toDouble()

        BalanceCheckResult(
            targetBenchmarkFund = targetBenchmarkFund,
            allocatedTotalFund = allocatedTotal,
            balanceDifference = difference
        )
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
        notifyDataChanged()
    }
}
