package com.akheparasu.tic_tac_toe.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akheparasu.tic_tac_toe.utils.*
import java.util.Date

@Entity(tableName = "career_table")
data class DataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "winner") val winner: Player,
    @ColumnInfo(name = "difficulty") val difficulty: Difficulty?,
    @ColumnInfo(name = "gridSize") val gridSize: Int,
    @ColumnInfo(name = "score") val score: Float,
) {
    init {
        require(gridSize > 2) { "Grid size must be greater than 2" }
        require(score >= 0) { "Score must be greater than or equal to 0" }
    }
}
