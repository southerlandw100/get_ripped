package com.example.get_ripped.data.model

data class ExerciseHistoryEntry (
    val workoutId: Long,
    val workoutName: String,
    val date: String?,
    val topSet: SetEntry?,
    val volume: Float,
    val sets: List<SetEntry>
)
