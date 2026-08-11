package com.gathers.app.core.domain.model

import java.io.File
import java.time.Instant

/**
 * 截图实体 — 领域层核心模型
 *
 * @param id 唯一标识（MediaStore URI 或文件路径 hash）
 * @param uri 原始 URI
 * @param file 本地文件引用（可能为 null（若未准备就绪））
 * @param fileName 原始文件名
 * @param sizeBytes 文件大小（字节）
 * @param mimeType MIME 类型
 * @param width 图片宽度（像素）
 * @param height 图片高度（像素）
 * @param dateTaken 拍摄/截图时间
 * @param dateAdded 添加到 MediaStore 的时间
 * @param sourceApp 来源应用包名（如 com.tencent.mm）
 * @param tags 关联标签列表
 * @param isTrashed 是否在回收站中
 * @param trashedAt 移入回收站的时间（null 表示不在回收站）
 */
data class Screenshot(
    val id: String,
    val uri: String,
    val file: File? = null,
    val fileName: String = "",
    val sizeBytes: Long = 0L,
    val mimeType: String = "image/png",
    val width: Int = 0,
    val height: Int = 0,
    val dateTaken: Instant = Instant.EPOCH,
    val dateAdded: Instant = Instant.EPOCH,
    val sourceApp: String? = null,
    val tags: List<String> = emptyList(),
    val isTrashed: Boolean = false,
    val trashedAt: Instant? = null,
)