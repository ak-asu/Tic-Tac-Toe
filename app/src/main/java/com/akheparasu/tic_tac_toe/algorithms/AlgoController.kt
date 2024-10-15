package com.akheparasu.tic_tac_toe.algorithms

import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.GridEntry
import kotlinx.coroutines.delay

class AlgoController(private val gameMode: GameMode) {
    suspend fun runOpponentTurn(
        grid: Array<Array<GridEntry>>,
        difficulty: Difficulty,
        func: () -> Unit,
    ): Array<Array<GridEntry>> {
        return when (gameMode) {
            GameMode.Computer -> {
                delay(1000)
                runAITurn(grid, difficulty)
            }
            GameMode.Human -> grid
            GameMode.Online -> {
                func()
                grid
            }
        }
    }
}