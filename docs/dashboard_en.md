# KastraX Dashboard System

The Dashboard System is an important component of the KastraX Observability module, used to visualize and monitor the operational status of applications and systems. This document provides a detailed introduction to the design, implementation, and usage of the Dashboard System.

## 1. Overview

The Dashboard System provides a mechanism for creating, managing, and accessing various monitoring dashboards. It supports multiple dashboard types, including Grafana dashboards and custom dashboards, helping developers and operations personnel monitor system status, performance metrics, and health conditions in real-time.

The main functions of the Dashboard System include:

- Dashboard Management: Creating, registering, and managing dashboards
- Grafana Integration: Integration with the Grafana monitoring system
- Custom Dashboards: Support for creating custom dashboards
- Dashboard Templates: Providing commonly used dashboard templates

## 2. Core Components

### 2.1 Dashboard Interface (Dashboard)

The Dashboard interface defines the basic operations of a dashboard:

```kotlin
interface Dashboard {
    fun getName(): String
    fun getDescription(): String
    fun getUrl(): String
    fun getType(): DashboardType
    fun exportConfig(): String
}
```

Dashboard Type (DashboardType) is used to distinguish different types of dashboards:

```kotlin
enum class DashboardType {
    GRAFANA,
    CUSTOM
}
```

### 2.2 Dashboard Manager (DashboardManager)

The Dashboard Manager is used to manage and access dashboards:

```kotlin
class DashboardManager {
    fun register(dashboard: Dashboard): DashboardManager
    fun unregister(name: String): DashboardManager
    fun getDashboards(): Map<String, Dashboard>
    fun getDashboard(name: String): Dashboard?
    fun getDashboardsByType(type: DashboardType): List<Dashboard>
    fun exportAllConfigs(): String
    fun exportConfigsByType(type: DashboardType): String
}
```

### 2.3 Dashboard System (DashboardSystem)

The Dashboard System is a singleton object that provides centralized management of dashboards:

```kotlin
object DashboardSystem {
    fun registerDashboard(dashboard: Dashboard)
    fun unregisterDashboard(name: String)
    fun getDashboards(): Map<String, Dashboard>
    fun getDashboard(name: String): Dashboard?
    fun getDashboardsByType(type: DashboardType): List<Dashboard>
    fun exportAllConfigs(): String
    fun exportConfigsByType(type: DashboardType): String
}
```

## 3. Grafana Integration

### 3.1 Grafana Dashboard (GrafanaDashboard)

The Grafana Dashboard is an integration with the Grafana monitoring system:

```kotlin
class GrafanaDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val uid: String,
    private val config: GrafanaDashboardConfig
) : Dashboard
```

### 3.2 Grafana Dashboard Configuration (GrafanaDashboardConfig)

The Grafana Dashboard Configuration contains detailed configuration for a Grafana dashboard:

```kotlin
data class GrafanaDashboardConfig(
    val title: String,
    val uid: String,
    val tags: List<String> = emptyList(),
    val timezone: String = "browser",
    val panels: List<GrafanaPanel> = emptyList(),
    val variables: List<GrafanaVariable> = emptyList()
)
```

### 3.3 Grafana Panel (GrafanaPanel)

The Grafana Panel is a component of a Grafana dashboard:

```kotlin
data class GrafanaPanel(
    val id: Int,
    val title: String,
    val type: String,
    val datasource: String,
    val targets: List<GrafanaTarget> = emptyList()
)
```

### 3.4 Grafana Client (GrafanaClient)

The Grafana Client is used to interact with the Grafana API:

```kotlin
class GrafanaClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val timeout: Duration = Duration.ofSeconds(10)
)
```

### 3.5 Grafana Dashboard Provider (GrafanaDashboardProvider)

The Grafana Dashboard Provider is used to create and manage Grafana dashboards:

```kotlin
class GrafanaDashboardProvider(
    private val client: GrafanaClient
)
```

## 4. Custom Dashboards

### 4.1 Custom Dashboard (CustomDashboard)

The Custom Dashboard is a type of dashboard that can be used within the application:

```kotlin
class CustomDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val id: String,
    private val config: CustomDashboardConfig
) : Dashboard
```

### 4.2 Custom Dashboard Configuration (CustomDashboardConfig)

The Custom Dashboard Configuration contains detailed configuration for a custom dashboard:

```kotlin
data class CustomDashboardConfig(
    val title: String,
    val id: String,
    val theme: String = "light",
    val refreshInterval: Int = 30,
    val widgets: List<DashboardWidget> = emptyList()
)
```

### 4.3 Dashboard Widget (DashboardWidget)

The Dashboard Widget is a component of a custom dashboard:

```kotlin
data class DashboardWidget(
    val id: String,
    val title: String,
    val type: WidgetType,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val dataSource: String,
    val config: Map<String, Any> = emptyMap()
)
```

### 4.4 Widget Type (WidgetType)

The Widget Type is used to distinguish different types of widgets:

```kotlin
enum class WidgetType {
    CHART,
    TABLE,
    STAT,
    TEXT,
    ALERT
}
```

### 4.5 Custom Dashboard Provider (CustomDashboardProvider)

The Custom Dashboard Provider is used to create and manage custom dashboards:

```kotlin
class CustomDashboardProvider
```

## 5. Usage Examples

### 5.1 Registering Dashboards

```kotlin
// Create a custom dashboard provider
val customProvider = CustomDashboardProvider()

// Register a system monitoring dashboard
val systemDashboard = customProvider.createSystemMonitoringDashboard("prometheus")
DashboardSystem.registerDashboard(systemDashboard)

// Register an application monitoring dashboard
val appDashboard = customProvider.createApplicationMonitoringDashboard("prometheus")
DashboardSystem.registerDashboard(appDashboard)

// Register a health check dashboard
val healthDashboard = customProvider.createHealthCheckDashboard("prometheus")
DashboardSystem.registerDashboard(healthDashboard)
```

### 5.2 Getting Dashboards

```kotlin
// Get all dashboards
val allDashboards = DashboardSystem.getDashboards()

// Get dashboards of a specific type
val customDashboards = DashboardSystem.getDashboardsByType(DashboardType.CUSTOM)
val grafanaDashboards = DashboardSystem.getDashboardsByType(DashboardType.GRAFANA)

// Get a dashboard by name
val systemDashboard = DashboardSystem.getDashboard("system_monitoring")
```

### 5.3 Exporting Dashboard Configurations

```kotlin
// Export all dashboard configurations
val allConfigs = DashboardSystem.exportAllConfigs()

// Export configurations of a specific type
val customConfigs = DashboardSystem.exportConfigsByType(DashboardType.CUSTOM)
val grafanaConfigs = DashboardSystem.exportConfigsByType(DashboardType.GRAFANA)
```

### 5.4 Integrating with Grafana

```kotlin
// Create a Grafana client
val client = GrafanaClient(
    baseUrl = "http://localhost:3000",
    apiKey = "your-api-key",
    timeout = Duration.ofSeconds(5)
)

// Create a Grafana dashboard provider
val grafanaProvider = GrafanaDashboardProvider(client)

// Create a system overview dashboard
val systemDashboard = grafanaProvider.createSystemOverviewDashboard("Prometheus")
DashboardSystem.registerDashboard(systemDashboard)

// Create an application performance dashboard
val appDashboard = grafanaProvider.createApplicationPerformanceDashboard("Prometheus")
DashboardSystem.registerDashboard(appDashboard)

// Create a health check dashboard
val healthDashboard = grafanaProvider.createHealthCheckDashboard("Prometheus")
DashboardSystem.registerDashboard(healthDashboard)
```

## 6. Custom Dashboard Templates

KastraX provides several built-in dashboard templates:

### 6.1 System Monitoring Dashboard

The System Monitoring Dashboard is used to monitor system resource usage:

- CPU usage
- Memory usage
- Disk usage
- Network traffic

### 6.2 Application Monitoring Dashboard

The Application Monitoring Dashboard is used to monitor application performance:

- Request rate
- Response time
- Error rate
- Status code distribution

### 6.3 Health Check Dashboard

The Health Check Dashboard is used to monitor the health status of systems and applications:

- Overall health status
- Health check results
- Health history

## 7. Best Practices

### 7.1 Dashboard Naming

Provide meaningful names for dashboards to facilitate identification and management:

```kotlin
val dashboard = CustomDashboard(
    name = "system_monitoring",
    description = "System monitoring dashboard",
    // ...
)
```

### 7.2 Dashboard Organization

Organize dashboards according to monitoring objectives:

- System monitoring dashboards: Monitor system resources
- Application monitoring dashboards: Monitor application performance
- Business monitoring dashboards: Monitor business metrics

### 7.3 Data Source Configuration

Configure appropriate data sources for dashboards:

```kotlin
val widget = DashboardWidget(
    // ...
    dataSource = "prometheus",
    // ...
)
```

### 7.4 Refresh Interval

Set appropriate refresh intervals based on monitoring requirements:

```kotlin
val config = CustomDashboardConfig(
    // ...
    refreshInterval = 30, // 30 seconds
    // ...
)
```

### 7.5 Widget Layout

Arrange widget layouts reasonably to make dashboards easy to read and understand:

```kotlin
val widget = DashboardWidget(
    // ...
    x = 0,
    y = 0,
    width = 6,
    height = 4,
    // ...
)
```

## 8. Integration and Extension

### 8.1 Integration with Monitoring Systems

You can integrate the Dashboard System with various monitoring systems:

- Prometheus
- InfluxDB
- Datadog
- New Relic

### 8.2 Integration with Alert Systems

You can integrate the Dashboard System with alert systems to achieve automated monitoring and alerting:

```kotlin
// Create an alert widget
val alertWidget = DashboardWidget(
    id = "error_rate_alert",
    title = "Error Rate Alert",
    type = WidgetType.ALERT,
    // ...
    config = mapOf(
        "metric" to "app.errors.rate",
        "threshold" to 0.05,
        "operator" to ">",
        "severity" to "critical"
    )
)
```

### 8.3 Custom Widgets

You can create custom widgets to extend dashboard functionality:

```kotlin
// Create custom widget types
enum class CustomWidgetType {
    HEATMAP,
    GAUGE,
    TIMELINE
}

// Create a custom widget
val heatmapWidget = DashboardWidget(
    id = "cpu_heatmap",
    title = "CPU Heatmap",
    type = WidgetType.CHART,
    // ...
    config = mapOf(
        "chartType" to "heatmap",
        "metric" to "system.cpu.usage",
        "colorScheme" to "warm"
    )
)
```

## 9. Summary

The KastraX Dashboard System provides a flexible, extensible mechanism for creating and managing monitoring dashboards. By using the Dashboard System, you can:

- Monitor the status of systems and applications in real-time
- Visualize performance metrics and health conditions
- Integrate with monitoring systems like Grafana
- Create custom dashboards and widgets

The Dashboard System is an important component of the KastraX Observability module, which, together with the Logging System, Metrics Collection, Health Checks, and Distributed Tracing, forms a complete observability solution.
