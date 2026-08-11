package com.gathers.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** GatherS 主题：跟随系统明暗，使用稳定默认配色 */
@Composable
fun GatherSTheme(content: @Composable () -> Unit) {
    val controller = remember {
        ThemeController(
            colorSchemeMode = ColorSchemeMode.System,
            keyColor = Color(0xFF3482FF),
        )
    }
    MiuixTheme(
        controller = controller,
        content = content,
    )
}
