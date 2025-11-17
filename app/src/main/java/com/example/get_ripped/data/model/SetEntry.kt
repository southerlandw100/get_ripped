package com.example.get_ripped.data.model

data class SetEntry(
    val id: Long,
    val reps: Int,
    val weight: Float,
    val repsLeft: Int? = null,
    val repsRight: Int? = null
)