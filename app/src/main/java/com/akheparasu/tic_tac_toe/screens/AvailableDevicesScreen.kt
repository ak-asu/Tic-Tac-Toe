package com.akheparasu.tic_tac_toe.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.akheparasu.tic_tac_toe.TwoPlayer
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.android.identity.cbor.Uint


@Composable
fun BluetoothDeviceItem(device: BluetoothDevice, onClick: (BluetoothDevice) -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(device) }.padding(8.dp),
    ) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Text(
                text = device.name ?: "Unknown Device",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun AvailableDevicesScreen(twoPlayer: TwoPlayer, activity: Activity) {

    //get permissions, enable bluetooth, and start discovery of other devices
    LaunchedEffect(Uint){
        twoPlayer.requestPermissions(activity, 1)
        twoPlayer.enableBluetooth(activity, 2)
        twoPlayer.discoverBluetoothDevices()
    }
    Column{
        Text("Available Bluetooth Devices")

        LazyColumn {
            // Iterating through each device in the `devices` list
            items(twoPlayer.deviceList) { device ->
                // For each device, we display a BluetoothDeviceItem composable
                BluetoothDeviceItem(
                    device = device,
                    onClick = { selectedDevice -> // When the item is clicked, trigger the callback
                        // pair and connect the devices
                    }
                )
            }
        }
    }
}