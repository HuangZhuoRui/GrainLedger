package com.vincent.grainledger.ui.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vincent.grainledger.ui.screens.MainContainerScreen
import com.vincent.grainledger.ui.screens.updater.UpdateScreen
import com.vincent.grainledger.ui.theme.GrainLedgerTheme
import com.vincent.grainledger.ui.theme.MiuixAnimation
import com.vincent.grainledger.ui.viewmodel.MainViewModel

/**
 * 应用全局根导航图 (AppNavHost)。
 *
 * 基于 Android Jetpack Navigation Compose 规范构建完整的返回栈管理与纯左右滑动的转场动效（无渐变）。
 *
 * @param viewModel 全局视图模型
 */
@Composable
fun AppNavHost(
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val systemInDarkTheme = isSystemInDarkTheme()
    val darkModePreference by viewModel.darkModePreference.collectAsState()
    val isFinalDarkTheme = darkModePreference ?: systemInDarkTheme

    GrainLedgerTheme(isDarkTheme = isFinalDarkTheme) {
        NavHost(
            navController = navController,
            startDestination = AppScreen.Main.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = MiuixAnimation.springSmooth()
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = MiuixAnimation.springSmooth()
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = MiuixAnimation.springSmooth()
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = MiuixAnimation.springSmooth()
                )
            }
        ) {
            // 主界面（底部导航：看板、预算、流水、设置）
            composable(AppScreen.Main.route) {
                MainContainerScreen(
                    viewModel = viewModel,
                    onNavigateToUpdate = {
                        navController.navigate(AppScreen.Update.route)
                    }
                )
            }

            // 独立检查更新与版本发布历史页面
            composable(AppScreen.Update.route) {
                UpdateScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
