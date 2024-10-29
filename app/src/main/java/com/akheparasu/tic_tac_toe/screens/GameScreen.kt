package com.akheparasu.tic_tac_toe.screens

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.akheparasu.tic_tac_toe.utils.GameResultData
import com.akheparasu.tic_tac_toe.utils.GridEntry
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.PADDING_HEIGHT
import com.akheparasu.tic_tac_toe.utils.Preference
import com.akheparasu.tic_tac_toe.utils.SPACER_HEIGHT
import com.akheparasu.tic_tac_toe.utils.Winner
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun GameScreen(
    gameMode: GameMode,
    preference: Preference,
    originalConnectedDevice: BluetoothDevice?,
    addScoreViewModel: AddScoreViewModel
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val maxCellSize = 0.9f * minOf(screenWidth.value, screenHeight.value) / 3
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
    val context = LocalContext.current
    val settings = LocalSettings.current
    val navController = LocalNavController.current
    val audioController = LocalAudioPlayer.current
    val connectionService = LocalConnectionService.current
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
    var resetGame by rememberSaveable { mutableStateOf(false) }
    val gameResultDataSaver = Saver(
        save = { gameResultData: MutableState<GameResultData?> ->
            gameResultData.value?.toJson()
        },
        restore = { saved: String ->
            mutableStateOf(GameResultData.fromJson(saved))
        }
    )
    var gameResultData by rememberSaveable(saver = gameResultDataSaver) {
        mutableStateOf(null)
    }
    val onlineSetupStage = if (gameMode == GameMode.TwoDevices) {
        connectionService.onlineSetupStage.collectAsState()
    } else {
        null
    }
    val connectedDevice = if (gameMode == GameMode.TwoDevices) {
        connectionService.connectedDevice.collectAsState(initial = null)
    } else {
        null
    }
    val checkWinner: () -> GameResultData? = {
        if (gameResultData == null) {
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
            var result: GameResultData? = null
            var drawFlag = true
            for (combination in winningCombinations) {
                val (first, second, third) = combination
                val (i1, j1) = first
                val (i2, j2) = second
                val (i3, j3) = third
                if (grid[i1][j1] == grid[i2][j2] && grid[i2][j2] == grid[i3][j3] && grid[i1][j1] != GridEntry.E) {
                    drawFlag = false
                    result = GameResultData(
                        startCell = i1 * 3 + j1,
                        endCell = i3 * 3 + j3,
                        gameResult = if (playerMarker == grid[i1][j1]) {
                            GameResult.Win
                        } else {
                            GameResult.Fail
                        }
                    )
                    break
                }
                val values = combination.map { (x, y) -> grid[x][y] }
                if (((values.count { it == GridEntry.X } == 2 ||
                            values.count { it == GridEntry.O } == 2) &&
                            values.count { it == GridEntry.E } == 1) ||
                    values.count { it == GridEntry.E } > 1) {
                    drawFlag = false
                }
            }
            if (drawFlag) {
                result = GameResultData(gameResult = GameResult.Draw)
            }
            if (result != null) {
                addScoreViewModel.insertScore(
                    if (result.gameResult == GameResult.Win) {
                        Winner.Human
                    } else if (result.gameResult == GameResult.Draw) {
                        Winner.Draw
                    } else {
                        if (gameMode == GameMode.Computer) {
                            Winner.Computer
                        } else {
                            Winner.Empty
                        }
                    }, gameMode, difficultyFlow.value
                )
            }
            result
        } else {
            gameResultData
        }
    }
    val onGoToScoreScreen: (GameResultData) -> Unit = {
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
                        board = grid.map { f -> f.map { e -> e.name } },
                        turn = "${connectionService.receivedDataModel.gameState.turn.toInt() + 1}",
                        winner = when (it.gameResult) {
                            GameResult.Win -> currentDeviceAddress
                            GameResult.Fail -> originalConnectedDevice.address
                            else -> " "
                        },
                        draw = it.gameResult == GameResult.Draw
                    )
                )
            )
        }
        navController?.navigate("score/${gameMode.name}/${preference.name}/${originalConnectedDevice?.address}/${difficultyFlow.value}/${it.gameResult.name}") {
            popUpTo("home") { inclusive = false }
        }
    }

    LaunchedEffect(playerTurn) {
        gameResultData = checkWinner()
        if (!playerTurn && gameMode != GameMode.OneDevice) {
            if (gameMode == GameMode.Computer && gameResultData == null) {
                grid = async {
                    delay(1000)
                    runAITurn(grid, difficultyFlow.value, playerMarker, opponentMarker)
                }.await()
                playerTurn = true
            } else if (gameMode == GameMode.TwoDevices) {
                connectionService.sendData(
                    connectionService.receivedDataModel.copy(
                        gameState = GameState(
                            board = grid.map { it.map { e -> e.name } },
                            turn = "${connectionService.receivedDataModel.gameState.turn.toInt() + 1}"
                        )
                    )
                )
            }
        }
        gameResultData = checkWinner()
    }
    if (gameMode == GameMode.TwoDevices) {
        LaunchedEffect(connectedDevice?.value) {
            if (connectedDevice?.value?.address != originalConnectedDevice!!.address) {
                connectionService.setOnlineSetupStage(OnlineSetupStage.Idle)
            }
        }
        LaunchedEffect(onlineSetupStage?.value) {
            if (onlineSetupStage?.value != OnlineSetupStage.GameStart) {
                navController?.popBackStack()
            }
        }
        LaunchedEffect(resetGame) {
            if (resetGame) {
                if (!connectionService.receivedDataModel.gameState.reset) {
                    connectionService.sendData(
                        connectionService.receivedDataModel.copy(gameState = GameState(reset = true))
                    )
                }
                connectionService.receivedDataModel = connectionService.receivedDataModel.copy(gameState = GameState())
                grid = Array(3) { Array(3) { GridEntry.E } }
                playerTurn = preference == Preference.First
                resetGame = false
            }
        }
    }
    DisposableEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        audioController.onGameStart()
        if (gameMode == GameMode.TwoDevices) {
            connectionService.setOnDataReceived { dataModel ->
                if (!dataModel.gameState.reset || dataModel.gameState.turn.toInt() < connectionService.receivedDataModel.gameState.turn.toInt()) {
                    connectionService.sendData(connectionService.receivedDataModel)
                } else {
                    connectionService.receivedDataModel = dataModel
                }
                resetGame = connectionService.receivedDataModel.gameState.reset
                grid = connectionService.receivedDataModel.gameState.board.map { r ->
                    r.map { GridEntry.valueOf(it) }.toTypedArray()
                }
                    .toTypedArray()
                gameResultData = checkWinner()
                if (!(connectionService.receivedDataModel.gameState.winner.isNotEmpty() || connectionService.receivedDataModel.gameState.draw)) {
                    playerTurn = if (preference == Preference.First) {
                        connectionService.receivedDataModel.gameState.turn.toInt() % 2 == 0
                    } else {
                        connectionService.receivedDataModel.gameState.turn.toInt() % 2 == 1
                    }
                } else {
                    // Can check winner/draw, but no need as already done before
                }
            }
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            connectionService.setOnDataReceived(null)
            connectionService.disconnectDevice()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PADDING_HEIGHT.dp),
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
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        Box(
            modifier = Modifier.size(maxCellSize.dp * 3),
            contentAlignment = Alignment.Center
        ) {
            Column {
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
                                if (gameResultData == null) {
                                    if (grid[i][j] == GridEntry.E) {
                                        if (playerTurn) {
                                            grid[i][j] = playerMarker
                                            playerTurn = false
                                            audioController.onPlayerTap()
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
                }
            }
            if (gameResultData != null) {
                MatchLineOverlay(
                    startCell = gameResultData!!.startCell ?: 0,
                    endCell = gameResultData!!.endCell ?: 0,
                    cellSize = maxCellSize,
                    onGoToScoreScreen = { onGoToScoreScreen(gameResultData!!) }
                )
            }
        }
        Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
        RoundedRectButton(onClick = {
            if (gameResultData == null) {
                resetGame = true
            }
        }, text = "Reset Game")
    }
}

@Composable
fun GridCell(value: String, maxCellSize: Float, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .requiredSizeIn(
                minWidth = maxCellSize.dp,
                minHeight = maxCellSize.dp,
                maxWidth = maxCellSize.dp,
                maxHeight = maxCellSize.dp
            )
            .padding((PADDING_HEIGHT / 2).dp)
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

@Composable
fun MatchLineOverlay(
    startCell: Int,
    endCell: Int,
    cellSize: Float,
    onGoToScoreScreen: () -> Unit
) {
    val delayMS = 2000
    val targetValue = 1f
    val calculateCellOffset: (Int) -> Pair<Float, Float> = {
        val row = it / 3
        val column = it % 3
        val x = cellSize * column + cellSize / 2
        val y = cellSize * row + cellSize / 2
        Pair(x, y)
    }
    val startPair = calculateCellOffset(startCell)
    val endPair = calculateCellOffset(endCell)
    val animatedOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (startCell != endCell) {
            animatedOffset.animateTo(
                targetValue = targetValue,
                animationSpec = tween(durationMillis = delayMS)
            )
        }
        onGoToScoreScreen()
    }

    Canvas(modifier = Modifier.size(cellSize.dp * 3)) {
        val startOffset = Offset(startPair.first.dp.toPx(), startPair.second.dp.toPx())
        val endOffset = Offset(endPair.first.dp.toPx(), endPair.second.dp.toPx())
        val animatedEndX = startOffset.x + (endOffset.x - startOffset.x) * animatedOffset.value
        val animatedEndY = startOffset.y + (endOffset.y - startOffset.y) * animatedOffset.value
        drawLine(
            color = Color.Red,
            start = startOffset,
            end = Offset(animatedEndX, animatedEndY),
            strokeWidth = 24f
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
