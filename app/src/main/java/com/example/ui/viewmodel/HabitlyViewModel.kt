package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HabitRepository
import com.example.data.SyncResult
import com.example.data.local.AchievementEntity
import com.example.data.local.AppPreferencesDataStore
import com.example.data.local.AppPreferencesState
import com.example.data.local.CategoryEntity
import com.example.data.local.DailyJournalEntity
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.UserAchievementEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.example.domain.DataExportHelper
import com.example.domain.MotivationalQuote
import com.example.domain.QuotesProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Syncing : SyncUiState
    data class Success(val syncedAtEpochMs: Long) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

data class HabitlyMainUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val completions: List<HabitCompletionEntity> = emptyList(),
    val journals: List<DailyJournalEntity> = emptyList(),
    val allAchievements: List<AchievementEntity> = emptyList(),
    val userAchievements: List<UserAchievementEntity> = emptyList(),
    val settings: UserSettingsEntity = UserSettingsEntity(userId = ""),
    val quote: MotivationalQuote = QuotesProvider.quotes.first()
)

class AppPreferencesViewModel(
    private val prefsDataStore: AppPreferencesDataStore
) : ViewModel() {

    val preferences: StateFlow<AppPreferencesState> = prefsDataStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppPreferencesState()
        )

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsDataStore.setOnboardingCompleted(true)
        }
    }

    fun setLocalModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsDataStore.setLocalModeEnabled(enabled)
        }
    }
}

class MainViewModel(
    private val repository: HabitRepository,
    private val prefsDataStore: AppPreferencesDataStore,
    val currentUserId: String,
    private val initialDisplayName: String,
    private val initialEmail: String,
    private val initialPhotoUrl: String?
) : ViewModel() {

    private val _quote = MutableStateFlow(QuotesProvider.quotes.first())
    private val _newlyUnlockedAchievement = MutableStateFlow<AchievementEntity?>(null)
    val newlyUnlockedAchievement: StateFlow<AchievementEntity?> = _newlyUnlockedAchievement.asStateFlow()

    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _lastSyncEpochMs = MutableStateFlow(0L)
    val lastSyncEpochMs: StateFlow<Long> = _lastSyncEpochMs.asStateFlow()

    private val coreDataFlow = combine(
        repository.observeUser(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeCompletions(currentUserId),
        repository.observeJournals(currentUserId)
    ) { user, categories, habits, completions, journals ->
        CoreBundle(user, categories, habits, completions, journals)
    }

    private val metaDataFlow = combine(
        repository.observeAllAchievements(),
        repository.observeUserAchievements(currentUserId),
        repository.observeSettings(currentUserId),
        _quote
    ) { allAch, userAch, settings, quote ->
        MetaBundle(allAch, userAch, settings, quote)
    }

    val uiState: StateFlow<HabitlyMainUiState> = combine(coreDataFlow, metaDataFlow) { core, meta ->
        HabitlyMainUiState(
            isLoading = false,
            user = core.user,
            categories = core.categories,
            habits = core.habits,
            completions = core.completions,
            journals = core.journals,
            allAchievements = meta.allAchievements,
            userAchievements = meta.userAchievements,
            settings = meta.settings ?: UserSettingsEntity(userId = currentUserId),
            quote = meta.quote
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HabitlyMainUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.ensureUserInitialized(
                userId = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail,
                photoUrl = initialPhotoUrl
            )
            val (_, nextQuote) = repository.getNextQuote()
            _quote.value = nextQuote
            _lastSyncEpochMs.value = prefsDataStore.getSnapshot().lastSyncEpochMs
            // Perform initial background cloud sync (safe if offline)
            performSync(silent = true)
        }
    }

    fun refreshQuote() {
        viewModelScope.launch {
            val (_, nextQuote) = repository.getNextQuote()
            _quote.value = nextQuote
        }
    }

    fun toggleHabitCompletion(
        habitId: String,
        dateIso: String = LocalDate.now().toString(),
        note: String = ""
    ) {
        viewModelScope.launch {
            val unlocked = repository.toggleCompletion(currentUserId, habitId, dateIso, note)
            if (unlocked.isNotEmpty()) {
                _newlyUnlockedAchievement.value = unlocked.first()
                refreshQuote()
            }
        }
    }

    fun updateCompletionNote(habitId: String, dateIso: String, note: String) {
        viewModelScope.launch {
            repository.updateCompletionNote(currentUserId, habitId, dateIso, note)
            _snackbarMessage.value = "Check-in note saved"
        }
    }

    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.saveHabit(habit.copy(userId = currentUserId))
            _snackbarMessage.value = "Habit saved"
        }
    }

    fun togglePauseHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.togglePauseHabit(habit)
            _snackbarMessage.value = if (habit.isPaused) "Habit resumed" else "Habit paused"
        }
    }

    fun toggleArchiveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleArchiveHabit(habit)
            _snackbarMessage.value = if (habit.isArchived) "Habit unarchived" else "Habit archived"
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            _snackbarMessage.value = "Habit deleted"
        }
    }

    fun createCategory(name: String, iconKey: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveCategory(currentUserId, name, iconKey, colorHex)
            _snackbarMessage.value = "Category \"$name\" added"
        }
    }

    fun saveJournal(
        existingId: String?,
        dateIso: String,
        mood: String,
        content: String,
        habitId: String = ""
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val entry = DailyJournalEntity(
                id = existingId ?: "j_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                userId = currentUserId,
                dateIso = dateIso,
                mood = mood,
                content = content.trim(),
                habitId = habitId
            )
            val unlocked = repository.saveJournalEntry(entry)
            if (unlocked.isNotEmpty()) {
                _newlyUnlockedAchievement.value = unlocked.first()
            }
            _snackbarMessage.value = "Journal reflection saved"
        }
    }

    fun deleteJournal(entry: DailyJournalEntity) {
        viewModelScope.launch {
            repository.deleteJournalEntry(entry)
            _snackbarMessage.value = "Journal entry deleted"
        }
    }

    fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        viewModelScope.launch {
            val current = uiState.value.settings
            val updated = transform(current).copy(userId = currentUserId)
            repository.updateSettings(updated)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            performSync(silent = false)
        }
    }

    private suspend fun performSync(silent: Boolean) {
        if (!silent) _syncState.value = SyncUiState.Syncing
        when (val result = repository.syncWithCloud(currentUserId)) {
            is SyncResult.Success -> {
                _lastSyncEpochMs.value = result.syncedAtEpochMs
                if (!silent) {
                    _syncState.value = SyncUiState.Success(result.syncedAtEpochMs)
                    _snackbarMessage.value = "Synced with cloud"
                }
            }
            is SyncResult.Error -> {
                if (!silent) {
                    _syncState.value = SyncUiState.Error(result.message)
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun exportData(context: Context) {
        viewModelScope.launch {
            runCatching {
                val json = repository.getExportJson(currentUserId)
                DataExportHelper.shareJsonExport(context, json)
            }.onFailure {
                _snackbarMessage.value = "Export failed: ${it.localizedMessage}"
            }
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllUserData(currentUserId)
            onComplete()
        }
    }

    fun dismissAchievementCelebration() {
        _newlyUnlockedAchievement.value = null
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private data class CoreBundle(
        val user: UserEntity?,
        val categories: List<CategoryEntity>,
        val habits: List<HabitEntity>,
        val completions: List<HabitCompletionEntity>,
        val journals: List<DailyJournalEntity>
    )

    private data class MetaBundle(
        val allAchievements: List<AchievementEntity>,
        val userAchievements: List<UserAchievementEntity>,
        val settings: UserSettingsEntity?,
        val quote: MotivationalQuote
    )
}
