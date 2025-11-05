package com.example.get_ripped.ui.exercisedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseDetailViewModel(
    private val repo: WorkoutRepository,
    private val workoutId: Long,
    private val exerciseId: Long
) : ViewModel() {

    // Screen state (Exercise with its sets + note)
    val exercise: StateFlow<Exercise?> =
        repo.exerciseById(workoutId, exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Actions
    fun addSet(defaultReps: Int = 8, defaultWeight: Int = 0) = viewModelScope.launch {
        repo.addSet(workoutId, exerciseId, defaultReps, defaultWeight)
    }

    fun updateSet(index: Int, reps: Int, weight: Int) = viewModelScope.launch {
        repo.updateSet(workoutId, exerciseId, index, reps, weight)
    }

    fun removeSet(index: Int) = viewModelScope.launch {
        repo.removeSet(workoutId, exerciseId, index)
    }

    fun updateNote(note: String) = viewModelScope.launch {
        repo.updateExerciseNote(workoutId, exerciseId, note)
    }
}

/** Simple factory so you can pass repo + IDs without DI **/
class ExerciseDetailViewModelFactory(
    private val repo: WorkoutRepository,
    private val workoutId: Long,
    private val exerciseId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExerciseDetailViewModel::class.java))
        return ExerciseDetailViewModel(repo, workoutId, exerciseId) as T
    }
}
