package com.ramk.bluetoothbytedata.bluetooth

import android.bluetooth.BluetoothDevice

interface BluetoothListener {

    fun onConnected(bluetoothDevice :BluetoothDevice)
    fun onConnectionFailed(msg :String?)
    fun onDataReceived(data :String)

}