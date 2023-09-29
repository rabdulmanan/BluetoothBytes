package com.ramk.bluetoothbytedata

import android.Manifest
import android.app.Instrumentation.ActivityResult
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ramk.bluetoothbytedata.BluetoothDeviceScanActivity.Companion.EXTRA_PARAM_CHANGE_BLUETOOTH
import com.ramk.bluetoothbytedata.adapters.MyBytesAdapter
import com.ramk.bluetoothbytedata.bluetooth.BluetoothConnectionManager
import com.ramk.bluetoothbytedata.databinding.ActivityMainBinding
import com.ramk.bluetoothbytedata.databinding.InputLayoutBinding
import com.ramk.bluetoothbytedata.models.MyByte
import com.ramk.bluetoothbytedata.utils.Utils.BYTES_DATA
import com.ramk.bluetoothbytedata.utils.Utils.NUMBER_OF_BYTES
import com.ramk.bluetoothbytedata.utils.Utils.TEST_VALUE
import com.ramk.bluetoothbytedata.utils.Utils.getAppSharedPrefs
import com.ramk.bluetoothbytedata.utils.Utils.showMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.name
    private lateinit var prefs: SharedPreferences
    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mAdapter: MyBytesAdapter

    private var permissionGrantFor = StoragePermissionGrantFor.NONE

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
            val mIntent = Intent(this, BluetoothDeviceScanActivity::class.java)
            mIntent.putExtra(EXTRA_PARAM_CHANGE_BLUETOOTH, true)
            startActivity(mIntent)
        }

        mBinding.resetBtn.setOnClickListener {
            yesNoDialog("Reset All", "Do you want to really want to reset all(30) bytes?") {
                resetAllBytes()
            }
        }

        mBinding.testBtn.setOnClickListener {

            val data = TEST_VALUE.toByteArray()
            BluetoothConnectionManager.sendDataToDevice(data)

        }

        mBinding.saveBtn.setOnClickListener {
            yesNoDialog(
                "Save",
                "Do you really want to save your all(30) bytes in external storage(Download Directory)?"
            ) {
                val myByteList = mAdapter.getData()
                saveAllBytesInExternalStorage(myByteList)
            }
        }

        mBinding.loadFileBtn.setOnClickListener {
            loadFileFromStorage()
        }


    }

    private fun yesNoDialog(title: String, msg: String, onYesButtonClicked: () -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(msg)

        builder.setPositiveButton("Yes",
            DialogInterface.OnClickListener { dialog, which ->
                // Handle the "Yes" button click
                dialog.dismiss() // Close the dialog
                // Add your code for the "Yes" action here
                onYesButtonClicked()


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

    private fun okayDialog(title: String, msg: String, onYesButtonClicked: () -> Unit) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(msg)

        builder.setPositiveButton("Ok",
            DialogInterface.OnClickListener { dialog, which ->
                // Handle the "Yes" button click
                dialog.dismiss() // Close the dialog
                // Add your code for the "Yes" action here
                onYesButtonClicked()


            })


        // Create and show the AlertDialog
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun resetAllBytes() {

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
        if (BluetoothConnectionManager.hasBluetoothConnection()) {
            mBinding.sendBtn.text = "Send"
        } else {
            mBinding.sendBtn.text = "Disconnected"
        }

        mBinding.testBtn.isEnabled = BluetoothConnectionManager.hasBluetoothConnection()

    }

    private fun initializeBytes() {

        val savedData = prefs.getString(BYTES_DATA, null)
        val myBytesList = ArrayList<MyByte>()
        if (savedData == null) {
            for (i in 1..NUMBER_OF_BYTES) {
                myBytesList.add(MyByte())
            }
        } else {
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

    private fun saveList(list: List<MyByte>) {

        CoroutineScope(Dispatchers.IO).launch {
            val jsonArray = JSONArray()

            list.forEach {
                val jsonObj = it.toJSON()

                jsonArray.put(jsonObj)
            }

            val str = jsonArray.toString()
            Log.d(TAG, "Saving data : $str")
            val editor = prefs.edit()
            editor.putString(BYTES_DATA, str)
            editor.apply()
        }


    }

    private fun loadFileFromStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

            permissionGrantFor = StoragePermissionGrantFor.LOAD_FILE
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)


        } else {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.setType("application/json");
            filePickRequestLauncher.launch(mIntent)
        }
    }


    private fun saveFileNameDialog(onFileNameSaved: (fileName: String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        val mInputLayoutBinding = InputLayoutBinding.inflate(LayoutInflater.from(this))

        val layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParams.marginEnd = 10
        layoutParams.marginStart = 10
        mInputLayoutBinding.root.layoutParams = layoutParams
        builder.setTitle("File Name")
            .setView(mInputLayoutBinding.root)
            .setPositiveButton("Save") { dialogInterface: DialogInterface, _: Int ->
                try {
                    val fileName = mInputLayoutBinding.fileNameTextFiled.editText?.text?.toString()
                    val regexPattern = "^[a-zA-Z0-9-_]+\$"
                    if (fileName != null && fileName.length > 0 && fileName.contains(
                            Regex(
                                regexPattern
                            )
                        )
                    ) {
                        onFileNameSaved(fileName)
                        dialogInterface.dismiss()
                    } else {
                        showMessage("Please enter valid file name")
                        saveFileNameDialog(onFileNameSaved)
                    }
                }catch (ex: Exception){
                    okayDialog("Error","${ex.message}"){

                    }
                    ex.printStackTrace()
                }

            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    private fun saveAllBytesInExternalStorage(list: List<MyByte>) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

            permissionGrantFor = StoragePermissionGrantFor.SAVE_FILE

            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        } else {

            saveFileNameDialog { fileName ->

                CoroutineScope(Dispatchers.IO).launch {


                    try {
                        val jsonArray = JSONArray()

                        list.forEach {
                            val jsonObj = it.toJSON()

                            jsonArray.put(jsonObj)
                        }

                        val str = jsonArray.toString()
                        Log.d(TAG, "Saving data : $str")


                        val downloadDirectory = Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

                        // Create a new file in the Download directory
                        val file = File(downloadDirectory, "$fileName.json")

                        if (!file.exists()) {
                            file.createNewFile()
                        }
                        // Write JSON data to the file
                        val outputStream = FileOutputStream(file)
                        val outputStreamWriter = OutputStreamWriter(outputStream)
                        outputStreamWriter.write(str)
                        outputStreamWriter.close()

                        withContext(Dispatchers.Main) {
                            showMessage("Data save successfully ${file.name}")
                        }
                        // File has been successfully written
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Handle the exception here
                        withContext(Dispatchers.Main) {
                            okayDialog("Error","${e.message}"){

                            }
                            showMessage("Failed to save file, please check file name or storage permission")
                        }
                    }

                }

            }


        }
    }

    private fun sendMyDataList(list: List<MyByte>) {

        CoroutineScope(Dispatchers.IO).launch {

            val byteArray = ByteArray(NUMBER_OF_BYTES)
            var index = 0
            list.forEach {
                val byte = it.getBytes()
                byteArray[index] = byte
                index++
            }
            Log.d(TAG, "Sending bytes : $byteArray")
            BluetoothConnectionManager.sendDataToDevice(byteArray)

        }


    }

    private fun readJsonFromUriAndLoadData(uri: Uri) {

        CoroutineScope(Dispatchers.IO).launch {

            contentResolver.openInputStream(uri)?.bufferedReader().use { reader ->
                val stringBuilder = StringBuilder()
                var data = reader?.readLine()
                while (data != null) {
                    stringBuilder.append(data)
                    data = reader?.readLine()
                }

                try {
                    val myBytesList = ArrayList<MyByte>()
                    val savedData = stringBuilder.toString()
                    val jsonArray = JSONArray(savedData)
                    for (i in 0 until jsonArray.length()) {
                        val jsonData = jsonArray.getJSONObject(i)

                        val myByte = MyByte()
                        myByte.fromJSONToObject(jsonData)
                        myBytesList.add(myByte)
                    }

                    withContext(Dispatchers.Main) {
                        mAdapter.setData(myBytesList)
                        showMessage("Data populated successfully")
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    withContext(Dispatchers.Main) {
                        yesNoDialog(
                            "Error",
                            "Error occurred while reading data, make sure you are selecting the correct file."
                        ) {

                        }
                    }
                }

            }

        }


    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {

        when (permissionGrantFor) {
            StoragePermissionGrantFor.LOAD_FILE -> {
                loadFileFromStorage()
            }

            StoragePermissionGrantFor.SAVE_FILE -> {
                val myByteList = mAdapter.getData()
                saveAllBytesInExternalStorage(myByteList)
            }

            else -> {

            }
        }
    }

    val filePickRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
            { result ->
                result?.let {
                    val uri = result.data?.data

                    if (uri != null) {
                        readJsonFromUriAndLoadData(uri)
                    }
                    Log.d(TAG, "File you picked: $uri")

                }
            })

    enum class StoragePermissionGrantFor {
        NONE,
        LOAD_FILE,
        SAVE_FILE
    }
}