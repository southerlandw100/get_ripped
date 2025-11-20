package com.example.get_ripped.ui.exercisedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.ExerciseKind
import com.example.get_ripped.data.model.ExerciseTypeConfig
import com.example.get_ripped.data.model.ExerciseTypes
import com.example.get_ripped.data.model.SetEntry
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    workoutId: Long,
    exerciseId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Stream this exercise from the repo
    val exercise: Exercise? by repo.exerciseById(workoutId, exerciseId)
        .collectAsState(initial = null)

    // Local note draft, saved on change
    var noteDraft by remember(exercise?.note) {
        mutableStateOf(exercise?.note.orEmpty())
    }

    // Used to trigger focus on the newest set's Reps/Seconds (or L reps) field
    var focusNewestToggle by remember { mutableStateOf(false) }

    // Derive exercise config (kind + capabilities) from name
    val config: ExerciseTypeConfig? = exercise?.let { ExerciseTypes.configForName(it.name) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            // Clean up any 0/0 sets, then navigate back
                            scope.launch {
                                repo.removeEmptySets(workoutId, exerciseId)
                            }
                            onBack()
                        }
                    ) {
                        Text("←")
                    }
                },
                actions = {
                    val ex = exercise
                    if (ex != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            // 1) Mark as Done / Not Done
                            DropdownMenuItem(
                                text = {
                                    Text(if (ex.isDone) "Mark as Not Done" else "Mark as Done")
                                },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        repo.setExerciseCompleted(
                                            workoutId = workoutId,
                                            exerciseId = ex.id,
                                            completed = !ex.isDone
                                        )
                                    }
                                }
                            )

                            // 2) Clear all sets
                            DropdownMenuItem(
                                text = { Text("Clear all sets") },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        repo.clearAllSets(workoutId, ex.id)
                                    }
                                }
                            )

                            // 3) Delete exercise
                            DropdownMenuItem(
                                text = { Text("Delete exercise") },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

            )

        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // For unilateral exercises, create an "empty" unilateral set;
                        // for everything else, use the classic reps/weight set.
                        val cfg = config
                        if (cfg != null && cfg.kind == ExerciseKind.UNILATERAL_REPS) {
                            repo.addUnilateralSet(
                                workoutId = workoutId,
                                exerciseId = exerciseId,
                                repsLeft = 0,
                                repsRight = 0,
                                weight = 0f
                            )
                        } else {
                            // New set starts empty; interpretation depends on config
                            // For TIMED_HOLD this is an "empty" duration until edited or timer-logged.
                            repo.addSet(
                                workoutId = workoutId,
                                exerciseId = exerciseId,
                                reps = 0,
                                weight = 0f
                            )
                        }
                        repo.markExercisePerformed(workoutId, exerciseId)
                    }
                    // Flip toggle so newest row grabs focus
                    focusNewestToggle = !focusNewestToggle
                }
            ) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (exercise == null || config == null) {
                Text("Exercise not found")
                return@Column
            }

            // Note (autosave)
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { txt ->
                    noteDraft = txt
                    scope.launch {
                        repo.updateExerciseNote(workoutId, exerciseId, txt)
                    }
                },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // TIMED HOLD: show timer controls for planks / wall sits / etc.
            if (config.kind == ExerciseKind.TIMED_HOLD) {
                TimedHoldControls(
                    onFinished = { durationSeconds ->
                        scope.launch {
                            // Store duration in "reps", weight = 0 for now
                            repo.addSet(
                                workoutId = workoutId,
                                exerciseId = exerciseId,
                                reps = durationSeconds,
                                weight = 0f
                            )
                            repo.markWorkoutActive(workoutId)
                        }
                        // After logging, focus newest row if user taps into it
                        focusNewestToggle = !focusNewestToggle
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Sets", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(exercise!!.sets) { index, set ->
                    val isLast = index == exercise!!.sets.lastIndex
                    val autoFocus = isLast && focusNewestToggle

                    if (config.kind == ExerciseKind.UNILATERAL_REPS) {
                        // UNILATERAL: show L/R + weight, talk to unilateral APIs.
                        UnilateralSetRow(
                            set = set,
                            config = config,
                            autoFocusLeft = autoFocus,
                            onChange = { repsLeft, repsRight, weight ->
                                scope.launch {
                                    repo.updateUnilateralSet(
                                        workoutId = workoutId,
                                        exerciseId = exerciseId,
                                        index = index,
                                        repsLeft = repsLeft,
                                        repsRight = repsRight,
                                        weight = weight
                                    )
                                    repo.markExercisePerformed(workoutId, exerciseId)
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    repo.removeSet(workoutId, exerciseId, index)
                                    repo.markExercisePerformed(workoutId, exerciseId)
                                }
                            }
                        )
                    } else {
                        // Existing bilateral / reps-only / timed behavior.
                        SetRow(
                            set = set,
                            config = config,
                            autoFocusReps = autoFocus,
                            onChange = { newReps, newWeight ->
                                scope.launch {
                                    repo.updateSet(
                                        workoutId = workoutId,
                                        exerciseId = exerciseId,
                                        index = index,
                                        reps = newReps,
                                        weight = newWeight
                                    )
                                    repo.markExercisePerformed(workoutId, exerciseId)
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    repo.removeSet(workoutId, exerciseId, index)
                                    repo.markExercisePerformed(workoutId, exerciseId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }


// ⬇️ Add this AFTER the Scaffold body, still inside ExerciseDetailScreen
if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text("Delete exercise?") },
        text = {
            Text("This will remove this exercise and all of its sets from this workout.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    showDeleteDialog = false
                    scope.launch {
                        repo.deleteExercise(exerciseId)
                        onBack()
                    }
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteDialog = false }) {
                Text("Cancel")
            }
        }
    )
}
}



@Composable
private fun SetRow(
    set: SetEntry,
    config: ExerciseTypeConfig,
    autoFocusReps: Boolean,
    onChange: (Int, Float) -> Unit,
    onRemove: () -> Unit
) {
    // Local editable text state, initialised from the model
    var repsText by remember(set) {
        mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
    }
    var weightText by remember(set) {
        mutableStateOf(
            if (set.weight == 0f) ""
            else if (set.weight % 1f == 0f) set.weight.toInt().toString()
            else set.weight.toString()
        )
    }

    // Focus requester for the Reps/Seconds field (now on the RIGHT)
    val repsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocusReps) {
        if (autoFocusReps) {
            repsFocusRequester.requestFocus()
        }
    }

    val repsLabel = when (config.kind) {
        ExerciseKind.TIMED_HOLD -> "Seconds"
        else -> "Reps"
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Weight field (LEFT) – only if this exercise tracks weight
        if (config.tracksWeight) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { txt ->
                    weightText = txt
                    val weight = txt.toFloatOrNull() ?: 0f
                    onChange(set.reps, weight)
                },
                label = { Text("Weight") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // Reps / Seconds field (RIGHT)
        OutlinedTextField(
            value = repsText,
            onValueChange = { txt ->
                repsText = txt
                val reps = txt.toIntOrNull() ?: 0
                val weight = if (config.tracksWeight) set.weight else 0f
                onChange(reps, weight)
            },
            label = { Text(repsLabel) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(repsFocusRequester)
        )

        TextButton(onClick = onRemove) {
            Text("Remove")
        }
    }
}

@Composable
private fun UnilateralSetRow(
    set: SetEntry,
    config: ExerciseTypeConfig,
    autoFocusLeft: Boolean,
    onChange: (Int, Int, Float) -> Unit,
    onRemove: () -> Unit
) {
    // Start from left/right if present, otherwise fall back to reps.
    val initialLeft = set.repsLeft ?: 0
    val initialRight = set.repsRight ?: 0

    var leftText by remember(set) {
        mutableStateOf(if (initialLeft == 0) "" else initialLeft.toString())
    }
    var rightText by remember(set) {
        mutableStateOf(if (initialRight == 0) "" else initialRight.toString())
    }
    var weightText by remember(set) {
        mutableStateOf(
            if (set.weight == 0f) ""
            else if (set.weight % 1f == 0f) set.weight.toInt().toString()
            else set.weight.toString()
        )
    }

    val leftFocusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocusLeft) {
        if (autoFocusLeft) {
            leftFocusRequester.requestFocus()
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Weight (optional, based on config)
        if (config.tracksWeight) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { txt ->
                    weightText = txt
                    val weight = txt.toFloatOrNull() ?: 0f
                    val left = leftText.toIntOrNull() ?: 0
                    val right = rightText.toIntOrNull() ?: 0
                    onChange(left, right, weight)
                },
                label = { Text("Weight") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        // Left reps
        OutlinedTextField(
            value = leftText,
            onValueChange = { txt ->
                leftText = txt
                val left = txt.toIntOrNull() ?: 0
                val right = rightText.toIntOrNull() ?: 0
                val weight = if (config.tracksWeight) {
                    weightText.toFloatOrNull() ?: 0f
                } else 0f
                onChange(left, right, weight)
            },
            label = { Text("L Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(leftFocusRequester)
        )

        // Right reps
        OutlinedTextField(
            value = rightText,
            onValueChange = { txt ->
                rightText = txt
                val left = leftText.toIntOrNull() ?: 0
                val right = txt.toIntOrNull() ?: 0
                val weight = if (config.tracksWeight) {
                    weightText.toFloatOrNull() ?: 0f
                } else 0f
                onChange(left, right, weight)
            },
            label = { Text("R Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        TextButton(onClick = onRemove) {
            Text("Remove")
        }
    }
}

@Composable
private fun TimedHoldControls(
    onFinished: (durationSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableStateOf(0L) }

    // Simple timer loop while running
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMillis
            while (isRunning) {
                val now = System.currentTimeMillis()
                elapsedMillis = now - startTime
                kotlinx.coroutines.delay(100L)
            }
        }
    }

    val totalSeconds = (elapsedMillis / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Timer",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = timeText,
            style = MaterialTheme.typography.displayMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isRunning = true },
                enabled = !isRunning
            ) {
                Text("Start")
            }

            Button(
                onClick = { isRunning = false },
                enabled = isRunning
            ) {
                Text("Stop")
            }

            OutlinedButton(
                onClick = {
                    isRunning = false
                    elapsedMillis = 0L
                },
                enabled = !isRunning && totalSeconds > 0
            ) {
                Text("Reset")
            }
        }

        Button(
            onClick = {
                if (totalSeconds > 0) {
                    onFinished(totalSeconds)
                    // Reset after logging
                    elapsedMillis = 0L
                }
            },
            enabled = !isRunning && totalSeconds > 0
        ) {
            Text("Log ${totalSeconds}s Set")
        }
    }
}
