package ai.kastrax.observability.dashboard.grafana

import ai.kastrax.observability.dashboard.Dashboard
import ai.kastrax.observability.dashboard.DashboardSystem
import ai.kastrax.observability.logging.LoggingSystem

/**
 * Grafana仪表板提供者。
 * 用于创建和管理Grafana仪表板。
 *
 * @property client Grafana客户端
 */
class GrafanaDashboardProvider(
    private val client: GrafanaClient
) {
    private val logger = LoggingSystem.getLogger("GrafanaDashboardProvider")

    /**
     * 创建系统概览仪表板。
     *
     * @param datasource 数据源名称
     * @return Grafana仪表板
     */
    fun createSystemOverviewDashboard(datasource: String): GrafanaDashboard {
        logger.info("Creating system overview dashboard")

        val panels = listOf(
            GrafanaPanel(
                id = 1,
                title = "CPU Usage",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "system_cpu_usage",
                        legendFormat = "CPU Usage"
                    )
                )
            ),
            GrafanaPanel(
                id = 2,
                title = "Memory Usage",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "system_memory_usage",
                        legendFormat = "Memory Usage"
                    )
                )
            ),
            GrafanaPanel(
                id = 3,
                title = "Disk Usage",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "system_disk_usage",
                        legendFormat = "Disk Usage"
                    )
                )
            )
        )

        val variables = listOf(
            GrafanaVariable(
                name = "instance",
                label = "Instance",
                query = "label_values(instance)"
            )
        )

        val uid = "system-overview"
        val title = "System Overview"
        val config = GrafanaDashboardConfig(
            title = title,
            uid = uid,
            tags = listOf("system", "overview"),
            panels = panels,
            variables = variables
        )

        val dashboard = GrafanaDashboard(
            name = "system_overview",
            description = "System overview dashboard",
            url = "${client.getBaseUrl()}/d/$uid",
            uid = uid,
            config = config
        )

        try {
            val response = client.createDashboard(dashboard)
            logger.info("Created system overview dashboard: $response")
        } catch (e: Exception) {
            logger.error("Failed to create system overview dashboard", e)
        }

        return dashboard
    }

    /**
     * 创建应用性能仪表板。
     *
     * @param datasource 数据源名称
     * @return Grafana仪表板
     */
    fun createApplicationPerformanceDashboard(datasource: String): GrafanaDashboard {
        logger.info("Creating application performance dashboard")

        val panels = listOf(
            GrafanaPanel(
                id = 1,
                title = "Request Rate",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "sum(rate(http_requests_total[1m])) by (instance)",
                        legendFormat = "{{instance}}"
                    )
                )
            ),
            GrafanaPanel(
                id = 2,
                title = "Response Time",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[1m])) by (le, instance))",
                        legendFormat = "{{instance}} - 95th percentile"
                    ),
                    GrafanaTarget(
                        refId = "B",
                        expr = "histogram_quantile(0.5, sum(rate(http_request_duration_seconds_bucket[1m])) by (le, instance))",
                        legendFormat = "{{instance}} - 50th percentile"
                    )
                )
            ),
            GrafanaPanel(
                id = 3,
                title = "Error Rate",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "sum(rate(http_requests_total{status_code=~\"5..\"}[1m])) by (instance) / sum(rate(http_requests_total[1m])) by (instance)",
                        legendFormat = "{{instance}}"
                    )
                )
            )
        )

        val variables = listOf(
            GrafanaVariable(
                name = "instance",
                label = "Instance",
                query = "label_values(http_requests_total, instance)"
            ),
            GrafanaVariable(
                name = "status_code",
                label = "Status Code",
                query = "label_values(http_requests_total, status_code)"
            )
        )

        val uid = "app-performance"
        val title = "Application Performance"
        val config = GrafanaDashboardConfig(
            title = title,
            uid = uid,
            tags = listOf("application", "performance"),
            panels = panels,
            variables = variables
        )

        val dashboard = GrafanaDashboard(
            name = "application_performance",
            description = "Application performance dashboard",
            url = "${client.getBaseUrl()}/d/$uid",
            uid = uid,
            config = config
        )

        try {
            val response = client.createDashboard(dashboard)
            logger.info("Created application performance dashboard: $response")
        } catch (e: Exception) {
            logger.error("Failed to create application performance dashboard", e)
        }

        return dashboard
    }

    /**
     * 创建健康检查仪表板。
     *
     * @param datasource 数据源名称
     * @return Grafana仪表板
     */
    fun createHealthCheckDashboard(datasource: String): GrafanaDashboard {
        logger.info("Creating health check dashboard")

        val panels = listOf(
            GrafanaPanel(
                id = 1,
                title = "Health Status",
                type = "stat",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "health_status",
                        legendFormat = ""
                    )
                )
            ),
            GrafanaPanel(
                id = 2,
                title = "Unhealthy Checks",
                type = "table",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "health_check_status{status!=\"UP\"}",
                        legendFormat = ""
                    )
                )
            ),
            GrafanaPanel(
                id = 3,
                title = "Health Check History",
                type = "graph",
                datasource = datasource,
                targets = listOf(
                    GrafanaTarget(
                        refId = "A",
                        expr = "health_check_status",
                        legendFormat = "{{check_name}}"
                    )
                )
            )
        )

        val variables = listOf(
            GrafanaVariable(
                name = "check_name",
                label = "Check Name",
                query = "label_values(health_check_status, check_name)"
            )
        )

        val uid = "health-checks"
        val title = "Health Checks"
        val config = GrafanaDashboardConfig(
            title = title,
            uid = uid,
            tags = listOf("health", "monitoring"),
            panels = panels,
            variables = variables
        )

        val dashboard = GrafanaDashboard(
            name = "health_checks",
            description = "Health check dashboard",
            url = "${client.getBaseUrl()}/d/$uid",
            uid = uid,
            config = config
        )

        try {
            val response = client.createDashboard(dashboard)
            logger.info("Created health check dashboard: $response")
        } catch (e: Exception) {
            logger.error("Failed to create health check dashboard", e)
        }

        return dashboard
    }

    /**
     * 注册所有仪表板。
     *
     * @param datasource 数据源名称
     */
    fun registerAllDashboards(datasource: String) {
        logger.info("Registering all dashboards")

        val systemDashboard = createSystemOverviewDashboard(datasource)
        val appDashboard = createApplicationPerformanceDashboard(datasource)
        val healthDashboard = createHealthCheckDashboard(datasource)

        DashboardSystem.registerDashboard(systemDashboard)
        DashboardSystem.registerDashboard(appDashboard)
        DashboardSystem.registerDashboard(healthDashboard)

        logger.info("Registered all dashboards")
    }
}

/**
 * 获取Grafana基础URL。
 *
 * @return Grafana基础URL
 */
fun GrafanaClient.getBaseUrl(): String {
    // 使用反射获取私有字段
    val field = this.javaClass.getDeclaredField("baseUrl")
    field.isAccessible = true
    return field.get(this) as String
}
