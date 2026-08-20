package com.vincent.grainledger.data.model

/**
 * 月度综合统计汇总数据模型。
 *
 * 对应 Excel 表格中的《综合查看》及看板核心指标：
 * 包含当月总规划预算（总价￥）、资金池总量（实际加入￥ + 收入入账 + 上月结余滚存）、总实际消费、总结余、
 * 收入类总额、上月结余滚存，以及各大类（支出信封与收入来源）的汇总数据。
 *
 * @property year 目标年份
 * @property month 目标月份 (1-12)
 * @property totalPlannedBudget 该月所有支出预算项总价之和
 * @property totalActualAllocated 该月总资金池总量（基础预算注入 + 实际入账 + 上月滚存）
 * @property totalActualSpent 该月所有支出流水之和
 * @property totalBalance 资金池可用总结余（totalActualAllocated - totalActualSpent）
 * @property totalIncome 当月实际入账总和
 * @property rolloverFromPreviousMonth 上月剩余资金滚存结转额度
 * @property categoryOverviewList 各支出大类的聚合统计列表
 * @property incomeOverviewList 各收入大类的入账聚合统计列表
 */
data class MonthlyOverview(
    val year: Int,
    val month: Int,
    val totalPlannedBudget: Double = 0.0,
    val totalActualAllocated: Double = 0.0,
    val totalActualSpent: Double = 0.0,
    val totalBalance: Double = totalActualAllocated - totalActualSpent,
    val totalIncome: Double = 0.0,
    val rolloverFromPreviousMonth: Double = 0.0,
    val categoryOverviewList: List<CategoryOverview> = emptyList(),
    val incomeOverviewList: List<IncomeCategoryOverview> = emptyList()
)

/**
 * 单个支出分类在特定月份下的聚合统计模型。
 *
 * @property categoryName 分类名称（例如 "强制类"）
 * @property categoryTotalBudget 该分类下的预算总额
 * @property categoryActualAllocated 该分类实际注入资金
 * @property categoryActualSpent 该分类实际已消费金额
 * @property categoryBalance 该分类剩余金额
 * @property isIncome 是否为收入类
 * @property budgetItemList 该分类包含的具体预算细项列表
 */
data class CategoryOverview(
    val categoryName: String,
    val categoryTotalBudget: Double = 0.0,
    val categoryActualAllocated: Double = 0.0,
    val categoryActualSpent: Double = 0.0,
    val categoryBalance: Double = categoryActualAllocated - categoryActualSpent,
    val isIncome: Boolean = false,
    val budgetItemList: List<BudgetItem> = emptyList()
) {
    /**
     * 获取分类消费占比（占分类实际加入的百分比）。
     */
    val usageRatio: Float
        get() {
            if (categoryActualAllocated <= 0.0) return if (categoryActualSpent > 0.0) 1.0f else 0.0f
            return (categoryActualSpent / categoryActualAllocated).toFloat().coerceIn(0.0f, 2.0f)
        }
}

/**
 * 单个收入分类在特定月份下的聚合统计模型。
 *
 * @property categoryName 收入大类名称（例如 "工资薪金"）
 * @property totalIncome 该分类当月累计入账总额
 * @property transactionCount 该分类当月入账笔数
 * @property transactionList 该分类当月的具体入账记录列表
 */
data class IncomeCategoryOverview(
    val categoryName: String,
    val totalIncome: Double = 0.0,
    val transactionCount: Int = 0,
    val transactionList: List<TransactionRecord> = emptyList()
)

/**
 * 资金池配平健康状态检查结果模型。
 *
 * 对应 Excel 表格中的《草稿页》配平公式：`总启动金 - SUM(实际加入) = 0`。
 * 用于检测分配的资金是否超额或尚有未分配额度。
 *
 * @property targetBenchmarkFund 例如设定的启动资金（如 10000.00）
 * @property allocatedTotalFund 已在预算项中实际加入的金额之和
 * @property balanceDifference targetBenchmarkFund - allocatedTotalFund（等于 0 代表完美配平）
 */
data class BalanceCheckResult(
    val targetBenchmarkFund: Double = 10000.0,
    val allocatedTotalFund: Double = 0.0,
    val balanceDifference: Double = targetBenchmarkFund - allocatedTotalFund
) {
    /**
     * 是否正好完全配平（差额在 0.01 元以内）。
     */
    val isBalanced: Boolean
        get() = kotlin.math.abs(balanceDifference) < 0.01

    /**
     * 是否超额分配。
     */
    val isOverAllocated: Boolean
        get() = balanceDifference < -0.01

    /**
     * 是否还有剩余未分配资金。
     */
    val hasUnallocatedFund: Boolean
        get() = balanceDifference > 0.01
}
