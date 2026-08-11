package com.gathers.app.core.data.storage

import com.gathers.app.core.domain.model.Screenshot
import com.gathers.app.core.domain.rule.Rule
import com.gathers.app.core.domain.trash.TrashItem

/**
 * 索引存储接口 — 管理标签、规则、回收站等持久化数据
 */
interface IndexStorage {
    // --- Screenshot 元数据 ---
    suspend fun saveScreenshots(screenshots: List<Screenshot>)
    suspend fun getAllScreenshots(): List<Screenshot>
    suspend fun getScreenshot(id: String): Screenshot?
    suspend fun updateScreenshot(screenshot: Screenshot)
    suspend fun deleteScreenshot(id: String)

    // --- Tags ---
    suspend fun saveTags(tags: List<com.gathers.app.core.domain.tag.Tag>)
    suspend fun getAllTags(): List<com.gathers.app.core.domain.tag.Tag>

    // --- Rules ---
    suspend fun saveRules(rules: List<Rule>)
    suspend fun getAllRules(): List<Rule>

    // --- Trash ---
    suspend fun saveTrashItems(items: List<TrashItem>)
    suspend fun getTrashItems(): List<TrashItem>
    suspend fun removeTrashItem(id: String)
}