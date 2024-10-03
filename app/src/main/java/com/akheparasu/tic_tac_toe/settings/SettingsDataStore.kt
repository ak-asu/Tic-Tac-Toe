package com.akheparasu.tic_tac_toe.settings

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.akheparasu.tic_tac_toe.utils.DEFAULT_GRID_SIZE
import com.akheparasu.tic_tac_toe.utils.DEFAULT_VOLUME
import com.akheparasu.tic_tac_toe.utils.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val DIFFICULTY = intPreferencesKey("difficulty")
        val DARK_THEME = booleanPreferencesKey("darkTheme")
        val VOLUME = floatPreferencesKey("volume")
        val GRID_SIZE = intPreferencesKey("gridSize")
    }

    val difficultyFlow: Flow<Difficulty> = context.dataStore.data
        .map { preferences ->
            val level = preferences[PreferencesKeys.DIFFICULTY] ?: Difficulty.Easy.getDifficultyLevel()
            Difficulty.fromLevel(level)
        }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val currentMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            preferences[PreferencesKeys.DARK_THEME] ?: (currentMode == Configuration.UI_MODE_NIGHT_YES)
        }

    val volumeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLUME] ?: DEFAULT_VOLUME }

    val gridSizeFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.GRID_SIZE] ?: DEFAULT_GRID_SIZE }

    suspend fun saveDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIFFICULTY] = difficulty.getDifficultyLevel()
        }
    }

    suspend fun saveDarkTheme(isDarkTheme: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_THEME] = isDarkTheme
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
