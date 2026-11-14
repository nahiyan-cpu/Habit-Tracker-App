package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId AND deleted = 0 ORDER BY isDefault DESC, name ASC")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getAllCategoriesForSync(userId: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId AND deleted = 0 ORDER BY createdAtEpochMs ASC")
    fun observeHabits(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId AND deleted = 0 LIMIT 1")
    fun observeHabit(habitId: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE userId = :userId AND deleted = 0 AND isArchived = 0 AND isPaused = 0 ORDER BY createdAtEpochMs ASC")
    suspend fun getActiveHabitsOnce(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE userId = :userId")
    suspend fun getAllHabitsForSync(userId: String): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface HabitCompletionDao {
    @Query("SELECT * FROM habit_completions WHERE userId = :userId ORDER BY dateIso DESC, completedAtEpochMs DESC")
    fun observeCompletions(userId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateIso DESC")
    fun observeCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND dateIso = :dateIso AND isCompleted = 1")
    suspend fun getCompletedForDate(userId: String, dateIso: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateIso = :dateIso LIMIT 1")
    suspend fun getCompletion(habitId: String, dateIso: String): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE userId = :userId")
    suspend fun getAllCompletionsForSync(userId: String): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(completion: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletions(completions: List<HabitCompletionEntity>)

    @Query("DELETE FROM habit_completions WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE userId = :userId")
    fun observeReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND enabled = 1")
    suspend fun getEnabledReminders(userId: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE habitId = :habitId")
    suspend fun getRemindersForHabit(habitId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE habitId = :habitId")
    suspend fun deleteRemindersForHabit(habitId: String)

    @Query("UPDATE reminders SET lastNotifiedDateIso = :dateIso WHERE id = :reminderId")
    suspend fun markNotified(reminderId: String, dateIso: String)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface DailyJournalDao {
    @Query("SELECT * FROM daily_journals WHERE userId = :userId AND deleted = 0 ORDER BY dateIso DESC, updatedAtEpochMs DESC")
    fun observeJournals(userId: String): Flow<List<DailyJournalEntity>>

    @Query("SELECT * FROM daily_journals WHERE userId = :userId")
    suspend fun getAllJournalsForSync(userId: String): List<DailyJournalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournal(journal: DailyJournalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournals(journals: List<DailyJournalEntity>)

    @Query("DELETE FROM daily_journals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY pointsReward ASC")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(achievements: List<AchievementEntity>)

    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY unlockedAtEpochMs DESC")
    fun observeUserAchievements(userId: String): Flow<List<UserAchievementEntity>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId")
    suspend fun getUserAchievementsOnce(userId: String): List<UserAchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserAchievement(userAchievement: UserAchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserAchievements(list: List<UserAchievementEntity>)

    @Query("DELETE FROM user_achievements WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun observeSettings(userId: String): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getSettings(userId: String): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettingsEntity)

    @Query("DELETE FROM user_settings WHERE userId = :userId")
    suspend fun deleteSettings(userId: String)
}
