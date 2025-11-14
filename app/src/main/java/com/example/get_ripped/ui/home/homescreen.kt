package com.example.get_ripped.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.model.Workout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    workouts: List<Workout>,
    onAddWorkout: (String) -> Unit,
    onWorkoutClick: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit
) {
    var showNewWorkoutDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    // Selection mode state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectionMode) {
                // Selection mode top bar: back, count, trash
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
                            Text("🗑") // simple trash icon without extra deps
                        }
                    }
                )
            } else {
                // Normal top bar
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "GET RIPPED",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            // Only show FAB in normal mode
            if (!selectionMode) {
                FloatingActionButton(onClick = { showNewWorkoutDialog = true }) {
                    Text("+")
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
            // In selection mode we just show the list; no extra header needed.

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(workouts, key = { it.id }) { w ->
                    val isSelected = selectedIds.contains(w.id)

                    WorkoutCard(
                        w = w,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                // Toggle selection
                                if (isSelected) {
                                    selectedIds.remove(w.id)
                                } else {
                                    selectedIds.add(w.id)
                                }
                                // If nothing selected, exit selection mode
                                if (selectedIds.isEmpty()) {
                                    selectionMode = false
                                }
                            } else {
                                onWorkoutClick(w.id)
                            }
                        },
                        onLongPress = {
                            if (!selectionMode) {
                                selectionMode = true
                                if (!isSelected) {
                                    selectedIds.add(w.id)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // New workout dialog
    if (showNewWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showNewWorkoutDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val name = newName.trim()
                    if (name.isNotEmpty()) onAddWorkout(name)
                    newName = ""
                    showNewWorkoutDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewWorkoutDialog = false }) {
                    Text("Cancel")
                }
            },
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

    // Delete confirmation dialog (multi-delete)
    if (showDeleteConfirm && selectedIds.isNotEmpty()) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    if (count == 1) "Delete workout?"
                    else "Delete $count workouts?"
                )
            },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    // Call delete for each selected id
                    selectedIds.forEach { id -> onDeleteWorkout(id) }
                    selectedIds.clear()
                    selectionMode = false
                    showDeleteConfirm = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutCard(
    w: Workout,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
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
            Text(w.name, style = MaterialTheme.typography.titleMedium)
            Text(w.lastDate, style = MaterialTheme.typography.bodySmall)
            w.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
    }
}
