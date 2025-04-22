package ai.kastrax.observability.examples

import ai.kastrax.observability.profiling.ProfilingSystem
import ai.kastrax.observability.profiling.ProfilingResult
import ai.kastrax.observability.profiling.SessionStatus
import ai.kastrax.observability.profiling.annotation.Profiled
import ai.kastrax.observability.profiling.bottleneck.BottleneckDetectionConfig
import ai.kastrax.observability.logging.LoggingSystem
import java.util.Random
import kotlin.concurrent.thread

/**
 * 性能分析示例。
 */
object ProfilingExample {
    private val logger = LoggingSystem.getLogger("ProfilingExample")
    private val random = Random()

    /**
     * 运行性能分析示例。
     */
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            logger.info("Starting profiling example")

            // 使用默认分析器进行性能分析
            val result = ProfilingSystem.withSession("main-session") { session ->
                // 添加会话标签
                session.addTag("example", "true")
                session.addTag("type", "demo")

                // 记录事件
                session.recordEvent("example-started", mapOf("timestamp" to System.currentTimeMillis().toString()))

                // 执行一些操作
                logger.info("Performing operations...")

                // 子会话：计算密集型操作
                session.withSubSession("compute-intensive") { subSession ->
                    performComputeIntensiveOperation()
                }

                // 子会话：内存密集型操作
                session.withSubSession("memory-intensive") { subSession ->
                    performMemoryIntensiveOperation()
                }

                // 子会话：I/O操作
                session.withSubSession("io-operation") { subSession ->
                    performIOOperation()
                }

                // 记录事件
                session.recordEvent("example-completed", mapOf("timestamp" to System.currentTimeMillis().toString()))

                "Example completed successfully"
            }

            // 输出性能分析结果
            logger.info("Profiling completed with result: $result")

            // 由于result是字符串，我们无法直接获取性能分析结果
            // 在实际应用中，我们应该保存ProfilingResult对象而不是字符串
            // 以下是模拟的性能分析结果
            val simulatedResult = ProfilingResult(
                sessionId = "simulated-session",
                name = "Simulated Session",
                startTime = java.time.Instant.now().minusMillis(1000),
                endTime = java.time.Instant.now(),
                duration = 1000,
                status = SessionStatus.COMPLETED,
                tags = mapOf("example" to "true", "type" to "demo"),
                metrics = mapOf(
                    "duration_ms" to 1000L,
                    "heap_growth" to (5 * 1024 * 1024L),
                    "max_heap_used" to (50 * 1024 * 1024L),
                    "avg_method_duration_ms" to 50L
                ),
                events = emptyList(),
                subSessions = emptyList()
            )

            logger.info("Simulated session duration: ${simulatedResult.duration}ms")
            logger.info("Simulated metrics: ${simulatedResult.metrics}")

            // 检测性能瓶颈
            val bottleneckConfig = BottleneckDetectionConfig(
                longRunningThresholdMs = 500,
                memoryGrowthThresholdBytes = 5 * 1024 * 1024, // 5MB
                highMemoryUsageThresholdBytes = 50 * 1024 * 1024, // 50MB
                slowMethodThresholdMs = 50,
                highInvocationCountThreshold = 100,
                lowSuccessRateThreshold = 0.9
            )

            val bottleneckResult = ProfilingSystem.detectBottlenecks(simulatedResult, bottleneckConfig)

            if (bottleneckResult.hasBottlenecks()) {
                logger.warn("Bottlenecks detected:")
                logger.warn(bottleneckResult.generateReport())
            } else {
                logger.info("No bottlenecks detected")
            }

            // 关闭性能分析系统
            ProfilingSystem.shutdown()

        } catch (e: Exception) {
            logger.error("Error in profiling example", e)
        }
    }

    /**
     * 执行计算密集型操作。
     */
    @Profiled(name = "compute-intensive-method", tags = ["type:compute"])
    private fun performComputeIntensiveOperation() {
        logger.info("Performing compute-intensive operation...")

        // 模拟计算密集型操作
        var result = 0.0
        for (i in 1..10_000_000) {
            result += Math.sin(i.toDouble()) * Math.cos(i.toDouble())
        }

        logger.info("Compute-intensive operation completed, result: $result")
    }

    /**
     * 执行内存密集型操作。
     */
    @Profiled(name = "memory-intensive-method", tags = ["type:memory"])
    private fun performMemoryIntensiveOperation() {
        logger.info("Performing memory-intensive operation...")

        // 模拟内存密集型操作
        val lists = mutableListOf<ByteArray>()
        for (i in 1..10) {
            val size = 1024 * 1024 // 1MB
            val array = ByteArray(size)
            random.nextBytes(array)
            lists.add(array)

            // 模拟一些处理
            Thread.sleep(50)
        }

        logger.info("Memory-intensive operation completed, allocated: ${lists.size * 1024 * 1024} bytes")

        // 清理内存
        lists.clear()
        System.gc()
    }

    /**
     * 执行I/O操作。
     */
    @Profiled(name = "io-operation-method", tags = ["type:io"])
    private fun performIOOperation() {
        logger.info("Performing I/O operation...")

        // 模拟I/O操作
        val threads = mutableListOf<Thread>()

        for (i in 1..5) {
            val t = thread {
                // 模拟网络请求
                Thread.sleep(100 + random.nextInt(200).toLong())
            }
            threads.add(t)
        }

        // 等待所有线程完成
        threads.forEach { it.join() }

        logger.info("I/O operation completed")
    }
}
