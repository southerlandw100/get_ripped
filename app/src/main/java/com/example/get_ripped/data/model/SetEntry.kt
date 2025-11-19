package com.example.get_ripped.data.model

data class SetEntry(
    val id: Long,
    val reps: Int,
    val weight: Float,
    val repsLeft: Int? = null,
    val repsRight: Int? = null
)

// A set counts as "non-empty" if any meaningful field is non-zero.
val SetEntry.isNonEmpty: Boolean
    get() {
        val left = repsLeft ?: 0
        val right = repsRight ?: 0
        return reps != 0 ||
                weight != 0f ||
                left != 0 ||
                right != 0
    }