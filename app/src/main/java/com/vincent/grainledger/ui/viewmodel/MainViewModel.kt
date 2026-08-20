package com.vincent.grainledger.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vincent.grainledger.data.excel.ExcelHelper
import com.vincent.grainledger.data.model.BalanceCheckResult
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.MonthlyOverview
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

/**
 * 余粮全局核心视图模型 (ViewModel)。
 *
 * 统一承载当前选中的年月、月度综合看板数据、预算细项列表、每日记账流水、资金池配平健康检查，
 * 并响应数据变动自动刷新全量 UI。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LedgerRepository(application)

    // 当前选中的年份与月份（默认为 2026年 8月）
    private val _currentYear = MutableStateFlow(2026)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(8)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 所有可用月份列表（如 [(2026, 8), (2026, 9), (2026, 10), (2026, 11), (2026, 12)]）
    private val _availableMonths = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val availableMonths: StateFlow<List<Pair<Int, Int>>> = _availableMonths.asStateFlow()

    // 月度综合汇总数据（对应综合看板）
    private val _monthlyOverview = MutableStateFlow(MonthlyOverview(2026, 8))
    val monthlyOverview: StateFlow<MonthlyOverview> = _monthlyOverview.asStateFlow()

    // 资金池配平状态（对应草稿页）
    private val _balanceCheckResult = MutableStateFlow(BalanceCheckResult(10000.0, 0.0, 10000.0))
    val balanceCheckResult: StateFlow<BalanceCheckResult> = _balanceCheckResult.asStateFlow()

    // 当前月份下的所有预算细项
    private val _currentBudgetItems = MutableStateFlow<List<BudgetItem>>(emptyList())
    val currentBudgetItems: StateFlow<List<BudgetItem>> = _currentBudgetItems.asStateFlow()

    // 当前月份下的记账流水记录
    private val _currentTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val currentTransactions: StateFlow<List<TransactionRecord>> = _currentTransactions.asStateFlow()

    // 全部分类列表
    private val _allCategories = MutableStateFlow<List<BudgetCategory>>(emptyList())
    val allCategories: StateFlow<List<BudgetCategory>> = _allCategories.asStateFlow()

    // 提示信息通知流
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // 主题深色模式设置（null 代表跟随系统，true 代表强制深色，false 代表强制浅色纯白）
    private val _darkModePreference = MutableStateFlow<Boolean?>(null)
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference.asStateFlow()

    // 正在进行 Excel 处理状态
    private val _isProcessingFile = MutableStateFlow(false)
    val isProcessingFile: StateFlow<Boolean> = _isProcessingFile.asStateFlow()

    init {
        // 监听数据仓库版本变化，自动重新加载数据
        viewModelScope.launch {
            repository.dataVersionFlow.collectLatest {
                refreshAllData()
            }
        }
        refreshAllData()
    }

    /**
     * 切换当前查看与记账的月份。
     */
    fun selectMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        refreshAllData()
    }

    /**
     * 刷新当前所选年月的全部聚合与列表数据。
     */
    fun refreshAllData() {
        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            _availableMonths.value = repository.getAvailableMonths()
            _allCategories.value = repository.getAllCategories()
            _currentBudgetItems.value = repository.getBudgetItemsByMonth(year, month)
            _currentTransactions.value = repository.getTransactionsByMonth(year, month)
            _monthlyOverview.value = repository.getMonthlyOverview(year, month)
            _balanceCheckResult.value = repository.getBalanceCheck(year, month, 10000.0)
        }
    }

    /**
     * 执行一笔快速记账。
     *
     * @param year 发生年份
     * @param month 发生月份
     * @param day 发生日期
     * @param categoryName 归属大类
     * @param detailName 对应预算细项
     * @param amount 交易金额（支出为负数，如 -180.59）
     * @param funder 支付出资人
     * @param remark 交易备注
     */
    fun recordTransaction(
        year: Int,
        month: Int,
        day: Int,
        categoryName: String,
        detailName: String,
        amount: Double,
        funder: String = "默认账户",
        remark: String = ""
    ) {
        viewModelScope.launch {
            val newRecord = TransactionRecord(
                recordId = 0L,
                year = year,
                month = month,
                day = day,
                categoryName = categoryName,
                detailName = detailName,
                amount = amount,
                funder = funder,
                remark = remark
            )
            repository.recordTransaction(newRecord)
            _toastMessage.value = "记账成功！已扣减相应预算额度"
        }
    }

    /**
     * 删除指定的一笔交易流水记录。
     */
    fun deleteTransaction(recordId: Long) {
        viewModelScope.launch {
            val success = repository.deleteTransaction(recordId)
            if (success) {
                _toastMessage.value = "已删除该条账单记录并恢复对应预算额度"
            }
        }
    }

    /**
     * 保存或更新预算项。
     */
    fun saveBudgetItem(budgetItem: BudgetItem) {
        viewModelScope.launch {
            repository.saveBudgetItem(budgetItem)
            _toastMessage.value = "预算项目「${budgetItem.detailName}」保存成功"
        }
    }

    /**
     * 删除指定的预算项。
     */
    fun deleteBudgetItem(itemId: Long) {
        viewModelScope.launch {
            val success = repository.deleteBudgetItem(itemId)
            if (success) {
                _toastMessage.value = "已成功删除该预算项目"
            }
        }
    }

    /**
     * 从 Excel 输入流导入数据。
     */
    fun importExcelData(inputStream: InputStream) {
        viewModelScope.launch {
            _isProcessingFile.value = true
            try {
                val result = ExcelHelper.importFromExcelStream(inputStream, repository)
                _toastMessage.value = result.message
            } catch (exception: Exception) {
                _toastMessage.value = "导入出错: ${exception.localizedMessage}"
            } finally {
                _isProcessingFile.value = false
            }
        }
    }

    /**
     * 将数据导出为 Excel 流。
     */
    fun exportExcelData(outputStream: OutputStream) {
        viewModelScope.launch {
            _isProcessingFile.value = true
            try {
                ExcelHelper.exportToExcelStream(outputStream, repository)
                _toastMessage.value = "账单已成功导出为标准 Excel 文件！"
            } catch (exception: Exception) {
                _toastMessage.value = "导出出错: ${exception.localizedMessage}"
            } finally {
                _isProcessingFile.value = false
            }
        }
    }

    /**
     * 重置所有账单数据为原始初始预置数据。
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetDatabaseToDefaults()
            _toastMessage.value = "已重置为初始账单数据"
        }
    }

    /**
     * 清除当前提示消息。
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * 切换深色模式偏好。
     */
    fun setDarkModePreference(preference: Boolean?) {
        _darkModePreference.value = preference
    }
}
