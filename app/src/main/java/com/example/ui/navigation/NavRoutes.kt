package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    // Bottom Navigation Top-Level Destinations
    @Serializable data object Home : AppRoute
    @Serializable data object Habits : AppRoute
    @Serializable data object Statistics : AppRoute
    @Serializable data object Calendar : AppRoute
    @Serializable data object Profile : AppRoute

    // Secondary / Detail / Modal Routes
    @Serializable data object AddHabit : AppRoute
    @Serializable data class EditHabit(val habitId: String) : AppRoute
    @Serializable data class HabitDetails(val habitId: String) : AppRoute
    @Serializable data object DailyJournal : AppRoute
    @Serializable data object Achievements : AppRoute
    @Serializable data object NotificationSettings : AppRoute
    @Serializable data object AppSettings : AppRoute
    @Serializable data object BackupAndSync : AppRoute
}

sealed interface AuthRoute {
    @Serializable data object Login : AuthRoute
    @Serializable data object Register : AuthRoute
    @Serializable data object ForgotPassword : AuthRoute
}
