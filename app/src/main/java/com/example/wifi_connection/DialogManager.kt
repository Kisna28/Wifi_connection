package com.example.wifi_connection

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class DialogManager(private val activity: AppCompatActivity) {

    fun showPermissionsRequiredDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs storage permissions to function. Please grant the permissions in the app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    fun showLocationPermissionsRequiredDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs Location permissions to function. Please grant the permissions in the app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}