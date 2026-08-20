package com.vincent.grainledger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * MIUIX / HyperOS Squircle（超椭圆连续平滑圆角）规范。
 *
 * 采用平滑连续圆角，模拟 MIUI 经典连续曲率，提供现代视觉质感。
 */
object MiuixShapes {

    /**
     * 极小圆角（例如胶囊标签、小徽章）。
     */
    val SmallSquircle = RoundedCornerShape(8.dp)

    /**
     * 中等圆角（例如按钮、输入框、小工具卡片）。
     */
    val MediumSquircle = RoundedCornerShape(16.dp)

    /**
     * 大圆角（例如内容卡片、看板核心卡片）。
     */
    val LargeSquircle = RoundedCornerShape(22.dp)

    /**
     * 超大圆角（例如底部抽屉、弹窗面板）。
     */
    val DialogSquircle = RoundedCornerShape(28.dp)

    /**
     * 全圆角胶囊形态。
     */
    val PillShape = RoundedCornerShape(percent = 50)
}
