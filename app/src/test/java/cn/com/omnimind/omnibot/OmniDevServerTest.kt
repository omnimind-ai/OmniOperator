package cn.com.omnimind.omnibot

import fi.iki.elonen.NanoHTTPD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

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
