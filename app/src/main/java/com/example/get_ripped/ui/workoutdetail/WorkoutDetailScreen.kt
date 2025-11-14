package com.example.get_ripped.ui.workoutdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.ExerciseTypes
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
    val names by vm.names.collectAsState()

    val scope = rememberCoroutineScope()

    // 🔹 Selection mode state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Auto-repeat last workout if this one is empty
    LaunchedEffect(exercises) {
        if (exercises.isEmpty()) {
            vm.prefillIfEmpty(workoutId)
        }
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                // Selection-mode app bar
                TopAppBar(
                    navigationIcon = {
                        TextButton(
                            onClick = {
                                selectionMode = false
                                selectedIds.clear()
                            }
                        ) { Text("←") }
                    },
                    title = {
                        Text(
                            text = selectedIds.size.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showDeleteConfirm = true
                                }
                            }
                        ) {
                            Text("🗑")
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text(workout?.name ?: "Workout") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Text("←") }
                    }
                )
            }
        },
        floatingActionButton = {
            // Hide FAB in selection mode
            if (!selectionMode) {
                FloatingActionButton(onClick = {
                    showPicker = true
                    vm.updateQuery("")       // start fresh each time (optional)
                }) { Text("Add Exercise") }
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
                        val isSelected = selectedIds.contains(ex.id)

                        ExerciseCard(
                            ex = ex,
                            isSelected = isSelected,
                            onClick = {
                                if (selectionMode) {
                                    // Toggle selection
                                    if (isSelected) {
                                        selectedIds.remove(ex.id)
                                    } else {
                                        selectedIds.add(ex.id)
                                    }
                                    if (selectedIds.isEmpty()) {
                                        selectionMode = false
                                    }
                                } else {
                                    onExerciseClick(workoutId, ex.id)
                                }
                            },
                            onLongPress = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    if (!isSelected) {
                                        selectedIds.add(ex.id)
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }

    // Exercise picker bottom sheet
    if (showPicker) {
        ExercisePickerSheet(
            query = query,
            names = names,
            onQueryChange = vm::updateQuery,
            onPick = { chosen ->
                showPicker = false
                scope.launch {
                    vm.addExercise(chosen)
                    vm.markActive()
                }
            },
            onDismiss = { showPicker = false }
        )
    }

    // Delete confirmation dialog for selected exercises
    if (showDeleteConfirm && selectedIds.isNotEmpty()) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    if (count == 1) "Remove exercise?"
                    else "Remove $count exercises?"
                )
            },
            text = {
                Text("This will remove the selected exercises and all of their sets from this workout.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val ids = selectedIds.toList()
                    scope.launch {
                        vm.deleteExercises(ids)
                        vm.markActive()
                    }
                    selectedIds.clear()
                    selectionMode = false
                    showDeleteConfirm = false
                }) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseCard(
    ex: Exercise,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val config = ExerciseTypes.configForName(ex.name)

    val bgColor =
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = bgColor
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium)
            Text("Last: ${ex.lastDate}", style = MaterialTheme.typography.bodySmall)

            ex.sets.lastOrNull()?.let { last ->
                when (config.kind) {
                    com.example.get_ripped.data.model.ExerciseKind.TIMED_HOLD -> {
                        Text(
                            "Last set: ${last.reps} sec",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    com.example.get_ripped.data.model.ExerciseKind.REPS_ONLY -> {
                        Text(
                            "Last set: ${last.reps} reps",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    com.example.get_ripped.data.model.ExerciseKind.UNILATERAL_REPS -> {
                        Text(
                            "Last set: ${last.reps} reps (per side)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    com.example.get_ripped.data.model.ExerciseKind.WEIGHT_REPS -> {
                        val displayWeight =
                            if (last.weight % 1f == 0f) last.weight.toInt().toString()
                            else last.weight.toString()

                        Text(
                            "Last set: ${last.reps} reps @ $displayWeight lbs",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            ex.note?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
