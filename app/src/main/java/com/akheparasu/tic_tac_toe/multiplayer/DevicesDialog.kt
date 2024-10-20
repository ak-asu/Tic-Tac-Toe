package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage

@SuppressLint("MissingPermission")
@Composable
fun DevicesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val connectionService = LocalConnectionService.current
    val devices = connectionService.devices.collectAsState()
    val selectedDevice = connectionService.connectedDevice.collectAsState()
    val isLocEnabled by rememberSaveable { connectionService.isLocEnabled }
    val locEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (connectionService.isLocEnabled.value && connectionService.isBtEnabled()) {
            connectionService.startDiscovery()
        }
    }

    // Update devices when new ones are discovered
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (connectionService.getMissingPermissions().second.isNotEmpty()) {
                    onDismiss()
                }
            }
        }
        val lifecycle = (context as LifecycleOwner).lifecycle
        lifecycle.addObserver(observer)
        connectionService.registerLocReceiver()
        connectionService.startDiscovery()
        onDispose {
            lifecycle.removeObserver(observer)
            connectionService.stopDiscovery()
            connectionService.unRegisterLocReceiver()
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxHeight(0.75f),
        onDismissRequest = { onDismiss() },
        title = { Text("Select Device") },
        text = {
            Column {
                val discoveredDevices =
                    devices.value.filter { it.bondState != BluetoothDevice.BOND_BONDED }
                val pairedDevices =
                    devices.value.filter { it.bondState == BluetoothDevice.BOND_BONDED }
                Text("Available New Devices:")
                if (!isLocEnabled) {
                    Button(onClick = {
                        locEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }) { Text("Turn On Location") }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(discoveredDevices.size) { index ->
                            DeviceItem(discoveredDevices[index])
                        }
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
                        onDismiss()
                        connectionService.setOnlineSetupStage(OnlineSetupStage.Preference)
                    }
                },
                enabled = selectedDevice.value != null
            ) { Text("Done") }
        },
        dismissButton = { Button(onClick = { onDismiss() }) { Text("Cancel") } }
    )
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