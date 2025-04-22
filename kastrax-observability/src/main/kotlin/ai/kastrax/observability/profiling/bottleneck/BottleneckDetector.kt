package ai.kastrax.observability.profiling.bottleneck

import ai.kastrax.observability.profiling.ProfilingResult
import ai.kastrax.observability.profiling.SessionStatus
import ai.kastrax.observability.logging.LoggingSystem

/**
 * 性能瓶颈检测器。
 * 用于检测性能瓶颈。
 */
class BottleneckDetector {
    private val logger = LoggingSystem.getLogger(BottleneckDetector::class.java)

    /**
     * 检测性能瓶颈。
     *
     * @param result 性能分析结果
     * @param config 瓶颈检测配置
     * @return 瓶颈检测结果
     */
    fun detectBottlenecks(
        result: ProfilingResult,
        config: BottleneckDetectionConfig = BottleneckDetectionConfig()
    ): BottleneckDetectionResult {
        logger.debug("Detecting bottlenecks for session: ${result.name}")
        
        val bottlenecks = mutableListOf<Bottleneck>()
        
        // 如果会话未成功完成，则跳过检测
        if (result.status != SessionStatus.COMPLETED) {
            logger.debug("Session ${result.name} is not completed, skipping bottleneck detection")
            return BottleneckDetectionResult(result.sessionId, result.name, emptyList())
        }
        
        // 检测长时间运行的会话
        if (result.duration > config.longRunningThresholdMs) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.LONG_RUNNING_SESSION,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Session took ${result.duration}ms, which exceeds the threshold of ${config.longRunningThresholdMs}ms",
                    metrics = mapOf("duration_ms" to result.duration)
                )
            )
        }
        
        // 检测内存使用异常
        detectMemoryBottlenecks(result, config, bottlenecks)
        
        // 检测方法调用瓶颈
        detectMethodInvocationBottlenecks(result, config, bottlenecks)
        
        // 递归检测子会话的瓶颈
        result.subSessions.forEach { subResult ->
            val subBottlenecks = detectBottlenecks(subResult, config)
            bottlenecks.addAll(subBottlenecks.bottlenecks)
        }
        
        return BottleneckDetectionResult(result.sessionId, result.name, bottlenecks)
    }

    /**
     * 检测内存使用瓶颈。
     *
     * @param result 性能分析结果
     * @param config 瓶颈检测配置
     * @param bottlenecks 瓶颈列表
     */
    private fun detectMemoryBottlenecks(
        result: ProfilingResult,
        config: BottleneckDetectionConfig,
        bottlenecks: MutableList<Bottleneck>
    ) {
        // 检查内存增长
        val heapGrowth = result.metrics["heap_growth"] as? Long
        if (heapGrowth != null && heapGrowth > config.memoryGrowthThresholdBytes) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.MEMORY_LEAK,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Heap memory grew by ${formatBytes(heapGrowth)}, which exceeds the threshold of ${formatBytes(config.memoryGrowthThresholdBytes)}",
                    metrics = mapOf("heap_growth" to heapGrowth)
                )
            )
        }
        
        // 检查最大内存使用
        val maxHeapUsed = result.metrics["max_heap_used"] as? Long
        if (maxHeapUsed != null && maxHeapUsed > config.highMemoryUsageThresholdBytes) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.HIGH_MEMORY_USAGE,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Maximum heap usage was ${formatBytes(maxHeapUsed)}, which exceeds the threshold of ${formatBytes(config.highMemoryUsageThresholdBytes)}",
                    metrics = mapOf("max_heap_used" to maxHeapUsed)
                )
            )
        }
    }

    /**
     * 检测方法调用瓶颈。
     *
     * @param result 性能分析结果
     * @param config 瓶颈检测配置
     * @param bottlenecks 瓶颈列表
     */
    private fun detectMethodInvocationBottlenecks(
        result: ProfilingResult,
        config: BottleneckDetectionConfig,
        bottlenecks: MutableList<Bottleneck>
    ) {
        // 检查慢方法
        val slowestMethod = result.metrics["slowest_method"] as? String
        val avgMethodDuration = result.metrics["avg_method_duration_ms"] as? Long
        
        if (slowestMethod != null && avgMethodDuration != null && avgMethodDuration > config.slowMethodThresholdMs) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.SLOW_METHOD,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Method $slowestMethod has an average execution time of ${avgMethodDuration}ms, which exceeds the threshold of ${config.slowMethodThresholdMs}ms",
                    metrics = mapOf(
                        "method" to slowestMethod,
                        "avg_duration_ms" to avgMethodDuration
                    )
                )
            )
        }
        
        // 检查高频调用方法
        val mostInvokedMethod = result.metrics["most_invoked_method"] as? String
        val totalMethodInvocations = result.metrics["total_method_invocations"] as? Int
        
        if (mostInvokedMethod != null && totalMethodInvocations != null && totalMethodInvocations > config.highInvocationCountThreshold) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.HIGH_INVOCATION_COUNT,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Method $mostInvokedMethod was invoked $totalMethodInvocations times, which exceeds the threshold of ${config.highInvocationCountThreshold}",
                    metrics = mapOf(
                        "method" to mostInvokedMethod,
                        "invocation_count" to totalMethodInvocations
                    )
                )
            )
        }
        
        // 检查低成功率方法
        val successRate = result.metrics["success_rate"] as? Double
        
        if (successRate != null && successRate < config.lowSuccessRateThreshold) {
            bottlenecks.add(
                Bottleneck(
                    type = BottleneckType.LOW_SUCCESS_RATE,
                    sessionId = result.sessionId,
                    sessionName = result.name,
                    description = "Method success rate is ${String.format("%.2f", successRate * 100)}%, which is below the threshold of ${String.format("%.2f", config.lowSuccessRateThreshold * 100)}%",
                    metrics = mapOf("success_rate" to successRate)
                )
            )
        }
    }

    /**
     * 格式化字节数。
     *
     * @param bytes 字节数
     * @return 格式化后的字符串
     */
    private fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var unitIndex = 0
        
        while (value > 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        
        return String.format("%.2f %s", value, units[unitIndex])
    }
}

/**
 * 瓶颈检测配置类。
 *
 * @property longRunningThresholdMs 长时间运行阈值（毫秒）
 * @property memoryGrowthThresholdBytes 内存增长阈值（字节）
 * @property highMemoryUsageThresholdBytes 高内存使用阈值（字节）
 * @property slowMethodThresholdMs 慢方法阈值（毫秒）
 * @property highInvocationCountThreshold 高调用次数阈值
 * @property lowSuccessRateThreshold 低成功率阈值
 */
data class BottleneckDetectionConfig(
    val longRunningThresholdMs: Long = 1000,
    val memoryGrowthThresholdBytes: Long = 10 * 1024 * 1024, // 10MB
    val highMemoryUsageThresholdBytes: Long = 100 * 1024 * 1024, // 100MB
    val slowMethodThresholdMs: Long = 100,
    val highInvocationCountThreshold: Int = 1000,
    val lowSuccessRateThreshold: Double = 0.95
)

/**
 * 瓶颈检测结果类。
 *
 * @property sessionId 会话ID
 * @property sessionName 会话名称
 * @property bottlenecks 瓶颈列表
 */
data class BottleneckDetectionResult(
    val sessionId: String,
    val sessionName: String,
    val bottlenecks: List<Bottleneck>
) {
    /**
     * 检查是否存在瓶颈。
     *
     * @return 如果存在瓶颈则返回true，否则返回false
     */
    fun hasBottlenecks(): Boolean = bottlenecks.isNotEmpty()

    /**
     * 获取特定类型的瓶颈。
     *
     * @param type 瓶颈类型
     * @return 指定类型的瓶颈列表
     */
    fun getBottlenecksByType(type: BottleneckType): List<Bottleneck> {
        return bottlenecks.filter { it.type == type }
    }

    /**
     * 获取最严重的瓶颈。
     *
     * @return 最严重的瓶颈，如果没有瓶颈则返回null
     */
    fun getMostSevereBottleneck(): Bottleneck? {
        return bottlenecks.maxByOrNull { it.type.severity }
    }

    /**
     * 生成瓶颈报告。
     *
     * @return 瓶颈报告
     */
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("Bottleneck Detection Report for Session: $sessionName ($sessionId)")
        sb.appendLine("Total Bottlenecks Found: ${bottlenecks.size}")
        
        if (bottlenecks.isEmpty()) {
            sb.appendLine("No bottlenecks detected.")
            return sb.toString()
        }
        
        // 按类型分组
        val bottlenecksByType = bottlenecks.groupBy { it.type }
        
        sb.appendLine("\nBottlenecks by Type:")
        bottlenecksByType.forEach { (type, typeBottlenecks) ->
            sb.appendLine("  ${type.name} (${typeBottlenecks.size}):")
            typeBottlenecks.forEach { bottleneck ->
                sb.appendLine("    - ${bottleneck.description}")
            }
        }
        
        // 最严重的瓶颈
        val mostSevere = getMostSevereBottleneck()
        if (mostSevere != null) {
            sb.appendLine("\nMost Severe Bottleneck:")
            sb.appendLine("  Type: ${mostSevere.type.name}")
            sb.appendLine("  Description: ${mostSevere.description}")
            sb.appendLine("  Metrics: ${mostSevere.metrics}")
        }
        
        return sb.toString()
    }
}

/**
 * 瓶颈类。
 *
 * @property type 瓶颈类型
 * @property sessionId 会话ID
 * @property sessionName 会话名称
 * @property description 瓶颈描述
 * @property metrics 瓶颈相关指标
 */
data class Bottleneck(
    val type: BottleneckType,
    val sessionId: String,
    val sessionName: String,
    val description: String,
    val metrics: Map<String, Any> = emptyMap()
)

/**
 * 瓶颈类型枚举。
 *
 * @property severity 严重程度（1-10，10最严重）
 */
enum class BottleneckType(val severity: Int) {
    /**
     * 长时间运行的会话。
     */
    LONG_RUNNING_SESSION(5),

    /**
     * 内存泄漏。
     */
    MEMORY_LEAK(9),

    /**
     * 高内存使用。
     */
    HIGH_MEMORY_USAGE(7),

    /**
     * 慢方法。
     */
    SLOW_METHOD(6),

    /**
     * 高调用次数。
     */
    HIGH_INVOCATION_COUNT(4),

    /**
     * 低成功率。
     */
    LOW_SUCCESS_RATE(8)
}
