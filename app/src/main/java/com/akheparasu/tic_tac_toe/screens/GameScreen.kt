package com.akheparasu.tic_tac_toe.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.akheparasu.tic_tac_toe.algorithms.AlgoController
import com.akheparasu.tic_tac_toe.multiplayer.GameState
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.GridEntry
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.Preference
import kotlinx.coroutines.async

@Composable
fun GameScreen(
    gameMode: GameMode,
    preference: Preference,
    originalConnectedDeviceAddress: String?
) {
    val algoController = AlgoController(gameMode)
    val settings = LocalSettings.current
    val navController = LocalNavController.current
    val audioController = LocalAudioPlayer.current
    val difficultyFlow = settings.difficultyFlow.collectAsState(initial = Difficulty.Easy)
    val gridSaver = Saver(
        save = { grid: MutableState<Array<Array<GridEntry>>> ->
            grid.value.map { r -> r.map { it.name }.toTypedArray() }.toTypedArray()
        },
        restore = { saved: Array<Array<String>> ->
            mutableStateOf(saved.map { r -> r.map { GridEntry.valueOf(it) }.toTypedArray() }
                .toTypedArray())
        }
    )
    var grid by rememberSaveable(saver = gridSaver) { mutableStateOf(Array(3) { Array(3) { GridEntry.E } }) }
    var playerTurn by rememberSaveable { mutableStateOf(preference == Preference.First) }
    val isGameComplete: (Array<Array<GridEntry>>) -> Boolean =
        { !it.any { c -> c.any { v -> v == GridEntry.E } } }
    val connectionService = LocalConnectionService.current
    val connectedDevice = if (gameMode == GameMode.Online) {
        connectionService.connectedDevice.collectAsState(initial = null)
    } else {
        null
    }
    connectionService.setOnDataReceived { dataModel ->
        grid = dataModel.gameState.board.map { r -> r.map { GridEntry.valueOf(it) }.toTypedArray() }
            .toTypedArray()
        playerTurn = if (preference == Preference.First) {
            dataModel.gameState.turn.toInt() % 2 == 0
        } else {
            dataModel.gameState.turn.toInt() % 2 == 1
        }
    }
    var gamePaused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playerTurn) {
        if (!playerTurn && gameMode != GameMode.Human) {
            val sendData = {
                if (gameMode == GameMode.Online) {
                    connectionService.sendData(
                        connectionService.receivedDataModel!!.copy(
                            gameState = GameState(
                                board = grid.map { it.map { e -> e.name } },
                                turn = "${connectionService.receivedDataModel!!.gameState.turn.toInt() + 1}",
                                connectionEstablished = true
                            )
                        )
                    )
                }
            }
            grid = async {
                algoController.runOpponentTurn(
                    grid,
                    difficultyFlow.value,
                    sendData
                )
            }.await()
            isGameComplete(grid)
            playerTurn = true
        }
    }
    if (gameMode == GameMode.Online) {
        LaunchedEffect(connectedDevice) {
            if (connectedDevice?.value?.address != originalConnectedDeviceAddress) {
                gamePaused = true
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            connectionService.setOnDataReceived()
            connectionService.stopDiscovery()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val maxCellSize = 0.9f * minOf(screenWidth.value, screenHeight.value) / 3

    if (gamePaused) {
        Text("Game paused")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text(
                    text = "Player: X", fontWeight = if (playerTurn) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Thin
                    }
                )
                Text(text = " | ")
                Text(
                    text = "Opponent: O", fontWeight = if (playerTurn) {
                        FontWeight.Thin
                    } else {
                        FontWeight.Bold
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            for (i in 0 until 3) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (j in 0 until 3) {
                        GridCell(
                            value = grid[i][j].getDisplayText(),
                            maxCellSize = maxCellSize,
                            onTap = {
                                if (grid[i][j] == GridEntry.E) {
                                    if (playerTurn) {
                                        grid[i][j] = GridEntry.X
                                        playerTurn = false
                                        //Check for the win condition
                                        if (checkWinner(grid)?.equals(GridEntry.X) == true) {
                                            navController?.navigate("score/${gameMode.name}/${difficultyFlow.value}/${GameResult.Win}")
                                        } else {
                                            audioController.onPlayerTap()
                                        }
                                    } else {
                                        //Player 2 Game Mode
                                        if (gameMode == GameMode.Human) {
                                            grid[i][j] = GridEntry.O
                                            playerTurn = true
                                            //Check for the win condition
                                            if (checkWinner(grid)?.equals(GridEntry.O) == true) {
                                                navController?.navigate("score/${gameMode.name}/${null}/${GameResult.Fail}")
                                            } else {
                                                audioController.onOpponentTap()
                                            }
                                        }
                                        //Computer Game Mode
                                        else {
                                            Log.i(
                                                "someTag",
                                                "We will have computer take their turn"
                                            )
                                            //Check for the win condition

                                            if (checkWinner(grid)?.equals(GridEntry.O) == true) {

                                            } else if (checkWinner(grid)?.equals(GridEntry.X) == true) {

                                            }
                                        }
                                    }
                                }
                            })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            RoundedRectButton(onClick = {
                grid = Array(3) { Array(3) { GridEntry.E } }
                playerTurn = true
            }, text = "Reset Game")
        }
    }
}

@Composable
fun GridCell(value: String, maxCellSize: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .requiredSizeIn(
                minWidth = 50.dp,
                minHeight = 50.dp,
                maxWidth = min(100.dp, maxCellSize.dp),
                maxHeight = min(100.dp, maxCellSize.dp)
            )
            .padding(8.dp)
            //.indication(interactionSource, LocalIndication.current),
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
        ) {
            drawRect(color = Color.Gray)
        }
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 75.sp
        )
    }
}

// This function checks all of the possibilities of a win and returns "X" or "O" depending on who won
fun checkWinner(grid: Array<Array<GridEntry>>): GridEntry? {
    // List all of the possible ways to win
    val winningCombinations = listOf(
        listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)), // Row 1
        listOf(Pair(1, 0), Pair(1, 1), Pair(1, 2)), // Row 2
        listOf(Pair(2, 0), Pair(2, 1), Pair(2, 2)), // Row 3
        listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)), // Column 1
        listOf(Pair(0, 1), Pair(1, 1), Pair(2, 1)), // Column 2
        listOf(Pair(0, 2), Pair(1, 2), Pair(2, 2)), // Column 3
        listOf(Pair(0, 0), Pair(1, 1), Pair(2, 2)), // Diagonal top-left to bottom-right
        listOf(Pair(0, 2), Pair(1, 1), Pair(2, 0))  // Diagonal top-right to bottom-left
    )

    // Check if any winning combination is met
    for (combination in winningCombinations) {
        val (first, second, third) = combination
        val (i1, j1) = first
        val (i2, j2) = second
        val (i3, j3) = third

        // Return X or O
        if (grid[i1][j1] == grid[i2][j2] && grid[i2][j2] == grid[i3][j3] && grid[i1][j1] != GridEntry.E) {
            return grid[i1][j1]
        }
    }
    // Return null if no one has won
    return null
}
