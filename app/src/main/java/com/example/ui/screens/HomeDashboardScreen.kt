package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.HabitScheduleEngine
import com.example.ui.components.AnimatedProgressRing
import com.example.ui.components.HabitCheckInCard
import com.example.ui.components.formatTimeDisplay
import com.example.ui.theme.LocalHabitlyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.HabitlyMainUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeDashboardScreen(
    state: HabitlyMainUiState,
    onToggleHabit: (String) -> Unit,
    onOpenHabitDetail: (String) -> Unit,
    onOpenAddHabit: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAchievements: () -> Unit,
    onRefreshQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val todayIso = remember { today.toString() }
    val dateFormatted = remember {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    }
    val greeting = remember {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = state.user?.displayName?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Friend"

    val todayHabits = remember(state.habits) {
        state.habits.filter { HabitScheduleEngine.isScheduledForDate(it, today) }
    }
    val completedTodayIds = remember(state.completions, todayIso) {
        state.completions.filter { it.dateIso == todayIso && it.isCompleted }.map { it.habitId }.toSet()
    }
    val completedCount = todayHabits.count { completedTodayIds.contains(it.id) }
    val totalCount = todayHabits.size
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    val progressPercent = (progress * 100).toInt()
    val overallStreak = remember(state.habits, state.completions) {
        HabitScheduleEngine.calculateOverallStreak(state.habits, state.completions, today)
    }

    val weeklyOverview = remember(state.habits, state.completions) {
        HabitScheduleEngine.buildPeriodSeries(
            habits = state.habits,
            completions = state.completions,
            startDate = today.minusDays(6),
            endDate = today
        )
    }
    val weeklyRate = remember(weeklyOverview) {
        val sched = weeklyOverview.sumOf { it.scheduledCount }
        val comp = weeklyOverview.sumOf { it.completedCount }
        if (sched == 0) 0 else ((comp.toFloat() / sched) * 100).toInt()
    }

    val upcomingReminders = remember(todayHabits, completedTodayIds) {
        val nowTime = LocalTime.now()
        todayHabits
            .filter { it.remindersEnabled && !completedTodayIds.contains(it.id) }
            .flatMap { h ->
                h.reminderTimes.mapNotNull { t ->
                    val parsed = runCatching { LocalTime.parse(t) }.getOrNull() ?: return@mapNotNull null
                    if (!parsed.isBefore(nowTime.minusMinutes(5))) Triple(h, t, parsed) else null
                }
            }
            .sortedBy { it.third }
            .take(5)
    }

    val categoriesMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val successColor = LocalHabitlyColors.current.success

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Quick Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatted.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$greeting, $firstName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onOpenJournal,
                            modifier = Modifier.testTag("home_journal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = "Open Daily Journal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = onOpenAchievements,
                            modifier = Modifier.testTag("home_achievements_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Open Achievements",
                                tint = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }

        // Daily Progress Ring & Streak Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedProgressRing(
                            progress = progress,
                            size = 106.dp,
                            strokeWidth = 10.dp,
                            progressColor = if (totalCount > 0 && completedCount == totalCount) successColor else MaterialTheme.colorScheme.primary
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$progressPercent%",
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$completedCount/$totalCount",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Today's Focus",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    totalCount == 0 -> "Create your first habit to start building momentum."
                                    completedCount == totalCount -> "All scheduled habits done! Outstanding."
                                    else -> "${totalCount - completedCount} habit${if (totalCount - completedCount == 1) "" else "s"} left for today."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${overallStreak}d streak",
                                            fontFamily = MonoFontFamily,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Lv ${state.user?.level ?: 1}",
                                        fontFamily = MonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // All Habits Completed Congratulatory Banner
        item {
            AnimatedVisibility(
                visible = totalCount > 0 && completedCount == totalCount,
                enter = fadeIn() + expandVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = successColor.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_celebration_trophy),
                            contentDescription = "All habits completed trophy",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Perfect Day Achieved!",
                                style = MaterialTheme.typography.titleMedium,
                                color = successColor
                            )
                            Text(
                                text = "You completed every scheduled habit today. Keep the chain unbroken!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Daily Motivational Quote Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onRefreshQuote),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"${state.quote.text}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "— ${state.quote.author}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshQuote,
                        modifier = Modifier.testTag("refresh_quote_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Next quote",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Weekly Overview Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("7-Day Consistency", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "$weeklyRate% / Goal ${state.settings.weeklyGoalPercent}%",
                            fontFamily = MonoFontFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (weeklyRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (weeklyRate >= state.settings.weeklyGoalPercent) successColor else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weeklyOverview.forEach { dayPoint ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                dayPoint.scheduledCount == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                                dayPoint.ratePercent >= 100 -> successColor
                                                dayPoint.ratePercent > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dayPoint.scheduledCount == 0) "-" else "${dayPoint.ratePercent}%",
                                        fontSize = 9.sp,
                                        fontFamily = MonoFontFamily,
                                        color = if (dayPoint.ratePercent > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayPoint.label.take(2),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Reminders
        if (upcomingReminders.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Upcoming Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(upcomingReminders) { (habit, timeStr, _) ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp,
                                modifier = Modifier.clickable { onOpenHabitDetail(habit.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(habit.name, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = formatTimeDisplay(timeStr, state.settings.use24HourFormat),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Habits Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Habits ($totalCount)",
                    style = MaterialTheme.typography.titleLarge
                )
                FilledTonalButton(
                    onClick = onOpenAddHabit,
                    modifier = Modifier.testTag("home_add_habit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Habit")
                }
            }
        }

        if (todayHabits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_habits),
                            contentDescription = "No habits scheduled today",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ready to build your routine?",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add a daily, weekly, or monthly habit to start tracking your streaks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenAddHabit,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("empty_create_habit_button")
                        ) {
                            Text("Create First Habit")
                        }
                    }
                }
            }
        } else {
            items(todayHabits, key = { it.id }) { habit ->
                val stats = remember(habit, state.completions) {
                    HabitScheduleEngine.calculateHabitStats(habit, state.completions, today)
                }
                HabitCheckInCard(
                    habit = habit,
                    category = categoriesMap[habit.categoryId],
                    currentStreak = stats.currentStreak,
                    isCompleted = completedTodayIds.contains(habit.id),
                    use24HourFormat = state.settings.use24HourFormat,
                    onToggleComplete = { onToggleHabit(habit.id) },
                    onCardClick = { onOpenHabitDetail(habit.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}
