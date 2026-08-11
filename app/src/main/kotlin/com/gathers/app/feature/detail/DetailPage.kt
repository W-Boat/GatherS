package com.gathers.app.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 详情页面 — 单张截图详情与操作
 */
@Composable
fun DetailPage(onBack: () -> Unit = {}) {
    val backEventState = remember { rememberNavigationEventState() }

    NavigationBackHandler(backEventState, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "详情",
            )
        },
    ) {
        // TODO: 阶段 3 实现 — 图片预览、标签编辑、元信息展示、操作按钮
    }
}