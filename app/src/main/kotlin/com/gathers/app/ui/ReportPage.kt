package com.gathers.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gathers.app.data.ReportGenerator
import com.gathers.app.data.ReportRange
import com.gathers.app.data.Screenshot
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.util.formatSize
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 可视化截图报告：范围切换、趋势、Top 应用、时段热力、Markdown 导出 */
@Composable
fun ReportPage(
    padding: PaddingValues,
    screenshots: List<Screenshot>,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ScreenshotRepository.instance }
    val rules by repo.rules.collectAsState()

    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    val range = ReportRange.entries[rangeIndex.coerceIn(0, ReportRange.entries.size - 1)]
    val data = remember(screenshots, range, rules) {
        ReportGenerator.generate(screenshots, range, rules)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            TopAppBar(
                title = "报表",
                largeTitle = "报表",
                subtitle = data.title,
                actions = {
                    top.yukonga.miuix.kmp.basic.IconButton(onClick = {
                        val md = ReportGenerator.toMarkdown(data)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/markdown"
                            putExtra(Intent.EXTRA_SUBJECT, data.title)
                            putExtra(Intent.EXTRA_TEXT, md)
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "导出报告")) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar("没有可用的分享应用") } }
                    }) {
                        Icon(imageVector = MiuixIcons.Share, contentDescription = "导出")
                    }
                },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(text = "本周", selected = range == ReportRange.WEEK) { rangeIndex = 0 }
                FilterChip(text = "本月", selected = range == ReportRange.MONTH) { rangeIndex = 1 }
                FilterChip(text = "全部", selected = range == ReportRange.ALL) { rangeIndex = 2 }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "截图总数",
                    value = data.totalCount.toString(),
                    subtitle = "近 14 天新增 ${data.newCount}",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "存储占用",
                    value = formatSize(data.totalSize),
                    subtitle = "财务/凭证 ${data.financeCount} 张",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    title = "高价值锁定",
                    value = data.protectedCount.toString(),
                    subtitle = "清理自动跳过",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "建议清理",
                    value = data.lowValueCount.toString(),
                    subtitle = "低价值/超期",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item { SectionTitle(text = "近 14 天趋势") }
        item {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                val max = data.dailyTrend.maxOfOrNull { it.second } ?: 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    data.dailyTrend.forEach { (day, count) ->
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                        ) {
                            Text(
                                text = if (count > 0) count.toString() else "",
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(if (max <= 0) 0.02f else count.toFloat() / max)
                                    .background(
                                        MiuixTheme.colorScheme.primary,
                                        RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                                    ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = day.substringAfter('-'),
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle(text = "Top 截图来源") }
        item {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                if (data.topApps.isEmpty()) {
                    Text(
                        text = "暂无数据",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    val max = data.topApps.maxOf { it.second }
                    data.topApps.forEach { (app, count) ->
                        ProportionalBar(
                            label = app,
                            value = count,
                            maxValue = max,
                            valueText = "$count",
                        )
                    }
                }
            }
        }

        item { SectionTitle(text = "最佳截屏时段") }
        item {
            Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                val maxH = data.hourlyHeat.maxOrNull() ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    data.hourlyHeat.forEachIndexed { h, c ->
                        val alpha = if (maxH <= 0) 0.08f else 0.15f + 0.85f * c / maxH
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .background(
                                    MiuixTheme.colorScheme.primary.copy(alpha = alpha.coerceIn(0.08f, 1f)),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("0时", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("12时", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("23时", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
                val hot = data.hourlyHeat.mapIndexed { h, c -> h to c }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .take(3)
                if (hot.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = hot.joinToString("  ") { (h, c) -> "%02d:00 ×%d".format(h, c) },
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    val md = ReportGenerator.toMarkdown(data)
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, data.title)
                        putExtra(Intent.EXTRA_TEXT, md)
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "导出报告")) }
                        .onFailure { scope.launch { snackbarHostState.showSnackbar("没有可用的分享应用") } }
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
            ) {
                Icon(imageVector = MiuixIcons.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出 Markdown 报告")
            }
        }
    }
}
