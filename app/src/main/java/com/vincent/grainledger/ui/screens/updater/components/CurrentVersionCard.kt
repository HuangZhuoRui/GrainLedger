package com.vincent.grainledger.ui.screens.updater.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
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
import com.vincent.grainledger.R
import com.vincent.grainledger.ui.components.card.MiuixSectionCard
import com.vincent.grainledger.ui.components.display.StatusBadge
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixShapes
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 检查更新页面当前运行版本卡片 (CurrentVersionCard)。
 *
 * 展示应用当前运行版本徽章与手动“检查新版本”按钮。
 *
 * @param currentVersion 当前安装版本号
 * @param onCheckUpdateClick 点击检查新版本回调
 * @param modifier 外部修饰符
 */
@Composable
fun CurrentVersionCard(
    currentVersion: String,
    onCheckUpdateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    MiuixSectionCard(
        modifier = modifier,
        cornerRadius = 22.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 软件官方精致 Logo
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "余粮 Logo",
                modifier = Modifier
                    .size(64.dp)
                    .clip(MiuixShapes.MediumSquircle)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "余粮 GrainLedger",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBadge(text = "当前版本 v$currentVersion", color = MiuixBlue)
                StatusBadge(text = "正式发行版", color = MiuixGreen)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCheckUpdateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(color = MiuixBlue)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "检查更新",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "检查新版本",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
