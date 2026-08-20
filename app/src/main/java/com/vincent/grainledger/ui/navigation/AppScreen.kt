package com.vincent.grainledger.ui.navigation

/**
 * 应用全局导航路由定义。
 *
 * @property route 路由路径字符串
 */
sealed class AppScreen(val route: String) {
    /**
     * 主界面路由（包含看板、预算、流水、设置四大核心页签）。
     */
    object Main : AppScreen("main")

    /**
     * 独立检查更新与版本发布历史全屏页面路由。
     */
    object Update : AppScreen("update")
}
