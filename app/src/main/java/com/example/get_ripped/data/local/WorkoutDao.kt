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

    // Update workout timestamp & lastDate
    @Query("UPDATE workouts SET timestamp = :nowMillis, lastDate = :lastDate WHERE id = :workoutId")
    suspend fun touchWorkout(workoutId: Long, nowMillis: Long, lastDate: String)

    // Update lastDate for all exercises in a workout
    @Query("UPDATE exercises SET lastDate = :lastDate WHERE workoutId = :workoutId")
    suspend fun touchExercisesForWorkout(workoutId: Long, lastDate: String)

    @Query("DELETE FROM workouts WHERE id = :workoutId")
    suspend fun deleteWorkoutById(workoutId: Long)

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
    // Most recent exercise (any workout) with this name
    @Query("""
        SELECT * FROM exercises
        WHERE name = :name
        ORDER BY id DESC
        LIMIT 1
    """)
    suspend fun lastExerciseByName(name: String): ExerciseEntity?


    // Distinct exercise names across all workouts, sorted alphabetically
    @Query("SELECT DISTINCT name FROM exercises ORDER BY name COLLATE NOCASE")
    fun allExerciseNames(): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("""
    SELECT e.name
    FROM exercises e
    JOIN workouts w ON w.id = e.workoutId
    WHERE e.name LIKE :prefix || '%'
    GROUP BY e.name
    ORDER BY MAX(w.timestamp) DESC, e.name COLLATE NOCASE ASC
""")
    fun searchExerciseNames(prefix: String): kotlinx.coroutines.flow.Flow<List<String>>

    @Query("UPDATE exercises SET lastDate = :lastDate WHERE id = :exerciseId")
    suspend fun touchExercise(exerciseId: Long, lastDate: String)

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)

    @Query("UPDATE exercises SET completedAt = :completedAt WHERE id = :exerciseId")
    suspend fun updateExerciseCompletedAt(
        exerciseId: Long,
        completedAt: Long?
    )

    // -------- Sets --------

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY id ASC")
    fun setsForExercise(exerciseId: Long): kotlinx.coroutines.flow.Flow<List<SetEntity>>

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
