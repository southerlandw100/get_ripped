package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    val workouts: Flow<List<Workout>>
    suspend fun addWorkout(name: String)

    fun workoutById(id: Long): Flow<Workout?>

    fun exercisesForWorkout(workoutId: Long): Flow<List<Exercise>>
    suspend fun addExercise(workoutId: Long, name: String)

    fun exerciseById(workoutId: Long, exerciseId: Long): Flow<Exercise?>

    suspend fun addSet(workoutId: Long, exerciseId: Long, reps: Int, weight: Float)
    suspend fun updateSet(workoutId: Long, exerciseId: Long, index: Int, reps: Int, weight: Float)
    suspend fun removeSet(workoutId: Long, exerciseId: Long, index: Int)
    suspend fun removeEmptySets(workoutId: Long, exerciseId: Long)

    suspend fun updateExerciseNote(workoutId: Long, exerciseId: Long, note: String)

    suspend fun repeatLastIfEmpty(workoutId: Long): Boolean
    suspend fun markWorkoutActive(workoutId: Long)
    suspend fun markExercisePerformed(workoutId: Long, exerciseId: Long)


    // --- Exercise picker helpers (Flow so UI auto-updates) ---
    fun allExerciseNames(): Flow<List<String>>
    fun searchExerciseNames(prefix: String): Flow<List<String>>

    suspend fun deleteWorkout(workoutId: Long)
    suspend fun deleteExercise(exerciseId: Long)
}
