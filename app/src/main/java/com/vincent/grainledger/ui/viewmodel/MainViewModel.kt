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
import com.vincent.grainledger.data.updater.GitHubRelease
import com.vincent.grainledger.data.updater.UpdateCheckState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    // 异步加载任务句柄（防并发重复请求与过时结果回写）
    private var loadDataJob: Job? = null

    // 当前选中的底部导航页签索引（0: 看板, 1: 预算, 2: 流水, 3: 设置）
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun setSelectedTabIndex(index: Int) {
        _selectedTabIndex.value = index
    }

    // 当前选中的年份与月份（默认为 2026年 8月）
    private val _currentYear = MutableStateFlow(2026)
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(8)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    // 所有可用月份列表（如 [(2026, 8), (2026, 9), (2026, 10), (2026, 11), (2026, 12)]）
    private val _availableMonths = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val availableMonths: StateFlow<List<Pair<Int, Int>>> = _availableMonths.asStateFlow()

    // 全月份数据预载映射（用于 HorizontalPager 滑月时 0 延迟直接秒加载，绝不等待）
    private val _monthlyOverviewMap = MutableStateFlow<Map<Pair<Int, Int>, MonthlyOverview>>(emptyMap())
    val monthlyOverviewMap: StateFlow<Map<Pair<Int, Int>, MonthlyOverview>> = _monthlyOverviewMap.asStateFlow()

    private val _budgetItemsMap = MutableStateFlow<Map<Pair<Int, Int>, List<BudgetItem>>>(emptyMap())
    val budgetItemsMap: StateFlow<Map<Pair<Int, Int>, List<BudgetItem>>> = _budgetItemsMap.asStateFlow()

    private val _transactionsMap = MutableStateFlow<Map<Pair<Int, Int>, List<TransactionRecord>>>(emptyMap())
    val transactionsMap: StateFlow<Map<Pair<Int, Int>, List<TransactionRecord>>> = _transactionsMap.asStateFlow()

    private val _balanceCheckMap = MutableStateFlow<Map<Pair<Int, Int>, BalanceCheckResult>>(emptyMap())
    val balanceCheckMap: StateFlow<Map<Pair<Int, Int>, BalanceCheckResult>> = _balanceCheckMap.asStateFlow()

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
     * 加载全量月份的所有业务数据到内存预载映射中（0 延迟直接就绪）。
     */
    fun loadAllData() {
        val targetYear = _currentYear.value
        val targetMonth = _currentMonth.value

        loadDataJob?.cancel()
        loadDataJob = viewModelScope.launch(Dispatchers.IO) {
            val categories = repository.getAllCategories()
            val months = repository.getAvailableMonths()

            // 预先批量将所有可用月份的数据加载进映射中
            val overviews = mutableMapOf<Pair<Int, Int>, MonthlyOverview>()
            val budgets = mutableMapOf<Pair<Int, Int>, List<BudgetItem>>()
            val txs = mutableMapOf<Pair<Int, Int>, List<TransactionRecord>>()
            val balanceChecks = mutableMapOf<Pair<Int, Int>, BalanceCheckResult>()

            for (m in months) {
                val (y, mo) = m
                overviews[m] = repository.getMonthlyOverview(y, mo)
                budgets[m] = repository.getBudgetItemsByMonth(y, mo)
                txs[m] = repository.getTransactionsByMonth(y, mo)
                balanceChecks[m] = repository.getBalanceCheck(y, mo)
            }

            _allCategories.value = categories
            _availableMonths.value = months
            _monthlyOverviewMap.value = overviews
            _budgetItemsMap.value = budgets
            _transactionsMap.value = txs
            _balanceCheckMap.value = balanceChecks

            val targetKey = Pair(targetYear, targetMonth)
            _currentBudgetItems.value = budgets[targetKey] ?: emptyList()
            _currentTransactions.value = txs[targetKey] ?: emptyList()
            _monthlyOverview.value = overviews[targetKey] ?: MonthlyOverview(targetYear, targetMonth)
            _balanceCheckResult.value = balanceChecks[targetKey] ?: BalanceCheckResult()
        }
    }

    /**
     * 切换选定的年份与月份（0 延迟秒切，直接从预载映射中极速读取）。
     */
    fun selectMonth(year: Int, month: Int) {
        _currentYear.value = year
        _currentMonth.value = month
        val targetKey = Pair(year, month)
        _monthlyOverviewMap.value[targetKey]?.let { _monthlyOverview.value = it }
        _budgetItemsMap.value[targetKey]?.let { _currentBudgetItems.value = it }
        _transactionsMap.value[targetKey]?.let { _currentTransactions.value = it }
        _balanceCheckMap.value[targetKey]?.let { _balanceCheckResult.value = it }
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
     * 删除指定交易流水（对象重载）。
     */
    fun deleteTransaction(record: TransactionRecord) {
        deleteTransaction(record.recordId)
    }

    /**
     * 重置数据库为默认初始测试数据。
     */
    fun resetToDefault() {
        viewModelScope.launch {
            repository.resetDatabaseToDefaults()
            _toastMessage.value = "已成功重置为初始默认账目数据！"
            loadAllData()
        }
    }

    /**
     * 清空数据库中所有的预算细项与记账流水记录（空白账本）。
     */
    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastMessage.value = "已成功清空所有数据！"
            val calendar = java.util.Calendar.getInstance()
            _currentYear.value = calendar.get(java.util.Calendar.YEAR)
            _currentMonth.value = calendar.get(java.util.Calendar.MONTH) + 1
            loadAllData()
        }
    }

    /**
     * 细分清空指定月份与分类的交易流水记录，并恢复受影响预算项的结余。
     */
    fun clearTransactionsFiltered(targetMonths: Set<Pair<Int, Int>>?, targetCategories: Set<String>?) {
        viewModelScope.launch {
            val count = repository.clearTransactionsFiltered(targetMonths, targetCategories)
            _toastMessage.value = if (count > 0) "已成功清理 $count 笔交易流水，预算结余已重新校准！" else "未找到符合条件的流水记录"
            loadAllData()
        }
    }

    /**
     * 细分清空指定月份与分类的预算规划细项。
     */
    fun clearBudgetsFiltered(targetMonths: Set<Pair<Int, Int>>?, targetCategories: Set<String>?) {
        viewModelScope.launch {
            val count = repository.clearBudgetsFiltered(targetMonths, targetCategories)
            _toastMessage.value = if (count > 0) "已成功清理 $count 项预算细项！" else "未找到符合条件的预算项"
            loadAllData()
        }
    }

    /**
     * 仅清空所有交易流水记录，并恢复所有预算项未消费结余。
     */
    fun clearAllTransactions() {
        clearTransactionsFiltered(null, null)
    }

    /**
     * 仅清空所有月份的预算规划细项。
     */
    fun clearAllBudgets() {
        clearBudgetsFiltered(null, null)
    }

    /**
     * 新建月份账本，并支持一键从当前月份复制预算结构。
     */
    fun createMonth(targetYear: Int, targetMonth: Int, copyFromCurrent: Boolean) {
        viewModelScope.launch {
            val sourceYear = _currentYear.value
            val sourceMonth = _currentMonth.value
            val success = repository.createMonth(
                targetYear = targetYear,
                targetMonth = targetMonth,
                sourceYear = sourceYear,
                sourceMonth = sourceMonth,
                copyBudget = copyFromCurrent
            )
            if (success) {
                _currentYear.value = targetYear
                _currentMonth.value = targetMonth
                _toastMessage.value = "已成功创建 ${targetYear}年${targetMonth}月 账本！"
                loadAllData()
            } else {
                _toastMessage.value = "${targetYear}年${targetMonth}月 账本已存在，无法重复添加！"
            }
        }
    }

    /**
     * 保存或更新分类。
     */
    fun saveCategory(category: BudgetCategory, oldName: String = "") {
        viewModelScope.launch {
            repository.saveCategory(category, oldName)
            _toastMessage.value = if (category.categoryId > 0L) "分类已更新" else "分类创建成功"
            loadAllData()
        }
    }

    /**
     * 删除指定分类。
     */
    fun deleteCategory(category: BudgetCategory, deleteAssociatedItems: Boolean = false) {
        viewModelScope.launch {
            repository.deleteCategory(category, deleteAssociatedItems)
            _toastMessage.value = "分类已删除"
            loadAllData()
        }
    }

    /**
     * 获取指定分类关联的预算细项数量。
     */
    suspend fun getCategoryUsageCount(categoryName: String): Int {
        return repository.getCategoryUsageCount(categoryName)
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

    // 历史版本列表流
    private val _releaseHistoryList = MutableStateFlow<List<GitHubRelease>>(emptyList())
    val releaseHistoryList: StateFlow<List<GitHubRelease>> = _releaseHistoryList.asStateFlow()

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    /**
     * 应用冷启动时静默检查更新（若发现新版本则自动展示弹窗，无更新则保持静默不打扰用户）。
     *
     * @param currentVersion 当前版本（默认为 1.0.0）
     */
    fun checkUpdateOnStartup(currentVersion: String = "1.0.0") {
        viewModelScope.launch {
            val hasUpdateState = updaterService.checkUpdateSilently(currentVersion)
            if (hasUpdateState != null) {
                _updateCheckState.value = hasUpdateState
            }
        }
    }

    /**
     * 拉取全量历史版本发布列表。
     */
    fun fetchReleaseHistory() {
        viewModelScope.launch {
            _isLoadingHistory.value = true
            try {
                val list = updaterService.fetchAllReleases()
                _releaseHistoryList.value = list
            } catch (_: Exception) {
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }

    /**
     * 触发检查应用更新。
     *
     * @param currentVersion 当前应用版本号（默认为 1.0.0）
     */
    fun checkForUpdates(currentVersion: String = "1.0.0") {
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            val resultState = updaterService.checkForUpdate(currentVersion)
            _updateCheckState.value = resultState
        }
    }

    // 当前正在下载的版本标签
    private val _downloadingTagName = MutableStateFlow<String?>(null)
    val downloadingTagName: StateFlow<String?> = _downloadingTagName.asStateFlow()

    /**
     * 开始下载 APK。
     *
     * @param context Android 上下文
     * @param downloadUrl 下载链接（加速或官方链接）
     * @param fileName 安装包文件名
     * @param tagName 当前版本标签（用于 UI 实时匹配下载进度）
     */
    fun startDownloadApk(context: Context, downloadUrl: String, fileName: String, tagName: String? = null) {
        viewModelScope.launch {
            _downloadingTagName.value = tagName
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
            if (!downloadSuccess) {
                _downloadingTagName.value = null
            }
        }
    }

    /**
     * 取消进行中的下载。
     */
    fun cancelDownload() {
        updaterService.cancelDownload()
        _downloadingTagName.value = null
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
