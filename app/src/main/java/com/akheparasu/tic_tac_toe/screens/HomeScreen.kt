package com.akheparasu.tic_tac_toe.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.multiplayer.DevicesDialog
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.Preference


@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val settings = LocalSettings.current
    val connectionService = LocalConnectionService.current
    val showDevicesDialog = remember { mutableStateOf(false) }
    val showPrefDialog = remember { mutableStateOf(false) }
    val gameMode = remember { mutableStateOf<GameMode?>(null) }
    val playerPrefFlow = settings.playerPrefFlow.collectAsState(initial = Preference.AskEveryTime)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        showDevicesDialog.value = permissions.values.all { it } && gameMode.value == GameMode.Online
    }

    LaunchedEffect(gameMode.value) {
        if (gameMode.value == GameMode.Online) {
            val allPermissions = connectionService.getMissingPermissions()
            permissionLauncher.launch(allPermissions)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showPrefDialog.value) {
            AlertDialog(
                onDismissRequest = { showPrefDialog.value = false },
                title = { Text(text = "Turn Preference") },
                text = {
                    Column {
                        Button(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(getGamePath(gameMode.value!!, Preference.First))
                            gameMode.value = null
                        }) {
                            Text(text = "First")
                        }
                        Button(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(
                                getGamePath(
                                    gameMode.value!!,
                                    Preference.Second
                                )
                            )
                            gameMode.value = null
                        }) {
                            Text(text = "Second")
                        }
                        Button(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(
                                getGamePath(
                                    gameMode.value!!,
                                    Preference.NoPreference
                                )
                            )
                            gameMode.value = null
                        }) {
                            Text(text = "No Preference")
                        }
                    }
                },
                confirmButton = { }
            )
        }
        if (showDevicesDialog.value) {
            DevicesDialog(
                onDismiss = { showDevicesDialog.value = false },
                onDeviceSelected = { device, pref ->
                    navController?.navigate(getGamePath(GameMode.Online, pref))
                    gameMode.value = null
                }
            )
        }
        Button(onClick = {
            gameMode.value = GameMode.Computer
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }) {
            Text(text = "Play against Computer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            gameMode.value = GameMode.Human
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }) {
            Text(text = "Play against Player")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            gameMode.value = GameMode.Online
            showPrefDialog.value = false
        }) {
            Text(text = "Play Online")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController?.navigate("career")
            gameMode.value = null
        }) {
            Text(text = "Career")
        }
    }
}

private fun getGamePath(gameMode: GameMode, preference: Preference): String {
    return "game/${gameMode.name}/${preference.name}"
}
