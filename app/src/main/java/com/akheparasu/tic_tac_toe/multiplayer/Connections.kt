package com.akheparasu.tic_tac_toe.multiplayer

import android.annotation.SuppressLint
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
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class Connections(private val context: Context) {
    private val appUUID = UUID.fromString("d8ea6ad2-5c66-4a16-a033-5b0b7e677f72")
    private val appName = "BluetoothTicTacToeASUApp25"
    private val aliveString = "I am alive $appName"

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
    var receivedDataModel: DataModel = DataModel()
    private val _onlineSetupStage = MutableStateFlow(OnlineSetupStage.Idle)
    val onlineSetupStage: StateFlow<OnlineSetupStage> = _onlineSetupStage

    init {
        getMissingPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun registerReceiver() {
        if (isReceiverRegistered) {
            return
        }
        acceptThread?.cancel()
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, intentFilter)
        val serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(appName, appUUID)
        acceptThread = AcceptThread(serverSocket)
        acceptThread?.start()
        isReceiverRegistered = true
    }

    fun unregisterReceiver() {
        if (!isReceiverRegistered) {
            return
        }
        stopDiscovery()
        unregisterLocReceiver()
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
        isLocEnabled.value = false
        isLocationRegistered = false
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(): Boolean {
        try {
            if (bluetoothAdapter?.isEnabled == true) {
                _devices.value =
                    bluetoothAdapter!!.bondedDevices.filter { it.name != null && it.address.isNotEmpty() }
                        .toMutableList()
                if (bluetoothAdapter!!.isDiscovering) {
                    bluetoothAdapter!!.cancelDiscovery()
                }
                bluetoothAdapter!!.startDiscovery()
                return true
            } else {
                _devices.value = mutableListOf()
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
        val (btMissingPermissions, remainingMissingPermissions) = missingPermissions.partition {
            it.lowercase().contains("bluetooth")
        }
        if (btMissingPermissions.isNotEmpty() || !isBtEnabled()) {
            _onlineSetupStage.value = OnlineSetupStage.NoService
            unregisterReceiver()
        } else {
            registerReceiver()
        }
        if (remainingMissingPermissions.isNotEmpty()) {
            unregisterLocReceiver()
        } else {
            isLocEnabled.value = locIsEnabled()
        }
        return Pair(btMissingPermissions.toTypedArray(), remainingMissingPermissions.toTypedArray())
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        if (getMissingPermissions().first.isNotEmpty() || !isBtEnabled()) {
            Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_SHORT).show()
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
        CoroutineScope(Dispatchers.IO).launch {
            val isConnected = withTimeoutOrNull(4000) {
                try {
                    connectedThread?.cancel()
                    val socket = device.createRfcommSocketToServiceRecord(appUUID)
                    connectedThread = ConnectedThread("player1", device, socket)
                    deviceToConnect = null
                    connectedThread?.getSocket()!!.connect()
                    true
                } catch (_: Exception) {
                    false
                }
            }
            withContext(Dispatchers.Main) {
                if (isConnected != true) {
                    Toast.makeText(context, "Could not connect", Toast.LENGTH_SHORT).show()
                    connectedThread?.cancel()
                    setOnlineSetupStage(OnlineSetupStage.Idle)
                }
            }
        }
    }

    private inner class LocationReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (getMissingPermissions().second.isNotEmpty()) {
                return
            }
            when (intent.action) {
                LocationManager.PROVIDERS_CHANGED_ACTION -> {
                    isLocEnabled.value = locIsEnabled()
                }
            }
        }
    }

    private inner class BluetoothReceiver : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            if (getMissingPermissions().first.isNotEmpty()) {
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
                        if (it.address == connectedThread?.getDevice()?.address &&
                            connectedThread?.checkRunning() == false
                        ) {
                            try {
                                connectedThread?.start()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
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

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (it.address == connectedThread?.getDevice()?.address) {
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
                        if (it.address == deviceToConnect?.address) {
                            if (it.bondState == BluetoothDevice.BOND_BONDED) {
                                setupConnection(deviceToConnect!!)
                            } else {
                                deviceToConnect = null
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread(private val serverSocket: BluetoothServerSocket?) : Thread() {
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
                            "player2",
                            bluetoothSocket.remoteDevice!!,
                            bluetoothSocket
                        )
                        connectedThread?.start()
                        cancel()
                    }
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
    private inner class ConnectedThread(
        private val playerId: String,
        private val device: BluetoothDevice,
        private val clientSocket: BluetoothSocket? = null
    ) :
        Thread() {
        private val handler = Handler(Looper.getMainLooper())
        private val runnable = object : Runnable {
            override fun run() {
                if (clientSocket?.isConnected == true && clientSocket.inputStream.available() > 0) {
                    sendData(aliveString)
                    handler.postDelayed(this, 10000)
                } else {
                    handler.removeCallbacks(this)
                }
            }
        }

        @Volatile
        private var isRunning = false

        override fun run() {
            clientSocket?.let {
                if (isRunning) {
                    return
                }
                isRunning = true
                _connectedDevice.value = it.remoteDevice!!
                handler.post(runnable)
                val buffer = ByteArray(1024)
                while (isRunning && it.isConnected && it.inputStream.available() > 0) {
                    try {
                        val bytes = it.inputStream.read(buffer)
                        val data = String(buffer, 0, bytes)
                        if (data.isNotEmpty()) {
                            if (data == aliveString) {
                                continue
                            }
                            if (onlineSetupStage.value == OnlineSetupStage.Idle) {
                                receivedDataModel = DataModel()
                                setOnlineSetupStage(OnlineSetupStage.Preference)
                                continue
                            }
                            val message = deserializeGameData(data)
                            if (!message.gameState.connectionEstablished) {
                                cancel()
                                break
                            }
                            if (onlineSetupStage.value == OnlineSetupStage.Preference) {
                                receivedDataModel = message
                                setOnlineSetupStage(OnlineSetupStage.Initialised)
                            } else if (onlineSetupStage.value == OnlineSetupStage.Initialised) {
                                if (message.metaData.miniGame.player2Choice.isEmpty()) {
                                    if (connectedDevice.value!!.address >= message.metaData.choices.first { c -> c.id == playerId }.name) {
                                        receivedDataModel = DataModel()
                                    }
                                } else {
                                    receivedDataModel = message
                                    setOnlineSetupStage(OnlineSetupStage.GameStart)
                                }
                            } else if (onlineSetupStage.value == OnlineSetupStage.GameStart) {
                                receivedDataModel = message
                                onDataReceived?.let { it(message) }
                            } else if (onlineSetupStage.value == OnlineSetupStage.NoService) {
                                cancel()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                isRunning = false
            }
        }

        fun sendData(data: String) {
            if (clientSocket?.isConnected == true) {
                val outputStream: OutputStream = clientSocket.outputStream
                outputStream.write(data.toByteArray())
                outputStream.flush()
            }
        }

        fun checkRunning(): Boolean {
            return isRunning
        }

        fun getDevice(): BluetoothDevice {
            return device
        }

        fun getSocket(): BluetoothSocket? {
            return clientSocket
        }

        fun cancel() {
            receivedDataModel = DataModel()
            handler.removeCallbacks(runnable)
            clientSocket?.close()
            _connectedDevice.value = null
            isRunning = false
            setOnlineSetupStage(OnlineSetupStage.Idle)
        }
    }

    fun sendData(dataModel: DataModel) {
        connectedThread?.sendData(serializeGameData(dataModel))
    }

    fun setOnDataReceived(value: ((DataModel) -> Unit)? = null) {
        onDataReceived = value
    }

    fun setOnlineSetupStage(value: OnlineSetupStage) {
        _onlineSetupStage.value = value
        if ((value == OnlineSetupStage.Idle || value == OnlineSetupStage.NoService) &&
            connectedThread?.checkRunning() == true
        ) {
            connectedThread?.cancel()
        }
        getMissingPermissions()
    }

    fun getBtDeviceFromAddress(value: String?): BluetoothDevice? {
        return _devices.value.firstOrNull { it.address == value }
    }

    fun disconnectDevice() {
        connectedThread?.cancel()
        setOnlineSetupStage(OnlineSetupStage.Idle)
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
        return Gson().fromJson(
            json.removeSuffix(aliveString).removePrefix(aliveString),
            DataModel::class.java
        )
    }
}
