package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.Preference

@Composable
fun DevicesDialog(
    onDeviceSelected: (BluetoothDevice, Preference) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val connectionService = LocalConnectionService.current
    val pairedDevices = connectionService.pairedDevices.collectAsState()
    val discoveredDevices = connectionService.discoveredDevices.collectAsState()
    val selectedDevice = connectionService.connectedDevice.collectAsState()
    var isFirstView by remember { mutableStateOf(true) }
    val onlinePrefFlow = settings.onlinePrefFlow.collectAsState(initial = Preference.AskEveryTime)
    val prefClickAction: (Preference) -> Unit = {
        if (selectedDevice.value == null) {
            isFirstView = true
        } else {
            onDismiss()
            onDeviceSelected(selectedDevice.value!!, it)
        }
    }

    LaunchedEffect(Unit) {
        connectionService.startDiscovery()
    }
    // Update devices when new ones are discovered
    DisposableEffect(context) {
        onDispose {
            connectionService.stopDiscovery()
        }
    }

    if (isFirstView || selectedDevice.value == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Device") },
            text = {
                Column {
                    Text("Available New Devices:")
                    discoveredDevices.value.forEach { device ->
                        DeviceItem(device, onDeviceSelected = {  })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Paired Devices:")
                    pairedDevices.value.forEach { device ->
                        DeviceItem(device, onDeviceSelected = {  })
                    }

                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDevice.value != null) {
                            if (onlinePrefFlow.value == Preference.AskEveryTime) {
                                isFirstView = false
                            } else {
                                onDismiss()
                                onDeviceSelected(selectedDevice.value!!, onlinePrefFlow.value)
                            }
                        }
                    },
                    enabled = selectedDevice.value != null
                ) { Text("Next") }
            },
            dismissButton = {
                Button(onClick = onDismiss) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Device") },
            text = {
                Column {
                    Button(onClick = { prefClickAction(Preference.First) }) { Text("First") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { prefClickAction(Preference.Second) }) { Text("Second") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { prefClickAction(Preference.NoPreference) }) { Text("No Preference") }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { isFirstView = true }) { Text("Back") }
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(device: BluetoothDevice, onDeviceSelected: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onDeviceSelected()
            }
    ) {
        Text(
            text = "${device.name} - ${device.address}",
            modifier = Modifier.padding(16.dp)
        )
    }
}