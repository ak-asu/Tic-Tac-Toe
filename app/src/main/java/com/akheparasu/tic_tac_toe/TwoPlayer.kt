package com.akheparasu.tic_tac_toe

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass.Device
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class TwoPlayer(private val context: Context) {
    private val bluetoothManager by lazy{
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy{
        bluetoothManager?.adapter
    }
    private var bluetoothReceiver: BroadcastReceiver? = null
    var deviceList: MutableList<BluetoothDevice> = mutableListOf()


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

                    if (device != null && !deviceList.contains(device)) {
                        deviceList.add(device)
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
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

    /*fun pairDevice(device: BluetoothDevice) {
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

    fun connectToDevice(device: BluetoothDevice, activity: Activity) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()

                manageConnection(socket)
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unable to connect to device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun manageConnection(socket: BluetoothSocket) = withContext(Dispatchers.IO) {
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

    }

    companion object {
        val MY_UUID: UUID = UUID.fromString("xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
        const val REQUEST_CODE = 1001

    }*/

}