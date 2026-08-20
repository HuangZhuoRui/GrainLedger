package com.vincent.grainledger.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 谷记账 (GrainLedger) 调色盘规范。
 *
 * 遵循用户要求的【以白色为主】的纯净明亮风格，结合 MIUI / HyperOS 的清透卡片质感，
 * 并全链路无缝适配深色模式（Dark Mode AMOLED 纯黑）。
 */

// 纯白明亮主题配色 (Light Theme - White Dominant)
val WhiteBackground = Color(0xFFF7F8FA)        // 浅灰白沉浸底色
val PureWhiteCard = Color(0xFFFFFFFF)          // 纯白高光卡片背景
val CardElevated = Color(0xFFFFFFFF)           // 抬升卡片背景
val PrimaryBlackText = Color(0xFF111111)       // 主要文字纯黑
val SecondaryGrayText = Color(0xFF757575)      // 次要说明文字灰
val TertiaryLightGrayText = Color(0xFFA0A0A0)  // 辅助提示文字
val BorderSubtleLight = Color(0xFFE5E7EB)      // 极细边界描边
val SurfaceVariantLight = Color(0xFFF0F2F5)    // 浅灰填充组件底色

// MIUIX 经典品牌点缀色
val MiuixBlue = Color(0xFF007AFF)              // MIUI 科技蓝（主行动色）
val MiuixGreen = Color(0xFF34C759)             // 收入与充盈结余绿
val MiuixRed = Color(0xFFFF3B30)               // 支出与超支警告红
val MiuixOrange = Color(0xFFFF9500)            // 预算预警橙
val MiuixPurple = Color(0xFF5856D6)            // 生活生活紫
val MiuixPink = Color(0xFFFF2D55)              // 恋爱粉

// 深色模式配色 (Dark Theme - AMOLED Pure Black)
val DarkBackground = Color(0xFF000000)         // 纯黑底色
val DarkCardBackground = Color(0xFF1C1C1E)     // 沉浸深灰卡片
val DarkElevatedCard = Color(0xFF2C2C2E)       // 抬升深灰卡片
val DarkPrimaryText = Color(0xFFF5F5F7)        // 深色主文本白
val DarkSecondaryText = Color(0xFF8E8E93)      // 深色次文本灰
val DarkBorderSubtle = Color(0xFF2C2C2E)       // 深色微细边框
val DarkSurfaceVariant = Color(0xFF242426)     // 深色组件底色