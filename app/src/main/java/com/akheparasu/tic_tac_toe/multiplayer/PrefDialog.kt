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
import com.akheparasu.tic_tac_toe.utils.Preference

@SuppressLint("MissingPermission")
@Composable
fun PrefDialog() {
    val navController = LocalNavController.current
    val connectionService = LocalConnectionService.current
    val selectedDevice = connectionService.connectedDevice.collectAsState()
    val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
    val prefClickAction: (Preference) -> Unit = {
        if (selectedDevice.value == null) {
            connectionService.setOnlineSetupStage(OnlineSetupStage.Idle)
        } else {
            connectionService.sendData(
                DataModel(
                    gameState = GameState(connectionEstablished = true),
                    metaData = MetaData(
                        choices = listOf(
                            PlayerChoice(
                                id = "player1",
                                name = ""
                            ),
                            PlayerChoice(
                                id = "player2",
                                name = selectedDevice.value!!.address
                            )
                        ),
                        miniGame = MiniGame(
                            player1Choice = if (it == Preference.Second) {
                                selectedDevice.value!!.address
                            } else {
                                ""
                            }
                        )
                    )
                )
            )
            connectionService.setOnlineSetupStage(OnlineSetupStage.Initialised)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (onlineSetupStage.value == OnlineSetupStage.GameStart) {
                navController?.navigate(
                    "game/${GameMode.Online.name}/${
                        if (selectedDevice.value?.address == connectionService.receivedDataModel!!.metaData.miniGame.player1Choice) {
                            Preference.Second.name
                        } else {
                            Preference.First.name
                        }
                    }/${selectedDevice.value?.address}"
                )
            } else if (onlineSetupStage.value != OnlineSetupStage.Initialised) {
                connectionService.receivedDataModel = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("Select Who Goes First") },
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
                    Spacer(modifier = Modifier.height(16.dp))
                    RoundedRectButton(
                        onClick = { prefClickAction(Preference.Second) },
                        text = "Opponent"
                    )
                } else if (connectionService.receivedDataModel != null &&
                    onlineSetupStage.value == OnlineSetupStage.Initialised
                ) {
                    RoundedRectButton(
                        onClick = {
                            connectionService.receivedDataModel = DataModel(
                                gameState = GameState(connectionEstablished = true),
                                metaData = MetaData(
                                    choices = listOf(
                                        PlayerChoice(
                                            id = "player1",
                                            name = selectedDevice.value!!.address
                                        ),
                                        connectionService.receivedDataModel!!.metaData.choices.last()
                                    ),
                                    miniGame = MiniGame(
                                        player1Choice = connectionService.receivedDataModel!!.metaData.miniGame.player1Choice.ifEmpty {
                                            selectedDevice.value!!.address
                                        },
                                        player2Choice = connectionService.receivedDataModel!!.metaData.miniGame.player1Choice.ifEmpty {
                                            selectedDevice.value!!.address
                                        }
                                    )
                                )
                            )
                            connectionService.sendData(connectionService.receivedDataModel!!)
                            connectionService.setOnlineSetupStage(OnlineSetupStage.GameStart)
                        },
                        text = "Play"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                RoundedRectButton(onClick = {
                    connectionService.sendData(
                        DataModel(gameState = GameState(connectionEstablished = false))
                    )
                    connectionService.setOnlineSetupStage(OnlineSetupStage.Idle)
                }, text = "Cancel")
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}
