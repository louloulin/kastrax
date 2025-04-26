package ai.kastrax.core.agent.analysis

import ai.kastrax.core.agent.AgentStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * 代理行为指标，用于收集和分析代理的行为数据
 *
 * @property agentId 代理ID
 * @property sessionId 会话ID，如果有的话
 * @property startTime 开始时间
 * @property endTime 结束时间，如果已完成
 * @property status 当前状态
 * @property promptTokens 提示词的token数量
 * @property completionTokens 完成词的token数量
 * @property totalTokens 总token数量
 * @property latency 延迟时间（毫秒）
 * @property stepMetrics 步骤指标
 * @property toolCalls 工具调用次数
 * @property errorCount 错误次数
 * @property retryCount 重试次数
 * @property tags 标签
 */
data class AgentMetrics(
    val agentId: String,
    val sessionId: String? = null,
    val startTime: Instant,
    var endTime: Instant? = null,
    var status: AgentStatus = AgentStatus.IDLE,
    var promptTokens: Int = 0,
    var completionTokens: Int = 0,
    var totalTokens: Int = 0,
    var latency: Long = 0,
    val stepMetrics: MutableMap<String, AgentStepMetrics> = mutableMapOf(),
    var toolCalls: Int = 0,
    var errorCount: Int = 0,
    var retryCount: Int = 0,
    val tags: Map<String, String> = emptyMap()
) {
    /**
     * 计算代理执行的持续时间
     * 如果代理仍在运行，则计算从开始时间到现在的持续时间
     *
     * @return 代理执行的持续时间（毫秒）
     */
    fun getDuration(): Long {
        val end = endTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
        return end.toEpochMilliseconds() - startTime.toEpochMilliseconds()
    }

    /**
     * 添加步骤指标
     *
     * @param stepMetric 步骤指标
     */
    fun addStepMetric(stepMetric: AgentStepMetrics) {
        stepMetrics[stepMetric.stepId] = stepMetric
    }

    /**
     * 完成代理指标收集
     *
     * @param status 最终状态
     * @param endTime 结束时间，默认为当前时间
     */
    fun complete(status: AgentStatus, endTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())) {
        this.status = status
        this.endTime = endTime
        this.latency = getDuration()
    }

    /**
     * 增加工具调用次数
     */
    fun incrementToolCalls() {
        toolCalls++
    }

    /**
     * 增加错误次数
     */
    fun incrementErrorCount() {
        errorCount++
    }

    /**
     * 增加重试次数
     */
    fun incrementRetryCount() {
        retryCount++
    }

    /**
     * 更新Token计数
     *
     * @param promptTokens 提示词token数
     * @param completionTokens 完成词token数
     */
    fun updateTokenCounts(promptTokens: Int, completionTokens: Int) {
        this.promptTokens += promptTokens
        this.completionTokens += completionTokens
        this.totalTokens = this.promptTokens + this.completionTokens
    }
}

/**
 * 代理步骤指标，用于收集和分析代理执行步骤的行为数据
 *
 * @property agentId 代理ID
 * @property sessionId 会话ID，如果有的话
 * @property stepId 步骤ID
 * @property stepName 步骤名称
 * @property stepType 步骤类型
 * @property startTime 开始时间
 * @property endTime 结束时间，如果已完成
 * @property status 当前状态
 * @property promptTokens 提示词的token数量
 * @property completionTokens 完成词的token数量
 * @property totalTokens 总token数量
 * @property latency 延迟时间（毫秒）
 * @property toolCalls 工具调用信息
 * @property errorMessage 错误信息，如果有的话
 * @property retryCount 重试次数
 * @property memoryUsage 内存使用量（字节）
 * @property customMetrics 自定义指标
 */
data class AgentStepMetrics(
    val agentId: String,
    val sessionId: String? = null,
    val stepId: String = UUID.randomUUID().toString(),
    val stepName: String,
    val stepType: String,
    val startTime: Instant,
    var endTime: Instant? = null,
    var status: AgentStepStatus = AgentStepStatus.RUNNING,
    var promptTokens: Int = 0,
    var completionTokens: Int = 0,
    var totalTokens: Int = 0,
    var latency: Long = 0,
    val toolCalls: MutableList<ToolCallMetrics> = mutableListOf(),
    var errorMessage: String? = null,
    var retryCount: Int = 0,
    var memoryUsage: Long? = null,
    val customMetrics: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 计算步骤执行的持续时间
     * 如果步骤仍在运行，则计算从开始时间到现在的持续时间
     *
     * @return 步骤执行的持续时间（毫秒）
     */
    fun getDuration(): Long {
        val end = endTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
        return end.toEpochMilliseconds() - startTime.toEpochMilliseconds()
    }

    /**
     * 完成步骤指标收集
     *
     * @param status 最终状态
     * @param endTime 结束时间，默认为当前时间
     * @param errorMessage 错误信息，如果有的话
     */
    fun complete(
        status: AgentStepStatus, 
        endTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()), 
        errorMessage: String? = null
    ) {
        this.status = status
        this.endTime = endTime
        this.errorMessage = errorMessage
        this.latency = getDuration()
    }

    /**
     * 增加重试次数
     */
    fun incrementRetryCount() {
        retryCount++
    }

    /**
     * 添加工具调用指标
     *
     * @param toolCall 工具调用指标
     */
    fun addToolCall(toolCall: ToolCallMetrics) {
        toolCalls.add(toolCall)
    }

    /**
     * 添加自定义指标
     *
     * @param key 指标键
     * @param value 指标值
     */
    fun addCustomMetric(key: String, value: Any) {
        customMetrics[key] = value
    }

    /**
     * 更新Token计数
     *
     * @param promptTokens 提示词token数
     * @param completionTokens 完成词token数
     */
    fun updateTokenCounts(promptTokens: Int, completionTokens: Int) {
        this.promptTokens += promptTokens
        this.completionTokens += completionTokens
        this.totalTokens = this.promptTokens + this.completionTokens
    }
}

/**
 * 工具调用指标，用于收集和分析工具调用的行为数据
 *
 * @property toolId 工具ID
 * @property toolName 工具名称
 * @property startTime 开始时间
 * @property endTime 结束时间，如果已完成
 * @property status 当前状态
 * @property latency 延迟时间（毫秒）
 * @property arguments 参数
 * @property result 结果
 * @property errorMessage 错误信息，如果有的话
 */
data class ToolCallMetrics(
    val toolId: String,
    val toolName: String,
    val startTime: Instant,
    var endTime: Instant? = null,
    var status: ToolCallStatus = ToolCallStatus.RUNNING,
    var latency: Long = 0,
    val arguments: JsonElement? = null,
    var result: JsonElement? = null,
    var errorMessage: String? = null
) {
    /**
     * 计算工具调用的持续时间
     * 如果工具调用仍在运行，则计算从开始时间到现在的持续时间
     *
     * @return 工具调用的持续时间（毫秒）
     */
    fun getDuration(): Long {
        val end = endTime ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())
        return end.toEpochMilliseconds() - startTime.toEpochMilliseconds()
    }

    /**
     * 完成工具调用指标收集
     *
     * @param status 最终状态
     * @param endTime 结束时间，默认为当前时间
     * @param result 结果
     * @param errorMessage 错误信息，如果有的话
     */
    fun complete(
        status: ToolCallStatus, 
        endTime: Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis()), 
        result: JsonElement? = null,
        errorMessage: String? = null
    ) {
        this.status = status
        this.endTime = endTime
        this.result = result
        this.errorMessage = errorMessage
        this.latency = getDuration()
    }
}

/**
 * 代理步骤状态
 */
enum class AgentStepStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED,
    WAITING
}

/**
 * 工具调用状态
 */
enum class ToolCallStatus {
    RUNNING,
    COMPLETED,
    FAILED
}
