package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HabitCatalog
import com.example.domain.HabitScheduleEngine
import com.example.domain.PeriodChartPoint
import com.example.ui.theme.LocalHabitlyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.HabitlyMainUiState
import java.time.LocalDate

enum class StatsRangeFilter(val label: String) {
    DAYS_7("7 Days"),
    DAYS_30("30 Days"),
    DAYS_90("90 Days"),
    CUSTOM("Custom")
}

@Composable
fun StatisticsScreen(
    state: HabitlyMainUiState,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var selectedFilter by remember { mutableStateOf(StatsRangeFilter.DAYS_7) }
    var customStart by remember { mutableStateOf(today.minusDays(14)) }
    var customEnd by remember { mutableStateOf(today) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    val (startDate, endDate) = remember(selectedFilter, customStart, customEnd, today) {
        when (selectedFilter) {
            StatsRangeFilter.DAYS_7 -> today.minusDays(6) to today
            StatsRangeFilter.DAYS_30 -> today.minusDays(29) to today
            StatsRangeFilter.DAYS_90 -> today.minusDays(89) to today
            StatsRangeFilter.CUSTOM -> customStart to customEnd
        }
    }

    val periodSeries = remember(state.habits, state.completions, startDate, endDate) {
        HabitScheduleEngine.buildPeriodSeries(state.habits, state.completions, startDate, endDate)
    }

    val monthlySeries = remember(state.habits, state.completions, today) {
        HabitScheduleEngine.buildPeriodSeries(state.habits, state.completions, today.minusDays(29), today)
    }

    val habitConsistencies = remember(state.habits, state.completions, startDate, endDate) {
        HabitScheduleEngine.buildHabitConsistencyList(state.habits, state.completions, startDate, endDate)
    }

    val totalCompletedInRange = remember(state.completions, startDate, endDate) {
        state.completions.count { cp ->
            cp.isCompleted && runCatching {
                val d = LocalDate.parse(cp.dateIso)
                !d.isBefore(startDate) && !d.isAfter(endDate)
            }.getOrDefault(false)
        }
    }

    val dailySuccessRate = remember(state.habits, state.completions, today) {
        val completedSet = state.completions.filter { it.isCompleted }.map { it.habitId to it.dateIso }.toSet()
        val s = HabitScheduleEngine.calculateDayStatus(today, state.habits, completedSet, today)
        if (s.scheduledCount == 0) 0 else ((s.completedCount.toFloat() / s.scheduledCount) * 100).toInt()
    }

    val monthlySuccessRate = remember(monthlySeries) {
        val sched = monthlySeries.sumOf { it.scheduledCount }
        val comp = monthlySeries.sumOf { it.completedCount }
        if (sched == 0) 0 else ((comp.toFloat() / sched) * 100).toInt()
    }

    val bestDay = remember(state.habits, state.completions, startDate, endDate) {
        HabitScheduleEngine.findBestPerformingDayOfWeek(state.habits, state.completions, startDate, endDate)
    }

    val mostConsistent = habitConsistencies.firstOrNull()
    val leastConsistent = habitConsistencies.lastOrNull()
    val successColor = LocalHabitlyColors.current.success

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Analytics & Insights", style = MaterialTheme.typography.headlineMedium)
        }

        // Date Range Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsRangeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            if (filter == StatsRangeFilter.CUSTOM) {
                                showCustomRangeDialog = true
                            }
                            selectedFilter = filter
                        },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("stats_filter_${filter.name}")
                    )
                }
            }
        }

        // Summary KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatKpiCard(
                        title = "Daily Success",
                        value = "$dailySuccessRate%",
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        title = "Monthly Success",
                        value = "$monthlySuccessRate%",
                        accent = successColor,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatKpiCard(
                        title = "Total Completed",
                        value = "$totalCompletedInRange",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        title = "Best Day",
                        value = bestDay,
                        accent = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Weekly/Period Progress Bar Chart
        item {
            InteractiveBarChartCard(
                title = "${selectedFilter.label} Progress",
                points = periodSeries,
                barColor = MaterialTheme.colorScheme.primary
            )
        }

        // Monthly Progress Trend Chart
        item {
            LineTrendChartCard(
                title = "Monthly Progress Trend (30 Days)",
                points = monthlySeries,
                lineColor = successColor
            )
        }

        // Most & Least Consistent Spotlight
        if (mostConsistent != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Star, contentDescription = null, tint = successColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Most Consistent", style = MaterialTheme.typography.labelSmall, color = successColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(mostConsistent.habit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                            Text(
                                "${mostConsistent.ratePercent}% • ${mostConsistent.currentStreak}d streak",
                                fontFamily = MonoFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (leastConsistent != null && habitConsistencies.size > 1) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.TrendingDown, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Needs Attention", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(leastConsistent.habit.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Text(
                                    "${leastConsistent.ratePercent}% completion",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Habit-wise Completion Chart
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Habit-Wise Completion", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (habitConsistencies.isEmpty()) {
                        Text(
                            text = "Track habits for a few days to see habit-by-habit completion rates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        habitConsistencies.forEach { item ->
                            val c = HabitCatalog.parseColor(item.habit.colorHex)
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.habit.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${item.ratePercent}% (${item.completedCount}/${item.scheduledCount})",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        color = c
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (item.ratePercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = c,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (showCustomRangeDialog) {
        var startInput by remember { mutableStateOf(customStart.toString()) }
        var endInput by remember { mutableStateOf(customEnd.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("Custom Date Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { startInput = it },
                        label = { Text("Start Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { endInput = it },
                        label = { Text("End Date (YYYY-MM-DD)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val s = runCatching { LocalDate.parse(startInput) }.getOrDefault(customStart)
                    val e = runCatching { LocalDate.parse(endInput) }.getOrDefault(customEnd)
                    if (!s.isAfter(e)) {
                        customStart = s
                        customEnd = e
                    }
                    showCustomRangeDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatKpiCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun InteractiveBarChartCard(
    title: String,
    points: List<PeriodChartPoint>,
    barColor: Color
) {
    var selectedIdx by remember(points) { mutableIntStateOf(points.lastIndex.coerceAtLeast(0)) }
    val selected = points.getOrNull(selectedIdx)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (selected != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = barColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${selected.label}: ${selected.ratePercent}% (${selected.completedCount}/${selected.scheduledCount})",
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            color = barColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (points.isEmpty()) {
                Text("No data in range", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isSel = idx == selectedIdx
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedIdx = idx },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(trackColor),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val fraction = (pt.ratePercent / 100f).coerceIn(0.05f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp * fraction)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) barColor else barColor.copy(alpha = 0.58f))
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSel) barColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LineTrendChartCard(
    title: String,
    points: List<PeriodChartPoint>,
    lineColor: Color
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(14.dp))

            if (points.size < 2) {
                Text(
                    "More data points will appear as your monthly history grows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                    val coords = points.mapIndexed { idx, p ->
                        val x = idx * stepX
                        val y = size.height - (p.ratePercent / 100f) * (size.height - 16f) - 8f
                        Offset(x, y)
                    }
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, size.height)
                        coords.forEach { lineTo(it.x, it.y) }
                        lineTo(coords.last().x, size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
                    val strokePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                    coords.forEach { pt ->
                        drawCircle(color = Color.White, radius = 7f, center = pt)
                        drawCircle(color = lineColor, radius = 4.5f, center = pt)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach {
                        Text(
                            text = it.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
