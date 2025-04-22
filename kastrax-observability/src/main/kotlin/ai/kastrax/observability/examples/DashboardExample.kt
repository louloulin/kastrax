package ai.kastrax.observability.examples

import ai.kastrax.observability.dashboard.DashboardSystem
import ai.kastrax.observability.dashboard.DashboardType
import ai.kastrax.observability.dashboard.custom.CustomDashboardProvider
import ai.kastrax.observability.dashboard.grafana.GrafanaClient
import ai.kastrax.observability.dashboard.grafana.GrafanaDashboardProvider
import ai.kastrax.observability.logging.LoggingSystem
import java.time.Duration

/**
 * 仪表板示例。
 */
object DashboardExample {
    /**
     * 运行仪表板示例。
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // 获取日志记录器
        val logger = LoggingSystem.getLogger("DashboardExample")

        // 注册自定义仪表板
        logger.info("Registering custom dashboards...")
        val customProvider = CustomDashboardProvider()
        customProvider.registerAllDashboards("prometheus")

        // 显示自定义仪表板
        logger.info("Custom dashboards:")
        val customDashboards = DashboardSystem.getDashboardsByType(DashboardType.CUSTOM)
        customDashboards.forEach { dashboard ->
            logger.info("- ${dashboard.getName()}: ${dashboard.getDescription()} (${dashboard.getUrl()})")
        }

        // 导出自定义仪表板配置
        val customConfig = DashboardSystem.exportConfigsByType(DashboardType.CUSTOM)
        logger.info("Custom dashboard configs: $customConfig")

        // 如果提供了Grafana配置，则注册Grafana仪表板
        val grafanaUrl = System.getenv("GRAFANA_URL") ?: "http://localhost:3000"
        val grafanaApiKey = System.getenv("GRAFANA_API_KEY")

        if (grafanaApiKey != null) {
            logger.info("Registering Grafana dashboards...")
            try {
                val client = GrafanaClient(grafanaUrl, grafanaApiKey, Duration.ofSeconds(5))
                val grafanaProvider = GrafanaDashboardProvider(client)
                grafanaProvider.registerAllDashboards("Prometheus")

                // 显示Grafana仪表板
                logger.info("Grafana dashboards:")
                val grafanaDashboards = DashboardSystem.getDashboardsByType(DashboardType.GRAFANA)
                grafanaDashboards.forEach { dashboard ->
                    logger.info("- ${dashboard.getName()}: ${dashboard.getDescription()} (${dashboard.getUrl()})")
                }

                // 导出Grafana仪表板配置
                val grafanaConfig = DashboardSystem.exportConfigsByType(DashboardType.GRAFANA)
                logger.info("Grafana dashboard configs: $grafanaConfig")
            } catch (e: Exception) {
                logger.error("Failed to register Grafana dashboards", e)
            }
        } else {
            logger.info("Skipping Grafana dashboard registration (GRAFANA_API_KEY not provided)")
        }

        // 显示所有仪表板
        logger.info("All dashboards:")
        val allDashboards = DashboardSystem.getDashboards()
        allDashboards.forEach { (name, dashboard) ->
            logger.info("- $name: ${dashboard.getDescription()} (${dashboard.getType()})")
        }
    }
}
