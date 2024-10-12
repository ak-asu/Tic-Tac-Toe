package com.akheparasu.tic_tac_toe.algorithms

import android.util.Log
import com.akheparasu.tic_tac_toe.utils.Difficulty
import kotlin.random.Random

fun runAITurn(grid: Array<Array<String>>,diff: Difficulty): Array<Array<String>> {
    var randEasy = 0                    // var and val used
    val randMed = Random.nextBoolean()  // the 50 / 50 for easy or hard per turn
    val gridSize = grid.size
    var gridCheckI =  mutableListOf<Int>()
    var gridCheckJ =  mutableListOf<Int>()
    var gridEmpty = 0
    var gridCount = 0

    for(i in 0 until gridSize){     // find spots that are filled or empty
        for(j in 0 until gridSize){
            if(grid[i][j] == ""){
                gridCheckI.add(i)
                gridCheckJ.add(j)
            }
            else{
                gridEmpty = gridEmpty + 1
            }
            gridCount = gridCount + 1
        }
    }

    if(diff == Difficulty.Easy){
        if(gridEmpty == (gridSize*gridSize)){
            return grid     // test for full board
        }
        randEasy = Random.nextInt((gridSize*gridSize) - gridEmpty)
        val outputI = gridCheckI[randEasy]  // takes the random value and uses it -
        val outputJ = gridCheckJ[randEasy]  // to find the given I and J found above
        grid[outputI][outputJ] = "O"        // set move and return
        //Log.i("randEasy:", randEasy.toString())           // testing only
        return grid
    }
    if(diff == Difficulty.Medium){
        if(!randMed){   // runs easy code from above
            if(gridEmpty == (gridSize*gridSize)){
                return grid     // test for full board
            }
            randEasy = Random.nextInt((gridSize*gridSize) - gridEmpty)
            val outputI = gridCheckI[randEasy]
            val outputJ = gridCheckJ[randEasy]
            grid[outputI][outputJ] = "O"
            Log.i("medRoll", "rolled Easy mode")    // testing only
            return grid
        }
        else{   // else runs hard code
            Log.i("medRoll", "rolled Hard mode")    // testing only

        }
    }
    if(diff == Difficulty.Hard || randMed){
        var bestScore = Int.MIN_VALUE   // maximize this value, start with lowest
        var outputI = 0
        var outputJ = 0
        for (i in 0 until gridSize) {   // loop through every move and find best outcomes
            for (j in 0 until gridSize) {
                if (grid[i][j] == "") {
                    grid[i][j] = "O" // make the test move
                    val score = miniMax(grid, 1, 0, Int.MIN_VALUE, Int.MAX_VALUE) // call miniMax for player
                    grid[i][j] = "" // undo the test move

                    if (score > bestScore) {    // find new best score and move
                        bestScore = score
                        outputI = i
                        outputJ = j
                    }
                }
            }
        }

        grid[outputI][outputJ] = "O"    // set move and return
        Log.i("hardDiff", "is Hard mode") // testing only

    }
    return grid
}

fun miniMax(grid: Array<Array<String>>, player: Int, depth: Int, alpha: Int, beta: Int): Int{
    val winnerCheck = findWinnners(grid)    // winner = "X", "O", ""
    var gridFull = 0
    var bestScore: Int
    if(winnerCheck == "X"){     // depth is used to find earliest win
        return -10 + depth
    }
    if(winnerCheck == "O"){
        return 10 - depth
    }
    for(i in 0 until grid.size){    // finds if grid is full and if so dont run
        for(j in 0 until grid.size){
            if(grid[i][j] != ""){
                gridFull = gridFull + 1
            }
        }
    }
    if(gridFull == (grid.size * grid.size)){
        return 0
    }

    if(player == 0){        // maximize cpu player, minimax algorithm runs recursively
        bestScore = Int.MIN_VALUE
        var newAlpha = alpha
        for(i in 0 until grid.size){
            for(j in 0 until grid.size){
                if(grid[i][j] == "") {
                    grid[i][j] = "O"
                    val score = miniMax(grid, 1, depth + 1, newAlpha, beta)
                    grid[i][j] = ""
                    bestScore = maxOf(score, bestScore)     // find max of old and new score
                    newAlpha = maxOf(newAlpha, bestScore)   // update alpha to trim tree
                    if (beta <= newAlpha) {
                        break
                    }
                }
            }
        }
    }
    else{               // minimize player similar to above
        bestScore = Int.MAX_VALUE
        var newBeta = beta
        for(i in 0 until grid.size){
            for(j in 0 until grid.size){
                if(grid[i][j] == "") {
                    grid[i][j] = "X"
                    val score = miniMax(grid, 0, depth + 1, alpha, newBeta)
                    grid[i][j] = ""
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

fun findWinnners(grid: Array<Array<String>>): String{
    val gridSize = grid.size
    // this works for 3x3 grid but might need to be updated, just checks all outcomes for winnin
    for(i in 0 until gridSize){
            if(grid[i][0] == "X" && grid[i][1] == "X" && grid[i][2] == "X"){
                return "X"
            }
            if(grid[0][i] == "X" && grid[1][i] == "X" && grid[2][i] == "X"){
                return "X"
            }
            if(grid[i][0] == "O" && grid[i][1] == "O" && grid[i][2] == "O"){
                return "O"
            }
            if(grid[0][i] == "O" && grid[1][i] == "O" && grid[2][i] == "O") {
                return "O"
            }
    }
    if(grid[0][0] == "X" && grid[1][1] == "X" && grid[2][2] == "X"){
        return "X"
    }
    if(grid[0][0] == "O" && grid[1][1] == "O" && grid[2][2] == "O"){
        return "O"
    }
    if(grid[0][2] == "X" && grid[1][1] == "X" && grid[2][0] == "X"){
        return "X"
    }
    if(grid[0][2] == "O" && grid[1][1] == "O" && grid[2][0] == "O"){
        return "O"
    }
    return ""
}