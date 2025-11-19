package com.example.get_ripped.data.local
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutEntity::class, ExerciseEntity::class, SetEntity::class],
    version = 8,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sets ADD COLUMN repsLeft INTEGER")
        db.execSQL("ALTER TABLE sets ADD COLUMN repsRight INTEGER")

        // Backfill: existing rows treat the old `reps` as both sides.
        db.execSQL("UPDATE sets SET repsLeft = reps, repsRight = reps")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // completedAt is nullable Long, so SIMPLE add-column
        db.execSQL("ALTER TABLE exercises ADD COLUMN completedAt INTEGER")
    }
}
