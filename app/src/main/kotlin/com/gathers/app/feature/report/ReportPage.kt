package com.gathers.app.feature.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 报表页面 — 可视化报告
 */
@Composable
fun ReportPage(onBack: () -> Unit = {}) {
    val backEventState = remember { rememberNavigationEventState() }

    NavigationBackHandler(backEventState, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "报表",
            )
        },
    ) {
        // TODO: 阶段 4 实现 — 统计图表、来源分布、趋势图、导出
    }
}