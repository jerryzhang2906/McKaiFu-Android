@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.mckaifu.app.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mckaifu.app.data.model.CoreType
import com.mckaifu.app.data.model.CoreVersion
import com.mckaifu.app.data.model.DownloadProgress
import com.mckaifu.app.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ZalithCardBorder,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium)
            .background(
                Brush.verticalGradient(
                    listOf(
                        ZalithCard.copy(alpha = 0.6f),
                        ZalithCard.copy(alpha = 0.4f)
                    )
                ),
                MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: Brush = GradientButton,
    icon: ImageVector? = null,
    loading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = ZalithSurfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) gradient
                    else Brush.linearGradient(listOf(ZalithSurfaceVariant, ZalithSurfaceVariant)),
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                } else if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        action?.invoke()
    }
}

@Composable
fun LoadingOverlay(isLoading: Boolean, modifier: Modifier = Modifier) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ZalithPrimary)
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String = "确认删除",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZalithSurface,
        shape = MaterialTheme.shapes.medium,
        icon = {
            Icon(Icons.Filled.Warning, null,
                tint = ServerError,
                modifier = Modifier.size(32.dp))
        },
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Text(message, color = TextSecondary)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ServerError)
            ) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextSecondary)
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String, labelColor: Color = TextSecondary, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = valueColor)
    }
}

@Composable
fun StatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    animated: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color by animateColorAsState(
        if (isOnline) ServerOnline else ServerOffline,
        label = "dotColor"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (isOnline && animated) {
            Box(
                modifier = Modifier
                    .size(size * 2.5f)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = pulseAlpha * 0.3f))
            )
        }
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Filled.Info,
    title: String,
    subtitle: String = "",
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(72.dp),
            tint = TextSecondary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary
        )
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.6f)
            )
        }
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun CoreDownloadDialog(
    coreType: CoreType,
    versions: List<CoreVersion>,
    downloadProgress: DownloadProgress,
    onDismiss: () -> Unit,
    onDownload: (CoreVersion) -> Unit
) {
    if (!downloadProgress.isDownloading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = ZalithCard,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("选择版本", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(versions) { version ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ZalithSurfaceVariant),
                            onClick = { onDownload(version) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        version.mcVersion,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        if (version.isRecommended) "推荐版本" else "核心: $coreType",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    Icons.Filled.Download, null,
                                    tint = ZalithPrimary, modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = TextSecondary)
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = {},
            containerColor = ZalithCard,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("正在下载核心", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = downloadProgress.progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = ZalithPrimary,
                        trackColor = ZalithCardBorder
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        downloadProgress.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        "${(downloadProgress.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = ZalithPrimary
                    )
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}
