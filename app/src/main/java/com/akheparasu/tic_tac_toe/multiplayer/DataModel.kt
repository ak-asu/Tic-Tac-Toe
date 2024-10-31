package com.akheparasu.tic_tac_toe.multiplayer

import com.akheparasu.tic_tac_toe.utils.PLAYER_1
import com.akheparasu.tic_tac_toe.utils.PLAYER_2


data class DataModel(
    val gameState: GameState = GameState(),
    val metaData: MetaData = MetaData()
)

data class GameState(
    val board: List<List<String>> = List(3) { List(3) { " " } },
    val turn: String = "0",
    val winner: String = "",
    val draw: Boolean = false,
    val connectionEstablished: Boolean = true,
    val reset: Boolean = false
)

data class MetaData(
    val choices: List<PlayerChoice> = listOf(
        PlayerChoice(PLAYER_1, ""),
        PlayerChoice(PLAYER_2, "")
    ),
    val miniGame: MiniGame = MiniGame()
)

data class PlayerChoice(val id: String, val name: String)

data class MiniGame(
    val player1Choice: String = "",
    val player2Choice: String = ""
)
