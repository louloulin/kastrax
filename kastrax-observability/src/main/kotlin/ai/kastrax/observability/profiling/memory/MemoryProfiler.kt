package ai.kastrax.observability.profiling.memory

import ai.kastrax.observability.profiling.*
import ai.kastrax.observability.logging.LoggingSystem
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 内存使用分析器。
 * 用于分析代码执行过程中的内存使用情况。
 *
 * @property name 分析器名称
 * @property samplingIntervalMs 内存采样间隔（毫秒）
 * @property maxCompletedSessions 最大保留的已完成会话数量
 */
class MemoryProfiler(
    private val name: String = "memory-profiler",
    private val samplingIntervalMs: Long = 100,
    private val maxCompletedSessions: Int = 100
) : Profiler {
    private val logger = LoggingSystem.getLogger(MemoryProfiler::class.java)
    private val activeSessions = ConcurrentHashMap<String, MemoryProfilingSession>()
    private val completedSessions = CopyOnWriteArrayList<MemoryProfilingSession>()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val memoryMXBean = ManagementFactory.getMemoryMXBean()

    init {
        // 启动内存采样任务
        scheduler.scheduleAtFixedRate(
            { sampleMemoryUsage() },
            0,
            samplingIntervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    override fun startSession(name: String): ProfilingSession {
        logger.debug("Starting memory profiling session: $name")
        val session = MemoryProfilingSession(name, this)
        activeSessions[session.getId()] = session
        return session
    }

    override fun <T> withSession(name: String, block: (ProfilingSession) -> T): T {
        val session = startSession(name)
        return try {
            val result = block(session)
            session.end()
            result
        } catch (e: Exception) {
            session.addMetric("error", e.message ?: "Unknown error")
            session.addTag("error", "true")
            session.end()
            throw e
        }
    }

    override fun getName(): String = name

    override fun getType(): ProfilerType = ProfilerType.MEMORY_USAGE

    override fun getActiveSessions(): List<ProfilingSession> = activeSessions.values.toList()

    override fun getCompletedSessions(): List<ProfilingSession> = completedSessions.toList()

    override fun clearCompletedSessions() {
        logger.debug("Clearing completed sessions")
        completedSessions.clear()
    }

    /**
     * 会话完成时调用。
     *
     * @param session 已完成的会话
     */
    internal fun sessionCompleted(session: MemoryProfilingSession) {
        activeSessions.remove(session.getId())
        completedSessions.add(session)
        
        // 如果已完成会话数量超过最大值，则移除最早的会话
        while (completedSessions.size > maxCompletedSessions) {
            completedSessions.removeAt(0)
        }
    }

    /**
     * 采样内存使用情况。
     */
    private fun sampleMemoryUsage() {
        try {
            val heapMemoryUsage = memoryMXBean.heapMemoryUsage
            val nonHeapMemoryUsage = memoryMXBean.nonHeapMemoryUsage
            
            val memoryUsage = mapOf(
                "heap_used" to heapMemoryUsage.used,
                "heap_committed" to heapMemoryUsage.committed,
                "heap_max" to heapMemoryUsage.max,
                "non_heap_used" to nonHeapMemoryUsage.used,
                "non_heap_committed" to nonHeapMemoryUsage.committed,
                "total_used" to (heapMemoryUsage.used + nonHeapMemoryUsage.used)
            )
            
            // 为每个活动会话记录内存使用情况
            activeSessions.values.forEach { session ->
                session.recordMemoryUsage(memoryUsage)
            }
        } catch (e: Exception) {
            logger.error("Error sampling memory usage", e)
        }
    }

    /**
     * 关闭分析器。
     */
    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}

/**
 * 内存分析会话。
 *
 * @property name 会话名称
 * @property profiler 所属的分析器
 * @property parentSession 父会话
 */
class MemoryProfilingSession(
    private val name: String,
    private val profiler: MemoryProfiler,
    private val parentSession: MemoryProfilingSession? = null
) : ProfilingSession {
    private val id = UUID.randomUUID().toString()
    private val startTime = Instant.now()
    private var endTime: Instant? = null
    private var status = SessionStatus.ACTIVE
    private val tags = ConcurrentHashMap<String, String>()
    private val metrics = ConcurrentHashMap<String, Any>()
    private val events = CopyOnWriteArrayList<ProfilingEvent>()
    private val subSessions = CopyOnWriteArrayList<MemoryProfilingSession>()
    private val memoryUsageSamples = CopyOnWriteArrayList<MemoryUsageSample>()
    private var cancelReason: String? = null

    override fun getId(): String = id

    override fun getName(): String = name

    override fun getStartTime(): Instant = startTime

    override fun getEndTime(): Instant? = endTime

    override fun getStatus(): SessionStatus = status

    override fun getTags(): Map<String, String> = tags.toMap()

    override fun addTag(key: String, value: String) {
        tags[key] = value
    }

    override fun startSubSession(name: String): ProfilingSession {
        checkActive()
        val subSession = MemoryProfilingSession(name, profiler, this)
        subSessions.add(subSession)
        return subSession
    }

    override fun <T> withSubSession(name: String, block: (ProfilingSession) -> T): T {
        val subSession = startSubSession(name)
        return try {
            val result = block(subSession)
            subSession.end()
            result
        } catch (e: Exception) {
            subSession.addMetric("error", e.message ?: "Unknown error")
            subSession.addTag("error", "true")
            subSession.end()
            throw e
        }
    }

    override fun getSubSessions(): List<ProfilingSession> = subSessions.toList()

    override fun getMetrics(): Map<String, Any> = metrics.toMap()

    override fun addMetric(key: String, value: Any) {
        metrics[key] = value
    }

    override fun recordEvent(name: String, attributes: Map<String, String>) {
        checkActive()
        val event = ProfilingEvent(name, Instant.now(), attributes)
        events.add(event)
    }

    override fun getEvents(): List<ProfilingEvent> = events.toList()

    override fun end(): ProfilingResult {
        if (isEnded()) {
            return createResult()
        }

        endTime = Instant.now()
        status = SessionStatus.COMPLETED

        // 计算执行时间
        val duration = calculateDuration()
        metrics["duration_ms"] = duration

        // 计算内存使用统计
        calculateMemoryUsageStats()

        // 如果有父会话，则不需要通知分析器
        if (parentSession == null) {
            profiler.sessionCompleted(this)
        }

        return createResult()
    }

    override fun cancel(reason: String) {
        if (isEnded()) {
            return
        }

        endTime = Instant.now()
        status = SessionStatus.CANCELLED
        cancelReason = reason
        metrics["cancel_reason"] = reason

        // 如果有父会话，则不需要通知分析器
        if (parentSession == null) {
            profiler.sessionCompleted(this)
        }
    }

    override fun isEnded(): Boolean = status != SessionStatus.ACTIVE

    /**
     * 记录内存使用情况。
     *
     * @param memoryUsage 内存使用情况
     */
    internal fun recordMemoryUsage(memoryUsage: Map<String, Long>) {
        if (isEnded()) {
            return
        }

        val sample = MemoryUsageSample(
            timestamp = Instant.now(),
            heapUsed = memoryUsage["heap_used"] ?: 0,
            heapCommitted = memoryUsage["heap_committed"] ?: 0,
            heapMax = memoryUsage["heap_max"] ?: 0,
            nonHeapUsed = memoryUsage["non_heap_used"] ?: 0,
            nonHeapCommitted = memoryUsage["non_heap_committed"] ?: 0,
            totalUsed = memoryUsage["total_used"] ?: 0
        )
        
        memoryUsageSamples.add(sample)
    }

    /**
     * 获取内存使用样本。
     *
     * @return 内存使用样本列表
     */
    fun getMemoryUsageSamples(): List<MemoryUsageSample> = memoryUsageSamples.toList()

    /**
     * 检查会话是否处于活动状态，如果不是则抛出异常。
     */
    private fun checkActive() {
        if (isEnded()) {
            throw IllegalStateException("Session is not active: $name")
        }
    }

    /**
     * 计算会话持续时间（毫秒）。
     *
     * @return 持续时间
     */
    private fun calculateDuration(): Long {
        val end = endTime ?: Instant.now()
        return end.toEpochMilli() - startTime.toEpochMilli()
    }

    /**
     * 计算内存使用统计。
     */
    private fun calculateMemoryUsageStats() {
        if (memoryUsageSamples.isEmpty()) {
            return
        }

        // 计算最大、最小和平均内存使用
        var maxHeapUsed = Long.MIN_VALUE
        var minHeapUsed = Long.MAX_VALUE
        var totalHeapUsed = 0L
        
        var maxTotalUsed = Long.MIN_VALUE
        var minTotalUsed = Long.MAX_VALUE
        var totalTotalUsed = 0L

        memoryUsageSamples.forEach { sample ->
            // 堆内存
            maxHeapUsed = maxOf(maxHeapUsed, sample.heapUsed)
            minHeapUsed = minOf(minHeapUsed, sample.heapUsed)
            totalHeapUsed += sample.heapUsed
            
            // 总内存
            maxTotalUsed = maxOf(maxTotalUsed, sample.totalUsed)
            minTotalUsed = minOf(minTotalUsed, sample.totalUsed)
            totalTotalUsed += sample.totalUsed
        }

        val avgHeapUsed = totalHeapUsed / memoryUsageSamples.size
        val avgTotalUsed = totalTotalUsed / memoryUsageSamples.size

        // 记录统计结果
        metrics["memory_samples_count"] = memoryUsageSamples.size
        metrics["max_heap_used"] = maxHeapUsed
        metrics["min_heap_used"] = minHeapUsed
        metrics["avg_heap_used"] = avgHeapUsed
        metrics["max_total_used"] = maxTotalUsed
        metrics["min_total_used"] = minTotalUsed
        metrics["avg_total_used"] = avgTotalUsed
        
        // 计算内存增长
        val firstSample = memoryUsageSamples.first()
        val lastSample = memoryUsageSamples.last()
        val heapGrowth = lastSample.heapUsed - firstSample.heapUsed
        val totalGrowth = lastSample.totalUsed - firstSample.totalUsed
        
        metrics["heap_growth"] = heapGrowth
        metrics["total_growth"] = totalGrowth
    }

    /**
     * 创建会话结果。
     *
     * @return 会话结果
     */
    private fun createResult(): ProfilingResult {
        val end = endTime ?: Instant.now()
        val subResults = subSessions.map { it.end() }
        
        return ProfilingResult(
            sessionId = id,
            name = name,
            startTime = startTime,
            endTime = end,
            duration = calculateDuration(),
            status = status,
            tags = tags.toMap(),
            metrics = metrics.toMap(),
            events = events.toList(),
            subSessions = subResults
        )
    }
}

/**
 * 内存使用样本类。
 *
 * @property timestamp 时间戳
 * @property heapUsed 堆内存使用量
 * @property heapCommitted 堆内存提交量
 * @property heapMax 堆内存最大值
 * @property nonHeapUsed 非堆内存使用量
 * @property nonHeapCommitted 非堆内存提交量
 * @property totalUsed 总内存使用量
 */
data class MemoryUsageSample(
    val timestamp: Instant,
    val heapUsed: Long,
    val heapCommitted: Long,
    val heapMax: Long,
    val nonHeapUsed: Long,
    val nonHeapCommitted: Long,
    val totalUsed: Long
)
