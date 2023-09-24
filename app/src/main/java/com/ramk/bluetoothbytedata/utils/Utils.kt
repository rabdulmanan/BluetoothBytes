package com.ramk.bluetoothbytedata.utils

import android.bluetooth.BluetoothAdapter

import android.content.Context
import android.content.SharedPreferences

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ramk.bluetoothbytedata.R



object Utils {

    val NUMBER_OF_BYTES = 30
    val SELECTED_BLUETOOTH_DEVICE_MAC = "selected_bluetooth_mac"
    val SELECTED_BLUETOOTH_DEVICE_NAME = "selected_bluetooth_name"
    val BYTES_DATA = "bytes_data"

    var selectedDeviceMac: String? = null

    var bluetoothAdapter: BluetoothAdapter? = null


    fun Context.getAppSharedPrefs(): SharedPreferences{
        return getSharedPreferences(getString(R.string.app_name),
            AppCompatActivity.MODE_PRIVATE)
    }

    fun Context.showMessage(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }





}