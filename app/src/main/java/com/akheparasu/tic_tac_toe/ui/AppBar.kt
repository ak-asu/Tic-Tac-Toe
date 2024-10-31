package com.akheparasu.tic_tac_toe.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import com.akheparasu.tic_tac_toe.settings.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    TopAppBar(
        title = {},
        navigationIcon = {},
        actions = { SettingsDialog() }
    )
}
