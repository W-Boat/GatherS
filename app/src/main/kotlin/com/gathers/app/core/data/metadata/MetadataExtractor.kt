package com.gathers.app.core.data.metadata

import com.gathers.app.core.domain.model.Screenshot
import java.io.InputStream

/**
 * 元数据提取接口 — 从文件内容提取 EXIF、尺寸等信息
 */
interface MetadataExtractor {
    /** 从输入流中提取元数据，丰富 Screenshot 对象 */
    suspend fun extract(screenshot: Screenshot, inputStream: InputStream): Screenshot
}