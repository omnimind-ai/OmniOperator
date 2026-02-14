package cn.com.omnimind.omnibot.devserver.route

import fi.iki.elonen.NanoHTTPD

class DevServerRouteDispatcher(
    private val staticContentHandler: StaticContentHandler,
    private val handleCommandsRequest: () -> NanoHTTPD.Response,
    private val handleOpenApiJsonRequest: () -> NanoHTTPD.Response,
    private val handleGetTimestampsRequest: () -> NanoHTTPD.Response,
    private val commandRoutes: Map<String, suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response>,
) {
    suspend fun dispatch(
        session: NanoHTTPD.IHTTPSession,
        healthVersion: String,
    ): NanoHTTPD.Response =
        when (session.uri) {
            "/" -> staticContentHandler.handleRootRequest()
            "/commands" -> handleCommandsRequest()
            "/openapi.json" -> handleOpenApiJsonRequest()
            "/redoc" -> staticContentHandler.handleRedocRequest()
            "/client" -> staticContentHandler.handleClientRequest()
            "/timestamps" -> handleGetTimestampsRequest()
            "/health" ->
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "text/plain",
                    healthVersion,
                )

            else -> {
                when {
                    session.uri.startsWith("/static/") -> staticContentHandler.handleStaticFileRequest(session)
                    commandRoutes.containsKey(session.uri) -> commandRoutes[session.uri]!!.invoke(session)
                    else -> handleNotFound(session.uri)
                }
            }
        }

    private fun handleNotFound(@Suppress("UNUSED_PARAMETER") uri: String): NanoHTTPD.Response {
        return NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.NOT_FOUND,
            "text/plain",
            "Not Found",
        )
    }
}
