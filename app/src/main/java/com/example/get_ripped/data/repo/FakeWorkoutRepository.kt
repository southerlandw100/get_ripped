package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.*
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

    // Map of workoutId -> exercises
    private val _exercisesByWorkout = MutableStateFlow(
        mapOf<Long, List<Exercise>>(
            1L to listOf(
                Exercise(
                    id = 101, name = "Overhead Press", lastDate = "10/29",
                    note = "Felt strong",
                    sets = listOf(SetEntry(8, 95), SetEntry(8, 95), SetEntry(6, 100))
                ),
                Exercise(
                    id = 102, name = "Back Squat", lastDate = "10/29",
                    sets = listOf(SetEntry(5, 185), SetEntry(5, 185), SetEntry(5, 185))
                )
            ),
            2L to listOf(
                Exercise(
                    id = 201, name = "Bench Press", lastDate = "10/27",
                    sets = listOf(SetEntry(10, 135), SetEntry(8, 155), SetEntry(6, 165))
                )
            )
        )
    )

    override suspend fun addWorkout(name: String) {
        val nextId = (_workouts.value.maxOfOrNull { it.id } ?: 0) + 1
        _workouts.value = listOf(Workout(nextId, name, "Today")) + _workouts.value
        // initialize empty exercise list
        _exercisesByWorkout.value = _exercisesByWorkout.value + (nextId to emptyList())
    }

    override fun workoutById(id: Long): Flow<Workout?> =
        _workouts.map { it.find { w -> w.id == id } }

    override fun exercisesForWorkout(workoutId: Long): Flow<List<Exercise>> =
        _exercisesByWorkout.map { it[workoutId] ?: emptyList() }

    override suspend fun addExercise(workoutId: Long, name: String) {
        val current = _exercisesByWorkout.value[workoutId] ?: emptyList()
        val nextId = (
                _exercisesByWorkout.value.values.flatten().maxOfOrNull { it.id } ?: 100
                ) + 1
        val updated = listOf(
            Exercise(id = nextId, name = name, lastDate = "Today")
        ) + current
        _exercisesByWorkout.value = _exercisesByWorkout.value + (workoutId to updated)
    }

    override fun exerciseById(workoutId: Long, exerciseId: Long): Flow<Exercise?> =
        _exercisesByWorkout.map { map -> map[workoutId]?.find { it.id == exerciseId } }

    override suspend fun addSet(workoutId: Long, exerciseId: Long, reps: Int, weight: Int) {
        val list = _exercisesByWorkout.value[workoutId].orEmpty()
        val updated = list.map { ex ->
            if (ex.id == exerciseId) ex.copy(sets = ex.sets + SetEntry(reps, weight)) else ex
        }
        _exercisesByWorkout.value = _exercisesByWorkout.value + (workoutId to updated)
    }

    override suspend fun updateSet(workoutId: Long, exerciseId: Long, index: Int, reps: Int, weight: Int) {
        val list = _exercisesByWorkout.value[workoutId].orEmpty()
        val updated = list.map { ex ->
            if (ex.id == exerciseId && index in ex.sets.indices) {
                val newSets = ex.sets.toMutableList().apply { this[index] = SetEntry(reps, weight) }
                ex.copy(sets = newSets)
            } else ex
        }
        _exercisesByWorkout.value = _exercisesByWorkout.value + (workoutId to updated)
    }

    override suspend fun removeSet(workoutId: Long, exerciseId: Long, index: Int) {
        val list = _exercisesByWorkout.value[workoutId].orEmpty()
        val updated = list.map { ex ->
            if (ex.id == exerciseId && index in ex.sets.indices) {
                val newSets = ex.sets.toMutableList().apply { removeAt(index) }
                ex.copy(sets = newSets)
            } else ex
        }
        _exercisesByWorkout.value = _exercisesByWorkout.value + (workoutId to updated)
    }

    override suspend fun updateExerciseNote(workoutId: Long, exerciseId: Long, note: String) {
        val list = _exercisesByWorkout.value[workoutId].orEmpty()
        val updated = list.map { ex -> if (ex.id == exerciseId) ex.copy(note = note) else ex }
        _exercisesByWorkout.value = _exercisesByWorkout.value + (workoutId to updated)
    }

}
