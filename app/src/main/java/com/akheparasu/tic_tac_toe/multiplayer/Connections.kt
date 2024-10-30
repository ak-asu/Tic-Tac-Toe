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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.akheparasu.tic_tac_toe.utils.OnlineSetupStage
import com.akheparasu.tic_tac_toe.utils.PLAYER_1
import com.akheparasu.tic_tac_toe.utils.PLAYER_2
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting
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
        if (acceptThread == null && connectedThread == null) {
            acceptThread = AcceptThread()
        }
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
        stopDiscovery()
        unregisterLocReceiver()
        acceptThread?.cancel()
        connectedThread?.cancel()
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
        _isConnecting.value = true
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
            val connectedSocket = try {
                deviceToConnect = null
                connectedThread?.cancel()
                acceptThread?.cancel()
                val bluetoothSocket = try {
                    device.javaClass.getMethod("createRfcommSocket", Int::class.java).invoke(device, 1) as BluetoothSocket
                } catch (e: Exception) {
                    device.createRfcommSocketToServiceRecord(appUUID)
                }
                bluetoothSocket?.connect()
                bluetoothSocket
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            withContext(Dispatchers.Main) {
                _isConnecting.value = false
                if (connectedSocket == null) {
                    Toast.makeText(context, "Could not connect", Toast.LENGTH_SHORT).show()
                    connectedThread?.cancel()
                } else {
                    connectedThread = ConnectedThread(PLAYER_1, connectedSocket)
                    connectedThread?.start()
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
                        if (it.address == _connectedDevice.value?.address) {
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
    private inner class AcceptThread : Thread() {
        @Volatile
        private var isRunning = true

        @Volatile
        private var serverSocket: BluetoothServerSocket? = null

        init {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(appName, appUUID)
            start()
        }

        override fun run() {
            connectedThread?.cancel()
            while (isRunning) {
                val bluetoothSocket = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
                bluetoothSocket?.also {
                    isRunning = false
                    connectedThread = ConnectedThread(PLAYER_2, bluetoothSocket)
                    connectedThread?.start()
                    cancel()
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            acceptThread = null
            interrupt()
        }
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectedThread(val playerId: String, val bluetoothSocket: BluetoothSocket) : Thread() {
//        private val handler = Handler(Looper.getMainLooper())
//        private val runnable = object : Runnable {
//            override fun run() {
//                if (bluetoothSocket?.isConnected == true) {
//                    handler.postDelayed(this, 10000)
//                    sendData(aliveString)
//                } else {
//                    handler.removeCallbacks(this)
//                }
//            }
//        }

        @Volatile
        private var isRunning = true

        override fun run() {
            _connectedDevice.value = bluetoothSocket.remoteDevice
            //handler.post(runnable)
            val buffer = ByteArray(1024)
            receivedDataModel = DataModel(
                metaData = MetaData(
                    choices = listOf(
                        PlayerChoice(
                            id = PLAYER_1,
                            name = if (playerId == PLAYER_2) {
                                _connectedDevice.value!!.address
                            } else {
                                ""
                            }
                        ),
                        PlayerChoice(
                            id = PLAYER_2,
                            name = if (playerId == PLAYER_1) {
                                _connectedDevice.value!!.address
                            } else {
                                ""
                            }
                        )
                    )
                )
            )
            sendData(receivedDataModel)
            while (isRunning) {
                try {
                    val bytes = bluetoothSocket!!.inputStream.read(buffer)
                    val data = String(buffer, 0, bytes)
                    if (data.isNotEmpty()) {
                        if (data == aliveString) {
                            continue
                        }
                        val message = deserializeGameData(data)
                        if (!message.gameState.connectionEstablished) {
                            cancel()
                            break
                        }
                        if (onlineSetupStage.value == OnlineSetupStage.Idle) {
                            receivedDataModel =
                                message.copy(
                                    metaData = MetaData(
                                        choices = listOf(
                                            PlayerChoice(
                                                id = PLAYER_1,
                                                name = if (playerId == PLAYER_2) {
                                                    _connectedDevice.value!!.address
                                                } else {
                                                    message.metaData.choices.first().name.ifEmpty { receivedDataModel.metaData.choices.first().name }
                                                }
                                            ),
                                            PlayerChoice(
                                                id = PLAYER_2,
                                                name = if (playerId == PLAYER_1) {
                                                    _connectedDevice.value!!.address
                                                } else {
                                                    message.metaData.choices.last().name.ifEmpty { receivedDataModel.metaData.choices.last().name }
                                                }
                                            )
                                        )
                                    )
                                )
                            setOnlineSetupStage(OnlineSetupStage.Preference)
                        } else if (onlineSetupStage.value == OnlineSetupStage.Preference) {
                            receivedDataModel = message
                            setOnlineSetupStage(OnlineSetupStage.Initialised)
                        } else if (onlineSetupStage.value == OnlineSetupStage.Initialised) {
                            if ((playerId == PLAYER_1 && message.metaData.miniGame.player2Choice.isNotEmpty()) ||
                                (playerId == PLAYER_2 && message.metaData.miniGame.player1Choice.isNotEmpty())
                            ) {
                                if (connectedDevice.value!!.address >= message.metaData.choices.first { c -> c.id == playerId }.name) {
                                    receivedDataModel = message
                                }
                            } else {
                                receivedDataModel = message
                                setOnlineSetupStage(OnlineSetupStage.GameStart)
                            }
                        } else if (onlineSetupStage.value == OnlineSetupStage.GameStart) {
                            onDataReceived?.let { it(message) }
                        } else {
                            cancel()
                        }
                    }
                } catch (_: Exception) {
                    //e.printStackTrace()
                }
            }
            cancel()
        }

        fun sendData(data: String) {
            if (bluetoothSocket?.isConnected == true) {
                val outputStream: OutputStream = bluetoothSocket!!.outputStream
                outputStream.write(data.toByteArray())
                outputStream.flush()
            }
        }

        fun cancel() {
            receivedDataModel = DataModel()
            //handler.removeCallbacks(runnable)
            bluetoothSocket.close()
            _connectedDevice.value = null
            isRunning = false
            connectedThread = null
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
        if (value == OnlineSetupStage.Preference && _onlineSetupStage.value != OnlineSetupStage.Idle) {
            return
        }
        _onlineSetupStage.value = value
        if ((value == OnlineSetupStage.Idle || value == OnlineSetupStage.NoService) &&
            connectedThread != null
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

    fun getPlayerId(): String? {
        return connectedThread?.playerId
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
        val a = json.removeSuffix(aliveString).removePrefix(aliveString)
        Log.e("tyty", a)
        return Gson().fromJson(
            a,
            DataModel::class.java
        )
    }
}
