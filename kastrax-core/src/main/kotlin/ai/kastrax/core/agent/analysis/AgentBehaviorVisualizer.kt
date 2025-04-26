package ai.kastrax.core.agent.analysis

import ai.kastrax.core.common.KastraXBase
import java.io.File
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant

/**
 * 代理行为可视化工具，用于生成代理行为的可视化报告
 *
 * @property metricsCollector 指标收集器
 */
class AgentBehaviorVisualizer(
    private val metricsStorage: AgentMetricsStorage
) : KastraXBase(component = "AGENT_BEHAVIOR", name = "visualizer") {

    /**
     * 生成代理行为的Mermaid图表
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param outputPath 输出路径，如果为null则返回图表内容
     * @return 图表内容，如果outputPath不为null则返回null
     */
    suspend fun generateMermaidDiagram(
        agentId: String,
        sessionId: String?,
        outputPath: String? = null
    ): String? {
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId) ?: return null
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

        if (stepMetrics.isEmpty()) {
            logger.warn { "没有找到代理步骤指标: 代理: $agentId, 会话: $sessionId" }
            return null
        }

        val sb = StringBuilder()

        // 开始Mermaid图表
        sb.appendLine("```mermaid")
        sb.appendLine("gantt")
        sb.appendLine("    title 代理执行时间线: ${agentMetrics.agentId}")
        sb.appendLine("    dateFormat  YYYY-MM-DD HH:mm:ss.SSS")
        sb.appendLine("    axisFormat %H:%M:%S.%L")

        // 设置时间范围
        val startTime = agentMetrics.startTime.toJavaInstant()
        val endTime = agentMetrics.endTime?.toJavaInstant() ?: java.time.Instant.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        sb.appendLine("    section 代理执行")
        sb.appendLine("    总执行时间: done, ${formatter.format(startTime)}, ${formatter.format(endTime)}")

        // 添加每个步骤
        stepMetrics.sortedBy { it.startTime }.forEach { step ->
            val stepStart = step.startTime.toJavaInstant()
            val stepEnd = step.endTime?.toJavaInstant() ?: java.time.Instant.now()
            val statusClass = when (step.status) {
                AgentStepStatus.COMPLETED -> "done"
                AgentStepStatus.FAILED -> "crit"
                AgentStepStatus.SKIPPED -> "milestone"
                AgentStepStatus.WAITING -> "active"
                AgentStepStatus.RUNNING -> "active"
                else -> "active"
            }

            sb.appendLine("    section ${step.stepName}")
            sb.appendLine("    ${step.stepType}: ${statusClass}, ${formatter.format(stepStart)}, ${formatter.format(stepEnd)}")

            // 添加工具调用
            step.toolCalls.sortedBy { it.startTime }.forEach { toolCall ->
                val toolStart = toolCall.startTime.toJavaInstant()
                val toolEnd = toolCall.endTime?.toJavaInstant() ?: java.time.Instant.now()
                val toolStatusClass = when (toolCall.status) {
                    ToolCallStatus.COMPLETED -> "done"
                    ToolCallStatus.FAILED -> "crit"
                    ToolCallStatus.RUNNING -> "active"
                    else -> "active"
                }

                sb.appendLine("    ${toolCall.toolName}: ${toolStatusClass}, ${formatter.format(toolStart)}, ${formatter.format(toolEnd)}")
            }
        }

        sb.appendLine("```")

        val diagram = sb.toString()

        // 如果指定了输出路径，则保存到文件
        if (outputPath != null) {
            try {
                File(outputPath).writeText(diagram)
                logger.info { "已保存Mermaid图表到: $outputPath" }
                return null
            } catch (e: Exception) {
                logger.error(e) { "保存Mermaid图表失败: ${e.message}" }
                return diagram
            }
        }

        return diagram
    }

    /**
     * 生成代理行为的HTML报告
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID，如果有的话
     * @param analysis 代理行为分析结果，如果为null则自动获取
     * @param outputPath 输出路径，如果为null则返回报告内容
     * @return 报告内容，如果outputPath不为null则返回null
     */
    suspend fun generateHtmlReport(
        agentId: String,
        sessionId: String?,
        analysis: AgentBehaviorAnalysis? = null,
        outputPath: String? = null
    ): String? {
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId) ?: return null
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

        if (stepMetrics.isEmpty()) {
            logger.warn { "没有找到代理步骤指标: 代理: $agentId, 会话: $sessionId" }
            return null
        }

        // 如果没有提供分析结果，则创建一个分析器并获取分析结果
        val behaviorAnalysis = analysis ?: run {
            val analyzer = AgentBehaviorAnalyzer(metricsStorage)
            analyzer.analyzeAgentBehavior(agentId, sessionId) ?: return null
        }

        val sb = StringBuilder()

        // 生成HTML报告
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang=\"zh-CN\">")
        sb.appendLine("<head>")
        sb.appendLine("    <meta charset=\"UTF-8\">")
        sb.appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        sb.appendLine("    <title>代理行为分析报告: ${agentMetrics.agentId}</title>")
        sb.appendLine("    <style>")
        sb.appendLine("        body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 20px; color: #333; }")
        sb.appendLine("        h1, h2, h3 { color: #2c3e50; }")
        sb.appendLine("        .container { max-width: 1200px; margin: 0 auto; }")
        sb.appendLine("        .card { background: #fff; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); padding: 20px; margin-bottom: 20px; }")
        sb.appendLine("        .metric { display: inline-block; width: 30%; margin: 10px 1%; background: #f8f9fa; padding: 15px; border-radius: 5px; box-sizing: border-box; }")
        sb.appendLine("        .metric h3 { margin-top: 0; font-size: 16px; }")
        sb.appendLine("        .metric p { font-size: 24px; font-weight: bold; margin: 5px 0; }")
        sb.appendLine("        table { width: 100%; border-collapse: collapse; margin: 20px 0; }")
        sb.appendLine("        th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #ddd; }")
        sb.appendLine("        th { background-color: #f8f9fa; }")
        sb.appendLine("        tr:hover { background-color: #f1f1f1; }")
        sb.appendLine("        .suggestion { background-color: #e8f4f8; padding: 15px; border-left: 4px solid #3498db; margin-bottom: 15px; }")
        sb.appendLine("        .suggestion h4 { margin-top: 0; color: #3498db; }")
        sb.appendLine("        .high { border-left-color: #e74c3c; }")
        sb.appendLine("        .high h4 { color: #e74c3c; }")
        sb.appendLine("        .medium { border-left-color: #f39c12; }")
        sb.appendLine("        .medium h4 { color: #f39c12; }")
        sb.appendLine("        .chart { width: 100%; height: 400px; margin: 20px 0; }")
        sb.appendLine("    </style>")
        sb.appendLine("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        sb.appendLine("    <div class=\"container\">")
        sb.appendLine("        <div class=\"card\">")
        sb.appendLine("            <h1>代理行为分析报告</h1>")
        sb.appendLine("            <p>代理ID: ${agentMetrics.agentId}</p>")
        sessionId?.let { sb.appendLine("            <p>会话ID: $it</p>") }
        sb.appendLine("            <p>分析时间: ${formatInstant(behaviorAnalysis.analysisTime)}</p>")
        sb.appendLine("        </div>")

        // 关键指标
        sb.appendLine("        <div class=\"card\">")
        sb.appendLine("            <h2>关键指标</h2>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>总执行时间</h3>")
        sb.appendLine("                <p>${formatDuration(behaviorAnalysis.totalDuration)}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>总Token使用</h3>")
        sb.appendLine("                <p>${behaviorAnalysis.totalTokens}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>工具调用次数</h3>")
        sb.appendLine("                <p>${behaviorAnalysis.totalToolCalls}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>平均步骤时间</h3>")
        sb.appendLine("                <p>${formatDuration(behaviorAnalysis.averageStepDuration.toLong())}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>错误次数</h3>")
        sb.appendLine("                <p>${behaviorAnalysis.totalErrors}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("            <div class=\"metric\">")
        sb.appendLine("                <h3>重试次数</h3>")
        sb.appendLine("                <p>${behaviorAnalysis.totalRetries}</p>")
        sb.appendLine("            </div>")
        sb.appendLine("        </div>")

        // 步骤执行时间图表
        sb.appendLine("        <div class=\"card\">")
        sb.appendLine("            <h2>步骤执行时间</h2>")
        sb.appendLine("            <canvas id=\"stepDurationChart\" class=\"chart\"></canvas>")
        sb.appendLine("        </div>")

        // 瓶颈步骤
        if (behaviorAnalysis.bottleneckSteps.isNotEmpty()) {
            sb.appendLine("        <div class=\"card\">")
            sb.appendLine("            <h2>性能瓶颈步骤</h2>")
            sb.appendLine("            <table>")
            sb.appendLine("                <thead>")
            sb.appendLine("                    <tr>")
            sb.appendLine("                        <th>步骤名称</th>")
            sb.appendLine("                        <th>类型</th>")
            sb.appendLine("                        <th>持续时间</th>")
            sb.appendLine("                        <th>占总时间比例</th>")
            sb.appendLine("                        <th>工具调用次数</th>")
            sb.appendLine("                        <th>Token使用量</th>")
            sb.appendLine("                    </tr>")
            sb.appendLine("                </thead>")
            sb.appendLine("                <tbody>")

            behaviorAnalysis.bottleneckSteps.forEach { bottleneck ->
                sb.appendLine("                    <tr>")
                sb.appendLine("                        <td>${bottleneck.stepName}</td>")
                sb.appendLine("                        <td>${bottleneck.stepType}</td>")
                sb.appendLine("                        <td>${formatDuration(bottleneck.duration)}</td>")
                sb.appendLine("                        <td>${String.format("%.2f", bottleneck.percentageOfTotal)}%</td>")
                sb.appendLine("                        <td>${bottleneck.toolCalls}</td>")
                sb.appendLine("                        <td>${bottleneck.tokenUsage}</td>")
                sb.appendLine("                    </tr>")
            }

            sb.appendLine("                </tbody>")
            sb.appendLine("            </table>")
            sb.appendLine("        </div>")
        }

        // 高错误率步骤
        if (behaviorAnalysis.highErrorSteps.isNotEmpty()) {
            sb.appendLine("        <div class=\"card\">")
            sb.appendLine("            <h2>高错误率步骤</h2>")
            sb.appendLine("            <table>")
            sb.appendLine("                <thead>")
            sb.appendLine("                    <tr>")
            sb.appendLine("                        <th>步骤名称</th>")
            sb.appendLine("                        <th>类型</th>")
            sb.appendLine("                        <th>重试次数</th>")
            sb.appendLine("                        <th>工具调用错误次数</th>")
            sb.appendLine("                        <th>错误信息</th>")
            sb.appendLine("                    </tr>")
            sb.appendLine("                </thead>")
            sb.appendLine("                <tbody>")

            behaviorAnalysis.highErrorSteps.forEach { errorStep ->
                sb.appendLine("                    <tr>")
                sb.appendLine("                        <td>${errorStep.stepName}</td>")
                sb.appendLine("                        <td>${errorStep.stepType}</td>")
                sb.appendLine("                        <td>${errorStep.retryCount}</td>")
                sb.appendLine("                        <td>${errorStep.toolCallErrors}</td>")
                sb.appendLine("                        <td>${errorStep.errorMessage ?: "无"}</td>")
                sb.appendLine("                    </tr>")
            }

            sb.appendLine("                </tbody>")
            sb.appendLine("            </table>")
            sb.appendLine("        </div>")
        }

        // 优化建议
        if (behaviorAnalysis.optimizationSuggestions.isNotEmpty()) {
            sb.appendLine("        <div class=\"card\">")
            sb.appendLine("            <h2>优化建议</h2>")

            behaviorAnalysis.optimizationSuggestions.forEach { suggestion ->
                val impactClass = when (suggestion.impact.lowercase()) {
                    "高" -> "high"
                    "中" -> "medium"
                    else -> ""
                }

                sb.appendLine("            <div class=\"suggestion ${impactClass}\">")
                sb.appendLine("                <h4>${suggestion.category}</h4>")
                sb.appendLine("                <p><strong>建议:</strong> ${suggestion.suggestion}</p>")
                sb.appendLine("                <p><strong>影响程度:</strong> ${suggestion.impact}</p>")
                sb.appendLine("                <p><strong>详细信息:</strong> ${suggestion.details}</p>")
                sb.appendLine("            </div>")
            }

            sb.appendLine("        </div>")
        }

        // 步骤详情
        sb.appendLine("        <div class=\"card\">")
        sb.appendLine("            <h2>步骤详情</h2>")
        sb.appendLine("            <table>")
        sb.appendLine("                <thead>")
        sb.appendLine("                    <tr>")
        sb.appendLine("                        <th>步骤名称</th>")
        sb.appendLine("                        <th>类型</th>")
        sb.appendLine("                        <th>状态</th>")
        sb.appendLine("                        <th>开始时间</th>")
        sb.appendLine("                        <th>结束时间</th>")
        sb.appendLine("                        <th>持续时间</th>")
        sb.appendLine("                        <th>Token使用量</th>")
        sb.appendLine("                    </tr>")
        sb.appendLine("                </thead>")
        sb.appendLine("                <tbody>")

        stepMetrics.sortedBy { it.startTime }.forEach { step ->
            val statusClass = when (step.status) {
                AgentStepStatus.COMPLETED -> "success"
                AgentStepStatus.FAILED -> "danger"
                AgentStepStatus.SKIPPED -> "warning"
                else -> ""
            }

            sb.appendLine("                    <tr class=\"${statusClass}\">")
            sb.appendLine("                        <td>${step.stepName}</td>")
            sb.appendLine("                        <td>${step.stepType}</td>")
            sb.appendLine("                        <td>${step.status}</td>")
            sb.appendLine("                        <td>${formatInstant(step.startTime)}</td>")
            sb.appendLine("                        <td>${step.endTime?.let { formatInstant(it) } ?: "进行中"}</td>")
            sb.appendLine("                        <td>${formatDuration(step.getDuration())}</td>")
            sb.appendLine("                        <td>${step.totalTokens}</td>")
            sb.appendLine("                    </tr>")
        }

        sb.appendLine("                </tbody>")
        sb.appendLine("            </table>")
        sb.appendLine("        </div>")

        // JavaScript图表
        sb.appendLine("        <script>")
        sb.appendLine("            document.addEventListener('DOMContentLoaded', function() {")
        sb.appendLine("                // 步骤执行时间图表")
        sb.appendLine("                const stepCtx = document.getElementById('stepDurationChart').getContext('2d');")
        sb.appendLine("                new Chart(stepCtx, {")
        sb.appendLine("                    type: 'bar',")
        sb.appendLine("                    data: {")
        sb.appendLine("                        labels: [${stepMetrics.sortedBy { it.startTime }.joinToString(", ") { "'${it.stepName}'" }}],")
        sb.appendLine("                        datasets: [{")
        sb.appendLine("                            label: '执行时间 (毫秒)',")
        sb.appendLine("                            data: [${stepMetrics.sortedBy { it.startTime }.joinToString(", ") { it.getDuration().toString() }}],")
        sb.appendLine("                            backgroundColor: [${stepMetrics.sortedBy { it.startTime }.joinToString(", ") {
                                                    when (it.status) {
                                                        AgentStepStatus.COMPLETED -> "'rgba(75, 192, 192, 0.2)'"
                                                        AgentStepStatus.FAILED -> "'rgba(255, 99, 132, 0.2)'"
                                                        AgentStepStatus.SKIPPED -> "'rgba(255, 206, 86, 0.2)'"
                                                        else -> "'rgba(54, 162, 235, 0.2)'"
                                                    }
                                                }}],")
        sb.appendLine("                            borderColor: [${stepMetrics.sortedBy { it.startTime }.joinToString(", ") {
                                                    when (it.status) {
                                                        AgentStepStatus.COMPLETED -> "'rgba(75, 192, 192, 1)'"
                                                        AgentStepStatus.FAILED -> "'rgba(255, 99, 132, 1)'"
                                                        AgentStepStatus.SKIPPED -> "'rgba(255, 206, 86, 1)'"
                                                        else -> "'rgba(54, 162, 235, 1)'"
                                                    }
                                                }}],")
        sb.appendLine("                            borderWidth: 1")
        sb.appendLine("                        }]")
        sb.appendLine("                    },")
        sb.appendLine("                    options: {")
        sb.appendLine("                        scales: {")
        sb.appendLine("                            y: {")
        sb.appendLine("                                beginAtZero: true")
        sb.appendLine("                            }")
        sb.appendLine("                        }")
        sb.appendLine("                    }")
        sb.appendLine("                });")
        sb.appendLine("            });")
        sb.appendLine("        </script>")

        sb.appendLine("    </div>")
        sb.appendLine("</body>")
        sb.appendLine("</html>")

        val report = sb.toString()

        // 如果指定了输出路径，则保存到文件
        if (outputPath != null) {
            try {
                File(outputPath).writeText(report)
                logger.info { "已保存HTML报告到: $outputPath" }
                return null
            } catch (e: Exception) {
                logger.error(e) { "保存HTML报告失败: ${e.message}" }
                return report
            }
        }

        return report
    }

    /**
     * 格式化Instant为可读字符串
     *
     * @param instant Instant对象
     * @return 格式化后的字符串
     */
    private fun formatInstant(instant: Instant): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return formatter.format(instant.toJavaInstant())
    }

    /**
     * 格式化持续时间为可读字符串
     *
     * @param millis 毫秒数
     * @return 格式化后的字符串
     */
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%d小时 %d分钟 %d秒", hours, minutes % 60, seconds % 60)
            minutes > 0 -> String.format("%d分钟 %d秒", minutes, seconds % 60)
            seconds > 0 -> String.format("%d秒 %d毫秒", seconds, millis % 1000)
            else -> String.format("%d毫秒", millis)
        }
    }
}
