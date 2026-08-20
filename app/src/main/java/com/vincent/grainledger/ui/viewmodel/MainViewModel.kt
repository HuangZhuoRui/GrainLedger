package com.vincent.grainledger.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vincent.grainledger.data.excel.ExcelHelper
import com.vincent.grainledger.data.model.BalanceCheckResult
import com.vincent.grainledger.data.model.BudgetCategory
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.model.MonthlyOverview
import com.vincent.grainledger.data.model.TransactionRecord
import com.vincent.grainledger.data.repository.LedgerRepository
import com.vincent.grainledger.data.updater.AppUpdaterService
import com.vincent.grainledger.data.updater.DownloadProgress
import com.vincent.grainledger.data.updater.DownloadStatus
import com.vincent.grainledger.data.updater.UpdateCheckState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * 余粮全局核心视图模型 (ViewModel)。
 *
 * 统一承载当前选中的年月、月度综合看板数据、预算细项列表、每日记账流水、资金池配平健康检查、
 * 检查更新与高速下载状态，并响应数据变动自动刷新全量 UI。
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LedgerRepository(application)
    private val updaterService = AppUpdaterService()

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

    // 提示信息流
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // 文件导入导出中状态
    private val _isProcessingFile = MutableStateFlow(false)
    val isProcessingFile: StateFlow<Boolean> = _isProcessingFile.asStateFlow()

    // 深色模式偏好：null 跟随系统，true 纯黑深色，false 纯白浅色
    private val _darkModePreference = MutableStateFlow<Boolean?>(null)
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference.asStateFlow()

    // 检查更新状态
    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

    // 实时下载进度
    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    init {
        // 监听底层数据库版本变动，自动刷新界面
        viewModelScope.launch {
            repository.dataVersionFlow.collectLatest {
                loadAllData()
            }
        }
    }

    /**
     * 加载当前选定月份的所有业务数据。
     */
    fun loadAllData() {
        viewModelScope.launch {
            val year = _currentYear.value
            val month = _currentMonth.value

            _allCategories.value = repository.getAllCategories()
            _availableMonths.value = repository.getAvailableMonths()
            _currentBudgetItems.value = repository.getBudgetItemsByMonth(year, month)
            _currentTransactions.value = repository.getTransactionsByMonth(year, month)
            _monthlyOverview.value = repository.getMonthlyOverview(year, month)
            _balanceCheckResult.value = repository.getBalanceCheck(year, month)
        }
    }

    /**
     * 切换选定的年份与月份。
     */
    fun selectMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        loadAllData()
    }

    /**
     * 新增记账流水。
     */
    fun addTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            repository.recordTransaction(transaction)
            _toastMessage.value = "记账成功！已实时更新细项与大类结余"
        }
    }

    /**
     * 新增记账流水（快捷参数重载）。
     */
    fun recordTransaction(
        year: Int,
        month: Int,
        day: Int,
        categoryName: String,
        detailName: String,
        amount: Double,
        funder: String,
        remark: String
    ) {
        val record = TransactionRecord(
            recordId = 0L,
            year = year,
            month = month,
            day = day,
            categoryName = categoryName,
            detailName = detailName,
            amount = amount,
            itemRemaining = 0.0,
            categoryRemaining = 0.0,
            funder = funder,
            remark = remark,
            timestamp = System.currentTimeMillis()
        )
        addTransaction(record)
    }

    /**
     * 删除指定交易流水。
     */
    fun deleteTransaction(recordId: Long) {
        viewModelScope.launch {
            val success = repository.deleteTransaction(recordId)
            if (success) {
                _toastMessage.value = "已删除该笔流水并还原预算结余"
            }
        }
    }

    /**
     * 保存或编辑预算细项。
     */
    fun saveBudgetItem(item: BudgetItem) {
        viewModelScope.launch {
            repository.saveBudgetItem(item)
            _toastMessage.value = "预算细项保存成功！"
        }
    }

    /**
     * 删除预算细项。
     */
    fun deleteBudgetItem(itemId: Long) {
        viewModelScope.launch {
            val success = repository.deleteBudgetItem(itemId)
            if (success) {
                _toastMessage.value = "预算细项已删除"
            }
        }
    }

    /**
     * 从 Excel 流中导入数据。
     */
    fun importExcelData(inputStream: InputStream) {
        viewModelScope.launch {
            _isProcessingFile.value = true
            try {
                val result = ExcelHelper.importFromExcelStream(inputStream, repository)
                if (result.isSuccess) {
                    _toastMessage.value = "Excel 导入成功！共导入 ${result.importedBudgetCount} 条预算，${result.importedTransactionCount} 条流水"
                    loadAllData()
                } else {
                    _toastMessage.value = "导入失败: ${result.message}"
                }
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
     * 触发检查应用更新。
     *
     * @param currentVersion 当前应用版本号
     */
    fun checkForUpdates(currentVersion: String) {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            val resultState = updaterService.checkForUpdate(currentVersion)
            _updateCheckState.value = resultState
        }
    }

    /**
     * 开始下载新版本 APK。
     *
     * @param context Android 上下文
     * @param downloadUrl 加速下载链接
     * @param fileName 安装包文件名
     */
    fun startDownloadApk(context: Context, downloadUrl: String, fileName: String) {
        viewModelScope.launch {
            val cacheDirectory = context.externalCacheDir ?: context.cacheDir
            val destinationApkFile = File(cacheDirectory, fileName)

            val downloadSuccess = updaterService.downloadApkFile(
                downloadUrl = downloadUrl,
                destinationFile = destinationApkFile,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )

            if (downloadSuccess && destinationApkFile.exists()) {
                updaterService.installApk(context, destinationApkFile)
            }
        }
    }

    /**
     * 取消进行中的下载。
     */
    fun cancelDownload() {
        updaterService.cancelDownload()
        _downloadProgress.value = DownloadProgress(status = DownloadStatus.CANCELED)
    }

    /**
     * 重置检查更新状态。
     */
    fun resetUpdateState() {
        _updateCheckState.value = UpdateCheckState.Idle
        _downloadProgress.value = DownloadProgress()
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
