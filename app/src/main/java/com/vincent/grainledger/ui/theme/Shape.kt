package com.vincent.grainledger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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

/**
 * 左右边缘渐变羽化遮罩修饰符 (Horizontal Fading Edge)。
 *
 * 为横向滑动列表/行（如分类滑轨、标签滑轨、日期轴、账户胶囊等）两端提供平滑淡出的模糊过渡，
 * 消除选项在容器边缘被生硬直接截断的割裂感。
 *
 * @param fadeWidth 左右边缘羽化遮罩宽度，默认 16.dp
 */
fun Modifier.horizontalFadingEdge(
    fadeWidth: Dp = 16.dp
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val fadeWidthPx = fadeWidth.toPx()
        if (fadeWidthPx > 0f && size.width > fadeWidthPx * 2) {
            // 左侧边缘平滑渐变淡出
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadeWidthPx
                ),
                blendMode = BlendMode.DstIn
            )
            // 右侧边缘平滑渐变淡出
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadeWidthPx,
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * 垂直顶部/底部边缘平滑渐变羽化遮罩修饰符 (Vertical Fading Edge)。
 *
 * 为纵向滑动列表（如检查更新页面、设置二级页等）顶部或底部提供平滑淡出的模糊过渡，
 * 消除卡片在向上滑入标题栏/顶部Tab时直接被生硬截断的割裂感。
 *
 * @param topFadeHeight 顶部边缘羽化遮罩高度，默认 16.dp
 * @param bottomFadeHeight 底部边缘羽化遮罩高度，默认 0.dp
 */
fun Modifier.verticalFadingEdge(
    topFadeHeight: Dp = 16.dp,
    bottomFadeHeight: Dp = 0.dp
): Modifier = this
    .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val topFadePx = topFadeHeight.toPx()
        val bottomFadePx = bottomFadeHeight.toPx()
        if (topFadePx > 0f && size.height > topFadePx) {
            // 顶部平滑渐变淡出
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstIn
            )
        }
        if (bottomFadePx > 0f && size.height > bottomFadePx) {
            // 底部平滑渐变淡出
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/**
 * 顶部边缘平滑渐变羽化遮罩修饰符。
 */
fun Modifier.topFadingEdge(
    fadeHeight: Dp = 18.dp
): Modifier = verticalFadingEdge(topFadeHeight = fadeHeight, bottomFadeHeight = 0.dp)


