package com.vincent.grainledger

import com.vincent.grainledger.data.updater.AppUpdaterService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 软件检查更新与高速下载服务单元测试。
 */
class AppUpdaterTest {

    private val updaterService = AppUpdaterService()

    @Test
    fun testAcceleratedUrlConversion() {
        val directGithubUrl = "https://github.com/HuangZhuoRui/GrainLedger/releases/download/v1.0.1/GrainLedger-v1.0.1.apk"
        val accelerated = updaterService.getAcceleratedDownloadUrl(directGithubUrl)
        val expected = "https://update.vincenthzr.org:8443/download/HuangZhuoRui/GrainLedger/releases/download/v1.0.1/GrainLedger-v1.0.1.apk"

        assertEquals(expected, accelerated)
    }

    @Test
    fun testVersionComparison() {
        // 新版本判定
        assertTrue(updaterService.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(updaterService.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(updaterService.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(updaterService.isNewerVersion("v1.0.0+2", "1.0.0+1"))

        // 同版本与旧版本判定
        assertFalse(updaterService.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(updaterService.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(updaterService.isNewerVersion("v0.9.9", "1.0.0"))
        assertFalse(updaterService.isNewerVersion("1.0.0", "1.0.1"))
    }
}
