package com.vincent.grainledger.ui.components.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vincent.grainledger.data.updater.DownloadProgress
import com.vincent.grainledger.ui.theme.MiuixBlue
import com.vincent.grainledger.ui.theme.MiuixGreen
import com.vincent.grainledger.ui.theme.MiuixRed
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 实时下载进度条面板组件 (DownloadProgressPanel)。
 *
 * 规范呈现下载进度百分比、实时下载速率、已下载/总大小、线性进度条与取消按钮。
 *
 * @param downloadProgress 实时下载进度数据流
 * @param onCancelDownload 取消下载回调
 * @param modifier 外部修饰符
 */
@Composable
fun DownloadProgressPanel(
    downloadProgress: DownloadProgress,
    onCancelDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "正在下载: ${(downloadProgress.progress * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixBlue
            )
            Text(
                text = downloadProgress.formattedSpeed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixGreen
            )
        }

        LinearProgressIndicator(
            progress = { downloadProgress.progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MiuixBlue,
            trackColor = MiuixTheme.colorScheme.surface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = downloadProgress.formattedDownloadedTotal,
                fontSize = 11.5.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )

            Button(
                onClick = onCancelDownload,
                modifier = Modifier.height(28.dp),
                insideMargin = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(color = MiuixRed.copy(alpha = 0.85f))
            ) {
                Text(
                    text = "取消",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
