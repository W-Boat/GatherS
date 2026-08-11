package com.gathers.app.core.data.scanner

import com.gathers.app.core.domain.model.Screenshot
import kotlinx.coroutines.flow.Flow

/**
 * 截图扫描器接口 — 负责查询 MediaStore 获取截图列表
 */
interface ScreenshotScanner {
    /** 扫描全部截图（返回 Flow，支持增量更新） */
    fun scanAll(): Flow<List<Screenshot>>

    /** 执行一次完整扫描 */
    suspend fun scanOnce(): List<Screenshot>
}