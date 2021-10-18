package com.example.blescanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import java.util.*
import java.util.jar.Manifest

private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)

class MainActivity : AppCompatActivity() {

    /*******************************************
     * Bluetooth adapter and Ble scanner set up
     *******************************************/
    // name of the bluetooth adapter for the program
    private val adapter = BluetoothAdapter.getDefaultAdapter()

    // name of the BLE scanner for the program
    private val bleScanner = adapter.bluetoothLeScanner

    // where the BLE scan results will be stored.
    private val scanResults = ArrayList<ScanResult>()

    //gives our list of scan results to the adapter to parse out as necessary
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults)
    }

    // sets the settings of our scan we use low latency because scans will only be a few seconds
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    // set the initial isScanning status and handles changing text of scan button
    private var isScanning = false
        set(value) {

            field = value
            runOnUiThread { btnScan.text = if (value) "Stop Scan" else "Start Scan" }
        }


    /*******************************************
     * Override functions
     *******************************************/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // handles the bluetooth on button being pressed
        btnOn.setOnClickListener { setBluetoothOn() }

        // handles the bluetooth off button being pressed
        btnOff.setOnClickListener { setBluetoothOff() }

        // handles the scan button being pressed
        btnScan.setOnClickListener {

            // if we were scanning when scan button was pressed we stop scanning
            if (isScanning) {
                stopBleScan()
                Toast.makeText(this, "Stopped Scanning", Toast.LENGTH_SHORT).show()
            }

            // if we were not scanning when scan button was pressed we start scanning
            else {
                startBleScan()
                Toast.makeText(this, "Scanning Started", Toast.LENGTH_SHORT).show()

            }
        }

        setupRecyclerView()
    }

    @SuppressLint("MissingSuperCall")

    // checks if permissions are enabled and asks user to give the permission if not already enabled.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        fun innerCheck(name: String) {

            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "$name permission refused", Toast.LENGTH_SHORT)
                    .show()
            }

            else {
                Toast.makeText(applicationContext, "$name permission granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> innerCheck("location")
        }
    }

    /*******************************************
     * Private Functions
     *******************************************/
    // Turns on bluetooth if it is not already turned on
    private fun setBluetoothOn(){

        if(adapter.isEnabled){
            // bluetooth is already enabled notify user if they press ON button
            Toast.makeText(this ,"Bluetooth Already ON", Toast.LENGTH_LONG).show()
        }

        else{
            // requests permission for bluetooth and turns it on if not already on
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST_CODE)

            //display message that bluetooth is turned on
            Toast.makeText(this ,"Bluetooth Turned ON", Toast.LENGTH_LONG).show()
        }
    }

    private fun setBluetoothOff(){
        if(!adapter.isEnabled){

            // bluetooth is already turned off notify user
            Toast.makeText(this ,"Bluetooth Already OFF", Toast.LENGTH_LONG).show()
        }

        // turn bluetooth off
        else{

            adapter.disable()

            // sets bluetooth icon to off and notifies user
            Toast.makeText(this ,"Bluetooth Turned OFF", Toast.LENGTH_LONG).show()
        }
    }


    // scans for devices
    private fun startBleScan(){

        isScanning = true

        // checks for location permissions because they are necessary for BLE functionality
        checkForPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, "Location",  LOCATION_PERMISSION_REQUEST_CODE)
        scanResultAdapter.notifyDataSetChanged()

        // start the BLE scan
        bleScanner.startScan(null, scanSettings, scanCallback)
    }

    // stop the BLE scan
    private fun stopBleScan(){
        isScanning = false
        bleScanner.stopScan(scanCallback)
    }

    // checks the given permission have been granted or not
    private fun checkForPermissions(permission: String, name: String, requestCode: Int ){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            when{

                ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED ->{
                    Toast.makeText(applicationContext, "$name permission Granted", Toast.LENGTH_SHORT).show()
                }

                shouldShowRequestPermissionRationale(permission) -> showDialog(permission, name, requestCode)

                 else -> ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }

    }


    // shows dialog if the user does not at first allow the application the needed permissions
    private fun showDialog(permission: String, name: String, requestCode: Int){
        val builder = AlertDialog.Builder(this)

        builder.apply{

            setMessage("Permission to access your $name is required to use this app")
            setTitle("Permission required")
            setPositiveButton("OK"){dialog, wich ->
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
            }
        }

        val dialog = builder.create()
        dialog.show()
    }

    // sets the recycler view up with vertical layout
    private fun setupRecyclerView() {
       recyclerView.apply {

            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
           // aplication does not support scrolling in individual view cards
            isNestedScrollingEnabled = false
        }

        val animator = recyclerView.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    /*******************************************
     * Callback bodies
     *******************************************/
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            // checks to make sure we are not adding the same thing to the recycler view multiple times
            val indexQuery = scanResults.indexOfFirst {
                it.device.address == result.device.address }

            if (indexQuery != -1){

                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            }

            else{
                // adds tha scan result to the results list
                with(result.device) {

                    Log.i(
                        "ScanCallback",
                        "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size -1)
            }
        }

        // Check in logcat for this error for failed scans
        override fun onScanFailed(errorCode: Int) {

           Log.e("ScanCallback", "onScanFailed: code $errorCode")
        }
    }

}


