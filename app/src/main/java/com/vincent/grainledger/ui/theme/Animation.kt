package com.vincent.grainledger.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * MIUIX / HyperOS 物理非线性动画曲线规范。
 *
 * 封装小米澎湃 OS 风格的高帧率物理阻尼弹簧、非线性贝塞尔曲线（Fast-out Slow-in），
 * 以及组件按压缩放与层级展开的平滑动效规格。
 */
object MiuixAnimation {

    /**
     * MIUI 经典非线性减速缓动曲线（快速进入，平缓停驻）。
     */
    val MiuixDecelerateEasing: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /**
     * MIUI 标准流体动效曲线（流畅非线性）。
     */
    val MiuixFluidEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    /**
     * MIUI 强调回弹曲线（轻度超调回弹）。
     */
    val MiuixOvershootEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

    /**
     * 高刚度低阻尼弹性规格，适用于按压反馈、按钮点击与弹窗弹出。
     */
    fun <T> springBouncy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * 平滑无振荡阻尼规格，适用于卡片折叠展开与页面位移。
     */
    fun <T> springSmooth(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * 超轻响应弹簧规格，用于数值滚动与进度条实时响应。
     */
    fun <T> springFast(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
}
