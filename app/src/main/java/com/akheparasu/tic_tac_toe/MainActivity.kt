package com.akheparasu.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.akheparasu.tic_tac_toe.screens.CareerScreen
import com.akheparasu.tic_tac_toe.screens.GameScreen
import com.akheparasu.tic_tac_toe.screens.HomeScreen
import com.akheparasu.tic_tac_toe.screens.ScoreScreen
import com.akheparasu.tic_tac_toe.settings.LocalSettings
import com.akheparasu.tic_tac_toe.settings.SettingsDataStore
import com.akheparasu.tic_tac_toe.ui.theme.TicTacToeTheme
import com.akheparasu.tic_tac_toe.utils.Difficulty

class MainActivity : ComponentActivity() {
    private val settingsDataStore by lazy { SettingsDataStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
//            val difficulty = settingsDataStore.difficultyFlow.collectAsState(initial = Difficulty.Easy)
//            val theme = settingsDataStore.themeFlow.collectAsState(initial = false)
//            val volume = settingsDataStore.volumeFlow.collectAsState(initial = 1.0f)
//            val gridSize = settingsDataStore.gridSizeFlow.collectAsState(initial = 3)

            CompositionLocalProvider(LocalSettings provides settingsDataStore) {
                TicTacToeTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val navController: NavHostController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen(navController = navController)
                            }
                            composable("game") {
                                GameScreen(navController = navController)
                            }
                            composable("score") {
                                ScoreScreen(navController = navController)
                            }
                            composable("career") {
                                CareerScreen(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
