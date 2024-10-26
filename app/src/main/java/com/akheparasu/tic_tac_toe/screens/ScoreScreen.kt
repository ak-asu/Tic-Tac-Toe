package com.akheparasu.tic_tac_toe.screens

import android.bluetooth.BluetoothDevice
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.multiplayer.GameState
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.Preference

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

    DisposableEffect(Unit) {
        when (gameResult) {
            GameResult.Win -> audioController.onWin()
            GameResult.Fail -> audioController.onFail()
            GameResult.Draw -> audioController.onDraw()
        }
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Score Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Mode: ${gameMode.name}")
        Spacer(modifier = Modifier.height(16.dp))
        if (difficulty != null) {
            Text(text = "Difficulty: ${difficulty.name}")
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(text = gameResult.getDisplayText())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (gameMode == GameMode.TwoDevices) {
                if (originalConnectedDevice == null) {
                    Toast.makeText(
                        context,
                        "Connection error",
                        Toast.LENGTH_SHORT
                    ).show()
                    navController?.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                } else {
                    connectionService.receivedDataModel =
                        connectionService.receivedDataModel.copy(
                            gameState = GameState()
                        )
                    connectionService.setOnlineSetupStage(
                        OnlineSetupStage.GameStart
                    )
                    connectionService.connectDevice(
                        originalConnectedDevice
                    )
                }
            } else {
                navController?.navigate("game/${gameMode.name}/${preference}/${originalConnectedDevice?.address}") {
                    popUpTo("home") { inclusive = false }
                }
            }
        }) { Text(text = "Replay") }
    }
}
