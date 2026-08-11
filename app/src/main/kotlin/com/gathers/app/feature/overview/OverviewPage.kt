package com.gathers.app.feature.overview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 概览页面 — 全息感知·截图总览
 */
@Composable
fun OverviewPage(onBack: () -> Unit = {}) {
    val backEventState = remember { rememberNavigationEventState() }

    NavigationBackHandler(backEventState, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "全息感知",
            )
        },
    ) {
        // TODO: 阶段 3 实现 — 截图列表、统计卡片、快速筛选
    }
}