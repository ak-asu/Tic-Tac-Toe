package com.akheparasu.tic_tac_toe.screens

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.DEFAULT_GRID_SIZE
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun GameScreen() {
    val settings = LocalSettings.current
    val gridSizeFlow = settings.gridSizeFlow.collectAsState(initial = null)
    val difficultyFlow = settings.difficultyFlow.collectAsState(initial = null)
    if (gridSizeFlow.value==null || difficultyFlow.value==null) {
        return Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    val gridSize by rememberSaveable { mutableIntStateOf(gridSizeFlow.value!!) }
    // Define your gridSaver as before
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
    val isGameComplete: (Array<Array<String>>) -> Boolean = { !it.any { c -> c.any { v -> v.isEmpty() } } }

    LaunchedEffect(playerTurn) {
        if (!playerTurn) {
            grid = async { runOpponentTurn(grid) }.await()
            if (isGameComplete(grid)) {
                // finishGame()
            } else {
                playerTurn = true
            }
        }
    }
    LaunchedEffect (gridSize) {
        if (gridSize!=grid.size) {
            grid = Array(gridSize) { Array(gridSize) { "" } }
            playerTurn = true
        }
    }

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
                        if (playerTurn && grid[i][j].isEmpty()) {
//                            grid = grid.toMutableList().apply {
//                                this[i] = this[i].toMutableList().apply {
//                                    this[j] = 'X'
//                                }
//                            }
                            if (isGameComplete(grid)) {
                                // finishGame()
                            } else {
                                playerTurn = true
                            }
                        }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* Reset Game Logic */ }) {
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

suspend fun runOpponentTurn(grid: Array<Array<String>>): Array<Array<String>> {
    grid[0][0] = "O"
    delay(1000)
    return grid
}
