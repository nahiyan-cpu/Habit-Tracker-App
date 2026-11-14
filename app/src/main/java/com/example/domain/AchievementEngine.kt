package com.example.domain

import com.example.data.local.AchievementEntity
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserAchievementEntity

object AchievementEngine {

    val predefinedAchievements: List<AchievementEntity> = listOf(
        AchievementEntity(
            key = "first_habit_completed",
            title = "First Step",
            description = "Complete your very first habit check-in.",
            iconKey = "bolt",
            pointsReward = 25,
            requirementType = "total_completions",
            requirementValue = 1
        ),
        AchievementEntity(
            key = "streak_3_days",
            title = "Spark Ignited",
            description = "Reach a 3-day streak on any habit.",
            iconKey = "local_fire_department",
            pointsReward = 50,
            requirementType = "streak_days",
            requirementValue = 3
        ),
        AchievementEntity(
            key = "streak_7_days",
            title = "Week Warrior",
            description = "Maintain a 7-day consistency streak.",
            iconKey = "workspace_premium",
            pointsReward = 100,
            requirementType = "streak_days",
            requirementValue = 7
        ),
        AchievementEntity(
            key = "streak_14_days",
            title = "Fortnight Focus",
            description = "Sustain a 14-day habit streak.",
            iconKey = "military_tech",
            pointsReward = 200,
            requirementType = "streak_days",
            requirementValue = 14
        ),
        AchievementEntity(
            key = "streak_30_days",
            title = "Monthly Master",
            description = "Achieve a 30-day unbroken streak.",
            iconKey = "emoji_events",
            pointsReward = 400,
            requirementType = "streak_days",
            requirementValue = 30
        ),
        AchievementEntity(
            key = "streak_100_days",
            title = "Centurion",
            description = "Conquer a legendary 100-day streak.",
            iconKey = "diamond",
            pointsReward = 1000,
            requirementType = "streak_days",
            requirementValue = 100
        ),
        AchievementEntity(
            key = "completions_25",
            title = "Momentum Builder",
            description = "Log 25 total habit completions.",
            iconKey = "trending_up",
            pointsReward = 75,
            requirementType = "total_completions",
            requirementValue = 25
        ),
        AchievementEntity(
            key = "completions_100",
            title = "Century Club",
            description = "Complete 100 total habit check-ins.",
            iconKey = "stars",
            pointsReward = 500,
            requirementType = "total_completions",
            requirementValue = 100
        ),
        AchievementEntity(
            key = "journal_5",
            title = "Mindful Reflector",
            description = "Write 5 daily journal reflections.",
            iconKey = "menu_book",
            pointsReward = 60,
            requirementType = "journal_entries",
            requirementValue = 5
        )
    )

    fun evaluateNewUnlocks(
        userId: String,
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        journals: List<DailyJournalEntity>,
        alreadyUnlockedKeys: Set<String>
    ): List<UserAchievementEntity> {
        val totalCompletions = completions.count { it.isCompleted }
        val maxHabitStreak = habits.maxOfOrNull { habit ->
            HabitScheduleEngine.calculateHabitStats(habit, completions).longestStreak
        } ?: 0
        val totalJournals = journals.count { !it.deleted && it.content.isNotBlank() }

        val now = System.currentTimeMillis()
        val newlyUnlocked = mutableListOf<UserAchievementEntity>()

        for (achievement in predefinedAchievements) {
            if (alreadyUnlockedKeys.contains(achievement.key)) continue
            val met = when (achievement.requirementType) {
                "total_completions" -> totalCompletions >= achievement.requirementValue
                "streak_days" -> maxHabitStreak >= achievement.requirementValue
                "journal_entries" -> totalJournals >= achievement.requirementValue
                else -> false
            }
            if (met) {
                newlyUnlocked.add(
                    UserAchievementEntity(
                        id = "${userId}_${achievement.key}",
                        userId = userId,
                        achievementKey = achievement.key,
                        unlockedAtEpochMs = now,
                        pointsAwarded = achievement.pointsReward,
                        updatedAtEpochMs = now
                    )
                )
            }
        }
        return newlyUnlocked
    }

    fun calculateTotalPoints(
        completions: List<HabitCompletionEntity>,
        unlockedAchievements: List<UserAchievementEntity>
    ): Long {
        val completionPoints = completions.count { it.isCompleted } * 10L
        val badgePoints = unlockedAchievements.sumOf { it.pointsAwarded.toLong() }
        return completionPoints + badgePoints
    }

    fun calculateLevel(totalPoints: Long): Int {
        // Level 1: 0-99, Level 2: 100-249, etc. (progressive curve)
        var level = 1
        var threshold = 100L
        var remaining = totalPoints
        while (remaining >= threshold) {
            remaining -= threshold
            level++
            threshold += 50L
        }
        return level
    }

    fun levelProgress(totalPoints: Long): Triple<Int, Long, Long> {
        var level = 1
        var threshold = 100L
        var remaining = totalPoints
        while (remaining >= threshold) {
            remaining -= threshold
            level++
            threshold += 50L
        }
        return Triple(level, remaining, threshold)
    }
}
