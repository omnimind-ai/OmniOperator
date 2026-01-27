package cn.com.omnimind.omnibot

import android.content.Context
import cn.com.omnimind.omnibot.util.OmniLog
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object DevServerManager {
    private const val TAG = "DevServerManager"
    private const val SERVER_PORT = 8080

    private var omniDevServer: OmniDevServer? = null
    var serverAddress: String? = null
        private set

    val isRunning: Boolean
        get() = omniDevServer?.isAlive == true

    fun startServer(context: Context): String {
        if (isRunning) {
            OmniLog.d(TAG, "Server is already running at $serverAddress")
            return serverAddress!!
        }

        try {
            // 1. Start the HTTP Server
            omniDevServer = OmniDevServer(SERVER_PORT).apply { start() }

            // 2. Get the IP address
            val ipAddress = getLocalIpAddress()
            serverAddress = "$ipAddress:$SERVER_PORT"
            OmniLog.d(TAG, "DevServer starting on $serverAddress")

            // 3. Perform other tasks
            readApplicationList(context)

            return serverAddress!!
        } catch (e: Exception) {
            OmniLog.e(TAG, "Error starting server", e)
            stopServer(context) // Clean up if something went wrong
            throw e // Rethrow exception to be caught in MainActivity
        }
    }

    fun stopServer(context: Context) {
        if (!isRunning) {
            OmniLog.d(TAG, "Server is not running.")
            return
        }

        try {
            // Stop the HTTP server
            omniDevServer?.stop()
            omniDevServer = null
            serverAddress = null
            OmniLog.d(TAG, "DevServer stopped.")
        } catch (e: Exception) {
            OmniLog.e(TAG, "Error stopping server", e)
        }
    }

    // --- Helper methods moved here ---

    private fun getLocalIpAddress(): String =
        try {
            Collections
                .list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { Collections.list(it.inetAddresses).asSequence() }
                .firstOrNull { it is Inet4Address }
                ?.hostAddress ?: "Unavailable"
        } catch (e: Exception) {
            OmniLog.e(TAG, "Error getting IP address", e)
            "Unavailable"
        }

    private fun readApplicationList(context: Context) {
        OmniLog.v(TAG, "Reading application list")
        val packageManager = context.packageManager
        val applications = packageManager.getInstalledApplications(0)
        val appList = applications.map { it.packageName }
        OmniLog.d(TAG, "Installed applications: $appList")
    }
}
