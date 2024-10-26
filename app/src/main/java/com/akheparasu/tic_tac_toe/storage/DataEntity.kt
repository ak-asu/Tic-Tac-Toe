package com.akheparasu.tic_tac_toe.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.Winner
import java.util.Date

@Entity(tableName = "career_table")
data class DataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "winner") val winner: Winner,
    @ColumnInfo(name = "difficulty") val difficulty: Difficulty?,
    @ColumnInfo(name = "gameMode") val gameMode: GameMode,
) {
    init {
        require(id >= 0) { "Id must be greater than or equal to 0" }
    }
}
