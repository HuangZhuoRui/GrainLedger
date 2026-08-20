package com.vincent.grainledger

import com.vincent.grainledger.util.MathFormulaEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 数学公式计算与表达式解析器单元测试。
 *
 * 验证加法、乘法、连续运算、浮点精度与异常容错。
 */
class MathFormulaEvaluatorTest {

    @Test
    fun testDirectNumbers() {
        assertEquals(180.59, MathFormulaEvaluator.evaluate("180.59"), 0.001)
        assertEquals(5200.0, MathFormulaEvaluator.evaluate("5200"), 0.001)
        assertEquals(0.0, MathFormulaEvaluator.evaluate("0"), 0.001)
    }

    @Test
    fun testAdditionExpressions() {
        // 电费: 30+50 = 80
        assertEquals(80.0, MathFormulaEvaluator.evaluate("30+50"), 0.001)
        // 电费: 31+50 = 81
        assertEquals(81.0, MathFormulaEvaluator.evaluate("31+50"), 0.001)
        // 车费: 39+26+5+5 = 75
        assertEquals(75.0, MathFormulaEvaluator.evaluate("39+26+5+5"), 0.001)
    }

    @Test
    fun testMultiplicationExpressions() {
        // 洗衣费: 6 * 4.2 = 25.2
        assertEquals(25.2, MathFormulaEvaluator.evaluate("6*4.2"), 0.001)
        // 日常吃: 30 * 30 = 900
        assertEquals(900.0, MathFormulaEvaluator.evaluate("30*30"), 0.001)
        // 减肥吃: 7 * 30 = 210
        assertEquals(210.0, MathFormulaEvaluator.evaluate("7*30"), 0.001)
    }

    @Test
    fun testComplexExpressions() {
        // 混合运算
        assertEquals(94.1, MathFormulaEvaluator.evaluate("127.01-32.91"), 0.01)
        assertEquals(0.0, MathFormulaEvaluator.evaluate(""), 0.001)
    }
}
