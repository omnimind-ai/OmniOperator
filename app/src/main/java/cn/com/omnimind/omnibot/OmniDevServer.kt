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
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking

class OmniDevServer(
    port: Int,
) : NanoHTTPD(port) {
    private var lastScreenshotTimestamp: Long = 0L
    private var lastXmlTimestamp: Long = 0L

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

    override fun serve(session: IHTTPSession): Response =
        runBlocking {
            DevServerAuth.enforce(session, apiKey)?.let { return@runBlocking it }
            return@runBlocking routeDispatcher.dispatch(
                session = session,
                healthVersion = BuildConfig.VERSION_NAME,
            )
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
