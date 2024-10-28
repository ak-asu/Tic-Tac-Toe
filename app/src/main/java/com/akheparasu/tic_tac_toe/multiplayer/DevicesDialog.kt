package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.PADDING_HEIGHT
import com.akheparasu.tic_tac_toe.utils.SPACER_HEIGHT

@SuppressLint("MissingPermission")
@Composable
fun DevicesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val connectionService = LocalConnectionService.current
    val devices = connectionService.devices.collectAsState()
    val onlineSetupStage = connectionService.onlineSetupStage.collectAsState()
    val isLocEnabled = connectionService.isLocEnabled
    val locEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        connectionService.getMissingPermissions()
        if (connectionService.isLocEnabled.value && connectionService.isBtEnabled()) {
            connectionService.registerLocReceiver()
            connectionService.startDiscovery()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            locEnableLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    LaunchedEffect(onlineSetupStage.value) {
        if (!(onlineSetupStage.value == OnlineSetupStage.Idle ||
                    onlineSetupStage.value == OnlineSetupStage.NoService)
        ) {
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (connectionService.getMissingPermissions().first.isNotEmpty()) {
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
            connectionService.unregisterLocReceiver()
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxHeight(0.75f),
        onDismissRequest = { onDismiss() },
        title = { Text("Select Device") },
        text = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val discoveredDevices =
                    devices.value.filter { it.bondState != BluetoothDevice.BOND_BONDED }
                val pairedDevices =
                    devices.value.filter { it.bondState == BluetoothDevice.BOND_BONDED }
                Text("Available New Devices:")
                if (!isLocEnabled.value) {
                    Button(onClick = {
                        permissionLauncher.launch(connectionService.getMissingPermissions().second)
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
                Spacer(modifier = Modifier.height(SPACER_HEIGHT.dp))
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
        confirmButton = { },
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
            modifier = Modifier.padding(PADDING_HEIGHT.dp)
        )
    }
}