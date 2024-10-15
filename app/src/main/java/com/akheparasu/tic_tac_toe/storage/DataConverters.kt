package com.akheparasu.tic_tac_toe.storage

import androidx.room.TypeConverter
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.Player
import java.util.Date

class DateConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

class DifficultyConverters {

    @TypeConverter
    fun fromDifficulty(level: Difficulty?): String? {
        return level?.name
    }

    @TypeConverter
    fun toDifficulty(level: String?): Difficulty? {
        return level?.let { runCatching { Difficulty.valueOf(it) }.getOrNull() }
    }
}

class PlayerConverters {

    @TypeConverter
    fun fromPlayer(player: Player): String {
        return player.name
    }

    @TypeConverter
    fun toPlayer(player: String): Player {
        return Player.valueOf(player)
    }
}

class GameModeConverters {

    @TypeConverter
    fun fromGameMode(gameMode: GameMode?): String? {
        return gameMode?.name
    }

    @TypeConverter
    fun toGameMode(gameMode: String?): GameMode? {
        return gameMode?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
    }
}
