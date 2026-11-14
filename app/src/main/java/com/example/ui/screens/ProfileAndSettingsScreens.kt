package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.UserSettingsEntity
import com.example.domain.HabitCatalog
import com.example.domain.HabitScheduleEngine
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.LocalHabitlyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.HabitlyMainUiState
import com.example.ui.viewmodel.SyncUiState
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    state: HabitlyMainUiState,
    onOpenAchievements: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenBackupSync: () -> Unit,
    onExportData: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalHabits = state.habits.size
    val totalCompletions = remember(state.completions) { state.completions.count { it.isCompleted } }
    val earnedBadges = state.userAchievements.size
    val overallRate = remember(state.habits, state.completions) {
        val list = HabitScheduleEngine.buildHabitConsistencyList(
            state.habits,
            state.completions,
            LocalDate.now().minusDays(29),
            LocalDate.now()
        )
        if (list.isEmpty()) 0 else list.map { it.ratePercent }.average().toInt()
    }

    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Profile & Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // User Profile Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val photo = state.user?.photoUrl
                        if (!photo.isNullOrBlank()) {
                            AsyncImage(
                                model = photo,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (state.user?.displayName?.firstOrNull() ?: 'H').uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.user?.displayName ?: "Habitly Member",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = state.user?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Level ${state.user?.level ?: 1} • ${state.user?.totalPoints ?: 0} XP",
                                    fontFamily = MonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileStatColumn("Habits", "$totalHabits")
                        ProfileStatColumn("Done", "$totalCompletions")
                        ProfileStatColumn("Badges", "$earnedBadges")
                        ProfileStatColumn("Rate", "$overallRate%")
                    }
                }
            }
        }

        // Quick Actions Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Rounded.EmojiEvents,
                        title = "Achievements & Badges",
                        subtitle = "$earnedBadges badges earned",
                        onClick = onOpenAchievements
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.EditNote,
                        title = "Daily Journal",
                        subtitle = "${state.journals.size} reflections",
                        onClick = onOpenJournal
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "App Settings & Theme",
                        subtitle = "Theme, accent color, week start, time format",
                        onClick = onOpenAppSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notification Settings",
                        subtitle = if (state.settings.notificationsEnabled) "Reminders enabled" else "Reminders muted",
                        onClick = onOpenNotificationSettings
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.CloudSync,
                        title = "Backup & Cloud Sync",
                        subtitle = "Firestore synchronization & recovery",
                        onClick = onOpenBackupSync
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Download,
                        title = "Export Data (JSON)",
                        subtitle = "Share or save a portable backup",
                        onClick = onExportData
                    )
                }
            }
        }

        // Legal & About Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Rounded.Policy,
                        title = "Privacy Policy",
                        subtitle = "How Habitly protects your personal data",
                        onClick = {
                            infoDialogTitle = "Privacy Policy"
                            infoDialogBody = "Habitly stores your habits, completion records, and private daily journal locally on your device via Room and syncs exclusively to your personal authenticated Cloud Firestore workspace (/users/{uid}). Private journal entries are never shared with third parties."
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Terms & About Habitly",
                        subtitle = "Version 1.0 • Smart Habit Tracker",
                        onClick = {
                            infoDialogTitle = "About Habitly"
                            infoDialogBody = "Habitly v1.0 helps you build lasting positive habits with offline-first reliability, Jetpack Glance widgets, WorkManager reminders, interactive analytics, and secure cloud synchronization."
                        }
                    )
                }
            }
        }

        // Account Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button"),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Account & Data")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(infoDialogTitle ?: "") },
            text = { Text(infoDialogBody) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) { Text("Close") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account & Data?") },
            text = {
                Text("This permanently erases your habits, completions, journals, and achievements from this device and Cloud Firestore.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    settings: UserSettingsEntity,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Appearance Mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { onUpdateSettings { it.copy(themeMode = mode) } },
                        label = { Text(label) }
                    )
                }
            }

            Text("Accent Color", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitCatalog.colorSwatches.forEach { hex ->
                    val c = HabitCatalog.parseColor(hex)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (settings.accentColorHex == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { onUpdateSettings { it.copy(accentColorHex = hex) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (settings.accentColorHex == hex) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Text("First Day of Week", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.firstDayOfWeek == 1,
                    onClick = { onUpdateSettings { it.copy(firstDayOfWeek = 1) } },
                    label = { Text("Monday") }
                )
                FilterChip(
                    selected = settings.firstDayOfWeek == 7,
                    onClick = { onUpdateSettings { it.copy(firstDayOfWeek = 7) } },
                    label = { Text("Sunday") }
                )
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Use 24-Hour Time", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (settings.use24HourFormat) "e.g. 20:30" else "e.g. 8:30 PM",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.use24HourFormat,
                        onCheckedChange = { checked -> onUpdateSettings { it.copy(use24HourFormat = checked) } }
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Weekly Consistency Goal", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${settings.weeklyGoalPercent}%",
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = settings.weeklyGoalPercent.toFloat(),
                        onValueChange = { v -> onUpdateSettings { it.copy(weeklyGoalPercent = v.toInt()) } },
                        valueRange = 20f..100f,
                        steps = 7
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    settings: UserSettingsEntity,
    onUpdateSettings: ((UserSettingsEntity) -> UserSettingsEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ReminderScheduler.hasNotificationPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notification Permission Required", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Allow notifications on Android 13+ to receive daily habit reminders and streak alerts.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("request_notification_permission_button")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            ToggleSettingCard(
                title = "Daily Habit Reminders",
                subtitle = "Notify at each habit's scheduled reminder times",
                checked = settings.notificationsEnabled,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(notificationsEnabled = checked) } }
            )

            ToggleSettingCard(
                title = "Missed Habit Evening Check-In",
                subtitle = "Gentle reminder at 8:30 PM if scheduled habits remain",
                checked = settings.missedReminderEnabled,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(missedReminderEnabled = checked) } }
            )

            ToggleSettingCard(
                title = "Streak Milestone Celebrations",
                subtitle = "Notify when hitting 3, 7, 14, 30, or 100-day streaks",
                checked = settings.streakMilestoneNotifications,
                onCheckedChange = { checked -> onUpdateSettings { it.copy(streakMilestoneNotifications = checked) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndSyncScreen(
    syncState: SyncUiState,
    lastSyncEpochMs: Long,
    onSyncNow: () -> Unit,
    onExportJson: () -> Unit,
    onBack: () -> Unit
) {
    val formattedLastSync = remember(lastSyncEpochMs) {
        if (lastSyncEpochMs <= 0L) "Not synced yet"
        else SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(lastSyncEpochMs))
    }
    val successColor = LocalHabitlyColors.current.success

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Cloud Sync") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Cloud Synchronization", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Room keeps your data fast offline. Sync merges your habits, completions, journals, and badges with Cloud Firestore using last-write-wins conflict resolution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Last Sync: $formattedLastSync",
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    when (syncState) {
                        is SyncUiState.Success -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Sync complete", style = MaterialTheme.typography.labelMedium, color = successColor)
                        }
                        is SyncUiState.Error -> {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(syncState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSyncNow,
                        enabled = syncState !is SyncUiState.Syncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sync_now_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (syncState is SyncUiState.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync & Restore Now")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onExportJson,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("backup_export_json_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Local Backup (JSON)")
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleSettingCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
