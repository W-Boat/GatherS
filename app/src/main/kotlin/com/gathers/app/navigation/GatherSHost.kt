package com.gathers.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import com.gathers.app.feature.gallery.GalleryPage
import com.gathers.app.feature.overview.OverviewPage
import com.gathers.app.feature.report.ReportPage
import com.gathers.app.feature.settings.SettingsPage
import com.gathers.app.ui.theme.GatherSTheme

/**
 * 导航路由定义
 */
sealed class Route {
    data object Overview : Route()
    data object Gallery : Route()
    data object Report : Route()
    data object Settings : Route()
}

/**
 * 底部导航栏标签
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Route,
)

private val bottomNavItems = listOf(
    BottomNavItem("全息感知", MiuixIcons.Folder, Route.Overview),
    BottomNavItem("图库", MiuixIcons.File, Route.Gallery),
    BottomNavItem("报表", MiuixIcons.All, Route.Report),
    BottomNavItem("设置", MiuixIcons.Settings, Route.Settings),
)

/**
 * GatherS 导航宿主 — 管理底部导航 + 页面切换
 */
@Composable
fun GatherSHost() {
    GatherSTheme {
        var currentRoute by remember { mutableStateOf<Route>(Route.Overview) }

        Scaffold(
            bottomBar = {
                FloatingNavigationBar {
                    bottomNavItems.forEach { item ->
                        FloatingNavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { currentRoute = item.route },
                            icon = item.icon,
                            label = item.label,
                        )
                    }
                }
            },
        ) { _ ->
            // 页面切换
            when (currentRoute) {
                Route.Overview -> OverviewPage()
                Route.Gallery -> GalleryPage()
                Route.Report -> ReportPage()
                Route.Settings -> SettingsPage()
            }
        }
    }
}