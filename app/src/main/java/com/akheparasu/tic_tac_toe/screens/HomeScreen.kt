package com.akheparasu.tic_tac_toe.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.multiplayer.DevicesDialog
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.Preference


@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val settings = LocalSettings.current
    val connectionService = LocalConnectionService.current
    val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
    val showDevicesDialog = rememberSaveable { mutableStateOf(false) }
    val showPrefDialog = rememberSaveable { mutableStateOf(false) }
    val gameMode = rememberSaveable { mutableStateOf<GameMode?>(null) }
    val playerPrefFlow = settings.playerPrefFlow.collectAsState(initial = Preference.AskEveryTime)
    val btEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (connectionService.isBtEnabled()) {
            showDevicesDialog.value = true
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canShowDevicesDialog = permissions.values.all { it } && gameMode.value == GameMode.Online
        if (canShowDevicesDialog) {
            if (!connectionService.isBtEnabled()) {
                btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                showDevicesDialog.value = true
            }
        }
    }

    LaunchedEffect(gameMode.value) {
        if (gameMode.value == GameMode.Online) {
            val allPermissions = connectionService.getMissingPermissions()
            permissionLauncher.launch(allPermissions.first)
            gameMode.value = null
        }
    }
    LaunchedEffect(onlineSetupStage.value) {
        if (onlineSetupStage.value == OnlineSetupStage.NoService) {
            showDevicesDialog.value = false
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
                onDismissRequest = {
                    showPrefDialog.value = false
                    gameMode.value = null
                },
                title = { Text(text = "Turn Preference") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        RoundedRectButton(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(getGamePath(gameMode.value!!, Preference.First))
                            gameMode.value = null
                        }, text = "First")
                        RoundedRectButton(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(
                                getGamePath(
                                    gameMode.value!!,
                                    Preference.Second
                                )
                            )
                            gameMode.value = null
                        }, text = "Second")
                        RoundedRectButton(onClick = {
                            showPrefDialog.value = false
                            navController?.navigate(
                                getGamePath(
                                    gameMode.value!!,
                                    Preference.NoPreference
                                )
                            )
                            gameMode.value = null
                        }, text = "No Preference")
                    }
                },
                confirmButton = { }
            )
        }
        if (showDevicesDialog.value) {
            DevicesDialog(onDismiss = { showDevicesDialog.value = false } )
        }
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.Computer
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }, text = "Play against Computer")
        Spacer(modifier = Modifier.height(16.dp))
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.Human
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }, text = "Play against Player")
        Spacer(modifier = Modifier.height(16.dp))
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.Online
            showPrefDialog.value = false
        }, text = "Play Online")
        Spacer(modifier = Modifier.height(16.dp))
        RoundedRectButton(onClick = {
            navController?.navigate("career")
            gameMode.value = null
        }, text = "View Career")
    }
}

private fun getGamePath(
    gameMode: GameMode,
    preference: Preference,
    deviceAddress: String? = null
): String {
    return "game/${gameMode.name}/${preference.name}/${deviceAddress}"
}
