package com.vincent.grainledger.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.data.model.BudgetItem
import com.vincent.grainledger.data.updater.UpdateCheckState
import com.vincent.grainledger.ui.components.layout.AppPageScaffold
import com.vincent.grainledger.ui.screens.bookkeeping.BookkeepingDialog
import com.vincent.grainledger.ui.screens.budget.BudgetScreen
import com.vincent.grainledger.ui.screens.budget.EditBudgetItemDialog
import com.vincent.grainledger.ui.screens.dashboard.DashboardScreen
import com.vincent.grainledger.ui.screens.settings.SettingsScreen
import com.vincent.grainledger.ui.screens.transactions.TransactionTreeScreen
import com.vincent.grainledger.ui.screens.updater.UpdateCheckDialog
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixShapes
import com.vincent.grainledger.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 导航页签项数据模型。
 *
 * @property index 页签索引
 * @property title 页签标题
 * @property icon 页签图标
 */
sealed class NavigationTab(val index: Int, val title: String, val icon: ImageVector) {
    object Dashboard : NavigationTab(0, "看板", Icons.Default.Home)
    object Budget : NavigationTab(1, "预算", Icons.Default.DateRange)
    object Transactions : NavigationTab(2, "流水", Icons.AutoMirrored.Filled.List)
    object Settings : NavigationTab(3, "设置", Icons.Default.Settings)
}

/**
 * 应用主容器界面 (MainContainerScreen)。
 *
 * 组织四大核心页面（看板、预算、流水、设置）的切换展示，
 * 容纳全局记账弹窗与预算编辑弹窗、启动静默检查更新弹窗。
 *
 * @param viewModel 全局视图模型
 * @param onNavigateToUpdate 导航至独立检查更新页面回调
 */
@Composable
fun MainContainerScreen(
    viewModel: MainViewModel,
    onNavigateToUpdate: () -> Unit = {}
) {
    val context = LocalContext.current

    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val selectedTabIndex by viewModel.selectedTabIndex.collectAsState()
    var showBookkeepingDialog by remember { mutableStateOf(false) }
    var editingBudgetItem by remember { mutableStateOf<BudgetItem?>(null) }
    var showBudgetEditDialog by remember { mutableStateOf(false) }

    // 冷启动时静默检查更新，仅在发现新版本时触发弹窗提示
    LaunchedEffect(Unit) {
        viewModel.checkUpdateOnStartup(BuildConfig.VERSION_NAME)
    }

    // 拦截系统返回键关闭弹窗
    BackHandler(enabled = showBookkeepingDialog) {
        showBookkeepingDialog = false
    }
    BackHandler(enabled = showBudgetEditDialog) {
        showBudgetEditDialog = false
    }

    // 提示信息监听
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    AppPageScaffold(
        applyStatusBarPadding = true,
        applyNavigationBarPadding = false
    ) {
        // 页面切换容器（纯左右平移无渐变）
        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = MiuixAnimation.springSmooth()
                    ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = MiuixAnimation.springSmooth()
                        )
                    )
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = MiuixAnimation.springSmooth()
                    ).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = MiuixAnimation.springSmooth()
                        )
                    )
                }
            },
            label = "主页签切换",
            modifier = Modifier.fillMaxSize()
        ) { index ->
            when (index) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onOpenBookkeeping = { showBookkeepingDialog = true },
                    onBudgetItemClick = { item ->
                        editingBudgetItem = item
                        showBudgetEditDialog = true
                    }
                )
                1 -> BudgetScreen(
                    viewModel = viewModel
                )
                2 -> TransactionTreeScreen(
                    viewModel = viewModel
                )
                3 -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToUpdate = onNavigateToUpdate
                )
            }
        }

        // 底部 MIUI 悬浮胶囊导航栏
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 16.dp,
                        shape = MiuixShapes.DialogSquircle,
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .background(
                        color = MiuixTheme.colorScheme.surface,
                        shape = MiuixShapes.DialogSquircle
                    )
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabList = listOf(
                    NavigationTab.Dashboard,
                    NavigationTab.Budget,
                    NavigationTab.Transactions,
                    NavigationTab.Settings
                )

                tabList.forEach { tab ->
                    val isSelected = (selectedTabIndex == tab.index)
                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                viewModel.setSelectedTabIndex(tab.index)
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = tab.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MiuixBlue else MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            }
        }

        // 全局快速记账弹窗
        if (showBookkeepingDialog) {
            BookkeepingDialog(
                viewModel = viewModel,
                onDismissRequest = { showBookkeepingDialog = false }
            )
        }

        // 预算细项编辑弹窗
        if (showBudgetEditDialog) {
            EditBudgetItemDialog(
                targetItem = editingBudgetItem,
                year = currentYear,
                month = currentMonth,
                categoryList = allCategories,
                onSave = { viewModel.saveBudgetItem(it) },
                onDelete = { viewModel.deleteBudgetItem(it) },
                onDismissRequest = { showBudgetEditDialog = false }
            )
        }

        // 启动时或检测到新版本的全局更新弹窗
        if (updateCheckState is UpdateCheckState.HasUpdate) {
            UpdateCheckDialog(
                checkState = updateCheckState,
                downloadProgress = downloadProgress,
                onStartDownload = { downloadUrl, fileName ->
                    viewModel.startDownloadApk(context, downloadUrl, fileName)
                },
                onCancelDownload = {
                    viewModel.cancelDownload()
                },
                onRetry = {
                    viewModel.checkForUpdates(BuildConfig.VERSION_NAME)
                },
                onDismiss = {
                    viewModel.resetUpdateState()
                }
            )
        }
    }
}
