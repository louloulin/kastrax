package ai.kastrax.observability.examples

import ai.kastrax.observability.health.HealthCheckSystem
import ai.kastrax.observability.health.HealthStatus
import ai.kastrax.observability.health.checks.CompositeHealthCheck
import ai.kastrax.observability.health.checks.DiskSpaceHealthCheck
import ai.kastrax.observability.health.checks.HttpEndpointHealthCheck
import ai.kastrax.observability.health.checks.MemoryHealthCheck
import ai.kastrax.observability.logging.LoggingSystem
import java.time.Duration

/**
 * 健康检查示例。
 */
object HealthCheckExample {
    /**
     * 运行健康检查示例。
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // 获取日志记录器
        val logger = LoggingSystem.getLogger("HealthCheckExample")

        // 注册健康检查
        logger.info("Registering health checks...")

        // 注册内存健康检查
        HealthCheckSystem.registerHealthCheck(
            MemoryHealthCheck(
                warningThreshold = 0.7,
                criticalThreshold = 0.9
            )
        )

        // 注册磁盘空间健康检查
        HealthCheckSystem.registerHealthCheck(
            DiskSpaceHealthCheck(
                path = ".",
                warningThreshold = 0.7,
                criticalThreshold = 0.9
            )
        )

        // 注册 HTTP 端点健康检查
        HealthCheckSystem.registerHealthCheck(
            HttpEndpointHealthCheck(
                url = "https://www.google.com",
                timeout = Duration.ofSeconds(3)
            )
        )

        // 注册组合健康检查
        HealthCheckSystem.registerHealthCheck(
            CompositeHealthCheck(
                name = "external_services",
                healthChecks = listOf(
                    HttpEndpointHealthCheck(
                        url = "https://www.google.com",
                        timeout = Duration.ofSeconds(3)
                    ),
                    HttpEndpointHealthCheck(
                        url = "https://www.github.com",
                        timeout = Duration.ofSeconds(3)
                    )
                )
            )
        )

        // 运行所有健康检查
        logger.info("Running all health checks...")
        val results = HealthCheckSystem.runHealthChecks()
        results.forEach { (name, result) ->
            logger.info("Health check '$name': ${result.status}")
            if (result.status != HealthStatus.UP) {
                logger.warn("Details: ${result.details}")
                result.error?.let { logger.error("Error: ${it.message}") }
            }
        }

        // 获取系统整体健康状态
        val status = HealthCheckSystem.getStatus()
        logger.info("Overall system health: $status")

        // 获取健康检查报告
        val report = HealthCheckSystem.getHealthReport()
        logger.info("Health report: ${report.toJson()}")

        // 获取不健康的检查结果
        val unhealthyChecks = report.getUnhealthyChecks()
        if (unhealthyChecks.isNotEmpty()) {
            logger.warn("Unhealthy checks: ${unhealthyChecks.keys.joinToString(", ")}")
        } else {
            logger.info("All checks are healthy!")
        }
    }
}
