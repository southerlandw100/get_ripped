package com.example.get_ripped.data.repo

import com.example.get_ripped.data.model.Exercise
import com.example.get_ripped.data.model.Workout
import com.example.get_ripped.data.model.SetEntry
import com.example.get_ripped.data.model.ExerciseHistoryEntry
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    val workouts: Flow<List<Workout>>
    suspend fun addWorkout(name: String)

    fun workoutById(id: Long): Flow<Workout?>

    fun exercisesForWorkout(workoutId: Long): Flow<List<Exercise>>
    suspend fun addExercise(workoutId: Long, name: String)

    fun exerciseById(workoutId: Long, exerciseId: Long): Flow<Exercise?>

    suspend fun setExerciseCompleted(workoutId: Long,
                                     exerciseId: Long,
                                     completed: Boolean
    )

    // All-time best set for this exercise name, or null if none
    suspend fun prForExerciseName(name: String): SetEntry?

    suspend fun resetCompletedIfNewDay(workoutId: Long)

    suspend fun renameWorkout(workoutId: Long, name: String)

    suspend fun exerciseHistoryForName(exerciseName: String): List<ExerciseHistoryEntry>

    // Existing bilateral / generic set APIs
    suspend fun addSet(
        workoutId: Long,
        exerciseId: Long,
        reps: Int,
        weight: Float
    )

    suspend fun updateSet(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        reps: Int,
        weight: Float
    )

    suspend fun clearAllSets(workoutId: Long, exerciseId: Long)

    // explicit unilateral variants (L/R reps)
    suspend fun addUnilateralSet(
        workoutId: Long,
        exerciseId: Long,
        repsLeft: Int,
        repsRight: Int,
        weight: Float
    )

    suspend fun updateUnilateralSet(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        repsLeft: Int,
        repsRight: Int,
        weight: Float
    )

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

    suspend fun deleteExerciseSession(exerciseName: String, workoutId: Long)

}
