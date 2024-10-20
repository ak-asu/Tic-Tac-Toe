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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akheparasu.tic_tac_toe.audio.AudioPlayer
import com.akheparasu.tic_tac_toe.multiplayer.Connections
import com.akheparasu.tic_tac_toe.multiplayer.PrefDialog
import com.akheparasu.tic_tac_toe.screens.CareerScreen
import com.akheparasu.tic_tac_toe.screens.CareerViewModel
import com.akheparasu.tic_tac_toe.screens.CareerViewModelFactory
import com.akheparasu.tic_tac_toe.screens.GameScreen
import com.akheparasu.tic_tac_toe.screens.HomeScreen
import com.akheparasu.tic_tac_toe.screens.ScoreScreen
import com.akheparasu.tic_tac_toe.settings.SettingsDataStore
import com.akheparasu.tic_tac_toe.ui.AppBar
import com.akheparasu.tic_tac_toe.ui.theme.TicTacToeTheme
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.Preference


class MainActivity : ComponentActivity() {
    private val settingsDataStore by lazy { SettingsDataStore(this) }
    private val audioPlayerContext by lazy { AudioPlayer(this) }
    private lateinit var connectionService: Connections

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val careerViewModel: CareerViewModel by viewModels {
            CareerViewModelFactory(application)
        }
        connectionService = Connections(this)
        setContent {
            val navController: NavHostController = rememberNavController()
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSettings provides settingsDataStore,
                LocalConnectionService provides connectionService,
                LocalAudioPlayer provides audioPlayerContext,
            ) {
                val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
                TicTacToeTheme {
                    Scaffold(
                        topBar = { AppBar() },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        if (onlineSetupStage.value == OnlineSetupStage.Preference ||
                            onlineSetupStage.value == OnlineSetupStage.Initialised) {
                            PrefDialog()
                        }
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("home") {
                                HomeScreen()
                            }
                            composable("game/{gameModeName}/{preference}/{deviceAddress}") { backStackEntry ->
                                val gameModeName =
                                    backStackEntry.arguments?.getString("gameModeName")
                                val preference = Preference.valueOf(
                                    backStackEntry.arguments?.getString("preference")
                                        ?: Preference.First.name
                                )
                                val originalConnectedDeviceAddress =
                                    backStackEntry.arguments?.getString("deviceAddress")
                                if (GameMode.entries.map { mode -> mode.name }
                                        .contains(gameModeName)) {
                                    val gameMode = GameMode.valueOf(gameModeName!!)
                                    if (gameMode == GameMode.Online) {
                                        if (originalConnectedDeviceAddress != null) {
                                            GameScreen(
                                                gameMode,
                                                preference,
                                                originalConnectedDeviceAddress
                                            )
                                        }
                                    } else {
                                        GameScreen(
                                            gameMode,
                                            preference,
                                            originalConnectedDeviceAddress
                                        )
                                    }
                                }
                            }
                            composable("score/{gameModeName}/{difficulty}/{gameResult}") { backStackEntry ->
                                val gameModeName =
                                    backStackEntry.arguments?.getString("gameModeName")
                                val difficulty = backStackEntry.arguments?.getString("difficulty")
                                    ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                                val gameResult = GameResult.valueOf(
                                    backStackEntry.arguments?.getString("gameResult")
                                        ?: GameResult.Draw.name
                                )
                                if (GameMode.entries.map { mode -> mode.name }
                                        .contains(gameModeName)) {
                                    val gameMode = GameMode.valueOf(gameModeName!!)
                                    when (gameResult) {
                                        GameResult.Win -> audioPlayerContext.onWin()
                                        GameResult.Fail -> audioPlayerContext.onFail()
                                        GameResult.Draw -> audioPlayerContext.onDraw()
                                    }
                                    ScoreScreen(gameMode, difficulty, gameResult)
                                }
                            }
                            composable("career") { CareerScreen(careerViewModel) }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (connectionService.getMissingPermissions().first.isNotEmpty()
            || !connectionService.isBtEnabled()
        ) {
            connectionService.setOnlineSetupStage(OnlineSetupStage.NoService)
        }
    }

    override fun onStop() {
        super.onStop()
        connectionService.dispose()
    }
}