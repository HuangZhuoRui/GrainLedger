package com.vincent.grainledger.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 数学公式与表达式解析工具类。
 *
 * 用于在记账和预算录入时，解析用户输入的算式（例如：30+50、6*4.2、39+26+5+5 等），
 * 避免用户在手机端需要手动切换计算器计算后再填入数值。
 */
object MathFormulaEvaluator {

    /**
     * 计算给定的算式字符串，并返回双精度浮点数结果。
     *
     * 支持加法（+）、减法（-）、乘法（*）、除法（/）以及带小数点的数值计算。
     * 如果输入的是普通数字字符串，则直接解析为双精度浮点数；
     * 如果输入为空或格式不合法，则返回默认值 0.0。
     *
     * @param expressionString 需要计算的算术表达式，例如 "30+50" 或 "180.59"
     * @return 计算后的数值结果
     */
    fun evaluate(expressionString: String): Double {
        val cleanExpression = expressionString.replace(" ", "").trim()
        if (cleanExpression.isEmpty()) {
            return 0.0
        }

        return try {
            // 如果仅为普通数字，直接转换
            if (cleanExpression.matches(Regex("^-?\\d+(\\.\\d+)?$"))) {
                return cleanExpression.toDouble()
            }

            // 解析带有简单四则运算的表达式
            val parsedResult = parseExpression(cleanExpression)
            // 保留两位小数并进行四舍五入
            BigDecimal(parsedResult).setScale(2, RoundingMode.HALF_UP).toDouble()
        } catch (exception: Exception) {
            0.0
        }
    }

    /**
     * 内部递归下降表达式解析器，用于处理加减乘除优先级。
     *
     * @param expressionContent 去除空格后的字符串
     * @return 计算结果
     */
    private fun parseExpression(expressionContent: String): Double {
        var cursorPosition = 0

        fun peekCurrentChar(): Char? {
            return if (cursorPosition < expressionContent.length) expressionContent[cursorPosition] else null
        }

        fun consumeCurrentChar(): Char {
            val char = expressionContent[cursorPosition]
            cursorPosition++
            return char
        }

        // 解析基本因子（数字或括号内的表达式）
        fun parseFactor(): Double {
            var isNegative = false
            if (peekCurrentChar() == '+') {
                consumeCurrentChar()
            } else if (peekCurrentChar() == '-') {
                consumeCurrentChar()
                isNegative = true
            }

            val startIndex = cursorPosition
            while (peekCurrentChar() != null && (peekCurrentChar()!!.isDigit() || peekCurrentChar() == '.')) {
                consumeCurrentChar()
            }

            if (startIndex == cursorPosition) {
                return 0.0
            }

            val numberText = expressionContent.substring(startIndex, cursorPosition)
            val numericValue = numberText.toDoubleOrNull() ?: 0.0
            return if (isNegative) -numericValue else numericValue
        }

        // 解析乘除项（高优先级）
        fun parseTerm(): Double {
            var currentValue = parseFactor()
            while (true) {
                val operator = peekCurrentChar()
                if (operator == '*' || operator == 'x' || operator == 'X') {
                    consumeCurrentChar()
                    val rightOperand = parseFactor()
                    currentValue *= rightOperand
                } else if (operator == '/' || operator == '÷') {
                    consumeCurrentChar()
                    val rightOperand = parseFactor()
                    if (rightOperand != 0.0) {
                        currentValue /= rightOperand
                    }
                } else {
                    break
                }
            }
            return currentValue
        }

        // 解析加减表达式（低优先级）
        var accumulatedResult = parseTerm()
        while (true) {
            val operator = peekCurrentChar()
            if (operator == '+') {
                consumeCurrentChar()
                val rightOperand = parseTerm()
                accumulatedResult += rightOperand
            } else if (operator == '-') {
                consumeCurrentChar()
                val rightOperand = parseTerm()
                accumulatedResult -= rightOperand
            } else {
                break
            }
        }

        return accumulatedResult
    }

    /**
     * 格式化金额数值为规范的货币显示文本。
     *
     * @param amountValue 需要格式化的双精度浮点数
     * @param decimalPlaces 默认保留 2 位小数
     * @return 格式化后的字符串，例如 "127.01" 或 "-180.59"
     */
    fun formatAmount(amountValue: Double, decimalPlaces: Int = 2): String {
        return String.format(java.util.Locale.CHINA, "%.${decimalPlaces}f", amountValue)
    }
}
