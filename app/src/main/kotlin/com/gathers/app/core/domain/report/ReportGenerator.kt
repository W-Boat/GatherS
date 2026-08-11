package com.gathers.app.core.domain.report

import com.gathers.app.core.domain.model.Screenshot

/**
 * 报表聚合数据
 */
data class ReportSummary(
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val bySourceApp: Map<String, Int> = emptyMap(),
    val byMonth: Map<String, Int> = emptyMap(),
    val byTag: Map<String, Int> = emptyMap(),
    val bySizeCategory: SizeDistribution = SizeDistribution(),
)

data class SizeDistribution(
    val tiny: Int = 0,   // < 100KB
    val small: Int = 0,  // 100KB ~ 1MB
    val medium: Int = 0, // 1MB ~ 5MB
    val large: Int = 0,  // 5MB ~ 10MB
    val huge: Int = 0,   // > 10MB
)

/**
 * 报表生成器 — 纯逻辑
 */
class ReportGenerator {
    fun generate(screenshots: List<Screenshot>): ReportSummary {
        if (screenshots.isEmpty()) return ReportSummary()

        val bySourceApp = screenshots
            .filter { it.sourceApp != null }
            .groupBy { it.sourceApp!! }
            .mapValues { it.value.size }

        val byMonth = screenshots
            .groupBy { "${it.dateTaken.year}-${it.dateTaken.month.value.toString().padStart(2, '0')}" }
            .mapValues { it.value.size }

        val byTag = screenshots
            .flatMap { it.tags }
            .groupBy { it }
            .mapValues { it.value.size }

        val totalSize = screenshots.sumOf { it.sizeBytes }

        val sizeDist = SizeDistribution(
            tiny = screenshots.count { it.sizeBytes < 100_000 },
            small = screenshots.count { it.sizeBytes in 100_000..<1_000_000 },
            medium = screenshots.count { it.sizeBytes in 1_000_000..<5_000_000 },
            large = screenshots.count { it.sizeBytes in 5_000_000..<10_000_000 },
            huge = screenshots.count { it.sizeBytes >= 10_000_000 },
        )

        return ReportSummary(
            totalCount = screenshots.size,
            totalSizeBytes = totalSize,
            bySourceApp = bySourceApp,
            byMonth = byMonth,
            byTag = byTag,
            bySizeCategory = sizeDist,
        )
    }
}