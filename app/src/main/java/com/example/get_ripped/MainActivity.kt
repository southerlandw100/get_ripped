package com.example.get_ripped

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import androidx.compose.material3.Text
import com.example.get_ripped.data.local.AppDb
import com.example.get_ripped.data.local.MIGRATION_6_7
import com.example.get_ripped.data.local.MIGRATION_7_8
import com.example.get_ripped.data.repo.RoomWorkoutRepository
import com.example.get_ripped.ui.home.HomeViewModel
import com.example.get_ripped.ui.navigation.AppNavGraph
import com.example.get_ripped.ui.theme.Get_rippedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent{ Text("HELLO FROM GET RIPPED!") }
        enableEdgeToEdge()

        // Build Room DB + Repo
        val db = Room.databaseBuilder(
            applicationContext,
            AppDb::class.java,
            "get_ripped.db"
        )
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .build()

        val repo = RoomWorkoutRepository(db.workoutDao())
        val homeVm = HomeViewModel(repo)

        setContent {
            Get_rippedTheme {
                AppNavGraph(
                    repo = repo,
                    homeVm = homeVm
                )
            }
        }
    }
}
