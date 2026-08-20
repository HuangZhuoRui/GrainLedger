package com.vincent.grainledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 余粮全局主题包装器。
 *
 * 接入 MIUIX 规范主题 `MiuixTheme`，自动跟随系统的亮色和暗色进行实时无缝切换。
 * 亮色模式下以纯白高通透为主色调，暗色模式下以 AMOLED 沉浸黑为主色调。
 *
 * @param isDarkTheme 是否启用深色模式，默认跟随系统
 * @param content 子组件内容
 */
@Composable
fun GrainLedgerTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val currentColors = if (isDarkTheme) {
        darkColorScheme(
            primary = MiuixBlue,
            background = DarkBackground,
            surface = DarkCardBackground,
            surfaceVariant = DarkSurfaceVariant,
            onSurface = DarkPrimaryText,
            onSurfaceSecondary = DarkSecondaryText
        )
    } else {
        lightColorScheme(
            primary = MiuixBlue,
            background = WhiteBackground,
            surface = PureWhiteCard,
            surfaceVariant = SurfaceVariantLight,
            onSurface = PrimaryBlackText,
            onSurfaceSecondary = SecondaryGrayText
        )
    }

    MiuixTheme(
        colors = currentColors,
        content = content
    )
}