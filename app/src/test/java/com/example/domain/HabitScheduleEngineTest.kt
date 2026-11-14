package com.example.domain

import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitScheduleEngineTest {

    private fun sampleHabit(
        id: String = "h1",
        frequencyType: String = "daily",
        startDate: String = "2026-11-01",
        targetDaysPerWeek: Int = 7,
        scheduledWeekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
        scheduledMonthDays: List<Int> = listOf(1, 15)
    ) = HabitEntity(
        id = id,
        userId = "u1",
        name = "Morning Run",
        categoryId = "cat_fitness",
        iconKey = "directions_run",
        colorHex = "#6C63FF",
        startDate = startDate,
        frequencyType = frequencyType,
        targetDaysPerWeek = targetDaysPerWeek,
        scheduledWeekdays = scheduledWeekdays,
        scheduledMonthDays = scheduledMonthDays
    )

    private fun completion(habitId: String, date: LocalDate) = HabitCompletionEntity(
        id = "${habitId}_$date",
        userId = "u1",
        habitId = habitId,
        dateIso = date.toString(),
        completedAtEpochMs = 1_700_000_000_000L,
        isCompleted = true
    )

    @Test
    fun weeklySchedule_unscheduledDaysDoNotBreakStreak() {
        // Scheduled Monday (1), Wednesday (3), Friday (5) starting 2026-11-02 (Monday)
        val habit = sampleHabit(
            frequencyType = "weekly",
            startDate = "2026-11-02",
            scheduledWeekdays = listOf(1, 3, 5)
        )
        val today = LocalDate.of(2026, 11, 9) // Next Monday
        // Complete Mon Nov 2, Wed Nov 4, Fri Nov 6, Mon Nov 9
        val completions = listOf(
            completion("h1", LocalDate.of(2026, 11, 2)),
            completion("h1", LocalDate.of(2026, 11, 4)),
            completion("h1", LocalDate.of(2026, 11, 6)),
            completion("h1", LocalDate.of(2026, 11, 9))
        )

        // Tue Nov 3 & Thu Nov 5 & weekend are unscheduled
        assertFalse(HabitScheduleEngine.isScheduledForDate(habit, LocalDate.of(2026, 11, 3)))
        assertTrue(HabitScheduleEngine.isScheduledForDate(habit, LocalDate.of(2026, 11, 4)))

        val stats = HabitScheduleEngine.calculateHabitStats(habit, completions, today)
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
        assertEquals(0, stats.totalMissedScheduledDays)
        assertEquals(100, stats.completionRatePercent)
    }

    @Test
    fun monthlySchedule_matchesSpecificDatesAndCountsMissedDays() {
        val habit = sampleHabit(
            frequencyType = "monthly",
            startDate = "2026-11-01",
            scheduledMonthDays = listOf(1, 10, 20)
        )
        assertTrue(HabitScheduleEngine.isScheduledForDate(habit, LocalDate.of(2026, 11, 1)))
        assertFalse(HabitScheduleEngine.isScheduledForDate(habit, LocalDate.of(2026, 11, 5)))
        assertTrue(HabitScheduleEngine.isScheduledForDate(habit, LocalDate.of(2026, 11, 10)))

        val today = LocalDate.of(2026, 11, 12)
        // Completed Nov 1, missed Nov 10
        val completions = listOf(completion("h1", LocalDate.of(2026, 11, 1)))
        val stats = HabitScheduleEngine.calculateHabitStats(habit, completions, today)
        assertEquals(0, stats.currentStreak)
        assertEquals(1, stats.longestStreak)
        assertEquals(1, stats.totalMissedScheduledDays)
        assertEquals(50, stats.completionRatePercent)
    }

    @Test
    fun dailyHabit_todayNotCompletedYet_preservesYesterdayStreak() {
        val habit = sampleHabit(frequencyType = "daily", startDate = "2026-11-10")
        val today = LocalDate.of(2026, 11, 13)
        val completions = listOf(
            completion("h1", LocalDate.of(2026, 11, 10)),
            completion("h1", LocalDate.of(2026, 11, 11)),
            completion("h1", LocalDate.of(2026, 11, 12))
        )
        val stats = HabitScheduleEngine.calculateHabitStats(habit, completions, today)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
        assertEquals(0, stats.totalMissedScheduledDays)
    }

    @Test
    fun calendarDayStatus_distinguishesCompletedPartialMissedAndFuture() {
        val h1 = sampleHabit(id = "h1", startDate = "2026-11-10")
        val h2 = sampleHabit(id = "h2", startDate = "2026-11-10")
        val habits = listOf(h1, h2)
        val today = LocalDate.of(2026, 11, 14)

        val completedPairs = setOf(
            "h1" to "2026-11-10",
            "h2" to "2026-11-10",
            "h1" to "2026-11-11"
        )

        assertEquals(
            CalendarDayState.COMPLETED_ALL,
            HabitScheduleEngine.calculateDayStatus(LocalDate.of(2026, 11, 10), habits, completedPairs, today).state
        )
        assertEquals(
            CalendarDayState.PARTIAL,
            HabitScheduleEngine.calculateDayStatus(LocalDate.of(2026, 11, 11), habits, completedPairs, today).state
        )
        assertEquals(
            CalendarDayState.MISSED,
            HabitScheduleEngine.calculateDayStatus(LocalDate.of(2026, 11, 12), habits, completedPairs, today).state
        )
        assertEquals(
            CalendarDayState.FUTURE_SCHEDULED,
            HabitScheduleEngine.calculateDayStatus(LocalDate.of(2026, 11, 15), habits, completedPairs, today).state
        )
    }

    @Test
    fun achievementEngine_unlocksFirstStepAndJournalAndUpdatesLevel() {
        val habit = sampleHabit(startDate = "2026-11-10")
        val completions = listOf(
            completion("h1", LocalDate.of(2026, 11, 10)),
            completion("h1", LocalDate.of(2026, 11, 11)),
            completion("h1", LocalDate.of(2026, 11, 12))
        )
        val journals = listOf(
            DailyJournalEntity(id = "j1", userId = "u1", dateIso = "2026-11-10", mood = "motivated", content = "Day 1")
        )

        val unlocked = AchievementEngine.evaluateNewUnlocks(
            userId = "u1",
            habits = listOf(habit),
            completions = completions,
            journals = journals,
            alreadyUnlockedKeys = emptySet()
        )
        val keys = unlocked.map { it.achievementKey }.toSet()
        assertTrue(keys.contains("first_habit_completed"))
        assertTrue(keys.contains("streak_3_days"))

        val points = AchievementEngine.calculateTotalPoints(completions, unlocked)
        // 3 completions * 10 + 25 (first step) + 50 (streak 3) = 105
        assertEquals(105L, points)
        assertEquals(2, AchievementEngine.calculateLevel(points))
    }
}
