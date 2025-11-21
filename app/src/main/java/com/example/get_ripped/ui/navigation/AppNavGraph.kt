package com.example.get_ripped.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.example.get_ripped.ui.exercisehistory.ExerciseHistoryScreen
import com.example.get_ripped.data.repo.WorkoutRepository
import com.example.get_ripped.ui.home.HomeScreen
import com.example.get_ripped.ui.home.HomeViewModel
import com.example.get_ripped.ui.workoutdetail.WorkoutDetailScreen
import com.example.get_ripped.ui.exercisedetail.ExerciseDetailScreen
import com.example.get_ripped.ui.exercisepicker.ExercisePickerScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_WORKOUT = "workout/{id}"
private const val ROUTE_EXERCISE = "exercise/{workoutId}/{exerciseId}"
private const val ROUTE_EXERCISE_HISTORY = "exerciseHistory/{exerciseName}"

@Composable
fun AppNavGraph(
    repo: WorkoutRepository,
    homeVm: HomeViewModel
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = ROUTE_HOME) {

        // HOME
        composable(ROUTE_HOME) {
            val workouts by homeVm.workouts.collectAsState()
            HomeScreen(
                workouts = workouts,
                onAddWorkout = homeVm::addWorkout,
                onWorkoutClick = { id ->
                    nav.navigate("workout/$id")
                },
                onDeleteWorkout = { id -> homeVm.deleteWorkout(id) }
            )
        }

        // WORKOUT DETAIL
        composable(
            route = ROUTE_WORKOUT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("id") ?: -1L
            WorkoutDetailScreen(
                workoutId = workoutId,
                repo = repo,
                onBack = { nav.popBackStack() },
                onExerciseClick = { wId, exerciseId ->
                    nav.navigate("exercise/$workoutId/$exerciseId")
                },
                // NEW: navigate to full-screen exercise picker
                onAddExercise = { wId ->
                    nav.navigate("exercisePicker/$wId")
                }
            )
        }

        // EXERCISE DETAIL
        composable(
            route = ROUTE_EXERCISE,
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType },
                navArgument("exerciseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: -1L
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: -1L

            ExerciseDetailScreen(
                workoutId = workoutId,
                exerciseId = exerciseId,
                repo = repo,
                onBack = { nav.popBackStack() },
                onViewHistory = { exerciseName ->
                    val encoded = Uri.encode(exerciseName)
                    nav.navigate("exerciseHistory/$encoded")
                }
            )
        }

        // EXERCISE PICKER (full-screen)
        composable(
            route = "exercisePicker/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: return@composable

            ExercisePickerScreen(
                workoutId = workoutId,
                repo = repo,
                onBack = { nav.popBackStack() }
            )
        }

        // EXERCISE HISTORY
        composable(
            route = ROUTE_EXERCISE_HISTORY,
            arguments = listOf(
                navArgument("exerciseName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedName = backStackEntry.arguments?.getString("exerciseName") ?: ""
            val exerciseName = Uri.decode(encodedName)

            ExerciseHistoryScreen(
                exerciseName = exerciseName,
                repo = repo,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
