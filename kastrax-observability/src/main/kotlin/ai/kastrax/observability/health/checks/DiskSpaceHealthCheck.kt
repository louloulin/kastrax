package ai.kastrax.observability.health.checks

import ai.kastrax.observability.health.HealthCheck
import ai.kastrax.observability.health.HealthResult
import ai.kastrax.observability.health.HealthStatus
import java.io.File

/**
 * 磁盘空间健康检查。
 * 检查指定路径的磁盘空间使用情况。
 *
 * @property path 要检查的路径
 * @property warningThreshold 警告阈值，当磁盘使用率超过此值时，健康状态为 DEGRADED
 * @property criticalThreshold 严重阈值，当磁盘使用率超过此值时，健康状态为 DOWN
 */
class DiskSpaceHealthCheck(
    private val path: String = ".",
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck {
    override fun getName(): String = "disk_space"

    override fun check(): HealthResult {
        val file = File(path)
        if (!file.exists()) {
            return HealthResult(
                HealthStatus.DOWN,
                mapOf("path" to path),
                RuntimeException("Path does not exist: $path")
            )
        }

        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usableSpace = file.usableSpace
        val usedSpace = totalSpace - freeSpace
        val usageRatio = usedSpace.toDouble() / totalSpace

        val details = mapOf(
            "path" to path,
            "total_space" to formatSize(totalSpace),
            "free_space" to formatSize(freeSpace),
            "usable_space" to formatSize(usableSpace),
            "used_space" to formatSize(usedSpace),
            "usage_ratio" to String.format("%.2f", usageRatio * 100) + "%"
        )

        return when {
            usageRatio >= criticalThreshold -> HealthResult(
                HealthStatus.DOWN,
                details,
                RuntimeException("Disk usage is critical: ${String.format("%.2f", usageRatio * 100)}%")
            )
            usageRatio >= warningThreshold -> HealthResult(
                HealthStatus.DEGRADED,
                details,
                RuntimeException("Disk usage is high: ${String.format("%.2f", usageRatio * 100)}%")
            )
            else -> HealthResult(HealthStatus.UP, details)
        }
    }

    private fun formatSize(size: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024
        val tb = gb * 1024

        return when {
            size >= tb -> String.format("%.2f TB", size.toDouble() / tb)
            size >= gb -> String.format("%.2f GB", size.toDouble() / gb)
            size >= mb -> String.format("%.2f MB", size.toDouble() / mb)
            size >= kb -> String.format("%.2f KB", size.toDouble() / kb)
            else -> "$size bytes"
        }
    }
}
