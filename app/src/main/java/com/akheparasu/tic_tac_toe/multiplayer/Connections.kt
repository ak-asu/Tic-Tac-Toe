package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.akheparasu.tic_tac_toe.utils.retryTask
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class Connections(private val context: Context) {
    private val TAG = "BluetoothHelper"
    private val UUID_STRING =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SerialPortService ID
    private val APP_NAME = "BluetoothChatApp"

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val bluetoothReceiver = BluetoothReceiver()
    private var isReceiverRegistered = false
    private var bluetoothSocket: BluetoothSocket? = null
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice
    private val _devices = MutableStateFlow(mutableListOf<BluetoothDevice>())
    val devices: StateFlow<List<BluetoothDevice>> = _devices
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null

    // private var acceptThread: AcceptThread? = null
    var onDataReceived: ((DataModel) -> Unit)? = null

    fun registerReceiver() {
        if (isReceiverRegistered) {
            return
        }
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, intentFilter)
        isReceiverRegistered = true
    }

    fun unregisterReceiver() {
        if (!isReceiverRegistered) {
            return
        }
        context.unregisterReceiver(bluetoothReceiver)
        isReceiverRegistered = false
    }

    private fun enableBluetooth() {
        if (getMissingPermissions().isNotEmpty()) {
            return
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(context, enableBtIntent, null)
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (getMissingPermissions().isNotEmpty() || bluetoothAdapter == null) {
            return
        }
        enableBluetooth()
        retryTask({
            if (bluetoothAdapter?.isEnabled == true) {
                if (bluetoothAdapter!!.isDiscovering) {
                    bluetoothAdapter!!.cancelDiscovery()
                }
                bluetoothAdapter!!.startDiscovery()
                acceptThread = AcceptThread()
                acceptThread?.start()
                true
            } else {
                false
            }
        }, 3000, 4)
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

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        if (getMissingPermissions().isNotEmpty()) {
            return
        }
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            device.createBond()
        }
        bluetoothAdapter?.cancelDiscovery()
        retryTask({
            try {
                bluetoothSocket?.close()
                val uuid: UUID = device.uuids[0].uuid
                bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_STRING)
                bluetoothSocket?.connect()
                connectedThread = ConnectedThread()
                connectedThread?.start()
                // bluetoothSocket?.connect()
                _connectedDevice.value = device
                true
            } catch (e: Exception) {
                false
            }
        }, 3000, 3)
    }

    private fun disconnectDevice() {
        _connectedDevice.value = null
        bluetoothSocket?.close()
        bluetoothSocket = null
        connectedThread?.destroy()
        connectedThread = null
        acceptThread?.cancel()
        acceptThread?.destroy()
        acceptThread = null
    }

    fun dispose() {
        unregisterReceiver()
        stopDiscovery()
        disconnectDevice()
    }

    inner class BluetoothReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                return
            }
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.name != null &&
                            !_devices.value.any { prev -> it.address == prev.address }
                        ) {
                            _devices.value = _devices.value.toMutableList().apply { add(it) }
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
                    // refreshDeviceLists()
                }

                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (bluetoothAdapter?.isEnabled == false) {
                        disconnectDevice()
                    }
                    // refreshDeviceLists()
                }
            }
        }
    }

    inner class AcceptThread : Thread() {
        @SuppressLint("MissingPermission")
        private val serverSocket: BluetoothServerSocket? = bluetoothAdapter
            ?.listenUsingRfcommWithServiceRecord(APP_NAME, UUID_STRING)

        override fun run() {
            var socket: BluetoothSocket?

            while (true) {
                socket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket accept failed", e)
                    break
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

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


    private fun serializeGameData(dataModel: DataModel): String {
        return Gson().toJson(dataModel)
    }

    private fun deserializeGameData(json: String): DataModel {
        return Gson().fromJson(json, DataModel::class.java)
    }
}
