package ai.kastrax.observability.examples

import ai.kastrax.observability.ObservabilityConfig
import ai.kastrax.observability.ObservabilitySystem
import ai.kastrax.observability.logging.LogContext
import ai.kastrax.observability.logging.LogLevel
import ai.kastrax.observability.logging.LoggingConfig
import ai.kastrax.observability.logging.LoggingSystem
import ai.kastrax.observability.metrics.MetricsConfig
import ai.kastrax.observability.metrics.MetricsSystem
import ai.kastrax.observability.tracing.OTelSpanKind as SpanKind
import ai.kastrax.observability.tracing.TracingConfig
import ai.kastrax.observability.tracing.TracingSystem
import kotlin.random.Random

/**
 * 可观测性示例应用程序。
 */
fun main() {
    // 配置可观测性系统
    val config = ObservabilityConfig().apply {
        // 配置日志系统
        logging = LoggingConfig().apply {
            level = LogLevel.DEBUG
            console = true
            file = LoggingConfig.FileConfig().apply {
                enabled = true
                directory = "logs"
                filePrefix = "example"
            }
        }

        // 配置指标系统
        metrics = MetricsConfig().apply {
            enabled = true
            console = MetricsConfig.ConsoleReporterConfig().apply {
                enabled = true
                intervalSeconds = 10
            }
        }

        // 配置跟踪系统
        tracing = TracingConfig().apply {
            enabled = true
            serviceName = "example-service"
            samplingRate = 1.0
            exporters.logging.enabled = true
        }
    }

    // 初始化可观测性系统
    ObservabilitySystem.init(config)

    // 获取日志记录器
    val logger = LoggingSystem.getLogger("ExampleApp")

    // 创建指标
    val requestCounter = MetricsSystem.counter(
        name = "example_requests_total",
        description = "Total number of requests",
        tags = mapOf("app" to "example")
    )

    val requestLatency = MetricsSystem.timer(
        name = "example_request_latency",
        description = "Request latency in milliseconds",
        tags = mapOf("app" to "example")
    )

    val activeRequests = MetricsSystem.gauge(
        name = "example_active_requests",
        description = "Number of active requests",
        tags = mapOf("app" to "example")
    )

    val responseSizes = MetricsSystem.histogram(
        name = "example_response_sizes",
        description = "Response sizes in bytes",
        tags = mapOf("app" to "example")
    )

    // 模拟处理请求
    logger.info("Starting example application")

    // 处理 10 个请求
    repeat(10) { requestId ->
        // 使用跟踪系统跟踪请求处理
        TracingSystem.withSpan(
            name = "process_request",
            kind = SpanKind.SERVER,
            attributes = mapOf(
                "request.id" to "req-$requestId",
                "request.type" to "example"
            )
        ) { span ->
            // 增加请求计数器
            requestCounter.increment()

            // 增加活动请求数
            activeRequests.setValue(activeRequests.getValue() + 1)

            try {
                // 使用日志上下文记录请求信息
                LogContext.withContext(
                    mapOf(
                        "requestId" to "req-$requestId",
                        "userId" to "user-${Random.nextInt(1, 100)}"
                    )
                ) {
                    logger.info("Processing request $requestId")

                    // 使用计时器测量请求处理时间
                    requestLatency.start().use {
                        // 模拟请求处理
                        processRequest(requestId, span)
                    }

                    // 记录响应大小
                    val responseSize = Random.nextInt(100, 10000)
                    responseSizes.record(responseSize.toLong())
                    span.setAttribute("response.size", responseSize.toLong())

                    logger.info("Request $requestId processed successfully", mapOf("responseSize" to responseSize))
                }
            } catch (e: Exception) {
                logger.error("Error processing request $requestId", e)
                span.setError("Error processing request: ${e.message}")
                span.recordException(e)
            } finally {
                // 减少活动请求数
                activeRequests.setValue(activeRequests.getValue() - 1)
            }
        }

        // 等待一段时间
        Thread.sleep(Random.nextLong(100, 500))
    }

    logger.info("Example application finished")

    // 等待一段时间，以便指标报告器有时间报告指标
    Thread.sleep(15000)
}

/**
 * 模拟处理请求。
 *
 * @param requestId 请求 ID
 * @param span 跟踪范围
 */
private fun processRequest(requestId: Int, span: ai.kastrax.observability.tracing.TraceSpan) {
    // 获取日志记录器
    val logger = LoggingSystem.getLogger("RequestProcessor")

    // 使用跟踪系统跟踪数据库操作
    TracingSystem.withSpan(
        name = "database_query",
        kind = SpanKind.CLIENT,
        attributes = mapOf("db.type" to "example")
    ) { dbSpan ->
        logger.debug("Executing database query for request $requestId")
        dbSpan.setAttribute("db.statement", "SELECT * FROM example WHERE id = $requestId")

        // 模拟数据库查询
        Thread.sleep(Random.nextLong(50, 200))

        dbSpan.addEvent("db.result", mapOf("rows" to Random.nextInt(1, 10)))
    }

    // 使用跟踪系统跟踪业务逻辑
    TracingSystem.withSpan(
        name = "business_logic",
        attributes = mapOf("business.operation" to "process")
    ) { bizSpan ->
        logger.debug("Executing business logic for request $requestId")

        // 模拟业务逻辑处理
        Thread.sleep(Random.nextLong(50, 200))

        // 随机抛出异常
        if (Random.nextInt(0, 10) == 0) {
            throw RuntimeException("Simulated error in business logic")
        }

        bizSpan.addEvent("business.result", mapOf("status" to "success"))
    }

    // 添加请求处理事件
    span.addEvent("request.processed", mapOf("time" to System.currentTimeMillis()))
}
