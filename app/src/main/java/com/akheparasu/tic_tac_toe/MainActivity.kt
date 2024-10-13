package com.akheparasu.tic_tac_toe

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.akheparasu.tic_tac_toe.audio.AudioPlayer
import com.akheparasu.tic_tac_toe.multiplayer.Connections
import com.akheparasu.tic_tac_toe.screens.CareerScreen
import com.akheparasu.tic_tac_toe.screens.CareerViewModel
import com.akheparasu.tic_tac_toe.screens.CareerViewModelFactory
import com.akheparasu.tic_tac_toe.screens.GameScreen
import com.akheparasu.tic_tac_toe.screens.HomeScreen
import com.akheparasu.tic_tac_toe.screens.ScoreScreen
import com.akheparasu.tic_tac_toe.settings.SettingsDataStore
import com.akheparasu.tic_tac_toe.ui.AppBar
import com.akheparasu.tic_tac_toe.ui.theme.TicTacToeTheme
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings


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
                            composable("game/{gameModeName}") { backStackEntry ->
                                val gameModeName =
                                    backStackEntry.arguments?.getString("gameModeName")
                                if (GameMode.entries.map { mode -> mode.name }
                                        .contains(gameModeName)) {
                                    val gameMode = GameMode.valueOf(gameModeName!!)
                                    var originalConnectedDevice: BluetoothDevice? = null
                                    val context = LocalContext.current
                                    val allPermissions = connectionService.getMissingPermissions()
                                    val permissionLauncher = rememberLauncherForActivityResult(
                                        ActivityResultContracts.RequestMultiplePermissions()
                                    ) { permissions ->
                                        originalConnectedDevice =
                                            if (permissions.values.all { it }) {
                                                connectionService.connectedDevice.value
                                            } else {
                                                null
                                            }
                                    }
                                    if (gameMode == GameMode.Online) {
                                        val permissionsCheck = allPermissions.all {
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                it
                                            ) == PackageManager.PERMISSION_GRANTED
                                        }
                                        if (!permissionsCheck) {
                                            LaunchedEffect(Unit) {
                                                permissionLauncher.launch(allPermissions)
                                            }
                                        } else {
                                            if (originalConnectedDevice == null) {
                                                Snackbar { Text("No device connected") }
                                            } else {
                                                GameScreen(
                                                    gameMode,
                                                    originalConnectedDevice
                                                )
                                            }
                                        }
                                    } else {
                                        GameScreen(gameMode, originalConnectedDevice)
                                    }
                                }
                            }
                            composable("score") { ScoreScreen() }
                            composable("career") { CareerScreen(careerViewModel) }
                        }
                    }
                }
            }
        }
    }

//    @SuppressLint("MissingPermission")
//    private fun enableBluetooth() {
//        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//        startActivity(enableBtIntent)
//    }

    override fun onStop() {
        super.onStop()
        connectionService.unregisterReceiver()
    }

    override fun onResume() {
        super.onResume()
        connectionService.registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        connectionService.unregisterReceiver()
    }
}