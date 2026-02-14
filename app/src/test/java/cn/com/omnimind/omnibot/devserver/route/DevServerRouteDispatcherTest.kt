package cn.com.omnimind.omnibot.devserver.route

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class DevServerRouteDispatcherTest {
    @Test
    fun `dispatch should return health version on health endpoint`() {
        val dispatcher = createDispatcher(emptyMap())

        val response = runBlocking { dispatcher.dispatch(FakeSession("/health"), "1.2.3") }
        val body = response.data.bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertEquals("1.2.3", body)
    }

    @Test
    fun `dispatch should route known command path`() {
        val commandRoutes =
            mapOf<String, suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response>(
                "/demo" to {
                    NanoHTTPD.newFixedLengthResponse(
                        NanoHTTPD.Response.Status.OK,
                        "text/plain",
                        "command-ok",
                    )
                },
            )
        val dispatcher = createDispatcher(commandRoutes)

        val response = runBlocking { dispatcher.dispatch(FakeSession("/demo"), "1.2.3") }
        val body = response.data.bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertEquals("command-ok", body)
    }

    @Test
    fun `dispatch should return 404 for unknown path`() {
        val dispatcher = createDispatcher(emptyMap())

        val response = runBlocking { dispatcher.dispatch(FakeSession("/unknown"), "1.2.3") }
        val body = response.data.bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.NOT_FOUND, response.status)
        assertTrue(body.contains("Not Found"))
    }

    private fun createDispatcher(
        commandRoutes: Map<String, suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response>,
    ): DevServerRouteDispatcher =
        DevServerRouteDispatcher(
            staticContentHandler = StaticContentHandler(),
            handleCommandsRequest = {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "[]",
                )
            },
            handleOpenApiJsonRequest = {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{}",
                )
            },
            handleGetTimestampsRequest = {
                NanoHTTPD.newFixedLengthResponse(
                    NanoHTTPD.Response.Status.OK,
                    "application/json",
                    "{}",
                )
            },
            commandRoutes = commandRoutes,
        )
}

private class FakeSession(
    private val uri: String,
) : NanoHTTPD.IHTTPSession {
    override fun execute() = Unit

    override fun getCookies(): NanoHTTPD.CookieHandler =
        throw UnsupportedOperationException("Cookies are not used in these tests")

    override fun getHeaders(): MutableMap<String, String> = mutableMapOf()

    override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

    override fun getMethod(): NanoHTTPD.Method = NanoHTTPD.Method.GET

    override fun getParms(): MutableMap<String, String> = mutableMapOf()

    override fun getParameters(): MutableMap<String, MutableList<String>> = mutableMapOf()

    override fun getQueryParameterString(): String = ""

    override fun getUri(): String = uri

    override fun parseBody(files: MutableMap<String, String>) = Unit

    override fun getRemoteIpAddress(): String = "127.0.0.1"

    override fun getRemoteHostName(): String = "localhost"
}
