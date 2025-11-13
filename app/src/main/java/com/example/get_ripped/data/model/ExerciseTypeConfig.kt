package com.example.get_ripped.data.model

/**
 * Configuration describing what kind of data an exercise tracks.
 *
 * This is deliberately flexible so we can expand later without schema changes.
 */
data class ExerciseTypeConfig(
    val kind: ExerciseKind,

    // Does the exercise track a barbell/dumbbell load?
    val tracksWeight: Boolean,

    // Does it track "reps" as a count?
    val tracksReps: Boolean,

    // Does it track a duration (e.g., seconds)?
    val tracksTime: Boolean,

    // Does it track left/right within a single set?
    val tracksSides: Boolean
) {
    companion object {
        val DEFAULT_WEIGHT_REPS = ExerciseTypeConfig(
            kind = ExerciseKind.WEIGHT_REPS,
            tracksWeight = true,
            tracksReps = true,
            tracksTime = false,
            tracksSides = false
        )

        val TIMED_HOLD = ExerciseTypeConfig(
            kind = ExerciseKind.TIMED_HOLD,
            tracksWeight = false,   // tweak later if you want weighted planks
            tracksReps = false,
            tracksTime = true,
            tracksSides = false
        )

        val UNILATERAL_REPS = ExerciseTypeConfig(
            kind = ExerciseKind.UNILATERAL_REPS,
            tracksWeight = true,
            tracksReps = true,
            tracksTime = false,
            tracksSides = true
        )

        val REPS_ONLY = ExerciseTypeConfig(
            kind = ExerciseKind.REPS_ONLY,
            tracksWeight = false,
            tracksReps = true,
            tracksTime = false,
            tracksSides = false
        )
    }
}
