package com.akheparasu.tic_tac_toe.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
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
import com.akheparasu.tic_tac_toe.utils.PADDING_HEIGHT
import com.akheparasu.tic_tac_toe.utils.Preference
import com.akheparasu.tic_tac_toe.utils.SPACER_HEIGHT
import kotlin.random.Random


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
        connectionService.getMissingPermissions()
        if (connectionService.isBtEnabled()) {
            showDevicesDialog.value = true
        }
        gameMode.value = null
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val canShowDevicesDialog =
            permissions.values.all { it } && gameMode.value == GameMode.TwoDevices
        if (canShowDevicesDialog) {
            btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            gameMode.value = null
        }
    }

    LaunchedEffect(gameMode.value) {
        if (gameMode.value == GameMode.TwoDevices) {
            permissionLauncher.launch(connectionService.getMissingPermissions().first)
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
            .padding(PADDING_HEIGHT.dp),
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
                                    if (Random.nextBoolean()) {
                                        Preference.First
                                    } else {
                                        Preference.Second
                                    }
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
            DevicesDialog(onDismiss = {
                showDevicesDialog.value = false
            })
        }
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.Computer
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }, text = "Play against ${GameMode.Computer.getDisplayText()}")
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.OneDevice
            if (playerPrefFlow.value == Preference.AskEveryTime) {
                showPrefDialog.value = true
            } else {
                navController?.navigate(getGamePath(gameMode.value!!, playerPrefFlow.value))
                gameMode.value = null
            }
        }, text = "Play on ${GameMode.OneDevice.getDisplayText()}")
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        RoundedRectButton(onClick = {
            gameMode.value = GameMode.TwoDevices
            showPrefDialog.value = false
        }, text = "Play on ${GameMode.TwoDevices.getDisplayText()}")
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
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
