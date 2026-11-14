package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CalendarDayState
import com.example.domain.HabitCatalog
import com.example.domain.HabitScheduleEngine
import com.example.ui.theme.LocalHabitlyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.HabitlyMainUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    state: HabitlyMainUiState,
    onToggleHabitForDate: (String, String) -> Unit,
    onOpenJournalForDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }

    val completedSet = remember(state.completions) {
        state.completions.filter { it.isCompleted }.map { it.habitId to it.dateIso }.toSet()
    }

    val firstDayIso = state.settings.firstDayOfWeek.coerceIn(1, 7) // 1=Mon, 7=Sun
    val dayHeaders = remember(firstDayIso) {
        val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        (0..6).map { offset -> names[(firstDayIso - 1 + offset) % 7] }
    }

    val monthDaysGrid = remember(currentMonth, firstDayIso) {
        val firstOfMonth = currentMonth.atDay(1)
        val leadingEmpty = ((firstOfMonth.dayOfWeek.value - firstDayIso) + 7) % 7
        val daysInMonth = currentMonth.lengthOfMonth()
        val cells = mutableListOf<LocalDate?>()
        repeat(leadingEmpty) { cells.add(null) }
        for (d in 1..daysInMonth) {
            cells.add(currentMonth.atDay(d))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        cells
    }

    val selectedDateIso = selectedDate.toString()
    val selectedDayHabits = remember(state.habits, selectedDate) {
        state.habits.filter {
            !it.deleted && !it.isArchived && HabitScheduleEngine.isScheduledOnDateIgnoringPause(it, selectedDate)
        }
    }
    val selectedDayStatus = remember(selectedDate, state.habits, completedSet) {
        HabitScheduleEngine.calculateDayStatus(selectedDate, state.habits, completedSet, today)
    }
    val selectedDayJournals = remember(state.journals, selectedDateIso) {
        state.journals.filter { it.dateIso == selectedDateIso }
    }

    val successColor = LocalHabitlyColors.current.success
    val warningColor = LocalHabitlyColors.current.warning
    val errorColor = MaterialTheme.colorScheme.error

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Consistency Calendar", style = MaterialTheme.typography.headlineMedium)
        }

        // Interactive Monthly Calendar Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentMonth = currentMonth.minusMonths(1) },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(
                            onClick = { currentMonth = currentMonth.plusMonths(1) },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayHeaders.forEach { label ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    monthDaysGrid.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val status = HabitScheduleEngine.calculateDayStatus(date, state.habits, completedSet, today)
                                    val isSelected = date == selectedDate
                                    val isToday = date == today
                                    val bgColor = when (status.state) {
                                        CalendarDayState.COMPLETED_ALL -> successColor.copy(alpha = 0.22f)
                                        CalendarDayState.PARTIAL -> warningColor.copy(alpha = 0.22f)
                                        CalendarDayState.MISSED -> errorColor.copy(alpha = 0.16f)
                                        CalendarDayState.FUTURE_SCHEDULED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        CalendarDayState.UNSCHEDULED -> Color.Transparent
                                    }
                                    val textColor = when (status.state) {
                                        CalendarDayState.COMPLETED_ALL -> successColor
                                        CalendarDayState.PARTIAL -> Color(0xFFD97706)
                                        CalendarDayState.MISSED -> errorColor
                                        CalendarDayState.FUTURE_SCHEDULED -> MaterialTheme.colorScheme.primary
                                        CalendarDayState.UNSCHEDULED -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else if (isToday) {
                                                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                                                } else Modifier
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontFamily = MonoFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendDot(color = successColor, label = "Completed")
                        LegendDot(color = warningColor, label = "Partial")
                        LegendDot(color = errorColor, label = "Missed")
                        LegendDot(color = MaterialTheme.colorScheme.primary, label = "Scheduled")
                    }
                }
            }
        }

        // GitHub-Style Activity Heatmap (Last 16 Weeks)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activity Heatmap (16 Weeks)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Darker green indicates higher daily habit completion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val startWeekSunday = today.minusWeeks(15).minusDays((today.dayOfWeek.value % 7).toLong())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (weekCol in 0 until 16) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (rowDay in 0 until 7) {
                                    val cellDate = startWeekSunday.plusWeeks(weekCol.toLong()).plusDays(rowDay.toLong())
                                    val cellStatus = HabitScheduleEngine.calculateDayStatus(
                                        cellDate,
                                        state.habits,
                                        completedSet,
                                        today
                                    )
                                    val intensity = if (cellStatus.scheduledCount == 0) 0f
                                    else cellStatus.completedCount.toFloat() / cellStatus.scheduledCount.toFloat()

                                    val cellColor = when {
                                        cellDate.isAfter(today) -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        intensity >= 1f -> successColor
                                        intensity >= 0.6f -> successColor.copy(alpha = 0.7f)
                                        intensity > 0f -> successColor.copy(alpha = 0.38f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(cellColor)
                                            .clickable { selectedDate = cellDate }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Summary & Records
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${selectedDayStatus.completedCount}/${selectedDayStatus.scheduledCount} habits completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenJournalForDate) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Journal")
                }
            }
        }

        if (selectedDayHabits.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No habits scheduled on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(selectedDayHabits, key = { it.id }) { habit ->
                val isDone = completedSet.contains(habit.id to selectedDateIso)
                val habitColor = HabitCatalog.parseColor(habit.colorHex)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = HabitCatalog.iconForKey(habit.iconKey),
                            contentDescription = null,
                            tint = habitColor
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDone) successColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onToggleHabitForDate(habit.id, selectedDateIso) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Toggle completion for ${habit.name}",
                                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedDayJournals.isNotEmpty()) {
            item {
                Text("Reflections on $selectedDateIso", style = MaterialTheme.typography.titleSmall)
            }
            items(selectedDayJournals, key = { it.id }) { j ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Mood: ${j.mood.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = j.content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
