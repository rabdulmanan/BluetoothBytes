package com.ramk.bluetoothbytedata

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import com.ramk.bluetoothbytedata.adapters.NearbyDevicesAdapter
import com.ramk.bluetoothbytedata.bluetooth.BluetoothConnectionManager
import com.ramk.bluetoothbytedata.bluetooth.BluetoothListener
import com.ramk.bluetoothbytedata.databinding.ActivityBlutoothDeviceScanBinding
import com.ramk.bluetoothbytedata.utils.Utils.SELECTED_BLUETOOTH_DEVICE_MAC
import com.ramk.bluetoothbytedata.utils.Utils.SELECTED_BLUETOOTH_DEVICE_NAME
import com.ramk.bluetoothbytedata.utils.Utils.bluetoothAdapter
import com.ramk.bluetoothbytedata.utils.Utils.getAppSharedPrefs
import com.ramk.bluetoothbytedata.utils.Utils.selectedDeviceMac
import com.ramk.bluetoothbytedata.utils.Utils.showMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BluetoothDeviceScanActivity : AppCompatActivity(),
    NearbyDevicesAdapter.NearbyDeviceClickListener, BluetoothListener {

    companion object{
        val EXTRA_PARAM_CHANGE_BLUETOOTH = "change_bluetooth"
    }
    private val TAG = BluetoothDeviceScanActivity::class.java.name

    private val permissionsAppNeeded = arrayListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var prefs: SharedPreferences
    private lateinit var mBinding : ActivityBlutoothDeviceScanBinding

    private var scanningDevices = false
    private val nearByDevices = ArrayList<BluetoothDevice>()

    private var mAdapter : NearbyDevicesAdapter? = null

    private var isLauncher = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityBlutoothDeviceScanBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        prefs = getAppSharedPrefs()

        if(intent.hasExtra(EXTRA_PARAM_CHANGE_BLUETOOTH)){

            isLauncher = !intent.getBooleanExtra(EXTRA_PARAM_CHANGE_BLUETOOTH,false)

        }else{
            isLauncher = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsAppNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissionsAppNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        checkAndGrantPermissions()

        mBinding.scanBtn.setOnClickListener {
            startDeviceScanning()
        }



    }

    override fun onResume() {
        super.onResume()

        BluetoothConnectionManager.setNewDataReceivedListener(this)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(discoverNearByDevices, filter)
    }

    override fun onPause() {
        super.onPause()
        try {
            if(bluetoothAdapter!=null && bluetoothAdapter!!.isDiscovering) {
                bluetoothAdapter?.cancelDiscovery()
            }
        }catch (ex: Exception){
            ex.printStackTrace()
        }
        unregisterReceiver(discoverNearByDevices)
    }

    private fun getNotGrantedPermissions():Array<String>{

        val notGrantedPermissions = ArrayList<String>()

        permissionsAppNeeded.forEach {

            if(ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_DENIED){
                notGrantedPermissions.add(it)
            }

        }

        return notGrantedPermissions.toTypedArray()
    }

    private fun checkAndGrantPermissions(){

        val notGrantedPermission = getNotGrantedPermissions()

        if(notGrantedPermission.any()){
            requestPermissionLauncher.launch(notGrantedPermission)
        }else{
            permissionAreOkProceed()
        }
    }

    override fun onNearbyDeviceClicked(device: BluetoothDevice) {
        bluetoothAdapter?.cancelDiscovery()
        BluetoothConnectionManager.connectToDevice(device)
    }

    private fun permissionAreOkProceed(){

        if(enableBluetooth()) {
            BluetoothConnectionManager.startAcceptingListeningConnection(getString(R.string.app_name))
            mAdapter = NearbyDevicesAdapter(nearByDevices,this)
            mBinding.nearByDeviceListView.adapter = mAdapter
            if (!isAnyBluetoothDeviceSelected()) {
                //start bluetooth scanning....
                scanNearbyUsingScanLeMethod()
                startDeviceScanning()

            } else {

                if(!BluetoothConnectionManager.hasBluetoothConnection()) {
                    //create connection to selected bluetooth device
                    val device = bluetoothAdapter?.getRemoteDevice(selectedDeviceMac)
                    if (device != null) {
                        BluetoothConnectionManager.connectToDevice(device)
                    }
                }else if(isLauncher){
                    showDeviceData()
                }

                if(!isLauncher){
                    scanNearbyUsingScanLeMethod()
                    startDeviceScanning()
                }
            }
        }

    }


    private fun enableBluetooth():Boolean{
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if(bluetoothManager == null){
            showMessage(getString(R.string.bluetooth_not_supported))
            finish()
            return false
        }

        bluetoothAdapter = bluetoothManager.adapter

        if(bluetoothAdapter == null){
            showMessage(getString(R.string.bluetooth_not_supported))
            finish()
            return false
        }


        if(!bluetoothAdapter!!.isEnabled){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            launchEnableBluetooth.launch(enableBtIntent)
            return false
        }

        return true

    }

    private fun isAnyBluetoothDeviceSelected():Boolean{

        selectedDeviceMac = prefs.getString(SELECTED_BLUETOOTH_DEVICE_MAC,null)
        return selectedDeviceMac != null

    }

    private fun scanNearbyUsingScanLeMethod(){
        if(bluetoothAdapter != null) {
            val bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(scanCallback)

                Handler(Looper.getMainLooper()).postDelayed({

                    bluetoothLeScanner.stopScan(scanCallback)
                    scanningFinished()
                },10000)
            }
        }
    }

    private fun startDeviceScanning(){
        scanningDevices = true
        nearByDevices.clear()
        bluetoothAdapter?.bondedDevices?.forEach {
            addNearByDeviceInList(it)
        }

        mBinding.scanBtn.visibility = View.GONE
        mBinding.progressLayout.visibility = View.VISIBLE
        bluetoothAdapter?.startDiscovery()
    }
    private fun scanningFinished(){
        scanningDevices = false

        mBinding.scanBtn.visibility = View.VISIBLE
        mBinding.progressLayout.visibility = View.GONE

        mAdapter?.setNearByDevices(nearByDevices)
    }



    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
        ) {

            checkAndGrantPermissions()

        }


    private fun addNearByDeviceInList(device: BluetoothDevice){

        val foundedDevice = nearByDevices.find { nbd-> nbd.address.equals(device.address) }
        if(foundedDevice == null){
            nearByDevices.add(device)
        }

    }


    override fun onConnected(bluetoothDevice: BluetoothDevice) {

        CoroutineScope(Dispatchers.Main).launch {

            showMessage("${bluetoothDevice.name} connected")

            val deviceName = bluetoothDevice.name
            val deviceMac = bluetoothDevice.address

            val editor = prefs.edit()
            editor.putString(SELECTED_BLUETOOTH_DEVICE_NAME,deviceName)
            editor.putString(SELECTED_BLUETOOTH_DEVICE_MAC, deviceMac)
            editor.apply()

            showDeviceData()

        }


    }

    private fun showDeviceData(){

        CoroutineScope(Dispatchers.Main).launch {
            showMessage("Connected")
            startActivity(Intent(this@BluetoothDeviceScanActivity, MainActivity::class.java))
            finish()
        }

    }

    override fun onConnectionFailed(msg: String?) {

        CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG,"Bluetooth connection failed: $msg")

            showMessage("Bluetooth connection failed: $msg")
        }

    }

    override fun onDataReceived(data: String) {
        Log.d(TAG,"New data received: $data")
    }

    val launchEnableBluetooth = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            //  you will get result here in result.data
            permissionAreOkProceed()
        }
    }

    private val discoverNearByDevices = object : BroadcastReceiver(){

        override fun onReceive(p0: Context?, p1: Intent?) {
            val action = p1?.action
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // A new Bluetooth device has been discovered
                // A new Bluetooth device has been discovered
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if(device != null) {
                    addNearByDeviceInList(device)
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                scanningFinished()
            }

        }

    }


    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            // A BLE device has been found
            val device: BluetoothDevice = result.getDevice()
            addNearByDeviceInList(device)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            // A batch of scan results is available
            for (result in results) {
                val device: BluetoothDevice = result.getDevice()
                addNearByDeviceInList(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
            // Error codes are defined in the ScanCallback documentation
            Log.d(TAG,"Error in scanning nearby devices: $errorCode")
        }
    }

}