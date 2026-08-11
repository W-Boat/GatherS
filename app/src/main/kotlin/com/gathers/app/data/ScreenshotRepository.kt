package com.gathers.app.data

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/** 用户持久化元数据 */
data class UserMeta(
    val favorite: Boolean = false,
    val protected: Boolean = false,
    val note: String = "",
    val blurScore: Int = -1,
)

/** 截图库仓库：扫描 MediaStore、维护索引、持久化用户状态 */
class ScreenshotRepository private constructor() {

    private val _screenshots = MutableStateFlow<List<Screenshot>>(emptyList())
    val screenshots: StateFlow<List<Screenshot>> = _screenshots.asStateFlow()

    private val _trashEntries = MutableStateFlow<List<TrashEntry>>(emptyList())
    val trashEntries: StateFlow<List<TrashEntry>> = _trashEntries.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _lastScanMillis = MutableStateFlow(0L)
    val lastScanMillis: StateFlow<Long> = _lastScanMillis.asStateFlow()

    private var metaMap: Map<Long, UserMeta> = emptyMap()
    private var metaFile: File? = null

    private val _rules = MutableStateFlow(BuiltinRules.all)
    val rules: StateFlow<List<SmartRule>> = _rules.asStateFlow()

    /** 从 SharedPreferences 读取规则开关状态 */
    fun loadRules(context: Context) {
        val prefs = context.getSharedPreferences("rules", Context.MODE_PRIVATE)
        _rules.value = BuiltinRules.all.map { r ->
            val saved = prefs.getBoolean("rule_${r.id}", r.enabled)
            r.copy(enabled = saved)
        }
    }

    /** 切换规则开关并持久化 */
    fun toggleRule(context: Context, ruleId: String, enabled: Boolean) {
        _rules.value = _rules.value.map { if (it.id == ruleId) it.copy(enabled = enabled) else it }
        context.getSharedPreferences("rules", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("rule_$ruleId", enabled)
            .apply()
    }

    private fun metaFile(context: Context): File = File(context.filesDir, "meta.json")

    private fun loadMeta(context: Context) {
        metaFile = metaFile(context)
        val f = metaFile ?: return
        if (!f.exists()) return
        metaMap = runCatching {
            val root = JSONObject(f.readText())
            val ids = root.names()
            buildMap {
                for (i in 0 until ids.length()) {
                    val idStr = ids.getString(i)
                    val o = root.getJSONObject(idStr)
                    put(
                        idStr.toLong(),
                        UserMeta(
                            favorite = o.optBoolean("favorite", false),
                            protected = o.optBoolean("protected", false),
                            note = o.optString("note", ""),
                            blurScore = o.optInt("blurScore", -1),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveMeta(context: Context) {
        val root = JSONObject()
        metaMap.forEach { (id, m) ->
            root.put(
                id.toString(),
                JSONObject().apply {
                    put("favorite", m.favorite)
                    put("protected", m.protected)
                    put("note", m.note)
                    put("blurScore", m.blurScore)
                },
            )
        }
        runCatching { (metaFile ?: metaFile(context)).writeText(root.toString()) }
    }

    private fun updateUserMeta(context: Context, id: Long, transform: (UserMeta) -> UserMeta) {
        val current = metaMap[id] ?: UserMeta()
        metaMap = metaMap + (id to transform(current))
        saveMeta(context)
        _screenshots.value = _screenshots.value.map {
            if (it.id == id) {
                val m = metaMap[id] ?: UserMeta()
                it.copy(
                    isFavorite = m.favorite,
                    isProtected = m.protected,
                    userNote = m.note,
                    blurScore = if (m.blurScore >= 0) m.blurScore else it.blurScore,
                )
            } else {
                it
            }
        }
    }

    /** 全量/增量扫描 MediaStore（增量：仅处理新增或移除） */
    suspend fun refresh(context: Context) {
        _loading.value = true
        loadMeta(context)
        loadRules(context)
        _trashEntries.value = TrashManager.loadIndex(context)
        withContext(Dispatchers.IO) {
            val scanned = scanMediaStore(context)
            withContext(Dispatchers.Main.immediate) {
                _screenshots.value = scanned
                _loading.value = false
                _lastScanMillis.value = System.currentTimeMillis()
            }
        }
    }

    private fun scanMediaStore(context: Context): List<Screenshot> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.MIME_TYPE,
        )
        // 关键词筛选：截图类命名 / 截图目录
        val selection =
            "(" +
                "DISPLAY_NAME LIKE '%screenshot%' OR " +
                "DISPLAY_NAME LIKE '%截图%' OR " +
                "DISPLAY_NAME LIKE '%scrn%' OR " +
                "DISPLAY_NAME LIKE '%screen_capture%' OR " +
                "DISPLAY_NAME LIKE '%截屏%' OR " +
                "RELATIVE_PATH LIKE '%Screenshot%' OR " +
                "RELATIVE_PATH LIKE '%截图%' OR " +
                "RELATIVE_PATH LIKE '%ScreenCapture%'" +
                ") AND " +
                "BUCKET_DISPLAY_NAME NOT LIKE '%Camera%'"
        val trashIds = _trashEntries.value.map { it.mediaId }.toSet()

        val list = mutableListOf<Screenshot>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val modCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    if (id in trashIds) continue
                    val name = cursor.getString(nameCol) ?: continue
                    val relPath = cursor.getString(pathCol) ?: ""
                    val taken = cursor.getLong(takenCol)
                    val modified = cursor.getLong(modCol)
                    val takenAt = if (taken > 0) taken else modified * 1000L
                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString(),
                    )
                    val parsed = FileNameParser.parse(name)
                    val meta = MetaExtractor.extractSync(context, uri, withPixels = false)
                    val tag = Tagger.tag(name, relPath, takenAt, meta, parsed.action)
                    val user = metaMap[id] ?: UserMeta()
                    list.add(
                        Screenshot(
                            id = id,
                            uri = uri,
                            displayName = name,
                            relativePath = relPath,
                            sizeBytes = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            takenAt = takenAt,
                            mimeType = cursor.getString(mimeCol) ?: "image/png",
                            sourceApp = tag.sourceApp,
                            contentTags = tag.contentTags,
                            stateTags = tag.stateTags,
                            visualTags = tag.visualTags,
                            summary = tag.summary,
                            blurScore = user.blurScore,
                            isFavorite = user.favorite,
                            isProtected = user.protected,
                            userNote = user.note,
                        ),
                    )
                }
            }
        }
        return list
    }

    /** 计算并缓存单张截图的模糊度/空白检测 */
    suspend fun computeBlur(context: Context, screenshot: Screenshot): Int {
        if (screenshot.blurScore >= 0) return screenshot.blurScore
        val meta = MetaExtractor.extract(context, screenshot.uri, withPixels = true)
        val blur = if (meta.isMostlyBlank) 100 else meta.blurScore
        updateUserMeta(context, screenshot.id) { it.copy(blurScore = blur) }
        return blur
    }

    fun toggleFavorite(context: Context, screenshot: Screenshot) {
        updateUserMeta(context, screenshot.id) { it.copy(favorite = !it.favorite) }
    }

    fun toggleProtected(context: Context, screenshot: Screenshot) {
        updateUserMeta(context, screenshot.id) { it.copy(protected = !it.protected) }
    }

    fun setNote(context: Context, screenshot: Screenshot, note: String) {
        updateUserMeta(context, screenshot.id) { it.copy(note = note) }
    }

    /** 批量移入回收站。返回需要系统确认的 PendingIntent；null 表示设备不支持（Android 10 以下） */
    suspend fun moveManyToTrash(context: Context, shots: List<Screenshot>): PendingIntent? {
        if (shots.isEmpty()) return null
        var resultPi: PendingIntent? = null
        val copiedUris = mutableListOf<Uri>()
        withContext(Dispatchers.IO) {
            shots.forEach { s ->
                val entry = TrashManager.copyToTrash(context, s)
                if (entry != null) copiedUris.add(s.uri)
            }
        }
        _trashEntries.value = TrashManager.loadIndex(context)
        if (copiedUris.isNotEmpty()) {
            resultPi = TrashManager.buildDeletePendingIntent(context, copiedUris)
        }
        if (resultPi == null) {
            // 设备无法删除原图：撤销全部副本
            shots.forEach { TrashManager.cancelTrash(context, it.id) }
            _trashEntries.value = TrashManager.loadIndex(context)
        }
        return resultPi
    }

    /** 移入回收站（副本 + 系统确认删除原图）。返回需要用户确认的 PendingIntent（可能为 null） */
    suspend fun moveToTrash(context: Context, screenshot: Screenshot): PendingIntent? {
        val entry = TrashManager.copyToTrash(context, screenshot) ?: return null
        _trashEntries.value = TrashManager.loadIndex(context)
        val pi = TrashManager.buildDeletePendingIntent(context, listOf(screenshot.uri))
        if (pi == null) {
            // 无法删除原图（Android 10 以下）：撤销副本，返回 null
            TrashManager.cancelTrash(context, entry.mediaId)
            _trashEntries.value = TrashManager.loadIndex(context)
        }
        return pi
    }

    /** 系统确认删除成功：从截图列表移除 */
    fun onTrashConfirmed(context: Context, mediaId: Long) {
        TrashManager.confirmDeleted(context, mediaId)
        _screenshots.value = _screenshots.value.filterNot { it.id == mediaId }
        _trashEntries.value = TrashManager.loadIndex(context)
    }

    /** 用户取消删除：撤销回收站副本 */
    fun onTrashCancelled(context: Context, mediaId: Long) {
        TrashManager.cancelTrash(context, mediaId)
        _trashEntries.value = TrashManager.loadIndex(context)
    }

    /** 一键还原 */
    suspend fun restore(context: Context, entry: TrashEntry): Boolean {
        val ok = TrashManager.restore(context, entry)
        _trashEntries.value = TrashManager.loadIndex(context)
        if (ok) refresh(context)
        return ok
    }

    /** 永久删除回收站条目 */
    suspend fun purgeTrash(context: Context, entry: TrashEntry) {
        TrashManager.purge(context, entry)
        _trashEntries.value = TrashManager.loadIndex(context)
    }

    /** 清空回收站 */
    suspend fun clearTrash(context: Context) {
        TrashManager.purgeAll(context)
        _trashEntries.value = emptyList()
    }

    /** 永久删除（不经回收站）：需系统确认 */
    fun buildDeleteForeverIntent(context: Context, screenshots: List<Screenshot>): PendingIntent? =
        TrashManager.buildDeletePendingIntent(context, screenshots.map { it.uri })

    fun onDeleteForeverConfirmed(context: Context, ids: List<Long>) {
        _screenshots.value = _screenshots.value.filterNot { it.id in ids }
        metaMap = metaMap.filterKeys { it !in ids }
        saveMeta(context)
    }

    companion object {
        val instance: ScreenshotRepository by lazy { ScreenshotRepository() }
    }
}
