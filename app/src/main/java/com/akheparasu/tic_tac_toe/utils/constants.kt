package com.akheparasu.tic_tac_toe.utils

import androidx.compose.ui.graphics.Color

const val DEFAULT_VOLUME = 1.0f
const val SPACER_HEIGHT = 16


enum class Difficulty(private val level: Int) {
    Easy(0),
    Medium(1),
    Hard(2);

    fun getDifficultyLevel(): Int {
        return level
    }

    fun getColor(): Color {
        return when (this) {
            Easy -> Color.Green
            Medium -> Color.Yellow
            Hard -> Color.Red
        }
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

enum class Player {
    Human,
    Computer,
    Challenger;
}

enum class GameMode {
    Computer,
    Human,
    Online;
}

enum class Preference(private val id: Int) {
    First(0),
    Second(1),
    NoPreference(2),
    AskEveryTime(3);

    fun getPreferenceId(): Int {
        return id
    }

    companion object {
        fun fromId(id: Int): Preference {
            return when (id) {
                0 -> First
                1 -> Second
                2 -> NoPreference
                3 -> AskEveryTime
                else -> AskEveryTime
            }
        }
    }
}

enum class GridEntry {
    X,
    O,
    E;

    fun getDisplayText(): String {
        return when (this) {
            X -> "X"
            O -> "O"
            E -> ""
        }
    }
}