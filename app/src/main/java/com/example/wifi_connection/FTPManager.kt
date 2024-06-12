package com.example.wifi_connection


import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.timer

class FTPManager {

    /*  val FTP_SERVER = "ftp.dlptest.com"
        val FTP_PORT = 21
        val REMOTE_DIRECTORY = "Info.txt"
        val username = "dlpuser"
        val password = "rNrKYTX9g7z3RgJRmxWuGHbeu"
    */

    /* val FTP_SERVER = "192.168.230.201"
       val FTP_PORT = 15432
       val REMOTE_DIRECTORY = "/storage/emulated/0/Bemrr"
       val username = "kisna"
       val password = "12345"*/

    private val FTP_SERVER = "192.168.0.34"
    private val FTP_PORT = 12345
    private val REMOTE_DIRECTORY = "/home/bemrrpi5/6-04"
    private val username = "kisna"
    private val password = "12345"

    var lastDownloadedTimestamp = 0L
    private var timer: Timer? = null
    private var isDownloading = false
    val ftpClient = FTPClient()

    init {
        startFileChecking()
    }

    private fun startFileChecking() {
        timer = timer(period = 20000) {
            println("Checking for new files...")
            if (!isDownloading) {
                downloadFiles()
            }
        }
    }

    private fun downloadFiles() {
        //  ftpClient.connectTimeout = 300000 // 3 minute
        Log.d("Enter", "Enter FTPClientClass")
        try {
            ftpClient.connect(FTP_SERVER, FTP_PORT)
            Log.d("ftpConnection", "Connection of FTP is ${FTPClient().isConnected}")
            val login = ftpClient.login(username, password)

            val replyCode = ftpClient.replyCode
            Log.d("code", "$replyCode")
            if (replyCode in 200..299) {
                ftpClient.login(username, password)
                println("${ftpClient.isConnected} Inside if bock")
            }
            if (!login) {
                println("FTP login failed.")
                return
            }
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.enterLocalPassiveMode()

            println("************************** THIS IS AFTER SORTED FILE ********************************")

            ftpClient.changeWorkingDirectory(REMOTE_DIRECTORY)
            val files = ftpClient.listFiles()
            val sortedFiles = files.sortedByDescending { it.timestamp.timeInMillis }
            for (filesort in sortedFiles) {
                println(filesort.name)
            }

            val newestFile = sortedFiles.firstOrNull {

                it.timestamp.timeInMillis > lastDownloadedTimestamp
            }

            if (newestFile != null) {
                downloadFile(ftpClient, newestFile)
                lastDownloadedTimestamp = newestFile.timestamp.timeInMillis
            }
            ftpClient.logout()

        }   catch (e: IOException) {
            e.printStackTrace()
            Log.d("catchblock", "GO to catch Block")
        } finally {
            try {
                ftpClient.disconnect()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.d("finally", "GO to finally Block")
            }
            isDownloading = false
        }
    }

    private fun downloadFile(
        ftpClient: FTPClient, file: FTPFile
    ): Boolean {
        val localDir = File(Environment.getExternalStorageDirectory(), "BemrrCreation")
        if (!localDir.exists()) {
            if (!localDir.mkdirs()) {
                Log.e("dirCreation", "Failed to create directory: ${localDir.path}")
                return false
            }
        }
        val localFile = File(localDir, file.name)
        try {
            FileOutputStream(localFile).use { outputStream ->
                if (ftpClient.retrieveFile(file.name, outputStream)) {
                    Log.d("ftpDownload", "File  downloaded: ${file.name}")
                    Thread.sleep(10000)
                    return true
                } else {
                    Log.e("ftpDownload", "Failed to download file: ${file.name}")
                    return false
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("ftpDownloadError", "Error downloading file: ${file.name}", e)
            return false
        }
    }
}










