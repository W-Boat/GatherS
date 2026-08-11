package com.gathers.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
private val monthFmt = SimpleDateFormat("yyyy年M月", Locale.getDefault())

/** 时间戳 → "2026-08-11 10:30" */
fun formatDateTime(epochMillis: Long): String =
    if (epochMillis <= 0) "—" else dateTimeFmt.format(Date(epochMillis))

/** 时间戳 → "2026-08-11" */
fun formatDate(epochMillis: Long): String =
    if (epochMillis <= 0) "—" else dateFmt.format(Date(epochMillis))

/** 时间戳 → "10:30" */
fun formatTime(epochMillis: Long): String =
    if (epochMillis <= 0) "—" else timeFmt.format(Date(epochMillis))

/** 时间戳 → "2026年8月" */
fun formatMonth(epochMillis: Long): String =
    if (epochMillis <= 0) "—" else monthFmt.format(Date(epochMillis))

/** 字节 → 人类可读 */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024 && i < units.size - 1) {
        value /= 1024
        i++
    }
    return if (i == 0) "${bytes} B" else String.format(Locale.getDefault(), "%.1f %s", value, units[i])
}

/** 相对时间：刚刚 / n分钟前 / n小时前 / n天前 */
fun formatRelative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0) return "—"
    val diff = now - epochMillis
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
        diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} 分钟前"
        diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} 小时前"
        diff < TimeUnit.DAYS.toMillis(30) -> "${diff / TimeUnit.DAYS.toMillis(1)} 天前"
        else -> formatDate(epochMillis)
    }
}

/** "8月11日 10:30" 风格 */
fun formatMonthDayTime(epochMillis: Long): String {
    if (epochMillis <= 0) return "—"
    val c = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
    val md = java.text.SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(epochMillis))
    return "$md ${formatTime(epochMillis)}"
}
