package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.DailyJournalEntity
import com.example.ui.viewmodel.HabitlyMainUiState
import java.time.LocalDate

private val moodOptions = listOf(
    "happy" to "Happy",
    "motivated" to "Motivated",
    "neutral" to "Neutral",
    "tired" to "Tired",
    "sad" to "Sad"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyJournalScreen(
    state: HabitlyMainUiState,
    onSaveJournal: (existingId: String?, dateIso: String, mood: String, content: String, habitId: String) -> Unit,
    onDeleteJournal: (DailyJournalEntity) -> Unit,
    onBack: () -> Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var dateIso by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedMood by remember { mutableStateOf("motivated") }
    var content by remember { mutableStateOf("") }
    var selectedHabitId by remember { mutableStateOf("") }
    var filterDate by remember { mutableStateOf("") }

    val habitsMap = remember(state.habits) { state.habits.associateBy { it.id } }
    val filteredEntries = remember(state.journals, filterDate) {
        if (filterDate.isBlank()) state.journals
        else state.journals.filter { it.dateIso == filterDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Journal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Private & encrypted to your account. Never shared publicly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Composer Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (editingId == null) "New Reflection" else "Edit Reflection",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("How are you feeling?", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            moodOptions.forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedMood == key,
                                    onClick = { selectedMood = key },
                                    label = { Text(label) }
                                )
                            }
                        }

                        if (state.habits.isNotEmpty()) {
                            Text("Link to Habit (optional)", style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedHabitId.isEmpty(),
                                    onClick = { selectedHabitId = "" },
                                    label = { Text("General") }
                                )
                                state.habits.take(5).forEach { h ->
                                    FilterChip(
                                        selected = selectedHabitId == h.id,
                                        onClick = { selectedHabitId = h.id },
                                        label = { Text(h.name) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Write your thoughts, wins, or lessons...") },
                            minLines = 3,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("journal_content_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    onSaveJournal(editingId, dateIso, selectedMood, content, selectedHabitId)
                                    editingId = null
                                    content = ""
                                    selectedHabitId = ""
                                    dateIso = LocalDate.now().toString()
                                },
                                enabled = content.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_journal_button")
                            ) {
                                Text(if (editingId == null) "Save Reflection" else "Update Entry")
                            }
                            if (editingId != null) {
                                androidx.compose.material3.TextButton(onClick = {
                                    editingId = null
                                    content = ""
                                }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // History filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Journal History (${filteredEntries.size})", style = MaterialTheme.typography.titleMedium)
                    FilterChip(
                        selected = filterDate == LocalDate.now().toString(),
                        onClick = {
                            filterDate = if (filterDate.isBlank()) LocalDate.now().toString() else ""
                        },
                        label = { Text("Today Only") }
                    )
                }
            }

            if (filteredEntries.isEmpty()) {
                item {
                    Text(
                        text = "No reflections saved yet. Take a moment to record today's progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredEntries, key = { it.id }) { entry ->
                    val linkedHabit = habitsMap[entry.habitId]
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
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = entry.mood.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (linkedHabit != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = linkedHabit.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingId = entry.id
                                        dateIso = entry.dateIso
                                        selectedMood = entry.mood
                                        content = entry.content
                                        selectedHabitId = entry.habitId
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit reflection", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteJournal(entry) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete reflection", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Text(
                                text = entry.dateIso,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = entry.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
