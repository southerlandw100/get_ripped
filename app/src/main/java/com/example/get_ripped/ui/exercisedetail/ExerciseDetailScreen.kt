package com.example.get_ripped.ui.exercisedetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.SetEntry
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    workoutId: Long,
    exerciseId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val exercise by repo.exerciseById(workoutId, exerciseId).collectAsState(initial = null)
    var noteDraft by remember(exercise?.note) { mutableStateOf(exercise?.note.orEmpty()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(exercise?.name ?: "Exercise") },
                navigationIcon = { IconButton(onClick = onBack) { Text("←") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch { repo.addSet(workoutId, exerciseId, reps = 8, weight = 0) }
            }) { Text("+") }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (exercise == null) {
                Text("Exercise not found")
                return@Column
            }

            // Note (autosave on change or when leaving this screen if you prefer)
            OutlinedTextField(
                value = noteDraft,
                onValueChange = {
                    noteDraft = it
                    scope.launch { repo.updateExerciseNote(workoutId, exerciseId, it) }
                },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Sets", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(exercise!!.sets) { index, set ->
                    SetRow(
                        set = set,
                        onChange = { newReps, newWeight ->
                            scope.launch { repo.updateSet(workoutId, exerciseId, index, newReps, newWeight) }
                        },
                        onRemove = {
                            scope.launch { repo.removeSet(workoutId, exerciseId, index) }
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
    onChange: (Int, Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Reps
        OutlinedTextField(
            value = set.reps.toString(),
            onValueChange = { txt -> onChange(txt.toIntOrNull() ?: 0, set.weight) },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        // Weight
        OutlinedTextField(
            value = set.weight.toString(),
            onValueChange = { txt -> onChange(set.reps, txt.toIntOrNull() ?: 0) },
            label = { Text("Weight (lb)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onRemove) { Text("Remove") }
    }
}
