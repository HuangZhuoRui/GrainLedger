package com.vincent.grainledger.data.model

/**
 * 预算细项数据模型。
 *
 * 对应 Excel 表格中的《数据源》每一行预算规划数据。
 * 记录特定年月的某个具体开支细项（如“2026年8月 强制类 学费”、“日常吃”等），
 * 包含单价、数量、总预算额度、实际资金注入（实际加入）、出资人、实际消费总额及当前剩余额度。
 *
 * @property itemId 唯一编号
 * @property year 所属年份（如 2026）
 * @property month 所属月份（1-12）
 * @property categoryName 所属大类名称（如 "强制类"）
 * @property detailName 具体项目名称（如 "学费"、"日常吃"、"教材费"）
 * @property unitPrice 预算单价（如 30.0，若直接定额则为总价）
 * @property quantity 预算数量或天数（如 30.0 天，4.2 次，定额为 1.0）
 * @property totalPrice 预算规划总额（单价 × 数量 或 固定总额）
 * @property actualAllocated 实际注入/分配的资金额度（例如 8月 谈恋爱 预算60元，实际加入5.4元）
 * @property funder 出资来源（如 "自有资金"、"父母支持"、"奖学金"）
 * @property actualSpent 实际已核销/已支出的金额汇总
 * @property balance 剩余结余额度（actualAllocated - actualSpent）
 * @property remark 备注说明
 */
data class BudgetItem(
    val itemId: Long = 0L,
    val year: Int,
    val month: Int,
    val categoryName: String,
    val detailName: String,
    val unitPrice: Double,
    val quantity: Double = 1.0,
    val totalPrice: Double = unitPrice * quantity,
    val actualAllocated: Double = totalPrice,
    val funder: String = "默认账户",
    val actualSpent: Double = 0.0,
    val balance: Double = actualAllocated - actualSpent,
    val remark: String = ""
) {
    /**
     * 判断该预算项是否超支。
     */
    val isOverBudget: Boolean
        get() = actualSpent > actualAllocated && actualAllocated > 0.0

    /**
     * 获取预算使用百分比（0.0 至 1.0 以上）。
     */
    val usageProgress: Float
        get() {
            if (actualAllocated <= 0.0) return if (actualSpent > 0.0) 1.0f else 0.0f
            return (actualSpent / actualAllocated).toFloat().coerceIn(0.0f, 2.0f)
        }
}
