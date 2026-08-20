package com.vincent.grainledger.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.vincent.grainledger.data.local.dao.BudgetItemDao
import com.vincent.grainledger.data.local.dao.CategoryDao
import com.vincent.grainledger.data.local.dao.TransactionDao
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.TransactionRecord

/**
 * 余粮统一本地数据库管理器 (GrainLedgerDatabase)。
 *
 * 采用 DAO 分层架构与 SQLite 事务控制，负责底层的持久化存储、表结构维护与数据预置。
 * 对上层提供面向对象的 CategoryDao、BudgetItemDao 与 TransactionDao 访问接口。
 */
class GrainLedgerDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "grain_ledger.db"
        const val DATABASE_VERSION = 1

        // 分类表常量
        const val TABLE_CATEGORIES = "budget_categories"
        const val COL_CAT_ID = "id"
        const val COL_CAT_NAME = "name"
        const val COL_CAT_ICON = "icon"
        const val COL_CAT_COLOR = "color_value"
        const val COL_CAT_SORT = "sort_order"

        // 预算细项表常量
        const val TABLE_BUDGET_ITEMS = "budget_items"
        const val COL_BUDGET_ID = "id"
        const val COL_BUDGET_YEAR = "year"
        const val COL_BUDGET_MONTH = "month"
        const val COL_BUDGET_CATEGORY = "category_name"
        const val COL_BUDGET_ITEM_NAME = "item_name"
        const val COL_BUDGET_UNIT_PRICE = "unit_price"
        const val COL_BUDGET_QUANTITY = "quantity"
        const val COL_BUDGET_TOTAL_PRICE = "total_price"
        const val COL_BUDGET_ACTUAL_ALLOCATED = "actual_allocated"
        const val COL_BUDGET_FUNDER = "funder"
        const val COL_BUDGET_ACTUAL_SPENT = "actual_spent"
        const val COL_BUDGET_BALANCE = "balance"
        const val COL_BUDGET_REMARK = "remark"

        // 交易流水表常量
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TRANS_ID = "id"
        const val COL_TRANS_YEAR = "year"
        const val COL_TRANS_MONTH = "month"
        const val COL_TRANS_DAY = "day"
        const val COL_TRANS_CATEGORY = "category_name"
        const val COL_TRANS_ITEM_DETAIL = "item_detail"
        const val COL_TRANS_AMOUNT = "amount"
        const val COL_TRANS_ITEM_REMAINING = "item_remaining"
        const val COL_TRANS_CATEGORY_REMAINING = "category_remaining"
        const val COL_TRANS_FUNDER = "funder"
        const val COL_TRANS_REMARK = "remark"
        const val COL_TRANS_TIMESTAMP = "timestamp"

        @Volatile
        private var instance: GrainLedgerDatabase? = null

        /**
         * 获取数据库全局单例。
         */
        fun getInstance(context: Context): GrainLedgerDatabase {
            return instance ?: synchronized(this) {
                instance ?: GrainLedgerDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    val categoryDao: CategoryDao by lazy { CategoryDao(this) }
    val budgetItemDao: BudgetItemDao by lazy { BudgetItemDao(this) }
    val transactionDao: TransactionDao by lazy { TransactionDao(this) }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建分类表
        val createCategoriesSql = """
            CREATE TABLE IF NOT EXISTS $TABLE_CATEGORIES (
                $COL_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CAT_NAME TEXT NOT NULL UNIQUE,
                $COL_CAT_ICON TEXT NOT NULL,
                $COL_CAT_COLOR INTEGER NOT NULL,
                $COL_CAT_SORT INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()
        db.execSQL(createCategoriesSql)

        // 创建预算项表
        val createBudgetItemsSql = """
            CREATE TABLE IF NOT EXISTS $TABLE_BUDGET_ITEMS (
                $COL_BUDGET_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BUDGET_YEAR INTEGER NOT NULL,
                $COL_BUDGET_MONTH INTEGER NOT NULL,
                $COL_BUDGET_CATEGORY TEXT NOT NULL,
                $COL_BUDGET_ITEM_NAME TEXT NOT NULL,
                $COL_BUDGET_UNIT_PRICE REAL NOT NULL,
                $COL_BUDGET_QUANTITY REAL NOT NULL DEFAULT 1.0,
                $COL_BUDGET_TOTAL_PRICE REAL NOT NULL,
                $COL_BUDGET_ACTUAL_ALLOCATED REAL NOT NULL,
                $COL_BUDGET_FUNDER TEXT NOT NULL,
                $COL_BUDGET_ACTUAL_SPENT REAL NOT NULL DEFAULT 0.0,
                $COL_BUDGET_BALANCE REAL NOT NULL,
                $COL_BUDGET_REMARK TEXT NOT NULL DEFAULT ''
            );
        """.trimIndent()
        db.execSQL(createBudgetItemsSql)

        // 创建交易流水表
        val createTransactionsSql = """
            CREATE TABLE IF NOT EXISTS $TABLE_TRANSACTIONS (
                $COL_TRANS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TRANS_YEAR INTEGER NOT NULL,
                $COL_TRANS_MONTH INTEGER NOT NULL,
                $COL_TRANS_DAY INTEGER NOT NULL,
                $COL_TRANS_CATEGORY TEXT NOT NULL,
                $COL_TRANS_ITEM_DETAIL TEXT NOT NULL,
                $COL_TRANS_AMOUNT REAL NOT NULL,
                $COL_TRANS_ITEM_REMAINING REAL NOT NULL,
                $COL_TRANS_CATEGORY_REMAINING REAL NOT NULL,
                $COL_TRANS_FUNDER TEXT NOT NULL,
                $COL_TRANS_REMARK TEXT NOT NULL DEFAULT '',
                $COL_TRANS_TIMESTAMP INTEGER NOT NULL
            );
        """.trimIndent()
        db.execSQL(createTransactionsSql)

        // 预填充初始数据
        seedInitialDatabaseData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 数据库迁移逻辑
    }

    /**
     * 在 SQLite 事务中执行操作。
     */
    fun <T> runInTransaction(block: (SQLiteDatabase) -> T): T {
        val database = writableDatabase
        database.beginTransaction()
        return try {
            val result = block(database)
            database.setTransactionSuccessful()
            result
        } finally {
            database.endTransaction()
        }
    }

    /**
     * 写入 2026年 8月~12月完整预算规划与流水数据。
     */
    fun seedInitialDatabaseData(db: SQLiteDatabase) {
        // 1. 分类
        BudgetCategory.defaultCategories.forEach { category ->
            val values = ContentValues().apply {
                put(COL_CAT_NAME, category.categoryName)
                put(COL_CAT_ICON, category.iconName)
                put(COL_CAT_COLOR, category.themeColorValue)
                put(COL_CAT_SORT, category.sortOrder)
            }
            db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }

        // 2. 预算细项
        val initialBudgetItems = listOf(
            // 8月份
            BudgetItem(0, 2026, 8, "强制类", "学费", 5200.0, 1.0, 5200.0, 5200.0, "默认账户", 5200.0, 0.0),
            BudgetItem(0, 2026, 8, "强制类", "学分费", 50.0, 12.0, 600.0, 600.0, "默认账户", 600.0, 0.0),
            BudgetItem(0, 2026, 8, "强制类", "住宿费", 1150.0, 1.0, 1150.0, 1150.0, "默认账户", 0.0, 1150.0),
            BudgetItem(0, 2026, 8, "强制类", "车费", 75.0, 1.0, 75.0, 75.0, "默认账户", 0.0, 75.0),
            BudgetItem(0, 2026, 8, "强制类", "电费", 56.0, 1.0, 56.0, 56.0, "默认账户", 0.0, 56.0),
            BudgetItem(0, 2026, 8, "强制类", "话费", 20.0, 1.0, 20.0, 20.0, "默认账户", 0.0, 20.0),
            BudgetItem(0, 2026, 8, "强制类", "洗衣费", 6.0, 1.0, 6.0, 6.0, "默认账户", 0.0, 6.0),
            BudgetItem(0, 2026, 8, "强制类", "沐浴头", 150.0, 1.0, 150.0, 150.0, "默认账户", 0.0, 150.0),
            BudgetItem(0, 2026, 8, "强制类", "纸巾", 10.0, 1.0, 10.0, 10.0, "默认账户", 0.0, 10.0),
            BudgetItem(0, 2026, 8, "强制类", "教材费", 307.6, 1.0, 307.6, 307.6, "默认账户", 213.5, 94.1),
            BudgetItem(0, 2026, 8, "强制类", "实习费", 2000.0, 1.0, 2000.0, 2000.0, "默认账户", 0.0, 2000.0),
            BudgetItem(0, 2026, 8, "强制类", "日常吃", 30.0, 6.0, 180.0, 180.0, "默认账户", 0.0, 180.0),
            BudgetItem(0, 2026, 8, "强制类", "洗衣液", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 8, "饮食类", "减肥吃", 7.0, 6.0, 42.0, 42.0, "默认账户", 0.0, 42.0),
            BudgetItem(0, 2026, 8, "预留类", "存储金", 10.0, 6.0, 60.0, 60.0, "默认账户", 0.0, 60.0),
            BudgetItem(0, 2026, 8, "文具类", "文具品", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 8, "恋爱类", "谈恋爱", 10.0, 6.0, 60.0, 5.4, "默认账户", 0.0, 5.4),
            BudgetItem(0, 2026, 8, "生活类", "生活品", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 8, "提升类", "四级", 24.0, 2.0, 48.0, 48.0, "默认账户", 0.0, 48.0),

            // 9月份
            BudgetItem(0, 2026, 9, "强制类", "电费", 80.0, 1.0, 80.0, 80.0, "默认账户", 0.0, 80.0),
            BudgetItem(0, 2026, 9, "强制类", "话费", 20.0, 1.0, 20.0, 20.0, "默认账户", 0.0, 20.0),
            BudgetItem(0, 2026, 9, "强制类", "洗衣费", 6.0, 4.2, 25.2, 25.2, "默认账户", 0.0, 25.2),
            BudgetItem(0, 2026, 9, "强制类", "纸巾", 10.0, 2.5, 25.0, 25.0, "默认账户", 0.0, 25.0),
            BudgetItem(0, 2026, 9, "强制类", "日常吃", 30.0, 30.0, 900.0, 900.0, "默认账户", 0.0, 900.0),
            BudgetItem(0, 2026, 9, "强制类", "洗衣液", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 9, "饮食类", "减肥吃", 7.0, 30.0, 210.0, 210.0, "默认账户", 0.0, 210.0),
            BudgetItem(0, 2026, 9, "预留类", "存储金", 100.0, 1.0, 100.0, 100.0, "默认账户", 0.0, 100.0),
            BudgetItem(0, 2026, 9, "文具类", "文具品", 5.0, 1.0, 5.0, 5.0, "默认账户", 0.0, 5.0),
            BudgetItem(0, 2026, 9, "恋爱类", "谈恋爱", 120.0, 1.0, 120.0, 120.0, "默认账户", 0.0, 120.0),

            // 10月份
            BudgetItem(0, 2026, 10, "强制类", "电费", 81.0, 1.0, 81.0, 81.0, "默认账户", 0.0, 81.0),
            BudgetItem(0, 2026, 10, "强制类", "话费", 20.0, 1.0, 20.0, 20.0, "默认账户", 0.0, 20.0),
            BudgetItem(0, 2026, 10, "强制类", "洗衣费", 6.0, 4.2, 25.2, 25.2, "默认账户", 0.0, 25.2),
            BudgetItem(0, 2026, 10, "强制类", "纸巾", 10.0, 2.5, 25.0, 25.0, "默认账户", 0.0, 25.0),
            BudgetItem(0, 2026, 10, "强制类", "日常吃", 30.0, 30.0, 900.0, 900.0, "默认账户", 0.0, 900.0),
            BudgetItem(0, 2026, 10, "强制类", "洗衣液", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 10, "饮食类", "减肥吃", 7.0, 30.0, 210.0, 210.0, "默认账户", 0.0, 210.0),
            BudgetItem(0, 2026, 10, "预留类", "存储金", 100.0, 1.0, 100.0, 100.0, "默认账户", 0.0, 100.0),
            BudgetItem(0, 2026, 10, "文具类", "文具品", 5.0, 1.0, 5.0, 5.0, "默认账户", 0.0, 5.0),
            BudgetItem(0, 2026, 10, "恋爱类", "谈恋爱", 120.0, 1.0, 120.0, 120.0, "默认账户", 0.0, 120.0),
            BudgetItem(0, 2026, 10, "生活类", "生活品", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),

            // 11月份
            BudgetItem(0, 2026, 11, "强制类", "电费", 80.0, 1.0, 80.0, 80.0, "默认账户", 0.0, 80.0),
            BudgetItem(0, 2026, 11, "强制类", "话费", 20.0, 1.0, 20.0, 20.0, "默认账户", 0.0, 20.0),
            BudgetItem(0, 2026, 11, "强制类", "洗衣费", 6.0, 4.2, 25.2, 25.2, "默认账户", 0.0, 25.2),
            BudgetItem(0, 2026, 11, "强制类", "纸巾", 10.0, 2.5, 25.0, 25.0, "默认账户", 0.0, 25.0),
            BudgetItem(0, 2026, 11, "强制类", "日常吃", 30.0, 30.0, 900.0, 900.0, "默认账户", 0.0, 900.0),
            BudgetItem(0, 2026, 11, "强制类", "洗衣液", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 11, "饮食类", "减肥吃", 7.0, 30.0, 210.0, 210.0, "默认账户", 0.0, 210.0),
            BudgetItem(0, 2026, 11, "预留类", "存储金", 100.0, 1.0, 100.0, 100.0, "默认账户", 0.0, 100.0),
            BudgetItem(0, 2026, 11, "文具类", "文具品", 5.0, 1.0, 5.0, 5.0, "默认账户", 0.0, 5.0),
            BudgetItem(0, 2026, 11, "恋爱类", "谈恋爱", 120.0, 1.0, 120.0, 120.0, "默认账户", 0.0, 120.0),

            // 12月份
            BudgetItem(0, 2026, 12, "强制类", "电费", 81.0, 1.0, 81.0, 81.0, "默认账户", 0.0, 81.0),
            BudgetItem(0, 2026, 12, "强制类", "话费", 20.0, 1.0, 20.0, 20.0, "默认账户", 0.0, 20.0),
            BudgetItem(0, 2026, 12, "强制类", "洗衣费", 6.0, 4.2, 25.2, 25.2, "默认账户", 0.0, 25.2),
            BudgetItem(0, 2026, 12, "强制类", "纸巾", 10.0, 2.5, 25.0, 25.0, "默认账户", 0.0, 25.0),
            BudgetItem(0, 2026, 12, "强制类", "日常吃", 30.0, 30.0, 900.0, 900.0, "默认账户", 0.0, 900.0),
            BudgetItem(0, 2026, 12, "强制类", "洗衣液", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0),
            BudgetItem(0, 2026, 12, "饮食类", "减肥吃", 7.0, 30.0, 210.0, 210.0, "默认账户", 0.0, 210.0),
            BudgetItem(0, 2026, 12, "预留类", "存储金", 100.0, 1.0, 100.0, 100.0, "默认账户", 0.0, 100.0),
            BudgetItem(0, 2026, 12, "文具类", "文具品", 5.0, 1.0, 5.0, 5.0, "默认账户", 0.0, 5.0),
            BudgetItem(0, 2026, 12, "恋爱类", "谈恋爱", 120.0, 1.0, 120.0, 120.0, "默认账户", 0.0, 120.0),
            BudgetItem(0, 2026, 12, "生活类", "生活品", 30.0, 1.0, 30.0, 30.0, "默认账户", 0.0, 30.0)
        )

        initialBudgetItems.forEach { item ->
            val values = ContentValues().apply {
                put(COL_BUDGET_YEAR, item.year)
                put(COL_BUDGET_MONTH, item.month)
                put(COL_BUDGET_CATEGORY, item.categoryName)
                put(COL_BUDGET_ITEM_NAME, item.detailName)
                put(COL_BUDGET_UNIT_PRICE, item.unitPrice)
                put(COL_BUDGET_QUANTITY, item.quantity)
                put(COL_BUDGET_TOTAL_PRICE, item.totalPrice)
                put(COL_BUDGET_ACTUAL_ALLOCATED, item.actualAllocated)
                put(COL_BUDGET_FUNDER, item.funder)
                put(COL_BUDGET_ACTUAL_SPENT, item.actualSpent)
                put(COL_BUDGET_BALANCE, item.balance)
                put(COL_BUDGET_REMARK, item.remark)
            }
            db.insert(TABLE_BUDGET_ITEMS, null, values)
        }

        // 3. 初始流水
        val initialTransactions = listOf(
            TransactionRecord(
                0, 2026, 8, 18, "强制类", "教材费", -180.59,
                itemRemaining = 127.01, categoryRemaining = 9604.01, funder = "默认账户", remark = "第一批教材费"
            ),
            TransactionRecord(
                0, 2026, 8, 18, "强制类", "教材费", -32.91,
                itemRemaining = 94.10, categoryRemaining = 9571.10, funder = "默认账户", remark = "补交教材费"
            ),
            TransactionRecord(
                0, 2026, 8, 19, "强制类", "学费", -5200.00,
                itemRemaining = 0.00, categoryRemaining = 4371.10, funder = "默认账户", remark = "秋季学费交纳"
            ),
            TransactionRecord(
                0, 2026, 8, 19, "强制类", "学分费", -600.00,
                itemRemaining = 0.00, categoryRemaining = 3771.10, funder = "默认账户", remark = "学分学费交纳"
            )
        )

        initialTransactions.forEach { transaction ->
            val values = ContentValues().apply {
                put(COL_TRANS_YEAR, transaction.year)
                put(COL_TRANS_MONTH, transaction.month)
                put(COL_TRANS_DAY, transaction.day)
                put(COL_TRANS_CATEGORY, transaction.categoryName)
                put(COL_TRANS_ITEM_DETAIL, transaction.detailName)
                put(COL_TRANS_AMOUNT, transaction.amount)
                put(COL_TRANS_ITEM_REMAINING, transaction.itemRemaining)
                put(COL_TRANS_CATEGORY_REMAINING, transaction.categoryRemaining)
                put(COL_TRANS_FUNDER, transaction.funder)
                put(COL_TRANS_REMARK, transaction.remark)
                put(COL_TRANS_TIMESTAMP, transaction.timestamp)
            }
            db.insert(TABLE_TRANSACTIONS, null, values)
        }
    }
}
