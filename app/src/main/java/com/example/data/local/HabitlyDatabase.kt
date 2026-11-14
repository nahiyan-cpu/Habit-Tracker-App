package com.example.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val totalPoints: Long = 0L,
    val level: Int = 1,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val deleted: Boolean = false,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habits",
    indices = [Index(value = ["userId"]), Index(value = ["categoryId"])]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String = "",
    val categoryId: String,
    val iconKey: String,
    val colorHex: String,
    val startDate: String, // YYYY-MM-DD
    val frequencyType: String, // "daily", "days_per_week", "weekly", "monthly"
    val targetDaysPerWeek: Int = 7,
    val scheduledWeekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val scheduledMonthDays: List<Int> = emptyList(),
    val reminderTimes: List<String> = emptyList(), // "HH:mm"
    val remindersEnabled: Boolean = false,
    val targetDurationMinutes: Int = 0,
    val notes: String = "",
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val deleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId", "dateIso"], unique = true),
        Index(value = ["userId", "dateIso"])
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String, // Deterministic: "${habitId}_${dateIso}"
    val userId: String,
    val habitId: String,
    val dateIso: String, // YYYY-MM-DD
    val completedAtEpochMs: Long,
    val note: String = "",
    val isCompleted: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val habitId: String,
    val timeHHmm: String,
    val enabled: Boolean = true,
    val lastNotifiedDateIso: String? = null
)

@Entity(
    tableName = "daily_journals",
    indices = [Index(value = ["userId", "dateIso"])]
)
data class DailyJournalEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateIso: String,
    val mood: String, // "happy", "motivated", "neutral", "tired", "sad"
    val content: String,
    val habitId: String = "",
    val deleted: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val pointsReward: Int,
    val requirementType: String, // "total_completions", "streak_days", "journal_entries"
    val requirementValue: Int
)

@Entity(
    tableName = "user_achievements",
    indices = [Index(value = ["userId", "achievementKey"], unique = true)]
)
data class UserAchievementEntity(
    @PrimaryKey val id: String, // "${userId}_${achievementKey}"
    val userId: String,
    val achievementKey: String,
    val unlockedAtEpochMs: Long,
    val pointsAwarded: Int,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val userId: String,
    val themeMode: String = "system",
    val accentColorHex: String = "#6C63FF",
    val firstDayOfWeek: Int = 1,
    val use24HourFormat: Boolean = false,
    val weeklyGoalPercent: Int = 80,
    val notificationsEnabled: Boolean = true,
    val missedReminderEnabled: Boolean = true,
    val streakMilestoneNotifications: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

// Future-ready entities (Section 20)
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val durationDays: Int,
    val startDate: String,
    val participantCount: Int = 1
)

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val friendName: String,
    val friendEmail: String,
    val status: String = "pending"
)

class RoomTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromIntList(list: List<Int>): String = json.encodeToString(list)

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isBlank()) emptyList() else runCatching { json.decodeFromString<List<Int>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun fromStringList(list: List<String>): String = json.encodeToString(list)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
}

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        ReminderEntity::class,
        DailyJournalEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class,
        UserSettingsEntity::class,
        ChallengeEntity::class,
        FriendshipEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class HabitlyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun completionDao(): HabitCompletionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun journalDao(): DailyJournalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun settingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: HabitlyDatabase? = null

        fun getInstance(context: Context): HabitlyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitlyDatabase::class.java,
                    "habitly_room.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
