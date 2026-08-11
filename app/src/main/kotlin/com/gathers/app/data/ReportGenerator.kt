package com.gathers.app.data

import java.util.Calendar
import java.util.Locale

/** 周/月报告数据 */
data class ReportData(
    val title: String,
    val totalCount: Int,
    val totalSize: Long,
    val newCount: Int,
    val dailyTrend: List<Pair<String, Int>>,       // "MM-dd" → 数量（最近 14 天）
    val topApps: List<Pair<String, Int>>,           // 应用 → 数量（Top5）
    val hourlyHeat: List<Int>,                      // 24 小时柱（0-23）
    val financeCount: Int,                          // 财务/账单类截图数
    val protectedCount: Int,                        // 受保护数
    val lowValueCount: Int,                         // 低价值建议清理数
)

/** 生成周/月截图行为报告（纯本地统计） */
object ReportGenerator {

    fun generate(
        screenshots: List<Screenshot>,
        range: ReportRange,
        rules: List<SmartRule> = BuiltinRules.all,
        now: Long = System.currentTimeMillis(),
    ): ReportData {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val start: Long
        val title: String
        when (range) {
            ReportRange.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                start = cal.timeInMillis
                title = "本周截图报告"
            }
            ReportRange.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                start = cal.timeInMillis
                title = "本月截图报告"
            }
            ReportRange.ALL -> {
                start = 0L
                title = "全部截图报告"
            }
        }

        val inRange = screenshots.filter { it.takenAt >= start && !it.inTrash }

        // 最近 14 天趋势（按天）
        val trend = mutableListOf<Pair<String, Int>>()
        val trendCal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (i in 13 downTo 0) {
            trendCal.add(Calendar.DAY_OF_MONTH, -1)
            val dayStart = trendCal.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            val count = inRange.count { it.takenAt in dayStart until dayEnd }
            val label = "%02d-%02d".format(
                trendCal.get(Calendar.MONTH) + 1,
                trendCal.get(Calendar.DAY_OF_MONTH),
            )
            trend.add(label to count)
        }
        // 14 天里的新增 = 趋势和
        val newCount = trend.sumOf { it.second }

        // Top5 来源应用
        val appCount = inRange.groupingBy { it.sourceApp.ifBlank { "未知" } }.eachCount()
        val topApps = appCount.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }

        // 24 小时热力
        val hourly = IntArray(24)
        inRange.forEach { s ->
            val h = Calendar.getInstance().apply { timeInMillis = s.takenAt }.get(Calendar.HOUR_OF_DAY)
            if (h in 0..23) hourly[h]++
        }

        return ReportData(
            title = title,
            totalCount = inRange.size,
            totalSize = inRange.sumOf { it.sizeBytes },
            newCount = newCount,
            dailyTrend = trend,
            topApps = topApps,
            hourlyHeat = hourly.toList(),
            financeCount = inRange.count { it.contentTags.contains("财务") || it.contentTags.contains("凭证") },
            protectedCount = inRange.count { it.isProtected || it.isFavorite },
            lowValueCount = inRange.count { it.isLowValue || RuleEngine.apply(it, rules).contains(RuleAction.SUGGEST_CLEAN) },
        )
    }

    /** 导出 Markdown 文本 */
    fun toMarkdown(data: ReportData): String {
        val sb = StringBuilder()
        sb.append("# ").append(data.title).append('\n')
        sb.append("\n- 截图总数：").append(data.totalCount)
        sb.append("\n- 存储占用：").append(com.gathers.app.util.formatSize(data.totalSize))
        sb.append("\n- 近 14 天新增：").append(data.newCount)
        sb.append("\n- 财务/凭证类：").append(data.financeCount)
        sb.append("\n- 受保护（高价值）：").append(data.protectedCount)
        sb.append("\n- 建议清理：").append(data.lowValueCount)
        sb.append("\n\n## Top 来源应用\n")
        data.topApps.forEachIndexed { i, (app, count) ->
            sb.append("${i + 1}. ").append(app).append(" — ").append(count).append(" 张\n")
        }
        sb.append("\n## 近 14 天趋势\n")
        data.dailyTrend.forEach { (day, count) ->
            sb.append("- ").append(day).append("：").append(count).append('\n')
        }
        sb.append("\n## 时段分布（小时）\n")
        val hotHours = data.hourlyHeat.mapIndexed { h, c -> h to c }.filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
        if (hotHours.isEmpty()) {
            sb.append("- 暂无数据\n")
        } else {
            hotHours.forEach { (h, c) ->
                sb.append("- ").append("%02d:00".format(Locale.getDefault(), h)).append(" — ").append(c).append(" 张\n")
            }
        }
        sb.append("\n> 由 GatherS 本地生成，不含 AI 联网分析。\n")
        return sb.toString()
    }
}

enum class ReportRange { WEEK, MONTH, ALL }
