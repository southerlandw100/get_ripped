package com.example.get_ripped.data.local
import com.example.get_ripped.data.model.*

fun WorkoutEntity.toDomain() = Workout(id, name, lastDate, note)
fun ExerciseEntity.toDomain(sets: List<SetEntry>) =
    Exercise(id, name, lastDate, note, completedAt = completedAt, sets)
fun SetEntity.toDomain() = SetEntry(id = id, reps = reps, weight = weight, repsLeft = repsLeft, repsRight = repsRight)

fun SetEntry.toEntity(exerciseId: Long) =
    SetEntity(
        id = id,
        exerciseId = exerciseId,
        reps = reps,
        weight = weight,
        repsLeft = repsLeft,
        repsRight = repsRight
    )
