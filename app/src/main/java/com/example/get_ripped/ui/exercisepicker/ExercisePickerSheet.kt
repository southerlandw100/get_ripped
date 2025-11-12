package com.example.get_ripped.ui.exercisepicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions


/**
 * Bottom-sheet version.
 * - vm.names streams exercise names (all or filtered by query).
 * - onPick(name) is called when the user taps an item or confirms a new name.
 * - onDismiss() closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    vm: ExercisePickerViewModel,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val names by vm.names.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ExercisePickerContent(
            names = names,
            onQueryChange = vm::updateQuery,
            onPick = onPick,
            onDismiss = onDismiss
        )
    }
}

/**
 * Reusable content: you can embed this in a full-screen route if you don’t want a sheet.
 */
@Composable
fun ExercisePickerContent(
    names: List<String>,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Add exercise",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))

            // Search / type-ahead field
            OutlinedTextField(
                value = "", // We keep field stateless here; drive it from parent if you prefer.
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search or type a new name…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { /*TODO*/ }
                )
            )

            Spacer(Modifier.height(8.dp))

            Divider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp), // keep sheet reasonable
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // List existing names
                items(names, key = { it }) { name ->
                    ListItem(
                        headlineContent = { Text(name) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(name) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Tip text
            Text(
                text = "Tap an item to select, or type a new name above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
