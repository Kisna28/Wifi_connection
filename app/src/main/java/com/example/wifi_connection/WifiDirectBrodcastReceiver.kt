package com.example.wifi_connection

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.wifi_connection.MainActivity.Companion.MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI

class WifiDirectBrodcastReceiver(
    val manager: WifiP2pManager?,
    val channel: WifiP2pManager.Channel,
    val activity: MainActivity,
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wi-Fi Direct mode is enabled or not, alert
                // the Activity.
                if (context == null) {
                    Log.e(ContentValues.TAG, "Context is null in onReceive")
                    return
                }
                val action = intent?.action
                if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Toast.makeText(context, "WIFI  iS On", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "WIFI  iS Off", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // The peer list has changed! We should probably do something about
                // that.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(
                            activity,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PermissionChecker.PERMISSION_GRANTED
                    ) {
                        // Permission is already granted, proceed with requestPeers
                        manager?.requestPeers(channel, activity.peerListListener)
                        Log.d(ContentValues.TAG, "P2P peers changed")
                    } else {
                        // Permission not granted, request it
                        activity.requestPermissions(
                            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION_AND_WIFI
                        )
                    }
                } else {
                    // For versions below Marshmallow, no runtime permission check needed
                    manager?.requestPeers(channel, activity.peerListListener)
                    Log.d(ContentValues.TAG, "P2P peers changed")
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {

                // Connection state changed! We should probably do something about
                // that.


            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                /* (activity.supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
                     .apply {
                         updateThisDevice(
                             intent.getParcelableExtra(
                                 WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                             ) as WifiP2pDevice
                         )
                     }*/
            }
        }
    }

}
