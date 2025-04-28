package ai.kastrax.core.agent.monitoring

import ai.kastrax.core.agent.analysis.*
import ai.kastrax.core.common.KastraXBase
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Agent 性能可视化器，用于可视化 Agent 性能数据
 *
 * @property metricsStorage Agent 指标存储
 */
class AgentPerformanceVisualizer(
    private val metricsStorage: AgentMetricsStorage
) : KastraXBase(component = "AGENT_PERFORMANCE", name = "visualizer") {

    /**
     * 生成性能可视化 HTML
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @param outputPath 输出路径，如果为 null 则返回 HTML 内容
     * @return HTML 内容，如果 outputPath 不为 null 则返回 null
     */
    suspend fun generatePerformanceVisualization(
        agentId: String,
        sessionId: String? = null,
        outputPath: String? = null
    ): String? {
        // 获取性能指标
        val agentMetrics = metricsStorage.getAgentMetrics(agentId, sessionId)
            ?: return null

        // 获取步骤指标
        val stepMetrics = metricsStorage.getStepMetricsForSession(agentId, sessionId)

        // 生成可视化 HTML
        val html = generateVisualizationHtml(agentId, sessionId, agentMetrics, stepMetrics)

        // 如果指定了输出路径，则保存 HTML
        if (outputPath != null) {
            try {
                val file = File(outputPath)
                file.parentFile?.mkdirs()
                file.writeText(html)
                logger.info { "性能可视化已保存到: $outputPath" }
            } catch (e: Exception) {
                logger.error(e) { "保存性能可视化时发生错误" }
            }
            return null
        }

        return html
    }

    /**
     * 生成性能可视化 HTML
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @param agentMetrics Agent 指标
     * @param stepMetrics 步骤指标列表
     * @return HTML 内容
     */
    private fun generateVisualizationHtml(
        agentId: String,
        sessionId: String?,
        agentMetrics: AgentMetrics,
        stepMetrics: List<AgentStepMetrics>
    ): String {
        val timestamp = Clock.System.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(formatter)

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

        // 准备步骤数据
        val stepData = stepMetrics.mapIndexed { index, step ->
            """
            {
                step: ${index + 1},
                name: "${step.stepName}",
                duration: ${step.getDuration()},
                promptTokens: ${step.promptTokens},
                completionTokens: ${step.completionTokens},
                totalTokens: ${step.promptTokens + step.completionTokens},
                toolCalls: ${step.toolCalls.size},
                hasError: false
            }
            """.trimIndent()
        }.joinToString(",\n")

        // 准备工具调用数据
        val toolCallData = stepMetrics.flatMap { step ->
            step.toolCalls.map { toolCall ->
                """
                {
                    step: "${step.stepName}",
                    tool: "${toolCall.toolName}",
                    duration: ${toolCall.getDuration()},
                    success: true
                }
                """.trimIndent()
            }
        }.joinToString(",\n")

        return """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Agent 性能可视化 - $agentId</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels"></script>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                    color: #333;
                }
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    background-color: white;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                }
                h1, h2, h3 {
                    color: #2c3e50;
                }
                .header {
                    margin-bottom: 30px;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .metrics-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
                    gap: 20px;
                    margin-bottom: 30px;
                }
                .metric-card {
                    background-color: #f9f9f9;
                    border-radius: 8px;
                    padding: 15px;
                    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                }
                .metric-title {
                    font-size: 14px;
                    color: #7f8c8d;
                    margin-bottom: 5px;
                }
                .metric-value {
                    font-size: 24px;
                    font-weight: bold;
                    color: #2980b9;
                }
                .chart-container {
                    margin-bottom: 30px;
                    background-color: white;
                    border-radius: 8px;
                    padding: 15px;
                    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                }
                .chart-row {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 20px;
                    margin-bottom: 20px;
                }
                .chart-col {
                    flex: 1;
                    min-width: 300px;
                }
                .footer {
                    margin-top: 30px;
                    text-align: center;
                    color: #7f8c8d;
                    font-size: 12px;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Agent 性能可视化</h1>
                    <p>Agent ID: <strong>$agentId</strong> | 会话 ID: <strong>${sessionId ?: "无"}</strong> | 生成时间: <strong>$formattedTimestamp</strong></p>
                </div>

                <h2>性能摘要</h2>
                <div class="metrics-grid">
                    <div class="metric-card">
                        <div class="metric-title">执行时间</div>
                        <div class="metric-value">${duration}ms</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">步骤数</div>
                        <div class="metric-value">${stepCount}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">平均步骤时间</div>
                        <div class="metric-value">${String.format("%.2f", averageStepDuration)}ms</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">每秒步骤数</div>
                        <div class="metric-value">${String.format("%.2f", stepsPerSecond)}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">总 Token</div>
                        <div class="metric-value">${agentMetrics.totalTokens}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">提示词 Token</div>
                        <div class="metric-value">${agentMetrics.promptTokens}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">完成词 Token</div>
                        <div class="metric-value">${agentMetrics.completionTokens}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">每秒 Token</div>
                        <div class="metric-value">${String.format("%.2f", tokensPerSecond)}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">工具调用次数</div>
                        <div class="metric-value">${agentMetrics.toolCalls}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">每秒工具调用</div>
                        <div class="metric-value">${String.format("%.2f", toolCallsPerSecond)}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">错误次数</div>
                        <div class="metric-value">${agentMetrics.errorCount}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">重试次数</div>
                        <div class="metric-value">${agentMetrics.retryCount}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">错误率</div>
                        <div class="metric-value">${String.format("%.2f", errorRate * 100)}%</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">重试率</div>
                        <div class="metric-value">${String.format("%.2f", retryRate * 100)}%</div>
                    </div>
                </div>

                <h2>性能图表</h2>

                <div class="chart-row">
                    <div class="chart-col">
                        <div class="chart-container">
                            <h3>Token 使用分布</h3>
                            <canvas id="tokenDistributionChart"></canvas>
                        </div>
                    </div>
                    <div class="chart-col">
                        <div class="chart-container">
                            <h3>步骤执行时间</h3>
                            <canvas id="stepDurationChart"></canvas>
                        </div>
                    </div>
                </div>

                <div class="chart-row">
                    <div class="chart-col">
                        <div class="chart-container">
                            <h3>步骤 Token 使用量</h3>
                            <canvas id="stepTokensChart"></canvas>
                        </div>
                    </div>
                    <div class="chart-col">
                        <div class="chart-container">
                            <h3>工具调用分布</h3>
                            <canvas id="toolCallsChart"></canvas>
                        </div>
                    </div>
                </div>

                <div class="chart-container">
                    <h3>步骤性能概览</h3>
                    <canvas id="stepPerformanceChart"></canvas>
                </div>

                <div class="footer">
                    <p>此可视化由 KastraX Agent 性能监控工具自动生成</p>
                </div>
            </div>

            <script>
                // 数据
                const stepData = [
                    $stepData
                ];

                const toolCallData = [
                    $toolCallData
                ];

                // 图表配置
                Chart.register(ChartDataLabels);

                // Token 使用分布图
                const tokenDistributionCtx = document.getElementById('tokenDistributionChart').getContext('2d');
                new Chart(tokenDistributionCtx, {
                    type: 'pie',
                    data: {
                        labels: ['提示词 Token', '完成词 Token'],
                        datasets: [{
                            data: [${agentMetrics.promptTokens}, ${agentMetrics.completionTokens}],
                            backgroundColor: ['#3498db', '#2ecc71'],
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            datalabels: {
                                formatter: (value, ctx) => {
                                    const total = ${agentMetrics.totalTokens};
                                    const percentage = (value / total * 100).toFixed(1) + '%';
                                    return value + ' (' + percentage + ')';
                                },
                                color: '#fff',
                                font: {
                                    weight: 'bold'
                                }
                            },
                            legend: {
                                position: 'bottom'
                            }
                        }
                    }
                });

                // 步骤执行时间图
                const stepDurationCtx = document.getElementById('stepDurationChart').getContext('2d');
                new Chart(stepDurationCtx, {
                    type: 'bar',
                    data: {
                        labels: stepData.map(d => '步骤 ' + d.step + ': ' + d.name),
                        datasets: [{
                            label: '执行时间 (ms)',
                            data: stepData.map(d => d.duration),
                            backgroundColor: stepData.map(d => d.hasError ? '#e74c3c' : '#3498db'),
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: '时间 (ms)'
                                }
                            }
                        },
                        plugins: {
                            legend: {
                                display: false
                            }
                        }
                    }
                });

                // 步骤 Token 使用量图
                const stepTokensCtx = document.getElementById('stepTokensChart').getContext('2d');
                new Chart(stepTokensCtx, {
                    type: 'bar',
                    data: {
                        labels: stepData.map(d => '步骤 ' + d.step + ': ' + d.name),
                        datasets: [
                            {
                                label: '提示词 Token',
                                data: stepData.map(d => d.promptTokens),
                                backgroundColor: '#3498db',
                                borderWidth: 1
                            },
                            {
                                label: '完成词 Token',
                                data: stepData.map(d => d.completionTokens),
                                backgroundColor: '#2ecc71',
                                borderWidth: 1
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: 'Token 数'
                                }
                            },
                            x: {
                                stacked: true
                            }
                        },
                        plugins: {
                            legend: {
                                position: 'bottom'
                            }
                        }
                    }
                });

                // 工具调用分布图
                const toolCallsCtx = document.getElementById('toolCallsChart').getContext('2d');

                // 统计每个工具的调用次数
                const toolCounts = {};
                toolCallData.forEach(d => {
                    if (!toolCounts[d.tool]) {
                        toolCounts[d.tool] = { total: 0, success: 0, failure: 0 };
                    }
                    toolCounts[d.tool].total++;
                    if (d.success) {
                        toolCounts[d.tool].success++;
                    } else {
                        toolCounts[d.tool].failure++;
                    }
                });

                const toolNames = Object.keys(toolCounts);

                new Chart(toolCallsCtx, {
                    type: 'bar',
                    data: {
                        labels: toolNames,
                        datasets: [
                            {
                                label: '成功',
                                data: toolNames.map(name => toolCounts[name].success),
                                backgroundColor: '#2ecc71',
                                borderWidth: 1
                            },
                            {
                                label: '失败',
                                data: toolNames.map(name => toolCounts[name].failure),
                                backgroundColor: '#e74c3c',
                                borderWidth: 1
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            y: {
                                beginAtZero: true,
                                title: {
                                    display: true,
                                    text: '调用次数'
                                }
                            },
                            x: {
                                stacked: true
                            }
                        },
                        plugins: {
                            legend: {
                                position: 'bottom'
                            }
                        }
                    }
                });

                // 步骤性能概览图
                const stepPerformanceCtx = document.getElementById('stepPerformanceChart').getContext('2d');
                new Chart(stepPerformanceCtx, {
                    type: 'scatter',
                    data: {
                        datasets: [{
                            label: '步骤性能',
                            data: stepData.map(d => ({
                                x: d.duration,
                                y: d.totalTokens,
                                r: d.toolCalls * 5 + 5,
                                step: d.step,
                                name: d.name,
                                hasError: d.hasError
                            })),
                            backgroundColor: stepData.map(d => d.hasError ? 'rgba(231, 76, 60, 0.7)' : 'rgba(52, 152, 219, 0.7)'),
                            borderColor: stepData.map(d => d.hasError ? '#c0392b' : '#2980b9'),
                            borderWidth: 1
                        }]
                    },
                    options: {
                        responsive: true,
                        scales: {
                            x: {
                                title: {
                                    display: true,
                                    text: '执行时间 (ms)'
                                }
                            },
                            y: {
                                title: {
                                    display: true,
                                    text: 'Token 使用量'
                                }
                            }
                        },
                        plugins: {
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        const d = context.raw;
                                        return [
                                            '步骤 ' + d.step + ': ' + d.name,
                                            '执行时间: ' + d.x + 'ms',
                                            'Token 使用量: ' + d.y,
                                            '工具调用: ' + ((d.r - 5) / 5),
                                            '错误: ' + (d.hasError ? '是' : '否')
                                        ];
                                    }
                                }
                            },
                            legend: {
                                display: false
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * 生成可视化文件名
     *
     * @param agentId Agent ID
     * @param sessionId 会话 ID，如果有的话
     * @return 文件名
     */
    fun generateVisualizationFileName(agentId: String, sessionId: String?): String {
        val timestamp = Clock.System.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val formattedTimestamp = timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime().format(formatter)

        return if (sessionId != null) {
            "agent_${agentId}_session_${sessionId}_${formattedTimestamp}.html"
        } else {
            "agent_${agentId}_${formattedTimestamp}.html"
        }
    }
}
