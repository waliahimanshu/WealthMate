package com.waliahimanshu.wealthmate.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waliahimanshu.wealthmate.storage.SyncStatus
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SyncStatusBar(status: SyncStatus) {
    val (color, text) = when (status) {
        is SyncStatus.Idle -> MaterialTheme.colorScheme.outline to ""
        is SyncStatus.Syncing -> MaterialTheme.colorScheme.tertiary to "Syncing..."
        is SyncStatus.NotConfigured -> Color(0xFFFF9800) to "Cloud sync not configured"
        is SyncStatus.Success -> MaterialTheme.colorScheme.primary to status.message
        is SyncStatus.Error -> MaterialTheme.colorScheme.error to status.message
    }

    if (text.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.15f))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
fun DarkModeSwitch(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isDarkMode) "\u263D" else "\u2600\uFE0F",
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle,
            thumbContent = {
                Box(
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (isDarkMode) 360f else 0f,
                        animationSpec = tween(400)
                    )
                    Text(
                        text = if (isDarkMode) "\u263D" else "\u2600\uFE0F",
                        modifier = Modifier.graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        )
    }
}

@Composable
fun TableRow(
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified,
    indent: Boolean = false,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 16.dp else 0.dp, top = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

val LocalShowMode = compositionLocalOf { false }

// Utility functions
fun formatCurrency(amount: Double): String {
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).roundToInt()
    val decimalStr = decimalPart.toString().padStart(2, '0')
    return "£$wholePart.$decimalStr"
}

fun maskAmount(amount: Double): Double {
    if (amount == 0.0) return 0.0
    val magnitude = when {
        abs(amount) >= 100_000 -> 100_000.0
        abs(amount) >= 10_000 -> 10_000.0
        abs(amount) >= 1_000 -> 1_000.0
        abs(amount) >= 100 -> 100.0
        abs(amount) >= 10 -> 10.0
        else -> 1.0
    }
    val hash = abs(amount.toBits().hashCode())
    val factor = 1.0 + (hash % 900) / 100.0
    return magnitude * factor * if (amount < 0) -1.0 else 1.0
}

@Composable
fun displayCurrency(amount: Double): String {
    val masked = LocalShowMode.current
    return if (masked) formatCurrency(maskAmount(amount)) else formatCurrency(amount)
}

fun parseColor(hex: String): Color {
    return try {
        Color(hex.removePrefix("#").toLong(16) or 0xFF000000)
    } catch (e: Exception) {
        Color(0xFF4CAF50)
    }
}
