package com.akheparasu.tic_tac_toe.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.akheparasu.tic_tac_toe.multiplayer.Connections
import com.akheparasu.tic_tac_toe.settings.SettingsDataStore

val LocalSettings = staticCompositionLocalOf<SettingsDataStore> {
    error("Settings not provided")
}

val LocalNavController = compositionLocalOf<NavHostController?> { null }

val LocalConnectionService = staticCompositionLocalOf<Connections> {
    error("Connections not provided")
}
