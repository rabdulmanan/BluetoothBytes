package com.ramk.bluetoothbytedata

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ramk.bluetoothbytedata.BluetoothDeviceScanActivity.Companion.EXTRA_PARAM_CHANGE_BLUETOOTH
import com.ramk.bluetoothbytedata.adapters.MyBytesAdapter
import com.ramk.bluetoothbytedata.bluetooth.BluetoothConnectionManager
import com.ramk.bluetoothbytedata.databinding.ActivityMainBinding
import com.ramk.bluetoothbytedata.models.MyByte
import com.ramk.bluetoothbytedata.utils.Utils.BYTES_DATA
import com.ramk.bluetoothbytedata.utils.Utils.NUMBER_OF_BYTES
import com.ramk.bluetoothbytedata.utils.Utils.getAppSharedPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name
    private lateinit var prefs: SharedPreferences
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mAdapter : MyBytesAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        prefs = getAppSharedPrefs()

        mAdapter = MyBytesAdapter(emptyList())

        mBinding.bytesListView.adapter = mAdapter
        mBinding.sendBtn.setOnClickListener {

            val myByteList = mAdapter.getData()
            sendMyDataList(myByteList)
            saveList(myByteList)


        }

        mBinding.bluetoothBtn.setOnClickListener {
            val mIntent = Intent(this,BluetoothDeviceScanActivity::class.java)
            mIntent.putExtra(EXTRA_PARAM_CHANGE_BLUETOOTH, true)
            startActivity(mIntent)
        }

        mBinding.resetBtn.setOnClickListener {
            confirmResetAll()
        }


    }

    private fun confirmResetAll(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Reset All")
        builder.setMessage("Do you want to really want to reset all(30) bytes?")

        builder.setPositiveButton("Yes",
            DialogInterface.OnClickListener { dialog, which ->
                // Handle the "Yes" button click
                dialog.dismiss() // Close the dialog
                // Add your code for the "Yes" action here

                resetAllBytes()

            })

        builder.setNegativeButton("No",
            DialogInterface.OnClickListener { dialog, which ->
                // Handle the "No" button click
                dialog.dismiss() // Close the dialog
                // Add your code for the "No" action here
            })

        // Create and show the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
    private fun resetAllBytes(){

        val myBytesList = ArrayList<MyByte>()
        for (i in 1..NUMBER_OF_BYTES) {
            myBytesList.add(MyByte())
        }
        mAdapter.setData(myBytesList)
        saveList(myBytesList)

    }

    override fun onResume() {
        super.onResume()

        initializeBytes()


        mBinding.sendBtn.isEnabled = BluetoothConnectionManager.hasBluetoothConnection()
        if(BluetoothConnectionManager.hasBluetoothConnection()){
            mBinding.sendBtn.text = "Send"
        }else{
            mBinding.sendBtn.text = "Disconnected"
        }

    }

    private fun initializeBytes(){

        val savedData = prefs.getString(BYTES_DATA, null)
        val myBytesList = ArrayList<MyByte>()
        if(savedData == null){
            for (i in 1..NUMBER_OF_BYTES) {
                myBytesList.add(MyByte())
            }
        }else{
            val jsonArray = JSONArray(savedData)
            for (i in 0 until jsonArray.length()) {
                val data = jsonArray.getJSONObject(i)

                val myByte = MyByte()
                myByte.fromJSONToObject(data)
                myBytesList.add(myByte)
            }

        }

        mAdapter.setData(myBytesList)
    }

    private fun saveList(list: List<MyByte>){

        CoroutineScope(Dispatchers.IO).launch {
            val jsonArray = JSONArray()

            list.forEach {
                val jsonObj = it.toJSON()

                jsonArray.put(jsonObj)
            }

            val str = jsonArray.toString()
            Log.d(TAG,"Saving data : $str")
            val editor = prefs.edit()
            editor.putString(BYTES_DATA,str)
            editor.apply()
        }


    }

    private fun sendMyDataList(list: List<MyByte>){

        CoroutineScope(Dispatchers.IO).launch {

            val byteArray = ByteArray(NUMBER_OF_BYTES)
            var index = 0
            list.forEach {
                val byte = it.getBytes()
                byteArray[index] = byte
                index++
            }
            Log.d(TAG,"Sending bytes : $byteArray")
            BluetoothConnectionManager.sendDataToDevice(byteArray)

        }


    }


}