package com.vincent.grainledger.data.local.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.vincent.grainledger.data.local.GrainLedgerDatabase
import com.vincent.grainledger.data.model.BudgetCategory

/**
 * 分类数据访问对象 (CategoryDao)。
 *
 * 封装分类表的底层 SQL 查询、新增、更新、重命名级联与删除操作。
 */
class CategoryDao(private val database: GrainLedgerDatabase) {

    /**
     * 获取全部预算分类列表（按排序序号升序排列）。
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
     * 根据名称查找分类实体。
     */
    fun findCategoryByName(name: String, db: SQLiteDatabase? = null): BudgetCategory? {
        val targetDb = db ?: database.readableDatabase
        val cursor = targetDb.query(
            GrainLedgerDatabase.TABLE_CATEGORIES,
            null,
            "${GrainLedgerDatabase.COL_CAT_NAME} = ?",
            arrayOf(name),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_ID))
                val catName = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_NAME))
                val icon = it.getString(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_ICON))
                val colorValue = it.getLong(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_COLOR))
                val sortOrder = it.getInt(it.getColumnIndexOrThrow(GrainLedgerDatabase.COL_CAT_SORT))
                return BudgetCategory(
                    categoryId = id,
                    categoryName = catName,
                    iconName = icon,
                    themeColorValue = colorValue,
                    sortOrder = sortOrder
                )
            }
        }
        return null
    }

    /**
     * 统计指定分类名称关联的预算细项数量。
     */
    fun countBudgetItemsByCategory(categoryName: String, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.readableDatabase
        val cursor = targetDb.rawQuery(
            "SELECT COUNT(*) FROM ${GrainLedgerDatabase.TABLE_BUDGET_ITEMS} WHERE ${GrainLedgerDatabase.COL_BUDGET_CATEGORY} = ?",
            arrayOf(categoryName)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0)
            }
        }
        return 0
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
     * 更新分类信息，并在重命名时级联更新关联的预算细项与记账流水的大类名称。
     */
    fun updateCategory(oldName: String, newCategory: BudgetCategory, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        val values = ContentValues().apply {
            put(GrainLedgerDatabase.COL_CAT_NAME, newCategory.categoryName)
            put(GrainLedgerDatabase.COL_CAT_ICON, newCategory.iconName)
            put(GrainLedgerDatabase.COL_CAT_COLOR, newCategory.themeColorValue)
            put(GrainLedgerDatabase.COL_CAT_SORT, newCategory.sortOrder)
        }

        val rowsAffected = targetDb.update(
            GrainLedgerDatabase.TABLE_CATEGORIES,
            values,
            "${GrainLedgerDatabase.COL_CAT_ID} = ?",
            arrayOf(newCategory.categoryId.toString())
        )

        // 若大类名称发生了改变，级联更新预算表与交易流水表中的 categoryName
        if (oldName != newCategory.categoryName && oldName.isNotBlank()) {
            val budgetValues = ContentValues().apply {
                put(GrainLedgerDatabase.COL_BUDGET_CATEGORY, newCategory.categoryName)
            }
            targetDb.update(
                GrainLedgerDatabase.TABLE_BUDGET_ITEMS,
                budgetValues,
                "${GrainLedgerDatabase.COL_BUDGET_CATEGORY} = ?",
                arrayOf(oldName)
            )

            val txValues = ContentValues().apply {
                put(GrainLedgerDatabase.COL_TRANS_CATEGORY, newCategory.categoryName)
            }
            targetDb.update(
                GrainLedgerDatabase.TABLE_TRANSACTIONS,
                txValues,
                "${GrainLedgerDatabase.COL_TRANS_CATEGORY} = ?",
                arrayOf(oldName)
            )
        }

        return rowsAffected
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
     * 删除指定分类。
     */
    fun deleteCategory(categoryId: Long, db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(
            GrainLedgerDatabase.TABLE_CATEGORIES,
            "${GrainLedgerDatabase.COL_CAT_ID} = ?",
            arrayOf(categoryId.toString())
        )
    }

    /**
     * 清空分类表。
     */
    fun clearAllCategories(db: SQLiteDatabase? = null): Int {
        val targetDb = db ?: database.writableDatabase
        return targetDb.delete(GrainLedgerDatabase.TABLE_CATEGORIES, null, null)
    }
}
