package com.gathers.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/** 单张截图的 EXIF / 像素级元数据 */
data class ImageMeta(
    val imageDescription: String? = null,
    val software: String? = null,
    val model: String? = null,
    val exifDateTime: Long? = null,
    val blurScore: Int = -1,
    val isMostlyBlank: Boolean = false,
)

/** 提取 EXIF 元数据与（可选的）模糊度/空白检测 */
object MetaExtractor {

    suspend fun extract(context: Context, uri: android.net.Uri, withPixels: Boolean = false): ImageMeta =
        withContext(Dispatchers.IO) { extractSync(context, uri, withPixels) }

    /** 同步版本：必须在 IO 线程调用（扫描循环内使用） */
    fun extractSync(context: Context, uri: android.net.Uri, withPixels: Boolean = false): ImageMeta {
        var exif: ImageMeta = ImageMeta()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val e = ExifInterface(stream)
                exif = ImageMeta(
                    imageDescription = e.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION),
                    software = e.getAttribute(ExifInterface.TAG_SOFTWARE),
                    model = e.getAttribute(ExifInterface.TAG_MODEL),
                    exifDateTime = parseExifDate(e.getAttribute(ExifInterface.TAG_DATETIME)),
                )
            }
        }
        if (withPixels) {
            runCatching {
                val bitmap = decodeSampled(context, uri, maxDim = 64)
                if (bitmap != null) {
                    val (blur, blank) = analyzeBitmap(bitmap)
                    bitmap.recycle()
                    exif = exif.copy(blurScore = blur, isMostlyBlank = blank)
                }
            }
        }
        return exif
    }

    /** 采样解码，最大边长不超过 maxDim */
    fun decodeSampled(context: Context, uri: android.net.Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    /**
     * 简化模糊度检测：缩放到 32x32，计算相邻像素灰度差绝对值的均值，
     * 归一化到 0-100（值越大越模糊/越平滑）。
     * 同时估计"空白页"（像素方差极小）。
     */
    fun analyzeBitmap(bitmap: Bitmap): Pair<Int, Boolean> {
        val w = 32
        val h = 32
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        if (scaled !== bitmap) scaled.recycle()

        var sumDiff = 0L
        var count = 0L
        var sumGray = 0L
        var sumGraySq = 0L
        for (y in 0 until h) {
            for (x in 0 until w) {
                val g = gray(pixels[y * w + x])
                sumGray += g
                sumGraySq += g.toLong() * g
                if (x > 0) {
                    sumDiff += kotlin.math.abs(g - gray(pixels[y * w + x - 1]))
                    count++
                }
                if (y > 0) {
                    sumDiff += kotlin.math.abs(g - gray(pixels[(y - 1) * w + x]))
                    count++
                }
            }
        }
        val avgDiff = if (count > 0) sumDiff.toDouble() / count else 0.0
        // 平滑区域 avgDiff 小 → 模糊分高。0..255 → 0..100
        val blur = (255.0 - avgDiff.coerceIn(0.0, 255.0)) / 255.0 * 100.0
        val mean = sumGray.toDouble() / (w * h)
        val variance = sumGraySq.toDouble() / (w * h) - mean * mean
        val mostlyBlank = variance < 60.0 && avgDiff < 4.0
        return blur.toInt() to mostlyBlank
    }

    private fun gray(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /** "2026:08:11 10:30:45" → epoch millis */
    private fun parseExifDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val m = Regex("""(\d{4}):(\d{2}):(\d{2})[ T](\d{2}):(\d{2}):(\d{2})""").find(value) ?: return null
        val parts = m.groupValues.drop(1).map { it.toInt() }
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(parts[0], parts[1] - 1, parts[2], parts[3], parts[4], parts[5])
        }
        return cal.timeInMillis
    }

    /** 从 content URI 读取原始字节流（供复制到回收站使用） */
    fun openStream(context: Context, uri: android.net.Uri): InputStream? =
        runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
}
