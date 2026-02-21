package cn.com.omnimind.omnibot.devserver.command

import cn.com.omnimind.omnibot.api.BaseOperatorResult
import cn.com.omnimind.omnibot.devserver.command.CommandResultWriter.handleResult
import cn.com.omnimind.omnibot.devserver.contract.CommandInfo
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class CommandRegistryTest {
    @Test
    fun `collect should register annotated handlers`() {
        val routes = CommandRegistry.collect(listOf(TestCommandHandlers()))

        assertEquals(1, routes.size)
        assertEquals("/testCommand", routes.first().path)
        assertEquals("testCommand", routes.first().commandName)
    }

    @Test
    fun `route handler should be invokable`() {
        val routes = CommandRegistry.collect(listOf(TestCommandHandlers()))
        val route = routes.first()
        val response = runBlocking { route.handler.invoke(FakeSession(uri = route.path)) }
        val body = response.getData().bufferedReader().use { it.readText() }

        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertTrue(body.contains("ok"))
    }
}

private class TestCommandHandlers {
    @CommandInfo(
        name = "testCommand",
        description = "A test command",
        argNames = [],
        responseType = Unit::class,
    )
    private suspend fun execute(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val result = BaseOperatorResult(success = true, message = "ok", data = Unit)
        return handleResult(result)
    }
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
