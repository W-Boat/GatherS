package com.gathers.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 设置页面 — 应用配置
 */
@Composable
fun SettingsPage(onBack: () -> Unit = {}) {
    val backEventState = remember { rememberNavigationEventState() }

    NavigationBackHandler(backEventState, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "设置",
            )
        },
    ) {
        // TODO: 阶段 3/5 实现 — 扫描设置、AI 开关、回收站保留期、关于
    }
}