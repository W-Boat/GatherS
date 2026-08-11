package com.gathers.app.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 图库页面 — 网格浏览截图
 */
@Composable
fun GalleryPage(onBack: () -> Unit = {}) {
    val backEventState = remember { rememberNavigationEventState() }

    NavigationBackHandler(backEventState, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = "图库",
            )
        },
    ) {
        // TODO: 阶段 3 实现 — 网格布局、多选、搜索
    }
}