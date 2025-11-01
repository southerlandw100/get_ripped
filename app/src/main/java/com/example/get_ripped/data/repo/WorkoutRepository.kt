package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    val workouts: Flow<List<Workout>>
    suspend fun addWorkout(name: String)

    fun workoutById(id: Long): Flow<Workout?>
}