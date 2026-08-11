package com.gathers.app.data

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** 回收站中的一条记录 */
data class TrashEntry(
    val mediaId: Long,
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val trashedAt: Long,
)

/**
 * 内置回收站：清理时先将原图复制到应用私有目录，
 * 再请求删除相册中的原图（Android 10+ 需系统确认）。
 * 支持一键还原、永久删除与 30 天自动过期。
 */
object TrashManager {

    private const val INDEX_NAME = "trash_index.json"
    private const val MAX_AGE_DAYS = 30L

    fun trashDir(context: Context): File =
        File(context.filesDir, "trash").apply { mkdirs() }

    private fun indexFile(context: Context): File = File(context.filesDir, INDEX_NAME)

    fun loadIndex(context: Context): List<TrashEntry> {
        val f = indexFile(context)
        if (!f.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(f.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        TrashEntry(
                            mediaId = o.getLong("mediaId"),
                            fileName = o.getString("fileName"),
                            relativePath = o.getString("relativePath"),
                            sizeBytes = o.optLong("sizeBytes", 0),
                            trashedAt = o.optLong("trashedAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveIndex(context: Context, entries: List<TrashEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("mediaId", e.mediaId)
                    put("fileName", e.fileName)
                    put("relativePath", e.relativePath)
                    put("sizeBytes", e.sizeBytes)
                    put("trashedAt", e.trashedAt)
                },
            )
        }
        indexFile(context).writeText(arr.toString())
    }

    private fun copyFile(context: Context, uri: Uri, dest: File): Boolean {
        val input = context.contentResolver.openInputStream(uri) ?: return false
        return try {
            input.use { ins ->
                FileOutputStream(dest).use { outs -> ins.copyTo(outs) }
            }
            true
        } catch (e: Exception) {
            dest.delete()
            false
        }
    }

    /**
     * 将截图复制进回收站并登记。
     * @return 成功返回登记条目；失败返回 null
     */
    suspend fun copyToTrash(context: Context, screenshot: Screenshot): TrashEntry? =
        withContext(Dispatchers.IO) {
            val safeName = screenshot.displayName.replace(Regex("""[\\/:*?"<>|]"""), "_")
            val dest = File(trashDir(context), "${screenshot.id}_$safeName")
            if (!copyFile(context, screenshot.uri, dest)) return@withContext null
            val entry = TrashEntry(
                mediaId = screenshot.id,
                fileName = screenshot.displayName,
                relativePath = screenshot.relativePath,
                sizeBytes = screenshot.sizeBytes,
                trashedAt = System.currentTimeMillis(),
            )
            val entries = loadIndex(context).filterNot { it.mediaId == entry.mediaId } + entry
            saveIndex(context, entries)
            entry
        }

    /** 原图删除被用户取消：撤销副本与登记 */
    fun cancelTrash(context: Context, mediaId: Long) {
        val entries = loadIndex(context).filterNot { it.mediaId == mediaId }
        trashDir(context).listFiles()?.forEach { f ->
            if (f.name.startsWith("${mediaId}_")) f.delete()
        }
        saveIndex(context, entries)
    }

    /** 原图已通过系统确认删除：副本保留（记录已存在，无需额外动作） */
    fun confirmDeleted(context: Context, mediaId: Long) {
        // 登记已在 copyToTrash 完成；此处仅为语义占位，确保索引存在
        if (loadIndex(context).none { it.mediaId == mediaId }) {
            // 极端情况：登记丢失则重建（需要文件名，一般不会走到）
        }
    }

    /**
     * 从回收站还原：将副本重新写入相册原目录。
     * 仅支持 Android 10+（RELATIVE_PATH）；旧版本返回 false。
     */
    suspend fun restore(context: Context, entry: TrashEntry): Boolean =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext false
            val src = trashDir(context).listFiles()?.firstOrNull { it.name.startsWith("${entry.mediaId}_") }
                ?: return@withContext false
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, entry.fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, entry.relativePath.ifBlank { "Pictures/Screenshots" })
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = context.contentResolver.insert(collection, values) ?: return@withContext false
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    src.inputStream().use { ins -> ins.copyTo(out) }
                }
                true
            }.getOrDefault(false)
            if (ok) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                src.delete()
                saveIndex(context, loadIndex(context).filterNot { it.mediaId == entry.mediaId })
                true
            } else {
                context.contentResolver.delete(uri, null, null)
                false
            }
        }

    /** 永久删除回收站条目 */
    suspend fun purge(context: Context, entry: TrashEntry): Boolean =
        withContext(Dispatchers.IO) {
            trashDir(context).listFiles()?.forEach { f ->
                if (f.name.startsWith("${entry.mediaId}_")) f.delete()
            }
            saveIndex(context, loadIndex(context).filterNot { it.mediaId == entry.mediaId })
            true
        }

    /** 清空回收站 */
    suspend fun purgeAll(context: Context) {
        withContext(Dispatchers.IO) {
            trashDir(context).listFiles()?.forEach { it.delete() }
            indexFile(context).delete()
        }
    }

    /** 过期（超过 30 天）的条目 */
    fun expired(context: Context): List<TrashEntry> {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_AGE_DAYS)
        return loadIndex(context).filter { it.trashedAt < cutoff }
    }

    /** 构建删除原图的系统确认 Intent（Android 10+） */
    fun buildDeletePendingIntent(context: Context, uris: List<Uri>): PendingIntent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || uris.isEmpty()) return null
        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        }.getOrNull()
    }
}
