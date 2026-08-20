package com.vincent.grainledger.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.BudgetItem

/**
 * 预算细项数据访问对象 (BudgetItemDao)。
 *
 * 封装预算细项表的底层 SQL 查询、新增、更新与删除操作。
 */
class BudgetItemDao(private val database: GrainLedgerDatabase) {

    /**
     * 获取指定年月的所有预算项列表。
     */
    fun getBudgetItemsByMonth(year: Int, month: Int): List<BudgetItem> {
        val resultList = mutableListOf<BudgetItem>()
        val db = database.readableDatabase
        val cursor = db.query(
            GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
            null,
            "${GrainLedgerDatabase.COL_BUDGET_YEAR} = ? AND ${GrainLedgerDatabase.COL_BUDGET_MONTH} = ?",
            arrayOf(year.toString(), month.toString()),
            null,
            null,
            "${GrainLedgerDatabase.COL_BUDGET_ID} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ID))
                val itemYear = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_YEAR))
                val itemMonth = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_MONTH))
                val categoryName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_CATEGORY))
                val itemName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ITEM_NAME))
                val unitPrice = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_UNIT_PRICE))
                val quantity = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_QUANTITY))
                val totalPrice = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_TOTAL_PRICE))
                val actualAllocated = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ACTUAL_ALLOCATED))
                val funder = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_FUNDER))
                val actualSpent = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ACTUAL_SPENT))
                val balance = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_BALANCE))
                val remark = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_REMARK))

                resultList.add(
                    BudgetItem(
                        itemId = id,
                        year = itemYear,
                        month = itemMonth,
                        categoryName = categoryName,
                        detailName = itemName,
                        unitPrice = unitPrice,
                        quantity = quantity,
                        totalPrice = totalPrice,
                        actualAllocated = actualAllocated,
                        funder = funder,
                        actualSpent = actualSpent,
                        balance = balance,
                        remark = remark
                    )
                )
            }
        }
        return resultList
    }

    /**
     * 获取所有可用的年份与月份列表。
     */
    fun getAvailableMonths(): List<Pair<Int, Int>> {
        val monthSet = sortedSetOf<Pair<Int, Int>>(Comparator { a, b ->
            if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
        })
        val db = database.readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT ${GrainLedgerDatabase.COL_BUDGET_YEAR}, ${GrainLedgerDatabase.COL_BUDGET_MONTH} FROM ${GrainLedgerDatabase.TABLE_BUDGET_ITEMS}",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                val year = it.getInt(0)
                val month = it.getInt(1)
                monthSet.add(Pair(year, month))
            }
        }
        return monthSet.toList()
    }

    /**
     * 根据年月与类别、详情名称精确查找预算项。
     */
    fun findBudgetItem(year: Int, month: Int, categoryName: String, detailName: String, db: SQLiteDatabase? = null): BudgetItem? {
        val targetDb = db ?: database.readableDatabase
        val cursor = targetDb.query(
            GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
            null,
            "${GrainLedgerDatabase.COL_BUDGET_YEAR} = ? AND ${GrainLedgerDatabase.COL_BUDGET_MONTH} = ? AND ${GrainLedgerDatabase.COL_BUDGET_CATEGORY} = ? AND ${GrainLedgerDatabase.COL_BUDGET_ITEM_NAME} = ?",
            arrayOf(year.toString(), month.toString(), categoryName, detailName),
            null,
            null,
            null,
            "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                return BudgetItem(
                    itemId = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ID)),
                    year = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_YEAR)),
                    month = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_MONTH)),
                    categoryName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_CATEGORY)),
                    detailName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ITEM_NAME)),
                    unitPrice = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_UNIT_PRICE)),
                    quantity = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_QUANTITY)),
                    totalPrice = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_TOTAL_PRICE)),
                    actualAllocated = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ACTUAL_ALLOCATED)),
                    funder = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_FUNDER)),
                    actualSpent = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_ACTUAL_SPENT)),
                    balance = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_BALANCE)),
                    remark = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_BUDGET_REMARK))
                )
            }
        }
        return null
    }

    /**
     * 保存或更新预算项。
     */
    fun saveBudgetItem(item: BudgetItem, db: SQLiteDatabase? = null): Long {
        val targetDb = db ?: database.writableDatabase
        val values = ContentValues().apply {
            put(GrainLedgerDatabase.COL_BUDGET_YEAR, item.year)
            put(GrainLedgerDatabase.COL_BUDGET_MONTH, item.month)
            put(GrainLedgerDatabase.COL_BUDGET_CATEGORY, item.categoryName)
            put(GrainLedgerDatabase.COL_BUDGET_ITEM_NAME, item.detailName)
            put(GrainLedgerDatabase.COL_BUDGET_UNIT_PRICE, item.unitPrice)
            put(GrainLedgerDatabase.COL_BUDGET_QUANTITY, item.quantity)
            put(GrainLedgerDatabase.COL_BUDGET_TOTAL_PRICE, item.totalPrice)
            put(GrainLedgerDatabase.COL_BUDGET_ACTUAL_ALLOCATED, item.actualAllocated)
            put(GrainLedgerDatabase.COL_BUDGET_FUNDER, item.funder)
            put(GrainLedgerDatabase.COL_BUDGET_ACTUAL_SPENT, item.actualSpent)
            put(GrainLedgerDatabase.COL_BUDGET_BALANCE, item.balance)
            put(GrainLedgerDatabase.COL_BUDGET_REMARK, item.remark)
        }
        return if (item.itemId > 0L) {
            targetDb.update(
                GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
                values,
                "${GrainLedgerDatabase.COL_BUDGET_ID} = ?",
                arrayOf(item.itemId.toString())
            )
            item.itemId
        } else {
            targetDb.insert(GrainLedgerDatabase.TABLE_BUDGET_ITEMS, null, values)
        }
    }

    /**
     * 更新预算项的实际消费与结余。
     */
    fun updateSpentAndBalance(itemId: Long, actualSpent: Double, balance: Double, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        val values = ContentValues().apply {
            put(GrainLedgerDatabase.COL_BUDGET_ACTUAL_SPENT, actualSpent)
            put(GrainLedgerDatabase.COL_BUDGET_BALANCE, balance)
        }
        return targetDb.update(
            GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
            values,
            "${GrainLedgerDatabase.COL_BUDGET_ID} = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * 根据主键删除预算项。
     */
    fun deleteBudgetItem(itemId: Long, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(
            GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
            "${GrainLedgerDatabase.COL_BUDGET_ID} = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * 清空预算项表。
     */
    fun clearAllBudgetItems(db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(GrainLedgerDatabase.TABLE_BUDGET_ITEMS, null, null)
    }
}
