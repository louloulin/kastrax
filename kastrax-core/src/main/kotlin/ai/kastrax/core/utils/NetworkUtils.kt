package ai.kastrax.core.utils

import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

/**
 * 网络工具类，提供网络相关的实用函数
 */
object NetworkUtils {
    // 上次使用的端口号，用于在指定范围内查找可用端口
    private val lastPort = AtomicInteger(10000)

    /**
     * 查找一个可用的随机端口
     *
     * @return 可用的端口号
     */
    fun findAvailablePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }

    /**
     * 在指定范围内查找可用端口
     *
     * @param minPort 最小端口号（包含）
     * @param maxPort 最大端口号（包含）
     * @return 可用的端口号，如果在范围内没有可用端口，则返回随机可用端口
     */
    fun findAvailablePort(minPort: Int, maxPort: Int): Int {
        require(minPort in 1024..65535) { "最小端口必须在1024-65535范围内" }
        require(maxPort in 1024..65535) { "最大端口必须在1024-65535范围内" }
        require(minPort <= maxPort) { "最小端口必须小于或等于最大端口" }

        // 从上次使用的端口开始，避免总是从同一个端口开始
        val startPort = lastPort.get()
        val normalizedStartPort = if (startPort < minPort || startPort > maxPort) {
            minPort
        } else {
            startPort
        }

        // 尝试在范围内查找可用端口
        for (port in normalizedStartPort..maxPort) {
            if (isPortAvailable(port)) {
                lastPort.set(port + 1) // 更新上次使用的端口
                return port
            }
        }

        // 如果没有找到，从最小端口到起始端口尝试
        for (port in minPort until normalizedStartPort) {
            if (isPortAvailable(port)) {
                lastPort.set(port + 1) // 更新上次使用的端口
                return port
            }
        }

        // 如果在指定范围内没有可用端口，则返回随机可用端口
        return findAvailablePort()
    }

    /**
     * 检查指定端口是否可用
     *
     * @param port 要检查的端口号
     * @return 如果端口可用，则返回true；否则返回false
     */
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 查找多个连续的可用端口
     *
     * @param count 需要的端口数量
     * @param minPort 最小端口号（包含）
     * @param maxPort 最大端口号（包含）
     * @return 可用的连续端口列表，如果无法找到足够的连续端口，则返回空列表
     */
    fun findConsecutiveAvailablePorts(count: Int, minPort: Int = 1024, maxPort: Int = 65535): List<Int> {
        require(count > 0) { "端口数量必须大于0" }
        require(minPort in 1024..65535) { "最小端口必须在1024-65535范围内" }
        require(maxPort in 1024..65535) { "最大端口必须在1024-65535范围内" }
        require(minPort <= maxPort) { "最小端口必须小于或等于最大端口" }
        require(maxPort - minPort + 1 >= count) { "端口范围必须大于或等于请求的端口数量" }

        // 尝试查找连续的可用端口
        for (startPort in minPort..(maxPort - count + 1)) {
            val ports = (startPort until startPort + count).toList()
            if (ports.all { isPortAvailable(it) }) {
                return ports
            }
        }

        // 如果无法找到连续的可用端口，则返回空列表
        return emptyList()
    }
}
