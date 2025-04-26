package ai.kastrax.core.agent.analysis

import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.common.KastraXBase
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement
import java.util.UUID

/**
 * 代理指标收集器，用于收集和管理代理行为指标
 *
 * @property metricsStorage 指标存储
 */
class AgentMetricsCollector(
    private val metricsStorage: AgentMetricsStorage
) : KastraXBase(component = "AGENT_METRICS", name = "collector") {

    /**
     * 开始跟踪代理执行
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param tags 可选标签
     * @return 代理指标
     */
    suspend fun startAgentExecution(
        agentId: String,
        sessionId: String? = null,
        tags: Map<String, String> = emptyMap()
    ): AgentMetrics {
        val metrics = AgentMetrics(
            agentId = agentId,
            sessionId = sessionId,
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            tags = tags
        )

        metricsStorage.saveAgentMetrics(metrics)
        logger.debug { "开始跟踪代理执行: $agentId, 会话: $sessionId" }
        return metrics
    }

    /**
     * 完成代理执行
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param status 最终状态
     * @return 更新后的代理指标，如果不存在则返回null
     */
    suspend fun completeAgentExecution(
        agentId: String,
        sessionId: String? = null,
        status: AgentStatus
    ): AgentMetrics? {
        val metrics = metricsStorage.getAgentMetrics(agentId, sessionId) ?: return null

        metrics.complete(status)
        metricsStorage.saveAgentMetrics(metrics)
        logger.debug { "完成代理执行: $agentId, 会话: $sessionId, 状态: $status" }
        return metrics
    }

    /**
     * 开始跟踪代理步骤执行
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepName 步骤名称
     * @param stepType 步骤类型
     * @return 代理步骤指标
     */
    suspend fun startStep(
        agentId: String,
        sessionId: String? = null,
        stepName: String,
        stepType: String
    ): AgentStepMetrics {
        val stepId = UUID.randomUUID().toString()
        val metrics = AgentStepMetrics(
            agentId = agentId,
            sessionId = sessionId,
            stepId = stepId,
            stepName = stepName,
            stepType = stepType,
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )

        metricsStorage.saveStepMetrics(metrics)
        logger.debug { "开始跟踪代理步骤: $stepName, 代理: $agentId, 会话: $sessionId" }
        return metrics
    }

    /**
     * 完成代理步骤执行
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepId 步骤ID
     * @param status 最终状态
     * @param errorMessage 错误信息，如果有的话
     * @param memoryUsage 内存使用量（字节）
     * @param customMetrics 自定义指标
     * @return 更新后的代理步骤指标，如果不存在则返回null
     */
    suspend fun completeStep(
        agentId: String,
        sessionId: String? = null,
        stepId: String,
        status: AgentStepStatus,
        errorMessage: String? = null,
        memoryUsage: Long? = null,
        customMetrics: Map<String, Any> = emptyMap()
    ): AgentStepMetrics? {
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)
            .find { it.stepId == stepId } ?: return null

        stepMetrics.complete(status, Instant.fromEpochMilliseconds(System.currentTimeMillis()), errorMessage)
        stepMetrics.memoryUsage = memoryUsage
        customMetrics.forEach { (key, value) -> stepMetrics.addCustomMetric(key, value) }

        metricsStorage.saveStepMetrics(stepMetrics)
        logger.debug { "完成代理步骤: ${stepMetrics.stepName}, 代理: $agentId, 会话: $sessionId, 状态: $status" }
        return stepMetrics
    }

    /**
     * 记录代理步骤重试
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepId 步骤ID
     * @return 更新后的代理步骤指标，如果不存在则返回null
     */
    suspend fun recordStepRetry(
        agentId: String,
        sessionId: String? = null,
        stepId: String
    ): AgentStepMetrics? {
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)
            .find { it.stepId == stepId } ?: return null

        stepMetrics.incrementRetryCount()
        metricsStorage.saveStepMetrics(stepMetrics)
        
        // 更新代理指标中的重试次数
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
        if (agentMetrics != null) {
            agentMetrics.incrementRetryCount()
            metricsStorage.saveAgentMetrics(agentMetrics)
        }

        logger.debug { "记录代理步骤重试: ${stepMetrics.stepName}, 代理: $agentId, 会话: $sessionId" }
        return stepMetrics
    }

    /**
     * 开始跟踪工具调用
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepId 步骤ID
     * @param toolId 工具ID
     * @param toolName 工具名称
     * @param arguments 参数
     * @return 工具调用指标
     */
    suspend fun startToolCall(
        agentId: String,
        sessionId: String? = null,
        stepId: String,
        toolId: String,
        toolName: String,
        arguments: JsonElement? = null
    ): ToolCallMetrics? {
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)
            .find { it.stepId == stepId } ?: return null

        val toolCallMetrics = ToolCallMetrics(
            toolId = toolId,
            toolName = toolName,
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            arguments = arguments
        )

        stepMetrics.addToolCall(toolCallMetrics)
        metricsStorage.saveStepMetrics(stepMetrics)
        metricsStorage.saveToolCallMetrics(toolCallMetrics)
        
        // 更新代理指标中的工具调用次数
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
        if (agentMetrics != null) {
            agentMetrics.incrementToolCalls()
            metricsStorage.saveAgentMetrics(agentMetrics)
        }

        logger.debug { "开始跟踪工具调用: $toolName, 代理: $agentId, 会话: $sessionId, 步骤: ${stepMetrics.stepName}" }
        return toolCallMetrics
    }

    /**
     * 完成工具调用
     *
     * @param toolCallMetrics 工具调用指标
     * @param status 最终状态
     * @param result 结果
     * @param errorMessage 错误信息，如果有的话
     * @return 更新后的工具调用指标
     */
    suspend fun completeToolCall(
        toolCallMetrics: ToolCallMetrics,
        status: ToolCallStatus,
        result: JsonElement? = null,
        errorMessage: String? = null
    ): ToolCallMetrics {
        toolCallMetrics.complete(status, Instant.fromEpochMilliseconds(System.currentTimeMillis()), result, errorMessage)
        metricsStorage.saveToolCallMetrics(toolCallMetrics)
        logger.debug { "完成工具调用: ${toolCallMetrics.toolName}, 状态: $status" }
        return toolCallMetrics
    }

    /**
     * 更新Token计数
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepId 步骤ID，如果有的话
     * @param promptTokens 提示词token数
     * @param completionTokens 完成词token数
     */
    suspend fun updateTokenCounts(
        agentId: String,
        sessionId: String? = null,
        stepId: String? = null,
        promptTokens: Int,
        completionTokens: Int
    ) {
        // 更新代理指标中的token计数
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
        if (agentMetrics != null) {
            agentMetrics.updateTokenCounts(promptTokens, completionTokens)
            metricsStorage.saveAgentMetrics(agentMetrics)
        }

        // 如果提供了步骤ID，也更新步骤指标中的token计数
        if (stepId != null) {
            val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)
                .find { it.stepId == stepId }
            if (stepMetrics != null) {
                stepMetrics.updateTokenCounts(promptTokens, completionTokens)
                metricsStorage.saveStepMetrics(stepMetrics)
            }
        }

        logger.debug { "更新Token计数: 代理: $agentId, 会话: $sessionId, 提示词: $promptTokens, 完成词: $completionTokens" }
    }

    /**
     * 记录错误
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param stepId 步骤ID，如果有的话
     * @param errorMessage 错误信息
     */
    suspend fun recordError(
        agentId: String,
        sessionId: String? = null,
        stepId: String? = null,
        errorMessage: String
    ) {
        // 更新代理指标中的错误次数
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
        if (agentMetrics != null) {
            agentMetrics.incrementErrorCount()
            metricsStorage.saveAgentMetrics(agentMetrics)
        }

        // 如果提供了步骤ID，也更新步骤指标中的错误信息
        if (stepId != null) {
            val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)
                .find { it.stepId == stepId }
            if (stepMetrics != null) {
                stepMetrics.errorMessage = errorMessage
                stepMetrics.status = AgentStepStatus.FAILED
                metricsStorage.saveStepMetrics(stepMetrics)
            }
        }

        logger.debug { "记录错误: 代理: $agentId, 会话: $sessionId, 错误: $errorMessage" }
    }

    /**
     * 获取代理指标
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @return 代理指标，如果不存在则返回null
     */
    suspend fun getAgentMetrics(agentId: String, sessionId: String?): AgentMetrics? {
        return metricsStorage.getAgentMetrics(agentId, sessionId)
    }

    /**
     * 获取代理的所有指标
     *
     * @param agentId 代理ID
     * @param limit 限制返回的记录数
     * @param offset 偏移量
     * @return 代理指标列表
     */
    suspend fun getAgentMetricsForAgent(agentId: String, limit: Int = 100, offset: Int = 0): List<AgentMetrics> {
        return metricsStorage.getAgentMetricsForAgent(agentId, limit, offset)
    }

    /**
     * 获取会话的所有代理步骤指标
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @return 代理步骤指标列表
     */
    suspend fun getStepMetricsForSession(agentId: String, sessionId: String?): List<AgentStepMetrics> {
        return metricsStorage.getStepMetricsForSession(agentId, sessionId)
    }
}
