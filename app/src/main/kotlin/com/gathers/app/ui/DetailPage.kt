package com.gathers.app.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.util.formatDate
import com.gathers.app.util.formatSize
import com.gathers.app.util.formatTime
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 截图详情：大图预览、元数据、标签、操作与备注 */
@Composable
fun DetailPage(
    screenshot: Screenshot,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val repo = remember { ScreenshotRepository.instance }
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNoteEditor by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(screenshot.userNote) }
    var computedBlur by remember { mutableStateOf(screenshot.blurScore) }

    // 进入页面时若未计算过模糊度则计算一次
    LaunchedEffect(screenshot.id) {
        if (screenshot.blurScore < 0) {
            computedBlur = repo.computeBlur(context, screenshot)
        }
    }

    var pendingDelete by remember { mutableStateOf<Screenshot?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val shot = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && shot != null) {
            repo.onDeleteForeverConfirmed(context, listOf(shot.id))
            scope.launch { snackbarHostState.showSnackbar("已永久删除") }
            onBack()
        }
    }

    val trashLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val shot = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && shot != null) {
            repo.onTrashConfirmed(context, shot.id)
            scope.launch { snackbarHostState.showSnackbar("已移入回收站") }
            onBack()
        } else {
            scope.launch { snackbarHostState.showSnackbar("已取消") }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            SmallTopAppBar(
                title = screenshot.displayName,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        repo.toggleFavorite(context, screenshot)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (screenshot.isFavorite) "已取消收藏" else "已收藏（高价值保护）",
                            )
                        }
                    }) {
                        Icon(
                            imageVector = if (screenshot.isFavorite) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
                            contentDescription = "收藏",
                        )
                    }
                    IconButton(onClick = {
                        repo.toggleProtected(context, screenshot)
                    }) {
                        Icon(
                            imageVector = if (screenshot.isProtected) MiuixIcons.Lock else MiuixIcons.Unlock,
                            contentDescription = "锁定保护",
                        )
                    }
                },
            )
        }

        item {
            // 大图预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp))
                    .aspectRatio(0.78f),
                contentAlignment = Alignment.Center,
            ) {
                ThumbnailImage(
                    uri = screenshot.uri,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = screenshot.displayName,
                    maxDim = 1600,
                )
            }
        }

        item {
            SmallTitle(text = "摘要")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(text = screenshot.summary.ifBlank { "暂无摘要" })
                if (screenshot.userNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "备注：${screenshot.userNote}",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
        }

        item {
            SmallTitle(text = "标签")
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TagChip(text = screenshot.sourceApp.ifBlank { "未知应用" }, highlight = true)
                screenshot.contentTags.forEach { TagChip(text = it) }
                screenshot.stateTags.forEach { TagChip(text = it) }
                screenshot.visualTags.forEach { TagChip(text = it) }
                if (computedBlur >= 70) TagChip(text = "模糊 $computedBlur", highlight = true)
            }
        }

        item {
            SmallTitle(text = "元数据")
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                InfoRow(label = "来源应用", value = screenshot.sourceApp.ifBlank { "未知" })
                InfoRow(label = "截图时间", value = "${formatDate(screenshot.takenAt)} ${formatTime(screenshot.takenAt)}")
                InfoRow(label = "分辨率", value = if (screenshot.width > 0) "${screenshot.width} × ${screenshot.height}" else "未知")
                InfoRow(label = "文件大小", value = formatSize(screenshot.sizeBytes))
                InfoRow(label = "存储路径", value = screenshot.relativePath)
                InfoRow(label = "文件名", value = screenshot.displayName)
                if (computedBlur >= 0) {
                    InfoRow(label = "清晰度", value = if (computedBlur < 40) "清晰" else if (computedBlur < 70) "一般" else "模糊（$computedBlur）")
                }
            }
        }

        item {
            SmallTitle(text = "操作")
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { showNoteEditor = true }, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = MiuixIcons.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("备注")
                }
                Button(onClick = {
                    scope.launch {
                        val pi = repo.moveToTrash(context, screenshot)
                        if (pi == null) {
                            snackbarHostState.showSnackbar("清理功能需要 Android 10 及以上")
                        } else {
                            pendingDelete = screenshot
                            trashLauncher.launch(IntentSenderRequest.Builder(pi).build())
                        }
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = MiuixIcons.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("回收站")
                }
                Button(onClick = { showDeleteConfirm = true }, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = MiuixIcons.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("删除")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        OverlayDialog(
            show = showDeleteConfirm,
            title = "永久删除这张截图？",
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
                        onClick = {
                            showDeleteConfirm = false
                            val pi = repo.buildDeleteForeverIntent(context, listOf(screenshot))
                            if (pi == null) {
                                scope.launch { snackbarHostState.showSnackbar("删除功能需要 Android 10 及以上") }
                            } else {
                                pendingDelete = screenshot
                                deleteLauncher.launch(IntentSenderRequest.Builder(pi).build())
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )
    }

    if (showNoteEditor) {
        OverlayDialog(
            show = showNoteEditor,
            title = "编辑备注",
            summary = "为这张截图添加自定义备注",
            onDismissRequest = { showNoteEditor = false },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    BasicTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = MiuixTheme.colorScheme.onSurface),
                        decorationBox = { inner ->
                            if (noteText.isEmpty()) {
                                Text(
                                    text = "例如：本月房租账单",
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                )
                            }
                            inner()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        text = "取消",
                        onClick = { showNoteEditor = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        text = "保存",
                        onClick = {
                            repo.setNote(context, screenshot, noteText.trim())
                            showNoteEditor = false
                            scope.launch { snackbarHostState.showSnackbar("备注已保存") }
                        },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )
    }
}
