package com.example.get_ripped.ui.workoutdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.Workout
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val repo: WorkoutRepository,
    private val workoutId: Long
) : ViewModel() {

    // existing state
    val workout: Flow<Workout?> = repo.workoutById(workoutId)
    val exercises: Flow<List<Exercise>> = repo.exercisesForWorkout(workoutId)

    fun prefillIfEmpty(workoutId: Long) {
        viewModelScope.launch {
            repo.repeatLastIfEmpty(workoutId)
        }
    }

    fun addExercise(name: String) {
        viewModelScope.launch {
            repo.addExercise(workoutId, name)
        }
    }

    fun addExerciseFromPicker(normalizedName: String) {
        viewModelScope.launch {
            repo.addExercise(workoutId, normalizedName)
            repo.markWorkoutActive(workoutId)
        }
    }

    fun markActive() {
        viewModelScope.launch { repo.markWorkoutActive(workoutId) }
    }

    // --- NEW: typeahead state for ExercisePickerSheet ---

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun updateQuery(q: String) { _query.value = q }

    @OptIn(FlowPreview::class)
    val names: StateFlow<List<String>> =
        _query
            .debounce(200)
            .flatMapLatest { q ->
                if (q.isBlank()) repo.allExerciseNames()
                else repo.searchExerciseNames(q)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}

/** Factory so WorkoutDetailScreen can get a VM with repo + workoutId */
class WorkoutDetailViewModelFactory(
    private val repo: WorkoutRepository,
    private val workoutId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WorkoutDetailViewModel::class.java))
        return WorkoutDetailViewModel(repo, workoutId) as T
    }
}
