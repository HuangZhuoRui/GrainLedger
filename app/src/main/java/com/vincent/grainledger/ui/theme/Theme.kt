package com.vincent.grainledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 余粮全局主题包装器。
 *
 * 接入 MIUIX 规范主题 `MiuixTheme`，自动适配系统深浅色模式切换，
 * 保证纯白通透质感与暗夜纯黑护眼模式。
 *
 * @param isDarkTheme 是否启用深色模式，默认跟随系统
 * @param content 子组件内容
 */
@Composable
fun GrainLedgerTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MiuixTheme(
        content = content
    )
}