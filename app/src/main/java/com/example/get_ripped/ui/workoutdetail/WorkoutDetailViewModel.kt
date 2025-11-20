package com.example.get_ripped.ui.workoutdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.Workout
import com.example.get_ripped.data.repo.WorkoutRepository
import com.example.get_ripped.data.model.SetEntry
import com.example.get_ripped.data.repo.RoomWorkoutRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val repo: WorkoutRepository,
    private val workoutId: Long
) : ViewModel() {

    init {
        viewModelScope.launch {
            // Freshen up completion flags if this workout is from a previous day
            repo.resetCompletedIfNewDay(workoutId)
        }
    }

    // Base flow of exercises from the repo
    private val rawExercises: Flow<List<Exercise>> =
        repo.exercisesForWorkout(workoutId)

    // Workout as StateFlow so UI can collect with initial = null
    val workout: StateFlow<Workout?> =
        repo.workoutById(workoutId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // Exercises sorted so:
    //  - done exercises first, ordered by completedAt (oldest first)
    //  - not-done exercises after, preserving their original order
    val exercises: StateFlow<List<Exercise>> =
        repo.exercisesForWorkout(workoutId)
            .map { list ->
                val (done, notDone) = list.partition { it.isDone }

                val doneSorted = done.sortedBy { it.completedAt ?: Long.MAX_VALUE }

                doneSorted + notDone
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // All-time PR per exercise name for the exercises currently in this workout
    val prsByName: StateFlow<Map<String, SetEntry?>> =
        rawExercises
            .flatMapLatest { list ->
                flow {
                    val names = list.map { it.name }.distinct()
                    val prs: Map<String, SetEntry?> =
                        names.associateWith { name ->
                            repo.prForExerciseName(name)
                        }
                    emit(prs)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

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

    fun markActive() {
        viewModelScope.launch { repo.markWorkoutActive(workoutId) }
    }

    // Delete selected exercises by id
    fun deleteExercises(ids: List<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                repo.deleteExercise(id)
            }
        }
    }

    // --- Typeahead state for ExercisePickerSheet ---

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
