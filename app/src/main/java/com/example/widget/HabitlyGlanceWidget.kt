package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.HabitEntity
import com.example.data.local.HabitlyDatabase
import com.example.di.HabitlyApplication
import com.example.domain.HabitScheduleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val PARAM_HABIT_ID = ActionParameters.Key<String>("habit_id")
private val PARAM_USER_ID = ActionParameters.Key<String>("user_id")

data class WidgetHabitRow(
    val habit: HabitEntity,
    val isCompleted: Boolean
)

class HabitlyGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) {
            loadWidgetState(context)
        }
        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    private suspend fun loadWidgetState(context: Context): WidgetUiState {
        val prefs = AppPreferencesDataStore(context).getSnapshot()
        val userId = prefs.activeUserId
        if (userId.isBlank()) {
            return WidgetUiState(isSignedIn = false)
        }
        val db = HabitlyDatabase.getInstance(context)
        val today = LocalDate.now()
        val todayIso = today.toString()
        val allHabits = db.habitDao().getActiveHabitsOnce(userId)
        val todayHabits = allHabits.filter { HabitScheduleEngine.isScheduledForDate(it, today) }
        val completedIds = db.completionDao().getCompletedForDate(userId, todayIso).map { it.habitId }.toSet()
        val allCompletions = db.completionDao().getAllCompletionsForSync(userId)
        val streak = HabitScheduleEngine.calculateOverallStreak(allHabits, allCompletions, today)

        val rows = todayHabits.map { WidgetHabitRow(it, completedIds.contains(it.id)) }
        val doneCount = rows.count { it.isCompleted }
        val percent = if (rows.isEmpty()) 0 else ((doneCount.toFloat() / rows.size) * 100).toInt()

        return WidgetUiState(
            isSignedIn = true,
            userId = userId,
            completedCount = doneCount,
            totalCount = rows.size,
            progressPercent = percent,
            streakDays = streak,
            rows = rows
        )
    }

    @Composable
    private fun WidgetContent(state: WidgetUiState) {
        val bg = ColorProvider(Color(0xFFFFFFFF))
        val primary = ColorProvider(Color(0xFF6C63FF))
        val success = ColorProvider(Color(0xFF22C55E))
        val textPrimary = ColorProvider(Color(0xFF12131A))
        val textSecondary = ColorProvider(Color(0xFF64748B))
        val cardSurface = ColorProvider(Color(0xFFF4F5FA))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bg)
                .cornerRadius(20.dp)
                .padding(14.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Habitly Today",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (state.isSignedIn) {
                            "${state.completedCount}/${state.totalCount} done • ${state.progressPercent}%"
                        } else {
                            "Sign in to sync your habits"
                        },
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }

                if (state.isSignedIn) {
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(Color(0xFFEDE9FE)))
                            .cornerRadius(12.dp)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${state.streakDays}d streak",
                            style = TextStyle(
                                color = primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            if (!state.isSignedIn) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(cardSurface)
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to open Habitly",
                        style = TextStyle(color = primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    )
                }
            } else if (state.rows.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(cardSurface)
                        .cornerRadius(14.dp)
                        .clickable(actionStartActivity<MainActivity>())
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No habits scheduled today. Tap to add one!",
                        style = TextStyle(color = textSecondary, fontSize = 12.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(state.rows, itemId = { it.habit.id.hashCode().toLong() }) { row ->
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(cardSurface)
                                .cornerRadius(12.dp)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CheckBox(
                                checked = row.isCompleted,
                                onCheckedChange = actionRunCallback<ToggleHabitActionCallback>(
                                    actionParametersOf(
                                        PARAM_HABIT_ID to row.habit.id,
                                        PARAM_USER_ID to state.userId
                                    )
                                ),
                                text = row.habit.name,
                                style = TextStyle(
                                    color = if (row.isCompleted) success else textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = if (row.isCompleted) "Done" else "Tap",
                                style = TextStyle(
                                    color = if (row.isCompleted) success else primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        suspend fun refreshAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(HabitlyGlanceWidget::class.java)
            if (ids.isNotEmpty()) {
                HabitlyGlanceWidget().updateAll(context)
            }
        }
    }
}

data class WidgetUiState(
    val isSignedIn: Boolean = false,
    val userId: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val progressPercent: Int = 0,
    val streakDays: Int = 0,
    val rows: List<WidgetHabitRow> = emptyList()
)

class ToggleHabitActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val habitId = parameters[PARAM_HABIT_ID] ?: return
        val userId = parameters[PARAM_USER_ID] ?: return
        val todayIso = LocalDate.now().toString()
        val app = context.applicationContext as? HabitlyApplication
        if (app != null) {
            app.container.habitRepository.toggleCompletion(userId, habitId, todayIso)
        }
        HabitlyGlanceWidget().update(context, glanceId)
    }
}

class HabitlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitlyGlanceWidget()
}
