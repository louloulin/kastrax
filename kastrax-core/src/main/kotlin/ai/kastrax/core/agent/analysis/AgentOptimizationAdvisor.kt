package ai.kastrax.core.agent.analysis

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 代理优化顾问，使用LLM生成更详细的优化建议
 *
 * @property llmProvider LLM提供者
 * @property analyzer 代理行为分析器
 */
class AgentOptimizationAdvisor(
    private val llmProvider: LlmProvider,
    private val analyzer: AgentBehaviorAnalyzer
) : KastraXBase(component = "AGENT_OPTIMIZATION", name = "advisor") {

    private val json = Json { prettyPrint = true }

    /**
     * 生成优化建议
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param includeDetails 是否包含详细信息
     * @return 优化建议
     */
    suspend fun generateOptimizationAdvice(
        agentId: String,
        sessionId: String?,
        includeDetails: Boolean = true
    ): OptimizationAdvice? {
        // 获取代理行为分析结果
        val analysis = analyzer.analyzeAgentBehavior(agentId, sessionId) ?: return null
        
        // 如果没有优化建议，直接返回
        if (analysis.optimizationSuggestions.isEmpty()) {
            logger.info { "没有找到优化建议: 代理: $agentId, 会话: $sessionId" }
            return OptimizationAdvice(
                agentId = agentId,
                sessionId = sessionId,
                analysisTime = analysis.analysisTime,
                suggestions = emptyList(),
                detailedAdvice = "没有发现需要优化的问题。"
            )
        }
        
        // 使用LLM生成更详细的优化建议
        val detailedAdvice = if (includeDetails) {
            generateDetailedAdviceWithLlm(analysis)
        } else {
            null
        }
        
        return OptimizationAdvice(
            agentId = agentId,
            sessionId = sessionId,
            analysisTime = analysis.analysisTime,
            suggestions = analysis.optimizationSuggestions,
            detailedAdvice = detailedAdvice
        )
    }

    /**
     * 使用LLM生成详细的优化建议
     *
     * @param analysis 代理行为分析结果
     * @return 详细的优化建议
     */
    private suspend fun generateDetailedAdviceWithLlm(analysis: AgentBehaviorAnalysis): String {
        val prompt = buildOptimizationPrompt(analysis)
        
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一名专业的AI代理优化专家，你的任务是分析代理的行为数据，并提供详细的优化建议。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )
        )
        
        try {
            val response = llmProvider.generate(messages, ai.kastrax.core.llm.LlmOptions())
            logger.debug { "生成优化建议成功: 代理: ${analysis.agentId}, 会话: ${analysis.sessionId}" }
            return response.content
        } catch (e: Exception) {
            logger.error(e) { "生成优化建议失败: ${e.message}" }
            return "无法生成详细的优化建议：${e.message}"
        }
    }

    /**
     * 构建优化提示词
     *
     * @param analysis 代理行为分析结果
     * @return 优化提示词
     */
    private fun buildOptimizationPrompt(analysis: AgentBehaviorAnalysis): String {
        val sb = StringBuilder()
        
        sb.appendLine("## 代理行为分析数据")
        sb.appendLine("代理ID: ${analysis.agentId}")
        analysis.sessionId?.let { sb.appendLine("会话ID: $it") }
        sb.appendLine("总执行时间: ${analysis.totalDuration}毫秒")
        sb.appendLine("平均步骤时间: ${String.format("%.2f", analysis.averageStepDuration)}毫秒")
        sb.appendLine("最长步骤时间: ${analysis.maxStepDuration}毫秒")
        sb.appendLine("最短步骤时间: ${analysis.minStepDuration}毫秒")
        sb.appendLine("总Token使用: ${analysis.totalTokens} (提示词: ${analysis.totalPromptTokens}, 完成词: ${analysis.totalCompletionTokens})")
        sb.appendLine("总工具调用次数: ${analysis.totalToolCalls}")
        sb.appendLine("每步骤平均工具调用次数: ${String.format("%.2f", analysis.toolCallsPerStep)}")
        sb.appendLine("总错误次数: ${analysis.totalErrors}")
        sb.appendLine("总重试次数: ${analysis.totalRetries}")
        sb.appendLine("错误率: ${String.format("%.2f", analysis.errorRate * 100)}%")
        sb.appendLine("重试率: ${String.format("%.2f", analysis.retryRate * 100)}%")
        
        if (analysis.bottleneckSteps.isNotEmpty()) {
            sb.appendLine("\n## 性能瓶颈步骤")
            analysis.bottleneckSteps.forEach { bottleneck ->
                sb.appendLine("- 步骤: ${bottleneck.stepName} (${bottleneck.stepType})")
                sb.appendLine("  - 持续时间: ${bottleneck.duration}毫秒 (占总时间的${String.format("%.2f", bottleneck.percentageOfTotal)}%)")
                sb.appendLine("  - 工具调用次数: ${bottleneck.toolCalls}")
                sb.appendLine("  - Token使用量: ${bottleneck.tokenUsage}")
            }
        }
        
        if (analysis.highErrorSteps.isNotEmpty()) {
            sb.appendLine("\n## 高错误率步骤")
            analysis.highErrorSteps.forEach { errorStep ->
                sb.appendLine("- 步骤: ${errorStep.stepName} (${errorStep.stepType})")
                sb.appendLine("  - 重试次数: ${errorStep.retryCount}")
                sb.appendLine("  - 工具调用错误次数: ${errorStep.toolCallErrors}")
                errorStep.errorMessage?.let { sb.appendLine("  - 错误信息: $it") }
            }
        }
        
        if (analysis.optimizationSuggestions.isNotEmpty()) {
            sb.appendLine("\n## 初步优化建议")
            analysis.optimizationSuggestions.forEach { suggestion ->
                sb.appendLine("- 类别: ${suggestion.category}")
                sb.appendLine("  - 建议: ${suggestion.suggestion}")
                sb.appendLine("  - 影响程度: ${suggestion.impact}")
                sb.appendLine("  - 详细信息: ${suggestion.details}")
            }
        }
        
        sb.appendLine("\n请基于以上数据，提供详细的代理优化建议。包括：")
        sb.appendLine("1. 总体性能评估")
        sb.appendLine("2. 主要瓶颈和问题分析")
        sb.appendLine("3. 具体的优化建议，包括代码示例或配置调整")
        sb.appendLine("4. 预期的优化效果")
        sb.appendLine("5. 长期改进策略")
        
        return sb.toString()
    }
}

/**
 * 优化建议
 *
 * @property agentId 代理ID
 * @property sessionId 会话ID，如果有的话
 * @property analysisTime 分析时间
 * @property suggestions 优化建议列表
 * @property detailedAdvice 详细的优化建议
 */
data class OptimizationAdvice(
    val agentId: String,
    val sessionId: String?,
    val analysisTime: kotlinx.datetime.Instant,
    val suggestions: List<OptimizationSuggestion>,
    val detailedAdvice: String?
)
