package ai.kastrax.observability.dashboard.custom

import ai.kastrax.observability.dashboard.DashboardSystem
import ai.kastrax.observability.logging.LoggingSystem

/**
 * 自定义仪表板提供者。
 * 用于创建和管理自定义仪表板。
 */
class CustomDashboardProvider {
    private val logger = LoggingSystem.getLogger("CustomDashboardProvider")

    /**
     * 创建系统监控仪表板。
     *
     * @param dataSource 数据源
     * @return 自定义仪表板
     */
    fun createSystemMonitoringDashboard(dataSource: String): CustomDashboard {
        logger.info("Creating system monitoring dashboard")

        val widgets = listOf(
            DashboardWidget(
                id = "cpu",
                title = "CPU Usage",
                type = WidgetType.CHART,
                x = 0,
                y = 0,
                width = 6,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "system.cpu.usage",
                    "chartType" to "line",
                    "timeRange" to "1h"
                )
            ),
            DashboardWidget(
                id = "memory",
                title = "Memory Usage",
                type = WidgetType.CHART,
                x = 6,
                y = 0,
                width = 6,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "system.memory.usage",
                    "chartType" to "line",
                    "timeRange" to "1h"
                )
            ),
            DashboardWidget(
                id = "disk",
                title = "Disk Usage",
                type = WidgetType.STAT,
                x = 0,
                y = 4,
                width = 4,
                height = 3,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "system.disk.usage",
                    "format" to "percent"
                )
            ),
            DashboardWidget(
                id = "network",
                title = "Network Traffic",
                type = WidgetType.CHART,
                x = 4,
                y = 4,
                width = 8,
                height = 3,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "system.network.traffic",
                    "chartType" to "area",
                    "timeRange" to "1h"
                )
            )
        )

        val id = "system-monitoring"
        val config = CustomDashboardConfig(
            title = "System Monitoring",
            id = id,
            refreshInterval = 10,
            widgets = widgets
        )

        return CustomDashboard(
            name = "system_monitoring",
            description = "System monitoring dashboard",
            url = "/dashboards/$id",
            id = id,
            config = config
        )
    }

    /**
     * 创建应用监控仪表板。
     *
     * @param dataSource 数据源
     * @return 自定义仪表板
     */
    fun createApplicationMonitoringDashboard(dataSource: String): CustomDashboard {
        logger.info("Creating application monitoring dashboard")

        val widgets = listOf(
            DashboardWidget(
                id = "requests",
                title = "Request Rate",
                type = WidgetType.CHART,
                x = 0,
                y = 0,
                width = 12,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "app.requests.rate",
                    "chartType" to "line",
                    "timeRange" to "1h"
                )
            ),
            DashboardWidget(
                id = "latency",
                title = "Response Time",
                type = WidgetType.CHART,
                x = 0,
                y = 4,
                width = 6,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "app.response.time",
                    "chartType" to "line",
                    "timeRange" to "1h"
                )
            ),
            DashboardWidget(
                id = "errors",
                title = "Error Rate",
                type = WidgetType.CHART,
                x = 6,
                y = 4,
                width = 6,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "app.errors.rate",
                    "chartType" to "line",
                    "timeRange" to "1h"
                )
            ),
            DashboardWidget(
                id = "status",
                title = "Status Codes",
                type = WidgetType.TABLE,
                x = 0,
                y = 8,
                width = 12,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "app.status.codes",
                    "columns" to listOf("status_code", "count", "percentage")
                )
            )
        )

        val id = "application-monitoring"
        val config = CustomDashboardConfig(
            title = "Application Monitoring",
            id = id,
            refreshInterval = 10,
            widgets = widgets
        )

        return CustomDashboard(
            name = "application_monitoring",
            description = "Application monitoring dashboard",
            url = "/dashboards/$id",
            id = id,
            config = config
        )
    }

    /**
     * 创建健康检查仪表板。
     *
     * @param dataSource 数据源
     * @return 自定义仪表板
     */
    fun createHealthCheckDashboard(dataSource: String): CustomDashboard {
        logger.info("Creating health check dashboard")

        val widgets = listOf(
            DashboardWidget(
                id = "overall",
                title = "Overall Health",
                type = WidgetType.STAT,
                x = 0,
                y = 0,
                width = 12,
                height = 2,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "health.overall",
                    "format" to "status"
                )
            ),
            DashboardWidget(
                id = "checks",
                title = "Health Checks",
                type = WidgetType.TABLE,
                x = 0,
                y = 2,
                width = 12,
                height = 6,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "health.checks",
                    "columns" to listOf("name", "status", "details")
                )
            ),
            DashboardWidget(
                id = "history",
                title = "Health History",
                type = WidgetType.CHART,
                x = 0,
                y = 8,
                width = 12,
                height = 4,
                dataSource = dataSource,
                config = mapOf(
                    "metric" to "health.history",
                    "chartType" to "line",
                    "timeRange" to "1d"
                )
            )
        )

        val id = "health-check"
        val config = CustomDashboardConfig(
            title = "Health Check",
            id = id,
            refreshInterval = 30,
            widgets = widgets
        )

        return CustomDashboard(
            name = "health_check",
            description = "Health check dashboard",
            url = "/dashboards/$id",
            id = id,
            config = config
        )
    }

    /**
     * 注册所有仪表板。
     *
     * @param dataSource 数据源
     */
    fun registerAllDashboards(dataSource: String) {
        logger.info("Registering all custom dashboards")

        val systemDashboard = createSystemMonitoringDashboard(dataSource)
        val appDashboard = createApplicationMonitoringDashboard(dataSource)
        val healthDashboard = createHealthCheckDashboard(dataSource)

        DashboardSystem.registerDashboard(systemDashboard)
        DashboardSystem.registerDashboard(appDashboard)
        DashboardSystem.registerDashboard(healthDashboard)

        logger.info("Registered all custom dashboards")
    }
}
