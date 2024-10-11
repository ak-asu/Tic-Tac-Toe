package com.akheparasu.tic_tac_toe

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.bluetooth.BluetoothClass.Device
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast

import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class TwoPlayer(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private var deviceDisplayList: MutableList<String> = mutableListOf()

    fun chooseMultiplayerOptionsScreen() {

    }

    fun requestPermissions(activity: Activity, requestCode: Int) {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        ActivityCompat.requestPermissions(activity, permissions, requestCode)

    }

    fun discoverBluetoothDevices() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Your Device Does Not Support Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Bluetooth permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothReceiver = object : BroadcastReceiver (){
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action){
                    val device: BluetoothDevice? = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "Bluetooth permissions are not granted", Toast.LENGTH_SHORT).show()
                        return
                    }
                    deviceList.add(device!!)
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(bluetoothReceiver, filter)

        // Start discovery
        bluetoothAdapter.startDiscovery()

    }

    private fun bluetoothStatus(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun enableBluetooth(activity: Activity, requestCode: Int) {
        if (!bluetoothStatus()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Bluetooth permissions are not granted", Toast.LENGTH_SHORT).show()
                return
            }
            activity.startActivityForResult(enableBtIntent, requestCode)
        }
    }

    fun stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Bluetooth permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }
        bluetoothAdapter?.cancelDiscovery()
        bluetoothReceiver?.let {
            context.unregisterReceiver(it)
        }
        bluetoothReceiver = null
    }

    fun discoverBluetoothDevices(activity: Activity) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            ), REQUEST_CODE)
            return
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Your Device Does Not Support Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        deviceList.add(it)
                        pairDevice(it)
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(bluetoothReceiver, filter)

        bluetoothAdapter?.startDiscovery()
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

    }
}