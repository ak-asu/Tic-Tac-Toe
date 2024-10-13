package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream
import java.util.UUID

class Connections(private val context: Context) {
    private val TAG = "BluetoothHelper"
    private val UUID_STRING =
        "00001101-0000-1000-8000-00805F9B34FB" // Standard SerialPortService ID
    private val APP_NAME = "BluetoothChatApp"

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothReceiver = BluetoothReceiver()
    private var bluetoothSocket: BluetoothSocket? = null
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice
    private val _discoveredDevices = MutableStateFlow(emptyList<BluetoothDevice>().toMutableList())
    val discoveredDevices: StateFlow<MutableList<BluetoothDevice>> = _discoveredDevices
    private val _pairedDevices = MutableStateFlow(emptyList<BluetoothDevice>())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices
    private var connectedThread: ConnectedThread? = null

    // private var acceptThread: AcceptThread? = null
    var onDataReceived: ((DataModel) -> Unit)? = null

    fun registerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, intentFilter)
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(bluetoothReceiver)
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Your Device Does Not Support Bluetooth", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (getMissingPermissions().isNotEmpty()) {
            return
        }
        unregisterReceiver()
        registerReceiver()
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }

    fun getMissingPermissions(): Array<String> {
        val packageManager = context.packageManager
        val packageInfo =
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val allPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val missingPermissions = allPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (missingPermissions.isNotEmpty()) {
            disconnectDevice()
        }
        return missingPermissions
    }

    private fun serializeGameData(dataModel: DataModel): String {
        return Gson().toJson(dataModel)
    }

    private fun deserializeGameData(json: String): DataModel {
        return Gson().fromJson(json, DataModel::class.java)
    }

    @SuppressLint("MissingPermission")
    private fun refreshDeviceLists() {
        if (bluetoothAdapter?.isEnabled == true) {
            _pairedDevices.value = bluetoothAdapter.bondedDevices.toList()
            _discoveredDevices.value =
                discoveredDevices.value.filter { !pairedDevices.value.contains(it) }.toMutableList()
        } else {
            _pairedDevices.value = emptyList()
            _discoveredDevices.value = emptyList<BluetoothDevice>().toMutableList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDevice(device: BluetoothDevice) {
        if (getMissingPermissions().isNotEmpty()) {
            return
        }
        bluetoothSocket?.close()
        val uuid: UUID = device.uuids[0].uuid
        bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
        connectedThread = ConnectedThread()
        connectedThread?.start()
        // bluetoothSocket?.connect()
        _connectedDevice.value = device
    }

    private fun disconnectDevice() {
        _connectedDevice.value = null
        bluetoothSocket?.close()
        bluetoothSocket = null
        connectedThread?.destroy()
        connectedThread = null
        //acceptThread?.destroy()
        //acceptThread = null
    }

    inner class BluetoothReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                return
            }
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!_discoveredDevices.value.contains(it) && !_pairedDevices.value.contains(
                                it
                            )
                        ) {
                            _discoveredDevices.value.add(it)
                        }
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let { connectDevice(it) }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device == _connectedDevice.value) {
                        disconnectDevice()
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshDeviceLists()
                    if (!_pairedDevices.value.contains(_connectedDevice.value)) {
                        disconnectDevice()
                    }
                }

                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (bluetoothAdapter?.isEnabled == false) {
                        disconnectDevice()
                    }
                    refreshDeviceLists()
                }
            }
        }
    }

//    inner class AcceptThread : Thread() {
//        private val serverSocket: BluetoothServerSocket? = bluetoothAdapter
//            ?.listenUsingRfcommWithServiceRecord(APP_NAME, UUID.fromString(UUID_STRING))
//
//        override fun run() {
//            var socket: BluetoothSocket?
//
//            while (true) {
//                socket = try {
//                    serverSocket?.accept()
//                } catch (e: IOException) {
//                    Log.e(TAG, "Socket accept failed", e)
//                    break
//                }
//
//                socket?.let {
//                    connectedThread = ConnectedThread(it)
//                    connectedThread?.start()
//                    break
//                }
//            }
//        }
//
//        fun cancel() {
//            try {
//                serverSocket?.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "Could not close the connect socket", e)
//            }
//        }
//    }

    inner class ConnectedThread : Thread() {
        override fun run() {
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val bytes = bluetoothSocket!!.inputStream.read(buffer)
                    val data = String(buffer, 0, bytes)
                    val message = deserializeGameData(data)
                    onDataReceived?.let { it(message) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun sendData(dataModel: DataModel) {
            if (bluetoothSocket == null) {
                return
            }
            val outputStream: OutputStream = bluetoothSocket!!.outputStream
            val data = serializeGameData(dataModel)
            outputStream.write(data.toByteArray())
            outputStream.flush()
        }
    }
}
