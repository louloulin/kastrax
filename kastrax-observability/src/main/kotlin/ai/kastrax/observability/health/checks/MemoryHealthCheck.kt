package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthCheck
import ai.kastrax.observability.health.HealthResult
import ai.kastrax.observability.health.HealthStatus

/**
 * 内存健康检查。
 * 检查系统内存使用情况。
 *
 * @property warningThreshold 警告阈值，当内存使用率超过此值时，健康状态为 DEGRADED
 * @property criticalThreshold 严重阈值，当内存使用率超过此值时，健康状态为 DOWN
 */
class MemoryHealthCheck(
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck {
    override fun getName(): String = "memory"

    override fun check(): HealthResult {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsageRatio = usedMemory.toDouble() / maxMemory

        val details = mapOf(
            "max_memory" to formatSize(maxMemory),
            "total_memory" to formatSize(totalMemory),
            "free_memory" to formatSize(freeMemory),
            "used_memory" to formatSize(usedMemory),
            "usage_ratio" to String.format("%.2f", memoryUsageRatio * 100) + "%"
        )

        return when {
            memoryUsageRatio >= criticalThreshold -> HealthResult(
                HealthStatus.DOWN,
                details,
                RuntimeException("Memory usage is critical: ${String.format("%.2f", memoryUsageRatio * 100)}%")
            )
            memoryUsageRatio >= warningThreshold -> HealthResult(
                HealthStatus.DEGRADED,
                details,
                RuntimeException("Memory usage is high: ${String.format("%.2f", memoryUsageRatio * 100)}%")
            )
            else -> HealthResult(HealthStatus.UP, details)
        }
    }

    private fun formatSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            size >= gb -> String.format("%.2f GB", size.toDouble() / gb)
            size >= mb -> String.format("%.2f MB", size.toDouble() / mb)
            size >= kb -> String.format("%.2f KB", size.toDouble() / kb)
            else -> "$size bytes"
        }
    }
}
