package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class Connections(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothReceiver = BluetoothReceiver()
    private var bluetoothSocket: BluetoothSocket? = null
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    fun registerReceiver() {
        context.registerReceiver(
            bluetoothReceiver,
            IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        )
    }

    fun unregisterReceiver() {
        context.unregisterReceiver(bluetoothReceiver)
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

    suspend fun receiveData(onDataReceived: (DataModel) -> Unit) = withContext(Dispatchers.IO) {
        if (bluetoothSocket == null) {
            return@withContext
        }
        val inputStream: InputStream = bluetoothSocket!!.inputStream
        try {
            val buffer = ByteArray(1024)
            val bytes = inputStream.read(buffer)
            val data = String(buffer, 0, bytes)
            val message = deserializeGameData(data)
            withContext(Dispatchers.Main) {
                onDataReceived(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
            bluetoothSocket?.close()
            _connectedDevice.value = null
        }
        return missingPermissions
    }

    private fun serializeGameData(dataModel: DataModel): String {
        return Gson().toJson(dataModel)
    }

    private fun deserializeGameData(json: String): DataModel {
        return Gson().fromJson(json, DataModel::class.java)
    }

    inner class BluetoothReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                return
            }
            if (BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED == intent?.action) {
                if (bluetoothAdapter?.isEnabled == true) {
                    val bondedDevices = bluetoothAdapter.bondedDevices
                    for (device in bondedDevices) {
                        // This is a simple check; you may need to adapt it to your specific use case
                        val deviceStatus =
                            bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET)
                        if (deviceStatus == BluetoothProfile.STATE_CONNECTED) {
                            bluetoothSocket?.close()
                            val uuid: UUID = device.uuids[0].uuid
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                            _connectedDevice.value = device
                            return
                        }
                    }
                }
                bluetoothSocket?.close()
                _connectedDevice.value = null
            }
        }
    }
}
