package cn.com.omnimind.omnibot.util

import cn.com.omnimind.omnibot.OmniOperatorService
import com.google.gson.Gson
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

// Define the ConnectionStatus enum for clear return values
enum class ConnectionStatus {
    ALREADY_CONNECTED,
    SUCCESS,
    FAILURE,
}

object SocketHandler {
    private var mSocket: Socket? = null
    private val gson = Gson()

    // Optional auth token for Socket.IO connection.
    // When set, it is sent as part of the Socket.IO v4 handshake auth payload.
    // The server can verify this token before allowing the connection.
    var authToken: String? = null

    // --- NEW: Internal state management ---
    private val internalState =
        ConcurrentHashMap<String, Boolean>().apply {
            put("companion_mode", false)
            put("finished_browsing", false)
        }

    @Synchronized
    fun setCompanionMode(enabled: Boolean) {
        internalState["companion_mode"] = enabled
        OmniLog.i("SocketHandler", "Internal state updated: companion_mode -> $enabled")
    }

    @Synchronized
    fun setFinishedBrowsing(finished: Boolean) {
        internalState["finished_browsing"] = finished
        OmniLog.i("SocketHandler", "Internal state updated: finished_browsing -> $finished")
    }

    @Synchronized
    fun getInternalStateJson(): JSONObject {
        return JSONObject(internalState as Map<*, *>)
    }
    // --- END OF NEW ---

    @Synchronized
    fun isConnected(): Boolean {
        return mSocket?.connected() == true
    }

    /**
     * Establishes a connection to the server. This is a suspending function.
     * It will not auto-reconnect if the initial connection fails.
     * It will time out after 2 seconds if a connection is not established.
     *
     * @param userInput The user input string to provide on request.
     * @param serverUrl The URL of the socket server.
     * @return A [ConnectionStatus] indicating the result of the connection attempt.
     */
    suspend fun establishConnection(
        userInput: String,
        serverUrl: String,
    ): ConnectionStatus {
        if (isConnected()) {
            OmniLog.i("SocketHandler", "Socket already connected.")
            return ConnectionStatus.ALREADY_CONNECTED
        }

        // Clean up any previous (failed) socket instance before creating a new one
        closeConnection()

        setFinishedBrowsing(false)

        val opts =
            IO.Options().apply {
                reconnection = false
                // If an auth token is configured, include it in the
                // Socket.IO v4 handshake so the server can verify it
                // before accepting the connection.
                val token = authToken
                if (!token.isNullOrBlank()) {
                    auth = mapOf("token" to token)
                    OmniLog.i("SocketHandler", "Auth token configured for connection.")
                }
            }

        try {
            val url =
                if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                    "http://$serverUrl"
                } else {
                    serverUrl
                }
            mSocket = IO.socket(url, opts)
            OmniLog.i("SocketHandler", "Initializing socket for server: $url")
        } catch (e: Exception) {
            e.printStackTrace()
            OmniLog.e("SocketHandler", "Failed to initialize socket: ${e.message}")
            return ConnectionStatus.FAILURE
        }

        // Register all persistent event handlers before connecting
        registerEventHandlers(userInput)

        // 2. Use withTimeoutOrNull to enforce a 2-second connection timeout
        val connectionResult =
            withTimeoutOrNull(2000L) {
                suspendCancellableCoroutine { continuation ->
                    mSocket?.once(Socket.EVENT_CONNECT) {
                        OmniLog.i("SocketHandler", "✅ Socket Connected!")
                        // Remove the error listener to prevent leaks
                        mSocket?.off(Socket.EVENT_CONNECT_ERROR)
                        if (continuation.isActive) {
                            continuation.resume(ConnectionStatus.SUCCESS)
                        }
                    }

                    mSocket?.once(Socket.EVENT_CONNECT_ERROR) { args ->
                        val error = args.getOrNull(0)?.toString() ?: "Unknown error"
                        OmniLog.e("SocketHandler", "❗️ Connection Error: $error")
                        // Remove the success listener
                        mSocket?.off(Socket.EVENT_CONNECT)
                        if (continuation.isActive) {
                            continuation.resume(ConnectionStatus.FAILURE)
                        }
                        // Clean up the failed socket instance
                        closeConnection()
                    }

                    // Handle cancellation of the coroutine (from timeout or other reasons)
                    continuation.invokeOnCancellation {
                        OmniLog.w("SocketHandler", "Connection attempt was cancelled.")
                        closeConnection()
                    }

                    OmniLog.i("SocketHandler", "Attempting to connect...")
                    mSocket?.connect()
                }
            }

        // 3. Check the result of the timeout block
        return if (connectionResult == null) {
            // This block executes if withTimeoutOrNull returned null (i.e., it timed out)
            OmniLog.w("SocketHandler", "❗️ Connection timed out after 2 seconds.")
            // The invokeOnCancellation block inside the coroutine will have already called closeConnection(),
            // but calling it again here ensures cleanup in case of any race conditions.
            closeConnection()
            ConnectionStatus.FAILURE
        } else {
            // Connection succeeded or failed explicitly before the timeout
            connectionResult
        }
    }

    private fun registerEventHandlers(userInput: String) {
        mSocket?.on("request_user_input") { args ->
            OmniLog.i("SocketHandler", "-> Received 'request_user_input' from server.")
            val ack = args.lastOrNull() as? Ack
            val response =
                JSONObject()
                    .put("data", userInput)
                    .put("success", true)
                    .put("message", "User input requested successfully.")
            ack?.call(response)
            OmniLog.i("SocketHandler", "<- Sent ACK for 'request_user_input' with data: $response")
        }

        mSocket?.on("get_internal_state") { args ->
            OmniLog.i("SocketHandler", "-> Received 'get_internal_state' from server.")
            val ack = args.lastOrNull() as? Ack
            val stateJson = getInternalStateJson()
            ack?.call(stateJson)
            OmniLog.i("SocketHandler", "<- Sent ACK for 'get_internal_state' with data: $stateJson")
        }

        mSocket?.on("capture_screenshot_image") { args ->
            handleCommandWithAck(args, "capture_screenshot_image") {
                OmniOperatorService.captureScreenshotImage()
            }
        }
        mSocket?.on("capture_screenshot_xml") { args ->
            handleCommandWithAck(args, "capture_screenshot_xml") {
                OmniOperatorService.captureScreenshotXml()
            }
        }
        mSocket?.on("click_coordinate") { args ->
            handleCommandWithAck(args, "click_coordinate") { data ->
                val x = data?.optDouble("x")?.toFloat() ?: 0f
                val y = data?.optDouble("y")?.toFloat() ?: 0f
                OmniOperatorService.clickCoordinate(x, y)
            }
        }
        mSocket?.on("long_click_coordinate") { args ->
            handleCommandWithAck(args, "long_click_coordinate") { data ->
                val x = data?.optDouble("x")?.toFloat() ?: 0f
                val y = data?.optDouble("y")?.toFloat() ?: 0f
                OmniOperatorService.longClickCoordinate(x, y)
            }
        }
        mSocket?.on("input_text") { args ->
            handleCommandWithAck(args, "input_text") { data ->
                val text = data?.optString("text") ?: ""
                OmniOperatorService.inputTextToFocusedNode(text)
            }
        }
        mSocket?.on("scroll_coordinate") { args ->
            handleCommandWithAck(args, "scroll_coordinate") { data ->
                val x = data?.optDouble("x")?.toFloat() ?: 0f
                val y = data?.optDouble("y")?.toFloat() ?: 0f
                val direction = data?.optString("direction") ?: ""
                val distance = data?.optDouble("distance")?.toFloat() ?: 300f
                OmniOperatorService.scrollCoordinate(x, y, direction, distance)
            }
        }
        mSocket?.on("launch_application") { args ->
            handleCommandWithAck(args, "launch_application") { data ->
                val packageName = data?.optString("package_name") ?: ""
                OmniOperatorService.launchApplication(packageName)
            }
        }
        mSocket?.on("list_installed_applications") { args ->
            handleCommandWithAck(args, "list_installed_applications") {
                OmniOperatorService.listInstalledApplications()
            }
        }
        mSocket?.on("go_home") { args -> handleCommandWithAck(args, "go_home") { OmniOperatorService.goHome() } }
        mSocket?.on("go_back") { args -> handleCommandWithAck(args, "go_back") { OmniOperatorService.goBack() } }

        mSocket?.on("require_user_confirmation") { args ->
            handleCommandWithAck(args, "require_user_confirmation") { data ->
                val prompt = data?.optString("prompt") ?: "Empty prompt"
                OmniOperatorService.requireUserConfirmation(prompt)
            }
        }

        mSocket?.on("require_user_choice") { args ->
            handleCommandWithAck(args, "require_user_choice") { data ->
                val prompt = data?.optString("prompt") ?: "Empty prompt"
                val options =
                    data?.optJSONArray("options")?.let { jsonArray ->
                        List(jsonArray.length()) { index -> jsonArray.optString(index) }
                    } ?: emptyList()
                OmniOperatorService.requireUserChoice(prompt, options)
            }
        }

        mSocket?.on("show_message") { args ->
            handleCommandWithAck(args, "show_message") { data ->
                val title = data?.optString("title") ?: "Message From Omni"
                val content = data?.optString("content") ?: "No content provided."
                OmniOperatorService.showMessage(title, content)
            }
        }

        mSocket?.on("push_message_to_bot") { args ->
            handleCommandWithAck(args, "push_message_to_bot") { data ->
                val message = data?.optString("message", "No message content.") ?: "No message content."
                val suggestionTitle = data?.optString("suggestion_title")
                val suggestions =
                    data?.optJSONArray("suggestions")?.let { jsonArray ->
                        List(jsonArray.length()) { index -> jsonArray.optString(index) }
                    }
                OmniOperatorService.pushMessageToBot(message, suggestionTitle, suggestions)
            }
        }

        mSocket?.on(Socket.EVENT_DISCONNECT) { OmniLog.w("SocketHandler", "🔴 Socket Disconnected.") }
    }

    @Synchronized
    fun closeConnection() {
        mSocket?.disconnect()
        mSocket?.off()
        mSocket = null
        OmniLog.i("SocketHandler", "Socket connection resources released.")
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleCommandWithAck(
        args: Array<Any>,
        commandName: String,
        operation: suspend (data: JSONObject?) -> Any,
    ) {
        val data = args.getOrNull(0) as? JSONObject
        val ack = args.lastOrNull() as? Ack
        OmniLog.i("SocketHandler", "-> '$commandName' with data: $data")
        GlobalScope.launch {
            val result =
                try {
                    operation(data)
                } catch (e: Exception) {
                    OmniLog.e("SocketHandler", "⚠️ Exception during '$commandName': ${e.message}", e)
                    mapOf("success" to false, "message" to "Exception: ${e.message}")
                }
            val resultJson =
                try {
                    JSONObject(gson.toJson(result))
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("success", false)
                        put("message", "Unknown error serializing result: ${e.message}")
                    }
                }
            ack?.call(resultJson)
            OmniLog.i("SocketHandler", "<- '$commandName' with message: ${resultJson.optString("message")}")
        }
    }
}
