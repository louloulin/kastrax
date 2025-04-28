package ai.kastrax.core.agent.monitoring

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.analysis.AgentBehaviorAnalyzer
import ai.kastrax.core.agent.analysis.AgentMetricsCollector
import ai.kastrax.core.agent.analysis.AgentMetricsStorage
import ai.kastrax.core.agent.analysis.AgentOptimizationAdvisor
import ai.kastrax.core.agent.analysis.BottleneckStep
import ai.kastrax.core.agent.analysis.HighErrorStep
import ai.kastrax.core.agent.analysis.OptimizationSuggestion
import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Agent 诊断工具接口，用于诊断 Agent 的性能问题
 */
interface AgentDiagnosticTool {
    /**
     * 诊断 Agent 性能
     *
     * @param agent Agent 实例
     * @param sessionId 会话 ID，如果有的话
     * @return Agent 诊断结果
     */
    suspend fun diagnoseAgentPerformance(agent: Agent, sessionId: String? = null): AgentDiagnosticResult?

    /**
     * 诊断 Agent 性能
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return Agent 诊断结果
     */
    suspend fun diagnoseAgentPerformance(agentId: String, sessionId: String? = null): AgentDiagnosticResult?

    /**
     * 生成性能优化建议
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return 性能优化建议
     */
    suspend fun generatePerformanceOptimizationSuggestions(agentId: String, sessionId: String? = null): List<PerformanceOptimizationSuggestion>

    /**
     * 生成详细的诊断报告
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return 诊断报告
     */
    suspend fun generateDiagnosticReport(agentId: String, sessionId: String? = null): AgentDiagnosticReport?
}

/**
 * Agent 诊断工具实现
 *
 * @property metricsCollector Agent 指标收集器
 * @property metricsStorage Agent 指标存储
 * @property behaviorAnalyzer Agent 行为分析器
 * @property optimizationAdvisor Agent 优化顾问
 * @property performanceMonitor Agent 性能监控器
 * @property llmProvider LLM 提供者，用于生成详细的诊断报告
 */
class AgentDiagnosticToolImpl(
    private val metricsCollector: AgentMetricsCollector,
    private val metricsStorage: AgentMetricsStorage,
    private val behaviorAnalyzer: AgentBehaviorAnalyzer,
    private val optimizationAdvisor: AgentOptimizationAdvisor,
    private val performanceMonitor: AgentPerformanceMonitor,
    private val llmProvider: LlmProvider? = null
) : AgentDiagnosticTool, KastraXBase(component = "AGENT_DIAGNOSTIC", name = "tool") {

    override suspend fun diagnoseAgentPerformance(agent: Agent, sessionId: String?): AgentDiagnosticResult? {
        return diagnoseAgentPerformance(agent.name, sessionId)
    }

    override suspend fun diagnoseAgentPerformance(agentId: String, sessionId: String?): AgentDiagnosticResult? {
        // 获取性能指标
        val performanceMetrics = performanceMonitor.getPerformanceMetrics(agentId, sessionId)
            ?: return null

        // 获取行为分析结果
        val behaviorAnalysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId)
            ?: return null

        // 识别性能问题
        val performanceIssues = identifyPerformanceIssues(performanceMetrics, behaviorAnalysis.bottleneckSteps, behaviorAnalysis.highErrorSteps)

        // 生成优化建议
        val optimizationSuggestions = generatePerformanceOptimizationSuggestions(agentId, sessionId)

        return AgentDiagnosticResult(
            agentId = agentId,
            sessionId = sessionId,
            timestamp = Clock.System.now(),
            performanceMetrics = performanceMetrics,
            performanceIssues = performanceIssues,
            optimizationSuggestions = optimizationSuggestions
        )
    }

    override suspend fun generatePerformanceOptimizationSuggestions(agentId: String, sessionId: String?): List<PerformanceOptimizationSuggestion> {
        // 获取行为分析结果
        val behaviorAnalysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId)
            ?: return emptyList()

        // 转换优化建议
        return behaviorAnalysis.optimizationSuggestions.map { suggestion ->
            PerformanceOptimizationSuggestion(
                category = suggestion.category,
                suggestion = suggestion.suggestion,
                impact = when (suggestion.impact) {
                    "高" -> OptimizationImpact.HIGH
                    "中" -> OptimizationImpact.MEDIUM
                    "低" -> OptimizationImpact.LOW
                    else -> OptimizationImpact.MEDIUM
                },
                details = suggestion.details
            )
        }
    }

    override suspend fun generateDiagnosticReport(agentId: String, sessionId: String?): AgentDiagnosticReport? {
        // 获取诊断结果
        val diagnosticResult = diagnoseAgentPerformance(agentId, sessionId)
            ?: return null

        // 获取优化建议
        val optimizationAdvice = optimizationAdvisor.generateOptimizationAdvice(agentId, sessionId)

        // 使用 LLM 生成详细的诊断报告
        val detailedReport = if (llmProvider != null) {
            generateDetailedReportWithLlm(diagnosticResult, optimizationAdvice?.detailedAdvice)
        } else {
            generateBasicReport(diagnosticResult, optimizationAdvice?.detailedAdvice)
        }

        return AgentDiagnosticReport(
            agentId = agentId,
            sessionId = sessionId,
            timestamp = Clock.System.now(),
            diagnosticResult = diagnosticResult,
            detailedReport = detailedReport
        )
    }

    /**
     * 识别性能问题
     *
     * @param metrics 性能指标
     * @param bottleneckSteps 瓶颈步骤列表
     * @param highErrorSteps 高错误率步骤列表
     * @return 性能问题列表
     */
    private fun identifyPerformanceIssues(
        metrics: AgentPerformanceMetrics,
        bottleneckSteps: List<BottleneckStep>,
        highErrorSteps: List<HighErrorStep>
    ): List<PerformanceIssue> {
        val issues = mutableListOf<PerformanceIssue>()

        // 检查持续时间
        if (metrics.duration > 60000) { // 超过 1 分钟
            issues.add(
                PerformanceIssue(
                    type = IssueType.LONG_DURATION,
                    severity = IssueSeverity.MEDIUM,
                    description = "Agent 执行时间过长: ${metrics.duration}ms",
                    details = "长时间运行可能导致用户等待时间过长，影响用户体验。"
                )
            )
        }

        // 检查 Token 使用量
        if (metrics.totalTokens > 10000) { // 超过 10000 个 Token
            issues.add(
                PerformanceIssue(
                    type = IssueType.HIGH_TOKEN_USAGE,
                    severity = IssueSeverity.HIGH,
                    description = "Token 使用量过高: ${metrics.totalTokens}",
                    details = "高 Token 使用量会增加成本，并可能导致模型上下文长度限制问题。"
                )
            )
        }

        // 检查工具调用次数
        if (metrics.toolCalls > 20) { // 超过 20 次工具调用
            issues.add(
                PerformanceIssue(
                    type = IssueType.EXCESSIVE_TOOL_CALLS,
                    severity = IssueSeverity.MEDIUM,
                    description = "工具调用次数过多: ${metrics.toolCalls}",
                    details = "过多的工具调用会增加执行时间和复杂性，可能导致错误率增加。"
                )
            )
        }

        // 检查错误率
        if (metrics.errorRate > 0.2) { // 超过 20% 错误率
            issues.add(
                PerformanceIssue(
                    type = IssueType.HIGH_ERROR_RATE,
                    severity = IssueSeverity.HIGH,
                    description = "错误率过高: ${String.format("%.2f", metrics.errorRate * 100)}%",
                    details = "高错误率表明 Agent 在执行过程中遇到了问题，可能需要改进错误处理或提示词。"
                )
            )
        }

        // 检查重试率
        if (metrics.retryRate > 0.3) { // 超过 30% 重试率
            issues.add(
                PerformanceIssue(
                    type = IssueType.HIGH_RETRY_RATE,
                    severity = IssueSeverity.MEDIUM,
                    description = "重试率过高: ${String.format("%.2f", metrics.retryRate * 100)}%",
                    details = "高重试率表明 Agent 在执行过程中需要多次尝试才能成功，可能需要改进提示词或工具使用。"
                )
            )
        }

        // 添加瓶颈步骤问题
        for (bottleneck in bottleneckSteps) {
            issues.add(
                PerformanceIssue(
                    type = IssueType.BOTTLENECK_STEP,
                    severity = IssueSeverity.HIGH,
                    description = "步骤 '${bottleneck.stepName}' 是性能瓶颈",
                    details = "该步骤占总执行时间的 ${String.format("%.2f", bottleneck.percentageOfTotal)}%，持续时间为 ${bottleneck.duration}ms。"
                )
            )
        }

        // 添加高错误率步骤问题
        for (errorStep in highErrorSteps) {
            issues.add(
                PerformanceIssue(
                    type = IssueType.HIGH_ERROR_STEP,
                    severity = IssueSeverity.HIGH,
                    description = "步骤 '${errorStep.stepName}' 错误率高",
                    details = "该步骤重试次数为 ${errorStep.retryCount}，工具调用错误次数为 ${errorStep.toolCallErrors}。错误信息：${errorStep.errorMessage ?: "无"}"
                )
            )
        }

        return issues
    }

    /**
     * 使用 LLM 生成详细的诊断报告
     *
     * @param diagnosticResult 诊断结果
     * @param optimizationAdvice 优化建议
     * @return 详细的诊断报告
     */
    private suspend fun generateDetailedReportWithLlm(
        diagnosticResult: AgentDiagnosticResult,
        optimizationAdvice: String?
    ): String {
        if (llmProvider == null) {
            return generateBasicReport(diagnosticResult, optimizationAdvice)
        }

        try {
            // 构建提示词
            val prompt = buildString {
                appendLine("请根据以下 Agent 性能诊断数据，生成一份详细的诊断报告，包括性能问题分析、优化建议和改进方向。")
                appendLine()
                appendLine("## Agent 信息")
                appendLine("- Agent ID: ${diagnosticResult.agentId}")
                appendLine("- 会话 ID: ${diagnosticResult.sessionId ?: "无"}")
                appendLine()
                appendLine("## 性能指标")
                appendLine("- 执行时间: ${diagnosticResult.performanceMetrics.duration}ms")
                appendLine("- 步骤数: ${diagnosticResult.performanceMetrics.stepCount}")
                appendLine("- 平均步骤时间: ${String.format("%.2f", diagnosticResult.performanceMetrics.averageStepDuration)}ms")
                appendLine("- 每秒步骤数: ${String.format("%.2f", diagnosticResult.performanceMetrics.stepsPerSecond)}")
                appendLine("- 提示词 Token: ${diagnosticResult.performanceMetrics.promptTokens}")
                appendLine("- 完成词 Token: ${diagnosticResult.performanceMetrics.completionTokens}")
                appendLine("- 总 Token: ${diagnosticResult.performanceMetrics.totalTokens}")
                appendLine("- 每秒 Token: ${String.format("%.2f", diagnosticResult.performanceMetrics.tokensPerSecond)}")
                appendLine("- 工具调用次数: ${diagnosticResult.performanceMetrics.toolCalls}")
                appendLine("- 每秒工具调用: ${String.format("%.2f", diagnosticResult.performanceMetrics.toolCallsPerSecond)}")
                appendLine("- 错误次数: ${diagnosticResult.performanceMetrics.errorCount}")
                appendLine("- 重试次数: ${diagnosticResult.performanceMetrics.retryCount}")
                appendLine("- 错误率: ${String.format("%.2f", diagnosticResult.performanceMetrics.errorRate * 100)}%")
                appendLine("- 重试率: ${String.format("%.2f", diagnosticResult.performanceMetrics.retryRate * 100)}%")
                appendLine()
                appendLine("## 性能问题")
                if (diagnosticResult.performanceIssues.isEmpty()) {
                    appendLine("未发现性能问题。")
                } else {
                    diagnosticResult.performanceIssues.forEach { issue ->
                        appendLine("- [${issue.severity}] ${issue.type}: ${issue.description}")
                        appendLine("  ${issue.details}")
                    }
                }
                appendLine()
                appendLine("## 优化建议")
                if (diagnosticResult.optimizationSuggestions.isEmpty()) {
                    appendLine("没有优化建议。")
                } else {
                    diagnosticResult.optimizationSuggestions.forEach { suggestion ->
                        appendLine("- [${suggestion.impact}] ${suggestion.category}: ${suggestion.suggestion}")
                        appendLine("  ${suggestion.details}")
                    }
                }
                appendLine()
                if (optimizationAdvice != null) {
                    appendLine("## 现有优化建议")
                    appendLine(optimizationAdvice)
                    appendLine()
                }
                appendLine("请基于以上数据，生成一份详细的诊断报告，包括：")
                appendLine("1. 性能问题总结和分析")
                appendLine("2. 详细的优化建议，包括具体的实施步骤")
                appendLine("3. 长期改进方向")
                appendLine("4. 性能监控建议")
                appendLine()
                appendLine("请使用专业的语言，并提供具体的、可操作的建议。")
            }

            // 使用 LLM 生成报告
            val messages = listOf(LlmMessage(role = LlmMessageRole.USER, content = prompt))
            val response = llmProvider.generate(messages, LlmOptions(temperature = 0.3))
            return response.content
        } catch (e: Exception) {
            logger.error(e) { "使用 LLM 生成诊断报告时发生错误" }
            return generateBasicReport(diagnosticResult, optimizationAdvice)
        }
    }

    /**
     * 生成基本的诊断报告
     *
     * @param diagnosticResult 诊断结果
     * @param optimizationAdvice 优化建议
     * @return 基本的诊断报告
     */
    private fun generateBasicReport(
        diagnosticResult: AgentDiagnosticResult,
        optimizationAdvice: String?
    ): String {
        return buildString {
            appendLine("# Agent 性能诊断报告")
            appendLine()
            appendLine("## Agent 信息")
            appendLine("- Agent ID: ${diagnosticResult.agentId}")
            appendLine("- 会话 ID: ${diagnosticResult.sessionId ?: "无"}")
            appendLine("- 诊断时间: ${diagnosticResult.timestamp}")
            appendLine()
            appendLine("## 性能指标")
            appendLine("- 执行时间: ${diagnosticResult.performanceMetrics.duration}ms")
            appendLine("- 步骤数: ${diagnosticResult.performanceMetrics.stepCount}")
            appendLine("- 平均步骤时间: ${String.format("%.2f", diagnosticResult.performanceMetrics.averageStepDuration)}ms")
            appendLine("- 每秒步骤数: ${String.format("%.2f", diagnosticResult.performanceMetrics.stepsPerSecond)}")
            appendLine("- 提示词 Token: ${diagnosticResult.performanceMetrics.promptTokens}")
            appendLine("- 完成词 Token: ${diagnosticResult.performanceMetrics.completionTokens}")
            appendLine("- 总 Token: ${diagnosticResult.performanceMetrics.totalTokens}")
            appendLine("- 每秒 Token: ${String.format("%.2f", diagnosticResult.performanceMetrics.tokensPerSecond)}")
            appendLine("- 工具调用次数: ${diagnosticResult.performanceMetrics.toolCalls}")
            appendLine("- 每秒工具调用: ${String.format("%.2f", diagnosticResult.performanceMetrics.toolCallsPerSecond)}")
            appendLine("- 错误次数: ${diagnosticResult.performanceMetrics.errorCount}")
            appendLine("- 重试次数: ${diagnosticResult.performanceMetrics.retryCount}")
            appendLine("- 错误率: ${String.format("%.2f", diagnosticResult.performanceMetrics.errorRate * 100)}%")
            appendLine("- 重试率: ${String.format("%.2f", diagnosticResult.performanceMetrics.retryRate * 100)}%")
            appendLine()
            appendLine("## 性能问题")
            if (diagnosticResult.performanceIssues.isEmpty()) {
                appendLine("未发现性能问题。")
            } else {
                diagnosticResult.performanceIssues.forEach { issue ->
                    appendLine("- [${issue.severity}] ${issue.type}: ${issue.description}")
                    appendLine("  ${issue.details}")
                }
            }
            appendLine()
            appendLine("## 优化建议")
            if (diagnosticResult.optimizationSuggestions.isEmpty()) {
                appendLine("没有优化建议。")
            } else {
                diagnosticResult.optimizationSuggestions.forEach { suggestion ->
                    appendLine("- [${suggestion.impact}] ${suggestion.category}: ${suggestion.suggestion}")
                    appendLine("  ${suggestion.details}")
                }
            }
            appendLine()
            if (optimizationAdvice != null) {
                appendLine("## 详细优化建议")
                appendLine(optimizationAdvice)
            }
        }
    }
}

/**
 * Agent 诊断结果
 *
 * @property agentId Agent ID
 * @property sessionId 会话 ID，如果有的话
 * @property timestamp 时间戳
 * @property performanceMetrics 性能指标
 * @property performanceIssues 性能问题列表
 * @property optimizationSuggestions 优化建议列表
 */
data class AgentDiagnosticResult(
    val agentId: String,
    val sessionId: String?,
    val timestamp: Instant,
    val performanceMetrics: AgentPerformanceMetrics,
    val performanceIssues: List<PerformanceIssue>,
    val optimizationSuggestions: List<PerformanceOptimizationSuggestion>
)

/**
 * Agent 诊断报告
 *
 * @property agentId Agent ID
 * @property sessionId 会话 ID，如果有的话
 * @property timestamp 时间戳
 * @property diagnosticResult 诊断结果
 * @property detailedReport 详细报告
 */
data class AgentDiagnosticReport(
    val agentId: String,
    val sessionId: String?,
    val timestamp: Instant,
    val diagnosticResult: AgentDiagnosticResult,
    val detailedReport: String
)

/**
 * 性能问题
 *
 * @property type 问题类型
 * @property severity 严重程度
 * @property description 问题描述
 * @property details 详细信息
 */
data class PerformanceIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val description: String,
    val details: String
)

/**
 * 问题类型
 */
enum class IssueType {
    LONG_DURATION, // 持续时间长
    HIGH_TOKEN_USAGE, // Token 使用量高
    EXCESSIVE_TOOL_CALLS, // 过多的工具调用
    HIGH_ERROR_RATE, // 高错误率
    HIGH_RETRY_RATE, // 高重试率
    BOTTLENECK_STEP, // 瓶颈步骤
    HIGH_ERROR_STEP // 高错误率步骤
}

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    LOW, // 低
    MEDIUM, // 中
    HIGH // 高
}

/**
 * 性能优化建议
 *
 * @property category 类别
 * @property suggestion 建议
 * @property impact 影响
 * @property details 详细信息
 */
data class PerformanceOptimizationSuggestion(
    val category: String,
    val suggestion: String,
    val impact: OptimizationImpact,
    val details: String
)

/**
 * 优化影响
 */
enum class OptimizationImpact {
    LOW, // 低
    MEDIUM, // 中
    HIGH // 高
}
