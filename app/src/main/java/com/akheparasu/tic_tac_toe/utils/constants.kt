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

enum class Winner {
    Human,
    Computer,
    Empty,
    Draw;

    fun getDisplayText(): String {
        return when (this) {
            Human -> "Human"
            Computer -> "Computer"
            Empty -> "-"
            Draw -> "Draw"
        }
    }
}

enum class GameMode {
    Computer,
    OneDevice,
    TwoDevices;

    fun getDisplayText(): String {
        return when (this) {
            Computer -> "Computer"
            OneDevice -> "1-Device"
            TwoDevices -> "2-Devices"
        }
    }
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

enum class GameResult {
    Win,
    Fail,
    Draw;

    fun getDisplayText(): String {
        return when (this) {
            Win -> "You Won"
            Fail -> "You Lost"
            Draw -> "Game Draw"
        }
    }
}

enum class OnlineSetupStage {
    Preference,
    Initialised,
    GameStart,
    NoService,
    Idle;
}
