package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.PLAYER_1
import com.akheparasu.tic_tac_toe.utils.PLAYER_2
import com.akheparasu.tic_tac_toe.utils.Preference
import com.akheparasu.tic_tac_toe.utils.SPACER_HEIGHT

@SuppressLint("MissingPermission")
@Composable
fun PrefDialog() {
    val navController = LocalNavController.current
    val connectionService = LocalConnectionService.current
    val selectedDevice = connectionService.connectedDevice.collectAsState()
    val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
    val prefClickAction: (Preference) -> Unit = {
        if (selectedDevice.value == null) {
            connectionService.disconnectDevice()
        } else {
            connectionService.receivedDataModel = connectionService.receivedDataModel.copy(
                metaData = connectionService.receivedDataModel.metaData.copy(
                    miniGame = MiniGame(
                        player1Choice = if (connectionService.getPlayerId() == PLAYER_1) {
                            if (it == Preference.First) {
                                connectionService.receivedDataModel.metaData.choices.first().name
                            } else {
                                connectionService.receivedDataModel.metaData.choices.last().name
                            }
                        } else {
                            ""
                        },
                        player2Choice = if (connectionService.getPlayerId() == PLAYER_2) {
                            if (it == Preference.First) {
                                connectionService.receivedDataModel.metaData.choices.last().name
                            } else {
                                connectionService.receivedDataModel.metaData.choices.first().name
                            }
                        } else {
                            ""
                        }
                    )
                )
            )
            connectionService.sendData(connectionService.receivedDataModel)
            connectionService.setOnlineSetupStage(OnlineSetupStage.Initialised)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (onlineSetupStage.value == OnlineSetupStage.GameStart) {
                navController?.navigate(
                    "game/${GameMode.TwoDevices.name}/${
                        if (selectedDevice.value?.address == connectionService.receivedDataModel.metaData.miniGame.player1Choice) {
                            Preference.Second.name
                        } else {
                            Preference.First.name
                        }
                    }/${selectedDevice.value?.address}"
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Who Goes First") },
        text = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (onlineSetupStage.value == OnlineSetupStage.Preference) {
                    RoundedRectButton(
                        onClick = { prefClickAction(Preference.First) },
                        text = "Me"
                    )
                    Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
                    RoundedRectButton(
                        onClick = { prefClickAction(Preference.Second) },
                        text = "Opponent"
                    )
                } else if (
                    ((connectionService.receivedDataModel.metaData.miniGame.player1Choice.isEmpty() &&
                            connectionService.getPlayerId() == PLAYER_1) ||
                            (connectionService.receivedDataModel.metaData.miniGame.player2Choice.isEmpty() &&
                                    connectionService.getPlayerId() == PLAYER_2)) &&
                    onlineSetupStage.value == OnlineSetupStage.Initialised
                ) {
                    RoundedRectButton(
                        onClick = {
                            connectionService.receivedDataModel =
                                connectionService.receivedDataModel.copy(
                                    metaData = connectionService.receivedDataModel.metaData.copy(
                                        miniGame = MiniGame(
                                            player1Choice = connectionService.receivedDataModel.metaData.miniGame.player1Choice.ifEmpty {
                                                connectionService.receivedDataModel.metaData.miniGame.player2Choice
                                            },
                                            player2Choice = connectionService.receivedDataModel.metaData.miniGame.player1Choice.ifEmpty {
                                                connectionService.receivedDataModel.metaData.miniGame.player2Choice
                                            }
                                        )
                                    )
                                )
                            connectionService.sendData(connectionService.receivedDataModel)
                            connectionService.setOnlineSetupStage(OnlineSetupStage.GameStart)
                        },
                        text = "Play"
                    )
                }
                Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
                RoundedRectButton(onClick = {
                    connectionService.sendData(
                        DataModel(gameState = GameState(connectionEstablished = false))
                    )
                    connectionService.disconnectDevice()
                }, text = "Cancel")
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}
