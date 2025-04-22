package ai.kastrax.observability.profiling.method

import ai.kastrax.observability.profiling.*
import ai.kastrax.observability.logging.LoggingSystem
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * 方法调用分析器。
 * 用于分析方法调用情况。
 *
 * @property name 分析器名称
 * @property maxCompletedSessions 最大保留的已完成会话数量
 */
class MethodInvocationProfiler(
    private val name: String = "method-invocation-profiler",
    private val maxCompletedSessions: Int = 100
) : Profiler {
    private val logger = LoggingSystem.getLogger(MethodInvocationProfiler::class.java)
    private val activeSessions = ConcurrentHashMap<String, MethodInvocationSession>()
    private val completedSessions = CopyOnWriteArrayList<MethodInvocationSession>()

    override fun startSession(name: String): ProfilingSession {
        logger.debug("Starting method invocation profiling session: $name")
        val session = MethodInvocationSession(name, this)
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

    override fun getType(): ProfilerType = ProfilerType.METHOD_INVOCATION

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
    internal fun sessionCompleted(session: MethodInvocationSession) {
        activeSessions.remove(session.getId())
        completedSessions.add(session)

        // 如果已完成会话数量超过最大值，则移除最早的会话
        while (completedSessions.size > maxCompletedSessions) {
            completedSessions.removeAt(0)
        }
    }
}

/**
 * 方法调用会话。
 *
 * @property name 会话名称
 * @property profiler 所属的分析器
 * @property parentSession 父会话
 */
class MethodInvocationSession(
    private val name: String,
    private val profiler: MethodInvocationProfiler,
    private val parentSession: MethodInvocationSession? = null
) : ProfilingSession {
    private val id = UUID.randomUUID().toString()
    private val startTime = Instant.now()
    private var endTime: Instant? = null
    private var status = SessionStatus.ACTIVE
    private val tags = ConcurrentHashMap<String, String>()
    private val metrics = ConcurrentHashMap<String, Any>()
    private val events = CopyOnWriteArrayList<ProfilingEvent>()
    private val subSessions = CopyOnWriteArrayList<MethodInvocationSession>()
    private val methodInvocations = ConcurrentHashMap<String, MethodInvocationInfo>()
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
        val subSession = MethodInvocationSession(name, profiler, this)
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

        // 计算方法调用统计
        calculateMethodInvocationStats()

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
     * 记录方法调用开始。
     *
     * @param className 类名
     * @param methodName 方法名
     * @return 调用ID
     */
    fun recordMethodStart(className: String, methodName: String): String {
        checkActive()
        val methodKey = "$className.$methodName"
        val invocationId = UUID.randomUUID().toString()

        // 获取或创建方法调用信息
        val invocationInfo = methodInvocations.computeIfAbsent(methodKey) {
            MethodInvocationInfo(className, methodName)
        }

        // 记录调用开始
        invocationInfo.recordInvocationStart(invocationId)

        return invocationId
    }

    /**
     * 记录方法调用结束。
     *
     * @param invocationId 调用ID
     * @param className 类名
     * @param methodName 方法名
     * @param durationMs 持续时间（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误消息
     */
    fun recordMethodEnd(
        invocationId: String,
        className: String,
        methodName: String,
        durationMs: Long,
        success: Boolean = true,
        errorMessage: String? = null
    ) {
        checkActive()
        val methodKey = "$className.$methodName"

        // 获取方法调用信息
        val invocationInfo = methodInvocations[methodKey]

        if (invocationInfo != null) {
            // 记录调用结束
            invocationInfo.recordInvocationEnd(invocationId, durationMs, success, errorMessage)
        } else {
            LoggingSystem.getLogger(MethodInvocationSession::class.java).warn("Method invocation info not found for $methodKey")
        }
    }

    /**
     * 获取方法调用信息。
     *
     * @return 方法调用信息映射
     */
    fun getMethodInvocations(): Map<String, MethodInvocationInfo> = methodInvocations.toMap()

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
     * 计算方法调用统计。
     */
    private fun calculateMethodInvocationStats() {
        if (methodInvocations.isEmpty()) {
            return
        }

        var totalInvocations = 0
        var totalSuccessful = 0
        var totalFailed = 0
        var totalDuration = 0L

        methodInvocations.values.forEach { info ->
            totalInvocations += info.getTotalInvocations()
            totalSuccessful += info.getSuccessfulInvocations()
            totalFailed += info.getFailedInvocations()
            totalDuration += info.getTotalDuration()
        }

        // 记录统计结果
        metrics["total_method_invocations"] = totalInvocations
        metrics["successful_method_invocations"] = totalSuccessful
        metrics["failed_method_invocations"] = totalFailed
        metrics["total_method_duration_ms"] = totalDuration

        if (totalInvocations > 0) {
            metrics["avg_method_duration_ms"] = totalDuration / totalInvocations
            metrics["success_rate"] = totalSuccessful.toDouble() / totalInvocations
        }

        // 找出调用次数最多的方法
        val mostInvokedMethod = methodInvocations.entries
            .maxByOrNull { it.value.getTotalInvocations() }
            ?.key

        if (mostInvokedMethod != null) {
            metrics["most_invoked_method"] = mostInvokedMethod
        }

        // 找出平均耗时最长的方法
        val slowestMethod = methodInvocations.entries
            .filter { it.value.getTotalInvocations() > 0 }
            .maxByOrNull { it.value.getAverageDuration() }
            ?.key

        if (slowestMethod != null) {
            metrics["slowest_method"] = slowestMethod
        }
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
 * 方法调用信息类。
 *
 * @property className 类名
 * @property methodName 方法名
 */
class MethodInvocationInfo(
    val className: String,
    val methodName: String
) {
    private val activeInvocations = ConcurrentHashMap<String, Instant>()
    private val totalInvocations = AtomicLong(0)
    private val successfulInvocations = AtomicLong(0)
    private val failedInvocations = AtomicLong(0)
    private val totalDuration = AtomicLong(0)
    private val minDuration = AtomicLong(Long.MAX_VALUE)
    private val maxDuration = AtomicLong(0)
    private val errors = CopyOnWriteArrayList<String>()

    /**
     * 记录调用开始。
     *
     * @param invocationId 调用ID
     */
    fun recordInvocationStart(invocationId: String) {
        activeInvocations[invocationId] = Instant.now()
    }

    /**
     * 记录调用结束。
     *
     * @param invocationId 调用ID
     * @param durationMs 持续时间（毫秒）
     * @param success 是否成功
     * @param errorMessage 错误消息
     */
    fun recordInvocationEnd(
        invocationId: String,
        durationMs: Long,
        success: Boolean,
        errorMessage: String?
    ) {
        activeInvocations.remove(invocationId)
        totalInvocations.incrementAndGet()

        if (success) {
            successfulInvocations.incrementAndGet()
        } else {
            failedInvocations.incrementAndGet()
            if (errorMessage != null) {
                errors.add(errorMessage)
            }
        }

        totalDuration.addAndGet(durationMs)

        // 更新最小持续时间
        var currentMin = minDuration.get()
        while (durationMs < currentMin) {
            if (minDuration.compareAndSet(currentMin, durationMs)) {
                break
            }
            currentMin = minDuration.get()
        }

        // 更新最大持续时间
        var currentMax = maxDuration.get()
        while (durationMs > currentMax) {
            if (maxDuration.compareAndSet(currentMax, durationMs)) {
                break
            }
            currentMax = maxDuration.get()
        }
    }

    /**
     * 获取总调用次数。
     *
     * @return 总调用次数
     */
    fun getTotalInvocations(): Int = totalInvocations.get().toInt()

    /**
     * 获取成功调用次数。
     *
     * @return 成功调用次数
     */
    fun getSuccessfulInvocations(): Int = successfulInvocations.get().toInt()

    /**
     * 获取失败调用次数。
     *
     * @return 失败调用次数
     */
    fun getFailedInvocations(): Int = failedInvocations.get().toInt()

    /**
     * 获取总持续时间。
     *
     * @return 总持续时间
     */
    fun getTotalDuration(): Long = totalDuration.get()

    /**
     * 获取最小持续时间。
     *
     * @return 最小持续时间
     */
    fun getMinDuration(): Long = if (minDuration.get() == Long.MAX_VALUE) 0 else minDuration.get()

    /**
     * 获取最大持续时间。
     *
     * @return 最大持续时间
     */
    fun getMaxDuration(): Long = maxDuration.get()

    /**
     * 获取平均持续时间。
     *
     * @return 平均持续时间
     */
    fun getAverageDuration(): Long {
        val total = totalInvocations.get()
        return if (total > 0) totalDuration.get() / total else 0
    }

    /**
     * 获取错误消息列表。
     *
     * @return 错误消息列表
     */
    fun getErrors(): List<String> = errors.toList()

    /**
     * 获取活动调用数量。
     *
     * @return 活动调用数量
     */
    fun getActiveInvocations(): Int = activeInvocations.size

    /**
     * 获取成功率。
     *
     * @return 成功率
     */
    fun getSuccessRate(): Double {
        val total = totalInvocations.get()
        return if (total > 0) successfulInvocations.get().toDouble() / total else 0.0
    }
}
