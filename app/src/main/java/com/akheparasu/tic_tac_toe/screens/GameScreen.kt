package com.akheparasu.tic_tac_toe.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun GameScreen(navController: NavHostController) {
    var grid by remember { mutableStateOf(Array(3) { Array(3) { "" } }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Player: X | Opponent: O")

        Spacer(modifier = Modifier.height(16.dp))

        // Render the grid
        for (i in 0 until 3) {
            Row {
                for (j in 0 until 3) {
                    GridCell(value = grid[i][j], onTap = {
                        if (grid[i][j] == "") {
                            grid[i][j] = "X"
                        }
                    })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* Reset Game Logic */ }) {
            Text(text = "Reset Game")
        }
        Button(onClick = { navController.navigate("home") }) {
            Text(text = "Back to Home")
        }
    }
}

@Composable
fun GridCell(value: String, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.LightGray)
        }
        BasicTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.clickable(onClick = onTap)
        )
    }
}
