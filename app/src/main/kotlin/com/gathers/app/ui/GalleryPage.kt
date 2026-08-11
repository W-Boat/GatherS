package com.gathers.app.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 时间范围筛选 */
enum class TimeFilter(val label: String, val days: Long?) {
    ALL("全部", null),
    TODAY("今天", 1),
    WEEK("近7天", 7),
    MONTH("近30天", 30),
    YEAR("今年", 365),
}

/** 清晰度筛选 */
enum class ClarityFilter(val label: String) {
    ALL("全部"),
    CLEAR("清晰"),
    BLUR("模糊"),
}

/** 截图库：搜索 / 组合筛选 / 网格多选 / 回收站与删除 */
@Composable
fun GalleryPage(
    padding: PaddingValues,
    screenshots: List<Screenshot>,
    loading: Boolean,
    snackbarHostState: SnackbarHostState,
    onOpenScreenshot: (Long) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { ScreenshotRepository.instance }
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var filterApp by rememberSaveable { mutableStateOf<String?>(null) }
    var filterTag by rememberSaveable { mutableStateOf<String?>(null) }
    var filterLowValue by rememberSaveable { mutableStateOf(false) }
    var timeFilter by rememberSaveable { mutableStateOf(TimeFilter.ALL) }
    var clarityFilter by rememberSaveable { mutableStateOf(ClarityFilter.ALL) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showFilterPanel by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(true) }

    var pendingTrashIds by remember { mutableStateOf(listOf<Long>()) }
    var pendingDeleteIds by remember { mutableStateOf(listOf<Long>()) }

    fun exitSelection() {
        selectionMode = false
        selectedIds = emptyList()
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val ids = pendingTrashIds
        pendingTrashIds = emptyList()
        if (result.resultCode == Activity.RESULT_OK) {
            ids.forEach { repo.onTrashConfirmed(context, it) }
            scope.launch { snackbarHostState.showSnackbar("已移入回收站 ${ids.size} 张") }
        } else {
            ids.forEach { repo.onTrashCancelled(context, it) }
            scope.launch { snackbarHostState.showSnackbar("已取消移入回收站") }
        }
        exitSelection()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val ids = pendingDeleteIds
        pendingDeleteIds = emptyList()
        if (result.resultCode == Activity.RESULT_OK) {
            repo.onDeleteForeverConfirmed(context, ids)
            scope.launch { snackbarHostState.showSnackbar("已永久删除 ${ids.size} 张") }
        }
        exitSelection()
    }

    val now = System.currentTimeMillis()
    val filtered = remember(screenshots, query, filterApp, filterTag, filterLowValue, timeFilter, clarityFilter) {
        screenshots.filter { !it.inTrash }.filter { s ->
            val qOk = query.isBlank() ||
                s.displayName.contains(query, ignoreCase = true) ||
                s.summary.contains(query, ignoreCase = true) ||
                s.sourceApp.contains(query, ignoreCase = true)
            val appOk = filterApp == null || s.sourceApp == filterApp
            val tagOk = filterTag == null ||
                s.contentTags.contains(filterTag) ||
                s.stateTags.contains(filterTag) ||
                s.visualTags.contains(filterTag)
            val lowOk = !filterLowValue || s.isLowValue
            val filterDays = timeFilter.days
            val timeOk = filterDays == null || s.takenAt >= now - filterDays * 86_400_000L
            val clarityOk = when (clarityFilter) {
                ClarityFilter.ALL -> true
                ClarityFilter.CLEAR -> s.blurScore < 0 || s.blurScore < 40
                ClarityFilter.BLUR -> s.blurScore >= 40
            }
            qOk && appOk && tagOk && lowOk && timeOk && clarityOk
        }
    }

    val topApps = remember(screenshots) {
        screenshots.groupingBy { it.sourceApp.ifBlank { "未知" } }.eachCount()
            .entries.sortedByDescending { it.value }.take(8).map { it.key }
    }

    fun onTrashClick(shots: List<Screenshot>) {
        scope.launch {
            val pi = repo.moveManyToTrash(context, shots)
            if (pi == null) {
                scope.launch { snackbarHostState.showSnackbar("清理功能需要 Android 10 及以上") }
                exitSelection()
            } else {
                pendingTrashIds = shots.map { it.id }
                trashLauncher.launch(IntentSenderRequest.Builder(pi).build())
            }
        }
    }

    fun onDeleteClick(shots: List<Screenshot>) {
        if (shots.isEmpty()) return
        showDeleteConfirm = true
    }

    fun confirmDelete(shots: List<Screenshot>) {
        showDeleteConfirm = false
        val pi = repo.buildDeleteForeverIntent(context, shots)
        if (pi == null) {
            scope.launch { snackbarHostState.showSnackbar("删除功能需要 Android 10 及以上") }
            exitSelection()
        } else {
            pendingDeleteIds = shots.map { it.id }
            deleteLauncher.launch(IntentSenderRequest.Builder(pi).build())
        }
    }

    val activeFilterCount = listOfNotNull(
        filterApp?.let { "应用" },
        filterTag?.let { "标签" },
        if (filterLowValue) "建议清理" else null,
        if (timeFilter != TimeFilter.ALL) timeFilter.label else null,
        if (clarityFilter != ClarityFilter.ALL) clarityFilter.label else null,
    ).size

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (!selectionMode) {
            TopAppBar(
                title = "图库",
                largeTitle = "图库",
                subtitle = "${filtered.size} 张截图" +
                    if (activeFilterCount > 0) " · ${activeFilterCount} 项筛选" else "",
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            repo.refresh(context)
                            snackbarHostState.showSnackbar("已重新扫描")
                        }
                    }) {
                        Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                    }
                },
            )
            // 搜索框（Miuix InputField）
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                label = "搜索文件名 / 摘要 / 应用",
            )
            // 快捷筛选 chips + 打开筛选面板
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    text = if (activeFilterCount > 0) "筛选（$activeFilterCount）" else "筛选",
                    selected = activeFilterCount > 0,
                    highlight = true,
                ) { showFilterPanel = true }
                topApps.take(4).forEach { app ->
                    FilterChip(text = app, selected = filterApp == app) {
                        filterApp = if (filterApp == app) null else app
                    }
                }
                listOf("财务", "凭证", "聊天", "游戏", "错误").forEach { tag ->
                    FilterChip(text = tag, selected = filterTag == tag) {
                        filterTag = if (filterTag == tag) null else tag
                    }
                }
                FilterChip(text = "建议清理", selected = filterLowValue, highlight = true) {
                    filterLowValue = !filterLowValue
                }
            }
        } else {
            TopAppBar(
                title = "已选 ${selectedIds.size} 张",
                navigationIcon = {
                    IconButton(onClick = { exitSelection() }) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "退出选择")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sel = screenshots.filter { it.id in selectedIds }
                        sel.forEach { repo.toggleFavorite(context, it) }
                        scope.launch { snackbarHostState.showSnackbar("已收藏 ${sel.size} 张") }
                        exitSelection()
                    }) {
                        Icon(imageVector = MiuixIcons.Favorites, contentDescription = "收藏")
                    }
                    IconButton(onClick = {
                        val sel = screenshots.filter { it.id in selectedIds }
                        sel.forEach { repo.toggleProtected(context, it) }
                        scope.launch { snackbarHostState.showSnackbar("已锁定 ${sel.size} 张") }
                        exitSelection()
                    }) {
                        Icon(imageVector = MiuixIcons.Lock, contentDescription = "锁定保护")
                    }
                    IconButton(onClick = {
                        onTrashClick(screenshots.filter { it.id in selectedIds })
                    }) {
                        Icon(imageVector = MiuixIcons.Folder, contentDescription = "移入回收站")
                    }
                    IconButton(onClick = {
                        onDeleteClick(screenshots.filter { it.id in selectedIds })
                    }) {
                        Icon(imageVector = MiuixIcons.Delete, contentDescription = "永久删除")
                    }
                },
            )
        }

        if (filtered.isEmpty()) {
            EmptyState(
                title = if (loading) "正在扫描截图…" else "没有符合条件的截图",
                subtitle = if (loading) "" else "试试调整筛选条件或重新扫描",
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(filtered, key = { it.id }) { shot ->
                    GridItem(
                        modifier = Modifier.animateItem(),
                        shot = shot,
                        selectionMode = selectionMode,
                        selected = shot.id in selectedIds,
                        onClick = {
                            if (selectionMode) {
                                selectedIds = if (shot.id in selectedIds) selectedIds - shot.id else selectedIds + shot.id
                            } else {
                                onOpenScreenshot(shot.id)
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            selectedIds = listOf(shot.id)
                        },
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val sel = screenshots.filter { it.id in selectedIds }
        OverlayDialog(
            show = showDeleteConfirm,
            title = "永久删除 ${sel.size} 张截图？",
            summary = "将直接从相册删除且无法还原，建议先移入回收站",
            onDismissRequest = { showDeleteConfirm = false },
            content = {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        text = "取消",
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        text = "删除",
                        onClick = { confirmDelete(sel) },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )
    }

    if (showFilterPanel) {
        FilterPanel(
            show = showFilterPanel,
            filterApp = filterApp,
            filterTag = filterTag,
            filterLowValue = filterLowValue,
            timeFilter = timeFilter,
            clarityFilter = clarityFilter,
            topApps = topApps,
            onDismiss = { showFilterPanel = false },
            onApply = { app, tag, low, time, clarity ->
                filterApp = app
                filterTag = tag
                filterLowValue = low
                timeFilter = time
                clarityFilter = clarity
                showFilterPanel = false
            },
            onReset = {
                filterApp = null
                filterTag = null
                filterLowValue = false
                timeFilter = TimeFilter.ALL
                clarityFilter = ClarityFilter.ALL
            },
        )
    }
}

/** 组合筛选面板：来源应用 × 时间范围 × 清晰度 × 内容标签 × 低价值 */
@Composable
private fun FilterPanel(
    show: Boolean,
    filterApp: String?,
    filterTag: String?,
    filterLowValue: Boolean,
    timeFilter: TimeFilter,
    clarityFilter: ClarityFilter,
    topApps: List<String>,
    onDismiss: () -> Unit,
    onApply: (String?, String?, Boolean, TimeFilter, ClarityFilter) -> Unit,
    onReset: () -> Unit,
) {
    var app by remember { mutableStateOf(filterApp) }
    var tag by remember { mutableStateOf(filterTag) }
    var low by remember { mutableStateOf(filterLowValue) }
    var time by remember { mutableStateOf(timeFilter) }
    var clarity by remember { mutableStateOf(clarityFilter) }

    OverlayDialog(
        show = show,
        title = "筛选",
        summary = "组合条件筛选截图（同时满足）",
        onDismissRequest = onDismiss,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilterGroup(label = "来源应用") {
                    ChipRow(options = listOf("全部") + topApps, selected = app) {
                        app = if (it == "全部") null else it
                    }
                }
                FilterGroup(label = "时间范围") {
                    TimeFilter.entries.forEach { t ->
                        FilterChip(text = t.label, selected = time == t) { time = t }
                    }
                }
                FilterGroup(label = "清晰度") {
                    ClarityFilter.entries.forEach { c ->
                        FilterChip(text = c.label, selected = clarity == c) { clarity = c }
                    }
                }
                FilterGroup(label = "内容标签") {
                    ChipRow(
                        options = listOf("全部", "财务", "凭证", "聊天", "游戏", "错误", "攻略", "网页"),
                        selected = tag,
                    ) {
                        tag = if (it == "全部") null else it
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "仅显示建议清理",
                        modifier = Modifier.weight(1f),
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                    Switch(checked = low, onCheckedChange = { low = it })
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(text = "重置", onClick = {
                        app = null; tag = null; low = false
                        time = TimeFilter.ALL; clarity = ClarityFilter.ALL
                        onReset()
                    }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        text = "完成",
                        onClick = { onApply(app, tag, low, time, clarity) },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
}

@Composable
private fun FilterGroup(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            fontWeight = FontWeight.Medium,
        )
        content()
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            FilterChip(text = opt, selected = selected == opt || (selected == null && opt == "全部")) {
                onSelect(opt)
            }
        }
    }
}

@Composable
private fun GridItem(
    modifier: Modifier = Modifier,
    shot: Screenshot,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        ThumbnailImage(
            uri = shot.uri,
            modifier = Modifier.fillMaxSize(),
            contentDescription = shot.displayName,
        )
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(22.dp)
                    .background(
                        if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surface.copy(alpha = 0.75f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Text(
                        text = "✓",
                        color = MiuixTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else if (shot.isProtected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(5.dp)
                    .size(20.dp)
                    .background(MiuixTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Lock,
                    contentDescription = "已锁定",
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
