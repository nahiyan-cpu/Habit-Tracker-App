package com.example.domain

import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class FrequencyType(val value: String, val label: String) {
    DAILY("daily", "Every Day"),
    DAYS_PER_WEEK("days_per_week", "Times per Week"),
    WEEKLY("weekly", "Specific Weekdays"),
    MONTHLY("monthly", "Monthly Dates");

    companion object {
        fun fromValue(value: String): FrequencyType =
            entries.firstOrNull { it.value == value } ?: DAILY
    }
}

data class HabitStatsSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletedDays: Int,
    val totalMissedScheduledDays: Int,
    val completionRatePercent: Int,
    val weeklySuccessRatePercent: Int,
    val monthlySuccessRatePercent: Int
)

data class DayStatus(
    val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val state: CalendarDayState
)

enum class CalendarDayState {
    COMPLETED_ALL,
    PARTIAL,
    MISSED,
    FUTURE_SCHEDULED,
    UNSCHEDULED
}

data class PeriodChartPoint(
    val label: String,
    val ratePercent: Int,
    val completedCount: Int,
    val scheduledCount: Int
)

data class HabitConsistencyItem(
    val habit: HabitEntity,
    val ratePercent: Int,
    val completedCount: Int,
    val scheduledCount: Int,
    val currentStreak: Int
)

object HabitScheduleEngine {

    fun isScheduledForDate(habit: HabitEntity, date: LocalDate): Boolean {
        if (habit.deleted || habit.isArchived || habit.isPaused) return false
        val start = runCatching { LocalDate.parse(habit.startDate) }.getOrNull() ?: date
        if (date.isBefore(start)) return false

        return when (FrequencyType.fromValue(habit.frequencyType)) {
            FrequencyType.DAILY -> true
            FrequencyType.DAYS_PER_WEEK -> {
                // Flexible goal per week; counts as active every day of the week unless met,
                // or scheduled on all days so user can check in any day.
                true
            }
            FrequencyType.WEEKLY -> {
                val isoDow = date.dayOfWeek.value // 1=Mon .. 7=Sun
                habit.scheduledWeekdays.contains(isoDow)
            }
            FrequencyType.MONTHLY -> {
                habit.scheduledMonthDays.contains(date.dayOfMonth)
            }
        }
    }

    fun isScheduledOnDateIgnoringPause(habit: HabitEntity, date: LocalDate): Boolean {
        if (habit.deleted) return false
        val start = runCatching { LocalDate.parse(habit.startDate) }.getOrNull() ?: date
        if (date.isBefore(start)) return false
        return when (FrequencyType.fromValue(habit.frequencyType)) {
            FrequencyType.DAILY -> true
            FrequencyType.DAYS_PER_WEEK -> true
            FrequencyType.WEEKLY -> habit.scheduledWeekdays.contains(date.dayOfWeek.value)
            FrequencyType.MONTHLY -> habit.scheduledMonthDays.contains(date.dayOfMonth)
        }
    }

    fun calculateHabitStats(
        habit: HabitEntity,
        completions: List<HabitCompletionEntity>,
        today: LocalDate = LocalDate.now()
    ): HabitStatsSummary {
        val completedDates = completions
            .asSequence()
            .filter { it.habitId == habit.id && it.isCompleted }
            .mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
            .toSet()

        val start = runCatching { LocalDate.parse(habit.startDate) }.getOrNull() ?: today
        val effectiveStart = if (start.isAfter(today)) today else start

        val freq = FrequencyType.fromValue(habit.frequencyType)
        if (freq == FrequencyType.DAYS_PER_WEEK) {
            return calculateWeeklyTargetStats(habit, completedDates, effectiveStart, today)
        }

        // Daily / Weekly / Monthly scheduled day calculation:
        // Unscheduled days NEVER break a streak.
        var longestStreak = 0
        var runningStreak = 0
        var totalScheduled = 0
        var totalCompleted = 0
        var totalMissed = 0

        var cursor = effectiveStart
        while (!cursor.isAfter(today)) {
            val scheduled = isScheduledOnDateIgnoringPause(habit, cursor)
            val completed = completedDates.contains(cursor)

            if (completed) {
                totalCompleted++
            }
            if (scheduled) {
                totalScheduled++
                if (completed) {
                    runningStreak++
                    if (runningStreak > longestStreak) longestStreak = runningStreak
                } else if (cursor.isBefore(today)) {
                    // Past scheduled day was missed -> streak breaks
                    totalMissed++
                    runningStreak = 0
                }
                // Note: If cursor == today and not yet completed, current streak from yesterday stays intact!
            } else if (completed) {
                // Completed on an unscheduled bonus day still increments running streak
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            }
            cursor = cursor.plusDays(1)
        }

        val currentStreak = runningStreak
        val completionRate = if (totalScheduled == 0) {
            if (totalCompleted > 0) 100 else 0
        } else {
            ((totalCompleted.toFloat() / totalScheduled) * 100).toInt().coerceIn(0, 100)
        }

        val weeklyRate = calculatePeriodRate(habit, completedDates, today.minusDays(6), today)
        val monthlyRate = calculatePeriodRate(habit, completedDates, today.minusDays(29), today)

        return HabitStatsSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletedDays = totalCompleted,
            totalMissedScheduledDays = totalMissed,
            completionRatePercent = completionRate,
            weeklySuccessRatePercent = weeklyRate,
            monthlySuccessRatePercent = monthlyRate
        )
    }

    private fun calculateWeeklyTargetStats(
        habit: HabitEntity,
        completedDates: Set<LocalDate>,
        effectiveStart: LocalDate,
        today: LocalDate
    ): HabitStatsSummary {
        val target = habit.targetDaysPerWeek.coerceIn(1, 7)
        val totalDays = ChronoUnit.DAYS.between(effectiveStart, today).toInt() + 1
        val totalWeeks = ((totalDays + 6) / 7).coerceAtLeast(1)
        val totalCompleted = completedDates.count { !it.isBefore(effectiveStart) && !it.isAfter(today) }

        // Group completions by ISO week (Monday-start)
        var cursor = effectiveStart.with(DayOfWeek.MONDAY)
        var currentStreak = 0
        var longestStreak = 0
        var missed = 0

        while (!cursor.isAfter(today)) {
            val weekEnd = cursor.plusDays(6)
            val weekCount = (0..6).count { offset ->
                val d = cursor.plusDays(offset.toLong())
                completedDates.contains(d)
            }
            if (weekCount >= target) {
                currentStreak += weekCount
                if (currentStreak > longestStreak) longestStreak = currentStreak
            } else if (weekEnd.isBefore(today)) {
                missed += (target - weekCount).coerceAtLeast(0)
                currentStreak = 0
            } else {
                // Current in-progress week
                currentStreak += weekCount
                if (currentStreak > longestStreak) longestStreak = currentStreak
            }
            cursor = cursor.plusWeeks(1)
        }

        val expectedTotal = (totalWeeks * target).coerceAtLeast(1)
        val completionRate = ((totalCompleted.toFloat() / expectedTotal) * 100).toInt().coerceIn(0, 100)

        val last7Count = (0..6).count { completedDates.contains(today.minusDays(it.toLong())) }
        val weeklyRate = ((last7Count.toFloat() / target) * 100).toInt().coerceIn(0, 100)

        val last30Count = (0..29).count { completedDates.contains(today.minusDays(it.toLong())) }
        val monthlyExpected = ((30f / 7f) * target).coerceAtLeast(1f)
        val monthlyRate = ((last30Count / monthlyExpected) * 100).toInt().coerceIn(0, 100)

        return HabitStatsSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalCompletedDays = totalCompleted,
            totalMissedScheduledDays = missed,
            completionRatePercent = completionRate,
            weeklySuccessRatePercent = weeklyRate,
            monthlySuccessRatePercent = monthlyRate
        )
    }

    private fun calculatePeriodRate(
        habit: HabitEntity,
        completedDates: Set<LocalDate>,
        from: LocalDate,
        to: LocalDate
    ): Int {
        var scheduled = 0
        var completed = 0
        var cursor = from
        while (!cursor.isAfter(to)) {
            if (isScheduledOnDateIgnoringPause(habit, cursor)) {
                scheduled++
                if (completedDates.contains(cursor)) completed++
            }
            cursor = cursor.plusDays(1)
        }
        return if (scheduled == 0) 0 else ((completed.toFloat() / scheduled) * 100).toInt().coerceIn(0, 100)
    }

    fun calculateDayStatus(
        date: LocalDate,
        habits: List<HabitEntity>,
        completionsByHabitAndDate: Set<Pair<String, String>>,
        today: LocalDate = LocalDate.now()
    ): DayStatus {
        val dateIso = date.toString()
        val activeHabits = habits.filter { !it.deleted && !it.isArchived }
        val scheduledHabits = activeHabits.filter { isScheduledOnDateIgnoringPause(it, date) && !it.isPaused }
        val completedCount = scheduledHabits.count { completionsByHabitAndDate.contains(it.id to dateIso) }
        val scheduledCount = scheduledHabits.size

        val state = when {
            scheduledCount == 0 -> CalendarDayState.UNSCHEDULED
            date.isAfter(today) -> CalendarDayState.FUTURE_SCHEDULED
            completedCount >= scheduledCount -> CalendarDayState.COMPLETED_ALL
            completedCount > 0 -> CalendarDayState.PARTIAL
            date.isBefore(today) -> CalendarDayState.MISSED
            else -> CalendarDayState.FUTURE_SCHEDULED // today with 0 done so far
        }

        return DayStatus(
            date = date,
            scheduledCount = scheduledCount,
            completedCount = completedCount,
            state = state
        )
    }

    fun buildPeriodSeries(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PeriodChartPoint> {
        val active = habits.filter { !it.deleted && !it.isArchived }
        val completedSet = completions.filter { it.isCompleted }.map { it.habitId to it.dateIso }.toSet()
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        if (totalDays <= 14) {
            // Daily points
            return (0 until totalDays).map { offset ->
                val d = startDate.plusDays(offset.toLong())
                val status = calculateDayStatus(d, active, completedSet)
                val rate = if (status.scheduledCount == 0) 0 else ((status.completedCount.toFloat() / status.scheduledCount) * 100).toInt()
                PeriodChartPoint(
                    label = d.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                    ratePercent = rate,
                    completedCount = status.completedCount,
                    scheduledCount = status.scheduledCount
                )
            }
        } else {
            // Bucket into weeks
            val points = mutableListOf<PeriodChartPoint>()
            var cursor = startDate
            var weekIdx = 1
            while (!cursor.isAfter(endDate)) {
                val bucketEnd = minOf(cursor.plusDays(6), endDate)
                var sched = 0
                var comp = 0
                var d = cursor
                while (!d.isAfter(bucketEnd)) {
                    val s = calculateDayStatus(d, active, completedSet)
                    sched += s.scheduledCount
                    comp += s.completedCount
                    d = d.plusDays(1)
                }
                val rate = if (sched == 0) 0 else ((comp.toFloat() / sched) * 100).toInt().coerceIn(0, 100)
                points.add(
                    PeriodChartPoint(
                        label = "W$weekIdx",
                        ratePercent = rate,
                        completedCount = comp,
                        scheduledCount = sched
                    )
                )
                weekIdx++
                cursor = bucketEnd.plusDays(1)
            }
            return points
        }
    }

    fun calculateOverallStreak(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        today: LocalDate = LocalDate.now()
    ): Int {
        val active = habits.filter { !it.deleted && !it.isArchived && !it.isPaused }
        if (active.isEmpty()) return 0
        val completedDates = completions.filter { it.isCompleted }.map { it.dateIso }.toSet()
        var streak = 0
        var cursor = today
        // If today has no completions yet, start checking from yesterday so streak isn't prematurely zeroed mid-day
        if (!completedDates.contains(today.toString())) {
            cursor = today.minusDays(1)
        }
        while (true) {
            if (completedDates.contains(cursor.toString())) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                break
            }
            if (streak > 3650) break
        }
        return streak
    }

    fun buildHabitConsistencyList(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<HabitConsistencyItem> {
        val active = habits.filter { !it.deleted && !it.isArchived }
        return active.map { habit ->
            val completedForHabit = completions.filter { it.habitId == habit.id && it.isCompleted }
            val completedDateSet = completedForHabit.map { it.dateIso }.toSet()
            var sched = 0
            var comp = 0
            var d = startDate
            while (!d.isAfter(endDate)) {
                if (isScheduledOnDateIgnoringPause(habit, d)) {
                    sched++
                    if (completedDateSet.contains(d.toString())) comp++
                }
                d = d.plusDays(1)
            }
            val rate = if (sched == 0) 0 else ((comp.toFloat() / sched) * 100).toInt().coerceIn(0, 100)
            val stats = calculateHabitStats(habit, completedForHabit, endDate)
            HabitConsistencyItem(
                habit = habit,
                ratePercent = rate,
                completedCount = comp,
                scheduledCount = sched,
                currentStreak = stats.currentStreak
            )
        }.sortedByDescending { it.ratePercent }
    }

    fun findBestPerformingDayOfWeek(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): String {
        val active = habits.filter { !it.deleted && !it.isArchived }
        if (active.isEmpty()) return "—"
        val completedSet = completions.filter { it.isCompleted }.map { it.habitId to it.dateIso }.toSet()
        val totals = mutableMapOf<DayOfWeek, Pair<Int, Int>>() // dow -> (completed, scheduled)
        var d = startDate
        while (!d.isAfter(endDate)) {
            val s = calculateDayStatus(d, active, completedSet)
            val prev = totals[d.dayOfWeek] ?: (0 to 0)
            totals[d.dayOfWeek] = (prev.first + s.completedCount) to (prev.second + s.scheduledCount)
            d = d.plusDays(1)
        }
        val best = totals.entries
            .filter { it.value.second > 0 }
            .maxByOrNull { it.value.first.toFloat() / it.value.second }
            ?.key ?: return "—"
        return best.name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
