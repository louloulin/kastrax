# KastraX 遥测注解

## 1. 概述

KastraX 遥测注解提供了一种简单、声明式的方式来为代码添加遥测跟踪功能。通过使用注解，开发者可以轻松地标记需要进行遥测跟踪的方法和类，而无需手动创建和管理跟踪范围。这种方式不仅提高了代码的可读性和可维护性，还与现有的跟踪系统无缝集成。

## 2. 核心组件

### 2.1 WithSpan 注解

`WithSpan` 注解用于标记需要进行遥测跟踪的方法。当带有此注解的方法被调用时，系统会自动创建一个跟踪范围，记录方法的执行情况，并在方法执行完毕后结束跟踪范围。

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithSpan(
    val spanName: String = "",
    val spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
    val skipIfNoTelemetry: Boolean = true,
    val tracerName: String = "default-tracer"
)
```

参数说明：

- `spanName`：跟踪范围名称，默认为方法名
- `spanKind`：跟踪范围类型，默认为 `INTERNAL`
- `skipIfNoTelemetry`：如果没有遥测系统，是否跳过跟踪，默认为 `true`
- `tracerName`：跟踪器名称，默认为 `"default-tracer"`

### 2.2 InstrumentClass 注解

`InstrumentClass` 注解用于标记需要进行遥测跟踪的类。当带有此注解的类中的方法被调用时，系统会自动为每个方法创建跟踪范围，除非该方法被排除。

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InstrumentClass(
    val prefix: String = "",
    val spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
    val excludeMethods: Array<String> = [],
    val tracerName: String = "default-tracer"
)
```

参数说明：

- `prefix`：跟踪范围名称前缀，默认为类名
- `spanKind`：跟踪范围类型，默认为 `INTERNAL`
- `excludeMethods`：排除的方法名列表
- `tracerName`：跟踪器名称，默认为 `"default-tracer"`

### 2.3 TelemetryService

`TelemetryService` 类用于管理遥测系统，包括跟踪器、指标收集器等。它提供了一系列方法来创建跟踪范围、执行带有遥测跟踪的方法，以及拦截带有遥测注解的方法。

```kotlin
class TelemetryService(private val tracer: Tracer) {
    private val tracers = ConcurrentHashMap<String, Tracer>()
    private val aspects = ConcurrentHashMap<String, TelemetryAspect>()

    // 注册跟踪器
    fun registerTracer(name: String, tracer: Tracer)

    // 获取跟踪器
    fun getTracer(name: String = "default-tracer"): Tracer

    // 获取遥测切面
    fun getAspect(name: String = "default-tracer"): TelemetryAspect

    // 创建跟踪范围
    fun createSpan(
        name: String,
        kind: OTelSpanKind = OTelSpanKind.INTERNAL,
        attributes: Map<String, Any> = emptyMap(),
        tracerName: String = "default-tracer"
    ): TraceSpan

    // 执行带有遥测跟踪的方法
    fun <T> executeWithSpan(
        spanName: String,
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        skipIfNoTelemetry: Boolean = true,
        tracerName: String = "default-tracer",
        args: Array<out Any?> = emptyArray(),
        block: () -> T
    ): T

    // 拦截带有 WithSpan 注解的方法
    fun <T> aroundWithSpan(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>,
        tracerName: String = "default-tracer"
    ): T

    // 拦截带有 InstrumentClass 注解的类中的方法
    fun <T> aroundInstrumentClass(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>,
        tracerName: String = "default-tracer"
    ): T
}
```

### 2.4 TelemetryAspect

`TelemetryAspect` 类用于拦截带有遥测注解的方法，添加遥测跟踪功能。它提供了两个主要方法：`aroundWithSpan` 和 `aroundInstrumentClass`，分别用于拦截带有 `WithSpan` 注解的方法和带有 `InstrumentClass` 注解的类中的方法。

```kotlin
class TelemetryAspect(private val tracer: Tracer) {
    // 拦截带有 WithSpan 注解的方法
    fun <T> aroundWithSpan(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>
    ): T

    // 拦截带有 InstrumentClass 注解的类中的方法
    fun <T> aroundInstrumentClass(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>
    ): T
}
```

### 2.5 TelemetryUtils

`TelemetryUtils` 类提供了一系列实用的遥测工具方法，包括检查是否有活动的遥测系统、执行带有遥测跟踪的方法、为类中的所有方法添加遥测跟踪等。

```kotlin
object TelemetryUtils {
    // 检查是否有活动的遥测系统
    fun hasActiveTelemetry(tracerName: String = "default-tracer"): Boolean

    // 执行带有遥测跟踪的方法
    fun <T> executeWithSpan(
        tracer: Tracer,
        spanName: String,
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        skipIfNoTelemetry: Boolean = true,
        tracerName: String = "default-tracer",
        args: Array<out Any?> = emptyArray(),
        block: () -> T
    ): T

    // 为类中的所有方法添加遥测跟踪
    fun instrumentClass(
        klass: KClass<*>,
        tracer: Tracer,
        prefix: String = klass.simpleName ?: "",
        spanKind: OTelSpanKind = OTelSpanKind.INTERNAL,
        excludeMethods: List<String> = emptyList(),
        tracerName: String = "default-tracer"
    )
}
```

## 3. 使用示例

### 3.1 使用 WithSpan 注解

```kotlin
class UserService(private val telemetryService: TelemetryService) {
    @WithSpan(spanName = "getUserById", spanKind = OTelSpanKind.CLIENT)
    fun getUserById(id: String): User {
        // 业务逻辑
        return User(id, "John Doe")
    }
}

// 使用 TelemetryService 拦截带有 WithSpan 注解的方法
val userService = UserService(telemetryService)
val user = telemetryService.aroundWithSpan(
    joinPoint = { userService.getUserById("123") },
    target = userService,
    methodName = "getUserById",
    args = arrayOf("123")
)
```

### 3.2 使用 InstrumentClass 注解

```kotlin
@InstrumentClass(prefix = "OrderService", excludeMethods = ["toString", "equals", "hashCode"])
class OrderService {
    fun createOrder(order: Order): String {
        // 业务逻辑
        return "ORDER-123"
    }

    fun getOrderById(id: String): Order {
        // 业务逻辑
        return Order(id, "Product", 100.0)
    }
}

// 使用 TelemetryService 拦截带有 InstrumentClass 注解的类中的方法
val orderService = OrderService()
val orderId = telemetryService.aroundInstrumentClass(
    joinPoint = { orderService.createOrder(Order("123", "Product", 100.0)) },
    target = orderService,
    methodName = "createOrder",
    args = arrayOf(Order("123", "Product", 100.0))
)
```

### 3.3 手动创建跟踪范围

```kotlin
class PaymentService(private val telemetryService: TelemetryService) {
    fun processPayment(payment: Payment): PaymentResult {
        // 创建跟踪范围
        val span = telemetryService.createSpan(
            name = "processPayment",
            kind = OTelSpanKind.CLIENT,
            attributes = mapOf(
                "payment.id" to payment.id,
                "payment.amount" to payment.amount
            )
        )

        return try {
            // 业务逻辑
            val result = PaymentResult(payment.id, "SUCCESS")
            span.setAttribute("payment.status", "SUCCESS")
            result
        } catch (e: Exception) {
            span.setError(e.message ?: "Unknown error")
            span.recordException(e)
            throw e
        } finally {
            span.end()
        }
    }
}
```

### 3.4 使用 executeWithSpan 方法

```kotlin
class NotificationService(private val telemetryService: TelemetryService) {
    fun sendNotification(notification: Notification): NotificationResult {
        return telemetryService.executeWithSpan(
            spanName = "sendNotification",
            spanKind = OTelSpanKind.CLIENT,
            args = arrayOf(notification)
        ) {
            // 业务逻辑
            NotificationResult(notification.id, "SENT")
        }
    }
}
```

## 4. 最佳实践

### 4.1 注解命名

- 使用有意义的名称来命名跟踪范围，以便在跟踪系统中更容易识别
- 对于 `WithSpan` 注解，如果不指定 `spanName`，则使用方法名作为跟踪范围名称
- 对于 `InstrumentClass` 注解，如果不指定 `prefix`，则使用类名作为跟踪范围名称前缀

### 4.2 注解粒度

- 不要为每个方法都添加注解，只为关键方法或性能敏感的方法添加注解
- 对于包含大量方法的类，考虑使用 `InstrumentClass` 注解，并排除不需要跟踪的方法
- 对于复杂的方法，考虑在方法内部使用手动创建的跟踪范围，以便更精细地控制跟踪

### 4.3 错误处理

- 确保在方法抛出异常时，跟踪范围会记录错误信息
- 使用 `executeWithSpan` 方法可以自动处理异常，并在跟踪范围中记录错误信息
- 对于手动创建的跟踪范围，确保在 `finally` 块中结束跟踪范围

### 4.4 性能考虑

- 遥测跟踪会增加一定的性能开销，因此不要过度使用
- 对于高频调用的方法，考虑使用采样策略，只对部分调用进行跟踪
- 使用 `skipIfNoTelemetry` 参数，在没有遥测系统时跳过跟踪，以减少性能开销

## 5. 扩展和定制

### 5.1 自定义跟踪器

可以通过实现 `Tracer` 接口来创建自定义跟踪器，并将其注册到 `TelemetryService` 中：

```kotlin
class CustomTracer : Tracer {
    // 实现 Tracer 接口的方法
}

val customTracer = CustomTracer()
telemetryService.registerTracer("custom-tracer", customTracer)
```

### 5.2 自定义注解

可以创建自定义注解，并在 `TelemetryAspect` 中添加相应的拦截方法：

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithMetric(
    val metricName: String = "",
    val metricType: MetricType = MetricType.COUNTER
)

class TelemetryAspect(private val tracer: Tracer) {
    // 拦截带有 WithMetric 注解的方法
    fun <T> aroundWithMetric(
        joinPoint: () -> T,
        target: Any,
        methodName: String,
        args: Array<out Any?>
    ): T {
        // 实现拦截逻辑
    }
}
```

### 5.3 集成其他遥测系统

可以通过实现 `Tracer` 接口来集成其他遥测系统，如 Zipkin、Jaeger 等：

```kotlin
class ZipkinTracer : Tracer {
    // 实现 Tracer 接口的方法，集成 Zipkin
}

class JaegerTracer : Tracer {
    // 实现 Tracer 接口的方法，集成 Jaeger
}
```

## 6. 总结

KastraX 遥测注解提供了一种简单、声明式的方式来为代码添加遥测跟踪功能。通过使用 `WithSpan` 和 `InstrumentClass` 注解，开发者可以轻松地标记需要进行遥测跟踪的方法和类，而无需手动创建和管理跟踪范围。`TelemetryService`、`TelemetryAspect` 和 `TelemetryUtils` 类提供了丰富的功能来支持遥测跟踪，包括创建跟踪范围、执行带有遥测跟踪的方法、拦截带有遥测注解的方法等。通过遵循最佳实践并根据需要进行扩展和定制，开发者可以构建强大的遥测系统，提高应用程序的可观测性。
