package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.HabitlyDatabase
import com.example.domain.HabitScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationChannels {
    const val CHANNEL_REMINDERS = "habitly_reminders"
    const val CHANNEL_MISSED = "habitly_missed"
    const val CHANNEL_MILESTONES = "habitly_milestones"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_REMINDERS,
                        "Habit Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Scheduled reminders for your daily habits" },
                    NotificationChannel(
                        CHANNEL_MISSED,
                        "Missed Habit Nudges",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Evening check-in alerts for incomplete scheduled habits" },
                    NotificationChannel(
                        CHANNEL_MILESTONES,
                        "Streak Milestones",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply { description = "Celebrations when you hit streak milestones" }
                )
            )
        }
    }
}

class ReminderScheduler(private val context: Context) {

    fun ensureScheduledForUser(userId: String) {
        if (userId.isBlank()) return
        val workManager = WorkManager.getInstance(context)

        // Periodic reminder dispatcher (checks due reminders every 15 minutes with deduplication)
        val reminderWork = PeriodicWorkRequestBuilder<HabitReminderWorker>(15, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_USER_ID to userId))
            .build()
        workManager.enqueueUniquePeriodicWork(
            "habitly_periodic_reminders_$userId",
            ExistingPeriodicWorkPolicy.UPDATE,
            reminderWork
        )

        // Evening missed-habit check around 20:30 local time
        val now = LocalDateTime.now()
        var targetEvening = now.toLocalDate().atTime(20, 30)
        if (now.isAfter(targetEvening)) {
            targetEvening = targetEvening.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, targetEvening).toMinutes().coerceAtLeast(1L)
        val missedWork = PeriodicWorkRequestBuilder<MissedHabitCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_USER_ID to userId))
            .build()
        workManager.enqueueUniquePeriodicWork(
            "habitly_missed_check_$userId",
            ExistingPeriodicWorkPolicy.UPDATE,
            missedWork
        )

        // Also run an immediate one-time check so freshly configured reminders due now fire promptly
        val immediateCheck = OneTimeWorkRequestBuilder<HabitReminderWorker>()
            .setInputData(workDataOf(KEY_USER_ID to userId))
            .build()
        workManager.enqueueUniqueWork(
            "habitly_immediate_reminder_check",
            ExistingWorkPolicy.REPLACE,
            immediateCheck
        )
    }

    fun sendStreakMilestoneNotification(habitName: String, streakDays: Int) {
        if (!hasNotificationPermission(context)) return
        NotificationChannels.createAll(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            habitName.hashCode() + streakDays,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.CHANNEL_MILESTONES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$streakDays-Day Streak Milestone!")
            .setContentText("Incredible consistency on $habitName. Keep the momentum going!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(
            ("milestone_${habitName}_$streakDays").hashCode(),
            notification
        )
    }

    companion object {
        const val KEY_USER_ID = "user_id"

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }
    }
}

class HabitReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!ReminderScheduler.hasNotificationPermission(appContext)) return Result.success()
        val prefs = AppPreferencesDataStore(appContext).getSnapshot()
        if (!prefs.notificationsEnabled) return Result.success()

        val userId = inputData.getString(ReminderScheduler.KEY_USER_ID)?.takeIf { it.isNotBlank() }
            ?: prefs.activeUserId.takeIf { it.isNotBlank() }
            ?: return Result.success()

        val db = HabitlyDatabase.getInstance(appContext)
        val today = LocalDate.now()
        val todayIso = today.toString()
        val nowTime = LocalTime.now()

        val habits = db.habitDao().getActiveHabitsOnce(userId).associateBy { it.id }
        val completedIds = db.completionDao().getCompletedForDate(userId, todayIso).map { it.habitId }.toSet()
        val reminders = db.reminderDao().getEnabledReminders(userId)

        NotificationChannels.createAll(appContext)
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        for (reminder in reminders) {
            // Prevent duplicate notifications on the same calendar date
            if (reminder.lastNotifiedDateIso == todayIso) continue
            val habit = habits[reminder.habitId] ?: continue
            if (!HabitScheduleEngine.isScheduledForDate(habit, today)) continue
            if (completedIds.contains(habit.id)) continue

            val scheduledTime = runCatching { LocalTime.parse(reminder.timeHHmm) }.getOrNull() ?: continue
            val diffMinutes = Duration.between(scheduledTime, nowTime).toMinutes()
            if (diffMinutes in 0L..20L) {
                val pending = PendingIntent.getActivity(
                    appContext,
                    reminder.id.hashCode(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notif = NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_REMINDERS)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Time for ${habit.name}")
                    .setContentText(
                        habit.description.ifBlank { "Tap to check in and keep your streak alive." }
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .build()

                NotificationManagerCompat.from(appContext).notify(reminder.id.hashCode(), notif)
                db.reminderDao().markNotified(reminder.id, todayIso)
            }
        }
        return Result.success()
    }
}

class MissedHabitCheckWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!ReminderScheduler.hasNotificationPermission(appContext)) return Result.success()
        val prefs = AppPreferencesDataStore(appContext).getSnapshot()
        if (!prefs.notificationsEnabled || !prefs.missedReminderEnabled) return Result.success()

        val userId = inputData.getString(ReminderScheduler.KEY_USER_ID)?.takeIf { it.isNotBlank() }
            ?: prefs.activeUserId.takeIf { it.isNotBlank() }
            ?: return Result.success()

        val db = HabitlyDatabase.getInstance(appContext)
        val today = LocalDate.now()
        val todayIso = today.toString()

        val scheduledToday = db.habitDao().getActiveHabitsOnce(userId)
            .filter { HabitScheduleEngine.isScheduledForDate(it, today) }
        if (scheduledToday.isEmpty()) return Result.success()

        val completedIds = db.completionDao().getCompletedForDate(userId, todayIso).map { it.habitId }.toSet()
        val remaining = scheduledToday.filter { !completedIds.contains(it.id) }
        if (remaining.isEmpty()) return Result.success()

        NotificationChannels.createAll(appContext)
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            appContext,
            9091,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = if (remaining.size == 1) {
            "1 scheduled habit left today: ${remaining.first().name}"
        } else {
            "${remaining.size} scheduled habits still waiting today. Finish strong!"
        }

        val notif = NotificationCompat.Builder(appContext, NotificationChannels.CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Evening Habit Check-In")
            .setContentText(summary)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(("missed_$todayIso").hashCode(), notif)
        return Result.success()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED ||
            intent.action == Intent.ACTION_TIME_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = AppPreferencesDataStore(context).getSnapshot().activeUserId
                    if (userId.isNotBlank()) {
                        ReminderScheduler(context).ensureScheduledForUser(userId)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
