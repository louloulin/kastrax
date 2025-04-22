# KastraX Health Check System

The Health Check System is an important component of the KastraX Observability module, used to monitor the health status of applications and dependent services. This document provides a detailed introduction to the design, implementation, and usage of the Health Check System.

## 1. Overview

The Health Check System provides a mechanism to check whether various components of an application and its dependent services are functioning properly. It helps developers and operations personnel identify issues promptly, improving system reliability and availability.

The main functions of the Health Check System include:

- Component Health Checks: Checking the health status of internal application components
- Dependency Service Health Checks: Checking the health status of external dependent services
- Health Status Aggregation: Aggregating multiple health check results into an overall health status
- Health Report Generation: Generating detailed health check reports

## 2. Core Components

### 2.1 Health Status (HealthStatus)

Health Status is an enumeration type representing the health status of a component or service:

- `UP`: The component or service is functioning normally
- `DEGRADED`: The component or service is not functioning normally but is still available
- `DOWN`: The component or service is unavailable

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

### 2.2 Health Check Interface (HealthCheck)

The Health Check interface defines the basic behavior of a health check:

```kotlin
interface HealthCheck {
    fun getName(): String
    fun check(): HealthResult
    fun getType(): HealthCheckType = HealthCheckType.COMPONENT
}
```

Health Check Type (HealthCheckType) is used to distinguish different types of health checks:

```kotlin
enum class HealthCheckType {
    COMPONENT,
    DEPENDENCY
}
```

### 2.3 Health Check Result (HealthResult)

The Health Check Result contains health status, detailed information, and error information:

```kotlin
data class HealthResult(
    val status: HealthStatus,
    val details: Map<String, Any> = emptyMap(),
    val error: Throwable? = null
)
```

### 2.4 Health Check Registry (HealthCheckRegistry)

The Health Check Registry is used to manage and execute all registered health checks:

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

### 2.5 Health Check System (HealthCheckSystem)

The Health Check System is a singleton object that provides centralized management of health checks:

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

### 2.6 Health Report (HealthReport)

The Health Report contains the overall health status of the system and the health check results of each component:

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

## 3. Built-in Health Checks

KastraX provides several built-in health check implementations:

### 3.1 Memory Health Check (MemoryHealthCheck)

Checks system memory usage:

```kotlin
class MemoryHealthCheck(
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck
```

### 3.2 Disk Space Health Check (DiskSpaceHealthCheck)

Checks disk space usage for a specified path:

```kotlin
class DiskSpaceHealthCheck(
    private val path: String = ".",
    private val warningThreshold: Double = 0.8, // 80%
    private val criticalThreshold: Double = 0.95 // 95%
) : HealthCheck
```

### 3.3 HTTP Endpoint Health Check (HttpEndpointHealthCheck)

Checks whether a specified URL HTTP endpoint is accessible:

```kotlin
class HttpEndpointHealthCheck(
    private val url: String,
    private val timeout: Duration = Duration.ofSeconds(5),
    private val expectedStatusCode: Int = 200
) : HealthCheck
```

### 3.4 Composite Health Check (CompositeHealthCheck)

Combines multiple health checks into one:

```kotlin
class CompositeHealthCheck(
    private val name: String,
    private val healthChecks: List<HealthCheck>,
    private val type: HealthCheckType = HealthCheckType.COMPONENT
) : HealthCheck
```

## 4. Usage Examples

### 4.1 Registering Health Checks

```kotlin
// Register memory health check
HealthCheckSystem.registerHealthCheck(
    MemoryHealthCheck(
        warningThreshold = 0.7,
        criticalThreshold = 0.9
    )
)

// Register disk space health check
HealthCheckSystem.registerHealthCheck(
    DiskSpaceHealthCheck(
        path = ".",
        warningThreshold = 0.7,
        criticalThreshold = 0.9
    )
)

// Register HTTP endpoint health check
HealthCheckSystem.registerHealthCheck(
    HttpEndpointHealthCheck(
        url = "https://api.example.com/health",
        timeout = Duration.ofSeconds(3)
    )
)
```

### 4.2 Running Health Checks

```kotlin
// Run all health checks
val results = HealthCheckSystem.runHealthChecks()

// Run health checks of a specific type
val componentResults = HealthCheckSystem.runHealthChecks(HealthCheckType.COMPONENT)
val dependencyResults = HealthCheckSystem.runHealthChecks(HealthCheckType.DEPENDENCY)

// Run a specific health check
val memoryResult = HealthCheckSystem.runHealthCheck("memory")
```

### 4.3 Getting Health Status

```kotlin
// Get overall system health status
val status = HealthCheckSystem.getStatus()

// Get health status of a specific type
val componentStatus = HealthCheckSystem.getStatus(HealthCheckType.COMPONENT)
val dependencyStatus = HealthCheckSystem.getStatus(HealthCheckType.DEPENDENCY)
```

### 4.4 Getting Health Reports

```kotlin
// Get health check report
val report = HealthCheckSystem.getHealthReport()

// Check if the system is available
val isAvailable = report.isAvailable()

// Get unhealthy check results
val unhealthyChecks = report.getUnhealthyChecks()

// Convert report to JSON format
val json = report.toJson()
```

## 5. Custom Health Checks

You can create custom health checks by implementing the `HealthCheck` interface:

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

## 6. Best Practices

### 6.1 Health Check Naming

Provide meaningful names for health checks to facilitate identification and management:

```kotlin
override fun getName(): String = "database_${databaseName}"
```

### 6.2 Health Check Types

Choose appropriate types based on the nature of the health check:

- For internal application components, use `HealthCheckType.COMPONENT`
- For external dependent services, use `HealthCheckType.DEPENDENCY`

### 6.3 Detailed Information

Provide detailed information in health check results to facilitate problem diagnosis:

```kotlin
return HealthResult.up(
    mapOf(
        "database" to databaseName,
        "connection_pool_size" to poolSize,
        "active_connections" to activeConnections
    )
)
```

### 6.4 Error Handling

Handle exceptions properly in health checks and include exception information in health check results:

```kotlin
try {
    // Perform health check
} catch (e: Exception) {
    return HealthResult.down(
        mapOf("message" to "Health check failed"),
        e
    )
}
```

### 6.5 Performance Considerations

Health checks should be lightweight and avoid time-consuming operations:

- Set appropriate timeout periods
- Avoid performing complex queries or calculations in health checks
- Consider caching health check results to avoid frequent execution

## 7. Integration and Extension

### 7.1 Integration with Web Frameworks

You can integrate the Health Check System with web frameworks to provide health check API endpoints:

```kotlin
@GetMapping("/health")
fun health(): ResponseEntity<String> {
    val report = HealthCheckSystem.getHealthReport()
    val status = if (report.isAvailable()) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
    return ResponseEntity(report.toJson(), status)
}
```

### 7.2 Integration with Monitoring Systems

You can send health check results to monitoring systems such as Prometheus, Grafana, etc.:

```kotlin
// Regularly perform health checks and update metrics
scheduler.scheduleAtFixedRate({
    val report = HealthCheckSystem.getHealthReport()
    healthStatusGauge.set(when (report.status) {
        HealthStatus.UP -> 2.0
        HealthStatus.DEGRADED -> 1.0
        HealthStatus.DOWN -> 0.0
    })
}, 0, 60, TimeUnit.SECONDS)
```

### 7.3 Integration with Alert Systems

You can trigger alerts based on health check results:

```kotlin
// Regularly perform health checks and trigger alerts
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

## 8. Summary

The KastraX Health Check System provides a flexible, extensible mechanism for monitoring the health status of applications and dependent services. By using the Health Check System, you can:

- Promptly discover and diagnose problems
- Improve system reliability and availability
- Integrate with monitoring and alert systems to achieve automated monitoring
- Provide operations personnel with a visual view of system health status

The Health Check System is an important component of the KastraX Observability module, which, together with the Logging System, Metrics Collection, and Distributed Tracing, forms a complete observability solution.
