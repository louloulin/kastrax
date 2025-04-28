package ai.kastrax.core.agent.monitoring

import ai.kastrax.core.agent.analysis.*
import ai.kastrax.core.common.KastraXBase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

/**
 * Agent 性能报告生成器，用于生成 Agent 性能报告
 *
 * @property metricsStorage Agent 指标存储
 * @property behaviorAnalyzer Agent 行为分析器
 * @property diagnosticTool Agent 诊断工具
 */
class AgentPerformanceReport(
    private val metricsStorage: AgentMetricsStorage,
    private val behaviorAnalyzer: AgentBehaviorAnalyzer,
    private val diagnosticTool: AgentDiagnosticTool
) : KastraXBase(component = "AGENT_PERFORMANCE", name = "report") {

    /**
     * 生成性能报告
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @param includeDetails 是否包含详细信息
     * @param outputPath 输出路径，如果为 null 则返回报告内容
     * @return 报告内容，如果 outputPath 不为 null 则返回 null
     */
    suspend fun generatePerformanceReport(
        agentId: String,
        sessionId: String? = null,
        includeDetails: Boolean = true,
        outputPath: String? = null
    ): String? {
        // 获取性能指标
        val performanceMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)?.let { agentMetrics ->
            val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

            // 计算性能指标
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

            AgentPerformanceMetrics(
                agentId = agentMetrics.agentId,
                sessionId = agentMetrics.sessionId,
                timestamp = Clock.System.now(),
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
        } ?: return null

        // 获取行为分析结果
        val behaviorAnalysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId) as? AgentBehaviorAnalysis

        // 获取诊断结果
        val diagnosticResult = diagnosticTool.diagnoseAgentPerformance(agentId, sessionId)

        // 生成报告
        val report = generateReport(
            agentId = agentId,
            sessionId = sessionId,
            performanceMetrics = performanceMetrics,
            behaviorAnalysis = behaviorAnalysis,
            diagnosticResult = diagnosticResult,
            includeDetails = includeDetails
        )

        // 如果指定了输出路径，则保存报告
        if (outputPath != null) {
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            file.writeText(report)
            logger.info { "性能报告已保存到: $outputPath" }
            return null
        }

        return report
    }

    /**
     * 生成 Markdown 格式的性能报告
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @param performanceMetrics 性能指标
     * @param behaviorAnalysis 行为分析结果
     * @param diagnosticResult 诊断结果
     * @param includeDetails 是否包含详细信息
     * @return Markdown 格式的报告
     */
    private fun generateReport(
        agentId: String,
        sessionId: String?,
        performanceMetrics: AgentPerformanceMetrics,
        behaviorAnalysis: ai.kastrax.core.agent.analysis.AgentBehaviorAnalysis?,
        diagnosticResult: AgentDiagnosticResult?,
        includeDetails: Boolean
    ): String {
        val timestamp = Clock.System.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(formatter)

        return buildString {
            appendLine("# Agent 性能报告")
            appendLine()
            appendLine("生成时间: $formattedTimestamp")
            appendLine()
            appendLine("## Agent 信息")
            appendLine()
            appendLine("- **Agent ID**: $agentId")
            appendLine("- **会话 ID**: ${sessionId ?: "无"}")
            appendLine("- **状态**: ${performanceMetrics.status}")
            appendLine()
            appendLine("## 性能摘要")
            appendLine()
            appendLine("| 指标 | 值 |")
            appendLine("|------|------|")
            appendLine("| 执行时间 | ${performanceMetrics.duration}ms |")
            appendLine("| 步骤数 | ${performanceMetrics.stepCount} |")
            appendLine("| 平均步骤时间 | ${String.format("%.2f", performanceMetrics.averageStepDuration)}ms |")
            appendLine("| 每秒步骤数 | ${String.format("%.2f", performanceMetrics.stepsPerSecond)} |")
            appendLine("| 总 Token | ${performanceMetrics.totalTokens} |")
            appendLine("| 提示词 Token | ${performanceMetrics.promptTokens} |")
            appendLine("| 完成词 Token | ${performanceMetrics.completionTokens} |")
            appendLine("| 每秒 Token | ${String.format("%.2f", performanceMetrics.tokensPerSecond)} |")
            appendLine("| 工具调用次数 | ${performanceMetrics.toolCalls} |")
            appendLine("| 每秒工具调用 | ${String.format("%.2f", performanceMetrics.toolCallsPerSecond)} |")
            appendLine("| 错误次数 | ${performanceMetrics.errorCount} |")
            appendLine("| 重试次数 | ${performanceMetrics.retryCount} |")
            appendLine("| 错误率 | ${String.format("%.2f", performanceMetrics.errorRate * 100)}% |")
            appendLine("| 重试率 | ${String.format("%.2f", performanceMetrics.retryRate * 100)}% |")
            appendLine()

            if (behaviorAnalysis != null) {
                appendLine("## 性能瓶颈")
                appendLine()

                if (behaviorAnalysis.bottleneckSteps.isEmpty()) {
                    appendLine("未发现明显的性能瓶颈。")
                } else {
                    appendLine("| 步骤 | 持续时间 (ms) | 占总时间比例 |")
                    appendLine("|------|--------------|------------|")

                    behaviorAnalysis.bottleneckSteps.forEach { bottleneck ->
                        appendLine("| ${bottleneck.stepName} | ${bottleneck.duration} | ${String.format("%.2f", bottleneck.percentageOfTotal * 100)}% |")
                    }
                }
                appendLine()

                appendLine("## 错误分析")
                appendLine()

                if (behaviorAnalysis.highErrorSteps.isEmpty()) {
                    appendLine("未发现高错误率的步骤。")
                } else {
                    appendLine("| 步骤 | 重试次数 | 工具调用错误 | 错误信息 |")
                    appendLine("|------|---------|------------|---------|")

                    behaviorAnalysis.highErrorSteps.forEach { errorStep ->
                        appendLine("| ${errorStep.stepName} | ${errorStep.retryCount} | ${errorStep.toolCallErrors} | ${errorStep.errorMessage ?: "无"} |")
                    }
                }
                appendLine()
            }

            if (diagnosticResult != null) {
                appendLine("## 性能问题")
                appendLine()

                if (diagnosticResult.performanceIssues.isEmpty()) {
                    appendLine("未发现性能问题。")
                } else {
                    appendLine("| 问题类型 | 严重程度 | 描述 |")
                    appendLine("|---------|---------|------|")

                    diagnosticResult.performanceIssues.forEach { issue ->
                        appendLine("| ${issue.type} | ${issue.severity} | ${issue.description} |")
                    }

                    if (includeDetails) {
                        appendLine()
                        appendLine("### 问题详情")
                        appendLine()

                        diagnosticResult.performanceIssues.forEach { issue ->
                            appendLine("#### ${issue.type}: ${issue.description}")
                            appendLine()
                            appendLine("**严重程度**: ${issue.severity}")
                            appendLine()
                            appendLine("**详情**: ${issue.details}")
                            appendLine()
                        }
                    }
                }
                appendLine()

                appendLine("## 优化建议")
                appendLine()

                if (diagnosticResult.optimizationSuggestions.isEmpty()) {
                    appendLine("没有优化建议。")
                } else {
                    appendLine("| 类别 | 影响 | 建议 |")
                    appendLine("|------|------|------|")

                    diagnosticResult.optimizationSuggestions.forEach { suggestion ->
                        appendLine("| ${suggestion.category} | ${suggestion.impact} | ${suggestion.suggestion} |")
                    }

                    if (includeDetails) {
                        appendLine()
                        appendLine("### 建议详情")
                        appendLine()

                        diagnosticResult.optimizationSuggestions.forEach { suggestion ->
                            appendLine("#### ${suggestion.category}: ${suggestion.suggestion}")
                            appendLine()
                            appendLine("**影响**: ${suggestion.impact}")
                            appendLine()
                            appendLine("**详情**: ${suggestion.details}")
                            appendLine()
                        }
                    }
                }
                appendLine()
            }

            appendLine("## 性能图表")
            appendLine()
            appendLine("### Token 使用情况")
            appendLine()
            appendLine("```mermaid")
            appendLine("pie")
            appendLine("    title Token 使用分布")
            appendLine("    \"提示词\" : ${performanceMetrics.promptTokens}")
            appendLine("    \"完成词\" : ${performanceMetrics.completionTokens}")
            appendLine("```")
            appendLine()

            if (behaviorAnalysis != null && behaviorAnalysis.bottleneckSteps.isNotEmpty()) {
                appendLine("### 步骤执行时间分布")
                appendLine()
                appendLine("```mermaid")
                appendLine("pie")
                appendLine("    title 步骤执行时间分布")

                behaviorAnalysis.bottleneckSteps.forEach { bottleneck ->
                    appendLine("    \"${bottleneck.stepName}\" : ${bottleneck.duration}")
                }

                // 添加其他步骤的总时间
                val bottleneckTime = behaviorAnalysis.bottleneckSteps.sumOf { it.duration }
                val totalTime = performanceMetrics.duration
                val otherTime = totalTime - bottleneckTime

                if (otherTime > 0) {
                    appendLine("    \"其他步骤\" : $otherTime")
                }

                appendLine("```")
                appendLine()
            }

            appendLine("## 结论")
            appendLine()

            if (diagnosticResult != null && diagnosticResult.performanceIssues.isNotEmpty()) {
                appendLine("Agent 存在一些性能问题，建议按照上述优化建议进行改进。")
            } else {
                appendLine("Agent 性能良好，未发现明显的性能问题。")
            }
            appendLine()

            appendLine("---")
            appendLine()
            appendLine("*此报告由 KastraX Agent 性能监控工具自动生成*")
        }
    }

    /**
     * 生成性能报告文件名
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return 文件名
     */
    fun generateReportFileName(agentId: String, sessionId: String?): String {
        val timestamp = Clock.System.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val formattedTimestamp = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(formatter)

        return if (sessionId != null) {
            "agent_${agentId}_session_${sessionId}_${formattedTimestamp}.md"
        } else {
            "agent_${agentId}_${formattedTimestamp}.md"
        }
    }
}
