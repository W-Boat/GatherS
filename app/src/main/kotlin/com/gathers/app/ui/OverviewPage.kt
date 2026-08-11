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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gathers.app.data.RuleEngine
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.util.formatRelative
import com.gathers.app.util.formatSize
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 首页概览：统计卡片 + Top 应用 + 清理建议 */
@Composable
fun OverviewPage(
    padding: PaddingValues,
    screenshots: List<Screenshot>,
    onOpenScreenshot: (Long) -> Unit,
) {
    val active = screenshots.filter { !it.inTrash }
    val totalSize = active.sumOf { it.sizeBytes }
    val monthAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
    val monthNew = active.count { it.takenAt >= monthAgo }
    val repo = remember { ScreenshotRepository.instance }
    val rules by repo.rules.collectAsState()
    val suggestions = RuleEngine.suggestCleanup(active, rules)
    val topApps = active
        .groupingBy { it.sourceApp.ifBlank { "未知" } }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)
    val maxApp = topApps.maxOfOrNull { it.value } ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            TopAppBar(
                title = "概览",
                largeTitle = "概览",
                subtitle = "${active.size} 张截图 · ${formatSize(totalSize)}",
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "截图总数",
                    value = active.size.toString(),
                    subtitle = "本月新增 ${monthNew}",
                    modifier = Modifier.weight(1f),
                    icon = {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Image,
                            contentDescription = null,
                        )
                    },
                )
                StatCard(
                    title = "存储占用",
                    value = formatSize(totalSize),
                    subtitle = "清理可释放空间",
                    modifier = Modifier.weight(1f),
                    icon = {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Folder,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "高价值已锁定",
                    value = active.count { it.isProtected || it.isFavorite }.toString(),
                    subtitle = "清理时自动跳过",
                    modifier = Modifier.weight(1f),
                    icon = {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Lock,
                            contentDescription = null,
                        )
                    },
                )
                StatCard(
                    title = "建议清理",
                    value = suggestions.size.toString(),
                    subtitle = "低价值 / 超期截图",
                    modifier = Modifier.weight(1f),
                    icon = {
                        androidx.compose.foundation.Image(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = null,
                        )
                    },
                )
            }
        }

        if (topApps.isNotEmpty()) {
            item { SectionTitle(text = "Top 截图来源") }
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    topApps.forEach { (app, count) ->
                        ProportionalBar(
                            label = app,
                            value = count,
                            maxValue = maxApp,
                            valueText = "$count",
                        )
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item { SectionTitle(text = "清理建议") }
            items(suggestions.take(8), key = { it.id }) { shot ->
                CleanupSuggestionCard(shot = shot, onClick = { onOpenScreenshot(shot.id) })
            }
        } else if (active.isEmpty()) {
            item {
                EmptyState(
                    title = "暂无截图",
                    subtitle = "截图后回到应用即可看到新内容",
                )
            }
        }
    }
}

@Composable
private fun CleanupSuggestionCard(
    shot: Screenshot,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shot.summary.ifBlank { shot.displayName },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${shot.sourceApp} · ${formatRelative(shot.takenAt)} · ${formatSize(shot.sizeBytes)}",
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (shot.blurScore >= 70) "模糊" else "低价值",
                color = MiuixTheme.colorScheme.error,
            )
        }
    }
}
