package cn.com.omnimind.omnibot

import android.content.Context
import cn.com.omnimind.omnibot.util.OmniLog
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object DevServerManager {
    private const val TAG = "DevServerManager"
    private const val BASE_SERVER_PORT = 8080
    private const val MAX_PORT_ATTEMPTS = 10
    private const val PORT_RETRY_DELAY_MS = 30L

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

        var lastError: Exception? = null
        for (attempt in 0 until MAX_PORT_ATTEMPTS) {
            val port = BASE_SERVER_PORT + attempt
            try {
                // 1. Start the HTTP Server
                omniDevServer = OmniDevServer(port).apply { start() }

                // 2. Get the IP address
                val ipAddress = getLocalIpAddress()
                serverAddress = "$ipAddress:$port"
                OmniLog.d(TAG, "DevServer starting on $serverAddress")

                // 3. Perform other tasks
                readApplicationList(context)

                return serverAddress!!
            } catch (e: Exception) {
                lastError = e
                omniDevServer?.stop()
                omniDevServer = null
                serverAddress = null

                if (isAddressInUseError(e)) {
                    OmniLog.w(TAG, "Port $port in use, trying next port", e)
                    if (attempt < MAX_PORT_ATTEMPTS - 1) {
                        Thread.sleep(PORT_RETRY_DELAY_MS * (attempt + 1))
                    }
                    continue
                }

                OmniLog.e(TAG, "Error starting server", e)
                break
            }
        }

        val error = lastError ?: IllegalStateException("Failed to start DevServer")
        OmniLog.e(TAG, "Error starting server after retries", error)
        throw error
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

    private fun isAddressInUseError(e: Exception): Boolean {
        var current: Throwable? = e
        while (current != null) {
            if (current is java.net.BindException) return true
            val message = current.message?.lowercase() ?: ""
            if (message.contains("address already in use") || message.contains("bind failed")) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
