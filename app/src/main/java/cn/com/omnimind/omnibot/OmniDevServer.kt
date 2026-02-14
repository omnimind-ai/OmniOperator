@file:Suppress("UNUSED")

package cn.com.omnimind.omnibot

import cn.com.omnimind.omnibot.devserver.auth.DevServerAuth
import cn.com.omnimind.omnibot.devserver.command.CommandRegistry
import cn.com.omnimind.omnibot.devserver.command.handlers.CoordinateCommandHandlers
import cn.com.omnimind.omnibot.devserver.command.handlers.MessageCommandHandlers
import cn.com.omnimind.omnibot.devserver.command.handlers.NodeCommandHandlers
import cn.com.omnimind.omnibot.devserver.command.handlers.ScreenshotCommandHandlers
import cn.com.omnimind.omnibot.devserver.command.handlers.SystemCommandHandlers
import cn.com.omnimind.omnibot.devserver.openapi.OpenApiDocumentBuilder
import cn.com.omnimind.omnibot.devserver.route.DevServerRouteDispatcher
import cn.com.omnimind.omnibot.devserver.route.StaticContentHandler
import cn.com.omnimind.omnibot.util.OmniLog
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class OmniDevServer(
    port: Int,
    private val requestTimeoutMillis: Long = DEFAULT_REQUEST_TIMEOUT_MILLIS,
) : NanoHTTPD(port) {
    companion object {
        private const val TAG = "OmniDevServer"
        private const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 15_000L
    }

    private var lastScreenshotTimestamp: Long = 0L
    private var lastXmlTimestamp: Long = 0L
    private val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    var apiKey: String? = null

    private val screenshotHandlers =
        ScreenshotCommandHandlers(
            markScreenshotTimestamp = { lastScreenshotTimestamp = System.currentTimeMillis() },
            markXmlTimestamp = { lastXmlTimestamp = System.currentTimeMillis() },
        )
    private val nodeHandlers = NodeCommandHandlers()
    private val coordinateHandlers = CoordinateCommandHandlers()
    private val systemHandlers = SystemCommandHandlers()
    private val messageHandlers = MessageCommandHandlers()

    private val commandRoutes =
        CommandRegistry.collect(
            listOf(
                screenshotHandlers,
                nodeHandlers,
                coordinateHandlers,
                systemHandlers,
                messageHandlers,
            ),
        )

    private val commandRouteMap = CommandRegistry.toRouteMap(commandRoutes)
    private val openApiBuilder = OpenApiDocumentBuilder()
    private val routeDispatcher =
        DevServerRouteDispatcher(
            staticContentHandler = StaticContentHandler(),
            handleCommandsRequest = { handleCommandsRequest() },
            handleOpenApiJsonRequest = { handleOpenApiJsonRequest() },
            handleGetTimestampsRequest = { handleGetTimestampsRequest() },
            commandRoutes = commandRouteMap,
        )

    override fun serve(session: IHTTPSession): Response {
        DevServerAuth.enforce(session, apiKey)?.let { return it }
        val routeFuture =
            requestScope.future {
                routeDispatcher.dispatch(
                    session = session,
                    healthVersion = BuildConfig.VERSION_NAME,
                )
            }
        return try {
            routeFuture.get(requestTimeoutMillis, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            routeFuture.cancel(true)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                """{"success":false,"message":"Request timed out"}""",
            )
        } catch (e: CancellationException) {
            OmniLog.e(TAG, "Request cancelled: ${e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                """{"success":false,"message":"Request cancelled"}""",
            )
        } catch (e: ExecutionException) {
            OmniLog.e(TAG, "Route execution failed: ${e.cause?.message ?: e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                """{"success":false,"message":"Internal server error"}""",
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            OmniLog.e(TAG, "Request thread interrupted: ${e.message}", e)
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "application/json",
                """{"success":false,"message":"Internal server error"}""",
            )
        }
    }

    override fun stop() {
        requestScope.cancel("Dev server stopped")
        super.stop()
    }

    private fun handleCommandsRequest(): Response {
        val commands = CommandRegistry.toCommandList(commandRoutes)
        val json = Gson().toJson(commands)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }

    private fun handleOpenApiJsonRequest(): Response = openApiBuilder.build(commandRoutes)

    private fun handleGetTimestampsRequest(): Response {
        val timestamps =
            mapOf(
                "screenshot" to lastScreenshotTimestamp,
                "xml" to lastXmlTimestamp,
            )
        val json = Gson().toJson(timestamps)
        return newFixedLengthResponse(Response.Status.OK, "application/json", json)
    }
}
