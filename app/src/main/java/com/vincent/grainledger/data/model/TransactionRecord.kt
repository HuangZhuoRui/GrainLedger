package com.vincent.grainledger.data.model

/**
 * 交易流水记录数据模型。
 *
 * 对应 Excel 表格中的《每日账单》流水。
 * 记录单笔收支发生的时间、类别、对应预算详情、收支金额，
 * 以及发生该笔交易后实时核算的【具体剩余】与【类剩余】。
 *
 * @property recordId 唯一流水编号
 * @property year 发生年份（如 2026）
 * @property month 发生月份（1-12）
 * @property day 发生日（1-31）
 * @property categoryName 归属大类（如 "强制类"）
 * @property detailName 对应预算细项或消费详情（如 "教材费"、"学费"）
 * @property amount 交易金额（支出用负数表示，例如 -180.59；收入用正数表示）
 * @property itemRemaining 交易发生后，该具体预算细项的即时剩余额度
 * @property categoryRemaining 交易发生后，该分类的即时剩余总额
 * @property funder 支付出资人（如 "默认账户"）
 * @property remark 交易备注
 * @property timestamp 毫秒级时间戳
 */
data class TransactionRecord(
    val recordId: Long = 0L,
    val year: Int,
    val month: Int,
    val day: Int,
    val categoryName: String,
    val detailName: String,
    val amount: Double,
    val itemRemaining: Double = 0.0,
    val categoryRemaining: Double = 0.0,
    val funder: String = "默认账户",
    val remark: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 判断是否为支出（金额为负数或零）。
     */
    val isExpense: Boolean
        get() = amount < 0.0

    /**
     * 获取金额的绝对值用于界面显示。
     */
    val absoluteAmount: Double
        get() = kotlin.math.abs(amount)
}
