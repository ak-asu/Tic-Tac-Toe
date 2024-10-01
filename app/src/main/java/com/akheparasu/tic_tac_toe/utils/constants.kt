package com.akheparasu.tic_tac_toe.utils

enum class Difficulty(private val level: Int) {
    Easy(0),
    Medium(1),
    Hard(2);

    fun getDifficultyLevel(): Int {
        return level
    }

    companion object {
        fun fromLevel(level: Int): Difficulty {
            return when (level) {
                0 -> Easy
                1 -> Medium
                2 -> Hard
                else -> Easy
            }
        }
    }
}

enum class Player() {
    Human,
    Computer,
    Challenger;
}
