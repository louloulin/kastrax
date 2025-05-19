package ai.kastrax.core.agent.monitoring

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.analysis.*
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Agent 性能监控器接口，用于监控 Agent 的性能
 */
interface AgentPerformanceMonitor {
    /**
     * 开始监控 Agent
     *
     * @param agent Agent 实例
     * @param sessionId 会话 ID，如果有的话
     * @param pollingInterval 轮询间隔
     * @return 是否成功开始监控
     */
    suspend fun startMonitoring(
        agent: Agent,
        sessionId: String? = null,
        pollingInterval: Duration = 5.seconds
    ): Boolean

    /**
     * 停止监控 Agent
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return 是否成功停止监控
     */
    suspend fun stopMonitoring(agentId: String, sessionId: String? = null): Boolean

    /**
     * 获取 Agent 性能指标
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return Agent 性能指标
     */
    suspend fun getPerformanceMetrics(agentId: String, sessionId: String? = null): AgentPerformanceMetrics?

    /**
     * 获取 Agent 性能指标流
     *
     * @return Agent 性能指标流
     */
    fun getPerformanceMetricsFlow(): SharedFlow<AgentPerformanceMetrics>

    /**
     * 设置性能阈值
     *
     * @param thresholds 性能阈值
     */
    fun setPerformanceThresholds(thresholds: AgentPerformanceThresholds)

    /**
     * 获取性能阈值
     *
     * @return 性能阈值
     */
    fun getPerformanceThresholds(): AgentPerformanceThresholds

    /**
     * 获取性能警报流
     *
     * @return 性能警报流
     */
    fun getPerformanceAlertsFlow(): SharedFlow<AgentPerformanceAlert>

    /**
     * 获取所有活动监控
     *
     * @return 活动监控列表
     */
    fun getActiveMonitorings(): List<Pair<String, String?>>

    /**
     * 清除所有监控
     */
    suspend fun clearAllMonitorings()
}

/**
 * Agent 性能监控器实现
 *
 * @property metricsCollector Agent 指标收集器
 * @property metricsStorage Agent 指标存储
 */
class AgentPerformanceMonitorImpl(
    private val metricsCollector: AgentMetricsCollector,
    private val metricsStorage: AgentMetricsStorage
) : AgentPerformanceMonitor, KastraXBase(component = "AGENT_PERFORMANCE", name = "monitor") {
    private val monitoringScope = KastraxCoroutineGlobal.getScope("AgentPerformanceMonitor")
    private val monitoringJobs = ConcurrentHashMap<Pair<String, String?>, kotlinx.coroutines.Job>()
    private var performanceThresholds = AgentPerformanceThresholds()

    private val _performanceMetricsFlow = MutableSharedFlow<AgentPerformanceMetrics>(
        replay = 100,
        extraBufferCapacity = 100
    )
    override fun getPerformanceMetricsFlow(): SharedFlow<AgentPerformanceMetrics> = _performanceMetricsFlow.asSharedFlow()

    private val _performanceAlertsFlow = MutableSharedFlow<AgentPerformanceAlert>(
        replay = 100,
        extraBufferCapacity = 100
    )
    override fun getPerformanceAlertsFlow(): SharedFlow<AgentPerformanceAlert> = _performanceAlertsFlow.asSharedFlow()

    override suspend fun startMonitoring(
        agent: Agent,
        sessionId: String?,
        pollingInterval: Duration
    ): Boolean {
        val agentId = agent.name
        val key = Pair(agentId, sessionId)

        if (monitoringJobs.containsKey(key)) {
            logger.warn { "已经在监控 Agent: $agentId, 会话: $sessionId" }
            return false
        }

        // 开始收集指标
        metricsCollector.startAgentExecution(
            agentId = agentId,
            sessionId = sessionId,
            tags = mapOf("monitored" to "true")
        )

        // 启动监控任务
        val job = monitoringScope.launch {
            logger.info { "开始监控 Agent: $agentId, 会话: $sessionId" }

            while (true) {
                try {
                    // 获取最新的指标
                    val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
                    val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

                    if (agentMetrics != null) {
                        // 计算性能指标
                        val performanceMetrics = calculatePerformanceMetrics(agentMetrics, stepMetrics)

                        // 发送性能指标
                        _performanceMetricsFlow.emit(performanceMetrics)

                        // 检查性能阈值并发送警报
                        checkPerformanceThresholds(performanceMetrics)

                        // 如果 Agent 已完成，停止监控
                        if (agentMetrics.status == AgentStatus.COMPLETED || agentMetrics.status == AgentStatus.ERROR) {
                            logger.info { "Agent 已完成，停止监控: $agentId, 会话: $sessionId, 状态: ${agentMetrics.status}" }
                            stopMonitoring(agentId, sessionId)
                            break
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "监控 Agent 时发生错误: $agentId, 会话: $sessionId" }
                }

                delay(pollingInterval)
            }
        }

        monitoringJobs[key] = job as kotlinx.coroutines.Job
        return true
    }

    override suspend fun stopMonitoring(agentId: String, sessionId: String?): Boolean {
        val key = Pair(agentId, sessionId)
        val job = monitoringJobs.remove(key) ?: return false

        job.cancel()
        logger.info { "停止监控 Agent: $agentId, 会话: $sessionId" }

        // 完成指标收集
        metricsCollector.completeAgentExecution(
            agentId = agentId,
            sessionId = sessionId,
            status = AgentStatus.COMPLETED
        )

        return true
    }

    override suspend fun getPerformanceMetrics(agentId: String, sessionId: String?): AgentPerformanceMetrics? {
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId) ?: return null
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

        return calculatePerformanceMetrics(agentMetrics, stepMetrics)
    }

    override fun setPerformanceThresholds(thresholds: AgentPerformanceThresholds) {
        this.performanceThresholds = thresholds
    }

    override fun getPerformanceThresholds(): AgentPerformanceThresholds {
        return performanceThresholds
    }

    override fun getActiveMonitorings(): List<Pair<String, String?>> {
        return monitoringJobs.keys().toList()
    }

    override suspend fun clearAllMonitorings() {
        val keys = monitoringJobs.keys().toList()

        for ((agentId, sessionId) in keys) {
            stopMonitoring(agentId, sessionId)
        }
    }

    /**
     * 计算性能指标
     *
     * @param agentMetrics Agent 指标
     * @param stepMetrics 步骤指标列表
     * @return Agent 性能指标
     */
    private fun calculatePerformanceMetrics(
        agentMetrics: AgentMetrics,
        stepMetrics: List<AgentStepMetrics>
    ): AgentPerformanceMetrics {
        val now = Clock.System.now()
        val duration = agentMetrics.getDuration()
        val stepCount = stepMetrics.size

        // 计算平均步骤持续时间
        val averageStepDuration = if (stepCount > 0) {
            stepMetrics.sumOf { it.getDuration() } / stepCount.toDouble()
        } else {
            0.0
        }

        // 计算每秒步骤数
        val stepsPerSecond = if (duration > 0) {
            stepCount.toDouble() / (duration / 1000.0)
        } else {
            0.0
        }

        // 计算每秒 Token 数
        val tokensPerSecond = if (duration > 0) {
            agentMetrics.totalTokens.toDouble() / (duration / 1000.0)
        } else {
            0.0
        }

        // 计算每秒工具调用数
        val toolCallsPerSecond = if (duration > 0) {
            agentMetrics.toolCalls.toDouble() / (duration / 1000.0)
        } else {
            0.0
        }

        // 计算错误率
        val errorRate = if (stepCount > 0) {
            agentMetrics.errorCount.toDouble() / stepCount
        } else {
            0.0
        }

        // 计算重试率
        val retryRate = if (stepCount > 0) {
            agentMetrics.retryCount.toDouble() / stepCount
        } else {
            0.0
        }

        // 计算 Token 使用率
        val promptTokenRate = if (agentMetrics.totalTokens > 0) {
            agentMetrics.promptTokens.toDouble() / agentMetrics.totalTokens
        } else {
            0.0
        }

        val completionTokenRate = if (agentMetrics.totalTokens > 0) {
            agentMetrics.completionTokens.toDouble() / agentMetrics.totalTokens
        } else {
            0.0
        }

        return AgentPerformanceMetrics(
            agentId = agentMetrics.agentId,
            sessionId = agentMetrics.sessionId,
            timestamp = now,
            status = agentMetrics.status,
            duration = duration,
            stepCount = stepCount,
            averageStepDuration = averageStepDuration,
            stepsPerSecond = stepsPerSecond,
            promptTokens = agentMetrics.promptTokens,
            completionTokens = agentMetrics.completionTokens,
            totalTokens = agentMetrics.totalTokens,
            tokensPerSecond = tokensPerSecond,
            promptTokenRate = promptTokenRate,
            completionTokenRate = completionTokenRate,
            toolCalls = agentMetrics.toolCalls,
            toolCallsPerSecond = toolCallsPerSecond,
            errorCount = agentMetrics.errorCount,
            retryCount = agentMetrics.retryCount,
            errorRate = errorRate,
            retryRate = retryRate
        )
    }

    /**
     * 检查性能阈值并发送警报
     *
     * @param metrics 性能指标
     */
    private suspend fun checkPerformanceThresholds(metrics: AgentPerformanceMetrics) {
        val alerts = mutableListOf<AgentPerformanceAlert>()

        // 检查持续时间
        if (performanceThresholds.maxDurationMs > 0 && metrics.duration > performanceThresholds.maxDurationMs) {
            alerts.add(
                AgentPerformanceAlert(
                    agentId = metrics.agentId,
                    sessionId = metrics.sessionId,
                    timestamp = Clock.System.now(),
                    type = AlertType.DURATION,
                    level = AlertLevel.WARNING,
                    message = "Agent 执行时间过长: ${metrics.duration}ms，超过阈值: ${performanceThresholds.maxDurationMs}ms",
                    metrics = metrics
                )
            )
        }

        // 检查 Token 使用量
        if (performanceThresholds.maxTokens > 0 && metrics.totalTokens > performanceThresholds.maxTokens) {
            alerts.add(
                AgentPerformanceAlert(
                    agentId = metrics.agentId,
                    sessionId = metrics.sessionId,
                    timestamp = Clock.System.now(),
                    type = AlertType.TOKEN_USAGE,
                    level = AlertLevel.WARNING,
                    message = "Token 使用量过高: ${metrics.totalTokens}，超过阈值: ${performanceThresholds.maxTokens}",
                    metrics = metrics
                )
            )
        }

        // 检查工具调用次数
        if (performanceThresholds.maxToolCalls > 0 && metrics.toolCalls > performanceThresholds.maxToolCalls) {
            alerts.add(
                AgentPerformanceAlert(
                    agentId = metrics.agentId,
                    sessionId = metrics.sessionId,
                    timestamp = Clock.System.now(),
                    type = AlertType.TOOL_CALLS,
                    level = AlertLevel.WARNING,
                    message = "工具调用次数过多: ${metrics.toolCalls}，超过阈值: ${performanceThresholds.maxToolCalls}",
                    metrics = metrics
                )
            )
        }

        // 检查错误率
        if (performanceThresholds.maxErrorRate > 0 && metrics.errorRate > performanceThresholds.maxErrorRate) {
            alerts.add(
                AgentPerformanceAlert(
                    agentId = metrics.agentId,
                    sessionId = metrics.sessionId,
                    timestamp = Clock.System.now(),
                    type = AlertType.ERROR_RATE,
                    level = AlertLevel.ERROR,
                    message = "错误率过高: ${String.format("%.2f", metrics.errorRate * 100)}%，超过阈值: ${String.format("%.2f", performanceThresholds.maxErrorRate * 100)}%",
                    metrics = metrics
                )
            )
        }

        // 检查重试率
        if (performanceThresholds.maxRetryRate > 0 && metrics.retryRate > performanceThresholds.maxRetryRate) {
            alerts.add(
                AgentPerformanceAlert(
                    agentId = metrics.agentId,
                    sessionId = metrics.sessionId,
                    timestamp = Clock.System.now(),
                    type = AlertType.RETRY_RATE,
                    level = AlertLevel.WARNING,
                    message = "重试率过高: ${String.format("%.2f", metrics.retryRate * 100)}%，超过阈值: ${String.format("%.2f", performanceThresholds.maxRetryRate * 100)}%",
                    metrics = metrics
                )
            )
        }

        // 发送警报
        for (alert in alerts) {
            _performanceAlertsFlow.emit(alert)
            logger.warn { "性能警报: ${alert.message}" }
        }
    }
}

/**
 * Agent 性能指标
 *
 * @property agentId Agent ID
 * @property sessionId 会话 ID，如果有的话
 * @property timestamp 时间戳
 * @property status Agent 状态
 * @property duration 持续时间（毫秒）
 * @property stepCount 步骤数
 * @property averageStepDuration 平均步骤持续时间（毫秒）
 * @property stepsPerSecond 每秒步骤数
 * @property promptTokens 提示词 Token 数
 * @property completionTokens 完成词 Token 数
 * @property totalTokens 总 Token 数
 * @property tokensPerSecond 每秒 Token 数
 * @property promptTokenRate 提示词 Token 比率
 * @property completionTokenRate 完成词 Token 比率
 * @property toolCalls 工具调用次数
 * @property toolCallsPerSecond 每秒工具调用次数
 * @property errorCount 错误次数
 * @property retryCount 重试次数
 * @property errorRate 错误率
 * @property retryRate 重试率
 */
data class AgentPerformanceMetrics(
    val agentId: String,
    val sessionId: String?,
    val timestamp: Instant,
    val status: AgentStatus,
    val duration: Long,
    val stepCount: Int,
    val averageStepDuration: Double,
    val stepsPerSecond: Double,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val tokensPerSecond: Double,
    val promptTokenRate: Double,
    val completionTokenRate: Double,
    val toolCalls: Int,
    val toolCallsPerSecond: Double,
    val errorCount: Int,
    val retryCount: Int,
    val errorRate: Double,
    val retryRate: Double
)

/**
 * Agent 性能阈值
 *
 * @property maxDurationMs 最大持续时间（毫秒）
 * @property maxTokens 最大 Token 数
 * @property maxToolCalls 最大工具调用次数
 * @property maxErrorRate 最大错误率
 * @property maxRetryRate 最大重试率
 */
data class AgentPerformanceThresholds(
    val maxDurationMs: Long = 60000, // 默认 1 分钟
    val maxTokens: Int = 10000, // 默认 10000 个 Token
    val maxToolCalls: Int = 20, // 默认 20 次工具调用
    val maxErrorRate: Double = 0.2, // 默认 20% 错误率
    val maxRetryRate: Double = 0.3 // 默认 30% 重试率
)

/**
 * Agent 性能警报
 *
 * @property agentId Agent ID
 * @property sessionId 会话 ID，如果有的话
 * @property timestamp 时间戳
 * @property type 警报类型
 * @property level 警报级别
 * @property message 警报消息
 * @property metrics 性能指标
 */
data class AgentPerformanceAlert(
    val agentId: String,
    val sessionId: String?,
    val timestamp: Instant,
    val type: AlertType,
    val level: AlertLevel,
    val message: String,
    val metrics: AgentPerformanceMetrics
)

/**
 * 警报类型
 */
enum class AlertType {
    DURATION, // 持续时间
    TOKEN_USAGE, // Token 使用量
    TOOL_CALLS, // 工具调用
    ERROR_RATE, // 错误率
    RETRY_RATE // 重试率
}

/**
 * 警报级别
 */
enum class AlertLevel {
    INFO, // 信息
    WARNING, // 警告
    ERROR // 错误
}
