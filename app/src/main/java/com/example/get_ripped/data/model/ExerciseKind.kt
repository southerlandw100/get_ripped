package com.example.get_ripped.data.model

/**
 * Conceptual "shape" of an exercise.
 *
 * This is NOT persisted yet; it's derived (for now) from the exercise name.
 */
enum class ExerciseKind {
    WEIGHT_REPS,      // standard: bench, squat, rows
    TIMED_HOLD,       // plank, wall sit
    UNILATERAL_REPS,  // lunges, curls where L/R reps can differ
    REPS_ONLY         // push-ups, sit-ups, pull-ups (no weight)
    // You can add DISTANCE_TIME later if you ever want sleds/runs, etc.
}
