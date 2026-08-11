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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.util.formatRelative
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 截图库：搜索 / 筛选 / 网格多选 / 回收站与删除 */
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
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(listOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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

    val filtered = remember(screenshots, query, filterApp, filterTag, filterLowValue) {
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
            qOk && appOk && tagOk && lowOk
        }
    }

    val topApps = remember(screenshots) {
        screenshots.groupingBy { it.sourceApp.ifBlank { "未知" } }.eachCount()
            .entries.sortedByDescending { it.value }.take(6).map { it.key }
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

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (!selectionMode) {
            TopAppBar(
                title = "图库",
                largeTitle = "图库",
                subtitle = "${filtered.size} 张截图${if (filterLowValue) " · 低价值筛选" else ""}",
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
            // 搜索框
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MiuixIcons.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = MiuixTheme.colorScheme.onSurface,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        ),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜索文件名 / 摘要 / 应用",
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                                )
                            }
                            inner()
                        },
                    )
                }
            }
            // 筛选 chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(text = "全部", selected = filterApp == null && filterTag == null && !filterLowValue) {
                    filterApp = null; filterTag = null; filterLowValue = false
                }
                topApps.forEach { app ->
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
                subtitle = if (loading) "" else "试试调整筛选条件或下拉刷新",
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
}

@Composable
private fun GridItem(
    shot: Screenshot,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
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
