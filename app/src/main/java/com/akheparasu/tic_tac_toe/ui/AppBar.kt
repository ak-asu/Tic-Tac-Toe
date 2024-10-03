package com.akheparasu.tic_tac_toe.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.akheparasu.tic_tac_toe.utils.LocalNavController
import com.akheparasu.tic_tac_toe.settings.SettingsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar() {
    val navController = LocalNavController.current
    val currentBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val currentRoute = currentBackStackEntry?.destination?.route
    TopAppBar(
        title = {},
        navigationIcon = {
            if (currentRoute != null && currentRoute != "home") {
                IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
            }
        },
        actions = { SettingsDialog() }
    )
}
