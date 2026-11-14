package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.auth.GoogleAuthHelper
import com.example.di.HabitlyApplication
import com.example.ui.navigation.AppRoute
import com.example.ui.viewmodel.MainViewModel

data class ActiveSessionUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isCloudBacked: Boolean = true
)

private data class TopLevelTab(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val topLevelTabs = listOf(
    TopLevelTab(AppRoute.Home, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
    TopLevelTab(AppRoute.Habits, "Habits", Icons.Rounded.CheckCircle, Icons.Outlined.CheckCircle, "nav_habits"),
    TopLevelTab(AppRoute.Statistics, "Stats", Icons.Rounded.BarChart, Icons.Outlined.BarChart, "nav_stats"),
    TopLevelTab(AppRoute.Calendar, "Calendar", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendar"),
    TopLevelTab(AppRoute.Profile, "Profile", Icons.Rounded.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun MainAppShell(
    sessionUser: ActiveSessionUser,
    onLocalSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    // ViewModel keyed by authenticated uid so switching accounts creates a fresh ViewModel
    val viewModel: MainViewModel = viewModel(
        key = sessionUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as HabitlyApplication
                MainViewModel(
                    repository = app.container.habitRepository,
                    prefsDataStore = app.container.preferencesDataStore,
                    currentUserId = sessionUser.uid,
                    initialDisplayName = sessionUser.displayName.ifBlank { "Habitly Member" },
                    initialEmail = sessionUser.email,
                    initialPhotoUrl = sessionUser.photoUrl
                )
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unlockedBadge by viewModel.newlyUnlockedAchievement.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val lastSyncEpochMs by viewModel.lastSyncEpochMs.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val showTopLevelNav = currentDest?.let { dest ->
        topLevelTabs.any { dest.hasRoute(it.route::class) }
    } ?: true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        topLevelTabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<AppRoute.Home> {
                        HomeDashboardScreen(
                            state = uiState,
                            onToggleHabit = { habitId -> viewModel.toggleHabitCompletion(habitId) },
                            onOpenHabitDetail = { habitId -> navController.navigate(AppRoute.HabitDetails(habitId)) },
                            onOpenAddHabit = { navController.navigate(AppRoute.AddHabit) },
                            onOpenJournal = { navController.navigate(AppRoute.DailyJournal) },
                            onOpenAchievements = { navController.navigate(AppRoute.Achievements) },
                            onRefreshQuote = { viewModel.refreshQuote() }
                        )
                    }

                    composable<AppRoute.Habits> {
                        HabitListScreen(
                            state = uiState,
                            onAddHabit = { navController.navigate(AppRoute.AddHabit) },
                            onOpenDetail = { habitId -> navController.navigate(AppRoute.HabitDetails(habitId)) },
                            onCreateCategory = { name, iconKey, colorHex ->
                                viewModel.createCategory(name, iconKey, colorHex)
                            }
                        )
                    }

                    composable<AppRoute.Statistics> {
                        StatisticsScreen(state = uiState)
                    }

                    composable<AppRoute.Calendar> {
                        CalendarScreen(
                            state = uiState,
                            onToggleHabitForDate = { habitId, dateIso ->
                                viewModel.toggleHabitCompletion(habitId, dateIso)
                            },
                            onOpenJournalForDate = { navController.navigate(AppRoute.DailyJournal) }
                        )
                    }

                    composable<AppRoute.Profile> {
                        ProfileScreen(
                            state = uiState,
                            onOpenAchievements = { navController.navigate(AppRoute.Achievements) },
                            onOpenJournal = { navController.navigate(AppRoute.DailyJournal) },
                            onOpenAppSettings = { navController.navigate(AppRoute.AppSettings) },
                            onOpenNotificationSettings = { navController.navigate(AppRoute.NotificationSettings) },
                            onOpenBackupSync = { navController.navigate(AppRoute.BackupAndSync) },
                            onExportData = { viewModel.exportData(context) },
                            onSignOut = {
                                if (sessionUser.isCloudBacked) {
                                    GoogleAuthHelper.signOut(credentialManager, scope)
                                } else {
                                    onLocalSignOut()
                                }
                            },
                            onDeleteAccount = {
                                viewModel.deleteAccountData {
                                    if (sessionUser.isCloudBacked) {
                                        GoogleAuthHelper.signOut(credentialManager, scope)
                                    } else {
                                        onLocalSignOut()
                                    }
                                }
                            }
                        )
                    }

                    composable<AppRoute.AddHabit> {
                        AddEditHabitScreen(
                            existingHabit = null,
                            categories = uiState.categories,
                            use24HourFormat = uiState.settings.use24HourFormat,
                            onSave = { habit -> viewModel.saveHabit(habit) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.EditHabit> { entry ->
                        val route = entry.toRoute<AppRoute.EditHabit>()
                        val habit = uiState.habits.firstOrNull { it.id == route.habitId }
                        AddEditHabitScreen(
                            existingHabit = habit,
                            categories = uiState.categories,
                            use24HourFormat = uiState.settings.use24HourFormat,
                            onSave = { updated -> viewModel.saveHabit(updated) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.HabitDetails> { entry ->
                        val route = entry.toRoute<AppRoute.HabitDetails>()
                        val habit = uiState.habits.firstOrNull { it.id == route.habitId }
                        HabitDetailsScreen(
                            habit = habit,
                            state = uiState,
                            onBack = { navController.popBackStack() },
                            onEdit = { id -> navController.navigate(AppRoute.EditHabit(id)) },
                            onTogglePause = { h -> viewModel.togglePauseHabit(h) },
                            onToggleArchive = { h -> viewModel.toggleArchiveHabit(h) },
                            onDelete = { h -> viewModel.deleteHabit(h) },
                            onToggleCompletionForDate = { hId, dateIso ->
                                viewModel.toggleHabitCompletion(hId, dateIso)
                            },
                            onUpdateNoteForDate = { hId, dateIso, note ->
                                viewModel.updateCompletionNote(hId, dateIso, note)
                            }
                        )
                    }

                    composable<AppRoute.DailyJournal> {
                        DailyJournalScreen(
                            state = uiState,
                            onSaveJournal = { id, dateIso, mood, content, habitId ->
                                viewModel.saveJournal(id, dateIso, mood, content, habitId)
                            },
                            onDeleteJournal = { entry -> viewModel.deleteJournal(entry) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.Achievements> {
                        AchievementsScreen(
                            state = uiState,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.NotificationSettings> {
                        NotificationSettingsScreen(
                            settings = uiState.settings,
                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.AppSettings> {
                        AppSettingsScreen(
                            settings = uiState.settings,
                            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.BackupAndSync> {
                        BackupAndSyncScreen(
                            syncState = syncState,
                            lastSyncEpochMs = lastSyncEpochMs,
                            onSyncNow = { viewModel.syncNow() },
                            onExportJson = { viewModel.exportData(context) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (unlockedBadge != null) {
        AchievementUnlockedDialog(
            achievement = unlockedBadge!!,
            quoteText = uiState.quote.text,
            onDismiss = { viewModel.dismissAchievementCelebration() }
        )
    }
}
