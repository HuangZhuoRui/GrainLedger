package com.vincent.grainledger.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期与时间处理工具类。
 *
 * 提供年、月、日提取，农历与公历转换支持，以及记账流水的时间格式化。
 */
object DateUtils {

    /**
     * 获取当前系统年份（例如 2026）。
     *
     * @return 当前年份整数
     */
    fun getCurrentYear(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR)
    }

    /**
     * 获取当前系统月份（1 至 12，例如 8 代表八月）。
     *
     * @return 当前月份整数
     */
    fun getCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.MONTH) + 1
    }

    /**
     * 获取当前系统日（1 至 31，例如 20 代表 20 号）。
     *
     * @return 当前日期整数
     */
    fun getCurrentDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * 获取指定年月的天数（例如 2026 年 8 月有 31 天）。
     *
     * @param year 目标年份
     * @param month 目标月份 (1-12)
     * @return 该月的总天数
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /**
     * 格式化年月日为标准展示字符串。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日 (1-31)
     * @return 格式化后的日期文本，例如 "2026-08-18"
     */
    fun formatDate(year: Int, month: Int, day: Int): String {
        return String.format(Locale.CHINA, "%04d-%02d-%02d", year, month, day)
    }

    /**
     * 格式化年月日为中文口语展示文本。
     *
     * @param year 年份
     * @param month 月份 (1-12)
     * @param day 日 (1-31)
     * @return 中文格式化日期，例如 "2026年8月18日"
     */
    fun formatDateChinese(year: Int, month: Int, day: Int): String {
        return String.format(Locale.CHINA, "%d年%d月%d日", year, month, day)
    }

    /**
     * 获取指定年月日对应的星期几中文名称。
     *
     * @param year 年份
     * @param month 月份
     * @param day 日
     * @return 星期文本，例如 "星期二"
     */
    fun getWeekDayName(year: Int, month: Int, day: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)
        val dayOfWeekIndex = calendar.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeekIndex) {
            Calendar.SUNDAY -> "星期日"
            Calendar.MONDAY -> "星期一"
            Calendar.TUESDAY -> "星期二"
            Calendar.WEDNESDAY -> "星期三"
            Calendar.THURSDAY -> "星期四"
            Calendar.FRIDAY -> "星期五"
            Calendar.SATURDAY -> "星期六"
            else -> "星期一"
        }
    }

    /**
     * 将时间戳转换为详细时间字符串。
     *
     * @param timestampMillis 毫秒级时间戳
     * @return 格式化后的时间字符串，例如 "2026-08-18 14:30:00"
     */
    fun formatTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return formatter.format(Date(timestampMillis))
    }
}
