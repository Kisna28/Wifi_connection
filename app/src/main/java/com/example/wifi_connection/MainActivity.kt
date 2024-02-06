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
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket


class MainActivity : AppCompatActivity() {

    lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var wifiManager: WifiManager
    private lateinit var receiver: WifiDirectBrodcastReceiver
    private lateinit var locationManager: LocationManager
    val PERMISSION_WIFI = Manifest.permission.ACCESS_WIFI_STATE
    private val PICK_IMAGE_REQUEST_CODE = 12
    private lateinit var wifiP2pInfo: WifiP2pInfo
    private lateinit var peerListAdapter: ArrayAdapter<String>
    private val intentFilter = IntentFilter()
    private val peers = mutableListOf<WifiP2pDevice>()

    /*//DIRECT-bemrr, MAC Address: ba:27:eb:b9:1d:74  *********************
    //Chirag, MAC Address: 06:98:80:da:57:5c
    //KISNA, MAC Address: 86:19:d7:09:64:9e
    //Android MITV, MAC Address: b2:41:1d:0e:cb:1f
    //VIERA_thds630d_b311, MAC Address: 2e:24:ff:6b:54:fe
    //realme 3 Project ke baad bhi, MAC Address: 8a:e4:e1:5d:39:87
    //MADHAV BUTANI, MAC Address: 8a:25:f2:0e:5a:9c
    //realme C21, MAC Address: 22:44:f4:14:2d:37
    //[TV] Samsung 4 Series (32), MAC Address: f2:70:4f:93:c6:e4*/
    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI = 123
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

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*   val groupOwnerIpAddress: String = wifiP2pInfo.groupOwnerAddress.hostAddress!!
               Log.d("hostAddress", "IpGo: $groupOwnerIpAddress")*/

        val btnonoff = findViewById<Button>(R.id.onOff)
        val btnDiscover = findViewById<Button>(R.id.discover)
        var listView = findViewById<ListView>(R.id.peerListView)
        val connicationStatus = findViewById<TextView>(R.id.connectionStatus)
        //  val sendbtn = findViewById<Button>(R.id.send)
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
                        connicationStatus.text = "Discovery Started"
                    }

                    override fun onFailure(reasonCode: Int) {
                        connicationStatus.text = "Discovery Started Failed"
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
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        //  ********************************************************************************************************

        sendbtn.setOnClickListener {
              // Server().startServer()
               createSocketAndReceiveVideo()
            //  val fileTransferAsyncTask =   FileTransferAsyncTask(this,"192.168.0.34",12345,statusTextView).execute()
            //  val fileTransferClientTask = FileTransferClientTask(this)
        }

        //******************************************************************************************************
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
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
    override fun onRequestPermissionsResult(
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
    }

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
                val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
                override fun onSuccess() {
                    // Connection initiation successful
                    Toast.makeText(
                        this@MainActivity,
                        "Connecting.. to ${peer.deviceName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    connect()
                    //   manager.requestConnectionInfo(channel, connectionInfoListener)
                    //  connectionStatus.text = "Connected to ${peer.deviceName}"
                    /*  WifiP2pManager.ConnectionInfoListener { wifiP2pInfo ->
                        val groupOwnerAddress = wifiP2pInfo.isGroupOwner
                        Log.d(
                            "HostIP",
                            "GroupOwner Ip : $groupOwnerAddress and Device name is ${peer.deviceName}"
                        )
                    }*/
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
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        )
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
                    Toast.makeText(
                        this@MainActivity,
                        "Successfully Connected",
                        Toast.LENGTH_SHORT
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
        if (wifiP2pInfo.isGroupOwner == true) {
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
            Log.d("IpAddress", "Connected to: ${wifiP2pInfo.groupOwnerAddress.hostAddress} (Client)")
            updateListView()
        }
    }
    /* val reader =
        BufferedReader(InputStreamReader(inputStream, "UTF-8")) // For text messages
        while (true) {
            val message = reader.readLine()
            runOnUiThread {
                statusTextView.text = message// Display in a text view
            }*/


    @OptIn(DelicateCoroutinesApi::class)
    private fun createSocketAndReceiveVideo() {
        val statusTextView = findViewById<TextView>(R.id.statusTextView)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                /*   val groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.hostAddress
                     Log.d("TryBlock","Try block initial")
                    Log.d("IpAddress", "GroupOwner IP: $groupOwnerAddress")
                    Log.d("Connection", "Attempting to connect to server at $groupOwnerAddress")*/
                val socket = Socket()
                Log.d("soc", "${socket.isConnected} before ")
                socket.connect(InetSocketAddress("192.168.0.34", 12346),200000) // Use groupOwnerAddress
                Log.d("soc", "${socket.isConnected} after ")
                /*    val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
                val message = inputStream.readLine() // Read message from the server
                Log.d("Message", message)

                // Update UI with the received message
                runOnUiThread {
                    statusTextView.text = "Received message: $message"
                }*/
                val inputStream = socket.getInputStream()
                val file = File("/storage/emulated/0/Bemrr/recived_video.mp4")
                val fileOutputStream = FileOutputStream(file)
                //  val fileOutputStream = FileOutputStream(getExternalFilesDir(null)?.path + "/received_file.txt") // Adjust filename as needed
                // Corrected file transfer logic:
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead)
               }
                runOnUiThread {
                    statusTextView.text = "Received Successfully: ${file.name}"
                    Toast.makeText(this@MainActivity, "Received successfully!", Toast.LENGTH_SHORT)
                        .show()

                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error receiving: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("FailedToReceive", "Error receiving: ${e.message}")
                }
            }
            /*  finally {
                  // Close resources in a finally block to ensure proper cleanup
                  try {
                      fileOutputStream.close()
                      inputStream.close()
                      socket.close()
                  } catch (e: IOException) {
                      Log.d("Error", "Failed to close resources: $e")
                  }
              }*/
        }
    }



    private fun updateListView() {
        listAdapter.notifyDataSetChanged()
    }
}




