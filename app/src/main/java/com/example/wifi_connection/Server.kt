package com.example.wifi_connection

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket

class Server {
    companion object {
        const val PORT = 12345 // Change port as needed
    }

    fun startServer() {
        val serverSocket = ServerSocket(PORT)
        Log.d("serverStart","Server started on port $PORT")

        while (true) {
            val clientSocket = serverSocket.accept()
            Log.d("ClientSocket","${clientSocket.isConnected}")
            Log.d("hostAddress","Client connected: ${clientSocket.inetAddress.hostAddress}")

            val outputStream = clientSocket.getOutputStream()
            val writer = OutputStreamWriter(outputStream)

            // Send message to the client
            val message = "Hello from Server!"
            writer.write(message)
            writer.flush()

            // Receive confirmation from client

            val inputStream = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val clientMessage = inputStream.readLine()

          Log.d("Message","Message sent to client: $message")

            writer.close()
            outputStream.close()
            clientSocket.close()
        }
    }
}