package com.example.di

import android.app.Application
import android.content.Context
import com.example.R
import com.example.data.HabitRepository
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.HabitlyDatabase
import com.example.data.remote.CloudSyncRepository
import com.example.notifications.NotificationChannels
import com.example.notifications.ReminderScheduler
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val databaseId: String = appContext.getString(R.string.firestore_database_id)
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(databaseId)
    val auth = Firebase.auth

    val roomDatabase: HabitlyDatabase = HabitlyDatabase.getInstance(appContext)
    val preferencesDataStore: AppPreferencesDataStore = AppPreferencesDataStore(appContext)
    val cloudSyncRepository: CloudSyncRepository = CloudSyncRepository(firestore, auth)
    val reminderScheduler: ReminderScheduler = ReminderScheduler(appContext)

    val habitRepository: HabitRepository = HabitRepository(
        context = appContext,
        db = roomDatabase,
        prefs = preferencesDataStore,
        cloudSync = cloudSyncRepository,
        reminderScheduler = reminderScheduler
    )
}

class HabitlyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
    }
}
