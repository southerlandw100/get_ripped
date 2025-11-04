package com.example.get_ripped.data.local
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY id DESC")
    fun workouts(): Flow<List<WorkoutEntity>>

    @Insert suspend fun insertWorkout(w: WorkoutEntity): Long
    @Query("SELECT * FROM workouts WHERE id = :id") fun workoutById(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId ORDER BY id DESC")
    fun exercisesForWorkout(workoutId: Long): Flow<List<ExerciseEntity>>
    @Insert suspend fun insertExercise(e: ExerciseEntity): Long
    @Query("SELECT * FROM exercises WHERE id = :exerciseId AND workoutId = :workoutId")
    fun exerciseById(workoutId: Long, exerciseId: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM sets WHERE exerciseId = :exerciseId ORDER BY id ASC")
    fun setsForExercise(exerciseId: Long): Flow<List<SetEntity>>
    @Insert suspend fun insertSet(s: SetEntity): Long
    @Update suspend fun updateSet(s: SetEntity)
    @Delete suspend fun deleteSet(s: SetEntity)

    @Query("UPDATE exercises SET note = :note WHERE id = :exerciseId")
    suspend fun updateExerciseNote(exerciseId: Long, note: String)
}
