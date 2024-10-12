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

class TwoPlayer(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothReceiver: BroadcastReceiver? = null
    var deviceList: MutableList<BluetoothDevice> = mutableListOf()

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
                    if (device != null && !deviceList.contains(device)) {
                        deviceList.add(device)
                    }
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

}