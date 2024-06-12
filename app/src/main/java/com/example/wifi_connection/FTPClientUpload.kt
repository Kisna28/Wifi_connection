package com.example.wifi_connection


import org.apache.commons.net.ftp.FTPClient

import java.io.File
import java.io.FileInputStream


//  This code is for to upload a simple text file on server

class FTPClientUpload {
    fun UploadFiles() {
        // Replace these with your details
        val FTP_SERVER = "ftp.dlptest.com"
        val FTP_PORT = 21 // Default FTP port
        val username = "dlpuser"
        val password = "rNrKYTX9g7z3RgJRmxWuGHbeu"
        val videoPath = "/storage/emulated/0/Bemrr/30_26163806.mkv"
        val targetDirectory = "Vorne"

        /*   val FTP_SERVER = "192.168.230.201"
        val FTP_PORT = 15432
        val videoPath = "Phonestorage/Bemrr1/video_123"
        val username = "kisna"
        val password = "12345"
        val targetDirectory = "storage/emulated/0/Bemrr"
*/

        val ftpClient = FTPClient()

        try {
            // Connect to the server
               ftpClient.connect(FTP_SERVER, FTP_PORT)
            val replycode = ftpClient.replyCode
            println("Replay code is : ${replycode}")

            if (replycode == 230 || replycode == 220) {

                ftpClient.login(username, password)

                println("Connection is : ${ftpClient.isConnected}")

                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
                ftpClient.enterLocalPassiveMode()
                val directories = ftpClient.listDirectories()

                println("Available directories on server:")
                for (dir in directories) {
                    println(dir.name)
                    var directoryExists = false
                    if (dir.name == targetDirectory) {
                        directoryExists = true
                        break
                    }
                }
            // Get the file to upload
            val videoFile = File(videoPath)
            val inputStream = FileInputStream(videoFile)


            // Upload the file (replace "/upload/directory" with the desired upload location on the server)
            if (!ftpClient.storeFile(targetDirectory + videoFile.name, inputStream)) {
                println("Failed to upload file")
            } else {
                println("File uploaded successfully!")
            }

   }

            // Check connection status
            if (!ftpClient.isConnected) {
                println("Failed to connect to server")
                return
            }

            // Set binary transfer mode for any file type

        } finally {
            // Disconnect from the server (always execute)
            //  ftpClient.disconnect()
        }
    }
}


/*class FTPClientClass {

    fun downloadFiles() {

        val FTP_SERVER = "ftp.dlptest.com"
        val FTP_PORT = 21
        val REMOTE_DIRECTORY = "/storage/emulated/0"
        val username = "dlpuser"
        val password = "rNrKYTX9g7z3RgJRmxWuGHbeu"


        val ftpClient = FTPClient()
        Log.d("Enter", "Enter FTPClientClass")
        try {
            // Connect to FTP server
            ftpClient.connect(FTP_SERVER, FTP_PORT)

            Log.d("ftpConnection", "Connection of FTP is ${FTPClient().isConnected}")
            ftpClient.keepAlive

            val replyCode = ftpClient.replyCode
            Log.d("code", "$replyCode")
            if (replyCode == 220) {
                ftpClient.login(username, password)
                println("${ftpClient.isConnected} Inside if bock")


                // Thread to monitor connection stability
                val connectionMonitor = Thread {
                    val startTime = System.currentTimeMillis()
                    while (ftpClient.isConnected && (System.currentTimeMillis() - startTime) < 10000) {
                        Thread.sleep(2000) // Check every 2 seconds
                        Log.d(
                            "Connection status",
                            "Connected for ${(System.currentTimeMillis() - startTime) / 1000} seconds"
                        )
                    }
                    Log.d("Logout is", "logout is ${ftpClient.logout()}")

                    Log.d("Connection is", "${ftpClient.isConnected}")

                  //  Log.d("Connection is", "${ftpClient.getStatus()}")

                    if (ftpClient.isConnected) {
                        Log.d("Connection status", "Disconnected after 10 seconds")
                        // Perform disconnect or handle disconnection here (e.g., retry)
                        ftpClient.logout()
                        Log.d("Logout is", "${ftpClient.logout()}")

                        ftpClient.disconnect()
                        Log.d("Disconnect is", "${ftpClient.isConnected}")

                    }

                }
                connectionMonitor.start()
            }
            Log.d("ftpConnection status", "Status ${ftpClient.status}")

        } catch (e: IOException) {
            e.printStackTrace()
            println("Error: ${e.message}")
            Log.d("catch block", "GO to catch Block")
        } finally {
            // Disconnect from FTP server
            try {
                if (ftpClient.isConnected) {

                    Log.d("ftpConnection", "Status ${ftpClient.isConnected}")
                    //  ftpClient.logout()
                    //    ftpClient.disconnect()
                    //  println("Disconnected from FTP server")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                println("Error disconnecting from FTP server: ${e.message}")
            }
        }
    }

}*/




/*//This code is for download file
class FTPClientClass {
    fun downloadFiles() {
        // Replace these with your details
        val serverAddress = "ftp.dlptest.com"
        val port = 21 // Default FTP port
        val username = "dlpuser"
        val password = "rNrKYTX9g7z3RgJRmxWuGHbeu"
        val remoteFilePath = "1.txt" // Replace with the remote file path on the server
        val localFilePath = "/storage/emulated/0/DwfmServer/kisna.txt" // Replace with desired local path

        val ftpClient = FTPClient()

        try {
            // Connect to the server
            ftpClient.connect(serverAddress, port)
            val replycode = ftpClient.replyCode
            println("Replay code is : ${replycode}")

            if (replycode == 230 || replycode == 220) {

                ftpClient.login(username, password)

                println("Connection is : ${ftpClient.isConnected}")

                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
                ftpClient.enterLocalPassiveMode()

                // List files in the directory
                val files: Array<FTPFile> = ftpClient.listFiles()
                println("Files in directory:")
                for (file in files) {
                    println(file.name)
                }

                // Check if remote file exists by attempting to retrieve it as a stream
                val inputStream = ftpClient.retrieveFileStream(remoteFilePath)
                if (inputStream == null) {
                    println("Remote file does not exist!")
                    return
                }

                // Create the local file (if it doesn't exist)
                val localFile = File(localFilePath)
                if (!localFile.exists()) {
                    localFile.createNewFile()
                }

                // Download the file
                val outputStream = FileOutputStream(localFile)
                val result = ftpClient.retrieveFile(remoteFilePath, outputStream)
                outputStream.close()

                if (result) {
                    println("File downloaded successfully!")
                } else {
                    println("Download failed!")
                }
            } else {
                println("Connection failed with code: $replycode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error downloading file: ${e.message}")
        } finally {
            // Disconnect from the server (always execute)
            ftpClient.disconnect()
        }
    }
}*/