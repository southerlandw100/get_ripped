package com.example.get_ripped.data.repo

import com.example.get_ripped.data.local.*
import com.example.get_ripped.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max   // ⬅️ for unilateral reps → `reps` field

class RoomWorkoutRepository(private val dao: WorkoutDao) : WorkoutRepository {

    override val workouts: Flow<List<Workout>> =
        dao.workouts().map { list -> list.map { it.toDomain() } }

    override suspend fun addWorkout(name: String) {
        dao.insertWorkout(
            WorkoutEntity(
                name = name,
                timestamp = System.currentTimeMillis(),
                lastDate = "Today",
                note = null
            )
        )
    }

    override fun workoutById(id: Long): Flow<Workout?> =
        dao.workoutById(id).map { it?.toDomain() }

    override fun exercisesForWorkout(workoutId: Long): Flow<List<Exercise>> =
        dao.exercisesForWorkout(workoutId).map { entities ->
            entities.map { e ->
                Exercise(
                    id = e.id,
                    name = e.name,
                    lastDate = e.lastDate,
                    note = e.note,
                    sets = emptyList() // sets loaded in exerciseById
                )
            }
        }

    override suspend fun addExercise(workoutId: Long, name: String) {
        // Most recent exercise with this name (any workout)
        val previous = dao.lastExerciseByName(name)

        val initialLastDate = previous?.lastDate ?: ""

        // Insert at end (position)
        val nextPos = dao.maxPositionForWorkout(workoutId) + 1
        val newExerciseId = dao.insertExercise(
            ExerciseEntity(
                workoutId = workoutId,
                name = name,
                lastDate = initialLastDate,
                note = previous?.note,   // or null if you prefer not to copy
                position = nextPos
            )
        )

        // Prefill sets from last time if available
        if (previous != null) {
            val prevSets = dao.setsForExercise(previous.id).first()
            prevSets.forEach { set ->
                // ⬇️ copy all fields, including repsLeft / repsRight, but reset PK + exerciseId
                dao.insertSet(
                    set.copy(
                        id = 0, // autoGenerate
                        exerciseId = newExerciseId
                    )
                )
            }
        }
    }

    override fun exerciseById(workoutId: Long, exerciseId: Long): Flow<Exercise?> =
        combine(
            dao.exerciseById(workoutId, exerciseId),
            dao.setsForExercise(exerciseId)
        ) { exEntity, setEntities ->
            exEntity?.let { e ->
                val sets: List<SetEntry> = setEntities.map { it.toDomain() }
                Exercise(
                    id = e.id,
                    name = e.name,
                    lastDate = e.lastDate,
                    note = e.note,
                    sets = sets
                )
            }
        }

    // --- Existing "plain" set APIs (bilateral / reps-only / timed) ---

    override suspend fun addSet(
        workoutId: Long,
        exerciseId: Long,
        reps: Int,
        weight: Float
    ) {
        dao.insertSet(
            SetEntity(
                exerciseId = exerciseId,
                reps = reps,
                weight = weight,
                repsLeft = null,
                repsRight = null
            )
        )
    }

    override suspend fun updateSet(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        reps: Int,
        weight: Float
    ) {
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        if (index in current.indices) {
            val existing = current[index]
            val target: SetEntity = existing.copy(
                reps = reps,
                weight = weight,
                // keep any existing repsLeft / repsRight as-is for non-unilateral flows
                repsLeft = existing.repsLeft,
                repsRight = existing.repsRight
            )
            dao.updateSet(target)
        }
    }

    // --- New: unilateral-specific set APIs ---

    override suspend fun addUnilateralSet(
        workoutId: Long,
        exerciseId: Long,
        repsLeft: Int,
        repsRight: Int,
        weight: Float
    ) {
        // For the legacy `reps` field we store something representative.
        // Here we use the max of L/R (you could change to sum if you prefer).
        val combinedReps = max(repsLeft, repsRight)

        dao.insertSet(
            SetEntity(
                exerciseId = exerciseId,
                reps = combinedReps,
                weight = weight,
                repsLeft = repsLeft,
                repsRight = repsRight
            )
        )
    }

    override suspend fun updateUnilateralSet(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        repsLeft: Int,
        repsRight: Int,
        weight: Float
    ) {
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        if (index in current.indices) {
            val combinedReps = max(repsLeft, repsRight)
            val target: SetEntity = current[index].copy(
                reps = combinedReps,
                weight = weight,
                repsLeft = repsLeft,
                repsRight = repsRight
            )
            dao.updateSet(target)
        }
    }

    // --- Delete / cleanup ---

    override suspend fun removeSet(workoutId: Long, exerciseId: Long, index: Int) {
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        if (index in current.indices) {
            dao.deleteSet(current[index])
        }
    }

    override suspend fun removeEmptySets(workoutId: Long, exerciseId: Long) {
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        current
            .filter { set ->
                val left = set.repsLeft ?: 0
                val right = set.repsRight ?: 0
                set.weight == 0f && set.reps == 0 && left == 0 && right == 0
            }
            .forEach { emptySet ->
                dao.deleteSet(emptySet)
            }
    }

    override suspend fun updateExerciseNote(workoutId: Long, exerciseId: Long, note: String) {
        dao.updateExerciseNote(exerciseId, note)
    }

    override suspend fun repeatLastIfEmpty(workoutId: Long): Boolean {
        if (dao.exerciseCountForWorkout(workoutId) > 0) return false

        val current: WorkoutEntity = dao.workoutById(workoutId).first() ?: return false
        val prev: WorkoutEntity =
            dao.lastWorkoutByNameBefore(current.name, current.timestamp) ?: return false

        val prevExercises: List<ExerciseEntity> = dao.exercisesForWorkout(prev.id).first()
        if (prevExercises.isEmpty()) return false

        var posBase = dao.maxPositionForWorkout(workoutId) + 1
        for (prevEx in prevExercises) {
            val newExerciseId = dao.insertExercise(
                ExerciseEntity(
                    workoutId = workoutId,
                    name = prevEx.name,
                    lastDate = prevEx.lastDate,
                    note = prevEx.note,
                    position = posBase++
                )
            )

            val latestEx = dao.lastExerciseByName(prevEx.name)
            if (latestEx != null) {
                val latestSets: List<SetEntity> = dao.listSetsForExercise(latestEx.id)
                for (s in latestSets) {
                    // ⬇️ copy all set fields, including unilateral, but reset PK + exerciseId
                    dao.insertSet(
                        s.copy(
                            id = 0,
                            exerciseId = newExerciseId
                        )
                    )
                }
            }
        }
        return true
    }

    override suspend fun markWorkoutActive(workoutId: Long) {
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val dateString = formatter.format(Date(now))

        dao.touchWorkout(workoutId, now, dateString)
    }

    override suspend fun markExercisePerformed(workoutId: Long, exerciseId: Long) {
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val dateString = formatter.format(Date(now))

        // Workout is active because something was actually done
        dao.touchWorkout(workoutId, now, dateString)
        // Only this exercise’s lastDate is updated
        dao.touchExercise(exerciseId, dateString)
    }

    // --- Built-in exercise list for the picker ---

    private val builtInNamesFlow: Flow<List<String>> =
        flowOf(BuiltInExercises.defaultNames)

    override fun allExerciseNames(): Flow<List<String>> {
        val dbNames: Flow<List<String>> = dao.allExerciseNames()

        return combine(builtInNamesFlow, dbNames) { builtIn, db ->
            (builtIn + db)
                .distinctBy { it.lowercase() }  // avoid duplicates, case-insensitive
                .sorted()
        }
    }

    override fun searchExerciseNames(prefix: String): Flow<List<String>> {
        val trimmed = prefix.trim()
        if (trimmed.isBlank()) return allExerciseNames()

        val pattern = trimmed.lowercase()

        val builtInFiltered: Flow<List<String>> =
            builtInNamesFlow.map { list ->
                list.filter { it.lowercase().contains(pattern) }
            }

        val dbFiltered: Flow<List<String>> = dao.searchExerciseNames(trimmed)

        return combine(builtInFiltered, dbFiltered) { builtIn, db ->
            (builtIn + db)
                .distinctBy { it.lowercase() }
                .sorted()
        }
    }

    override suspend fun deleteWorkout(workoutId: Long) {
        dao.deleteWorkoutById(workoutId)
    }

    override suspend fun deleteExercise(exerciseId: Long) {
        dao.deleteExerciseById(exerciseId)
    }
}
