package com.akheparasu.tic_tac_toe.screens

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    gameMode: GameMode,
    gridSize: Int,
    originalConnectedDevice: BluetoothDevice?
) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val difficultyFlow = settings.difficultyFlow.collectAsState(initial = null)
    if (difficultyFlow.value == null) {
        return Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
//    val gridSaver = Saver<List<List<String>>, ArrayList<ArrayList<String>>>(
//        save = { grid ->
//            ArrayList(grid.map { ArrayList(it) })
//        },
//        restore = { saved: ArrayList<ArrayList<String>> ->
//            saved.map { it.toList() }
//        }
//    )
    var grid by rememberSaveable { mutableStateOf(Array(gridSize) { Array(gridSize) { "" } }) }
    var playerTurn by rememberSaveable { mutableStateOf(true) }

    val isGameComplete: (Array<Array<String>>) -> Boolean =
        { !it.any { c -> c.any { v -> v.isEmpty() } } }
    val connectionService = LocalConnectionService.current
    val connectedDevice = if (gameMode == GameMode.Online) {
        connectionService.connectedDevice.collectAsState(initial = null)
    } else {
        null
    }
    val player2 = rememberSaveable { mutableStateOf(gameMode) }
    val isGameComplete: (Array<Array<String>>) -> Boolean = { !it.any { c -> c.any { v -> v.isEmpty() } } }

    //THIS IS FOR DEVELOPMENT (CAN REMOVE AFTER)
    var count by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(playerTurn) {
        if (!playerTurn && !player2.value) {
            grid = async { runOpponentTurn(grid, count)}.await()
            runAITurn(grid, diff)
            count += 1

            //check if game complete

            playerTurn = true
        }
    }
    LaunchedEffect(gridSize) {
        if (gridSize != grid.size) {
            grid = Array(gridSize) { Array(gridSize) { "" } }
        }
    }
    LaunchedEffect(connectedDevice) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Player: X | Opponent: O")

        Spacer(modifier = Modifier.height(16.dp))

        for (i in 0 until gridSize) {
            Row {
                for (j in 0 until gridSize) {
                    GridCell(value = grid[i][j], onTap = {
                        if(grid[i][j].isNotEmpty()){
                            Toast.makeText(context, "That spot has already been selected!", Toast.LENGTH_SHORT).show()
                        }
                        else{
                            if (playerTurn) {
                                grid[i][j] = "X"
                                playerTurn = false
                                //Check for the win condition
                                if(checkWinner(grid).equals("X")) {
                                    Toast.makeText(context, "PLAYER X WINS!!!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else {
                                //Player 2 Game Mode
                                if(player2.value){
                                    grid[i][j] = "O"
                                    playerTurn = true
                                    //Check for the win condition
                                    if(checkWinner(grid).equals("O")) {
                                        Toast.makeText(context, "PLAYER O WINS!!!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                //Computer Game Mode
                                else {
                                    Log.i("someTag", "We will have computer take their turn")
                                    //Check for the win condition

                                    if(checkWinner(grid).equals("O")) {
                                        Toast.makeText(context, "PLAYER O WINS!!!", Toast.LENGTH_SHORT).show()
                                    } else if (checkWinner(grid).equals("X")) {
                                        Toast.makeText(context, "PLAYER X WINS!!!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    grid[i][j] = ""
                }
            }
            playerTurn = true
        }) {
            Text(text = "Reset Game")
        }
    }
}

@Composable
fun GridCell(value: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(8.dp)
            //.indication(interactionSource, LocalIndication.current),
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.LightGray)
        }
        Text(text = value.toString())
    }
}

suspend fun runOpponentTurn(grid: Array<Array<String>>, count:Int): Array<Array<String>> {
    //This is for testing if computer turn works
    if (count == 0) {
        grid[0][0] = "O"
    }
    else if (count == 1) {
        grid[0][1] = "O"
    }
    else {
        grid[0][2] = "O"
    }

    delay(1000)
    return grid
}

// This function checks all of the possibilities of a win and returns "X" or "O" depending on who won
fun checkWinner(grid: Array<Array<String>>): String? {
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
        if (grid[i1][j1] == grid[i2][j2] && grid[i2][j2] == grid[i3][j3] && grid[i1][j1].isNotEmpty()) {
            return grid[i1][j1]
        }
    }
    // Return null if no one has won
    return null
}
