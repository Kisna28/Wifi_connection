package com.example.wifi_connection

import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import android.widget.TextView
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
//Client Side      ***********

class FileTransferAsyncTask(
    private val context: Context,
    private val host: String,
    private val port: Int,
    private val statusTextView: TextView
) : AsyncTask<Uri, Void, String>() {

    override fun doInBackground(vararg params: Uri): String? {
        try {
            Log.d("Called","HEllO DoInBcckgrounf")
            val socket = Socket()
            Log.d("soc","${socket.isConnected} Before ")

            socket.bind(null)
            socket.connect(InetSocketAddress(host, port), 50000)
            Log.d("soc","${socket.isConnected} After ")
            /* val inputStream = socket.getInputStream()
             //  val fileOutputStream = FileOutputStream("/storage/emulated/0/Bemrr")
             val reader =
                 BufferedReader(InputStreamReader(inputStream, "UTF-8")) // For text messages
                  val message = reader.readLine()
                     statusTextView.text = message// Display in a text view*/

            val outputStream: OutputStream = socket.getOutputStream()
            val cr = context.contentResolver
            val inputStream: FileInputStream = cr.openFileDescriptor(params[0], "r")?.fileDescriptor?.let {
                FileInputStream(it)
            }!!

            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }

            outputStream.close()
            inputStream.close()
            socket.close()

            return "File Received Successfully."
        } catch (e: IOException) {
            Log.d("FileTransferAsyncTask", e.message ?: "Unknown error")
            return null
        }
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        statusTextView.text = result ?: "File Received Failed."
    }
}
