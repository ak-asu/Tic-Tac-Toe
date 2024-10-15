package com.akheparasu.tic_tac_toe.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.akheparasu.tic_tac_toe.utils.DEFAULT_VOLUME
import com.akheparasu.tic_tac_toe.utils.Difficulty
import com.akheparasu.tic_tac_toe.utils.LocalAudioPlayer
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.Preference
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
                    PlayerPrefMenu()
                    OnlinePrefMenu()
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
            .padding(8.dp),
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
                    containerColor = if (selectedDifficulty == difficulty) selectedDifficulty.getColor() else Color.Gray,
                ),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.weight(1f).padding(4.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(text = difficulty.name)
            }
        }
    }
}

@Composable
fun VolumeSlider() {
    val settingsDataStore = LocalSettings.current
    val audioController = LocalAudioPlayer.current
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
            audioController.setVolume(volume)
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
fun PlayerPrefMenu() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    val options = Preference.entries.toList()
    var selectedOption by remember { mutableStateOf(Preference.AskEveryTime) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        settingsDataStore.playerPrefFlow.collect { playerPref ->
            selectedOption = playerPref
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
                Text(text = "Player Preference", modifier = Modifier.padding(8.dp))
                Text(text = selectedOption.name, modifier = Modifier.padding(8.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = pressOffset.copy(
                y = pressOffset.y - itemHeight
            )
        ) {
            options.forEach {
                DropdownMenuItem(onClick = {
                    selectedOption = it
                    coroutineScope.launch {
                        settingsDataStore.savePlayerPref(it)
                    }
                    expanded = false
                }, text = { Text(text = it.name) }
                )
            }
        }
    }
}

@Composable
fun OnlinePrefMenu() {
    val settingsDataStore = LocalSettings.current
    val coroutineScope = rememberCoroutineScope()

    val options = Preference.entries.toList()
    var selectedOption by remember { mutableStateOf(Preference.AskEveryTime) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        settingsDataStore.onlinePrefFlow.collect { onlinePref ->
            selectedOption = onlinePref
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
                Text(text = "Online Preference", modifier = Modifier.padding(8.dp))
                Text(text = selectedOption.name, modifier = Modifier.padding(8.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = pressOffset.copy(
                y = pressOffset.y - itemHeight
            )
        ) {
            options.forEach {
                DropdownMenuItem(onClick = {
                    selectedOption = it
                    coroutineScope.launch {
                        settingsDataStore.saveOnlinePref(it)
                    }
                    expanded = false
                }, text = { Text(text = it.name) }
                )
            }
        }
    }
}
