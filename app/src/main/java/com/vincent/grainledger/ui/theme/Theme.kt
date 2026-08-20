package com.vincent.grainledger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 余粮全局主题包装器。
 *
 * 接入 MIUIX 规范主题 `MiuixTheme`，自动跟随系统的亮色和暗色进行实时无缝切换。
 * 亮色模式下以纯白高通透为主色调，暗色模式下以 AMOLED 沉浸黑为主色调，
 * 并全自动协同系统状态栏与手势导航栏的沉浸式图标颜色自适应。
 *
 * @param isDarkTheme 是否启用深色模式，默认跟随系统
 * @param content 子组件内容
 */
@Composable
fun GrainLedgerTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

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