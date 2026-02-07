package com.waliahimanshu.wealthmate.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Data class for a single pie chart slice
 */
data class PieSlice(
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * Predefined color palette for charts
 */
object ChartColors {
    val palette = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF795548), // Brown
        Color(0xFFE91E63), // Pink
        Color(0xFF607D8B), // Blue Grey
    )

    fun getColor(index: Int): Color = palette[index % palette.size]
}

/**
 * Reusable Pie Chart composable using Canvas
 */
@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    showLegend: Boolean = true,
    strokeWidth: Float = 0f // 0 = filled, > 0 = donut style
) {
    if (slices.isEmpty() || slices.all { it.value == 0.0 }) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No data to display", color = Color.Gray)
        }
        return
    }

    val total = slices.sumOf { it.value }
    val validSlices = slices.filter { it.value > 0 }

    Column(modifier = modifier) {
        // Pie chart canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(16.dp)
        ) {
            val canvasSize = min(size.width, size.height)
            val radius = canvasSize / 2
            val center = Offset(size.width / 2, size.height / 2)

            var startAngle = -90f // Start from top

            validSlices.forEach { slice ->
                val sweepAngle = (slice.value / total * 360f).toFloat()

                if (strokeWidth > 0) {
                    // Donut style
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius + strokeWidth / 2, center.y - radius + strokeWidth / 2),
                        size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    // Filled pie
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(canvasSize, canvasSize)
                    )
                }

                startAngle += sweepAngle
            }
        }

        // Legend
        if (showLegend) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                validSlices.forEach { slice ->
                    val percentage = (slice.value / total * 100).roundToInt()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Text(
                            text = "${slice.label} ($percentage%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pie chart wrapped in a card with title
 */
@Composable
fun PieChartCard(
    title: String,
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            PieChart(
                slices = slices,
                modifier = Modifier.fillMaxWidth(),
                strokeWidth = 40f // Donut style looks cleaner
            )
        }
    }
}

/**
 * Compact pie chart for dashboard (smaller, no legend)
 */
@Composable
fun CompactPieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty() || slices.all { it.value == 0.0 }) {
        Box(
            modifier = modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("--", color = Color.Gray)
        }
        return
    }

    val total = slices.sumOf { it.value }
    val validSlices = slices.filter { it.value > 0 }

    Canvas(modifier = modifier.size(80.dp)) {
        val canvasSize = min(size.width, size.height)
        val strokeWidth = 16f
        val radius = (canvasSize - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f

        validSlices.forEach { slice ->
            val sweepAngle = (slice.value / total * 360f).toFloat()

            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )

            startAngle += sweepAngle
        }
    }
}
