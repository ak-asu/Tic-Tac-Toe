package com.akheparasu.tic_tac_toe.multiplayer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat


@SuppressLint("MissingPermission")
@Composable
fun DevicesScreen(twoPlayer: TwoPlayer, activity: Activity) {
    //val context = LocalContext.current
    val devices by twoPlayer.deviceList
    val pairedDevices by twoPlayer.pairedDevicesList

    val context = LocalContext.current
    var showWhoGoesFirstDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isServerRunning by remember { mutableStateOf(false) } // New state variable

    LaunchedEffect(Unit) {
        twoPlayer.discoverBluetoothDevices() // Start discovery when this screen is shown
    }

    Column{
        Text("Available Bluetooth Devices",modifier = Modifier.padding(16.dp))
        LazyColumn {
            // Iterating through each device in the `devices` list
            items(devices) { device ->
                // For each device, display a BluetoothDeviceItem
                BluetoothDeviceItem(
                    device = device,
                    onClick = { selectedDevice -> // When the item is clicked, trigger the callback
                        // pair the devices
                        twoPlayer.pairDevice(selectedDevice)
                    }
                )
            }
        }
        // Section for paired devices
        Text("Paired Bluetooth Devices", modifier = Modifier.padding(16.dp))
        LazyColumn {
            items(pairedDevices) { pairedDevice ->
                BluetoothDeviceItem(
                    device = pairedDevice,
                    onClick = { selectedDevice = pairedDevice

                        //start server if its not running
                        /*if(!isServerRunning) {

                        }*/
                    }
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                Toast.makeText(context, "Trying To start a server", Toast.LENGTH_SHORT).show()
                twoPlayer.startBluetoothServer {
                    isServerRunning = true
                    Toast.makeText(context, "Server Started", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp) // Full width with padding
        ){
            Text("Start Server")
        }

        Button(
            onClick = {
                if(selectedDevice != null){
                    twoPlayer.connectToDevice(selectedDevice!!, activity)
                }else{
                    Toast.makeText(context, "No device selected", Toast.LENGTH_SHORT).show()
                }

            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp) // Full width with padding
        ){
            Text("Connect To Server")
        }
    }
    if (showWhoGoesFirstDialog && selectedDevice != null){
        WhoGoesFirst {  }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoGoesFirst(
    onConfirm: (Boolean) -> Unit, // true if "ME" is selected, false if "OPPONENT" is selected
){
    AlertDialog(
        onDismissRequest = {onConfirm(false)},
        confirmButton = {
            Button(onClick = { onConfirm(true) }) {
                Text("ME")
            }
        },
        dismissButton = {
            Button(onClick = { onConfirm(false) }) {
                Text("OPPONENT")
            }
        },
        title = { Text("Who Goes First?") },
        text = { Text("Please select who will go first.") },
        properties = DialogProperties()
    )

}

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