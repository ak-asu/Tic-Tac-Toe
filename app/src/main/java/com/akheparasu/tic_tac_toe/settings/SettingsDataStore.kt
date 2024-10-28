package com.akheparasu.tic_tac_toe.settings

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.akheparasu.tic_tac_toe.utils.DEFAULT_VOLUME
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.Preference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val DIFFICULTY = intPreferencesKey("difficulty")
        val DARK_THEME = booleanPreferencesKey("darkTheme")
        val VOLUME = floatPreferencesKey("volume")
        val PLAYER_PREF = intPreferencesKey("playerPref")
    }

    val difficultyFlow: Flow<Difficulty> = context.dataStore.data
        .map { preferences ->
            val level =
                preferences[PreferencesKeys.DIFFICULTY] ?: Difficulty.Easy.getDifficultyLevel()
            Difficulty.fromLevel(level)
        }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            val currentMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            preferences[PreferencesKeys.DARK_THEME]
                ?: (currentMode == Configuration.UI_MODE_NIGHT_YES)
        }

    val volumeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.VOLUME] ?: DEFAULT_VOLUME }

    val playerPrefFlow: Flow<Preference> = context.dataStore.data
        .map { preferences ->
            val id =
                preferences[PreferencesKeys.PLAYER_PREF]
                    ?: Preference.AskEveryTime.getPreferenceId()
            Preference.fromId(id)
        }

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

    suspend fun savePlayerPref(playerPref: Preference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYER_PREF] = playerPref.getPreferenceId()
        }
    }
}
