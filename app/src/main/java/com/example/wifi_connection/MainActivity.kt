package com.example.wifi_connection

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var wifiManager: WifiManager
    private lateinit var receiver: WifiDirectBrodcastReceiver
    private lateinit var locationManager: LocationManager
    private val intentFilter = IntentFilter()
    private val peers = mutableListOf<WifiP2pDevice>()
    private val STORAGE_PERMISSION_CODE = 123

     //DIRECT-bemrr, MAC Address: ba:27:eb:b9:1d:74

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI = 101
    }
    // private val allowSSIDs = listOf("ba:27:eb:b9:1d:74")

    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        // val filteredPeers = refreshedPeers.filter { allowSSIDs.contains(it.deviceAddress) }
        for (peer in refreshedPeers) {
            Log.d(
                "DiscoveredDevice",
                "Device Name: ${peer.deviceName}, MAC Address: ${peer.deviceAddress}"
            )
        }
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            updateListView()
        }
        if (peers.isEmpty()) {
            Log.d("NotFound", "No devices found")
            return@PeerListListener
        }
    }

    private lateinit var listAdapter: WiFiPeerListAdapter

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NewApi", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnonoff = findViewById<Button>(R.id.onOff)
        val btnDiscover = findViewById<Button>(R.id.discover)
        val listView = findViewById<ListView>(R.id.peerListView)
        val communicationStatus = findViewById<TextView>(R.id.connectionStatus)
        listAdapter = WiFiPeerListAdapter(this, R.layout.item, peers)

        listView.adapter = listAdapter
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = peers[position]
            // Handle the click event, for example, initiate a connection to the selected device
            connectToPeer(selectedDevice)
        }

        btnonoff.setOnClickListener {
            if (wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = false
                btnonoff.text = "Wifi Off"
                Toast.makeText(this, "WiFi is enable", Toast.LENGTH_SHORT).show()
            } else {
                wifiManager.isWifiEnabled = true
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
                btnonoff.text = "Wifi On"
                Toast.makeText(this, "WiFi is disabled", Toast.LENGTH_SHORT).show()
            }
            Log.d("WiFiState", "WiFi state after change: ${wifiManager.isWifiEnabled}")
            Log.d("Location", "Location: ${locationManager.isLocationEnabled}")
        }
        btnDiscover.setOnClickListener {
            // Initiate peer discovery when the button is clicked
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Initiate peer discovery when the button is clicked
                manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        // Code for when the discovery initiation is successful goes here.
                        Toast.makeText(
                            this@MainActivity, "Discovery initiation successful", Toast.LENGTH_SHORT
                        ).show()
                        communicationStatus.text = "Discovery Started"
                    }

                    override fun onFailure(reasonCode: Int) {
                        communicationStatus.text = "Discovery Started Failed"
                        // Code for when the discovery initiation fails goes here.
                        // Alert the user that something went wrong.
                        Toast.makeText(
                            this@MainActivity,
                            "Discovery initiation failed. Reason code: $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            } else {
                // Request the necessary permission if not granted
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI
                )
            }
        }
        val sendbtn = findViewById<Button>(R.id.send)
        val receivebtn = findViewById<Button>(R.id.receive)

        receivebtn.setOnClickListener {
            requestStoragePermission()
        }
        sendbtn.setOnClickListener {
            GlobalScope.launch {
                try {
                    FTPClientUpload().UploadFiles()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private fun requestStoragePermission() {
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (readPermission && writePermission) {
            // Permissions already granted
            Toast.makeText(this, "Storage permissions already granted!", Toast.LENGTH_SHORT).show()
            showLoginDialog()
        } else {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLoginDialog()
            } else {
                val dialogManager = PermissionDialogManager(this)
                dialogManager.showPermissionsRequiredDialog()
            }
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private fun showLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)
        val usernameEditText = dialogView.findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = dialogView.findViewById<EditText>(R.id.passwordEditText)

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("FTP Login")
        dialogBuilder.setPositiveButton("Login") { _, _ ->
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            GlobalScope.launch {
                try {
                    FTPManager().logIn(username, password)
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                    }
                    e.printStackTrace()
                }
            }
        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }


    public override fun onResume() {
        super.onResume()
        receiver = WifiDirectBrodcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    public override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }
    /*  override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
// Handle permission request results if needed
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry the operation
                Toast.makeText(
                    this@MainActivity, "Permission granted. Retrying...", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Permission denied. Cannot initiate discovery",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }*/

/*    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionManager.MY_PERMISSIONS_REQUEST_CODE) {
            if (permissionManager.hasAllPermissions()) {
                // Permissions granted, continue with your logic (e.g., show login dialog)
                showLoginDialog()
            } else {
                // Permissions denied, show dialog to inform user
                dialogManager.showPermissionsRequiredDialog()
            }
        }
    }*/


    private fun connectToPeer(peer: WifiP2pDevice) {
       // Check if the necessary permission is granted
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, proceed with connection initiation
            val config = WifiP2pConfig().apply {
                deviceAddress = peer.deviceAddress
                wps.setup = WpsInfo.PBC
                //     groupOwnerIntent = 0
                //Less probability to become the GO
            }
            // config.groupOwnerIntent=0       /////////////////************************!!!!!!!!!!!!!!!!!
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Connection initiation successful
                    Toast.makeText(
                        this@MainActivity, "Connecting.. to ${peer.deviceName}", Toast.LENGTH_SHORT
                    ).show()
                    connect()
                }

                override fun onFailure(reason: Int) {
                    // Connection initiation failed
                    Toast.makeText(
                        this@MainActivity,
                        "Connection Failed. Reason code: $reason",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
            //  connectionListener
        } else {
            // Request the necessary permission if not granted
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI
            )
        }
    }

    fun connect() {
        val device = peers[0]
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) manager.connect(channel, config, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                Toast.makeText(
                    this@MainActivity, "Successfully Connected", Toast.LENGTH_SHORT
                ).show()
                Handler().postDelayed({
                    manager.requestConnectionInfo(channel, connectionInfoListener)
                }, 100)
                // manager.requestConnectionInfo(channel, connectionInfoListener)
            }

            override fun onFailure(reason: Int) {
                //   Toast.makeText(this@MainActivity, "Connect failed. Retry.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    val connectionInfoListener = WifiP2pManager.ConnectionInfoListener { wifiP2pInfo ->
        if (wifiP2pInfo.isGroupOwner) {
            // Your logic for when the device is the group owner
            Log.d("P2PHost", "Connected as Group Owner")
            val group = findViewById<TextView>(R.id.Group)
            group.text = "Connected to: ${wifiP2pInfo.groupOwnerAddress.hostAddress} (Host)"
            Log.d("IpAddress", "Connected to: ${wifiP2pInfo.groupOwnerAddress.hostAddress} (Host)")
        } else {
            // Your logic for when the device is a client
            Log.d("P2PClient", "Connected as Client")
            val group = findViewById<TextView>(R.id.Group)
            group.text = "Connected to: ${wifiP2pInfo.groupOwnerAddress} (Client)"
            Log.d(
                "IpAddress", "Connected to: ${wifiP2pInfo.groupOwnerAddress.hostAddress} (Client)"
            )
            updateListView()
        }
    }

   /* private fun receiveFiles(host: String, port: Int) {
        try {
//            Socket(serverIP, port).use { socket ->
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2000000)
//            Log.d("Socket"," $socket.isConnected.toString()")
            val dataInputStream = DataInputStream(socket.getInputStream())

            // Number of files
            val filesCount = dataInputStream.readInt()
            Log.i(TAG, "fileCount $filesCount")

            repeat(filesCount) {
                receiveFile(dataInputStream, it, filesCount)
            }
//            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }*/

 /*   private fun receiveFile(dataInputStream: DataInputStream, fileCount: Int, totalFileCount: Int) {
        val nameSize = dataInputStream.readInt()
        val nameBytes = ByteArray(nameSize)
        dataInputStream.readFully(nameBytes)
        val fileName = String(nameBytes)

        val fileSize = dataInputStream.readLong()
        val file =
            File(Environment.getExternalStorageDirectory().absolutePath + "/Bemrr1/video-${System.currentTimeMillis()}-$fileCount.mp4")
        val dirs = file.parentFile
        dirs?.mkdirs()
        file.createNewFile()
        Log.d("path", "File $fileCount path ${file.absolutePath}")
        runOnUiThread {
            Toast.makeText(this@MainActivity, "File receiving", Toast.LENGTH_SHORT).show()
        }
        FileOutputStream(file).use { fos ->
            val buffer = ByteArray(4096)
            var bytesRemaining = fileSize
            while (bytesRemaining > 0) {
                val bytesRead = dataInputStream.read(
                    buffer,
                    0,
                    kotlin.math.min(bytesRemaining, buffer.size.toLong()).toInt()
                )
                if (bytesRead == -1) {
                    break
                }
                fos.write(buffer, 0, bytesRead)
                bytesRemaining -= bytesRead
            }
        }
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                "File ${fileCount + 1}/$totalFileCount",
                Toast.LENGTH_SHORT
            ).show()
        }

    }*/
    private fun updateListView() {
        listAdapter.notifyDataSetChanged()
    }
}






