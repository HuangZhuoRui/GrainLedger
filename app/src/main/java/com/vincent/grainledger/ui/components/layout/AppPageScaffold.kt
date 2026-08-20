package com.vincent.grainledger.ui.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用页面脚手架与容器组件 (AppPageScaffold)。
 *
 * 统一管理应用的沉浸式状态栏/导航栏安全间距 (Edge-to-Edge Safe Insets)、
 * 标准化顶部导航栏（左上角返回按钮、主标题、副标题、右侧自定义操作插槽）、
 * 背景色渲染、底部导航栏/操作栏插槽与悬浮按钮 (FAB) 定位。
 *
 * @param modifier 外部修饰符
 * @param title 页面标题（可选）
 * @param subtitle 页面副说明文本（可选）
 * @param onBack 左上角返回按钮点击回调（若为 null 则不显示返回键）
 * @param topBar 自定义完全替换顶部栏插槽（若提供则覆盖默认 header）
 * @param topBarActions 顶部栏右侧操作组件插槽
 * @param applyStatusBarPadding 是否自动应用顶部状态栏安全内边距（默认为 true，确保内容不被挖孔或状态图标遮挡）
 * @param applyNavigationBarPadding 是否自动应用底部手势导航条安全内边距（默认为 false）
 * @param applyImePadding 是否随软键盘弹起自动添加底部内边距（默认为 false）
 * @param backgroundColor 页面背景色（默认遵循当前主题的 background 色）
 * @param floatingActionButton 页面悬浮按钮插槽（位于右下角）
 * @param bottomBar 页面底部栏插槽（位于底部中间）
 * @param content 页面主内容区域
 */
@Composable
fun AppPageScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    topBarActions: (@Composable RowScope.() -> Unit)? = null,
    applyStatusBarPadding: Boolean = true,
    applyNavigationBarPadding: Boolean = false,
    applyImePadding: Boolean = false,
    backgroundColor: Color = MiuixTheme.colorScheme.background,
    floatingActionButton: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .then(if (applyNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier)
            .then(if (applyImePadding) Modifier.imePadding() else Modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. 顶部栏渲染
            if (topBar != null) {
                topBar()
            } else if (title != null) {
                DefaultPageTopBar(
                    title = title,
                    subtitle = subtitle,
                    onBack = onBack,
                    actions = topBarActions
                )
            }

            // 2. 主体内容容器
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                content = content
            )

            // 3. 底部栏渲染（如果有）
            if (bottomBar != null) {
                bottomBar()
            }
        }

        // 4. 悬浮操作按钮 (FAB)
        if (floatingActionButton != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 20.dp)
            ) {
                floatingActionButton()
            }
        }
    }
}

/**
 * 通用默认顶部标题栏。
 *
 * 自动根据是否有 onBack 回调呈现二级返回模式或一级大标题模式。
 */
@Composable
private fun DefaultPageTopBar(
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (onBack != null) 12.dp else 20.dp,
                vertical = if (onBack != null) 8.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    fontSize = if (onBack != null) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }

        if (actions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    }
}
