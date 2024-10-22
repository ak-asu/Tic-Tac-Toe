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
import android.location.LocationManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.retryTask
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class Connections(private val context: Context) {
    private val appUUID = UUID.fromString("d8ea6ad2-5c66-4a16-a033-5b0b7e677f72")
    private val appName = "BluetoothTicTacToeASUApp25"

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    val isLocEnabled = mutableStateOf(false)
    private val bluetoothReceiver = BluetoothReceiver()
    private val locationReceiver = LocationReceiver()
    private var isReceiverRegistered = false
    private var isLocationRegistered = false
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice
    private var deviceToConnect: BluetoothDevice? = null
    private val _devices = MutableStateFlow(mutableListOf<BluetoothDevice>())
    val devices: StateFlow<List<BluetoothDevice>> = _devices
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null

    private var onDataReceived: ((DataModel) -> Unit)? = null
    var receivedDataModel: DataModel? = null
    private val _onlineSetupStage = MutableStateFlow(OnlineSetupStage.Idle)
    val onlineSetupStage: StateFlow<OnlineSetupStage> = _onlineSetupStage

    init {
        getMissingPermissions()
    }

    private fun registerReceiver() {
        if (isReceiverRegistered) {
            return
        }
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, intentFilter)
        acceptThread?.cancel()
        acceptThread = AcceptThread()
        acceptThread?.start()
        isReceiverRegistered = true
    }

    fun unregisterReceiver() {
        unregisterLocReceiver()
        if (!isReceiverRegistered) {
            return
        }
        stopDiscovery()
        connectedThread?.cancel()
        acceptThread?.cancel()
        context.unregisterReceiver(bluetoothReceiver)
        isReceiverRegistered = false
    }

    fun registerLocReceiver() {
        if (isLocationRegistered) {
            return
        }
        val intentFilter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        context.registerReceiver(locationReceiver, intentFilter)
        isLocationRegistered = true
    }

    fun unregisterLocReceiver() {
        if (!isLocationRegistered) {
            return
        }
        context.unregisterReceiver(locationReceiver)
        isLocationRegistered = false
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        try {
            if (bluetoothAdapter?.isEnabled == true) {
                if (bluetoothAdapter!!.isDiscovering) {
                    bluetoothAdapter!!.cancelDiscovery()
                }
                bluetoothAdapter!!.startDiscovery()
                return true
            } else {
                return false
            }
        } catch (_: Exception) {
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery(): Boolean {
        return try {
            bluetoothAdapter?.cancelDiscovery() ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun getMissingPermissions(): Pair<Array<String>, Array<String>> {
        val packageManager = context.packageManager
        val packageInfo =
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val allPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val missingPermissions = allPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        val btMissingPermissions =
            missingPermissions.filter { p -> p.contains("bluetooth") }.toTypedArray()
        val remainingMissingPermissions =
            missingPermissions.filter { p -> !p.contains("bluetooth") }.toTypedArray()
        if (btMissingPermissions.isNotEmpty() || !isBtEnabled()) {
            _onlineSetupStage.value = OnlineSetupStage.NoService
            unregisterReceiver()
        } else {
            registerReceiver()
        }
        if (remainingMissingPermissions.isNotEmpty()) {
            isLocEnabled.value = false
        } else {
            isLocEnabled.value = locIsEnabled()
        }
        return Pair(btMissingPermissions, remainingMissingPermissions)
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        if (getMissingPermissions().first.isNotEmpty() || !isBtEnabled()) {
            return
        }
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            device.createBond()
            deviceToConnect = device
        } else {
            setupConnection(device)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupConnection(device: BluetoothDevice) {
        retryTask({
            try {
                connectedThread?.cancel()
                connectedThread = ConnectedThread(device)
                connectedThread?.start()
                deviceToConnect = null
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }, 3000, 2)
    }

    inner class LocationReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.second.isNotEmpty()) {
                context.unregisterReceiver(locationReceiver)
                return
            }
            when (intent.action) {
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    isLocEnabled.value = locIsEnabled()
                }
            }
        }
    }

    inner class BluetoothReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val missingPermissions = getMissingPermissions()
            if (missingPermissions.first.isNotEmpty()) {
                context.unregisterReceiver(bluetoothReceiver)
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
                    device?.let {
                        // todo
                        connectedThread?.start()
                        _devices.value = _devices.value.map { d ->
                            if (device.address == d.address) {
                                device
                            } else {
                                d
                            }
                        }.toMutableList()
                    }
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it == _connectedDevice.value) {
                            connectedThread?.cancel()
                        }
                        _devices.value = _devices.value.map { d ->
                            if (device.address == d.address) {
                                device
                            } else {
                                d
                            }
                        }.toMutableList()
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        _devices.value = _devices.value.map { d ->
                            if (device.address == d.address) {
                                device
                            } else {
                                d
                            }
                        }.toMutableList()
                        if (it.address == deviceToConnect?.address &&
                            it.bondState == BluetoothDevice.BOND_BONDED
                        ) {
                            setupConnection(deviceToConnect!!)
                        }
                    }
                }

                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (bluetoothAdapter?.isEnabled == false) {
                        connectedThread?.cancel()
                    }
                    device?.let {
                        _devices.value = _devices.value.map { d ->
                            if (device.address == d.address) {
                                device
                            } else {
                                d
                            }
                        }.toMutableList()
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    bluetoothAdapter!!.bondedDevices.toList().forEach { d ->
                        if (!_devices.value.any { prev -> d.address == prev.address }
                        ) {
                            _devices.value = _devices.value.toMutableList().apply { add(d) }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(appName, appUUID)
        }

        @Volatile
        private var isRunning = true

        override fun run() {
            while (isRunning) {
                val bluetoothSocket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    isRunning = false
                    null
                }
                bluetoothSocket?.also {
                    if (it.remoteDevice != null) {
                        connectedThread?.cancel()
                        connectedThread = ConnectedThread(
                            bluetoothSocket.remoteDevice!!,
                            bluetoothSocket
                        )
                        connectedThread?.start()
                    }
                    cancel()
                }
            }
        }

        fun cancel() {
            isRunning = false
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    inner class ConnectedThread(device: BluetoothDevice, bluetoothSocket: BluetoothSocket? = null) :
        Thread() {
        private val clientSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            stopDiscovery()
            (bluetoothSocket ?: device.createRfcommSocketToServiceRecord(appUUID)).apply {
                connect()
            }
        }

        @Volatile
        private var isRunning = true

        override fun run() {
            clientSocket?.let {
                if (!it.isConnected) {
                    cancel()
                    return
                }
                _connectedDevice.value = it.remoteDevice!!
                sendData(DataModel())
                val buffer = ByteArray(1024)
                while (isRunning && it.isConnected) {
                    try {
                        val bytes = it.inputStream.read(buffer)
                        val data = String(buffer, 0, bytes)
                        if (data.isNotEmpty()) {
                            val message = deserializeGameData(data)
                            if (!message.gameState.connectionEstablished) {
                                cancel()
                                break
                            }
                            if (message.metaData.choices.isEmpty()) {
                                setOnlineSetupStage(OnlineSetupStage.Preference)
                            } else if (onlineSetupStage.value == OnlineSetupStage.Initialised) {
                                if (message.metaData.miniGame.player2Choice.isEmpty()) {
                                    receivedDataModel =
                                        if (connectedDevice.value!!.address < message.metaData.choices.last().name) {
                                            message
                                        } else {
                                            null
                                        }
                                } else {
                                    receivedDataModel = message
                                    setOnlineSetupStage(OnlineSetupStage.GameStart)
                                }
                            } else if (onlineSetupStage.value == OnlineSetupStage.Idle) {
                                receivedDataModel = message
                                setOnlineSetupStage(OnlineSetupStage.Initialised)
                            } else {
                                onDataReceived?.let { it(message) }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun sendData(dataModel: DataModel) {
            if (clientSocket?.isConnected == true) {
                val outputStream: OutputStream = clientSocket!!.outputStream
                val data = serializeGameData(dataModel)
                outputStream.write(data.toByteArray())
                //outputStream.flush()
                //todo
            }
        }

        fun cancel() {
            receivedDataModel = null
            clientSocket?.close()
            _connectedDevice.value = null
            setOnlineSetupStage(OnlineSetupStage.Idle)
            isRunning = false
        }
    }

    fun sendData(dataModel: DataModel) {
        connectedThread?.sendData(dataModel)
    }

    fun setOnDataReceived(value: ((DataModel) -> Unit)? = null) {
        onDataReceived = value
    }

    fun setOnlineSetupStage(value: OnlineSetupStage) {
        _onlineSetupStage.value = value
        getMissingPermissions()
    }

    fun isBtEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    private fun locIsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun serializeGameData(dataModel: DataModel): String {
        return Gson().toJson(dataModel)
    }

    private fun deserializeGameData(json: String): DataModel {
        return Gson().fromJson(json, DataModel::class.java)
    }
}
