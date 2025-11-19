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
import kotlinx.coroutines.flow.map

class ExerciseDetailViewModel(
    private val repo: WorkoutRepository,
    private val workoutId: Long,
    private val exerciseId: Long
) : ViewModel() {

    // Screen state (Exercise with its sets + note)
    val exercise: StateFlow<Exercise?> =
        repo.exerciseById(workoutId, exerciseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isDone: StateFlow<Boolean> =
        exercise
            .map { ex -> ex?.isDone ?: false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleCompleted() {
        val ex = exercise.value ?: return
        viewModelScope.launch {
            val newValue = !ex.isDone
            repo.setExerciseCompleted(workoutId, exerciseId, completed = newValue)
        }
    }


    // Actions
    fun addSet() = viewModelScope.launch {
        repo.addSet(workoutId, exerciseId, reps = 0, weight = 0f)
        repo.markWorkoutActive(workoutId)
    }

    fun updateSet(index: Int, reps: Int, weight: Float) = viewModelScope.launch {
        repo.updateSet(workoutId, exerciseId, index, reps, weight)
        val ex = exercise.value
        val hasRealValues = reps != 0 || weight != 0f

        if (ex?.completedAt == null && hasRealValues) {
            repo.setExerciseCompleted(
                workoutId = workoutId,
                exerciseId = exerciseId,
                completed = true
            )
        }
        repo.markWorkoutActive(workoutId)
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
