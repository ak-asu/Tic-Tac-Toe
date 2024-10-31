package com.akheparasu.tic_tac_toe.screens

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.multiplayer.GameState
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.PADDING_HEIGHT
import com.akheparasu.tic_tac_toe.utils.Preference
import com.akheparasu.tic_tac_toe.utils.SPACER_HEIGHT

@Composable
fun ScoreScreen(
    gameMode: GameMode,
    preference: Preference,
    difficulty: Difficulty?,
    gameResult: GameResult,
    originalConnectedDevice: BluetoothDevice?
) {
    val context = LocalContext.current
    val audioController = LocalAudioPlayer.current
    val navController = LocalNavController.current
    val connectionService = LocalConnectionService.current
    val onlineSetupStage = if (gameMode == GameMode.TwoDevices) {
        connectionService.onlineSetupStage.collectAsState()
    } else {
        null
    }

    if (gameMode == GameMode.TwoDevices) {
        LaunchedEffect(onlineSetupStage?.value) {
            if (onlineSetupStage?.value == OnlineSetupStage.GameStart) {
                navController?.navigate("game/${gameMode.name}/${preference.name}/${originalConnectedDevice?.address}") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }
        BackHandler {
            connectionService.disconnectDevice()
            navController?.popBackStack(route = "home", inclusive = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PADDING_HEIGHT.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Mode: ${gameMode.name}")
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        Text(text = "Difficulty: ${difficulty?.name ?: gameMode.getDisplayText()}")
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        Text(text = gameResult.getDisplayText(), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        RoundedRectButton(
            onClick = {
                if (gameMode == GameMode.TwoDevices) {
                    if (originalConnectedDevice == null) {
                        Toast.makeText(
                            context,
                            "Connection error",
                            Toast.LENGTH_SHORT
                        ).show()
                        connectionService.disconnectDevice()
                        navController?.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        connectionService.receivedDataModel =
                            connectionService.receivedDataModel.copy(
                                gameState = GameState()
                            )
                        connectionService.sendData(connectionService.receivedDataModel)
                        connectionService.setOnlineSetupStage(OnlineSetupStage.GameStart)
                    }
                } else {
                    navController?.navigate("game/${gameMode.name}/${preference}/${originalConnectedDevice?.address}") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            },
            text = "Replay"
        )
    }
}
