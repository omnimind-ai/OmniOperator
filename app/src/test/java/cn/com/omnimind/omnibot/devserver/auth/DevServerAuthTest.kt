package cn.com.omnimind.omnibot.devserver.auth

import fi.iki.elonen.NanoHTTPD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class DevServerAuthTest {
    @Test
    fun `should allow request when api key is null`() {
        val session = FakeSession(uri = "/commands")
        val response = DevServerAuth.enforce(session, null)

        assertNull(response)
    }

    @Test
    fun `should allow health request without auth header`() {
        val session = FakeSession(uri = "/health")
        val response = DevServerAuth.enforce(session, "secret")

        assertNull(response)
    }

    @Test
    fun `should reject non-health request when auth is missing`() {
        val session = FakeSession(uri = "/commands")
        val response = DevServerAuth.enforce(session, "secret")

        assertNotNull(response)
        assertEquals(NanoHTTPD.Response.Status.UNAUTHORIZED, response!!.status)
    }

    @Test
    fun `should allow non-health request when auth matches`() {
        val session =
            FakeSession(
                uri = "/commands",
                headers = mapOf("authorization" to "Bearer secret"),
            )
        val response = DevServerAuth.enforce(session, "secret")

        assertNull(response)
    }
}

private class FakeSession(
    private val uri: String,
    private val headers: Map<String, String> = emptyMap(),
) : NanoHTTPD.IHTTPSession {
    override fun execute() = Unit

    override fun getCookies(): NanoHTTPD.CookieHandler =
        throw UnsupportedOperationException("Cookies are not used in these tests")

    override fun getHeaders(): MutableMap<String, String> = headers.toMutableMap()

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
