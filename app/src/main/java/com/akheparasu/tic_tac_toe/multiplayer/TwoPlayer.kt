package com.akheparasu.tic_tac_toe.multiplayer

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import androidx.compose.runtime.mutableStateOf


class TwoPlayer(private val context: Context) {
    private val bluetoothManager by lazy{
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy{
        bluetoothManager?.adapter
    }
    private var bluetoothReceiver: BroadcastReceiver? = null
    //var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    var deviceList = mutableStateOf<List<BluetoothDevice>>(listOf())
    var pairedDevicesList = mutableStateOf<List<BluetoothDevice>>(listOf())

    private var currentServerSocket: BluetoothServerSocket? = null
    private var currentClientSocket: BluetoothSocket? = null

    // BroadcastReceiver to listen for paired/unpaired devices
    private val bondStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                updatePairedDevices()  // if a device is paired/unpaired, update the list
            }
        }
    }

    companion object {
        val MY_UUID: UUID = UUID.fromString("a87a2c7c-bc26-4782-bc6a-1998fa5168d0")
        const val REQUEST_CODE = 1001

    }

    init {
        // find initial paired devices
        updatePairedDevices()

        // Register BroadcastReceiver to listen for pairing/unpairing
        val filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bondStateReceiver, filter)
    }

    @SuppressLint("MissingPermission")
    private fun updatePairedDevices() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            pairedDevicesList.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } else {
            Toast.makeText(context, "Bluetooth CONNECT permissions are not granted", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverBluetoothDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Your Device Does Not Support Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Bluetooth SCAN permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothReceiver = object : BroadcastReceiver (){
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action){
                    val device: BluetoothDevice? = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if (device != null && !deviceList.value.contains(device)) {
                        //deviceList.add(device)
                        deviceList.value = deviceList.value + device
                    }
                }
                if(BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && deviceList.value.contains(device)) {
                        // Remove the disconnected device
                        deviceList.value = deviceList.value - device
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)

        // Start discovery
        bluetoothAdapter?.startDiscovery()

    }

    fun bluetoothStatus(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Bluetooth Scan and Connect permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }
        bluetoothAdapter?.cancelDiscovery()
        bluetoothReceiver?.let {
            context.unregisterReceiver(it)
        }
        bluetoothReceiver = null
    }

    fun pairDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            device.createBond()
            Toast.makeText(context, "Pairing with device...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Pairing failed", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("MissingPermission")
    fun startBluetoothServer(onServerStarted: () -> Unit){
        CoroutineScope(Dispatchers.IO).launch {
            try {
                currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("TicTacToeServer", MY_UUID)

                withContext(Dispatchers.Main) {
                    onServerStarted()
                    Toast.makeText(context, "Server Started", Toast.LENGTH_SHORT).show()
                }

                while(true){
                    val clientSocket = currentServerSocket?.accept()
                    if(clientSocket != null){
                        withContext(Dispatchers.Main){
                            Toast.makeText(context, "Client Connected!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to start server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                try {
                    currentServerSocket?.close()
                } catch (closeException: IOException) {
                    Log.e("Bluetooth", "Failed to close server socket: ${closeException.message}")
                }
            }
        }
    }

    fun connectToDevice(device: BluetoothDevice, activity: Activity) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                currentClientSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                currentClientSocket?.connect()
                //manageConnection(socket)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Connected to device", Toast.LENGTH_SHORT).show()
                }


            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unable to connect to device: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                try {
                    currentClientSocket?.close()
                } catch (closeException: IOException) {
                    Log.e("Bluetooth", "Failed to close socket: ${closeException.message}")
                }
            }
        }
    }

    /*fun isServerReady(device: BluetoothDevice): Boolean {
        CoroutineScope(Dispatchers.IO).launch {
            // Implement a method to check if Device 1 is available (could be a simple connect attempt)
            val isServerReady = attemptToConnect(device)

            if (isServerReady) {
                // Server is ready, proceed with connection
                connectToDevice(device, activity)
            } else {
                // Retry or handle failure
            }
        }
    }

    private suspend fun attemptToConnect(device: BluetoothDevice): Boolean {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            socket.connect()  // This will throw an exception if it can't connect
            true  // Connection successful
        } catch (e: IOException) {
            false  // Connection failed
        }
    }*/







    /*suspend fun manageConnection(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
        try {
            socket.inputStream.use { input ->
                val buffer = ByteArray(1024)
                var bytes: Int
                while (true) {
                    bytes = input.read(buffer)
                    if (bytes == -1) break
                    val receivedMessage = String(buffer, 0, bytes)
                    handleReceivedMessage(receivedMessage)
                }
            }
        } catch (e: IOException) {

        } finally {
            socket.close()
        }
    }

    fun handleReceivedMessage(message: String) {

    }*/

}