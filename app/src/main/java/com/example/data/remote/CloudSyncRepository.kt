package com.example.data.remote

import com.example.data.local.CategoryEntity
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserAchievementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDocRef(uid: String) = db.collection("users").document(uid)

    fun observeRemoteHabits(): Flow<List<HabitDoc>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/habits"
        emitAll(
            userDocRef(uid).collection("habits")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HabitDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun upsertUserProfile(user: UserEntity, settings: UserSettingsEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid)
        val path = docRef.path
        val commonFields = mapOf(
            "displayName" to user.displayName.take(100),
            "email" to user.email.take(200),
            "photoUrl" to user.photoUrl?.take(500),
            "totalPoints" to user.totalPoints.coerceAtLeast(0L),
            "level" to user.level.toLong().coerceAtLeast(1L),
            "themeMode" to settings.themeMode,
            "accentColorHex" to settings.accentColorHex,
            "firstDayOfWeek" to settings.firstDayOfWeek.toLong(),
            "use24HourFormat" to settings.use24HourFormat,
            "weeklyGoalPercent" to settings.weeklyGoalPercent.toLong(),
            "notificationsEnabled" to settings.notificationsEnabled,
            "missedReminderEnabled" to settings.missedReminderEnabled,
            "streakMilestoneNotifications" to settings.streakMilestoneNotifications,
            "lastSyncedAtEpochMs" to System.currentTimeMillis(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(commonFields).await()
            } else {
                val createPayload = commonFields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            throw e
        }
    }

    suspend fun getUserProfile(): UserProfileDoc? {
        val uid = requireUserId()
        val docRef = userDocRef(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun pushCategory(category: CategoryEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid).collection("categories").document(category.id)
        val fields = mapOf(
            "name" to category.name.take(50),
            "iconKey" to category.iconKey.take(40),
            "colorHex" to category.colorHex.take(12),
            "isDefault" to category.isDefault,
            "deleted" to category.deleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        upsertDocument(docRef, uid, fields)
    }

    suspend fun pushHabit(habit: HabitEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid).collection("habits").document(habit.id)
        val fields = mapOf(
            "name" to habit.name.take(100),
            "description" to habit.description.take(500),
            "categoryId" to habit.categoryId.take(128),
            "iconKey" to habit.iconKey.take(40),
            "colorHex" to habit.colorHex.take(12),
            "startDate" to habit.startDate.take(10),
            "frequencyType" to habit.frequencyType,
            "targetDaysPerWeek" to habit.targetDaysPerWeek.toLong().coerceIn(1L, 7L),
            "scheduledWeekdays" to habit.scheduledWeekdays.map { it.toLong() }.take(7),
            "scheduledMonthDays" to habit.scheduledMonthDays.map { it.toLong() }.take(31),
            "reminderTimes" to habit.reminderTimes.take(10),
            "remindersEnabled" to habit.remindersEnabled,
            "targetDurationMinutes" to habit.targetDurationMinutes.toLong().coerceIn(0L, 1440L),
            "notes" to habit.notes.take(1000),
            "isPaused" to habit.isPaused,
            "isArchived" to habit.isArchived,
            "deleted" to habit.deleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        upsertDocument(docRef, uid, fields)
    }

    suspend fun pushCompletion(completion: HabitCompletionEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid).collection("completions").document(completion.id)
        val fields = mapOf(
            "completedAtEpochMs" to completion.completedAtEpochMs,
            "note" to completion.note.take(500),
            "isCompleted" to completion.isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(fields).await()
            } else {
                val createPayload = fields + mapOf(
                    "userId" to uid,
                    "habitId" to completion.habitId,
                    "dateIso" to completion.dateIso,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun pushJournal(journal: DailyJournalEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid).collection("journals").document(journal.id)
        val fields = mapOf(
            "dateIso" to journal.dateIso.take(10),
            "mood" to journal.mood,
            "content" to journal.content.take(2000),
            "habitId" to journal.habitId.take(128),
            "deleted" to journal.deleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        upsertDocument(docRef, uid, fields)
    }

    suspend fun pushAchievement(achievement: UserAchievementEntity) {
        val uid = requireUserId()
        val docRef = userDocRef(uid).collection("achievements").document(achievement.id)
        val fields = mapOf(
            "unlockedAtEpochMs" to achievement.unlockedAtEpochMs,
            "pointsAwarded" to achievement.pointsAwarded.toLong(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(fields).await()
            } else {
                val createPayload = fields + mapOf(
                    "userId" to uid,
                    "achievementKey" to achievement.achievementKey,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun fetchAllRemoteData(): RemoteUserBundle {
        val uid = requireUserId()
        val baseRef = userDocRef(uid)
        val behavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        return try {
            val profile = baseRef.get().await().let {
                if (it.exists()) it.toObject(UserProfileDoc::class.java, behavior) else null
            }
            val categories = baseRef.collection("categories").get().await()
                .toObjects(CategoryDoc::class.java, behavior)
            val habits = baseRef.collection("habits").get().await()
                .toObjects(HabitDoc::class.java, behavior)
            val completions = baseRef.collection("completions").get().await()
                .toObjects(CompletionDoc::class.java, behavior)
            val journals = baseRef.collection("journals").get().await()
                .toObjects(JournalDoc::class.java, behavior)
            val achievements = baseRef.collection("achievements").get().await()
                .toObjects(UserAchievementDoc::class.java, behavior)

            RemoteUserBundle(
                profile = profile,
                categories = categories,
                habits = habits,
                completions = completions,
                journals = journals,
                achievements = achievements
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid")
            throw e
        }
    }

    suspend fun deleteAllUserCloudData() {
        val uid = requireUserId()
        val baseRef = userDocRef(uid)
        try {
            val subcollections = listOf("categories", "habits", "completions", "journals", "achievements")
            for (sub in subcollections) {
                val docs = baseRef.collection(sub).get().await().documents
                for (chunk in docs.chunked(400)) {
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            baseRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, baseRef.path)
            throw e
        }
    }

    private suspend fun upsertDocument(
        docRef: com.google.firebase.firestore.DocumentReference,
        uid: String,
        fields: Map<String, Any>
    ) {
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(fields).await()
            } else {
                val createPayload = fields + mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}

data class RemoteUserBundle(
    val profile: UserProfileDoc?,
    val categories: List<CategoryDoc>,
    val habits: List<HabitDoc>,
    val completions: List<CompletionDoc>,
    val journals: List<JournalDoc>,
    val achievements: List<UserAchievementDoc>
)
