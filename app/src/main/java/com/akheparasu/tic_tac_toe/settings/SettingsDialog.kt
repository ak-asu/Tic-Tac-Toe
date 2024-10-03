package com.akheparasu.tic_tac_toe.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.DEFAULT_GRID_SIZE
import com.akheparasu.tic_tac_toe.utils.DEFAULT_VOLUME
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.MAX_GRID_SIZE
import com.akheparasu.tic_tac_toe.utils.MIN_GRID_SIZE
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog() {
    val showDialog = remember { mutableStateOf(false) }

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
            confirmButton = { }
        )
    }
}

@Composable
fun DifficultySelector() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    var selectedDifficulty by remember { mutableStateOf(Difficulty.Easy) }

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

    var currentVolume by remember { mutableFloatStateOf(DEFAULT_VOLUME) }

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
        valueRange = 0f..DEFAULT_VOLUME,
        steps = 10,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun ThemeToggle() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsDataStore.darkThemeFlow.collect { isDark ->
            isDarkTheme = isDark
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = if (isDarkTheme) "Dark Theme" else "Light Theme")
        Switch(
            checked = isDarkTheme,
            onCheckedChange = { isChecked ->
                isDarkTheme = isChecked
                coroutineScope.launch {
                    settingsDataStore.saveDarkTheme(isDarkTheme)
                }
            }
        )
    }
}

@Composable
fun GridMenu() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    val gridOptions = (MIN_GRID_SIZE..MAX_GRID_SIZE).toList()
    var selectedGridSize by remember { mutableIntStateOf(DEFAULT_GRID_SIZE) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        settingsDataStore.gridSizeFlow.collect { gridSize ->
            selectedGridSize = gridSize
        }
    }

    Card(
        modifier = Modifier
            .padding(4.dp)
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            }
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(true) {
                    detectTapGestures(
                        onPress = {
                            expanded = !expanded
                            pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                            val press = PressInteraction.Press(it)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(PressInteraction.Release(press))
                        }
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Grid dimension ", modifier = Modifier.padding(8.dp))
                Text(text = selectedGridSize.toString(), modifier = Modifier.padding(8.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = pressOffset.copy(
                y = pressOffset.y - itemHeight
            )
        ) {
            gridOptions.forEach {
                DropdownMenuItem(onClick = {
                    selectedGridSize = it
                    coroutineScope.launch {
                        settingsDataStore.saveGridSize(it)
                    }
                    expanded = false
                }, text = { Text(text = it.toString()) }
                )
            }
        }
    }
}
