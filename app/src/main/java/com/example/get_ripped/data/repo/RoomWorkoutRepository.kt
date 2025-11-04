package com.example.get_ripped.data.repo

import com.example.get_ripped.data.local.*
import com.example.get_ripped.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RoomWorkoutRepository(private val dao: WorkoutDao) : WorkoutRepository {
    override val workouts: Flow<List<Workout>> =
        dao.workouts().map { list -> list.map { it.toDomain() } }

    override suspend fun addWorkout(name: String) {
        dao.insertWorkout(WorkoutEntity(name = name, lastDate = "Today", note = null))
    }

    override fun workoutById(id: Long): Flow<Workout?> =
        dao.workoutById(id).map { it?.toDomain() }

    override fun exercisesForWorkout(workoutId: Long): Flow<List<Exercise>> =
        // exercise list for the workout (no sets here).
        dao.exercisesForWorkout(workoutId).map { entities ->
            entities.map { e ->
                Exercise(
                    id = e.id,
                    name = e.name,
                    lastDate = e.lastDate,
                    note = e.note,
                    sets = emptyList() // Sets are fetched in exerciseById
                )
            }
        }

    override suspend fun addExercise(workoutId: Long, name: String) {
        dao.insertExercise(ExerciseEntity(workoutId = workoutId, name = name, lastDate = "Today", note = null))
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

    override suspend fun addSet(workoutId: Long, exerciseId: Long, reps: Int, weight: Int) {
        dao.insertSet(SetEntity(exerciseId = exerciseId, reps = reps, weight = weight))
    }

    override suspend fun updateSet(
        workoutId: Long,
        exerciseId: Long,
        index: Int,
        reps: Int,
        weight: Int
    ) {
        // Fetch current sets, update the one at `index`
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        if (index in current.indices) {
            val target: SetEntity = current[index].copy(reps = reps, weight = weight)
            dao.updateSet(target)
        }
    }

    override suspend fun removeSet(workoutId: Long, exerciseId: Long, index: Int) {
        val current: List<SetEntity> = dao.setsForExercise(exerciseId).first()
        if (index in current.indices) {
            dao.deleteSet(current[index])
        }
    }


    override suspend fun updateExerciseNote(workoutId: Long, exerciseId: Long, note: String) {
        dao.updateExerciseNote(exerciseId, note)
    }
}
