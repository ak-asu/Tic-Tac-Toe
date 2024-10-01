package com.akheparasu.tic_tac_toe.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DataDao {
    @Insert
    suspend fun insertRow(dataEntity: DataEntity)

    @Query("SELECT * FROM career_table ORDER BY date DESC")
    fun getAllRows(): Flow<List<DataEntity>>
}
