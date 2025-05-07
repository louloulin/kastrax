package ai.kastrax.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.ServerSocket

/**
 * NetworkUtils 的单元测试
 */
class NetworkUtilsTest {

    @Test
    fun `findAvailablePort should return a valid port`() {
        val port = NetworkUtils.findAvailablePort()
        assertTrue(port in 1024..65535, "端口应该在有效范围内")
        
        // 验证端口是否真的可用
        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(port)
            assertTrue(true, "端口应该可用")
        } catch (e: Exception) {
            fail("端口应该可用，但绑定失败: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    @Test
    fun `findAvailablePort with range should return a port within range`() {
        val minPort = 10000
        val maxPort = 10100
        val port = NetworkUtils.findAvailablePort(minPort, maxPort)
        
        assertTrue(port in minPort..maxPort, "端口应该在指定范围内")
        
        // 验证端口是否真的可用
        var serverSocket: ServerSocket? = null
        try {
            serverSocket = ServerSocket(port)
            assertTrue(true, "端口应该可用")
        } catch (e: Exception) {
            fail("端口应该可用，但绑定失败: ${e.message}")
        } finally {
            serverSocket?.close()
        }
    }

    @Test
    fun `isPortAvailable should return true for available port`() {
        val port = NetworkUtils.findAvailablePort()
        assertTrue(NetworkUtils.isPortAvailable(port), "端口应该可用")
    }

    @Test
    fun `isPortAvailable should return false for unavailable port`() {
        var serverSocket: ServerSocket? = null
        try {
            // 绑定一个端口
            serverSocket = ServerSocket(0)
            val port = serverSocket.localPort
            
            // 验证端口不可用
            assertFalse(NetworkUtils.isPortAvailable(port), "端口应该不可用")
        } finally {
            serverSocket?.close()
        }
    }

    @Test
    fun `findConsecutiveAvailablePorts should return correct number of ports`() {
        val count = 3
        val ports = NetworkUtils.findConsecutiveAvailablePorts(count)
        
        assertEquals(count, ports.size, "应该返回请求数量的端口")
        
        // 验证端口是连续的
        for (i in 0 until ports.size - 1) {
            assertEquals(ports[i] + 1, ports[i + 1], "端口应该是连续的")
        }
        
        // 验证所有端口都可用
        val serverSockets = mutableListOf<ServerSocket>()
        try {
            for (port in ports) {
                val socket = ServerSocket(port)
                serverSockets.add(socket)
            }
            assertEquals(count, serverSockets.size, "所有端口都应该可用")
        } finally {
            serverSockets.forEach { it.close() }
        }
    }
}
