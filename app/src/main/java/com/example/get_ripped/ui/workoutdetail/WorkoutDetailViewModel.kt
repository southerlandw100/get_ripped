package com.example.get_ripped.ui.workoutdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.Workout
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutDetailViewModel(
    private val repo: WorkoutRepository,
    private val workoutId: Long
) : ViewModel() {

    // Screen state
    val workout: StateFlow<Workout?> =
        repo.workoutById(workoutId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<Exercise>> =
        repo.exercisesForWorkout(workoutId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Actions
    fun addExercise(name: String) = viewModelScope.launch {
        repo.addExercise(workoutId, name)
    }

    fun prefillIfEmpty(workoutId: Long) {
        viewModelScope.launch {
            repo.repeatLastIfEmpty(workoutId)
        }
    }
}

/** Simple factory so you can pass repo + workoutId without DI */
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
