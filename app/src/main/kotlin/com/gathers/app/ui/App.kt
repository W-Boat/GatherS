package com.gathers.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gathers.app.R
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.ui.theme.GatherSTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun hasMediaPermission(context: Context): Boolean {
    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun GatherSApp() {
    GatherSTheme {
        val context = LocalContext.current
        val repo = remember { ScreenshotRepository.instance }
        var hasPermission by remember { mutableStateOf(hasMediaPermission(context)) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            hasPermission = result.values.any { it }
        }
        LaunchedEffect(hasPermission) {
            if (hasPermission) repo.refresh(context)
        }
        if (!hasPermission) {
            PermissionScreen(
                onRequest = { permissionLauncher.launch(requiredPermissions()) },
            )
        } else {
            MainScreen(repo = repo)
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.title1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "需要读取媒体库权限以扫描你的截图",
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("授予权限")
        }
    }
}

private val navItems = listOf(
    NavItem("概览", MiuixIcons.Home),
    NavItem("图库", MiuixIcons.Photos),
    NavItem("报表", MiuixIcons.Report),
    NavItem("设置", MiuixIcons.Settings),
)

@Composable
private fun MainScreen(repo: ScreenshotRepository) {
    val context = LocalContext.current
    val screenshots by repo.screenshots.collectAsState()
    val trashEntries by repo.trashEntries.collectAsState()
    val loading by repo.loading.collectAsState()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    val detail = screenshots.find { it.id == detailId }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (detail != null) {
        DetailPage(
            screenshot = detail,
            onBack = { detailId = null },
            snackbarHostState = snackbarHostState,
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(state = snackbarHostState)
        },
    ) { padding ->
        when (tab) {
            0 -> OverviewPage(
                padding = padding,
                screenshots = screenshots,
                onOpenScreenshot = { detailId = it },
            )
            1 -> GalleryPage(
                padding = padding,
                screenshots = screenshots,
                loading = loading,
                snackbarHostState = snackbarHostState,
                onOpenScreenshot = { detailId = it },
            )
            2 -> ReportPage(
                padding = padding,
                screenshots = screenshots,
                snackbarHostState = snackbarHostState,
            )
            3 -> SettingsPage(
                padding = padding,
                trashEntries = trashEntries,
                screenshots = screenshots,
                snackbarHostState = snackbarHostState,
                onRefresh = { scope.launch { repo.refresh(context) } },
            )
        }
    }
}
