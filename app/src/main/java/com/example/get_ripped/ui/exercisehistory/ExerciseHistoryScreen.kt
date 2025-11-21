package com.example.get_ripped.ui.exercisehistory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.ExerciseHistoryEntry
import com.example.get_ripped.data.model.ExerciseTypes
import com.example.get_ripped.data.model.ExerciseKind
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    exerciseName: String,
    repo: WorkoutRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<ExerciseHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedEntry by remember { mutableStateOf<ExerciseHistoryEntry?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }


    LaunchedEffect(exerciseName) {
        isLoading = true
        entries = repo.exerciseHistoryForName(exerciseName)
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("$exerciseName history") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                entries.isEmpty() -> {
                    Text(
                        text = "No history yet for this exercise.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(entries) { entry ->
                            HistoryEntryCard(
                                entry = entry,
                                exerciseName = exerciseName,
                                onClick = {
                                    selectedEntry = entry
                                    showDetails = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    // Details dialog: show all sets from that day
    if (showDetails && selectedEntry != null) {
        val entry = selectedEntry!!
        val config = ExerciseTypes.configForName(exerciseName)

        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = {
                Text(text = "${entry.workoutName} — ${entry.date ?: ""}")
            },
            text = {
                Column {
                    if (entry.sets.isEmpty()) {
                        Text("No sets recorded for this session.")
                    } else {
                        entry.sets.forEachIndexed { index, set ->
                            Text(
                                text = "Set ${index + 1}: ${formatTopSet(config.kind, set)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDetails = false
                        showDeleteConfirm = true
                    }
                ) {
                    Text("Remove from memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }

// Confirmation dialog for deleting this session's sets
    if (showDeleteConfirm && selectedEntry != null) {
        val entry = selectedEntry!!

        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove this session?") },
            text = {
                Text("This will permanently delete all sets for $exerciseName from this workout day.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        // delete sets + refresh history
                        val workoutId = entry.workoutId
                        scope.launch {
                            repo.deleteExerciseSession(exerciseName, workoutId)
                            // refresh the history list
                            entries = repo.exerciseHistoryForName(exerciseName)
                            selectedEntry = null
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HistoryEntryCard(
    entry: ExerciseHistoryEntry,
    exerciseName: String,
    onClick: () -> Unit
) {
    val config = ExerciseTypes.configForName(exerciseName)

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Top row: workout name + date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.workoutName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = entry.date ?: "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(4.dp))

            // Top set
            entry.topSet?.let { set ->
                Text(
                    text = "Top set: ${formatTopSet(config.kind, set)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Volume
            Text(
                text = "Volume: ${entry.volume.toInt()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun formatTopSet(kind: ExerciseKind, set: com.example.get_ripped.data.model.SetEntry): String {
    val weightStr =
        if (set.weight % 1f == 0f) set.weight.toInt().toString()
        else set.weight.toString()

    return when (kind) {
        ExerciseKind.TIMED_HOLD -> {
            "${set.reps} sec"
        }

        ExerciseKind.REPS_ONLY -> {
            "${set.reps} reps"
        }

        ExerciseKind.UNILATERAL_REPS -> {
            val left = set.repsLeft ?: set.reps
            val right = set.repsRight ?: set.reps
            "$left/$right reps x $weightStr lbs"
        }

        ExerciseKind.WEIGHT_REPS -> {
            "${set.reps} reps x $weightStr lbs"
        }
    }
}
