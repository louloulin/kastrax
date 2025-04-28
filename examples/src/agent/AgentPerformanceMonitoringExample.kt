package ai.kastrax.examples.agent

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.analysis.*
import ai.kastrax.core.agent.monitoring.AgentDiagnosticTool
import ai.kastrax.core.agent.monitoring.AgentDiagnosticToolImpl
import ai.kastrax.core.agent.monitoring.AgentPerformanceMonitor
import ai.kastrax.core.agent.monitoring.AgentPerformanceMonitorImpl
import ai.kastrax.core.agent.monitoring.AgentPerformanceReport
import ai.kastrax.core.agent.monitoring.AgentPerformanceThresholds
import ai.kastrax.core.agent.monitoring.AgentPerformanceVisualizer
import ai.kastrax.core.agent.monitoring.AlertLevel
import ai.kastrax.core.agent.monitoring.AlertType
import ai.kastrax.core.agent.tools.calculator.CalculatorTool
import ai.kastrax.core.agent.tools.search.SearchTool
import ai.kastrax.core.agent.tools.search.SearchToolConfig
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Agent 性能监控示例
 *
 * 这个示例展示了如何使用 Agent 性能监控和诊断工具来监控和诊断 Agent 的性能。
 */
fun main() = runBlocking {
    println("启动 Agent 性能监控示例...")

    // 创建指标存储
    val metricsStorage = InMemoryAgentMetricsStorage()

    // 创建指标收集器
    val metricsCollector = AgentMetricsCollector(metricsStorage)

    // 创建行为分析器
    val behaviorAnalyzer = AgentBehaviorAnalyzer(metricsStorage)

    // 创建优化顾问
    val optimizationAdvisor = AgentOptimizationAdvisor(behaviorAnalyzer)

    // 创建性能监控器
    val performanceMonitor = AgentPerformanceMonitorImpl(metricsCollector, metricsStorage)

    // 创建诊断工具
    val diagnosticTool = AgentDiagnosticToolImpl(
        metricsCollector = metricsCollector,
        metricsStorage = metricsStorage,
        behaviorAnalyzer = behaviorAnalyzer,
        optimizationAdvisor = optimizationAdvisor,
        performanceMonitor = performanceMonitor
    )

    // 创建性能报告生成器
    val performanceReport = AgentPerformanceReport(
        metricsStorage = metricsStorage,
        behaviorAnalyzer = behaviorAnalyzer,
        diagnosticTool = diagnosticTool
    )

    // 创建性能可视化器
    val performanceVisualizer = AgentPerformanceVisualizer(metricsStorage)

    // 设置性能阈值
    performanceMonitor.setPerformanceThresholds(
        AgentPerformanceThresholds(
            maxDurationMs = 30000, // 30 秒
            maxTokens = 2000, // 2000 个 Token
            maxToolCalls = 10, // 10 次工具调用
            maxErrorRate = 0.2, // 20% 错误率
            maxRetryRate = 0.3 // 30% 重试率
        )
    )

    // 监听性能指标
    val metricsJob = performanceMonitor.getPerformanceMetricsFlow()
        .onEach { metrics ->
            println("收到性能指标: Agent=${metrics.agentId}, 会话=${metrics.sessionId}")
            println("  执行时间: ${metrics.duration}ms")
            println("  步骤数: ${metrics.stepCount}")
            println("  Token 使用量: ${metrics.totalTokens}")
            println("  工具调用次数: ${metrics.toolCalls}")
            println("  错误率: ${String.format("%.2f", metrics.errorRate * 100)}%")
            println("  重试率: ${String.format("%.2f", metrics.retryRate * 100)}%")
        }
        .launchIn(this)

    // 监听性能警报
    val alertsJob = performanceMonitor.getPerformanceAlertsFlow()
        .onEach { alert ->
            val levelSymbol = when (alert.level) {
                AlertLevel.INFO -> "ℹ️"
                AlertLevel.WARNING -> "⚠️"
                AlertLevel.ERROR -> "🔴"
            }

            val typeSymbol = when (alert.type) {
                AlertType.DURATION -> "⏱️"
                AlertType.TOKEN_USAGE -> "🔤"
                AlertType.TOOL_CALLS -> "🔧"
                AlertType.ERROR_RATE -> "❌"
                AlertType.RETRY_RATE -> "🔄"
            }

            println("$levelSymbol $typeSymbol 性能警报: ${alert.message}")
        }
        .launchIn(this)

    // 创建 Agent
    val agent = agent {
        name = "计算器助手"
        instructions = """
            你是一个计算器助手，可以帮助用户进行数学计算。
            你可以使用计算器工具进行计算，也可以使用搜索工具查找信息。
            请尽可能详细地解释计算过程和结果。
        """.trimIndent()
        model = openAi("gpt-3.5-turbo")
        tools {
            tool(CalculatorTool())
            tool(SearchTool(SearchToolConfig()))
        }

        // 添加指标收集器
        metricsCollector(metricsCollector)
    }

    // 启动性能监控
    val sessionId = "example-session"
    performanceMonitor.startMonitoring(agent, sessionId)

    // 使用 Agent 执行任务
    val job = launch {
        try {
            println("使用 Agent 执行任务...")

            // 执行一些计算任务
            val response1 = agent.generate("计算 123 + 456 的结果")
            println("Agent 响应: ${response1.text}")

            // 执行一些搜索任务
            val response2 = agent.generate("搜索关于人工智能的信息")
            println("Agent 响应: ${response2.text}")

            // 执行一些复杂任务
            val response3 = agent.generate("计算 (123 + 456) * 789 的结果，并解释计算过程")
            println("Agent 响应: ${response3.text}")

            // 执行一些可能导致错误的任务
            val response4 = agent.generate("计算 1/0 的结果")
            println("Agent 响应: ${response4.text}")

        } catch (e: Exception) {
            println("执行任务时发生错误: ${e.message}")
        } finally {
            // 停止性能监控
            performanceMonitor.stopMonitoring(agent.id, sessionId)
        }
    }

    // 等待任务完成
    job.join()

    // 等待一段时间，确保所有指标都已收集
    delay(1000)

    // 诊断 Agent 性能
    println("\n诊断 Agent 性能...")
    val diagnosticResult = diagnosticTool.diagnoseAgentPerformance(agent.id, sessionId)

    if (diagnosticResult != null) {
        println("诊断结果:")
        println("  性能问题数量: ${diagnosticResult.performanceIssues.size}")
        diagnosticResult.performanceIssues.forEach { issue ->
            println("  - [${issue.severity}] ${issue.type}: ${issue.description}")
        }

        println("  优化建议数量: ${diagnosticResult.optimizationSuggestions.size}")
        diagnosticResult.optimizationSuggestions.forEach { suggestion ->
            println("  - [${suggestion.impact}] ${suggestion.category}: ${suggestion.suggestion}")
        }
    } else {
        println("未能获取诊断结果")
    }

    // 生成诊断报告
    println("\n生成诊断报告...")
    val diagnosticReport = diagnosticTool.generateDiagnosticReport(agent.id, sessionId)

    if (diagnosticReport != null) {
        println("诊断报告生成成功")

        // 保存诊断报告
        val reportDir = File("reports")
        reportDir.mkdirs()

        val reportFile = File(reportDir, "diagnostic_report.md")
        reportFile.writeText(diagnosticReport.detailedReport)
        println("诊断报告已保存到: ${reportFile.absolutePath}")
    } else {
        println("未能生成诊断报告")
    }

    // 生成性能报告
    println("\n生成性能报告...")
    val reportFileName = performanceReport.generateReportFileName(agent.id, sessionId)
    val reportDir = File("reports")
    reportDir.mkdirs()

    val reportPath = File(reportDir, reportFileName).absolutePath
    performanceReport.generatePerformanceReport(agent.id, sessionId, outputPath = reportPath)
    println("性能报告已保存到: $reportPath")

    // 生成性能可视化
    println("\n生成性能可视化...")
    val visualizationFileName = performanceVisualizer.generateVisualizationFileName(agent.id, sessionId)
    val visualizationPath = File(reportDir, visualizationFileName).absolutePath
    performanceVisualizer.generatePerformanceVisualization(agent.id, sessionId, outputPath = visualizationPath)
    println("性能可视化已保存到: $visualizationPath")

    // 取消监听
    metricsJob.cancel()
    alertsJob.cancel()

    println("\nAgent 性能监控示例结束")
}
