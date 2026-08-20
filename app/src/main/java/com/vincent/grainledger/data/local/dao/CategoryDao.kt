package com.vincent.grainledger.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.BudgetCategory

/**
 * 分类数据访问对象 (CategoryDao)。
 *
 * 封装分类表的底层 SQL 查询、新增与清理操作。
 */
class CategoryDao(private val database: GrainLedgerDatabase) {

    /**
     * 获取全部预算分类列表。
     */
    fun getAllCategories(): List<BudgetCategory> {
        val resultList = mutableListOf<BudgetCategory>()
        val db = database.readableDatabase
        val cursor = db.query(
            GrainLedgerDatabase.TABLE_CATEGORIES,
            null,
            null,
            null,
            null,
            null,
            "${GrainLedgerDatabase.COL_CAT_SORT} ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_ID))
                val name = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_NAME))
                val icon = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_ICON))
                val colorValue = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_COLOR))
                val sortOrder = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_SORT))
                resultList.add(
                    BudgetCategory(
                        categoryId = id,
                        categoryName = name,
                        iconName = icon,
                        themeColorValue = colorValue,
                        sortOrder = sortOrder
                    )
                )
            }
        }
        return resultList
    }

    /**
     * 插入或替换单条分类数据。
     */
    fun insertCategory(category: BudgetCategory, db: SQLiteDatabase? = null): Long {
        val targetDb = db ?: database.writableDatabase
        val values = ContentValues().apply {
            if (category.categoryId > 0L) {
                put(GrainLedgerDatabase.COL_CAT_ID, category.categoryId)
            }
            put(GrainLedgerDatabase.COL_CAT_NAME, category.categoryName)
            put(GrainLedgerDatabase.COL_CAT_ICON, category.iconName)
            put(GrainLedgerDatabase.COL_CAT_COLOR, category.themeColorValue)
            put(GrainLedgerDatabase.COL_CAT_SORT, category.sortOrder)
        }
        return targetDb.insertWithOnConflict(
            GrainLedgerDatabase.TABLE_CATEGORIES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * 批量插入分类数据。
     */
    fun insertCategories(categories: List<BudgetCategory>) {
        database.runInTransaction { db ->
            categories.forEach { insertCategory(it, db) }
        }
    }

    /**
     * 清空分类表。
     */
    fun clearAllCategories(db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(GrainLedgerDatabase.TABLE_CATEGORIES, null, null)
    }
}
