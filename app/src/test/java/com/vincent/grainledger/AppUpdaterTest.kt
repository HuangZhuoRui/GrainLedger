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

    @Test
    fun testParsedChangelogConventionalCommits() {
        val sampleReleaseBody = """
            # Release v1.1.0
            - feat(ui): 增加通用页面容器AppPageScaffold并适配沉浸式状态栏
            * feat(budget)：新增预算细项算式公式支持
            - fix(icon): 修复自适应图标退出动画露出绿色底色
            - fix: 修复记账弹窗点击无响应问题
            - perf(core): 优化流水按日聚合计算性能
            - refactor(updater): 重构结构化更新日志解析引擎
            - style(theme): 优化AMOLED纯黑模式高对比度卡片
            - docs: 新增项目Git提交规范指南COMMIT_CONVENTION.md
            - chore(deps): 升级Compose依赖版本
            - 包含多项常规体验优化
            - tmp: 忽略临时调试提交
        """.trimIndent()

        val parsed = com.vincent.grainledger.data.updater.ParsedChangelog.parse(sampleReleaseBody)

        // 验证 features
        assertEquals(2, parsed.features.size)
        assertEquals("[UI] 增加通用页面容器AppPageScaffold并适配沉浸式状态栏", parsed.features[0])
        assertEquals("[BUDGET] 新增预算细项算式公式支持", parsed.features[1])

        // 验证 fixes
        assertEquals(2, parsed.fixes.size)
        assertEquals("[ICON] 修复自适应图标退出动画露出绿色底色", parsed.fixes[0])
        assertEquals("修复记账弹窗点击无响应问题", parsed.fixes[1])

        // 验证 improvements (perf, refactor, style)
        assertEquals(3, parsed.improvements.size)
        assertEquals("[CORE] 优化流水按日聚合计算性能", parsed.improvements[0])
        assertEquals("[UPDATER] 重构结构化更新日志解析引擎", parsed.improvements[1])
        assertEquals("[THEME] 优化AMOLED纯黑模式高对比度卡片", parsed.improvements[2])

        // 验证 others (docs, chore, other lines)
        assertEquals(3, parsed.others.size)
        assertEquals("新增项目Git提交规范指南COMMIT_CONVENTION.md", parsed.others[0])
        assertEquals("[DEPS] 升级Compose依赖版本", parsed.others[1])
        assertEquals("包含多项常规体验优化", parsed.others[2])

        assertTrue(parsed.hasCategorized)
    }
}
