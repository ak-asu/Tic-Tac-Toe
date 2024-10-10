package com.akheparasu.tic_tac_toe.multiplayer

data class DataModel(
    val gameState: GameState,
    val metadata: Metadata
)

data class GameState(
    val board: List<List<String>>,
    val turn: String,
    val winner: String?,
    val draw: Boolean,
    val connectionEstablished: Boolean,
    val reset: Boolean
)

data class Metadata(
    val choices: List<PlayerChoice>,
    val miniGame: MiniGame
)

data class PlayerChoice(val id: String, val name: String)

data class MiniGame(
    val player1Choice: String,
    val player2Choice: String
)
