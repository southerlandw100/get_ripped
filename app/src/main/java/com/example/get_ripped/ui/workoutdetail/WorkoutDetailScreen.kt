package com.example.get_ripped.ui.workoutdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.repo.WorkoutRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit
) {
    val workout by repo.workoutById(workoutId).collectAsState(initial = null)
    val exercises by repo.exercisesForWorkout(workoutId).collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // all detail UI lives here
        }
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        // add exercise to this workout
                        // (since repo is not a VM here, we can't launch; FakeRepo is sync-safe for demo)
                        // In Room version we'll call a VM method.
                        // For now we can use LaunchedEffect or rememberCoroutineScope:
                        // Keep it simple:
                    }
                    showDialog = false
                    newName = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text("New Exercise") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name") }
                )
            }
        )
    }
}

@Composable
private fun ExerciseCard(ex: Exercise, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium)
            Text("Last: ${ex.lastDate}", style = MaterialTheme.typography.bodySmall)
            // quick summary: last set if available
            ex.sets.lastOrNull()?.let { last ->
                Text("Last set: ${last.reps} reps @ ${last.weight} lbs", style = MaterialTheme.typography.bodySmall)
            }
            ex.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
    }
}
