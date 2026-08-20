package com.vincent.grainledger.ui.screens.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.BuildConfig
import com.vincent.grainledger.R
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于软件与开发者信息卡片 (AppInfoCard)。
 *
 * 展示应用名称、版本号、包名及设计理念。
 *
 * @param modifier 外部修饰符
 */
@Composable
fun AppInfoCard(
    modifier: Modifier = Modifier
) {
    val currentAppVersion = BuildConfig.VERSION_NAME

    MiuixSectionCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 软件官方精致 Logo（使用 rasterized drawable 避免 Compose 加载 adaptive-icon 崩溃）
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "余粮 Logo",
                modifier = Modifier
                    .size(60.dp)
                    .clip(MiuixShapes.MediumSquircle)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "余粮 GrainLedger",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(text = "版本 v$currentAppVersion", color = MiuixBlue)
                StatusBadge(text = "正式发行版", color = MiuixGreen)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "基于信封预算法则与复式双剩余模型构建的现代化个人记账工具。坚持单一数据源驱动与纯本地隐私优先设计。",
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}
