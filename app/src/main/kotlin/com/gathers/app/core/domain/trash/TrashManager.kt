package com.gathers.app.core.domain.trash

import com.gathers.app.core.domain.model.Screenshot
import java.time.Instant

/**
 * 回收站模型
 */
data class TrashBin(
    val items: List<TrashItem> = emptyList(),
)

data class TrashItem(
    val screenshot: Screenshot,
    val trashedAt: Instant = Instant.now(),
    val autoDeleteAt: Instant? = null,
)

/**
 * 回收站管理 — 纯逻辑（无 Android 依赖）
 */
class TrashManager(
    private val retentionDays: Int = 30,
) {
    /** 移入回收站 */
    fun trash(screenshot: Screenshot): TrashItem {
        val autoDelete = screenshot.dateTaken.plusSeconds(retentionDays * 86400L)
        return TrashItem(
            screenshot = screenshot.copy(isTrashed = true, trashedAt = Instant.now()),
            trashedAt = Instant.now(),
            autoDeleteAt = autoDelete,
        )
    }

    /** 恢复 */
    fun restore(item: TrashItem): Screenshot {
        return item.screenshot.copy(isTrashed = false, trashedAt = null)
    }

    /** 查找过期项 */
    fun expiredItems(bin: TrashBin): List<TrashItem> {
        val now = Instant.now()
        return bin.items.filter { it.autoDeleteAt != null && it.autoDeleteAt <= now }
    }
}