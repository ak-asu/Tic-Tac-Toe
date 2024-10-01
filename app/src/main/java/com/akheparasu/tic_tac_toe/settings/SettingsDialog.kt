package com.akheparasu.tic_tac_toe.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.Difficulty
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog() {
    val showDialog = remember { mutableStateOf(false) }

    // Top right button to open settings
    IconButton(onClick = { showDialog.value = true }) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(text = "Settings") },
            text = {
                Column {
                    DifficultySelector()
                    VolumeSlider()
                    ThemeToggle()
                    GridMenu()
                }
            },
            confirmButton = {
                Button(onClick = { showDialog.value = false }) {
                    Text("Confirm")
                }
            }
        )
    }
}

@Composable
fun DifficultySelector() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDifficulty by remember { mutableStateOf(Difficulty.Easy) }

    // Collect the difficulty value from DataStore
    LaunchedEffect(Unit) {
        settingsDataStore.difficultyFlow.collect { difficulty ->
            selectedDifficulty = difficulty
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Difficulty.entries.forEach { difficulty ->
            Button(
                onClick = {
                    selectedDifficulty = difficulty
                    coroutineScope.launch {
                        settingsDataStore.saveDifficulty(difficulty)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedDifficulty == difficulty) Color.Green else Color.Gray,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = difficulty.name)
            }
        }
    }
}

@Composable
fun VolumeSlider() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    var currentVolume by remember { mutableFloatStateOf(1f) }

    // Collect the volume from DataStore
    LaunchedEffect(Unit) {
        settingsDataStore.volumeFlow.collect { volume ->
            currentVolume = volume
        }
    }

    Slider(
        value = currentVolume,
        onValueChange = { volume ->
            currentVolume = volume
            coroutineScope.launch {
                settingsDataStore.saveVolume(volume)
            }
        },
        valueRange = 0f..1f,
        steps = 10,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun ThemeToggle() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    // Track whether dark mode is enabled
    var isDarkTheme by remember { mutableStateOf(false) }

    // Collect the theme setting from DataStore
    LaunchedEffect(Unit) {
        settingsDataStore.themeFlow.collect { isDark ->
            isDarkTheme = isDark
        }
    }

    // Switch for toggling dark mode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = if (isDarkTheme) "Dark Mode" else "Light Mode")
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { isChecked ->
                isDarkTheme = isChecked
                coroutineScope.launch {
                    settingsDataStore.saveTheme(isDarkTheme)
                }
            }
        )
    }
}

@Composable
fun GridMenu() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    val gridOptions = listOf(3, 4, 5)
    var selectedGridSize by remember { mutableIntStateOf(3) }

    // Collect the grid size from DataStore
    LaunchedEffect(Unit) {
        settingsDataStore.gridSizeFlow.collect { gridSize ->
            selectedGridSize = gridSize
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        gridOptions.forEach { gridSize ->
            Button(
                onClick = {
                    selectedGridSize = gridSize
                    coroutineScope.launch {
                        settingsDataStore.saveGridSize(gridSize)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedGridSize == gridSize) Color.Green else Color.Gray,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(text = gridSize.toString())
            }
        }
    }
}
