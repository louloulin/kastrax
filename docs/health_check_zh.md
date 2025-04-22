# KastraX 健康检查系统

健康检查系统是 KastraX 可观测性模块的重要组成部分，用于监控应用程序和依赖服务的健康状态。本文档详细介绍了健康检查系统的设计、实现和使用方法。

## 1. 概述

健康检查系统提供了一种机制，用于检查应用程序的各个组件和依赖服务是否正常运行。它可以帮助开发者和运维人员及时发现问题，提高系统的可靠性和可用性。

健康检查系统的主要功能包括：

- 组件健康检查：检查应用程序内部组件的健康状态
- 依赖服务健康检查：检查外部依赖服务的健康状态
- 健康状态聚合：将多个健康检查结果聚合为整体健康状态
- 健康报告生成：生成详细的健康检查报告

## 2. 核心组件

### 2.1 健康状态（HealthStatus）

健康状态是一个枚举类型，表示组件或服务的健康状态：

- `UP`：组件或服务运行正常
- `DEGRADED`：组件或服务运行不正常，但仍然可用
- `DOWN`：组件或服务不可用

```kotlin
enum class HealthStatus {
    UP,
    DEGRADED,
    DOWN;

    fun isAvailable(): Boolean {
        return this == UP || this == DEGRADED
    }
}
```

### 2.2 健康检查接口（HealthCheck）

健康检查接口定义了健康检查的基本行为：

```kotlin
interface HealthCheck {
    fun getName(): String
    fun check(): HealthResult
    fun getType(): HealthCheckType = HealthCheckType.COMPONENT
}
```

健康检查类型（HealthCheckType）用于区分不同类型的健康检查：

```kotlin
enum class HealthCheckType {
    COMPONENT,
    DEPENDENCY
}
```

### 2.3 健康检查结果（HealthResult）

健康检查结果包含健康状态、详细信息和错误信息：

```kotlin
data class HealthResult(
    val status: HealthStatus,
    val details: Map<String, Any> = emptyMap(),
    val error: Throwable? = null
)
```

### 2.4 健康检查注册表（HealthCheckRegistry）

健康检查注册表用于管理和执行所有注册的健康检查：

```kotlin
class HealthCheckRegistry {
    fun register(healthCheck: HealthCheck): HealthCheckRegistry
    fun unregister(name: String): HealthCheckRegistry
    fun getHealthChecks(): Map<String, HealthCheck>
    fun runHealthChecks(): Map<String, HealthResult>
    fun runHealthChecks(type: HealthCheckType): Map<String, HealthResult>
    fun runHealthCheck(name: String): HealthResult?
    fun getAggregateStatus(): HealthStatus
    fun getAggregateStatus(type: HealthCheckType): HealthStatus
}
```

### 2.5 健康检查系统（HealthCheckSystem）

健康检查系统是一个单例对象，提供了集中管理健康检查的功能：

```kotlin
object HealthCheckSystem {
    fun registerHealthCheck(healthCheck: HealthCheck)
    fun unregisterHealthCheck(name: String)
    fun getHealthChecks(): Map<String, HealthCheck>
    fun runHealthChecks(): Map<String, HealthResult>
    fun runHealthChecks(type: HealthCheckType): Map<String, HealthResult>
    fun runHealthCheck(name: String): HealthResult?
    fun getStatus(): HealthStatus
    fun getStatus(type: HealthCheckType): HealthStatus
    fun getHealthReport(): HealthReport
}
```

### 2.6 健康报告（HealthReport）

健康报告包含系统整体健康状态和各个组件的健康检查结果：

```kotlin
data class HealthReport(
    val status: HealthStatus,
    val checks: Map<String, HealthResult>,
    val timestamp: Instant = Instant.now()
) {
    fun isAvailable(): Boolean
    fun getUnhealthyChecks(): Map<String, HealthResult>
    fun getChecksByType(type: HealthCheckType): Map<String, HealthResult>
    fun toJson(): String
}
```

## 3. 内置健康检查

KastraX 提供了几种内置的健康检查实现：

### 3.1 内存健康检查（MemoryHealthCheck）

检查系统内存使用情况：

```kotlin
class MemoryHealthCheck(
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck
```

### 3.2 磁盘空间健康检查（DiskSpaceHealthCheck）

检查指定路径的磁盘空间使用情况：

```kotlin
class DiskSpaceHealthCheck(
    private val path: String = ".",
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck
```

### 3.3 HTTP 端点健康检查（HttpEndpointHealthCheck）

检查指定 URL 的 HTTP 端点是否可访问：

```kotlin
class HttpEndpointHealthCheck(
    private val url: String,
    private val timeout: Duration = Duration.ofSeconds(5),
    private val expectedStatusCode: Int = 200
) : HealthCheck
```

### 3.4 组合健康检查（CompositeHealthCheck）

将多个健康检查组合成一个：

```kotlin
class CompositeHealthCheck(
    private val name: String,
    private val healthChecks: List<HealthCheck>,
    private val type: HealthCheckType = HealthCheckType.COMPONENT
) : HealthCheck
```

## 4. 使用示例

### 4.1 注册健康检查

```kotlin
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
        url = "https://api.example.com/health",
        timeout = Duration.ofSeconds(3)
    )
)
```

### 4.2 运行健康检查

```kotlin
// 运行所有健康检查
val results = HealthCheckSystem.runHealthChecks()

// 运行特定类型的健康检查
val componentResults = HealthCheckSystem.runHealthChecks(HealthCheckType.COMPONENT)
val dependencyResults = HealthCheckSystem.runHealthChecks(HealthCheckType.DEPENDENCY)

// 运行特定名称的健康检查
val memoryResult = HealthCheckSystem.runHealthCheck("memory")
```

### 4.3 获取健康状态

```kotlin
// 获取系统整体健康状态
val status = HealthCheckSystem.getStatus()

// 获取特定类型的健康状态
val componentStatus = HealthCheckSystem.getStatus(HealthCheckType.COMPONENT)
val dependencyStatus = HealthCheckSystem.getStatus(HealthCheckType.DEPENDENCY)
```

### 4.4 获取健康报告

```kotlin
// 获取健康检查报告
val report = HealthCheckSystem.getHealthReport()

// 检查系统是否可用
val isAvailable = report.isAvailable()

// 获取不健康的检查结果
val unhealthyChecks = report.getUnhealthyChecks()

// 将报告转换为 JSON 格式
val json = report.toJson()
```

## 5. 自定义健康检查

你可以通过实现 `HealthCheck` 接口来创建自定义的健康检查：

```kotlin
class DatabaseHealthCheck(
    private val dataSource: DataSource
) : HealthCheck {
    override fun getName(): String = "database"

    override fun getType(): HealthCheckType = HealthCheckType.DEPENDENCY

    override fun check(): HealthResult {
        try {
            dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                val result = statement.executeQuery("SELECT 1")
                if (result.next() && result.getInt(1) == 1) {
                    return HealthResult.up(mapOf("message" to "Database connection successful"))
                } else {
                    return HealthResult.down(mapOf("message" to "Database query failed"))
                }
            }
        } catch (e: Exception) {
            return HealthResult.down(
                mapOf("message" to "Database connection failed"),
                e
            )
        }
    }
}
```

## 6. 最佳实践

### 6.1 健康检查命名

为健康检查提供有意义的名称，以便于识别和管理：

```kotlin
override fun getName(): String = "database_${databaseName}"
```

### 6.2 健康检查类型

根据健康检查的性质选择适当的类型：

- 对于应用程序内部组件，使用 `HealthCheckType.COMPONENT`
- 对于外部依赖服务，使用 `HealthCheckType.DEPENDENCY`

### 6.3 详细信息

在健康检查结果中提供详细的信息，以便于诊断问题：

```kotlin
return HealthResult.up(
    mapOf(
        "database" to databaseName,
        "connection_pool_size" to poolSize,
        "active_connections" to activeConnections
    )
)
```

### 6.4 错误处理

在健康检查中妥善处理异常，并将异常信息包含在健康检查结果中：

```kotlin
try {
    // 执行健康检查
} catch (e: Exception) {
    return HealthResult.down(
        mapOf("message" to "Health check failed"),
        e
    )
}
```

### 6.5 性能考虑

健康检查应该是轻量级的，避免执行耗时的操作：

- 设置适当的超时时间
- 避免在健康检查中执行复杂的查询或计算
- 考虑缓存健康检查结果，避免频繁执行

## 7. 集成与扩展

### 7.1 与 Web 框架集成

你可以将健康检查系统与 Web 框架集成，提供健康检查 API 端点：

```kotlin
@GetMapping("/health")
fun health(): ResponseEntity<String> {
    val report = HealthCheckSystem.getHealthReport()
    val status = if (report.isAvailable()) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
    return ResponseEntity(report.toJson(), status)
}
```

### 7.2 与监控系统集成

你可以将健康检查结果发送到监控系统，如 Prometheus、Grafana 等：

```kotlin
// 定期执行健康检查并更新指标
scheduler.scheduleAtFixedRate({
    val report = HealthCheckSystem.getHealthReport()
    healthStatusGauge.set(when (report.status) {
        HealthStatus.UP -> 2.0
        HealthStatus.DEGRADED -> 1.0
        HealthStatus.DOWN -> 0.0
    })
}, 0, 60, TimeUnit.SECONDS)
```

### 7.3 与警报系统集成

你可以根据健康检查结果触发警报：

```kotlin
// 定期执行健康检查并触发警报
scheduler.scheduleAtFixedRate({
    val report = HealthCheckSystem.getHealthReport()
    if (report.status == HealthStatus.DOWN) {
        alertSystem.sendAlert(
            "System is DOWN",
            "The system is currently unavailable. Unhealthy checks: ${report.getUnhealthyChecks().keys.joinToString(", ")}"
        )
    } else if (report.status == HealthStatus.DEGRADED) {
        alertSystem.sendAlert(
            "System is DEGRADED",
            "The system is running in degraded mode. Degraded checks: ${report.getUnhealthyChecks().keys.joinToString(", ")}"
        )
    }
}, 0, 5, TimeUnit.MINUTES)
```

## 8. 总结

KastraX 健康检查系统提供了一种灵活、可扩展的机制，用于监控应用程序和依赖服务的健康状态。通过使用健康检查系统，你可以：

- 及时发现和诊断问题
- 提高系统的可靠性和可用性
- 与监控和警报系统集成，实现自动化监控
- 为运维人员提供系统健康状态的可视化视图

健康检查系统是 KastraX 可观测性模块的重要组成部分，与日志系统、指标收集和分布式追踪一起，构成了完整的可观测性解决方案。
