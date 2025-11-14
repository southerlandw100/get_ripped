package com.example.get_ripped.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.get_ripped.data.model.Workout
import com.example.get_ripped.data.repo.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repo: WorkoutRepository) : ViewModel() {

    //Expose a stateflow that the UI can collect
    val workouts: StateFlow<List<Workout>> =
        repo.workouts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addWorkout(name: String) {
        viewModelScope.launch { repo.addWorkout(name) }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            repo.deleteWorkout(id)
        }
    }
}