package com.ramk.bluetoothbytedata.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import com.ramk.bluetoothbytedata.R
import com.ramk.bluetoothbytedata.utils.Utils.bluetoothAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


object BluetoothConnectionManager {

    private val TAG = BluetoothConnectionManager::class.java.name
    // Inside your Activity or Fragment
    private var connectedDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    private var mIsBluetoothConnection = false

    private var mInputStream : InputStream? = null
    private var mOutputStream : OutputStream? = null

    private var bluetoothListener : BluetoothListener? = null
    private var isConnecting = false

    private var isAcceptSocketStarted = false

    fun setNewDataReceivedListener(listener : BluetoothListener?){
        bluetoothListener = listener

    }

    fun startAcceptingListeningConnection(appName: String){
        if(!isAcceptSocketStarted){
            isAcceptSocketStarted = true
            AcceptThread(appName,{ socket, device ->
                bluetoothSocket = socket
                setupConntectedDevice(device)

            }).start()
        }
    }

    fun connectToDevice(bluetoothDevice: BluetoothDevice){

        CoroutineScope(Dispatchers.IO).launch {

            try {

                if(!isConnecting) {
                    mIsBluetoothConnection = false
                    connectedDevice = null
                    isConnecting = true
                    if(bluetoothSocket !=null && bluetoothSocket!!.isConnected){
                        closeBluetoothConnection()
                    }
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID)


                    bluetoothSocket?.connect()

                    setupConntectedDevice(bluetoothDevice)

                }


            }catch (ex: Exception){
                isConnecting = false
                ex.printStackTrace()
                bluetoothListener?.onConnectionFailed(ex.message)
            }

        }


    }
    private fun setupConntectedDevice(bluetoothDevice: BluetoothDevice){
        setupDataCommunication()

        mIsBluetoothConnection = true
        connectedDevice = bluetoothDevice

        startListeningBluetoothData()
        bluetoothListener?.onConnected(bluetoothDevice)

        isConnecting = false
    }

    private fun setupDataCommunication() {
        try {
            mInputStream = bluetoothSocket?.inputStream
            mOutputStream = bluetoothSocket?.outputStream
            // Now you can use inputStream.read() and outputStream.write() to send and receive data
        } catch (e: IOException) {
            // Handle IOException
            e.printStackTrace()
        }
    }

    fun closeBluetoothConnection() {
        try {

            mIsBluetoothConnection = false
            connectedDevice = null

            if (mInputStream != null) {
                mInputStream?.close()
            }
            if (mOutputStream != null) {
                mOutputStream?.close()
            }
            if (bluetoothSocket != null) {
                bluetoothSocket?.close()
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendDataToDevice(byteArray: ByteArray){

        if(mOutputStream !=null){

            mOutputStream?.write(byteArray)
            mOutputStream?.flush()

            Log.d(TAG,"Data sent")

        }

    }


    private fun startListeningBluetoothData(){

        Thread{

            while (mIsBluetoothConnection){

                try{

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val byteArray = mInputStream?.readAllBytes()

                        if(byteArray != null)
                            convertByteArrayToString(byteArray)

                    } else {
                        val byteArray = ByteArray(2048)
                        mInputStream?.read(byteArray)

                        convertByteArrayToString(byteArray)

                    }


                }catch (ex: Exception){
                    ex.printStackTrace()
                }

            }

        }.start()

    }

    private fun convertByteArrayToString(byteArray: ByteArray){
        val data = String(byteArray)

        bluetoothListener?.onDataReceived(data)

    }

    fun hasBluetoothConnection():Boolean{
        return  mIsBluetoothConnection
    }

    private class AcceptThread(private val appName: String,
                               private val onDeviceConnected : (socket: BluetoothSocket,
                                                                device: BluetoothDevice) -> Unit) : Thread() {
        private val TAG = AcceptThread::class.java.name
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        override fun run() {
            Log.d(
                TAG,
                "BEGIN mAcceptThread$this"
            )
            name = "AcceptThread"
            var socket: BluetoothSocket? = null

            // Listen to the server socket if we're not connected
            while (!mIsBluetoothConnection || connectedDevice==null) {
                socket = try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "accept() failed", e)
                    connectedDevice = null
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    onDeviceConnected(socket, socket.remoteDevice)

                }
            }
            Log.i(
                TAG,
                "END mAcceptThread"
            )
        }

        fun cancel() {
            Log.d(
                TAG,
                "cancel $this"
            )
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                Log.e(
                    TAG,
                    "close() of server failed",
                    e
                )
            }
            connectedDevice = null
        }

        init {
            var tmp: BluetoothServerSocket? = null

            // Create a new listening server socket
            try {
                if(bluetoothAdapter!=null) {
                    tmp = bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(
                        appName,
                        MY_UUID
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "listen() failed", e)
            }
            mmServerSocket = tmp
        }
    }

}