package com.vincent.grainledger.ui.screens.settings.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.control.MiuixSwitchRow
import com.vincent.grainledger.ui.theme.MiuixPurple

/**
 * 主题外观设置卡片 (ThemeSettingCard)。
 *
 * 提供深色模式开关与跟随系统偏好配置。
 *
 * @param darkModePreference 用户偏好设置（null 为跟随系统，true 为深色，false 为浅色）
 * @param onPreferenceChange 偏好变更回调
 * @param modifier 外部修饰符
 */
@Composable
fun ThemeSettingCard(
    darkModePreference: Boolean?,
    onPreferenceChange: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDark = darkModePreference ?: systemInDarkTheme

    MiuixSectionCard(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        MiuixSwitchRow(
            title = "深色模式",
            subtitle = if (darkModePreference == null) "当前跟随系统设置" else if (isDark) "已启用纯黑深色外观" else "已启用纯白浅色外观",
            checked = isDark,
            onCheckedChange = { checked ->
                onPreferenceChange(checked)
            },
            icon = Icons.Default.DarkMode,
            iconTint = MiuixPurple
        )
    }
}
