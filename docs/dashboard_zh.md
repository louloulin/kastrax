# KastraX 仪表板系统

仪表板系统是 KastraX 可观测性模块的重要组成部分，用于可视化监控应用程序和系统的运行状态。本文档详细介绍了仪表板系统的设计、实现和使用方法。

## 1. 概述

仪表板系统提供了一种机制，用于创建、管理和访问各种监控仪表板。它支持多种仪表板类型，包括 Grafana 仪表板和自定义仪表板，可以帮助开发者和运维人员实时监控系统状态、性能指标和健康状况。

仪表板系统的主要功能包括：

- 仪表板管理：创建、注册和管理仪表板
- Grafana 集成：与 Grafana 监控系统集成
- 自定义仪表板：支持创建自定义仪表板
- 仪表板模板：提供常用的仪表板模板

## 2. 核心组件

### 2.1 仪表板接口（Dashboard）

仪表板接口定义了仪表板的基本操作：

```kotlin
interface Dashboard {
    fun getName(): String
    fun getDescription(): String
    fun getUrl(): String
    fun getType(): DashboardType
    fun exportConfig(): String
}
```

仪表板类型（DashboardType）用于区分不同类型的仪表板：

```kotlin
enum class DashboardType {
    GRAFANA,
    CUSTOM
}
```

### 2.2 仪表板管理器（DashboardManager）

仪表板管理器用于管理和访问仪表板：

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

### 2.3 仪表板系统（DashboardSystem）

仪表板系统是一个单例对象，提供了集中管理仪表板的功能：

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

## 3. Grafana 集成

### 3.1 Grafana 仪表板（GrafanaDashboard）

Grafana 仪表板是对 Grafana 监控系统的集成：

```kotlin
class GrafanaDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val uid: String,
    private val config: GrafanaDashboardConfig
) : Dashboard
```

### 3.2 Grafana 仪表板配置（GrafanaDashboardConfig）

Grafana 仪表板配置包含了 Grafana 仪表板的详细配置：

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

### 3.3 Grafana 面板（GrafanaPanel）

Grafana 面板是 Grafana 仪表板的组成部分：

```kotlin
data class GrafanaPanel(
    val id: Int,
    val title: String,
    val type: String,
    val datasource: String,
    val targets: List<GrafanaTarget> = emptyList()
)
```

### 3.4 Grafana 客户端（GrafanaClient）

Grafana 客户端用于与 Grafana API 交互：

```kotlin
class GrafanaClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val timeout: Duration = Duration.ofSeconds(10)
)
```

### 3.5 Grafana 仪表板提供者（GrafanaDashboardProvider）

Grafana 仪表板提供者用于创建和管理 Grafana 仪表板：

```kotlin
class GrafanaDashboardProvider(
    private val client: GrafanaClient
)
```

## 4. 自定义仪表板

### 4.1 自定义仪表板（CustomDashboard）

自定义仪表板是一种可以在应用程序内部使用的仪表板：

```kotlin
class CustomDashboard(
    private val name: String,
    private val description: String,
    private val url: String,
    private val id: String,
    private val config: CustomDashboardConfig
) : Dashboard
```

### 4.2 自定义仪表板配置（CustomDashboardConfig）

自定义仪表板配置包含了自定义仪表板的详细配置：

```kotlin
data class CustomDashboardConfig(
    val title: String,
    val id: String,
    val theme: String = "light",
    val refreshInterval: Int = 30,
    val widgets: List<DashboardWidget> = emptyList()
)
```

### 4.3 仪表板小部件（DashboardWidget）

仪表板小部件是自定义仪表板的组成部分：

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

### 4.4 小部件类型（WidgetType）

小部件类型用于区分不同类型的小部件：

```kotlin
enum class WidgetType {
    CHART,
    TABLE,
    STAT,
    TEXT,
    ALERT
}
```

### 4.5 自定义仪表板提供者（CustomDashboardProvider）

自定义仪表板提供者用于创建和管理自定义仪表板：

```kotlin
class CustomDashboardProvider
```

## 5. 使用示例

### 5.1 注册仪表板

```kotlin
// 创建自定义仪表板提供者
val customProvider = CustomDashboardProvider()

// 注册系统监控仪表板
val systemDashboard = customProvider.createSystemMonitoringDashboard("prometheus")
DashboardSystem.registerDashboard(systemDashboard)

// 注册应用监控仪表板
val appDashboard = customProvider.createApplicationMonitoringDashboard("prometheus")
DashboardSystem.registerDashboard(appDashboard)

// 注册健康检查仪表板
val healthDashboard = customProvider.createHealthCheckDashboard("prometheus")
DashboardSystem.registerDashboard(healthDashboard)
```

### 5.2 获取仪表板

```kotlin
// 获取所有仪表板
val allDashboards = DashboardSystem.getDashboards()

// 获取特定类型的仪表板
val customDashboards = DashboardSystem.getDashboardsByType(DashboardType.CUSTOM)
val grafanaDashboards = DashboardSystem.getDashboardsByType(DashboardType.GRAFANA)

// 获取特定名称的仪表板
val systemDashboard = DashboardSystem.getDashboard("system_monitoring")
```

### 5.3 导出仪表板配置

```kotlin
// 导出所有仪表板配置
val allConfigs = DashboardSystem.exportAllConfigs()

// 导出特定类型的仪表板配置
val customConfigs = DashboardSystem.exportConfigsByType(DashboardType.CUSTOM)
val grafanaConfigs = DashboardSystem.exportConfigsByType(DashboardType.GRAFANA)
```

### 5.4 与 Grafana 集成

```kotlin
// 创建 Grafana 客户端
val client = GrafanaClient(
    baseUrl = "http://localhost:3000",
    apiKey = "your-api-key",
    timeout = Duration.ofSeconds(5)
)

// 创建 Grafana 仪表板提供者
val grafanaProvider = GrafanaDashboardProvider(client)

// 创建系统概览仪表板
val systemDashboard = grafanaProvider.createSystemOverviewDashboard("Prometheus")
DashboardSystem.registerDashboard(systemDashboard)

// 创建应用性能仪表板
val appDashboard = grafanaProvider.createApplicationPerformanceDashboard("Prometheus")
DashboardSystem.registerDashboard(appDashboard)

// 创建健康检查仪表板
val healthDashboard = grafanaProvider.createHealthCheckDashboard("Prometheus")
DashboardSystem.registerDashboard(healthDashboard)
```

## 6. 自定义仪表板模板

KastraX 提供了几种内置的仪表板模板：

### 6.1 系统监控仪表板

系统监控仪表板用于监控系统资源使用情况：

- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络流量

### 6.2 应用监控仪表板

应用监控仪表板用于监控应用程序性能：

- 请求率
- 响应时间
- 错误率
- 状态码分布

### 6.3 健康检查仪表板

健康检查仪表板用于监控系统和应用程序的健康状态：

- 整体健康状态
- 健康检查结果
- 健康历史记录

## 7. 最佳实践

### 7.1 仪表板命名

为仪表板提供有意义的名称，以便于识别和管理：

```kotlin
val dashboard = CustomDashboard(
    name = "system_monitoring",
    description = "System monitoring dashboard",
    // ...
)
```

### 7.2 仪表板组织

根据监控目标组织仪表板：

- 系统监控仪表板：监控系统资源
- 应用监控仪表板：监控应用程序性能
- 业务监控仪表板：监控业务指标

### 7.3 数据源配置

为仪表板配置适当的数据源：

```kotlin
val widget = DashboardWidget(
    // ...
    dataSource = "prometheus",
    // ...
)
```

### 7.4 刷新间隔

根据监控需求设置适当的刷新间隔：

```kotlin
val config = CustomDashboardConfig(
    // ...
    refreshInterval = 30, // 30 秒
    // ...
)
```

### 7.5 小部件布局

合理安排小部件的布局，使仪表板易于阅读和理解：

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

## 8. 集成与扩展

### 8.1 与监控系统集成

你可以将仪表板系统与各种监控系统集成：

- Prometheus
- InfluxDB
- Datadog
- New Relic

### 8.2 与警报系统集成

你可以将仪表板系统与警报系统集成，实现自动化监控和告警：

```kotlin
// 创建警报小部件
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

### 8.3 自定义小部件

你可以创建自定义小部件，扩展仪表板的功能：

```kotlin
// 创建自定义小部件类型
enum class CustomWidgetType {
    HEATMAP,
    GAUGE,
    TIMELINE
}

// 创建自定义小部件
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

## 9. 总结

KastraX 仪表板系统提供了一种灵活、可扩展的机制，用于创建和管理监控仪表板。通过使用仪表板系统，你可以：

- 实时监控系统和应用程序的状态
- 可视化性能指标和健康状况
- 与 Grafana 等监控系统集成
- 创建自定义仪表板和小部件

仪表板系统是 KastraX 可观测性模块的重要组成部分，与日志系统、指标收集、健康检查和分布式追踪一起，构成了完整的可观测性解决方案。
