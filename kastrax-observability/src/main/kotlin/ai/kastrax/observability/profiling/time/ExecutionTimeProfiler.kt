package ai.kastrax.observability.profiling.time

import ai.kastrax.observability.profiling.*
import ai.kastrax.observability.logging.LoggingSystem
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 执行时间分析器。
 * 用于分析代码执行时间。
 *
 * @property name 分析器名称
 * @property maxCompletedSessions 最大保留的已完成会话数量
 */
class ExecutionTimeProfiler(
    private val name: String = "execution-time-profiler",
    private val maxCompletedSessions: Int = 100
) : Profiler {
    private val logger = LoggingSystem.getLogger(ExecutionTimeProfiler::class.java)
    private val activeSessions = ConcurrentHashMap<String, ExecutionTimeSession>()
    private val completedSessions = CopyOnWriteArrayList<ExecutionTimeSession>()

    override fun startSession(name: String): ProfilingSession {
        logger.debug("Starting execution time profiling session: $name")
        val session = ExecutionTimeSession(name, this)
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

    override fun getType(): ProfilerType = ProfilerType.EXECUTION_TIME

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
    internal fun sessionCompleted(session: ExecutionTimeSession) {
        activeSessions.remove(session.getId())
        completedSessions.add(session)
        
        // 如果已完成会话数量超过最大值，则移除最早的会话
        while (completedSessions.size > maxCompletedSessions) {
            completedSessions.removeAt(0)
        }
    }
}

/**
 * 执行时间会话。
 *
 * @property name 会话名称
 * @property profiler 所属的分析器
 * @property parentSession 父会话
 */
class ExecutionTimeSession(
    private val name: String,
    private val profiler: ExecutionTimeProfiler,
    private val parentSession: ExecutionTimeSession? = null
) : ProfilingSession {
    private val id = UUID.randomUUID().toString()
    private val startTime = Instant.now()
    private var endTime: Instant? = null
    private var status = SessionStatus.ACTIVE
    private val tags = ConcurrentHashMap<String, String>()
    private val metrics = ConcurrentHashMap<String, Any>()
    private val events = CopyOnWriteArrayList<ProfilingEvent>()
    private val subSessions = CopyOnWriteArrayList<ExecutionTimeSession>()
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
        val subSession = ExecutionTimeSession(name, profiler, this)
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
