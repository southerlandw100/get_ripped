package com.example.get_ripped.data.local
import com.example.get_ripped.data.model.*

fun WorkoutEntity.toDomain() = Workout(id, name, lastDate, note)
fun ExerciseEntity.toDomain(sets: List<SetEntry>) =
    Exercise(id, name, lastDate, note, sets)
fun SetEntity.toDomain() = SetEntry(reps, weight)
