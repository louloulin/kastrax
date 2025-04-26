package ai.kastrax.core.agent.analysis

import ai.kastrax.core.common.KastraXBase
import kotlinx.datetime.Instant
import kotlin.math.max
import kotlin.math.min

/**
 * 代理行为分析器，用于分析代理的行为并提供优化建议
 *
 * @property metricsCollector 指标收集器
 */
class AgentBehaviorAnalyzer(
    private val metricsStorage: AgentMetricsStorage
) : KastraXBase(component = "AGENT_BEHAVIOR", name = "analyzer") {

    /**
     * 分析代理行为
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @return 代理行为分析结果
     */
    suspend fun analyzeAgentBehavior(agentId: String, sessionId: String?): AgentBehaviorAnalysis? {
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId) ?: return null
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

        if (stepMetrics.isEmpty()) {
            logger.warn { "没有找到代理步骤指标: 代理: $agentId, 会话: $sessionId" }
            return null
        }

        // 计算性能指标
        val totalDuration = agentMetrics.getDuration()
        val averageStepDuration = stepMetrics.map { step -> step.getDuration() }.average()
        val maxStepDuration = stepMetrics.maxOfOrNull { step -> step.getDuration() } ?: 0L
        val minStepDuration = stepMetrics.minOfOrNull { step -> step.getDuration() } ?: 0L

        // 计算Token使用情况
        val totalPromptTokens = agentMetrics.promptTokens
        val totalCompletionTokens = agentMetrics.completionTokens
        val totalTokens = agentMetrics.totalTokens

        // 计算工具调用情况
        val totalToolCalls = agentMetrics.toolCalls
        val toolCallsPerStep = if (stepMetrics.isNotEmpty()) totalToolCalls.toDouble() / stepMetrics.size else 0.0

        // 计算错误和重试情况
        val totalErrors = agentMetrics.errorCount
        val totalRetries = agentMetrics.retryCount
        val errorRate = if (stepMetrics.isNotEmpty()) totalErrors.toDouble() / stepMetrics.size else 0.0
        val retryRate = if (stepMetrics.isNotEmpty()) totalRetries.toDouble() / stepMetrics.size else 0.0

        // 识别瓶颈步骤
        val bottleneckSteps = identifyBottleneckSteps(stepMetrics)

        // 识别高错误率步骤
        val highErrorSteps = identifyHighErrorSteps(stepMetrics)

        // 生成优化建议
        val optimizationSuggestions = generateOptimizationSuggestions(
            agentMetrics,
            stepMetrics,
            bottleneckSteps,
            highErrorSteps
        )

        return AgentBehaviorAnalysis(
            agentId = agentId,
            sessionId = sessionId,
            analysisTime = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            totalDuration = totalDuration,
            averageStepDuration = averageStepDuration,
            maxStepDuration = maxStepDuration,
            minStepDuration = minStepDuration,
            totalPromptTokens = totalPromptTokens,
            totalCompletionTokens = totalCompletionTokens,
            totalTokens = totalTokens,
            totalToolCalls = totalToolCalls,
            toolCallsPerStep = toolCallsPerStep,
            totalErrors = totalErrors,
            totalRetries = totalRetries,
            errorRate = errorRate,
            retryRate = retryRate,
            bottleneckSteps = bottleneckSteps,
            highErrorSteps = highErrorSteps,
            optimizationSuggestions = optimizationSuggestions
        )
    }

    /**
     * 识别瓶颈步骤
     *
     * @param stepMetrics 步骤指标列表
     * @return 瓶颈步骤列表
     */
    private fun identifyBottleneckSteps(stepMetrics: List<AgentStepMetrics>): List<BottleneckStep> {
        if (stepMetrics.isEmpty()) return emptyList()

        // 计算平均步骤持续时间和标准差
        val durations = stepMetrics.map { it.getDuration() }
        val averageDuration = durations.average()
        val stdDeviation = calculateStandardDeviation(durations, averageDuration)

        // 识别持续时间超过平均值+1.5倍标准差的步骤为瓶颈
        val threshold = averageDuration + 1.5 * stdDeviation

        return stepMetrics
            .filter { it.getDuration() > threshold }
            .map { step ->
                BottleneckStep(
                    stepId = step.stepId,
                    stepName = step.stepName,
                    stepType = step.stepType,
                    duration = step.getDuration(),
                    percentageOfTotal = if (durations.sum() > 0) step.getDuration().toDouble() / durations.sum() * 100 else 0.0,
                    toolCalls = step.toolCalls.size,
                    tokenUsage = step.totalTokens
                )
            }
            .sortedByDescending { it.duration }
    }

    /**
     * 识别高错误率步骤
     *
     * @param stepMetrics 步骤指标列表
     * @return 高错误率步骤列表
     */
    private fun identifyHighErrorSteps(stepMetrics: List<AgentStepMetrics>): List<HighErrorStep> {
        return stepMetrics
            .filter { it.status == AgentStepStatus.FAILED || it.retryCount > 0 }
            .map { step ->
                HighErrorStep(
                    stepId = step.stepId,
                    stepName = step.stepName,
                    stepType = step.stepType,
                    errorMessage = step.errorMessage,
                    retryCount = step.retryCount,
                    toolCallErrors = step.toolCalls.count { it.status == ToolCallStatus.FAILED }
                )
            }
            .sortedByDescending { it.retryCount }
    }

    /**
     * 生成优化建议
     *
     * @param agentMetrics 代理指标
     * @param stepMetrics 步骤指标列表
     * @param bottleneckSteps 瓶颈步骤列表
     * @param highErrorSteps 高错误率步骤列表
     * @return 优化建议列表
     */
    private fun generateOptimizationSuggestions(
        agentMetrics: AgentMetrics,
        stepMetrics: List<AgentStepMetrics>,
        bottleneckSteps: List<BottleneckStep>,
        highErrorSteps: List<HighErrorStep>
    ): List<OptimizationSuggestion> {
        val suggestions = mutableListOf<OptimizationSuggestion>()

        // 检查Token使用情况
        if (agentMetrics.totalTokens > 10000) {
            suggestions.add(
                OptimizationSuggestion(
                    category = "Token使用",
                    suggestion = "考虑减少提示词长度或优化提示词结构，以减少Token使用量。",
                    impact = "高",
                    details = "当前Token使用量为${agentMetrics.totalTokens}，其中提示词${agentMetrics.promptTokens}，完成词${agentMetrics.completionTokens}。"
                )
            )
        }

        // 检查工具调用情况
        if (agentMetrics.toolCalls > 20) {
            suggestions.add(
                OptimizationSuggestion(
                    category = "工具调用",
                    suggestion = "考虑减少工具调用次数，或优化工具调用逻辑。",
                    impact = "中",
                    details = "当前工具调用次数为${agentMetrics.toolCalls}，平均每步骤${agentMetrics.toolCalls.toDouble() / max(1, stepMetrics.size)}次。"
                )
            )
        }

        // 检查错误和重试情况
        if (agentMetrics.errorCount > 0 || agentMetrics.retryCount > 0) {
            suggestions.add(
                OptimizationSuggestion(
                    category = "错误处理",
                    suggestion = "优化错误处理逻辑，减少重试次数。",
                    impact = "高",
                    details = "当前错误次数为${agentMetrics.errorCount}，重试次数为${agentMetrics.retryCount}。"
                )
            )
        }

        // 针对瓶颈步骤的建议
        bottleneckSteps.forEach { bottleneck ->
            suggestions.add(
                OptimizationSuggestion(
                    category = "性能瓶颈",
                    suggestion = "优化步骤'${bottleneck.stepName}'的性能。",
                    impact = "高",
                    details = "该步骤占总执行时间的${String.format("%.2f", bottleneck.percentageOfTotal)}%，持续时间为${bottleneck.duration}毫秒。"
                )
            )
        }

        // 针对高错误率步骤的建议
        highErrorSteps.forEach { errorStep ->
            suggestions.add(
                OptimizationSuggestion(
                    category = "错误处理",
                    suggestion = "改进步骤'${errorStep.stepName}'的错误处理。",
                    impact = "高",
                    details = "该步骤重试次数为${errorStep.retryCount}，工具调用错误次数为${errorStep.toolCallErrors}。错误信息：${errorStep.errorMessage ?: "无"}"
                )
            )
        }

        return suggestions
    }

    /**
     * 计算标准差
     *
     * @param values 值列表
     * @param mean 平均值
     * @return 标准差
     */
    private fun calculateStandardDeviation(values: List<Long>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.sum() / values.size
        return kotlin.math.sqrt(variance)
    }
}

/**
 * 代理行为分析结果
 *
 * @property agentId 代理ID
 * @property sessionId 会话ID，如果有的话
 * @property analysisTime 分析时间
 * @property totalDuration 总持续时间（毫秒）
 * @property averageStepDuration 平均步骤持续时间（毫秒）
 * @property maxStepDuration 最长步骤持续时间（毫秒）
 * @property minStepDuration 最短步骤持续时间（毫秒）
 * @property totalPromptTokens 总提示词token数
 * @property totalCompletionTokens 总完成词token数
 * @property totalTokens 总token数
 * @property totalToolCalls 总工具调用次数
 * @property toolCallsPerStep 每步骤平均工具调用次数
 * @property totalErrors 总错误次数
 * @property totalRetries 总重试次数
 * @property errorRate 错误率
 * @property retryRate 重试率
 * @property bottleneckSteps 瓶颈步骤列表
 * @property highErrorSteps 高错误率步骤列表
 * @property optimizationSuggestions 优化建议列表
 */
data class AgentBehaviorAnalysis(
    val agentId: String,
    val sessionId: String?,
    val analysisTime: Instant,
    val totalDuration: Long,
    val averageStepDuration: Double,
    val maxStepDuration: Long,
    val minStepDuration: Long,
    val totalPromptTokens: Int,
    val totalCompletionTokens: Int,
    val totalTokens: Int,
    val totalToolCalls: Int,
    val toolCallsPerStep: Double,
    val totalErrors: Int,
    val totalRetries: Int,
    val errorRate: Double,
    val retryRate: Double,
    val bottleneckSteps: List<BottleneckStep>,
    val highErrorSteps: List<HighErrorStep>,
    val optimizationSuggestions: List<OptimizationSuggestion>
)

/**
 * 瓶颈步骤
 *
 * @property stepId 步骤ID
 * @property stepName 步骤名称
 * @property stepType 步骤类型
 * @property duration 持续时间（毫秒）
 * @property percentageOfTotal 占总时间的百分比
 * @property toolCalls 工具调用次数
 * @property tokenUsage token使用量
 */
data class BottleneckStep(
    val stepId: String,
    val stepName: String,
    val stepType: String,
    val duration: Long,
    val percentageOfTotal: Double,
    val toolCalls: Int,
    val tokenUsage: Int
)

/**
 * 高错误率步骤
 *
 * @property stepId 步骤ID
 * @property stepName 步骤名称
 * @property stepType 步骤类型
 * @property errorMessage 错误信息
 * @property retryCount 重试次数
 * @property toolCallErrors 工具调用错误次数
 */
data class HighErrorStep(
    val stepId: String,
    val stepName: String,
    val stepType: String,
    val errorMessage: String?,
    val retryCount: Int,
    val toolCallErrors: Int
)

/**
 * 优化建议
 *
 * @property category 类别
 * @property suggestion 建议
 * @property impact 影响程度
 * @property details 详细信息
 */
data class OptimizationSuggestion(
    val category: String,
    val suggestion: String,
    val impact: String,
    val details: String
)
