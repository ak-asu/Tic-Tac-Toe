package com.akheparasu.tic_tac_toe.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.akheparasu.tic_tac_toe.utils.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val DIFFICULTY = intPreferencesKey("difficulty")
        val THEME = booleanPreferencesKey("theme")
        val VOLUME = floatPreferencesKey("volume")
        val GRID_SIZE = intPreferencesKey("gridSize")
    }

    val difficultyFlow: Flow<Difficulty> = context.dataStore.data
        .map { preferences ->
            val level = preferences[PreferencesKeys.DIFFICULTY] ?: Difficulty.Easy.getDifficultyLevel()
            Difficulty.fromLevel(level)
        }

    val themeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.THEME] ?: false }

    val volumeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLUME] ?: 1.0f }

    val gridSizeFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GRID_SIZE] ?: 3 }

    suspend fun saveDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIFFICULTY] = difficulty.getDifficultyLevel()
        }
    }

    suspend fun saveTheme(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = isDarkMode
        }
    }

    suspend fun saveVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOLUME] = volume
        }
    }

    suspend fun saveGridSize(gridSize: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_SIZE] = gridSize
        }
    }
}
