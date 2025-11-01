package com.example.get_ripped.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.get_ripped.data.repo.WorkoutRepository
import com.example.get_ripped.ui.home.HomeScreen
import com.example.get_ripped.ui.home.HomeViewModel
import com.example.get_ripped.ui.workoutdetail.WorkoutDetailScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_WORKOUT = "workout/{id}"

@Composable
fun AppNavGraph(
    repo: WorkoutRepository,
    homeVm: HomeViewModel
) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            val workouts by homeVm.workouts.collectAsState()
            HomeScreen(
                workouts = workouts,
                onAddWorkout = homeVm::addWorkout,
                onWorkoutClick = { id -> nav.navigate("workout/$id") }
            )
        }

        composable(
            route = ROUTE_WORKOUT,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            WorkoutDetailScreen(
                workoutId = id,
                repo = repo,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
