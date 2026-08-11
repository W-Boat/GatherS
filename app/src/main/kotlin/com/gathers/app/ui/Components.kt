package com.gathers.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gathers.app.data.MetaExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 异步加载缩略图（内置 BitmapFactory 采样解码，无第三方库） */
@Composable
fun ThumbnailImage(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    maxDim: Int = 160,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            MetaExtractor.decodeSampled(context, uri, maxDim = maxDim)
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(MiuixTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "…",
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
            )
        }
    }
}

/** 标签小胶囊 */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    val bg = if (highlight) MiuixTheme.colorScheme.primaryContainer else MiuixTheme.colorScheme.surfaceContainer
    val fg = if (highlight) MiuixTheme.colorScheme.onPrimaryContainer else MiuixTheme.colorScheme.onSurfaceSecondary
    Text(
        text = text,
        modifier = modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = fg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 概览统计卡片 */
@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (icon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(contentAlignment = Alignment.Center) { icon() }
            }
        }
    }
}

/** 章节标题 */
@Composable
fun SectionTitle(text: String) {
    SmallTitle(text = text)
}

/** 空状态占位 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, fontWeight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 文件信息行 */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 筛选/选项 Chip（可选中） */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = when {
        selected && highlight -> MiuixTheme.colorScheme.errorContainer
        selected -> MiuixTheme.colorScheme.primaryContainer
        else -> MiuixTheme.colorScheme.surfaceContainer
    }
    val fg = when {
        selected && highlight -> MiuixTheme.colorScheme.onErrorContainer
        selected -> MiuixTheme.colorScheme.onPrimaryContainer
        else -> MiuixTheme.colorScheme.onSurfaceSecondary
    }
    Text(
        text = text,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onClick, onLongClick = null)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = fg,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
    )
}

/** 带比例的柱状条（报表用） */
@Composable
fun ProportionalBar(
    label: String,
    value: Int,
    maxValue: Int,
    valueText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.width(52.dp),
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp)),
        ) {
            val fraction = if (maxValue <= 0) 0f else value.toFloat() / maxValue
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(MiuixTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = valueText, color = MiuixTheme.colorScheme.onSurface)
    }
}
