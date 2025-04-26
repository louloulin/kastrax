package ai.kastrax.examples.agent

import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.InMemorySessionManager
import ai.kastrax.core.agent.InMemoryStateManager
import ai.kastrax.core.agent.analysis.*
import ai.kastrax.core.tools.web.WebSearchTool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonNull
import java.nio.file.Paths
import java.io.File

/**
 * 代理行为分析示例
 *
 * 本示例演示如何使用代理行为分析和优化工具来分析代理的行为，
 * 识别性能瓶颈，并提供优化建议。
 */
fun main() = runBlocking {
    // 创建指标存储
    val metricsStorage = InMemoryAgentMetricsStorage()

    // 创建行为分析器和可视化工具
    val behaviorAnalyzer = AgentBehaviorAnalyzer(metricsStorage)
    val behaviorVisualizer = AgentBehaviorVisualizer(metricsStorage)

    // 创建DeepSeek模型
    val deepSeekModel = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "sk-85e83081df28490b9ae63188f0cb4f79")
    }

    // 创建优化顾问
    val optimizationAdvisor = AgentOptimizationAdvisor(deepSeekModel, behaviorAnalyzer)

    // 创建工具
    val webSearchTool = WebSearchTool(
        apiKey = System.getenv("GOOGLE_API_KEY") ?: "",
        searchEngine = WebSearchTool.SearchEngine.MOCK
    )

    // 创建代理
    val agent = AgentTemplates.createResearchAssistantAgent(
        name = "ResearchAssistant",
        model = deepSeekModel,
        additionalTools = mapOf("web_search" to webSearchTool),
        customInstructions = """
            你是一名专业的研究助手，名为ResearchAssistant。
            你的主要职责是帮助用户进行深入的研究和分析，特别是关于人工智能和机器学习领域的最新发展。
            你必须使用提供的web_search工具来获取最新信息，并确保引用信息来源。
        """.trimIndent()
    )

    // 设置状态和会话管理
    val stateManager = InMemoryStateManager()
    val sessionManager = InMemorySessionManager()

    // 开始收集代理指标
    val agentId = agent.name
    val sessionId = "behavior-analysis-session"
    val agentMetrics = AgentMetrics(
        agentId = agentId,
        sessionId = sessionId,
        startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    )
    metricsStorage.saveAgentMetrics(agentMetrics)

    println("开始收集代理指标: ${agentMetrics.agentId}, 会话: ${agentMetrics.sessionId}")

    try {
        // 开始第一个步骤：准备查询
        val step1 = AgentStepMetrics(
            agentId = agentId,
            sessionId = sessionId,
            stepName = "准备查询",
            stepType = "preparation",
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveStepMetrics(step1)

        // 模拟步骤执行
        println("执行步骤: ${step1.stepName}")
        Thread.sleep(500)

        // 更新Token计数
        step1.updateTokenCounts(100, 50)
        metricsStorage.saveStepMetrics(step1)

        // 更新代理指标中的token计数
        agentMetrics.updateTokenCounts(100, 50)
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 完成步骤
        step1.status = AgentStepStatus.COMPLETED
        step1.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        step1.latency = step1.getDuration()
        metricsStorage.saveStepMetrics(step1)

        // 开始第二个步骤：执行搜索
        val step2 = AgentStepMetrics(
            agentId = agentId,
            sessionId = sessionId,
            stepName = "执行搜索",
            stepType = "search",
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveStepMetrics(step2)

        // 模拟步骤执行
        println("执行步骤: ${step2.stepName}")

        // 开始工具调用
        val toolCall = ToolCallMetrics(
            toolId = "web_search",
            toolName = "Web Search",
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )

        // 添加工具调用到步骤指标
        step2.addToolCall(toolCall)
        metricsStorage.saveStepMetrics(step2)
        metricsStorage.saveToolCallMetrics(toolCall)

        // 更新代理指标中的工具调用次数
        agentMetrics.incrementToolCalls()
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 模拟工具调用执行
        println("执行工具调用: Web Search")
        Thread.sleep(1500)

        // 完成工具调用
        toolCall.status = ToolCallStatus.COMPLETED
        toolCall.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        toolCall.latency = toolCall.getDuration()
        metricsStorage.saveToolCallMetrics(toolCall)
        metricsStorage.saveStepMetrics(step2)

        // 更新Token计数
        step2.updateTokenCounts(200, 100)
        metricsStorage.saveStepMetrics(step2)

        // 更新代理指标中的token计数
        agentMetrics.updateTokenCounts(200, 100)
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 完成步骤
        step2.status = AgentStepStatus.COMPLETED
        step2.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        step2.latency = step2.getDuration()
        metricsStorage.saveStepMetrics(step2)

        // 开始第三个步骤：分析结果
        val step3 = AgentStepMetrics(
            agentId = agentId,
            sessionId = sessionId,
            stepName = "分析结果",
            stepType = "analysis",
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveStepMetrics(step3)

        // 模拟步骤执行
        println("执行步骤: ${step3.stepName}")
        Thread.sleep(800)

        // 更新Token计数
        step3.updateTokenCounts(300, 150)
        metricsStorage.saveStepMetrics(step3)

        // 更新代理指标中的token计数
        agentMetrics.updateTokenCounts(300, 150)
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 完成步骤
        step3.status = AgentStepStatus.COMPLETED
        step3.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        step3.latency = step3.getDuration()
        metricsStorage.saveStepMetrics(step3)

        // 开始第四个步骤：生成回复
        val step4 = AgentStepMetrics(
            agentId = agentId,
            sessionId = sessionId,
            stepName = "生成回复",
            stepType = "generation",
            startTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveStepMetrics(step4)

        // 模拟步骤执行
        println("执行步骤: ${step4.stepName}")

        // 模拟错误和重试
        println("遇到错误，进行重试...")

        // 记录错误
        step4.errorMessage = "生成回复时出现错误"
        metricsStorage.saveStepMetrics(step4)

        // 更新代理指标中的错误次数
        agentMetrics.incrementErrorCount()
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 记录重试
        step4.incrementRetryCount()
        metricsStorage.saveStepMetrics(step4)

        // 更新代理指标中的重试次数
        agentMetrics.incrementRetryCount()
        metricsStorage.saveAgentMetrics(agentMetrics)

        Thread.sleep(1200)

        // 更新Token计数
        step4.updateTokenCounts(400, 200)
        metricsStorage.saveStepMetrics(step4)

        // 更新代理指标中的token计数
        agentMetrics.updateTokenCounts(400, 200)
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 完成步骤
        step4.status = AgentStepStatus.COMPLETED
        step4.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        step4.latency = step4.getDuration()
        metricsStorage.saveStepMetrics(step4)

        // 完成代理执行
        agentMetrics.status = AgentStatus.IDLE
        agentMetrics.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        agentMetrics.latency = agentMetrics.getDuration()
        metricsStorage.saveAgentMetrics(agentMetrics)

        println("代理执行完成")

        // 分析代理行为
        val analysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId)

        if (analysis != null) {
            println("\n代理行为分析结果:")
            println("总执行时间: ${analysis.totalDuration}毫秒")
            println("平均步骤时间: ${String.format("%.2f", analysis.averageStepDuration)}毫秒")
            println("最长步骤时间: ${analysis.maxStepDuration}毫秒")
            println("最短步骤时间: ${analysis.minStepDuration}毫秒")
            println("总Token使用: ${analysis.totalTokens} (提示词: ${analysis.totalPromptTokens}, 完成词: ${analysis.totalCompletionTokens})")
            println("总工具调用次数: ${analysis.totalToolCalls}")
            println("每步骤平均工具调用次数: ${String.format("%.2f", analysis.toolCallsPerStep)}")
            println("总错误次数: ${analysis.totalErrors}")
            println("总重试次数: ${analysis.totalRetries}")
            println("错误率: ${String.format("%.2f", analysis.errorRate * 100)}%")
            println("重试率: ${String.format("%.2f", analysis.retryRate * 100)}%")

            if (analysis.bottleneckSteps.isNotEmpty()) {
                println("\n性能瓶颈步骤:")
                analysis.bottleneckSteps.forEach { bottleneck ->
                    println("- ${bottleneck.stepName}: ${bottleneck.duration}毫秒 (占总时间的${String.format("%.2f", bottleneck.percentageOfTotal)}%)")
                }
            }

            if (analysis.optimizationSuggestions.isNotEmpty()) {
                println("\n优化建议:")
                analysis.optimizationSuggestions.forEach { suggestion ->
                    println("- [${suggestion.impact}] ${suggestion.category}: ${suggestion.suggestion}")
                    println("  ${suggestion.details}")
                }
            }

            // 生成优化建议
            val advice = optimizationAdvisor.generateOptimizationAdvice(agentId, sessionId)

            if (advice != null && advice.detailedAdvice != null) {
                println("\n详细优化建议:")
                println(advice.detailedAdvice)
            }

            // 生成简单的HTML报告
            println("\n生成简单的HTML报告...")
            val outputDir = "/Users/louloulin/Documents/linchong/agent/kastra/kastrax/kastrax-examples/examples"
            File(outputDir).mkdirs()
            println("创建输出目录: $outputDir")

            // 生成HTML报告
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>代理行为分析报告</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        h1 { color: #333; }
                        .metric { margin-bottom: 10px; }
                        .label { font-weight: bold; }
                    </style>
                </head>
                <body>
                    <h1>代理行为分析报告</h1>
                    <div class="metric"><span class="label">代理ID:</span> ${analysis.agentId}</div>
                    <div class="metric"><span class="label">会话ID:</span> ${analysis.sessionId ?: "N/A"}</div>
                    <div class="metric"><span class="label">总执行时间:</span> ${analysis.totalDuration}毫秒</div>
                    <div class="metric"><span class="label">平均步骤时间:</span> ${String.format("%.2f", analysis.averageStepDuration)}毫秒</div>
                    <div class="metric"><span class="label">最长步骤时间:</span> ${analysis.maxStepDuration}毫秒</div>
                    <div class="metric"><span class="label">最短步骤时间:</span> ${analysis.minStepDuration}毫秒</div>
                    <div class="metric"><span class="label">总Token使用:</span> ${analysis.totalTokens} (提示词: ${analysis.totalPromptTokens}, 完成词: ${analysis.totalCompletionTokens})</div>
                    <div class="metric"><span class="label">总工具调用次数:</span> ${analysis.totalToolCalls}</div>
                    <div class="metric"><span class="label">每步骤平均工具调用次数:</span> ${String.format("%.2f", analysis.toolCallsPerStep)}</div>
                    <div class="metric"><span class="label">总错误次数:</span> ${analysis.totalErrors}</div>
                    <div class="metric"><span class="label">总重试次数:</span> ${analysis.totalRetries}</div>
                    <div class="metric"><span class="label">错误率:</span> ${String.format("%.2f", analysis.errorRate * 100)}%</div>
                    <div class="metric"><span class="label">重试率:</span> ${String.format("%.2f", analysis.retryRate * 100)}%</div>
                </body>
                </html>
            """.trimIndent()

            val htmlPath = "$outputDir/agent_behavior_report.html"
            File(htmlPath).writeText(htmlContent)
            println("已保存HTML报告到: $htmlPath")

            // 生成Mermaid图表
            println("\n生成简单的Mermaid图表...")
            val mermaidContent = """
                ```mermaid
                gantt
                    title 代理执行时间线
                    dateFormat  HH:mm:ss
                    axisFormat %H:%M:%S

                    section 准备查询
                    步骤执行 : done, 00:00:00, 00:00:01

                    section 执行搜索
                    步骤执行 : done, 00:00:01, 00:00:03
                    Web Search : done, 00:00:01, 00:00:02

                    section 分析结果
                    步骤执行 : done, 00:00:03, 00:00:04

                    section 生成回复
                    步骤执行 : crit, 00:00:04, 00:00:06
                ```
            """.trimIndent()

            val mermaidPath = "$outputDir/agent_timeline.mmd"
            File(mermaidPath).writeText(mermaidContent)
            println("已保存Mermaid图表到: $mermaidPath")
        } else {
            println("无法分析代理行为")
        }

    } catch (e: Exception) {
        println("代理执行出错: ${e.message}")
        agentMetrics.status = AgentStatus.ERROR
        agentMetrics.endTime = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        agentMetrics.latency = agentMetrics.getDuration()
        metricsStorage.saveAgentMetrics(agentMetrics)
    }
}
