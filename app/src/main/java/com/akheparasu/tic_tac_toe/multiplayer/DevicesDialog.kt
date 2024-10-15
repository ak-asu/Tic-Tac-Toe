package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.akheparasu.tic_tac_toe.ui.RoundedRectButton
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalSettings
import com.akheparasu.tic_tac_toe.utils.Preference

@SuppressLint("MissingPermission")
@Composable
fun DevicesDialog(
    onDeviceSelected: (BluetoothDevice, Preference) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val connectionService = LocalConnectionService.current
    val devices = connectionService.devices.collectAsState()
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
        connectionService.registerReceiver()
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
            modifier = Modifier.fillMaxHeight(0.75f),
            onDismissRequest = {
                connectionService.unregisterReceiver()
                onDismiss()
            },
            title = { Text("Select Device") },
            text = {
                Column {
                    val discoveredDevices =
                        devices.value.filter { it.bondState != BluetoothDevice.BOND_BONDED }
                    val pairedDevices =
                        devices.value.filter { it.bondState == BluetoothDevice.BOND_BONDED }
                    Text("Available New Devices:")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(discoveredDevices.size) { index ->
                            DeviceItem(discoveredDevices[index])
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Paired Devices:")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(pairedDevices.size) { index ->
                            DeviceItem(pairedDevices[index])
                        }
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
                Button(onClick = {
                    connectionService.unregisterReceiver()
                    onDismiss()
                }) { Text("Cancel") }
            }
        )
    } else {
        AlertDialog(
            modifier = Modifier.fillMaxHeight(0.75f),
            onDismissRequest = {
                connectionService.unregisterReceiver()
                onDismiss()
            },
            title = { Text("Select Preference") },
            text = {
                Column (verticalArrangement = Arrangement.Center) {
                    RoundedRectButton(onClick = { prefClickAction(Preference.First) }, text="First")
                    Spacer(modifier = Modifier.height(16.dp))
                    RoundedRectButton(onClick = { prefClickAction(Preference.Second) }, text ="Second")
                    Spacer(modifier = Modifier.height(16.dp))
                    RoundedRectButton(onClick = { prefClickAction(Preference.NoPreference) }, text ="No Preference")
                    Spacer(modifier = Modifier.height(16.dp))
                    RoundedRectButton(onClick = { isFirstView = true }, text = "Back")
                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItem(device: BluetoothDevice) {
    val connectionService = LocalConnectionService.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                connectionService.connectDevice(device)
            }
    ) {
        Text(
            text = device.name,
            modifier = Modifier.padding(16.dp)
        )
    }
}