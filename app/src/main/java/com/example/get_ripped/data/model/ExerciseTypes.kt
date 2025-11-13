package com.example.get_ripped.data.model

/**
 * Central registry for exercise "types" (kinds + capabilities).
 *
 * For now this uses normalized exercise names as the key.
 * Later we can move this into the DB if we add a "kind" column.
 */
object ExerciseTypes {

    // Helper: normalize names so mapping is reliable
    private fun normalize(name: String): String =
        name.trim().lowercase()

    // Map of normalized exercise names -> type config
    private val definitions: Map<String, ExerciseTypeConfig> = buildMap {
        // --- TIMED HOLDS ---
        put(normalize("Plank"), ExerciseTypeConfig.TIMED_HOLD)
        put(normalize("Side Plank"), ExerciseTypeConfig.TIMED_HOLD)
        put(normalize("Wall Sit"), ExerciseTypeConfig.TIMED_HOLD)

        // --- UNILATERAL EXERCISES ---
        // lunges, curls, single-leg work, etc.
        put(normalize("Lunge"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Walking Lunge"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Reverse Lunge"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Bulgarian Split Squat"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Single-Leg Romanian Deadlift"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Single-Arm Row"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Dumbbell Curl"), ExerciseTypeConfig.UNILATERAL_REPS)
        put(normalize("Hammer Curl"), ExerciseTypeConfig.UNILATERAL_REPS)

        // --- REPS-ONLY (no weight field shown in UI) ---
        put(normalize("Push-Up"), ExerciseTypeConfig.REPS_ONLY)
        put(normalize("Pull-Up"), ExerciseTypeConfig.REPS_ONLY)
        put(normalize("Chin-Up"), ExerciseTypeConfig.REPS_ONLY)
        put(normalize("Sit-Up"), ExerciseTypeConfig.REPS_ONLY)
        put(normalize("Air Squat"), ExerciseTypeConfig.REPS_ONLY)

        // Most other built-ins default to weight×reps (bench, squat, etc.),
        // so we don't need to explicitly list them here.
    }

    /**
     * Returns the configuration for a given exercise name.
     * Falls back to a standard weight×reps config if unknown.
     */
    fun configForName(name: String): ExerciseTypeConfig {
        val key = normalize(name)
        return definitions[key] ?: ExerciseTypeConfig.DEFAULT_WEIGHT_REPS
    }

    /**
     * Convenience helpers if you just need to branch on behavior.
     */
    fun kindForName(name: String): ExerciseKind =
        configForName(name).kind

    fun isTimedHold(name: String): Boolean =
        configForName(name).kind == ExerciseKind.TIMED_HOLD

    fun isUnilateral(name: String): Boolean =
        configForName(name).kind == ExerciseKind.UNILATERAL_REPS
}
