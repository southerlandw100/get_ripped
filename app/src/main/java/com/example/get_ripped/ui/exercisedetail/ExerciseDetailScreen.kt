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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Exercise
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

    // Used to trigger focus on the newest set's Reps field
    var focusNewestToggle by remember { mutableStateOf(false) }

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
                        // New set starts empty (0/0f) and we mark workout active
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
            if (exercise == null) {
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

            Text("Sets", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(exercise!!.sets) { index, set ->
                    val isLast = index == exercise!!.sets.lastIndex
                    val autoFocus = isLast && focusNewestToggle

                    SetRow(
                        set = set,
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

    // Focus requester for the Reps field
    val repsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocusReps) {
        if (autoFocusReps) {
            repsFocusRequester.requestFocus()
        }
    }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Reps field
        OutlinedTextField(
            value = repsText,
            onValueChange = { txt ->
                repsText = txt
                val reps = txt.toIntOrNull() ?: 0
                onChange(reps, set.weight)
            },
            label = { Text("Reps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .weight(1f)
                .focusRequester(repsFocusRequester)
        )

        // Weight field (decimal, same height as Reps)
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

        TextButton(onClick = onRemove) {
            Text("Remove")
        }
    }
}
