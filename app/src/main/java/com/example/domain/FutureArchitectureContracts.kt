package com.example.domain

import com.example.data.local.ChallengeEntity
import com.example.data.local.FriendshipEntity
import kotlinx.coroutines.flow.Flow

/**
 * Extensible architecture contracts for Section 20 (Future Development):
 * Accountability Partners, Group Challenges, and opt-in AI Habit Advisor.
 * Private journal entries are explicitly excluded from sharing and AI prompts unless consented.
 */
interface AccountabilityPartnerService {
    fun observeFriends(userId: String): Flow<List<FriendshipEntity>>
    suspend fun invitePartner(userId: String, partnerEmail: String)
    suspend fun shareHabitProgress(userId: String, habitId: String, partnerId: String)
}

interface GroupChallengeService {
    fun observeChallenges(): Flow<List<ChallengeEntity>>
    suspend fun createChallenge(challenge: ChallengeEntity)
    suspend fun joinChallenge(challengeId: String, userId: String)
}

data class HabitSuggestion(
    val title: String,
    val rationale: String,
    val suggestedCategory: String
)

interface AiHabitAdvisor {
    suspend fun suggestHabits(userId: String, includePrivateJournals: Boolean = false): List<HabitSuggestion>
}
