package com.gathers.app.data

import java.util.Calendar
import java.util.TimeZone

/** 文件名解析结果 */
data class ParsedFileName(
    val epochMillis: Long? = null,
    val deviceHint: String? = null,
    val action: String? = null,
)

/**
 * 解析系统截图文件名中的时间戳、设备代号与行为标注。
 *
 * 支持常见命名模式：
 * - Screenshot_2026-08-11-10-30-45.png（Android 原生）
 * - Screenshot_20260811-103045.png（部分国产 ROM）
 * - IMG_20260811_103045.png / 2026-08-11-10-30-45.png
 * - 文件名后缀行为标注：_edit / _crop / _enhanced / _recovered / _trimmed
 */
object FileNameParser {

    private val regexTimestampDash = Regex(
        """(20\d{2})[-_.](\d{2})[-_.](\d{2})[-_.](\d{2})[-_.](\d{2})[-_.](\d{2})""",
    )
    private val regexTimestampCompact = Regex(
        """(20\d{2})(\d{2})(\d{2})[-_.](\d{2})(\d{2})(\d{2})""",
    )
    private val regexTimestampDateOnly = Regex(
        """(20\d{2})[-_.](\d{2})[-_.](\d{2})""",
    )

    private val actionSuffixes = listOf(
        "_edit" to "编辑",
        "_crop" to "裁剪",
        "_enhanced" to "增强",
        "_recovered" to "恢复",
        "_trimmed" to "裁剪",
        "_long" to "长截图",
        "_scroll" to "长截图",
    )

    fun parse(fileName: String): ParsedFileName {
        val name = fileName.substringBeforeLast('.').lowercase()
        var epoch: Long? = null

        regexTimestampDash.find(name)?.let { m ->
            epoch = toEpoch(
                m.groupValues[1].toInt(),
                m.groupValues[2].toInt(),
                m.groupValues[3].toInt(),
                m.groupValues[4].toInt(),
                m.groupValues[5].toInt(),
                m.groupValues[6].toInt(),
            )
        }
        if (epoch == null) {
            regexTimestampCompact.find(name)?.let { m ->
                epoch = toEpoch(
                    m.groupValues[1].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[3].toInt(),
                    m.groupValues[4].toInt(),
                    m.groupValues[5].toInt(),
                    m.groupValues[6].toInt(),
                )
            }
        }
        if (epoch == null) {
            regexTimestampDateOnly.find(name)?.let { m ->
                epoch = toEpoch(
                    m.groupValues[1].toInt(),
                    m.groupValues[2].toInt(),
                    m.groupValues[3].toInt(),
                    12, 0, 0,
                )
            }
        }

        val device = when {
            name.contains("screenshot_") && !name.contains("screenshot_2") -> "Android"
            else -> null
        }

        val action = actionSuffixes.firstOrNull { (suffix, _) -> name.contains(suffix) }?.second

        return ParsedFileName(epoch, device, action)
    }

    /** 是否为疑似截图文件名（用于 MediaStore 预筛选） */
    fun isScreenshotLike(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("screenshot") ||
            lower.contains("截图") ||
            lower.contains("scrn") ||
            lower.contains("screen_capture") ||
            lower.contains("截屏")
    }

    private fun toEpoch(
        y: Int,
        mo: Int,
        d: Int,
        h: Int,
        mi: Int,
        s: Int,
    ): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(y, mo - 1, d, h, mi, s)
        }
        return cal.timeInMillis
    }
}
