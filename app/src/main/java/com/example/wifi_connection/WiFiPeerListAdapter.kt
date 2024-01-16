package com.example.wifi_connection

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class WiFiPeerListAdapter(context: Context, resource: Int, private val peers: List<WifiP2pDevice>) :
    ArrayAdapter<WifiP2pDevice>(context, resource, peers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.item, null)
        }

        val device = getItem(position)

        if (device != null) {
            val deviceName = view?.findViewById<TextView>(R.id.txt)

            deviceName?.text = device.deviceName
        }

        return view!!
    }
    private fun getDeviceStatus(status: Int): String {
        return when (status) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }
}