package cn.com.omnimind.omnibot

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class OmniDevServerTest {
    @Test
    fun `serve should return 401 when api key is set and auth header is missing`() {
        val server = OmniDevServer(18080).apply { apiKey = "secret" }
        val session = FakeSession(uri = "/commands")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED, response.status)
        assertTrue(body.contains("Unauthorized"))
    }

    @Test
    fun `serve should return 401 when api key is set and auth header is invalid`() {
        val server = OmniDevServer(18080).apply { apiKey = "secret" }
        val session =
            FakeSession(
                uri = "/commands",
                headers = mapOf("authorization" to "Bearer wrong-secret"),
            )

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED, response.status)
        assertTrue(body.contains("Unauthorized"))
    }

    @Test
    fun `serve should allow health endpoint without auth`() {
        val server = OmniDevServer(18080).apply { apiKey = "secret" }
        val session = FakeSession(uri = "/health")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertTrue(body.isNotBlank())
    }

    @Test
    fun `clickCoordinate should return 400 when coordinates are missing`() {
        val server = OmniDevServer(18080)
        val session = FakeSession(uri = "/clickCoordinate")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, response.status)
        assertTrue(body.contains("Invalid coordinates"))
    }

    @Test
    fun `clickNode should return 400 when nodeId is missing`() {
        val server = OmniDevServer(18080)
        val session = FakeSession(uri = "/clickNode")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, response.status)
        assertTrue(body.contains("Invalid node id"))
    }

    @Test
    fun `inputText should return 400 when text is missing`() {
        val server = OmniDevServer(18080)
        val session = FakeSession(uri = "/inputText", parameters = mapOf("nodeId" to listOf("node-1")))

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST, response.status)
        assertTrue(body.contains("Invalid node id or empty text"))
    }

    @Test
    fun `serve should return 500 when route execution throws`() {
        val server = OmniDevServer(18080, requestTimeoutMillis = 1000)
        registerTestRoute(server, "/boom") { error("boom") }
        val session = FakeSession(uri = "/boom")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.INTERNAL_ERROR, response.status)
        assertTrue(body.contains("Internal server error"))
    }

    @Test
    fun `serve should return 500 on request timeout`() {
        val server = OmniDevServer(18080, requestTimeoutMillis = 50)
        registerTestRoute(server, "/slow") {
            delay(300)
            NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", "ok")
        }
        val session = FakeSession(uri = "/slow")

        val response = server.serve(session)
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.INTERNAL_ERROR, response.status)
        assertTrue(body.contains("Request timed out"))
    }

    @Test
    fun `stop should cancel in flight request and return 500`() {
        val server = OmniDevServer(18080, requestTimeoutMillis = 5_000)
        val cancelled = AtomicBoolean(false)
        registerTestRoute(server, "/block") {
            suspendCancellableCoroutine<NanoHTTPD.Response> { continuation ->
                continuation.invokeOnCancellation { cancelled.set(true) }
            }
        }
        val responseRef = AtomicReference<NanoHTTPD.Response?>()
        val thread =
            Thread {
                responseRef.set(server.serve(FakeSession(uri = "/block")))
            }

        thread.start()
        Thread.sleep(100)
        server.stop()
        thread.join(2_000)

        assertFalse(thread.isAlive)
        val response = responseRef.get()
        assertNotNull(response)
        val body = response!!.getData().bufferedReader().use { it.readText() }
        assertEquals(NanoHTTPD.Response.Status.INTERNAL_ERROR, response.status)
        assertTrue(body.contains("Request cancelled"))
        assertTrue(cancelled.get())
    }
}

@Suppress("UNCHECKED_CAST")
private fun registerTestRoute(
    server: OmniDevServer,
    path: String,
    handler: suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response,
) {
    val routeDispatcherField = OmniDevServer::class.java.getDeclaredField("routeDispatcher")
    routeDispatcherField.isAccessible = true
    val routeDispatcher = routeDispatcherField.get(server)

    val commandRoutesField = routeDispatcher.javaClass.getDeclaredField("commandRoutes")
    commandRoutesField.isAccessible = true
    val commandRoutes =
        commandRoutesField.get(routeDispatcher) as MutableMap<String, suspend (NanoHTTPD.IHTTPSession) -> NanoHTTPD.Response>
    commandRoutes[path] = handler
}

private class FakeSession(
    private val uri: String,
    private val headers: Map<String, String> = emptyMap(),
    private val parameters: Map<String, List<String>> = emptyMap(),
) : NanoHTTPD.IHTTPSession {
    override fun execute() = Unit

    override fun getCookies(): NanoHTTPD.CookieHandler =
        throw UnsupportedOperationException("Cookies are not used in these tests")

    override fun getHeaders(): MutableMap<String, String> = headers.toMutableMap()

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(ByteArray(0))

    override fun getMethod(): NanoHTTPD.Method = NanoHTTPD.Method.GET

    override fun getParms(): MutableMap<String, String> =
        parameters.mapValues { it.value.firstOrNull().orEmpty() }.toMutableMap()

    override fun getParameters(): MutableMap<String, MutableList<String>> =
        parameters.mapValues { it.value.toMutableList() }.toMutableMap()

    override fun getQueryParameterString(): String =
        parameters.entries.joinToString("&") { (key, values) ->
            values.joinToString("&") { value ->
                "${key.encode()}=${value.encode()}"
            }
        }

    override fun getUri(): String = uri

    override fun parseBody(files: MutableMap<String, String>) = Unit

    override fun getRemoteIpAddress(): String = "127.0.0.1"

    override fun getRemoteHostName(): String = "localhost"
}

private fun String.encode(): String =
    java.net.URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
