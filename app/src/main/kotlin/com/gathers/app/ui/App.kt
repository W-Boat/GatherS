package com.gathers.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gathers.app.R
import com.gathers.app.data.ScreenshotRepository
import com.gathers.app.ui.theme.GatherSTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
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
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            // Android 13+：完整访问或"选择照片"部分授权任一即可
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED)
        }
        else -> context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

private fun requiredPermissions(): Array<String> =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
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

        // 从系统设置返回时重新检查权限
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermission = hasMediaPermission(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

/** Miuix 风格权限引导页 */
@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(MiuixTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Photos,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = context.getString(R.string.app_name),
            style = MiuixTheme.textStyles.title1,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "将散乱的截图转化为结构化信息资产",
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            PermissionItem(
                icon = MiuixIcons.Photos,
                text = "自动扫描相册中的全部截图",
            )
            PermissionItem(
                icon = MiuixIcons.ScreenCapture,
                text = "识别来源应用、内容标签与智能摘要",
            )
            PermissionItem(
                icon = MiuixIcons.Lock,
                text = "全部识别在本地完成，图片不会上传",
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequest) {
            Text("授予权限")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "拒绝后可在系统设置中重新开启",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun PermissionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MiuixTheme.colorScheme.surfaceContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, modifier = Modifier.weight(1f))
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

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Crossfade(
        targetState = detailId,
        animationSpec = tween(220),
        label = "detailTransition",
    ) { currentDetailId ->
        val detail = screenshots.find { it.id == currentDetailId }
        if (detail != null) {
            DetailPage(
                screenshot = detail,
                onBack = { detailId = null },
                snackbarHostState = snackbarHostState,
            )
        } else {
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
                Crossfade(
                    targetState = tab,
                    animationSpec = tween(200),
                    label = "tabTransition",
                ) { t ->
                    when (t) {
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
                        else -> SettingsPage(
                            padding = padding,
                            trashEntries = trashEntries,
                            screenshots = screenshots,
                            snackbarHostState = snackbarHostState,
                            onRefresh = { scope.launch { repo.refresh(context) } },
                        )
                    }
                }
            }
        }
    }
}
