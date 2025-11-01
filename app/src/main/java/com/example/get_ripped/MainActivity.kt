package com.example.get_ripped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.get_ripped.data.repo.FakeWorkoutRepository
import com.example.get_ripped.ui.home.HomeScreen
import com.example.get_ripped.ui.home.HomeViewModel
import com.example.get_ripped.ui.theme.Get_rippedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // TEMP: in-memory repo + VM
        val repo = FakeWorkoutRepository()
        val vm = HomeViewModel(repo)

        setContent {
            Get_rippedTheme {
                val workouts by vm.workouts.collectAsState()

                HomeScreen(
                    workouts = workouts,
                    onAddWorkout = vm::addWorkout,
                    onWorkoutClick = { /* TODO: navigate to detail later */ }
                )
            }
        }
    }
}
