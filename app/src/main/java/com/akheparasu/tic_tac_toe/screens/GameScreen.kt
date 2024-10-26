package com.akheparasu.tic_tac_toe.screens

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akheparasu.tic_tac_toe.algorithms.runAITurn
import com.akheparasu.tic_tac_toe.multiplayer.GameState
import com.akheparasu.tic_tac_toe.storage.DataEntity
import com.akheparasu.tic_tac_toe.storage.StorageDB
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.GridEntry
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.Preference
import com.akheparasu.tic_tac_toe.utils.Winner
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun GameScreen(
    gameMode: GameMode,
    preference: Preference,
    originalConnectedDevice: BluetoothDevice?,
    addScoreViewModel: AddScoreViewModel
) {
    val playerMarker = if (preference == Preference.First) {
        GridEntry.X
    } else {
        GridEntry.O
    }
    val opponentMarker = if (playerMarker == GridEntry.X) {
        GridEntry.O
    } else {
        GridEntry.X
    }
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
    val connectionService = LocalConnectionService.current
    val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
    val connectedDevice = if (gameMode == GameMode.TwoDevices) {
        connectionService.connectedDevice.collectAsState(initial = null)
    } else {
        null
    }
    val onGoToScoreScreen: (GameResult) -> Unit = {
        addScoreViewModel.insertScore(
            if (it == GameResult.Win) {
                Winner.Human
            } else if (it == GameResult.Draw) {
                Winner.Draw
            } else {
                if (gameMode == GameMode.Computer) {
                    Winner.Computer
                } else {
                    Winner.Empty
                }
            }, gameMode, difficultyFlow.value
        )
        if (gameMode == GameMode.TwoDevices && connectedDevice?.value?.address == originalConnectedDevice!!.address) {
            val currentDeviceAddress =
                if (originalConnectedDevice.address == connectionService.receivedDataModel.metaData.choices.first().name) {
                    connectionService.receivedDataModel.metaData.choices.last().name
                } else {
                    connectionService.receivedDataModel.metaData.choices.first().name
                }
            connectionService.sendData(
                connectionService.receivedDataModel.copy(
                    gameState = GameState(
                        winner = when (it) {
                            GameResult.Win -> currentDeviceAddress
                            GameResult.Fail -> originalConnectedDevice.address
                            else -> " "
                        },
                        draw = it == GameResult.Draw
                    )
                )
            )
        }
        navController?.navigate("score/${gameMode.name}/${preference.name}/${originalConnectedDevice?.address}/${difficultyFlow.value}/${it.name}") {
            popUpTo("home") { inclusive = false }
        }
    }
    connectionService.setOnDataReceived { dataModel ->
        connectionService.receivedDataModel = dataModel
        grid = dataModel.gameState.board.map { r -> r.map { GridEntry.valueOf(it) }.toTypedArray() }
            .toTypedArray()
        if (!(dataModel.gameState.winner != "" || dataModel.gameState.draw)) {
            when (checkWinner(grid)) {
                GridEntry.X -> onGoToScoreScreen(GameResult.Win)
                GridEntry.O -> onGoToScoreScreen(GameResult.Fail)
                GridEntry.E -> onGoToScoreScreen(GameResult.Draw)
                null -> {}
            }
            playerTurn = if (preference == Preference.First) {
                dataModel.gameState.turn.toInt() % 2 == 0
            } else {
                dataModel.gameState.turn.toInt() % 2 == 1
            }
        }
    }
    var gamePaused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playerTurn) {
        if (!playerTurn && gameMode != GameMode.OneDevice) {
            if (gameMode == GameMode.Computer) {
                grid = async {
                    delay(1000)
                    runAITurn(grid, difficultyFlow.value)
                }.await()
                playerTurn = true
            } else if (gameMode == GameMode.TwoDevices) {
                connectionService.sendData(
                    connectionService.receivedDataModel.copy(
                        gameState = GameState(
                            board = grid.map { it.map { e -> e.name } },
                            turn = "${connectionService.receivedDataModel.gameState.turn.toInt() + 1}",
                            connectionEstablished = true
                        )
                    )
                )
            }
        }
        when (checkWinner(grid)) {
            GridEntry.X -> onGoToScoreScreen(GameResult.Win)
            GridEntry.O -> onGoToScoreScreen(GameResult.Fail)
            GridEntry.E -> onGoToScoreScreen(GameResult.Draw)
            null -> {}
        }
    }
    val btEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (connectionService.isBtEnabled()) {
            connectionService.connectDevice(originalConnectedDevice!!)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it } && gameMode == GameMode.TwoDevices) {
            btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
    if (gameMode == GameMode.TwoDevices) {
        LaunchedEffect(connectedDevice) {
            if (connectedDevice?.value?.address != originalConnectedDevice!!.address) {
                gamePaused = true
            }
        }
        LaunchedEffect(onlineSetupStage) {
            if (onlineSetupStage.value == OnlineSetupStage.Idle) {
                navController?.popBackStack()
            }
        }
    }
    DisposableEffect(Unit) {
        audioController.onGameStart()
        onDispose {
            connectionService.setOnDataReceived()
            connectionService.disconnectDevice()
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val maxCellSize = 0.9f * minOf(screenWidth.value, screenHeight.value) / 3

    if (gamePaused) {
        Text("Game paused")
        if (gameMode == GameMode.TwoDevices) {
            Button(onClick = {
                permissionLauncher.launch(connectionService.getMissingPermissions().first)
            }) { Text("Reconnect") }
        }
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
                    text = "Player: ${playerMarker.name}", fontWeight = if (playerTurn) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Thin
                    }
                )
                Text(text = " | ")
                Text(
                    text = "Opponent: ${opponentMarker.name}", fontWeight = if (playerTurn) {
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
                            maxCellSize = maxCellSize
                        ) {
                            if (grid[i][j] == GridEntry.E) {
                                if (playerTurn) {
                                    grid[i][j] = playerMarker
                                    playerTurn = false
                                    if (checkWinner(grid)?.equals(playerMarker) == true) {
                                        onGoToScoreScreen(GameResult.Win)
                                    } else {
                                        audioController.onPlayerTap()
                                    }
                                } else {
                                    if (gameMode == GameMode.OneDevice) {
                                        grid[i][j] = opponentMarker
                                        playerTurn = true
                                        audioController.onOpponentTap()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (false) {
                MatchLineOverlay(
                    startCell = 0,
                    endCell = 0,
                    cellSize = min(100.dp, maxCellSize.dp),
                    onGoToScoreScreen = {}
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            RoundedRectButton(onClick = {
                grid = Array(3) { Array(3) { GridEntry.E } }
                playerTurn = preference == Preference.First
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

@Composable
fun MatchLineOverlay(
    startCell: Int,
    endCell: Int,
    cellSize: Dp,
    onGoToScoreScreen: () -> Unit
) {
    val delayMS = 1000
    val calculateCellOffset: @Composable (Int) -> Offset = {
        with(LocalDensity.current) {
            val row = it / 3
            val column = it % 3
            val x = column * cellSize.toPx() + cellSize.toPx() / 2
            val y = row * cellSize.toPx() + cellSize.toPx() / 2
            Offset(x, y)
        }
    }
    val startOffset = calculateCellOffset(startCell)
    val endOffset = calculateCellOffset(endCell)
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = delayMS), label = ""
    )

    LaunchedEffect(Unit) {
        delay(delayMS.toLong())
        onGoToScoreScreen()
    }

    Canvas(modifier = Modifier.requiredSizeIn(cellSize * 3)) {
        val animatedEndX = startOffset.x + (endOffset.x - startOffset.x) * animationProgress
        val animatedEndY = startOffset.y + (endOffset.y - startOffset.y) * animationProgress
        drawLine(
            color = Color.Red,
            start = startOffset,
            end = Offset(animatedEndX, animatedEndY),
            strokeWidth = 8f
        )
    }
}

class AddScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = StorageDB.getDatabase(application).dataDao()
    fun insertScore(winner: Winner, gameMode: GameMode, difficulty: Difficulty) {
        val dataEntity = DataEntity(
            winner = winner,
            gameMode = gameMode,
            difficulty = if (gameMode == GameMode.Computer) {
                difficulty
            } else {
                null
            },
            date = Date()
        )
        viewModelScope.launch {
            dataDao.insertRow(dataEntity)
        }
    }
}

class AddScoreViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddScoreViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddScoreViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
