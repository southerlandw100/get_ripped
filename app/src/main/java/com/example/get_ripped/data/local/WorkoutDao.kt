package com.example.get_ripped.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // -------- Workouts --------

    @Query("SELECT * FROM workouts ORDER BY timestamp DESC")
    fun workouts(): Flow<List<WorkoutEntity>>

    @Insert
    suspend fun insertWorkout(w: WorkoutEntity): Long

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun workoutById(id: Long): Flow<WorkoutEntity?>

    // Find the last workout by exact name before a given time (for auto-repeat later)
    @Query("""
        SELECT * FROM workouts
        WHERE name = :name AND timestamp < :beforeMillis
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun lastWorkoutByNameBefore(name: String, beforeMillis: Long): WorkoutEntity?

    // -------- Exercises --------

    // NOTE: requires ExerciseEntity to have `position: Int`
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY `position` ASC, id ASC")
    fun exercisesForWorkout(workoutId: Long): Flow<List<ExerciseEntity>>


    @Insert
    suspend fun insertExercise(e: ExerciseEntity): Long

    @Query("SELECT * FROM exercises WHERE id = :exerciseId AND workoutId = :workoutId")
    fun exerciseById(workoutId: Long, exerciseId: Long): Flow<ExerciseEntity?>

    // Count exercises for a workout (to know if it's empty)
    @Query("SELECT COUNT(*) FROM exercises WHERE workoutId = :workoutId")
    suspend fun exerciseCountForWorkout(workoutId: Long): Int

    // Helper to compute next position for new exercise
    @Query("SELECT COALESCE(MAX(`position`), -1) FROM exercises WHERE workoutId = :workoutId")
    suspend fun maxPositionForWorkout(workoutId: Long): Int

    // Find the most recent occurrence of an exercise name across ALL workouts,
    // ordered by the workout's timestamp (newest first).
    @Query("""
        SELECT e.*
        FROM exercises e
        INNER JOIN workouts w ON w.id = e.workoutId
        WHERE e.name = :name
        ORDER BY w.timestamp DESC, e.id DESC
        LIMIT 1
    """)
    suspend fun lastExerciseByName(name: String): ExerciseEntity?

    // -------- Sets --------

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY id ASC")
    fun setsForExercise(exerciseId: Long): Flow<List<SetEntity>>

    // One-shot list of sets
    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY id ASC")
    suspend fun listSetsForExercise(exerciseId: Long): List<SetEntity>


    @Insert
    suspend fun insertSet(s: SetEntity): Long

    @Update
    suspend fun updateSet(s: SetEntity)

    @Delete
    suspend fun deleteSet(s: SetEntity)

    @Query("UPDATE exercises SET note = :note WHERE id = :exerciseId")
    suspend fun updateExerciseNote(exerciseId: Long, note: String)
}
