package com.example.get_ripped.ui.workoutdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.ExerciseKind
import com.example.get_ripped.data.model.ExerciseTypes
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit,
    onExerciseClick: (Long, Long) -> Unit = { _, _ -> },
    onAddExercise: (Long) -> Unit
) {
    // ViewModel with repo + workoutId
    val vm: WorkoutDetailViewModel = viewModel(
        factory = WorkoutDetailViewModelFactory(repo, workoutId)
    )

    // Screen state
    val workout by vm.workout.collectAsState()
    val exercises by vm.exercises.collectAsState()

    // Local UI state for selection mode / multi-delete
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            if (selectionMode) {
                // Selection / delete mode AppBar
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        TextButton(
                            onClick = {
                                selectionMode = false
                                selectedIds.clear()
                            }
                        ) {
                            Text("←")
                        }
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
                // Normal AppBar
                CenterAlignedTopAppBar(
                    title = { Text(workout?.name ?: "Workout") },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("←")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            // Hide FAB in selection mode
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = { onAddExercise(workoutId) }
                ) {
                    Text(" Add Exercise ")
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Exercise list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = exercises,
                    key = { it.id }
                ) { ex ->
                    val isSelected = ex.id in selectedIds

                    ExerciseCard(
                        ex = ex,
                        isSelected = isSelected,
                        onClick = {
                            if (selectionMode) {
                                if (isSelected) {
                                    selectedIds.remove(ex.id)
                                    if (selectedIds.isEmpty()) {
                                        selectionMode = false
                                    }
                                } else {
                                    selectedIds.add(ex.id)
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
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete exercises?") },
            text = { Text("This will permanently delete the selected exercises from this workout.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vm.deleteExercises(selectedIds.toList())
                        selectedIds.clear()
                        selectionMode = false
                        showDeleteConfirm = false
                    }
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

            // HEADER: name + done check icon (if completed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ex.name,
                    style = MaterialTheme.typography.titleMedium
                )

                if (ex.isDone) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Last date
            Text(
                text = "Last: ${ex.lastDate}",
                style = MaterialTheme.typography.bodySmall
            )

            // Last set info
            ex.sets.lastOrNull()?.let { last ->
                Spacer(Modifier.height(2.dp))

                when (config.kind) {
                    ExerciseKind.TIMED_HOLD -> {
                        Text(
                            "Last set: ${last.reps} sec",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ExerciseKind.REPS_ONLY -> {
                        Text(
                            "Last set: ${last.reps} reps",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ExerciseKind.UNILATERAL_REPS -> {
                        Text(
                            "Last set: ${last.reps} reps (per side)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ExerciseKind.WEIGHT_REPS -> {
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

            // Optional note preview
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
