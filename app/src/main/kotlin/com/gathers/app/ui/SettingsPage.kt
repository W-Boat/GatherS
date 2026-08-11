package com.gathers.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.data.TrashEntry
import com.gathers.app.util.formatMonthDayTime
import com.gathers.app.util.formatSize
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 设置与回收站：智能规则开关、回收站管理、关于 */
@Composable
fun SettingsPage(
    padding: PaddingValues,
    trashEntries: List<TrashEntry>,
    screenshots: List<Screenshot>,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { ScreenshotRepository.instance }
    val scope = rememberCoroutineScope()
    val rules by repo.rules.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            TopAppBar(
                title = "设置",
                largeTitle = "设置",
                subtitle = "回收站 · 智能规则 · 关于",
            )
        }

        item { SectionTitle(text = "回收站") }
        item {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(
                    text = if (trashEntries.isEmpty()) {
                        "回收站为空。清理的截图会先暂存于此，保留 30 天。"
                    } else {
                        "共 ${trashEntries.size} 项 · ${formatSize(trashEntries.sumOf { it.sizeBytes })} · 保留 30 天自动过期"
                    },
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                if (trashEntries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            text = "清理过期（>30天）",
                            onClick = {
                                scope.launch {
                                    expireTrashEntries(context, snackbarHostState)
                                }
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            text = "清空回收站",
                            onClick = { showClearConfirm = true },
                        )
                    }
                }
            }
        }
        items(trashEntries, key = { it.mediaId }) { entry ->
            TrashEntryRow(
                entry = entry,
                onRestore = {
                    scope.launch {
                        val ok = repo.restore(context, entry)
                        snackbarHostState.showSnackbar(
                            if (ok) "已还原到原相册目录" else "还原失败（需 Android 10 及以上）",
                        )
                    }
                },
                onPurge = {
                    scope.launch {
                        repo.purgeTrash(context, entry)
                        snackbarHostState.showSnackbar("已永久删除")
                    }
                },
            )
        }

        item { SectionTitle(text = "智能规则") }
        items(rules, key = { it.id }) { rule ->
            SwitchPreference(
                checked = rule.enabled,
                onCheckedChange = { repo.toggleRule(context, rule.id, it) },
                title = rule.name,
                summary = rule.description,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        item { SectionTitle(text = "关于") }
        item {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(text = "GatherS · 截图智能管家")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "基于 Miuix 组件库构建。全部识别与归纳均在本地完成，不上传任何图片。",
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = MiuixIcons.Refresh, contentDescription = "重新扫描")
                    }
                    Text(
                        text = "重新扫描截图",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
        }
    }

    if (showClearConfirm) {
        OverlayDialog(
            show = showClearConfirm,
            title = "清空回收站？",
            summary = "回收站中的 ${trashEntries.size} 项将被永久删除，无法还原",
            onDismissRequest = { showClearConfirm = false },
            content = {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        text = "取消",
                        onClick = { showClearConfirm = false },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(
                        text = "清空",
                        onClick = {
                            showClearConfirm = false
                            scope.launch {
                                repo.clearTrash(context)
                                snackbarHostState.showSnackbar("回收站已清空")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            },
        )
    }
}

/** 清理超过 30 天的回收站条目 */
private suspend fun expireTrashEntries(
    context: android.content.Context,
    snackbarHostState: SnackbarHostState,
) {
    val repo = ScreenshotRepository.instance
    val expired = com.gathers.app.data.TrashManager.expired(context)
    if (expired.isEmpty()) {
        snackbarHostState.showSnackbar("没有过期的回收站条目")
        return
    }
    expired.forEach { repo.purgeTrash(context, it) }
    snackbarHostState.showSnackbar("已清理 ${expired.size} 条过期记录")
}

@Composable
private fun TrashEntryRow(
    entry: TrashEntry,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp)
            .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatMonthDayTime(entry.trashedAt)} · ${formatSize(entry.sizeBytes)}",
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRestore) {
                Icon(imageVector = MiuixIcons.Undo, contentDescription = "还原")
            }
            IconButton(onClick = onPurge) {
                Icon(imageVector = MiuixIcons.Delete, contentDescription = "永久删除")
            }
        }
    }
}
