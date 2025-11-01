package com.example.get_ripped.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Workout

@Composable
fun HomeScreen(
    workouts: List<Workout>,
    onAddWorkout: (String) -> Unit,
    onWorkoutClick: (Long) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    androidx.compose.material3.Scaffold(
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
            Text("WORKOUTS", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(workouts) { w ->
                    WorkoutCard(
                        w = w,
                        onClick = { onWorkoutClick(w.id) }   // <— report click with ID
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) onAddWorkout(name)
                    newName = ""
                    showDialog = false
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
            title = { Text("New Workout") },
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
private fun WorkoutCard(w: Workout, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },      // <— make the card tappable
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(w.name, style = MaterialTheme.typography.titleMedium)
            Text(w.lastDate, style = MaterialTheme.typography.bodySmall)
            w.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
    }
}
