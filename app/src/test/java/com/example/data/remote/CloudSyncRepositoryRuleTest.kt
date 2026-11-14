package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun pushAndFetchUserHabitAndCompletion_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice@habitly.test")
        val repo = CloudSyncRepository(firestore, auth)

        val user = UserEntity(id = uid, displayName = "Alice", email = "alice@habitly.test", totalPoints = 35L, level = 1)
        val settings = UserSettingsEntity(userId = uid)
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.upsertUserProfile(user, settings) }

        val habitId = "habit_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val habit = HabitEntity(
            id = habitId,
            userId = uid,
            name = "Hydrate",
            description = "Drink 2L water",
            categoryId = "cat_health",
            iconKey = "water_drop",
            colorHex = "#0EA5E9",
            startDate = "2026-11-14",
            frequencyType = "daily",
            targetDaysPerWeek = 7,
            scheduledWeekdays = listOf(1, 2, 3, 4, 5, 6, 7),
            reminderTimes = listOf("09:00"),
            remindersEnabled = true
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.pushHabit(habit) }

        val completion = HabitCompletionEntity(
            id = "${habitId}_2026-11-14",
            userId = uid,
            habitId = habitId,
            dateIso = "2026-11-14",
            completedAtEpochMs = System.currentTimeMillis(),
            note = "Felt energized",
            isCompleted = true
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.pushCompletion(completion) }

        val journal = DailyJournalEntity(
            id = "j_${UUID.randomUUID().toString().replace("-", "").take(10)}",
            userId = uid,
            dateIso = "2026-11-14",
            mood = "motivated",
            content = "Great first day of Habitly!"
        )
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.pushJournal(journal) }

        val bundle = withTimeout(DEFAULT_TIMEOUT_MS) { repo.fetchAllRemoteData() }
        assertTrue(bundle.habits.any { it.id == habitId && it.name == "Hydrate" })
        assertTrue(bundle.completions.any { it.habitId == habitId && it.isCompleted })
        assertTrue(bundle.journals.any { it.content.contains("Great first day") })
    }

    @Test
    fun observeRemoteHabits_unauthenticatedUser_throwsIllegalState() = runBlocking {
        auth.signOut()
        val repo = CloudSyncRepository(firestore, auth)
        var failed = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeRemoteHabits().first() }
        } catch (e: IllegalStateException) {
            failed = true
        }
        assertTrue("Expected unauthenticated check to block query", failed)
    }

    @Test
    fun crossUserAccess_readingAnotherUsersHabit_deniedByRules() = runBlocking {
        val aliceUid = signInTestUser("alice_owner@habitly.test")
        val aliceRepo = CloudSyncRepository(firestore, auth)
        val habitId = "habit_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.pushHabit(
                HabitEntity(
                    id = habitId,
                    userId = aliceUid,
                    name = "Private Habit",
                    categoryId = "cat_health",
                    iconKey = "favorite",
                    colorHex = "#6C63FF",
                    startDate = "2026-11-14",
                    frequencyType = "daily"
                )
            )
        }

        signInTestUser("bob_intruder@habitly.test")
        var deniedCode: FirebaseFirestoreException.Code? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).collection("habits").document(habitId).get().await()
            }
        } catch (e: Exception) {
            deniedCode = e.unwrapFirestoreException()?.code
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, deniedCode)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
