package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

class FakeWorkoutRepository : WorkoutRepository {

    private val _workouts = MutableStateFlow(
        listOf(
            Workout(1, "Full Body A", "10/29", "Felt good today"),
            Workout(2, "Full Body B", "10/27", "Bodied it"),
            Workout(3, "Full Body A", "10/25"),
            Workout(4, "Full Body B", "10/23")
        )
    )

    override val workouts: Flow<List<Workout>> = _workouts

    override suspend fun addWorkout(name: String) {
        val nextID = (_workouts.value.maxOfOrNull { it.id } ?: 0) + 1
        _workouts.value = listOf(
            Workout(nextID, name, "Today")
        ) + _workouts.value
    }
}