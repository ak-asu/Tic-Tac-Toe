package com.akheparasu.tic_tac_toe

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akheparasu.tic_tac_toe.audio.AudioPlayer
import com.akheparasu.tic_tac_toe.multiplayer.Connections
import com.akheparasu.tic_tac_toe.multiplayer.PrefDialog
import com.akheparasu.tic_tac_toe.screens.AddScoreViewModel
import com.akheparasu.tic_tac_toe.screens.AddScoreViewModelFactory
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
import kotlin.random.Random


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
        val addScoreViewModel: AddScoreViewModel by viewModels {
            AddScoreViewModelFactory(application)
        }
        connectionService = Connections(this)
        val missingPermissions = connectionService.getMissingPermissions()
        val btEnableLauncher = registerForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            connectionService.getMissingPermissions()
        }
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        }.launch(missingPermissions.first + missingPermissions.second)
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
                            onlineSetupStage.value == OnlineSetupStage.Initialised
                        ) {
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
                                val originalPreference = Preference.valueOf(
                                    backStackEntry.arguments?.getString("preference")
                                        ?: Preference.First.name
                                )
                                val preference =
                                    if (originalPreference == Preference.NoPreference) {
                                        if (Random.nextBoolean()) {
                                            Preference.First
                                        } else {
                                            Preference.Second
                                        }
                                    } else {
                                        originalPreference
                                    }
                                val originalConnectedDeviceAddress =
                                    backStackEntry.arguments?.getString("deviceAddress")
                                val context = LocalContext.current
                                if (GameMode.entries.map { mode -> mode.name }
                                        .contains(gameModeName)) {
                                    val gameMode = GameMode.valueOf(gameModeName!!)
                                    if (gameMode == GameMode.TwoDevices) {
                                        val originalConnectedDevice =
                                            connectionService.getBtDeviceFromAddress(
                                                originalConnectedDeviceAddress
                                            )
                                        if (originalConnectedDevice != null) {
                                            if (onlineSetupStage.value == OnlineSetupStage.GameStart) {
                                                GameScreen(
                                                    gameMode,
                                                    preference,
                                                    originalConnectedDevice,
                                                    addScoreViewModel
                                                )
                                            }
                                        } else {
                                            connectionService.setOnlineSetupStage(OnlineSetupStage.Idle)
                                            Toast.makeText(
                                                context,
                                                "Connection error",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.popBackStack(
                                                route = "home",
                                                inclusive = false
                                            )
                                        }
                                    } else {
                                        GameScreen(
                                            gameMode,
                                            preference,
                                            null,
                                            addScoreViewModel
                                        )
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Game selection error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack(route = "home", inclusive = false)
                                }
                            }
                            composable("score/{gameModeName}/{preference}/{deviceAddress}/{difficulty}/{gameResult}") { backStackEntry ->
                                val gameModeName =
                                    backStackEntry.arguments?.getString("gameModeName")
                                val preference = Preference.valueOf(
                                    backStackEntry.arguments?.getString("preference")
                                        ?: Preference.First.name
                                )
                                val originalConnectedDeviceAddress =
                                    backStackEntry.arguments?.getString("deviceAddress")
                                val difficulty = backStackEntry.arguments?.getString("difficulty")
                                    ?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
                                val gameResult = GameResult.valueOf(
                                    backStackEntry.arguments?.getString("gameResult")
                                        ?: GameResult.Draw.name
                                )
                                val context = LocalContext.current
                                if (GameMode.entries.map { mode -> mode.name }
                                        .contains(gameModeName)) {
                                    val gameMode = GameMode.valueOf(gameModeName!!)
                                    ScoreScreen(
                                        gameMode,
                                        preference,
                                        difficulty,
                                        gameResult,
                                        connectionService.getBtDeviceFromAddress(
                                            originalConnectedDeviceAddress
                                        )
                                    )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Score screen error",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack(route = "home", inclusive = false)
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
        connectionService.unregisterReceiver()
    }
}