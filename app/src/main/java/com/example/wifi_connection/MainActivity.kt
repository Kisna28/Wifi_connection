package com.example.wifi_connection

import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.contentValuesOf

class MainActivity : AppCompatActivity() {

    lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var wifiManager: WifiManager
    private lateinit var receiver: WifiDirectBrodcastReceiver
    val PERMISSION_WIFI = Manifest.permission.ACCESS_WIFI_STATE
    private lateinit var peerListAdapter: ArrayAdapter<String>
    private val intentFilter = IntentFilter()
    private val peers = mutableListOf<WifiP2pDevice>()

    companion object {
        const val MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI = 123
    }

    val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)

            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
            updateListView()
            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }
        if (peers.isEmpty()) {
            Log.d("NotFound", "No devices found")
            return@PeerListListener
        }
    }
    private lateinit var listAdapter: WiFiPeerListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnonoff = findViewById<Button>(R.id.onOff)
        val btnDiscover = findViewById<Button>(R.id.discover)
        var listView = findViewById<ListView>(R.id.peerListView)
        val connicationStatus = findViewById<TextView>(R.id.connectionStatus)
        listAdapter = WiFiPeerListAdapter(this, R.layout.item, peers)
        listView.adapter = listAdapter
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
        }
        btnDiscover.setOnClickListener {

            // Initiate peer discovery when the button is clicked
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Initiate peer discovery when the button is clicked
                manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        connicationStatus.text ="Discovery Started"
                        // Code for when the discovery initiation is successful goes here.
                        Toast.makeText(
                            this@MainActivity,
                            "Discovery initiation successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        connicationStatus.text ="Discovery Started Failed"
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
                    this@MainActivity,
                    "Permission granted. Retrying...",
                    Toast.LENGTH_SHORT
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
            val device = peers[0]

            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }

            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Connection initiation successful
                    Toast.makeText(
                        this@MainActivity,
                        "Connection initiation successful",
                        Toast.LENGTH_SHORT
                    ).show()
                  connect()
                }
                override fun onFailure(reason: Int) {
                    // Connection initiation failed
                    Toast.makeText(
                        this@MainActivity,
                        "Connection initiation failed. Reason code: $reason",
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
     fun connect() {
        // Picking the first device found on the network.
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
                    "Successfully",
                    Toast.LENGTH_SHORT
                ).show()

            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "Connect failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateListView() {
        listAdapter.notifyDataSetChanged()
    }
}

