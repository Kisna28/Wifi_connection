package com.example.wifi_connection


import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.AsyncTask
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
class FileTransferClientTask(private val context: Context) : AsyncTask<Void, Void, String>() {

    // Specify whether you want to save the file in internal storage
    private val internalStorageRequired = false

    // Specify whether you want to save the file in external storage for sharing
    private val externalStorageForSharingRequired = false

    fun createFile(context: Context, directory: String, fileName: String): DocumentFile {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream") // Adjust the MIME type as needed
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + File.separator + directory)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), contentValues)
            ?: throw IOException("Failed to create file in MediaStore")

        return DocumentFile.fromSingleUri(context, uri)?.createFile("*/*", fileName)
            ?: throw IOException("Failed to create DocumentFile")
    }

    override fun doInBackground(vararg params: Void?): String {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("192.168.0.34", 12345), 200000)
            val clientData = ByteArray(1024 * 5000)
            val receivedBytesLen = socket.getInputStream().read(clientData)

            val fileNameLen = clientData[0]
            val fileName = String(clientData, 4, fileNameLen.toInt(), Charsets.US_ASCII)

            // Choose appropriate file saving method based on your requirements:
            val fileOutputStream = when {
                // Internal storage:
                internalStorageRequired -> context.openFileOutput(fileName, MODE_PRIVATE)

                // External shared storage:
                externalStorageForSharingRequired -> {
                    val externalFilesDir = context.getExternalFilesDir(null)
                    val file = File(externalFilesDir, fileName)
                    FileOutputStream(file)
                }
                // Scoped storage (Android 10+):

                else -> {
                    val file = createFile(context, "/", fileName)
                    context.contentResolver.openOutputStream(file.uri)

                }
            }

            fileOutputStream?.write(clientData, 4 + fileNameLen, receivedBytesLen - 4 - fileNameLen)
            fileOutputStream?.close()

            "File $fileName received and saved."
        } catch (e: Exception) {
            "File transfer failed: ${e.message}"
        }
    }

    override fun onPostExecute(result: String) {
        // Display the result message to the user using a Toast or UI element
    }

}


