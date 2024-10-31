package com.akheparasu.tic_tac_toe.algorithms

import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GridEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

suspend fun runAITurn(
    grid: Array<Array<GridEntry>>,
    diff: Difficulty,
    playerMarker: GridEntry,
    opponentMarker: GridEntry
): Array<Array<GridEntry>> {
    return withContext(Dispatchers.IO) {
        var randEasy = 0                    // var and val used
        val randMed = Random.nextBoolean()  // the 50 / 50 for easy or hard per turn
        val gridSize = grid.size
        val gridCheckI = mutableListOf<Int>()
        val gridCheckJ = mutableListOf<Int>()
        var gridEmpty = 0
        var gridCount = 0

        for (i in 0 until gridSize) {     // find spots that are filled or empty
            for (j in 0 until gridSize) {
                if (grid[i][j] == GridEntry.E) {
                    gridCheckI.add(i)
                    gridCheckJ.add(j)
                } else {
                    gridEmpty += 1
                }
                gridCount += 1
            }
        }

        if (diff == Difficulty.Easy) {
            if (gridEmpty == (gridSize * gridSize)) {
                return@withContext grid     // test for full board
            }
            randEasy = Random.nextInt((gridSize * gridSize) - gridEmpty)
            val outputI = gridCheckI[randEasy]  // takes the random value and uses it -
            val outputJ = gridCheckJ[randEasy]  // to find the given I and J found above
            grid[outputI][outputJ] = opponentMarker        // set move and return
            return@withContext grid
        }
        if (diff == Difficulty.Medium) {
            if (!randMed) {   // runs easy code from above
                if (gridEmpty == (gridSize * gridSize)) {
                    return@withContext grid     // test for full board
                }
                randEasy = Random.nextInt((gridSize * gridSize) - gridEmpty)
                val outputI = gridCheckI[randEasy]
                val outputJ = gridCheckJ[randEasy]
                grid[outputI][outputJ] = opponentMarker
                return@withContext grid
            } else {   // else runs hard code
            }
        }
        if (diff == Difficulty.Hard || randMed) {
            var bestScore = Int.MIN_VALUE   // maximize this value, start with lowest
            var outputI = 0
            var outputJ = 0
            for (i in 0 until gridSize) {   // loop through every move and find best outcomes
                for (j in 0 until gridSize) {
                    if (grid[i][j] == GridEntry.E) {
                        grid[i][j] = opponentMarker // make the test move
                        val score =
                            miniMax(
                                grid,
                                1,
                                0,
                                Int.MIN_VALUE,
                                Int.MAX_VALUE,
                                opponentMarker,
                                playerMarker
                            ) // call miniMax for player
                        grid[i][j] = GridEntry.E // undo the test move
                        if (score > bestScore) {    // find new best score and move
                            bestScore = score
                            outputI = i
                            outputJ = j
                        }
                    }
                }
            }
            grid[outputI][outputJ] = opponentMarker    // set move and return
        }
        return@withContext grid
    }
}

fun miniMax(
    grid: Array<Array<GridEntry>>,
    player: Int,
    depth: Int,
    alpha: Int,
    beta: Int,
    oppMark: GridEntry,
    playerMark: GridEntry
): Int {
    val winnerCheck = findWinners(grid)    // winner = GridEntry.X, GridEntry.O, GridEntry.E
    var gridFull = 0
    var bestScore: Int
    if (winnerCheck == playerMark) {     // depth is used to find earliest win
        return -10 + depth
    }
    if (winnerCheck == oppMark) {
        return 10 - depth
    }
    for (element in grid) {    // finds if grid is full and if so dont run
        for (j in grid.indices) {
            if (element[j] != GridEntry.E) {
                gridFull += 1
            }
        }
    }
    if (gridFull == (grid.size * grid.size)) {
        return 0
    }

    if (player == 0) {        // maximize cpu player, minimax algorithm runs recursively
        bestScore = Int.MIN_VALUE
        var newAlpha = alpha
        for (i in grid.indices) {
            for (j in grid.indices) {
                if (grid[i][j] == GridEntry.E) {
                    grid[i][j] = oppMark
                    val score = miniMax(grid, 1, depth + 1, newAlpha, beta, oppMark, playerMark)
                    grid[i][j] = GridEntry.E
                    bestScore = maxOf(score, bestScore)     // find max of old and new score
                    newAlpha = maxOf(newAlpha, bestScore)   // update alpha to trim tree
                    if (beta <= newAlpha) {
                        break
                    }
                }
            }
        }
    } else {               // minimize player similar to above
        bestScore = Int.MAX_VALUE
        var newBeta = beta
        for (i in grid.indices) {
            for (j in grid.indices) {
                if (grid[i][j] == GridEntry.E) {
                    grid[i][j] = playerMark
                    val score = miniMax(grid, 0, depth + 1, alpha, newBeta, oppMark, playerMark)
                    grid[i][j] = GridEntry.E
                    bestScore = minOf(score, bestScore)     // same as above but minimize score
                    newBeta = minOf(newBeta, bestScore)     // trim using new beta
                    if (newBeta <= alpha) {
                        break
                    }
                }
            }
        }
    }
    return bestScore
}

fun findWinners(grid: Array<Array<GridEntry>>): GridEntry {
    val gridSize = grid.size
    // this works for 3x3 grid but might need to be updated, just checks all outcomes for winnin
    for (i in 0 until gridSize) {
        if (grid[i][0] == GridEntry.X && grid[i][1] == GridEntry.X && grid[i][2] == GridEntry.X) {
            return GridEntry.X
        }
        if (grid[0][i] == GridEntry.X && grid[1][i] == GridEntry.X && grid[2][i] == GridEntry.X) {
            return GridEntry.X
        }
        if (grid[i][0] == GridEntry.O && grid[i][1] == GridEntry.O && grid[i][2] == GridEntry.O) {
            return GridEntry.O
        }
        if (grid[0][i] == GridEntry.O && grid[1][i] == GridEntry.O && grid[2][i] == GridEntry.O) {
            return GridEntry.O
        }
    }
    if (grid[0][0] == GridEntry.X && grid[1][1] == GridEntry.X && grid[2][2] == GridEntry.X) {
        return GridEntry.X
    }
    if (grid[0][0] == GridEntry.O && grid[1][1] == GridEntry.O && grid[2][2] == GridEntry.O) {
        return GridEntry.O
    }
    if (grid[0][2] == GridEntry.X && grid[1][1] == GridEntry.X && grid[2][0] == GridEntry.X) {
        return GridEntry.X
    }
    if (grid[0][2] == GridEntry.O && grid[1][1] == GridEntry.O && grid[2][0] == GridEntry.O) {
        return GridEntry.O
    }
    return GridEntry.E
}