package com.example.get_ripped.data.local
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class, SetEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}
