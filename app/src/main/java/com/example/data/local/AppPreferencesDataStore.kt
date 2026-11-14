package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.habitlyDataStore: DataStore<Preferences> by preferencesDataStore(name = "habitly_prefs")

data class AppPreferencesState(
    val onboardingCompleted: Boolean = false,
    val themeMode: String = "system", // "system", "light", "dark"
    val accentColorHex: String = "#6C63FF",
    val firstDayOfWeek: Int = 1, // 1 = Monday, 7 = Sunday
    val use24HourFormat: Boolean = false,
    val weeklyGoalPercent: Int = 80,
    val notificationsEnabled: Boolean = true,
    val missedReminderEnabled: Boolean = true,
    val streakMilestoneNotifications: Boolean = true,
    val lastQuoteIndex: Int = -1,
    val lastSyncEpochMs: Long = 0L,
    val activeUserId: String = "",
    val localModeEnabled: Boolean = false
)

class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_HEX = stringPreferencesKey("accent_color_hex")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val WEEKLY_GOAL_PERCENT = intPreferencesKey("weekly_goal_percent")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MISSED_REMINDER_ENABLED = booleanPreferencesKey("missed_reminder_enabled")
        val STREAK_MILESTONE_NOTIFICATIONS = booleanPreferencesKey("streak_milestone_notifications")
        val LAST_QUOTE_INDEX = intPreferencesKey("last_quote_index")
        val LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val LOCAL_MODE_ENABLED = booleanPreferencesKey("local_mode_enabled")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.habitlyDataStore.data.map { prefs ->
        AppPreferencesState(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            accentColorHex = prefs[Keys.ACCENT_COLOR_HEX] ?: "#6C63FF",
            firstDayOfWeek = prefs[Keys.FIRST_DAY_OF_WEEK] ?: 1,
            use24HourFormat = prefs[Keys.USE_24_HOUR_FORMAT] ?: false,
            weeklyGoalPercent = prefs[Keys.WEEKLY_GOAL_PERCENT] ?: 80,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            missedReminderEnabled = prefs[Keys.MISSED_REMINDER_ENABLED] ?: true,
            streakMilestoneNotifications = prefs[Keys.STREAK_MILESTONE_NOTIFICATIONS] ?: true,
            lastQuoteIndex = prefs[Keys.LAST_QUOTE_INDEX] ?: -1,
            lastSyncEpochMs = prefs[Keys.LAST_SYNC_EPOCH_MS] ?: 0L,
            activeUserId = prefs[Keys.ACTIVE_USER_ID] ?: "",
            localModeEnabled = prefs[Keys.LOCAL_MODE_ENABLED] ?: false
        )
    }

    suspend fun getSnapshot(): AppPreferencesState = preferencesFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.habitlyDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setThemeMode(mode: String) {
        context.habitlyDataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentColorHex(hex: String) {
        context.habitlyDataStore.edit { it[Keys.ACCENT_COLOR_HEX] = hex }
    }

    suspend fun setFirstDayOfWeek(day: Int) {
        context.habitlyDataStore.edit { it[Keys.FIRST_DAY_OF_WEEK] = day.coerceIn(1, 7) }
    }

    suspend fun setUse24HourFormat(use24: Boolean) {
        context.habitlyDataStore.edit { it[Keys.USE_24_HOUR_FORMAT] = use24 }
    }

    suspend fun setWeeklyGoalPercent(percent: Int) {
        context.habitlyDataStore.edit { it[Keys.WEEKLY_GOAL_PERCENT] = percent.coerceIn(10, 100) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.habitlyDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setMissedReminderEnabled(enabled: Boolean) {
        context.habitlyDataStore.edit { it[Keys.MISSED_REMINDER_ENABLED] = enabled }
    }

    suspend fun setStreakMilestoneNotifications(enabled: Boolean) {
        context.habitlyDataStore.edit { it[Keys.STREAK_MILESTONE_NOTIFICATIONS] = enabled }
    }

    suspend fun setLastQuoteIndex(index: Int) {
        context.habitlyDataStore.edit { it[Keys.LAST_QUOTE_INDEX] = index }
    }

    suspend fun setLastSyncEpochMs(epochMs: Long) {
        context.habitlyDataStore.edit { it[Keys.LAST_SYNC_EPOCH_MS] = epochMs }
    }

    suspend fun setActiveUserId(userId: String) {
        context.habitlyDataStore.edit { it[Keys.ACTIVE_USER_ID] = userId }
    }

    suspend fun setLocalModeEnabled(enabled: Boolean) {
        context.habitlyDataStore.edit { it[Keys.LOCAL_MODE_ENABLED] = enabled }
    }
}
