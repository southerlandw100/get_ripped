package com.example.get_ripped.ui.exercisepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    workoutId: Long,
    repo: WorkoutRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Simple local VM instance
    val vm = remember { ExercisePickerViewModel(repo) }
    val names by vm.names.collectAsState()

    val queryState = remember { mutableStateOf("") }
    LaunchedEffect(queryState.value) {
        vm.updateQuery(queryState.value)
    }

    fun handlePick(name: String) {
        val normalized = normalizeName(name)
        if (normalized.isBlank()) {
            onBack()
            return
        }
        scope.launch {
            repo.addExercise(workoutId, normalized)
            repo.markWorkoutActive(workoutId)
        }
        onBack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Exercise") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val query = queryState.value

            // Search / type-ahead field
            OutlinedTextField(
                value = query,
                onValueChange = { queryState.value = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search or type a new name…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val normalized = normalizeName(query)
                        if (normalized.isNotEmpty()) {
                            handlePick(normalized)
                        } else {
                            onBack()
                        }
                    }
                ),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        TextButton(onClick = {
                            queryState.value = ""
                            vm.clearQuery()
                        }) { Text("Clear") }
                    }
                }
            )

            // If query doesn't match an existing item, offer a "Create" row
            val exists = names.any { it.equals(query.trim(), ignoreCase = true) }
            if (query.isNotBlank() && !exists) {
                CreateRow(
                    title = normalizeName(query),
                    onClick = { handlePick(normalizeName(query)) }
                )
            } else {
                HorizontalDivider()
            }

            // Suggestions list
            if (names.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matches")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(names, key = { it.lowercase() }) { name ->
                        SuggestionRow(
                            title = name,
                            onClick = { handlePick(name) }
                        )
                        HorizontalDivider()
                    }
                }
            }

        }
    }
}

@Composable
private fun SuggestionRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            "Select",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CreateRow(title: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Create \"$title\"", style = MaterialTheme.typography.bodyLarge)
        Text(
            "Create",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun normalizeName(raw: String): String {
    val trimmed = raw.trim().replace(Regex("\\s+"), " ")
    if (trimmed.isEmpty()) return ""
    return trimmed.replaceFirstChar { it.uppercase() }
}
