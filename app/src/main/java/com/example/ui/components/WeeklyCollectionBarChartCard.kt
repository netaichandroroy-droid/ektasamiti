package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeeklyChartBucket
import kotlin.math.ceil

@Composable
fun WeeklyCollectionBarChartCard(
    buckets: List<WeeklyChartBucket>,
    modifier: Modifier = Modifier
) {
    val savingsColor = Color(0xFF0B6638)
    val loanColor = Color(0xFFD97706)
    val gridColor = Color(0xFF94A3B8)

    var selectedWeekIndex by remember { mutableIntStateOf(3) }
    var animateBars by remember { mutableStateOf(false) }

    LaunchedEffect(buckets) {
        animateBars = true
    }

    val barScale by animateFloatAsState(
        targetValue = if (animateBars) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "bar_height_animation"
    )

    val rawMax = remember(buckets) {
        buckets.maxOfOrNull { maxOf(it.savingsTotal, it.loanCollectionTotal) } ?: 0.0
    }
    val chartMax = remember(rawMax) {
        if (rawMax <= 0.0) 1000.0
        else {
            val step = when {
                rawMax <= 1000.0 -> 250.0
                rawMax <= 5000.0 -> 1000.0
                rawMax <= 20000.0 -> 2500.0
                else -> 5000.0
            }
            ceil(rawMax / step) * step
        }
    }

    val total4WeekSavings = remember(buckets) { buckets.sumOf { it.savingsTotal } }
    val total4WeekLoans = remember(buckets) { buckets.sumOf { it.loanCollectionTotal } }
    val selectedBucket = buckets.getOrNull(selectedWeekIndex) ?: buckets.lastOrNull()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6E9EE)),
        border = BorderStroke(1.2.dp, Color(0xFFB3BCC8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_weekly_collection_chart")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8F8EE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "সাপ্তাহিক কালেকশন চার্ট",
                            tint = savingsColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "গত ৪ সপ্তাহের সঞ্চয় ও ঋণ আদায়",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "সাপ্তাহিক তুলনামূলক বার চার্ট (ট্যাপ করে দেখুন)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendItem(
                    color = savingsColor,
                    label = "সঞ্চয় জমা (৳${total4WeekSavings.toInt()})"
                )
                ChartLegendItem(
                    color = loanColor,
                    label = "কিস্তি আদায় (৳${total4WeekLoans.toInt()})"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Week Tooltip / Detail Strip
            selectedBucket?.let { bucket ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCE1E8),
                    border = BorderStroke(1.dp, Color(0xFFB3BCC8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chart_selected_week_tooltip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = bucket.fullLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Text(
                                text = "মোট: ৳${bucket.combinedTotal.toInt()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1565C0)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("সঞ্চয়", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                                Text("৳${bucket.savingsTotal.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = savingsColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("কিস্তি", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                                Text("৳${bucket.loanCollectionTotal.toInt()}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = loanColor)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas + Y-axis labels + Interactive Week Columns
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                // Y-Axis Labels
                Column(
                    modifier = Modifier
                        .width(46.dp)
                        .height(140.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCompactAmount(chartMax),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = formatCompactAmount(chartMax / 2.0),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "৳0",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Gridlines + Grouped Bars
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

                            // Horizontal grid lines (top, 50%, baseline)
                            listOf(0f, height * 0.5f, height).forEachIndexed { idx, y ->
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = if (idx == 2) 2.5f else 1.5f,
                                    pathEffect = if (idx == 2) null else dashEffect
                                )
                            }

                            val count = buckets.size.coerceAtLeast(1)
                            val groupWidth = width / count
                            val barWidth = (groupWidth * 0.28f).coerceAtMost(42f)
                            val gapBetweenBars = (groupWidth * 0.08f).coerceAtMost(14f)

                            buckets.forEachIndexed { index, bucket ->
                                val groupCenterX = groupWidth * index + groupWidth / 2f

                                if (index == selectedWeekIndex) {
                                    drawRoundRect(
                                        color = Color(0xFFCBD5E1),
                                        topLeft = Offset(groupWidth * index + 4f, 0f),
                                        size = Size(groupWidth - 8f, height),
                                        cornerRadius = CornerRadius(10f, 10f)
                                    )
                                }

                                val savingsRatio = (bucket.savingsTotal / chartMax).toFloat().coerceIn(0f, 1f) * barScale
                                val loanRatio = (bucket.loanCollectionTotal / chartMax).toFloat().coerceIn(0f, 1f) * barScale

                                val savingsHeight = if (bucket.savingsTotal > 0) (height * savingsRatio).coerceAtLeast(6f) else 0f
                                val loanHeight = if (bucket.loanCollectionTotal > 0) (height * loanRatio).coerceAtLeast(6f) else 0f

                                val savingsLeft = groupCenterX - barWidth - (gapBetweenBars / 2f)
                                val loanLeft = groupCenterX + (gapBetweenBars / 2f)

                                if (savingsHeight > 0f) {
                                    drawRoundRect(
                                        color = savingsColor,
                                        topLeft = Offset(savingsLeft, height - savingsHeight),
                                        size = Size(barWidth, savingsHeight),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }

                                if (loanHeight > 0f) {
                                    drawRoundRect(
                                        color = loanColor,
                                        topLeft = Offset(loanLeft, height - loanHeight),
                                        size = Size(barWidth, loanHeight),
                                        cornerRadius = CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }

                        // Invisible tap overlay columns for selecting each week
                        Row(modifier = Modifier.fillMaxSize()) {
                            buckets.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clickable { selectedWeekIndex = index }
                                        .testTag("chart_week_bar_$index")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // X-Axis Week Labels
                    Row(modifier = Modifier.fillMaxWidth()) {
                        buckets.forEachIndexed { index, bucket ->
                            val isSelected = index == selectedWeekIndex
                            Text(
                                text = bucket.shortLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedWeekIndex = index }
                            )
                        }
                    }
                }
            }

            if (rawMax <= 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFB3BCC8))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "সাপ্তাহিক সঞ্চয় বা কিস্তি আদায় এন্ট্রি করলে চার্টে গ্রাফ দেখা যাবে।",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

private fun formatCompactAmount(amount: Double): String {
    return when {
        amount >= 1000.0 -> {
            val k = amount / 1000.0
            if (k % 1.0 == 0.0) "৳${k.toInt()}k" else String.format(java.util.Locale.US, "৳%.1fk", k)
        }
        else -> "৳${amount.toInt()}"
    }
}
