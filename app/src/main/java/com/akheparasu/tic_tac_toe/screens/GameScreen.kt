package com.akheparasu.tic_tac_toe.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.DEFAULT_GRID_SIZE
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

@Composable
fun GameScreen(gameMode: Boolean) {
    val context = LocalContext.current

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

    /* Define your gridSaver as before
//    val gridSaver = Saver<List<List<String>>, ArrayList<ArrayList<String>>>(
//        save = { grid ->
//            ArrayList(grid.map { ArrayList(it) })
//        },
//        restore = { saved: ArrayList<ArrayList<String>> ->
//            saved.map { it.toList() }
//        }
//    )
    */
    var grid by rememberSaveable { mutableStateOf(Array(gridSize) { Array(gridSize) { "" } }) }
    var playerTurn by rememberSaveable { mutableStateOf(true) }
    val player2 = rememberSaveable { mutableStateOf(gameMode) }
    val isGameComplete: (Array<Array<String>>) -> Boolean = { !it.any { c -> c.any { v -> v.isEmpty() } } }

    //THIS IS FOR DEVELOPMENT (CAN REMOVE AFTER)
    var count by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(playerTurn) {
        if (!playerTurn && !player2.value) {
            grid = async { runOpponentTurn(grid, count)}.await()

            count += 1

            //check if game complete

            playerTurn = true
        }
    }
    LaunchedEffect (gridSize) {
        if (gridSize!=grid.size) {
            grid = Array(gridSize) { Array(gridSize) { "" } }
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
                        if(grid[i][j].isNotEmpty()){
                            Toast.makeText(context, "That spot has already been selected!", Toast.LENGTH_SHORT).show()
                        }
                        else{
                            if (playerTurn) {
                                grid[i][j] = "X"
                                //Check for the win condition
                                playerTurn = false
                            }
                            else {
                                //Player 2 Game Mode
                                if(player2.value){
                                    grid[i][j] = "O"
                                    //Check for the win condition
                                    playerTurn = true
                                }
                                //Computer Game Mode
                                else {
                                    Log.i("someTag", "We will have computer take their turn")
                                    //Check for the win condition
                                }
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

suspend fun runOpponentTurn(grid: Array<Array<String>>, count:Int): Array<Array<String>> {
    //This is for testing if computer turn works
    if(count == 0){
        grid[0][0] = "O"
    }
    else if(count == 1){
        grid[0][1] = "O"
    }
    else{
        grid[0][2] = "O"
    }

    delay(1000)
    return grid
}
