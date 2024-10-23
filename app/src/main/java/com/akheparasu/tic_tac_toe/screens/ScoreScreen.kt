package com.akheparasu.tic_tac_toe.screens

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
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GameResult
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalNavController

@Composable
fun ScoreScreen(
    gameMode: GameMode,
    difficulty: Difficulty?,
    gameResult: GameResult,
    onReplay: () -> Unit
) {
    val audioController = LocalAudioPlayer.current
    val navController = LocalNavController.current

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
            navController?.popBackStack()
            onReplay()
        }) { Text(text = "Replay") }
    }
}
