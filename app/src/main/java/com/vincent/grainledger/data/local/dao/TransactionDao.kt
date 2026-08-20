package com.vincent.grainledger.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.TransactionRecord

/**
 * 交易流水数据访问对象 (TransactionDao)。
 *
 * 封装流水记录表的底层 SQL 查询、新增与删除操作。
 */
class TransactionDao(private val database: GrainLedgerDatabase) {

    /**
     * 获取指定年月的全部交易流水记录（按日期与主键降序排列）。
     */
    fun getTransactionsByMonth(year: Int, month: Int): List<TransactionRecord> {
        val resultList = mutableListOf<TransactionRecord>()
        val db = database.readableDatabase
        val cursor = db.query(
            GrainLedgerDatabase.TABLE_TRANSACTIONS,
            null,
            "${GrainLedgerDatabase.COL_TRANS_YEAR} = ? AND ${GrainLedgerDatabase.COL_TRANS_MONTH} = ?",
            arrayOf(year.toString(), month.toString()),
            null,
            null,
            "${GrainLedgerDatabase.COL_TRANS_DAY} DESC, ${GrainLedgerDatabase.COL_TRANS_ID} DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ID))
                val transYear = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_YEAR))
                val transMonth = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_MONTH))
                val transDay = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_DAY))
                val categoryName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_CATEGORY))
                val itemDetail = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ITEM_DETAIL))
                val amount = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_AMOUNT))
                val itemRemaining = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ITEM_REMAINING))
                val categoryRemaining = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_CATEGORY_REMAINING))
                val funder = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_FUNDER))
                val remark = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_REMARK))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_TIMESTAMP))

                resultList.add(
                    TransactionRecord(
                        recordId = id,
                        year = transYear,
                        month = transMonth,
                        day = transDay,
                        categoryName = categoryName,
                        detailName = itemDetail,
                        amount = amount,
                        itemRemaining = itemRemaining,
                        categoryRemaining = categoryRemaining,
                        funder = funder,
                        remark = remark,
                        timestamp = timestamp
                    )
                )
            }
        }
        return resultList
    }

    /**
     * 获取全部流水记录中包含的年份与月份列表。
     */
    fun getAvailableMonths(): List<Pair<Int, Int>> {
        val monthSet = sortedSetOf<Pair<Int, Int>>(Comparator { a, b ->
            if (a.first != b.first) a.first.compareTo(b.first) else a.second.compareTo(b.second)
        })
        val db = database.readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT ${GrainLedgerDatabase.COL_TRANS_YEAR}, ${GrainLedgerDatabase.COL_TRANS_MONTH} FROM ${GrainLedgerDatabase.TABLE_TRANSACTIONS}",
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
     * 根据主键查询单条流水。
     */
    fun getTransactionById(recordId: Long, db: SQLiteDatabase? = null): TransactionRecord? {
        val targetDb = db ?: database.readableDatabase
        val cursor = targetDb.query(
            GrainLedgerDatabase.TABLE_TRANSACTIONS,
            null,
            "${GrainLedgerDatabase.COL_TRANS_ID} = ?",
            arrayOf(recordId.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return TransactionRecord(
                    recordId = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ID)),
                    year = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_YEAR)),
                    month = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_MONTH)),
                    day = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_DAY)),
                    categoryName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_CATEGORY)),
                    detailName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ITEM_DETAIL)),
                    amount = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_AMOUNT)),
                    itemRemaining = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_ITEM_REMAINING)),
                    categoryRemaining = it.getDouble(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_CATEGORY_REMAINING)),
                    funder = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_FUNDER)),
                    remark = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_REMARK)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_TRANS_TIMESTAMP))
                )
            }
        }
        return null
    }

    /**
     * 插入单条流水记录。
     */
    fun insertTransaction(record: TransactionRecord, db: SQLiteDatabase? = null): Long {
        val targetDb = db ?: database.writableDatabase
        val values = ContentValues().apply {
            put(GrainLedgerDatabase.COL_TRANS_YEAR, record.year)
            put(GrainLedgerDatabase.COL_TRANS_MONTH, record.month)
            put(GrainLedgerDatabase.COL_TRANS_DAY, record.day)
            put(GrainLedgerDatabase.COL_TRANS_CATEGORY, record.categoryName)
            put(GrainLedgerDatabase.COL_TRANS_ITEM_DETAIL, record.detailName)
            put(GrainLedgerDatabase.COL_TRANS_AMOUNT, record.amount)
            put(GrainLedgerDatabase.COL_TRANS_ITEM_REMAINING, record.itemRemaining)
            put(GrainLedgerDatabase.COL_TRANS_CATEGORY_REMAINING, record.categoryRemaining)
            put(GrainLedgerDatabase.COL_TRANS_FUNDER, record.funder)
            put(GrainLedgerDatabase.COL_TRANS_REMARK, record.remark)
            put(GrainLedgerDatabase.COL_TRANS_TIMESTAMP, record.timestamp)
        }
        return targetDb.insert(GrainLedgerDatabase.TABLE_TRANSACTIONS, null, values)
    }

    /**
     * 根据主键删除流水。
     */
    fun deleteTransaction(recordId: Long, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(
            GrainLedgerDatabase.TABLE_TRANSACTIONS,
            "${GrainLedgerDatabase.COL_TRANS_ID} = ?",
            arrayOf(recordId.toString())
        )
    }

    /**
     * 清空流水表。
     */
    fun clearAllTransactions(db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(GrainLedgerDatabase.TABLE_TRANSACTIONS, null, null)
    }
}
