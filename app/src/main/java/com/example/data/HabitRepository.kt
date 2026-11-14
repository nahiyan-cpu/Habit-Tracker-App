package com.example.data

import android.content.Context
import com.example.data.local.AchievementEntity
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.CategoryEntity
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitlyDatabase
import com.example.data.local.ReminderEntity
import com.example.data.local.UserAchievementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.remote.CloudSyncRepository
import com.example.domain.AchievementEngine
import com.example.domain.HabitCatalog
import com.example.domain.HabitScheduleEngine
import com.example.domain.QuotesProvider
import com.example.notifications.ReminderScheduler
import com.example.widget.HabitlyGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

sealed class SyncResult {
    data class Success(val syncedAtEpochMs: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class HabitRepository(
    private val context: Context,
    private val db: HabitlyDatabase,
    private val prefs: AppPreferencesDataStore,
    private val cloudSync: CloudSyncRepository,
    private val reminderScheduler: ReminderScheduler
) {
    private val userDao = db.userDao()
    private val categoryDao = db.categoryDao()
    private val habitDao = db.habitDao()
    private val completionDao = db.completionDao()
    private val reminderDao = db.reminderDao()
    private val journalDao = db.journalDao()
    private val achievementDao = db.achievementDao()
    private val settingsDao = db.settingsDao()

    fun observeUser(userId: String): Flow<UserEntity?> = userDao.observeUser(userId)
    fun observeCategories(userId: String): Flow<List<CategoryEntity>> = categoryDao.observeCategories(userId)
    fun observeHabits(userId: String): Flow<List<HabitEntity>> = habitDao.observeHabits(userId)
    fun observeHabit(habitId: String): Flow<HabitEntity?> = habitDao.observeHabit(habitId)
    fun observeCompletions(userId: String): Flow<List<HabitCompletionEntity>> = completionDao.observeCompletions(userId)
    fun observeCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>> = completionDao.observeCompletionsForHabit(habitId)
    fun observeJournals(userId: String): Flow<List<DailyJournalEntity>> = journalDao.observeJournals(userId)
    fun observeAllAchievements(): Flow<List<AchievementEntity>> = achievementDao.observeAllAchievements()
    fun observeUserAchievements(userId: String): Flow<List<UserAchievementEntity>> = achievementDao.observeUserAchievements(userId)
    fun observeSettings(userId: String): Flow<UserSettingsEntity?> = settingsDao.observeSettings(userId)

    suspend fun ensureUserInitialized(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ) {
        prefs.setActiveUserId(userId)
        achievementDao.upsertAchievements(AchievementEngine.predefinedAchievements)

        val existingUser = userDao.getUser(userId)
        if (existingUser == null) {
            userDao.upsertUser(
                UserEntity(
                    id = userId,
                    displayName = displayName.ifBlank { "Habitly Explorer" },
                    email = email,
                    photoUrl = photoUrl,
                    totalPoints = 0L,
                    level = 1
                )
            )
        } else if (existingUser.displayName != displayName || existingUser.photoUrl != photoUrl) {
            userDao.upsertUser(
                existingUser.copy(
                    displayName = displayName.ifBlank { existingUser.displayName },
                    email = email.ifBlank { existingUser.email },
                    photoUrl = photoUrl ?: existingUser.photoUrl,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }

        val existingCats = categoryDao.getAllCategoriesForSync(userId)
        if (existingCats.isEmpty()) {
            categoryDao.upsertCategories(HabitCatalog.defaultCategories(userId))
        }

        val existingSettings = settingsDao.getSettings(userId)
        if (existingSettings == null) {
            val snap = prefs.getSnapshot()
            settingsDao.upsertSettings(
                UserSettingsEntity(
                    userId = userId,
                    themeMode = snap.themeMode,
                    accentColorHex = snap.accentColorHex,
                    firstDayOfWeek = snap.firstDayOfWeek,
                    use24HourFormat = snap.use24HourFormat,
                    weeklyGoalPercent = snap.weeklyGoalPercent,
                    notificationsEnabled = snap.notificationsEnabled,
                    missedReminderEnabled = snap.missedReminderEnabled,
                    streakMilestoneNotifications = snap.streakMilestoneNotifications
                )
            )
        }

        reminderScheduler.ensureScheduledForUser(userId)
        refreshWidgets()
    }

    suspend fun saveCategory(userId: String, name: String, iconKey: String, colorHex: String) {
        val cat = CategoryEntity(
            id = "cat_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            userId = userId,
            name = name.trim().take(50),
            iconKey = iconKey,
            colorHex = colorHex,
            isDefault = false,
            deleted = false,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        categoryDao.upsertCategory(cat)
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushCategory(cat) } }
    }

    suspend fun saveHabit(habit: HabitEntity) {
        val now = System.currentTimeMillis()
        val finalHabit = habit.copy(updatedAtEpochMs = now)
        habitDao.upsertHabit(finalHabit)

        // Sync reminders table
        reminderDao.deleteRemindersForHabit(finalHabit.id)
        if (finalHabit.remindersEnabled && finalHabit.reminderTimes.isNotEmpty() && !finalHabit.deleted && !finalHabit.isArchived) {
            val reminders = finalHabit.reminderTimes.distinct().mapIndexed { idx, time ->
                ReminderEntity(
                    id = "${finalHabit.id}_rem_$idx",
                    userId = finalHabit.userId,
                    habitId = finalHabit.id,
                    timeHHmm = time,
                    enabled = true
                )
            }
            reminderDao.upsertReminders(reminders)
        }

        reminderScheduler.ensureScheduledForUser(finalHabit.userId)
        refreshWidgets()
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushHabit(finalHabit) } }
    }

    suspend fun togglePauseHabit(habit: HabitEntity) {
        saveHabit(habit.copy(isPaused = !habit.isPaused))
    }

    suspend fun toggleArchiveHabit(habit: HabitEntity) {
        saveHabit(habit.copy(isArchived = !habit.isArchived))
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        saveHabit(habit.copy(deleted = true))
    }

    suspend fun toggleCompletion(
        userId: String,
        habitId: String,
        dateIso: String,
        note: String = ""
    ): List<AchievementEntity> {
        val id = "${habitId}_${dateIso}"
        val existing = completionDao.getCompletion(habitId, dateIso)
        val now = System.currentTimeMillis()
        val nextCompleted = if (existing == null) true else !existing.isCompleted
        val entity = HabitCompletionEntity(
            id = id,
            userId = userId,
            habitId = habitId,
            dateIso = dateIso,
            completedAtEpochMs = now,
            note = note.ifBlank { existing?.note ?: "" },
            isCompleted = nextCompleted,
            updatedAtEpochMs = now
        )
        completionDao.upsertCompletion(entity)

        val newlyUnlocked = evaluateAndPersistRewards(userId)
        if (nextCompleted) {
            checkAndNotifyStreakMilestone(userId, habitId)
        }
        refreshWidgets()
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushCompletion(entity) } }
        return newlyUnlocked
    }

    suspend fun updateCompletionNote(userId: String, habitId: String, dateIso: String, note: String) {
        val existing = completionDao.getCompletion(habitId, dateIso)
        val now = System.currentTimeMillis()
        val entity = existing?.copy(note = note.take(500), updatedAtEpochMs = now)
            ?: HabitCompletionEntity(
                id = "${habitId}_${dateIso}",
                userId = userId,
                habitId = habitId,
                dateIso = dateIso,
                completedAtEpochMs = now,
                note = note.take(500),
                isCompleted = false,
                updatedAtEpochMs = now
            )
        completionDao.upsertCompletion(entity)
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushCompletion(entity) } }
    }

    suspend fun saveJournalEntry(entry: DailyJournalEntity): List<AchievementEntity> {
        val now = System.currentTimeMillis()
        val finalEntry = entry.copy(updatedAtEpochMs = now)
        journalDao.upsertJournal(finalEntry)
        val newlyUnlocked = evaluateAndPersistRewards(finalEntry.userId)
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushJournal(finalEntry) } }
        return newlyUnlocked
    }

    suspend fun deleteJournalEntry(entry: DailyJournalEntity) {
        val deletedEntry = entry.copy(deleted = true, updatedAtEpochMs = System.currentTimeMillis())
        journalDao.upsertJournal(deletedEntry)
        CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushJournal(deletedEntry) } }
    }

    private suspend fun checkAndNotifyStreakMilestone(userId: String, habitId: String) {
        val settings = settingsDao.getSettings(userId)
        if (settings != null && (!settings.notificationsEnabled || !settings.streakMilestoneNotifications)) return
        val habit = habitDao.getHabitById(habitId) ?: return
        val completions = completionDao.getAllCompletionsForSync(userId)
        val stats = HabitScheduleEngine.calculateHabitStats(habit, completions)
        if (stats.currentStreak in setOf(3, 7, 14, 30, 100)) {
            reminderScheduler.sendStreakMilestoneNotification(habit.name, stats.currentStreak)
        }
    }

    suspend fun evaluateAndPersistRewards(userId: String): List<AchievementEntity> {
        val habits = habitDao.getAllHabitsForSync(userId)
        val completions = completionDao.getAllCompletionsForSync(userId)
        val journals = journalDao.getAllJournalsForSync(userId)
        val existingUnlocked = achievementDao.getUserAchievementsOnce(userId)
        val existingKeys = existingUnlocked.map { it.achievementKey }.toSet()

        val newlyUnlocked = AchievementEngine.evaluateNewUnlocks(
            userId = userId,
            habits = habits,
            completions = completions,
            journals = journals,
            alreadyUnlockedKeys = existingKeys
        )
        if (newlyUnlocked.isNotEmpty()) {
            achievementDao.upsertUserAchievements(newlyUnlocked)
            newlyUnlocked.forEach { ua ->
                CoroutineScope(Dispatchers.IO).launch { runCatching { cloudSync.pushAchievement(ua) } }
            }
        }

        val allUserAchievements = existingUnlocked + newlyUnlocked
        val totalPoints = AchievementEngine.calculateTotalPoints(completions, allUserAchievements)
        val level = AchievementEngine.calculateLevel(totalPoints)
        val user = userDao.getUser(userId)
        if (user != null) {
            userDao.upsertUser(
                user.copy(
                    totalPoints = totalPoints,
                    level = level,
                    updatedAtEpochMs = System.currentTimeMillis()
                )
            )
        }

        val unlockedKeys = newlyUnlocked.map { it.achievementKey }.toSet()
        return AchievementEngine.predefinedAchievements.filter { unlockedKeys.contains(it.key) }
    }

    suspend fun updateSettings(settings: UserSettingsEntity) {
        settingsDao.upsertSettings(settings.copy(updatedAtEpochMs = System.currentTimeMillis()))
        prefs.setThemeMode(settings.themeMode)
        prefs.setAccentColorHex(settings.accentColorHex)
        prefs.setFirstDayOfWeek(settings.firstDayOfWeek)
        prefs.setUse24HourFormat(settings.use24HourFormat)
        prefs.setWeeklyGoalPercent(settings.weeklyGoalPercent)
        prefs.setNotificationsEnabled(settings.notificationsEnabled)
        prefs.setMissedReminderEnabled(settings.missedReminderEnabled)
        prefs.setStreakMilestoneNotifications(settings.streakMilestoneNotifications)
        reminderScheduler.ensureScheduledForUser(settings.userId)
    }

    suspend fun getNextQuote(): Pair<Int, com.example.domain.MotivationalQuote> {
        val snap = prefs.getSnapshot()
        val next = QuotesProvider.nextQuote(snap.lastQuoteIndex)
        prefs.setLastQuoteIndex(next.first)
        return next
    }

    suspend fun syncWithCloud(userId: String): SyncResult {
        return try {
            val bundle = cloudSync.fetchAllRemoteData()

            // 1. Merge Categories (last-write-wins by updatedAtEpochMs)
            val localCategories = categoryDao.getAllCategoriesForSync(userId).associateBy { it.id }.toMutableMap()
            bundle.categories.forEach { remote ->
                val remoteUpdated = remote.updatedAt?.toDate()?.time ?: 0L
                val local = localCategories[remote.id]
                if (local == null || remoteUpdated > local.updatedAtEpochMs) {
                    localCategories[remote.id] = CategoryEntity(
                        id = remote.id,
                        userId = userId,
                        name = remote.name,
                        iconKey = remote.iconKey,
                        colorHex = remote.colorHex,
                        isDefault = remote.isDefault,
                        deleted = remote.deleted,
                        updatedAtEpochMs = remoteUpdated
                    )
                }
            }
            categoryDao.upsertCategories(localCategories.values.toList())
            localCategories.values.forEach { cat ->
                val remoteUpdated = bundle.categories.firstOrNull { it.id == cat.id }?.updatedAt?.toDate()?.time ?: -1L
                if (cat.updatedAtEpochMs > remoteUpdated) {
                    cloudSync.pushCategory(cat)
                }
            }

            // 2. Merge Habits
            val localHabits = habitDao.getAllHabitsForSync(userId).associateBy { it.id }.toMutableMap()
            bundle.habits.forEach { remote ->
                val remoteUpdated = remote.updatedAt?.toDate()?.time ?: 0L
                val local = localHabits[remote.id]
                if (local == null || remoteUpdated > local.updatedAtEpochMs) {
                    localHabits[remote.id] = HabitEntity(
                        id = remote.id,
                        userId = userId,
                        name = remote.name,
                        description = remote.description,
                        categoryId = remote.categoryId,
                        iconKey = remote.iconKey,
                        colorHex = remote.colorHex,
                        startDate = remote.startDate,
                        frequencyType = remote.frequencyType,
                        targetDaysPerWeek = remote.targetDaysPerWeek.toInt(),
                        scheduledWeekdays = remote.scheduledWeekdays.map { it.toInt() },
                        scheduledMonthDays = remote.scheduledMonthDays.map { it.toInt() },
                        reminderTimes = remote.reminderTimes,
                        remindersEnabled = remote.remindersEnabled,
                        targetDurationMinutes = remote.targetDurationMinutes.toInt(),
                        notes = remote.notes,
                        isPaused = remote.isPaused,
                        isArchived = remote.isArchived,
                        deleted = remote.deleted,
                        createdAtEpochMs = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAtEpochMs = remoteUpdated
                    )
                }
            }
            habitDao.upsertHabits(localHabits.values.toList())
            localHabits.values.forEach { h ->
                val remoteUpdated = bundle.habits.firstOrNull { it.id == h.id }?.updatedAt?.toDate()?.time ?: -1L
                if (h.updatedAtEpochMs > remoteUpdated) {
                    cloudSync.pushHabit(h)
                }
            }

            // 3. Merge Completions (deduplicated by deterministic habitId_dateIso ID)
            val localCompletions = completionDao.getAllCompletionsForSync(userId).associateBy { it.id }.toMutableMap()
            bundle.completions.forEach { remote ->
                val remoteUpdated = remote.updatedAt?.toDate()?.time ?: 0L
                val local = localCompletions[remote.id]
                if (local == null || remoteUpdated > local.updatedAtEpochMs) {
                    localCompletions[remote.id] = HabitCompletionEntity(
                        id = remote.id,
                        userId = userId,
                        habitId = remote.habitId,
                        dateIso = remote.dateIso,
                        completedAtEpochMs = remote.completedAtEpochMs,
                        note = remote.note,
                        isCompleted = remote.isCompleted,
                        updatedAtEpochMs = remoteUpdated
                    )
                }
            }
            completionDao.upsertCompletions(localCompletions.values.toList())
            localCompletions.values.forEach { c ->
                val remoteUpdated = bundle.completions.firstOrNull { it.id == c.id }?.updatedAt?.toDate()?.time ?: -1L
                if (c.updatedAtEpochMs > remoteUpdated) {
                    cloudSync.pushCompletion(c)
                }
            }

            // 4. Merge Journals
            val localJournals = journalDao.getAllJournalsForSync(userId).associateBy { it.id }.toMutableMap()
            bundle.journals.forEach { remote ->
                val remoteUpdated = remote.updatedAt?.toDate()?.time ?: 0L
                val local = localJournals[remote.id]
                if (local == null || remoteUpdated > local.updatedAtEpochMs) {
                    localJournals[remote.id] = DailyJournalEntity(
                        id = remote.id,
                        userId = userId,
                        dateIso = remote.dateIso,
                        mood = remote.mood,
                        content = remote.content,
                        habitId = remote.habitId,
                        deleted = remote.deleted,
                        createdAtEpochMs = remote.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                        updatedAtEpochMs = remoteUpdated
                    )
                }
            }
            journalDao.upsertJournals(localJournals.values.toList())
            localJournals.values.forEach { j ->
                val remoteUpdated = bundle.journals.firstOrNull { it.id == j.id }?.updatedAt?.toDate()?.time ?: -1L
                if (j.updatedAtEpochMs > remoteUpdated) {
                    cloudSync.pushJournal(j)
                }
            }

            // 5. Merge Achievements (union of unlocked achievements)
            val localAchievements = achievementDao.getUserAchievementsOnce(userId).associateBy { it.id }.toMutableMap()
            bundle.achievements.forEach { remote ->
                if (!localAchievements.containsKey(remote.id)) {
                    localAchievements[remote.id] = UserAchievementEntity(
                        id = remote.id,
                        userId = userId,
                        achievementKey = remote.achievementKey,
                        unlockedAtEpochMs = remote.unlockedAtEpochMs,
                        pointsAwarded = remote.pointsAwarded.toInt(),
                        updatedAtEpochMs = remote.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
            }
            achievementDao.upsertUserAchievements(localAchievements.values.toList())
            localAchievements.values.forEach { a ->
                if (bundle.achievements.none { it.id == a.id }) {
                    cloudSync.pushAchievement(a)
                }
            }

            // 6. Push profile & settings
            evaluateAndPersistRewards(userId)
            val user = userDao.getUser(userId)
            val settings = settingsDao.getSettings(userId) ?: UserSettingsEntity(userId = userId)
            if (user != null) {
                cloudSync.upsertUserProfile(user, settings)
            }

            val now = System.currentTimeMillis()
            prefs.setLastSyncEpochMs(now)
            refreshWidgets()
            SyncResult.Success(now)
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "Cloud sync failed. Offline data remains safely saved.")
        }
    }

    suspend fun deleteAllUserData(userId: String) {
        runCatching { cloudSync.deleteAllUserCloudData() }
        reminderDao.deleteAllForUser(userId)
        completionDao.deleteAllForUser(userId)
        habitDao.deleteAllForUser(userId)
        categoryDao.deleteAllForUser(userId)
        journalDao.deleteAllForUser(userId)
        achievementDao.deleteAllForUser(userId)
        settingsDao.deleteSettings(userId)
        userDao.deleteUser(userId)
        prefs.setActiveUserId("")
        refreshWidgets()
    }

    suspend fun refreshWidgets() {
        runCatching { HabitlyGlanceWidget.refreshAll(context) }
    }

    suspend fun getExportJson(userId: String): String {
        val user = userDao.getUser(userId)
        val categories = categoryDao.observeCategories(userId).first()
        val habits = habitDao.observeHabits(userId).first()
        val completions = completionDao.observeCompletions(userId).first()
        val journals = journalDao.observeJournals(userId).first()
        val achievements = achievementDao.observeUserAchievements(userId).first()
        return com.example.domain.DataExportHelper.buildExportJson(
            userId = userId,
            displayName = user?.displayName ?: "User",
            email = user?.email ?: "",
            categories = categories,
            habits = habits,
            completions = completions,
            journals = journals,
            achievements = achievements
        )
    }
}
