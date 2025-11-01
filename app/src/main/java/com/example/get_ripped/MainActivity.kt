package com.example.get_ripped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.get_ripped.data.repo.FakeWorkoutRepository
import com.example.get_ripped.ui.home.HomeViewModel
import com.example.get_ripped.ui.navigation.AppNavGraph
import com.example.get_ripped.ui.theme.Get_rippedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = FakeWorkoutRepository()
        val homeVm = HomeViewModel(repo)

        setContent {
            Get_rippedTheme {
                AppNavGraph(repo = repo, homeVm = homeVm)
            }
        }
    }
}
