package cn.com.omnimind.omnibot.util

import io.socket.client.Ack
import kotlinx.coroutines.CoroutineScope
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class SocketHandlerTest {
    @Before
    fun setUp() {
        SocketHandler.closeConnection()
    }

    @After
    fun tearDown() {
        SocketHandler.closeConnection()
    }

    @Test
    fun `handleCommandWithAck should return failure when scope is inactive`() {
        val ack = RecordingAck()

        invokeHandleCommandWithAck(
            args = arrayOf<Any>(ack),
            commandName = "test_command",
            operation = { mapOf("success" to true, "message" to "ok") },
        )

        assertTrue(ack.latch.await(1, TimeUnit.SECONDS))
        val payload = ack.payload
        assertNotNull(payload)
        assertTrue(payload!!.optString("message").contains("not active"))
    }

    @Test
    fun `handleCommandWithAck should execute command when scope is active`() {
        invokeCreateCommandScope()
        val ack = RecordingAck()

        invokeHandleCommandWithAck(
            args = arrayOf(JSONObject(), ack),
            commandName = "test_command",
            operation = { mapOf("success" to true, "message" to "ok") },
        )

        assertTrue(ack.latch.await(2, TimeUnit.SECONDS))
        val payload = ack.payload
        assertNotNull(payload)
        assertTrue(payload!!.optBoolean("success"))
        assertTrue(payload.optString("message") == "ok")
    }

    @Test
    fun `closeConnection should cancel command scope`() {
        invokeCreateCommandScope()
        assertNotNull(currentCommandScope())

        SocketHandler.closeConnection()

        assertNull(currentCommandScope())
        val ack = RecordingAck()
        invokeHandleCommandWithAck(
            args = arrayOf<Any>(ack),
            commandName = "after_close",
            operation = { mapOf("success" to true, "message" to "ok") },
        )
        assertTrue(ack.latch.await(1, TimeUnit.SECONDS))
        assertTrue(ack.payload!!.optString("message").contains("not active"))
    }
}

private class RecordingAck : Ack {
    val latch = CountDownLatch(1)
    @Volatile
    var payload: JSONObject? = null

    override fun call(vararg args: Any?) {
        payload = args.firstOrNull() as? JSONObject
        latch.countDown()
    }
}

private fun invokeCreateCommandScope() {
    val method = SocketHandler::class.java.getDeclaredMethod("createCommandScope")
    method.isAccessible = true
    method.invoke(SocketHandler)
}

private fun currentCommandScope(): CoroutineScope? {
    val method = SocketHandler::class.java.getDeclaredMethod("currentCommandScope")
    method.isAccessible = true
    return method.invoke(SocketHandler) as? CoroutineScope
}

private fun invokeHandleCommandWithAck(
    args: Array<Any>,
    commandName: String,
    operation: suspend (JSONObject?) -> Any,
) {
    val method =
        SocketHandler::class.java.getDeclaredMethod(
            "handleCommandWithAck",
            Array<Any>::class.java,
            String::class.java,
            kotlin.jvm.functions.Function2::class.java,
        )
    method.isAccessible = true
    method.invoke(SocketHandler, args, commandName, operation)
}
