package com.example.get_ripped.ui.exercisedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
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

    // Stream this exercise from the repo
    val exercise: Exercise? by repo.exerciseById(workoutId, exerciseId)
        .collectAsState(initial = null)

    // Local note draft, saved on change
    var noteDraft by remember(exercise?.note) {
        mutableStateOf(exercise?.note.orEmpty())
    }

    // Used to trigger focus on the newest set's Reps/Seconds field
    var focusNewestToggle by remember { mutableStateOf(false) }

    // Derive exercise config (kind + capabilities) from name
    val config: ExerciseTypeConfig? = exercise?.let { ExerciseTypes.configForName(it.name) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        // New set starts empty; interpretation depends on config
                        // For TIMED_HOLD this is an "empty" duration until edited or timer-logged.
                        repo.addSet(workoutId, exerciseId, reps = 0, weight = 0f)
                        repo.markWorkoutActive(workoutId)
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
                                repo.markWorkoutActive(workoutId)
                            }
                        },
                        onRemove = {
                            scope.launch {
                                repo.removeSet(workoutId, exerciseId, index)
                                repo.markWorkoutActive(workoutId)
                            }
                        }
                    )
                }
            }
        }
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
        mutableStateOf(
            if (set.reps == 0) ""
            else set.reps.toString()
        )
    }

    var weightText by remember(set) {
        mutableStateOf(
            if (set.weight == 0f) ""
            else if (set.weight % 1f == 0f) set.weight.toInt().toString()
            else set.weight.toString()
        )
    }

    // Focus requester for the Reps/Seconds field
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
        // Reps / Seconds field
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

        // Weight field (decimal) only if this exercise tracks weight
        if (config.tracksWeight) {
            OutlinedTextField(
                value = weightText,
                onValueChange = { txt ->
                    weightText = txt
                    val weight = txt.toFloatOrNull() ?: 0f
                    onChange(set.reps, weight)
                },
                label = { Text("Weight") },   // shortened so it doesn't wrap
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

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
