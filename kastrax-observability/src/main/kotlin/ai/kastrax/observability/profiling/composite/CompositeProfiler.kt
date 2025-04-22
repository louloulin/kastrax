package ai.kastrax.observability.profiling.composite

import ai.kastrax.observability.profiling.*
import ai.kastrax.observability.profiling.time.ExecutionTimeProfiler
import ai.kastrax.observability.profiling.memory.MemoryProfiler
import ai.kastrax.observability.profiling.method.MethodInvocationProfiler
import ai.kastrax.observability.logging.LoggingSystem
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 综合性能分析器。
 * 组合多个性能分析器，提供全面的性能分析。
 *
 * @property name 分析器名称
 * @property profilers 包含的分析器列表
 * @property maxCompletedSessions 最大保留的已完成会话数量
 */
class CompositeProfiler(
    private val name: String = "composite-profiler",
    private val profilers: List<Profiler> = listOf(
        ExecutionTimeProfiler(),
        MemoryProfiler(),
        MethodInvocationProfiler()
    ),
    private val maxCompletedSessions: Int = 100
) : Profiler {
    private val logger = LoggingSystem.getLogger(CompositeProfiler::class.java)
    private val activeSessions = ConcurrentHashMap<String, CompositeProfilingSession>()
    private val completedSessions = CopyOnWriteArrayList<CompositeProfilingSession>()

    override fun startSession(name: String): ProfilingSession {
        logger.debug("Starting composite profiling session: $name")
        
        // 在每个分析器中启动会话
        val profilerSessions = profilers.associateWith { it.startSession(name) }
        
        // 创建综合会话
        val session = CompositeProfilingSession(name, this, profilerSessions)
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

    override fun getType(): ProfilerType = ProfilerType.COMPOSITE

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
    internal fun sessionCompleted(session: CompositeProfilingSession) {
        activeSessions.remove(session.getId())
        completedSessions.add(session)
        
        // 如果已完成会话数量超过最大值，则移除最早的会话
        while (completedSessions.size > maxCompletedSessions) {
            completedSessions.removeAt(0)
        }
    }

    /**
     * 关闭分析器。
     */
    fun shutdown() {
        // 关闭所有分析器
        profilers.forEach { profiler ->
            if (profiler is MemoryProfiler) {
                profiler.shutdown()
            }
        }
    }
}

/**
 * 综合性能分析会话。
 *
 * @property name 会话名称
 * @property profiler 所属的分析器
 * @property profilerSessions 各个分析器的会话
 * @property parentSession 父会话
 */
class CompositeProfilingSession(
    private val name: String,
    private val profiler: CompositeProfiler,
    private val profilerSessions: Map<Profiler, ProfilingSession>,
    private val parentSession: CompositeProfilingSession? = null
) : ProfilingSession {
    private val id = UUID.randomUUID().toString()
    private val startTime = Instant.now()
    private var endTime: Instant? = null
    private var status = SessionStatus.ACTIVE
    private val tags = ConcurrentHashMap<String, String>()
    private val metrics = ConcurrentHashMap<String, Any>()
    private val events = CopyOnWriteArrayList<ProfilingEvent>()
    private val subSessions = CopyOnWriteArrayList<CompositeProfilingSession>()
    private var cancelReason: String? = null

    override fun getId(): String = id

    override fun getName(): String = name

    override fun getStartTime(): Instant = startTime

    override fun getEndTime(): Instant? = endTime

    override fun getStatus(): SessionStatus = status

    override fun getTags(): Map<String, String> = tags.toMap()

    override fun addTag(key: String, value: String) {
        tags[key] = value
        
        // 向所有分析器会话添加标签
        profilerSessions.values.forEach { it.addTag(key, value) }
    }

    override fun startSubSession(name: String): ProfilingSession {
        checkActive()
        
        // 在每个分析器中启动子会话
        val subProfilerSessions = profilerSessions.mapValues { (_, session) ->
            session.startSubSession(name)
        }
        
        // 创建综合子会话
        val subSession = CompositeProfilingSession(name, profiler, subProfilerSessions, this)
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
        
        // 向所有分析器会话记录事件
        profilerSessions.values.forEach { it.recordEvent(name, attributes) }
    }

    override fun getEvents(): List<ProfilingEvent> = events.toList()

    override fun end(): ProfilingResult {
        if (isEnded()) {
            return createResult()
        }

        endTime = Instant.now()
        status = SessionStatus.COMPLETED

        // 结束所有分析器会话
        val results = profilerSessions.mapValues { (_, session) -> session.end() }
        
        // 合并所有分析器会话的指标
        results.values.forEach { result ->
            result.metrics.forEach { (key, value) ->
                // 添加前缀以避免冲突
                val prefixedKey = when {
                    key.startsWith("duration") -> key // 保持持续时间指标不变
                    result.name.contains("execution-time") -> "time_$key"
                    result.name.contains("memory") -> "memory_$key"
                    result.name.contains("method") -> "method_$key"
                    else -> key
                }
                metrics[prefixedKey] = value
            }
        }

        // 计算执行时间
        val duration = calculateDuration()
        metrics["duration_ms"] = duration

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

        // 取消所有分析器会话
        profilerSessions.values.forEach { it.cancel(reason) }

        // 如果有父会话，则不需要通知分析器
        if (parentSession == null) {
            profiler.sessionCompleted(this)
        }
    }

    override fun isEnded(): Boolean = status != SessionStatus.ACTIVE

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
