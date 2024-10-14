package com.akheparasu.tic_tac_toe.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DataEntity::class], version = 1, exportSchema = false)
@TypeConverters(
    DateConverters::class,
    DifficultyConverters::class,
    PlayerConverters::class,
    GameModeConverters::class,
)
abstract class StorageDB : RoomDatabase() {

    abstract fun dataDao(): DataDao

    companion object {
        @Volatile
        private var INSTANCE: StorageDB? = null

        fun getDatabase(context: Context): StorageDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StorageDB::class.java,
                    "career_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
