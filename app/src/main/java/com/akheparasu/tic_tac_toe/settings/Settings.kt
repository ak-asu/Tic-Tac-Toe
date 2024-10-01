package com.akheparasu.tic_tac_toe.settings

import androidx.compose.runtime.staticCompositionLocalOf
import com.akheparasu.tic_tac_toe.utils.Difficulty

val LocalSettings = staticCompositionLocalOf<SettingsDataStore> {
    error("Settings not provided")
}
