package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.Workout
import kotlinx.coroutines.flow.*

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
        val nextId = (_workouts.value.maxOfOrNull { it.id } ?: 0) + 1
        _workouts.value = listOf(Workout(nextId, name, "Today")) + _workouts.value
    }

    override fun workoutById(id: Long): Flow<Workout?> =
        _workouts.map { list -> list.find { it.id == id } }
}
