package com.ramk.bluetoothbytedata

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.ramk.bluetoothbytedata.BluetoothDeviceScanActivity.Companion.EXTRA_PARAM_CHANGE_BLUETOOTH
import com.ramk.bluetoothbytedata.adapters.MyBytesAdapter
import com.ramk.bluetoothbytedata.bluetooth.BluetoothConnectionManager
import com.ramk.bluetoothbytedata.databinding.ActivityMainBinding
import com.ramk.bluetoothbytedata.databinding.InputLayoutBinding
import com.ramk.bluetoothbytedata.models.MyByte
import com.ramk.bluetoothbytedata.utils.Utils.BYTES_DATA
import com.ramk.bluetoothbytedata.utils.Utils.FILE_STORAGE_DIR
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
import java.io.IOException
import java.io.OutputStreamWriter


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

        mAdapter = MyBytesAdapter(emptyList(),object: MyBytesAdapter.ItemScrollListener {
            override fun needToScroll(position: Int, offset: Int) {
                val layoutManager = mBinding.bytesListView.layoutManager as LinearLayoutManager
                layoutManager.scrollToPositionWithOffset(position,offset)
            }

        })

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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            val mIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            mIntent.addCategory(Intent.CATEGORY_OPENABLE)
            mIntent.type = "application/json"
            if (contentResolver.persistedUriPermissions.any()) {
                val uri = contentResolver.persistedUriPermissions[0].uri
                mIntent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)

            }
            filePickRequestLauncher.launch(mIntent)
        }
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {

            permissionGrantFor = StoragePermissionGrantFor.LOAD_FILE
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)


        } else {
            val mIntent = Intent(Intent.ACTION_GET_CONTENT)
            mIntent.setType("application/json")
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

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            if (contentResolver.persistedUriPermissions.isEmpty()) {

                okayDialog("Storage permission","Please select directory where you want to save your data in file."){
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    permissionGrantFor = StoragePermissionGrantFor.SAVE_FILE
                    selectFileStorageLauncher.launch(intent)
                }

            }else{
                saveFileNameDialog { fileName ->

                    CoroutineScope(Dispatchers.IO).launch {
                        val uri = contentResolver.persistedUriPermissions[0].uri
                        try {
                            val directory = DocumentFile.fromTreeUri(this@MainActivity, uri)

                            if(directory!=null) {
                                val file = directory.createFile("application/json",
                                    "$fileName.json"
                                )
                                val pfd: ParcelFileDescriptor? =
                                    contentResolver.openFileDescriptor(file!!.uri, "w")
                                if(pfd!=null) {
                                    val jsonArray = JSONArray()

                                    list.forEach {
                                        val jsonObj = it.toJSON()

                                        jsonArray.put(jsonObj)
                                    }

                                    val str = jsonArray.toString()

                                    val fos = FileOutputStream(pfd.fileDescriptor)
                                    fos.write(str.toByteArray())
                                    fos.close()
                                    withContext(Dispatchers.Main) {
                                        showMessage("Data save successfully ${file.name}")
                                    }
                                }else{
                                    withContext(Dispatchers.Main) {
                                        showMessage("Failed to create file $fileName")
                                    }
                                }
                            }else{
                                withContext(Dispatchers.Main) {
                                    showMessage("${uri.lastPathSegment} directory not found")
                                }
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
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
        else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

    private val filePickRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result?.let {
                val uri = result.data?.data

                if (uri != null) {
                    readJsonFromUriAndLoadData(uri)
                }
                Log.d(TAG, "File you picked: $uri")

            }
        }


    private val selectFileStorageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()
        ) { result ->
            result?.let {
                val uri = result.data?.data

                if (uri != null) {
                    val takeFlags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    // take persistable Uri Permission for future use
                    contentResolver
                        .takePersistableUriPermission(uri, takeFlags)

                    val prefs = getAppSharedPrefs()
                    prefs.edit().putString(FILE_STORAGE_DIR,uri.toString()).commit()

                    val myByteList = mAdapter.getData()
                    saveAllBytesInExternalStorage(myByteList)

                }
                Log.d(TAG, "File you picked: $uri")

            }
        }

    enum class StoragePermissionGrantFor {
        NONE,
        LOAD_FILE,
        SAVE_FILE
    }
}