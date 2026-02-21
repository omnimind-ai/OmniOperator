package cn.com.omnimind.omnibot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.net.BindException
import java.net.ServerSocket

@RunWith(RobolectricTestRunner::class)
class DevServerManagerTest {
    @Test
    fun `startServer should skip occupied base port`() {
        val context = RuntimeEnvironment.getApplication()
        val occupiedSocket = ServerSocket(8080)
        try {
            val address = DevServerManager.startServer(context)
            val selectedPort = address.substringAfterLast(":").toInt()
            assertTrue(selectedPort in 8081..8089)
            assertTrue(DevServerManager.isRunning)
        } finally {
            DevServerManager.stopServer(context)
            occupiedSocket.close()
        }
    }

    @Test
    fun `isAddressInUseError should detect bind exceptions from cause chain`() {
        val method =
            DevServerManager::class.java.getDeclaredMethod(
                "isAddressInUseError",
                Exception::class.java,
            )
        method.isAccessible = true

        val bindException = Exception("top", BindException("Address already in use"))
        val unknownException = Exception("other")

        val bindResult = method.invoke(DevServerManager, bindException) as Boolean
        val unknownResult = method.invoke(DevServerManager, unknownException) as Boolean

        assertTrue(bindResult)
        assertFalse(unknownResult)
    }

    @Test
    fun `start and stop should be idempotent and update running state`() {
        val context = RuntimeEnvironment.getApplication()
        DevServerManager.stopServer(context)
        assertFalse(DevServerManager.isRunning)

        try {
            val firstAddress = DevServerManager.startServer(context)
            assertTrue(DevServerManager.isRunning)

            val secondAddress = DevServerManager.startServer(context)
            assertTrue(DevServerManager.isRunning)
            assertTrue(firstAddress.isNotBlank())
            assertTrue(firstAddress == secondAddress)

            DevServerManager.stopServer(context)
            assertFalse(DevServerManager.isRunning)

            DevServerManager.stopServer(context)
            assertFalse(DevServerManager.isRunning)
        } finally {
            DevServerManager.stopServer(context)
        }
    }
}
