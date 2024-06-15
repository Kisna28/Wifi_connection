package com.example.wifi_connection

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PermissionDialogManager(private val activity: AppCompatActivity) {

    @RequiresApi(Build.VERSION_CODES.R)
    fun showPermissionsRequiredDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs storage permissions to function. Please grant the permissions in the app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                   val intent= Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                try {
                    activity.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // Inform user that the functionality is not available on their device
                    Toast.makeText(
                        activity,
                        "Storage permission management not supported on this device. Please request permissions in the app.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}