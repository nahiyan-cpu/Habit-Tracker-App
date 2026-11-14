package com.example.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.HabitEntity
import com.example.domain.FrequencyType
import com.example.domain.HabitCatalog
import com.example.domain.HabitScheduleEngine
import com.example.ui.components.formatTimeDisplay
import com.example.ui.theme.LocalHabitlyColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.viewmodel.HabitlyMainUiState
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    state: HabitlyMainUiState,
    onAddHabit: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onCreateCategory: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }
    var statusFilter by remember { mutableStateOf("active") } // "active", "paused", "archived"
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val filteredHabits = remember(state.habits, searchQuery, selectedCategoryId, statusFilter) {
        state.habits.filter { h ->
            val matchesStatus = when (statusFilter) {
                "paused" -> h.isPaused && !h.isArchived
                "archived" -> h.isArchived
                else -> !h.isPaused && !h.isArchived
            }
            val matchesCategory = selectedCategoryId == "all" || h.categoryId == selectedCategoryId
            val matchesSearch = searchQuery.isBlank() || h.name.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesCategory && matchesSearch
        }
    }
    val categoriesMap = remember(state.categories) { state.categories.associateBy { it.id } }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Habits", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    TextButton(
                        onClick = { showNewCategoryDialog = true },
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Category")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("habits_fab_add")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Create new habit")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search habits...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("habit_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status tabs
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("active" to "Active", "paused" to "Paused", "archived" to "Archived").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            // Category filter row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == "all",
                        onClick = { selectedCategoryId = "all" },
                        label = { Text("All") }
                    )
                }
                items(state.categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            if (filteredHabits.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.img_empty_habits),
                            contentDescription = "No habits found",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No habits in this view", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tap + to add a new habit or adjust your filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val stats = remember(habit, state.completions) {
                            HabitScheduleEngine.calculateHabitStats(habit, state.completions)
                        }
                        val color = HabitCatalog.parseColor(habit.colorHex, MaterialTheme.colorScheme.primary)
                        val category = categoriesMap[habit.categoryId]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("habit_list_item_${habit.id}")
                                .clickable { onOpenDetail(habit.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(color.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = HabitCatalog.iconForKey(habit.iconKey),
                                        contentDescription = null,
                                        tint = color
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "${category?.name ?: "Habit"} • ${FrequencyType.fromValue(habit.frequencyType).label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${stats.completionRatePercent}%",
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    Text(
                                        text = "${stats.currentStreak}d streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showNewCategoryDialog = false },
            onSave = { name, iconKey, colorHex ->
                onCreateCategory(name, iconKey, colorHex)
                showNewCategoryDialog = false
            }
        )
    }
}

@Composable
private fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(HabitCatalog.habitIcons.first().key) }
    var selectedColor by remember { mutableStateOf(HabitCatalog.colorSwatches.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitCatalog.colorSwatches.take(6).forEach { hex ->
                        val c = HabitCatalog.parseColor(hex)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HabitCatalog.habitIcons.take(10)) { opt ->
                        val active = opt.key == selectedIcon
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = opt.key }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    opt.icon,
                                    contentDescription = opt.label,
                                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitScreen(
    existingHabit: HabitEntity?,
    categories: List<CategoryEntity>,
    use24HourFormat: Boolean,
    onSave: (HabitEntity) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(existingHabit) { mutableStateOf(existingHabit?.name ?: "") }
    var description by remember(existingHabit) { mutableStateOf(existingHabit?.description ?: "") }
    var categoryId by remember(existingHabit, categories) {
        mutableStateOf(existingHabit?.categoryId ?: categories.firstOrNull()?.id ?: "cat_other")
    }
    var iconKey by remember(existingHabit) { mutableStateOf(existingHabit?.iconKey ?: "check_circle") }
    var colorHex by remember(existingHabit) { mutableStateOf(existingHabit?.colorHex ?: "#6C63FF") }
    var startDate by remember(existingHabit) {
        mutableStateOf(existingHabit?.startDate ?: LocalDate.now().toString())
    }
    var frequencyType by remember(existingHabit) {
        mutableStateOf(FrequencyType.fromValue(existingHabit?.frequencyType ?: "daily"))
    }
    var targetDaysPerWeek by remember(existingHabit) {
        mutableIntStateOf(existingHabit?.targetDaysPerWeek ?: 4)
    }
    val selectedWeekdays = remember(existingHabit) {
        mutableStateListOf<Int>().apply {
            addAll(existingHabit?.scheduledWeekdays ?: listOf(1, 3, 5))
        }
    }
    val selectedMonthDays = remember(existingHabit) {
        mutableStateListOf<Int>().apply {
            addAll(existingHabit?.scheduledMonthDays ?: listOf(1, 15))
        }
    }
    var remindersEnabled by remember(existingHabit) {
        mutableStateOf(existingHabit?.remindersEnabled ?: false)
    }
    val reminderTimes = remember(existingHabit) {
        mutableStateListOf<String>().apply {
            addAll(existingHabit?.reminderTimes ?: listOf("08:00"))
        }
    }
    var targetDuration by remember(existingHabit) {
        mutableStateOf(
            if ((existingHabit?.targetDurationMinutes ?: 0) > 0) existingHabit?.targetDurationMinutes.toString() else ""
        )
    }
    var notes by remember(existingHabit) { mutableStateOf(existingHabit?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingHabit == null) "Create Habit" else "Edit Habit") },
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text("Habit Name *") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Please enter a habit name") } } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category picker
            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = categoryId == cat.id,
                        onClick = { categoryId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Color picker
            Text("Color Accent", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HabitCatalog.colorSwatches.forEach { hex ->
                    val color = HabitCatalog.parseColor(hex)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (colorHex == hex) 3.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { colorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorHex == hex) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Icon picker
            Text("Icon", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitCatalog.habitIcons.forEach { opt ->
                    val selected = opt.key == iconKey
                    val accent = HabitCatalog.parseColor(colorHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { iconKey = opt.key }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = opt.icon,
                                contentDescription = opt.label,
                                tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Frequency selector
            Text("Schedule Frequency", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FrequencyType.entries.forEach { freq ->
                    FilterChip(
                        selected = frequencyType == freq,
                        onClick = { frequencyType = freq },
                        label = { Text(freq.label) }
                    )
                }
            }

            when (frequencyType) {
                FrequencyType.DAYS_PER_WEEK -> {
                    Column {
                        Text(
                            text = "Target: $targetDaysPerWeek day(s) per week",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = targetDaysPerWeek.toFloat(),
                            onValueChange = { targetDaysPerWeek = it.toInt().coerceIn(1, 7) },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                    }
                }
                FrequencyType.WEEKLY -> {
                    val dayLabels = listOf(
                        1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu",
                        5 to "Fri", 6 to "Sat", 7 to "Sun"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayLabels.forEach { (iso, label) ->
                            val selected = selectedWeekdays.contains(iso)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected && selectedWeekdays.size > 1) selectedWeekdays.remove(iso)
                                    else if (!selected) selectedWeekdays.add(iso)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
                FrequencyType.MONTHLY -> {
                    Text(
                        text = "Select dates of every month:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..31).forEach { dom ->
                            val selected = selectedMonthDays.contains(dom)
                            Surface(
                                shape = CircleShape,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable {
                                        if (selected && selectedMonthDays.size > 1) selectedMonthDays.remove(dom)
                                        else if (!selected) selectedMonthDays.add(dom)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dom.toString(),
                                        fontSize = 12.sp,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                FrequencyType.DAILY -> Unit
            }

            // Reminders section (supports multiple reminders)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Habit Reminders", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Receive scheduled notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { remindersEnabled = it }
                        )
                    }

                    if (remindersEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            reminderTimes.forEachIndexed { idx, timeStr ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatTimeDisplay(timeStr, use24HourFormat),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (reminderTimes.size > 1) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Remove reminder",
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable { reminderTimes.removeAt(idx) },
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val formatted = String.format("%02d:%02d", hour, minute)
                                            if (!reminderTimes.contains(formatted)) {
                                                reminderTimes.add(formatted)
                                            }
                                        },
                                        8,
                                        0,
                                        use24HourFormat
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Time")
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetDuration,
                    onValueChange = { targetDuration = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Personal Notes") },
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val validStartDate = runCatching { LocalDate.parse(startDate).toString() }
                        .getOrDefault(LocalDate.now().toString())

                    val habit = HabitEntity(
                        id = existingHabit?.id ?: "habit_${UUID.randomUUID().toString().replace("-", "").take(12)}",
                        userId = existingHabit?.userId ?: "",
                        name = name.trim(),
                        description = description.trim(),
                        categoryId = categoryId,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        startDate = validStartDate,
                        frequencyType = frequencyType.value,
                        targetDaysPerWeek = targetDaysPerWeek,
                        scheduledWeekdays = selectedWeekdays.toList().sorted(),
                        scheduledMonthDays = selectedMonthDays.toList().sorted(),
                        reminderTimes = if (remindersEnabled) reminderTimes.toList() else emptyList(),
                        remindersEnabled = remindersEnabled,
                        targetDurationMinutes = targetDuration.toIntOrNull() ?: 0,
                        notes = notes.trim(),
                        isPaused = existingHabit?.isPaused ?: false,
                        isArchived = existingHabit?.isArchived ?: false,
                        createdAtEpochMs = existingHabit?.createdAtEpochMs ?: System.currentTimeMillis()
                    )
                    onSave(habit)
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_habit_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (existingHabit == null) "Create Habit" else "Save Changes")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailsScreen(
    habit: HabitEntity?,
    state: HabitlyMainUiState,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTogglePause: (HabitEntity) -> Unit,
    onToggleArchive: (HabitEntity) -> Unit,
    onDelete: (HabitEntity) -> Unit,
    onToggleCompletionForDate: (String, String) -> Unit,
    onUpdateNoteForDate: (String, String, String) -> Unit
) {
    if (habit == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Habit Details") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { p ->
            Box(Modifier.fillMaxSize().padding(p), contentAlignment = Alignment.Center) {
                Text("Habit not found")
            }
        }
        return
    }

    val habitCompletions = remember(habit.id, state.completions) {
        state.completions.filter { it.habitId == habit.id }
    }
    val stats = remember(habit, habitCompletions) {
        HabitScheduleEngine.calculateHabitStats(habit, habitCompletions)
    }
    val color = HabitCatalog.parseColor(habit.colorHex)
    val successColor = LocalHabitlyColors.current.success
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingNoteDate by remember { mutableStateOf<String?>(null) }
    var noteDraft by remember { mutableStateOf("") }

    val last14Days = remember {
        val today = LocalDate.now()
        (0..13).map { today.minusDays(it.toLong()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onEdit(habit.id) },
                        modifier = Modifier.testTag("habit_detail_edit_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit habit")
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("habit_detail_delete_button")
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete habit")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HabitCatalog.iconForKey(habit.iconKey),
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(habit.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = habit.description.ifBlank { FrequencyType.fromValue(habit.frequencyType).label },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (habit.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = habit.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { onTogglePause(habit) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (habit.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (habit.isPaused) "Resume" else "Pause")
                            }
                            OutlinedButton(
                                onClick = { onToggleArchive(habit) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (habit.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (habit.isArchived) "Unarchive" else "Archive")
                            }
                        }
                    }
                }
            }

            // Streak & Rate Metrics
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Current Streak", "${stats.currentStreak} Days", color, Modifier.weight(1f))
                        MetricTile("Longest Streak", "${stats.longestStreak} Days", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Completion Rate", "${stats.completionRatePercent}%", successColor, Modifier.weight(1f))
                        MetricTile("Weekly / Monthly", "${stats.weeklySuccessRatePercent}% / ${stats.monthlySuccessRatePercent}%", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Completed Days", "${stats.totalCompletedDays}", successColor, Modifier.weight(1f))
                        MetricTile("Missed Days", "${stats.totalMissedScheduledDays}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                    }
                }
            }

            // Recent 14-day interactive check-in history
            item {
                Text("Recent Check-Ins & Daily Notes", style = MaterialTheme.typography.titleMedium)
            }

            items(last14Days) { date ->
                val dateIso = date.toString()
                val completion = habitCompletions.firstOrNull { it.dateIso == dateIso }
                val isDone = completion?.isCompleted == true
                val scheduled = HabitScheduleEngine.isScheduledOnDateIgnoringPause(habit, date)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (date == LocalDate.now()) "Today ($dateIso)" else dateIso,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = completion?.note?.takeIf { it.isNotBlank() }
                                    ?: if (scheduled) "Scheduled" else "Rest / unscheduled day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {
                            editingNoteDate = dateIso
                            noteDraft = completion?.note ?: ""
                        }) {
                            Text("Note")
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDone) successColor else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onToggleCompletionForDate(habit.id, dateIso) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = "Toggle check-in for $dateIso",
                                tint = if (isDone) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit?") },
            text = { Text("Are you sure you want to delete \"${habit.name}\"? This removes it from your active schedule.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    onDelete(habit)
                    onBack()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (editingNoteDate != null) {
        AlertDialog(
            onDismissRequest = { editingNoteDate = null },
            title = { Text("Note for ${editingNoteDate}") },
            text = {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text("How did it go?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val d = editingNoteDate ?: return@Button
                    onUpdateNoteForDate(habit.id, d, noteDraft)
                    editingNoteDate = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteDate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = accent
            )
        }
    }
}
