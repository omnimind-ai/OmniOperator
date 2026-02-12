package cn.com.omnimind.omnibot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import cn.com.omnimind.omnibot.util.ConnectionStatus
import cn.com.omnimind.omnibot.util.OmniLog
import cn.com.omnimind.omnibot.util.SocketHandler
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.launch

class MainActivity : FlutterActivity() {
    private val methodChannelName = "cn.com.omnimind.omnibot/dev_server"
    private val botMessageEventChannelName = "cn.com.omnimind.omnibot/bot_message_events"

    private var omniDevServer: OmniDevServer? = null
    private val serverPort = 8080

    private var agentServerAddress: String? = null

    companion object {
        private const val TAG = "MainActivity"
        // --- NEW: Static EventSink for bot messages ---
        // This allows SocketHandler to access it easily
        var botMessageEventSink: EventChannel.EventSink? = null
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // --- SETUP METHOD CHANNEL ---
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, methodChannelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "sendServerAddress" -> {
                    try {
                        agentServerAddress = call.argument<String>("serverAddress")
                        OmniLog.v(TAG, "Receive server address $agentServerAddress")
                        result.success(agentServerAddress)
                    } catch (e: Exception) {
                        OmniLog.e(TAG, "Error receiving server address", e)
                        result.error("INVALID_ARGS", "Failed to receive server address.", e.message)
                    }
                }
                "sendTextToNative" -> {
                    val message = call.argument<String>("message")
                    // Use a local variable to prevent race conditions if agentServerAddress changes
                    val serverAddress = agentServerAddress

                    if (message == null || serverAddress == null) {
                        OmniLog.e(TAG, "Missing message or server address.")
                        result.error("INVALID_ARGS", "Message or server address is null.", null)
                        return@setMethodCallHandler
                    }

                    OmniLog.v(TAG, "Queueing connection request for task: $message")

                    lifecycleScope.launch {
                        try {
                            val status: ConnectionStatus = SocketHandler.establishConnection(message, serverAddress)
                            OmniLog.i(TAG, "Connection attempt finished with status: ${status.name}")
                            result.success(status.name)
                        } catch (e: Exception) {
                            OmniLog.e(TAG, "Error establishing connection via coroutine", e)
                            result.error("CONNECTION_FAILED", "An exception occurred during connection.", e.message)
                        }
                    }
                }
                "isAccessibilityServiceEnabled" -> {
                    try {
                        val isEnabled = OmniOperatorService.isAccessibilityServiceEnabled()
                        result.success(isEnabled)
                    } catch (e: Exception) {
                        OmniLog.e(TAG, "Error checking accessibility service", e)
                        result.error("CHECK_FAILED", "Failed to check accessibility service.", e.message)
                    }
                }
                "openAccessibilitySettings" -> {
                    openAccessibilitySettings(context)
                    result.success(null) // Acknowledge the call
                }
                "isIgnoringBatteryOptimizations" -> {
                    result.success(isIgnoringBatteryOptimizations())
                }
                "openBatteryOptimizationSettings" -> {
                    openBatteryOptimizationSettings()
                    result.success(null)
                }
                "startServer" -> {
                    try {
                        val address = DevServerManager.startServer(applicationContext)
                        result.success(address)
                    } catch (e: Exception) {
                        OmniLog.e(TAG, "Error starting server", e)
                        result.error("SERVER_START_FAILED", "Could not start the server.", e.message)
                    }
                }
                "stopServer" -> {
                    try {
                        DevServerManager.stopServer(applicationContext)
                        result.success(null)
                    } catch (e: Exception) {
                        OmniLog.e(TAG, "Error stopping server", e)
                        result.error("SERVER_STOP_FAILED", "Could not stop the server.", e.message)
                    }
                }
                "toggleCompanionMode" -> {
                    val enabled = call.argument<Boolean>("enabled") ?: false
                    SocketHandler.setCompanionMode(enabled)
                    result.success(null)
                }
                "toggleFinishedBrowsing" -> {
                    val finished = call.argument<Boolean>("finished") ?: false
                    SocketHandler.setFinishedBrowsing(finished)
                    result.success(null)
                }
                "setSocketAuthToken" -> {
                    val token = call.argument<String>("token")
                    SocketHandler.authToken =
                        if (token.isNullOrBlank()) null else token
                    OmniLog.v(TAG, "Socket.IO auth token updated.")
                    result.success(null)
                }
                "setDevServerApiKey" -> {
                    val key = call.argument<String>("apiKey")
                    DevServerManager.apiKey =
                        if (key.isNullOrBlank()) null else key
                    OmniLog.v(TAG, "DevServer API key updated.")
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        // --- NEW: SETUP BOT MESSAGE EVENT CHANNEL ---
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, botMessageEventChannelName).setStreamHandler(
            object : EventChannel.StreamHandler {
                override fun onListen(
                    arguments: Any?,
                    events: EventChannel.EventSink?,
                ) {
                    OmniLog.d(TAG, "EventChannel: onListen (Bot Messages)")
                    botMessageEventSink = events
                }

                override fun onCancel(arguments: Any?) {
                    OmniLog.d(TAG, "EventChannel: onCancel (Bot Messages)")
                    botMessageEventSink = null
                }
            },
        )
    }


    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent =
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:$packageName".toUri()
                    }
                startActivity(intent)
            } catch (e: Exception) {
                OmniLog.e(TAG, "请求忽略电池优化时发生异常，可能没有 Activity 能处理此 Intent。", e)
            }
        }
    }

    private fun openAccessibilitySettings(context: Context) {
        val intent =
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        context.startActivity(intent)
        OmniLog.d(TAG, "Opening accessibility settings.")
    }
}
