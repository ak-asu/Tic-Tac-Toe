package com.akheparasu.tic_tac_toe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.akheparasu.tic_tac_toe.screens.CareerScreen
import com.akheparasu.tic_tac_toe.screens.CareerViewModel
import com.akheparasu.tic_tac_toe.screens.CareerViewModelFactory
import com.akheparasu.tic_tac_toe.screens.GameScreen
import com.akheparasu.tic_tac_toe.screens.HomeScreen
import com.akheparasu.tic_tac_toe.screens.ScoreScreen
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.settings.SettingsDataStore
import com.akheparasu.tic_tac_toe.ui.AppBar
import com.akheparasu.tic_tac_toe.ui.theme.TicTacToeTheme
import com.akheparasu.tic_tac_toe.TwoPlayer
import com.akheparasu.tic_tac_toe.screens.AvailableDevicesScreen

class MainActivity : ComponentActivity() {
    private val settingsDataStore by lazy { SettingsDataStore(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val careerViewModel: CareerViewModel by viewModels {
            CareerViewModelFactory(application)
        }
        setContent {
            val navController: NavHostController = rememberNavController()

            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSettings provides settingsDataStore,
                ) {
                TicTacToeTheme {
                    Scaffold(
                        topBar = { AppBar() },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen()
                            }
                            /*
                            composable("game") {
                                GameScreen()
                            }
                             */
                            composable("game/{gameMode}") { backStackEntry ->
                                val gameMode = backStackEntry.arguments?.getString("gameMode")?.toBoolean()
                                GameScreen(gameMode = gameMode ?: false)
                            }
                            composable("score") {
                                ScoreScreen()
                            }
                            composable("career") {
                                CareerScreen(careerViewModel)
                            }
                            composable("available_devices") {
                                AvailableDevicesScreen(twoPlayer = TwoPlayer(LocalContext.current), activity = this@MainActivity)
                            }
                        }
                    }
                }
            }
        }
    }
}
