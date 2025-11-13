package com.example.get_ripped.ui.workoutdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.repo.WorkoutRepository
import com.example.get_ripped.ui.exercisepicker.ExercisePickerSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit,
    onExerciseClick: (Long, Long) -> Unit = { _, _ -> }   // (workoutId, exerciseId)
) {
    // ViewModel with repo + workoutId
    val vm: WorkoutDetailViewModel = viewModel(
        factory = WorkoutDetailViewModelFactory(repo, workoutId)
    )

    // Screen state
    val workout by vm.workout.collectAsState(initial = null)
    val exercises by vm.exercises.collectAsState(initial = emptyList())

    // Picker sheet state
    var showPicker by remember { mutableStateOf(false) }
    val query by vm.query.collectAsState()
    val names by vm.names.collectAsState() // debounced list of names (all or filtered)

    val scope = rememberCoroutineScope()

    // Auto-repeat last workout if this one is empty
    LaunchedEffect(exercises) {
        if (exercises.isEmpty()) {
            vm.prefillIfEmpty(workoutId)
        }
    }

    // Top-level scaffold
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(workout?.name ?: "Workout") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showPicker = true
                vm.updateQuery("")       // start fresh each time (optional)
            }) { Text("Add Exercise") }
        }

    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (exercises.isEmpty()) {
                Text("No exercises yet.\nTap 'Add Exercise' to begin.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(exercises, key = { it.id }) { ex ->
                        ExerciseCard(
                            ex = ex,
                            onClick = { onExerciseClick(workoutId, ex.id) }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

    }

    if (showPicker) {
        ExercisePickerSheet(
            query = query,
            names = names,
            onQueryChange = vm::updateQuery,
            onPick = { rawName ->
                // Close the sheet right away
                showPicker = false

                val trimmed = rawName.trim()
                if (trimmed.isEmpty()) {
                    return@ExercisePickerSheet
                }

                // Normalize: collapse spaces & Title Case each word
                val normalized = trimmed
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { word ->
                        word.lowercase()
                            .replaceFirstChar { ch ->
                                if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                            }
                    }

                // Let the ViewModel own the coroutine + repo calls
                vm.addExerciseFromPicker(normalized)
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ExerciseCard(
    ex: Exercise,
    onClick: () -> Unit
) {
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

            ex.sets.lastOrNull()?.let { last ->
                val displayWeight =
                    if (last.weight % 1f == 0f) last.weight.toInt().toString()
                    else last.weight.toString()
                Text(
                    "Last set: ${last.reps} reps @ $displayWeight lbs",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ex.note?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
