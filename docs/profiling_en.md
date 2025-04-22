# KastraX Profiling System

The Profiling System is an important component of the KastraX Observability module, used to analyze and monitor application performance. This document provides a detailed introduction to the design, implementation, and usage of the Profiling System.

## 1. Overview

The Profiling System provides a mechanism for analyzing application performance characteristics, including execution time, memory usage, and method invocation patterns. It helps developers identify performance bottlenecks, optimize application performance, and improve system reliability and availability.

The main functions of the Profiling System include:

- Execution Time Profiling: Analyzing code execution time
- Memory Usage Profiling: Monitoring memory usage
- Method Invocation Profiling: Analyzing method call patterns
- Performance Bottleneck Detection: Automatically detecting performance bottlenecks

## 2. Core Components

### 2.1 Profiler Interface (Profiler)

The Profiler interface defines the basic operations for performance profiling:

```kotlin
interface Profiler {
    fun startSession(name: String): ProfilingSession
    fun <T> withSession(name: String, block: (ProfilingSession) -> T): T
    fun getName(): String
    fun getType(): ProfilerType
    fun getActiveSessions(): List<ProfilingSession>
    fun getCompletedSessions(): List<ProfilingSession>
    fun clearCompletedSessions()
}
```

Profiler Type (ProfilerType) is used to distinguish different types of profilers:

```kotlin
enum class ProfilerType {
    EXECUTION_TIME,
    MEMORY_USAGE,
    CPU_USAGE,
    METHOD_INVOCATION,
    COMPOSITE
}
```

### 2.2 Profiling Session Interface (ProfilingSession)

The Profiling Session interface defines a session for performance profiling:

```kotlin
interface ProfilingSession {
    fun getId(): String
    fun getName(): String
    fun getStartTime(): Instant
    fun getEndTime(): Instant?
    fun getStatus(): SessionStatus
    fun getTags(): Map<String, String>
    fun addTag(key: String, value: String)
    fun startSubSession(name: String): ProfilingSession
    fun <T> withSubSession(name: String, block: (ProfilingSession) -> T): T
    fun getSubSessions(): List<ProfilingSession>
    fun getMetrics(): Map<String, Any>
    fun addMetric(key: String, value: Any)
    fun recordEvent(name: String, attributes: Map<String, String> = emptyMap())
    fun getEvents(): List<ProfilingEvent>
    fun end(): ProfilingResult
    fun cancel(reason: String)
    fun isEnded(): Boolean
}
```

Session Status (SessionStatus) is used to represent the status of a session:

```kotlin
enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    ERROR
}
```

### 2.3 Profiling Result (ProfilingResult)

The Profiling Result contains detailed information about the profiling:

```kotlin
data class ProfilingResult(
    val sessionId: String,
    val name: String,
    val startTime: Instant,
    val endTime: Instant,
    val duration: Long,
    val status: SessionStatus,
    val tags: Map<String, String>,
    val metrics: Map<String, Any>,
    val events: List<ProfilingEvent>,
    val subSessions: List<ProfilingResult> = emptyList()
)
```

### 2.4 Profiling System (ProfilingSystem)

The Profiling System is a singleton object that provides centralized management of profilers:

```kotlin
object ProfilingSystem {
    fun registerProfiler(profiler: Profiler)
    fun unregisterProfiler(name: String)
    fun getProfilers(): Map<String, Profiler>
    fun getProfiler(name: String): Profiler?
    fun getProfilersByType(type: ProfilerType): List<Profiler>
    fun getDefaultProfiler(): Profiler
    fun setDefaultProfiler(profiler: Profiler)
    fun startSession(name: String, profilerName: String? = null): ProfilingSession
    fun <T> withSession(name: String, profilerName: String? = null, block: (ProfilingSession) -> T): T
    fun detectBottlenecks(result: ProfilingResult, config: BottleneckDetectionConfig = BottleneckDetectionConfig()): BottleneckDetectionResult
    fun shutdown()
}
```

## 3. Built-in Profilers

KastraX provides several built-in profilers:

### 3.1 Execution Time Profiler (ExecutionTimeProfiler)

The Execution Time Profiler is used to analyze code execution time:

```kotlin
class ExecutionTimeProfiler(
    private val name: String = "execution-time-profiler",
    private val maxCompletedSessions: Int = 100
) : Profiler
```

### 3.2 Memory Profiler (MemoryProfiler)

The Memory Profiler is used to monitor memory usage:

```kotlin
class MemoryProfiler(
    private val name: String = "memory-profiler",
    private val samplingIntervalMs: Long = 100,
    private val maxCompletedSessions: Int = 100
) : Profiler
```

### 3.3 Method Invocation Profiler (MethodInvocationProfiler)

The Method Invocation Profiler is used to analyze method call patterns:

```kotlin
class MethodInvocationProfiler(
    private val name: String = "method-invocation-profiler",
    private val maxCompletedSessions: Int = 100
) : Profiler
```

### 3.4 Composite Profiler (CompositeProfiler)

The Composite Profiler combines multiple profilers to provide comprehensive performance profiling:

```kotlin
class CompositeProfiler(
    private val name: String = "composite-profiler",
    private val profilers: List<Profiler> = listOf(
        ExecutionTimeProfiler(),
        MemoryProfiler(),
        MethodInvocationProfiler()
    ),
    private val maxCompletedSessions: Int = 100
) : Profiler
```

## 4. Performance Bottleneck Detection

### 4.1 Bottleneck Detector (BottleneckDetector)

The Bottleneck Detector is used to detect performance bottlenecks:

```kotlin
class BottleneckDetector {
    fun detectBottlenecks(
        result: ProfilingResult,
        config: BottleneckDetectionConfig = BottleneckDetectionConfig()
    ): BottleneckDetectionResult
}
```

### 4.2 Bottleneck Detection Configuration (BottleneckDetectionConfig)

The Bottleneck Detection Configuration is used to configure thresholds for bottleneck detection:

```kotlin
data class BottleneckDetectionConfig(
    val longRunningThresholdMs: Long = 1000,
    val memoryGrowthThresholdBytes: Long = 10 * 1024 * 1024, // 10MB
    val highMemoryUsageThresholdBytes: Long = 100 * 1024 * 1024, // 100MB
    val slowMethodThresholdMs: Long = 100,
    val highInvocationCountThreshold: Int = 1000,
    val lowSuccessRateThreshold: Double = 0.95
)
```

### 4.3 Bottleneck Type (BottleneckType)

The Bottleneck Type is used to distinguish different types of performance bottlenecks:

```kotlin
enum class BottleneckType(val severity: Int) {
    LONG_RUNNING_SESSION(5),
    MEMORY_LEAK(9),
    HIGH_MEMORY_USAGE(7),
    SLOW_METHOD(6),
    HIGH_INVOCATION_COUNT(4),
    LOW_SUCCESS_RATE(8)
}
```

## 5. Usage Examples

### 5.1 Basic Usage

```kotlin
// Use the default profiler for performance profiling
val result = ProfilingSystem.withSession("main-session") { session ->
    // Add session tags
    session.addTag("example", "true")
    session.addTag("type", "demo")

    // Record events
    session.recordEvent("example-started", mapOf("timestamp" to System.currentTimeMillis().toString()))

    // Perform some operations
    performSomeOperation()

    // Record events
    session.recordEvent("example-completed", mapOf("timestamp" to System.currentTimeMillis().toString()))

    "Example completed successfully"
}

// Output profiling results
println("Session duration: ${result.duration}ms")
println("Metrics: ${result.metrics}")
```

### 5.2 Using Sub-sessions

```kotlin
ProfilingSystem.withSession("parent-session") { session ->
    // First sub-session
    session.withSubSession("child-session-1") { child1 ->
        performFirstOperation()
    }
    
    // Second sub-session
    session.withSubSession("child-session-2") { child2 ->
        performSecondOperation()
    }
}
```

### 5.3 Using Specific Profilers

```kotlin
// Use the execution time profiler
ProfilingSystem.withSession("time-session", "execution-time-profiler") { session ->
    performTimeIntensiveOperation()
}

// Use the memory profiler
ProfilingSystem.withSession("memory-session", "memory-profiler") { session ->
    performMemoryIntensiveOperation()
}

// Use the method invocation profiler
ProfilingSystem.withSession("method-session", "method-invocation-profiler") { session ->
    performMethodIntensiveOperation()
}
```

### 5.4 Detecting Performance Bottlenecks

```kotlin
// Get profiling results
val result = ProfilingSystem.withSession("bottleneck-session") {
    performPotentialBottleneckOperation()
}

// Configure bottleneck detection
val config = BottleneckDetectionConfig(
    longRunningThresholdMs = 500,
    memoryGrowthThresholdBytes = 5 * 1024 * 1024, // 5MB
    highMemoryUsageThresholdBytes = 50 * 1024 * 1024, // 50MB
    slowMethodThresholdMs = 50,
    highInvocationCountThreshold = 100,
    lowSuccessRateThreshold = 0.9
)

// Detect performance bottlenecks
val bottleneckResult = ProfilingSystem.detectBottlenecks(result, config)

// Output bottleneck detection results
if (bottleneckResult.hasBottlenecks()) {
    println("Bottlenecks detected:")
    println(bottleneckResult.generateReport())
} else {
    println("No bottlenecks detected")
}
```

### 5.5 Using Annotations for Profiling

```kotlin
@Profiled(name = "annotated-method", tags = ["type:example"])
fun performAnnotatedMethod() {
    // Method implementation
}
```

## 6. Best Practices

### 6.1 Session Naming

Provide meaningful names for sessions to facilitate identification and analysis:

```kotlin
ProfilingSystem.withSession("user-registration-flow") {
    // User registration flow
}
```

### 6.2 Using Tags

Use tags to add metadata to sessions for categorization and filtering:

```kotlin
session.addTag("component", "authentication")
session.addTag("user_id", userId)
session.addTag("environment", "production")
```

### 6.3 Recording Key Events

Record key events to track the execution flow of sessions:

```kotlin
session.recordEvent("validation-started", mapOf("input" to input.toString()))
// Perform validation
session.recordEvent("validation-completed", mapOf("result" to "success"))
```

### 6.4 Using Sub-sessions

Use sub-sessions to break down complex operations for more granular analysis:

```kotlin
session.withSubSession("database-operation") {
    // Database operations
}

session.withSubSession("external-api-call") {
    // External API calls
}
```

### 6.5 Adding Custom Metrics

Add custom metrics for more comprehensive performance analysis:

```kotlin
session.addMetric("items_processed", itemCount)
session.addMetric("cache_hit_ratio", cacheHitRatio)
session.addMetric("response_size_bytes", responseSize)
```

## 7. Integration and Extension

### 7.1 Creating Custom Profilers

You can create custom profilers to meet specific profiling needs:

```kotlin
class CustomProfiler(
    private val name: String = "custom-profiler",
    private val maxCompletedSessions: Int = 100
) : Profiler {
    // Implement Profiler interface methods
}

// Register the custom profiler
ProfilingSystem.registerProfiler(CustomProfiler())
```

### 7.2 Integration with Logging System

You can integrate the Profiling System with the Logging System for more comprehensive application behavior analysis:

```kotlin
ProfilingSystem.withSession("logged-session") { session ->
    val logger = LoggingSystem.getLogger("MyComponent")
    
    logger.info("Starting operation")
    session.recordEvent("operation-started")
    
    // Perform operations
    
    logger.info("Operation completed")
    session.recordEvent("operation-completed")
}
```

### 7.3 Integration with Monitoring Systems

You can send profiling results to monitoring systems for real-time application performance monitoring:

```kotlin
// Periodically perform profiling and send results
scheduler.scheduleAtFixedRate({
    val result = ProfilingSystem.withSession("monitoring-session") {
        // Perform profiling
    }
    
    // Send results to monitoring system
    monitoringSystem.sendMetrics(result.metrics)
}, 0, 1, TimeUnit.MINUTES)
```

### 7.4 Integration with Alert Systems

You can integrate bottleneck detection results with alert systems to promptly discover and resolve performance issues:

```kotlin
// Periodically detect bottlenecks and trigger alerts
scheduler.scheduleAtFixedRate({
    val result = ProfilingSystem.withSession("bottleneck-detection-session") {
        // Perform profiling
    }
    
    val bottleneckResult = ProfilingSystem.detectBottlenecks(result)
    
    if (bottleneckResult.hasBottlenecks()) {
        // Trigger alert
        alertSystem.triggerAlert(
            "Performance bottlenecks detected",
            bottleneckResult.generateReport()
        )
    }
}, 0, 5, TimeUnit.MINUTES)
```

## 8. Summary

The KastraX Profiling System provides a flexible, extensible mechanism for analyzing and monitoring application performance. By using the Profiling System, you can:

- Analyze code execution time to identify performance bottlenecks
- Monitor memory usage to discover memory leaks
- Analyze method call patterns to optimize call frequency
- Automatically detect performance bottlenecks to improve system performance

The Profiling System is an important component of the KastraX Observability module, which, together with the Logging System, Metrics Collection, Health Checks, and Distributed Tracing, forms a complete observability solution.
