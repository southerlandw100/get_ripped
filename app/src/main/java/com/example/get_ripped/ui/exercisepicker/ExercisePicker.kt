package com.example.get_ripped.ui.exercisepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Lightweight VM that exposes a live list of exercise names for a search/picker UI.
 * - When query is blank -> emits ALL names (Flow from Room)
 * - When query has text -> emits filtered names (prefix search)
 */
class ExercisePickerViewModel(
    private val repo: WorkoutRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Names that update automatically as DB changes and as the query changes. */
    val names: StateFlow<List<String>> = query
        .debounce(150) // keep typing smooth
        .flatMapLatest { q ->
            if (q.isBlank()) repo.allExerciseNames()
            else repo.searchExerciseNames(q)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun updateQuery(newQuery: String) {
        query.value = newQuery
    }

    fun clearQuery() {
        query.value = ""
    }
}
