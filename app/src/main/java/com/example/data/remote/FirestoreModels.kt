package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val totalPoints: Long = 0L,
    val level: Long = 1L,
    val themeMode: String = "system",
    val accentColorHex: String = "#6C63FF",
    val firstDayOfWeek: Long = 1L,
    val use24HourFormat: Boolean = false,
    val weeklyGoalPercent: Long = 80L,
    val notificationsEnabled: Boolean = true,
    val missedReminderEnabled: Boolean = true,
    val streakMilestoneNotifications: Boolean = true,
    val lastSyncedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CategoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "category",
    val colorHex: String = "#6C63FF",
    val isDefault: Boolean = false,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class HabitDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val iconKey: String = "check_circle",
    val colorHex: String = "#6C63FF",
    val startDate: String = "",
    val frequencyType: String = "daily",
    val targetDaysPerWeek: Long = 7L,
    val scheduledWeekdays: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
    val scheduledMonthDays: List<Long> = emptyList(),
    val reminderTimes: List<String> = emptyList(),
    val remindersEnabled: Boolean = false,
    val targetDurationMinutes: Long = 0L,
    val notes: String = "",
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CompletionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val dateIso: String = "",
    val completedAtEpochMs: Long = 0L,
    val note: String = "",
    val isCompleted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JournalDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateIso: String = "",
    val mood: String = "neutral",
    val content: String = "",
    val habitId: String = "",
    val deleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserAchievementDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val achievementKey: String = "",
    val unlockedAtEpochMs: Long = 0L,
    val pointsAwarded: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
