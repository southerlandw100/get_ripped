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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit,
    onExerciseClick: (Long, Long) -> Unit = { _, _ -> }   // (workoutId, exerciseId)
) {
    val vm: WorkoutDetailViewModel = viewModel(
        factory = WorkoutDetailViewModelFactory(repo, workoutId)
    )

    val workout by vm.workout.collectAsState()
    val exercises by vm.exercises.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var existingNames by remember { mutableStateOf<List<String>>(emptyList()) }

    val scope = rememberCoroutineScope()

    // Auto-repeat previous session if this workout is empty
    LaunchedEffect(exercises) {
        if (exercises.isEmpty()) {
            vm.prefillIfEmpty(workoutId)
        }
    }

    // When dialog opens, load existing exercise names
    LaunchedEffect(showDialog) {
        if (showDialog) {
            existingNames = repo.allExerciseNames()
        }
    }

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
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("Add Exercise")
            }
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Exercise") },
            text = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // New exercise name input
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New exercise name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (existingNames.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Or select an existing exercise:", style = MaterialTheme.typography.bodySmall)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(existingNames) { name ->
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                vm.addExercise(name)
                                                showDialog = false
                                                newName = ""
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) {
                        scope.launch {
                            vm.addExercise(name)
                        }
                    }
                    newName = ""
                    showDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
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
                val displayWeight = if (last.weight % 1f == 0f) {
                    last.weight.toInt().toString()
                } else {
                    last.weight.toString()
                }
                Text(
                    "Last set: ${last.reps} reps @ $displayWeight lbs",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            ex.note?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
}
