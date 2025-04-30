package ai.kastrax.actor.monitoring

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.network.AgentNetwork
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * 性能指标类型
 */
enum class MetricType {
    COUNTER,    // 计数器，只增不减
    GAUGE,      // 仪表，可增可减
    HISTOGRAM,  // 直方图，记录值的分布
    TIMER       // 计时器，记录操作耗时
}

/**
 * 性能指标
 */
data class Metric(
    val name: String,
    val type: MetricType,
    val description: String,
    val tags: Map<String, String> = emptyMap()
)

// 使用已经定义的 MetricDataPoint 类

/**
 * 性能仪表盘，用于收集和可视化 Actor 系统的性能指标
 */
class PerformanceDashboard(private val system: ActorSystem) {

    /**
     * 指标定义映射
     */
    private val metrics = ConcurrentHashMap<String, Metric>()

    /**
     * 计数器指标数据
     */
    private val counters = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 仪表指标数据
     */
    private val gauges = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 直方图指标数据
     */
    private val histograms = ConcurrentHashMap<String, MutableList<Double>>()

    /**
     * 计时器指标数据
     */
    private val timers = ConcurrentHashMap<String, MutableList<Double>>()

    /**
     * 指标时间序列数据
     */
    private val timeSeriesData = ConcurrentHashMap<String, MutableList<MetricDataPoint>>()

    /**
     * 最大保留的时间序列数据点数量
     */
    private val maxDataPoints = 1000

    /**
     * 注册指标
     *
     * @param metric 指标定义
     */
    fun registerMetric(metric: Metric) {
        metrics[metric.name] = metric

        // 初始化指标数据结构
        when (metric.type) {
            MetricType.COUNTER -> counters[metric.name] = AtomicLong(0)
            MetricType.GAUGE -> gauges[metric.name] = AtomicLong(0)
            MetricType.HISTOGRAM -> histograms[metric.name] = mutableListOf()
            MetricType.TIMER -> timers[metric.name] = mutableListOf()
        }

        // 初始化时间序列数据
        timeSeriesData[metric.name] = mutableListOf()
    }

    /**
     * 增加计数器值
     *
     * @param name 指标名称
     * @param value 增加的值
     * @param tags 标签
     */
    fun incrementCounter(name: String, value: Long = 1, tags: Map<String, String> = emptyMap()) {
        val counter = counters[name] ?: return
        val newValue = counter.addAndGet(value)

        // 记录数据点
        recordDataPoint(name, newValue.toDouble(), tags)
    }

    /**
     * 设置仪表值
     *
     * @param name 指标名称
     * @param value 设置的值
     * @param tags 标签
     */
    fun setGauge(name: String, value: Long, tags: Map<String, String> = emptyMap()) {
        val gauge = gauges[name] ?: return
        gauge.set(value)

        // 记录数据点
        recordDataPoint(name, value.toDouble(), tags)
    }

    /**
     * 记录直方图值
     *
     * @param name 指标名称
     * @param value 记录的值
     * @param tags 标签
     */
    fun recordHistogram(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        val histogram = histograms[name] ?: return
        synchronized(histogram) {
            histogram.add(value)
        }

        // 记录数据点
        recordDataPoint(name, value, tags)
    }

    /**
     * 记录计时器值
     *
     * @param name 指标名称
     * @param durationMs 持续时间（毫秒）
     * @param tags 标签
     */
    fun recordTimer(name: String, durationMs: Double, tags: Map<String, String> = emptyMap()) {
        val timer = timers[name] ?: return
        synchronized(timer) {
            timer.add(durationMs)
        }

        // 记录数据点
        recordDataPoint(name, durationMs, tags)
    }

    /**
     * 记录数据点
     *
     * @param name 指标名称
     * @param value 值
     * @param tags 标签
     */
    private fun recordDataPoint(name: String, value: Double, tags: Map<String, String>) {
        val dataPoints = timeSeriesData[name] ?: return
        synchronized(dataPoints) {
            dataPoints.add(MetricDataPoint(Clock.System.now(), value, tags))

            // 限制数据点数量
            if (dataPoints.size > maxDataPoints) {
                dataPoints.removeAt(0)
            }
        }
    }

    /**
     * 获取计数器当前值
     *
     * @param name 指标名称
     * @return 计数器值
     */
    fun getCounterValue(name: String): Long {
        return counters[name]?.get() ?: 0
    }

    /**
     * 获取仪表当前值
     *
     * @param name 指标名称
     * @return 仪表值
     */
    fun getGaugeValue(name: String): Long {
        return gauges[name]?.get() ?: 0
    }

    /**
     * 获取直方图统计信息
     *
     * @param name 指标名称
     * @return 统计信息映射，包含 min, max, avg, count, p50, p90, p99
     */
    fun getHistogramStats(name: String): Map<String, Double> {
        val histogram = histograms[name] ?: return emptyMap()
        synchronized(histogram) {
            if (histogram.isEmpty()) return emptyMap()

            val sorted = histogram.sorted()
            val min = sorted.first()
            val max = sorted.last()
            val avg = sorted.average()
            val count = sorted.size.toDouble()
            val p50 = sorted[((sorted.size - 1) * 0.5).toInt()]
            val p90 = sorted[((sorted.size - 1) * 0.9).toInt()]
            val p99 = sorted[((sorted.size - 1) * 0.99).toInt()]

            return mapOf(
                "min" to min,
                "max" to max,
                "avg" to avg,
                "count" to count,
                "p50" to p50,
                "p90" to p90,
                "p99" to p99
            )
        }
    }

    /**
     * 获取计时器统计信息
     *
     * @param name 指标名称
     * @return 统计信息映射，包含 min, max, avg, count, p50, p90, p99
     */
    fun getTimerStats(name: String): Map<String, Double> {
        val timer = timers[name] ?: return emptyMap()
        synchronized(timer) {
            if (timer.isEmpty()) return emptyMap()

            val sorted = timer.sorted()
            val min = sorted.first()
            val max = sorted.last()
            val avg = sorted.average()
            val count = sorted.size.toDouble()
            val p50 = sorted[((sorted.size - 1) * 0.5).toInt()]
            val p90 = sorted[((sorted.size - 1) * 0.9).toInt()]
            val p99 = sorted[((sorted.size - 1) * 0.99).toInt()]

            return mapOf(
                "min" to min,
                "max" to max,
                "avg" to avg,
                "count" to count,
                "p50" to p50,
                "p90" to p90,
                "p99" to p99
            )
        }
    }

    /**
     * 获取指标的时间序列数据
     *
     * @param name 指标名称
     * @param limit 限制返回的数据点数量，默认为所有
     * @return 数据点列表
     */
    fun getTimeSeriesData(name: String, limit: Int = Int.MAX_VALUE): List<MetricDataPoint> {
        val dataPoints = timeSeriesData[name] ?: return emptyList()
        synchronized(dataPoints) {
            return if (limit >= dataPoints.size) {
                dataPoints.toList()
            } else {
                dataPoints.takeLast(limit)
            }
        }
    }

    /**
     * 监控 Agent 网络
     *
     * @param network Agent 网络
     */
    fun monitorAgentNetwork(network: AgentNetwork) {
        // 注册网络指标
        registerMetric(Metric("network.nodes", MetricType.GAUGE, "Number of nodes in the network"))
        registerMetric(Metric("network.edges", MetricType.GAUGE, "Number of edges in the network"))
        registerMetric(Metric("network.density", MetricType.GAUGE, "Network density"))
        registerMetric(Metric("network.avg_degree", MetricType.GAUGE, "Average node degree"))

        // 更新网络指标
        val topology = network.getTopology()
        val nodes = topology.getAllNodes()
        val edges = topology.getAllEdges()

        setGauge("network.nodes", nodes.size.toLong())
        setGauge("network.edges", edges.size.toLong())

        // 计算网络密度
        val nodeCount = nodes.size
        val density = if (nodeCount > 1) {
            (2.0 * edges.size) / (nodeCount * (nodeCount - 1))
        } else {
            0.0
        }
        setGauge("network.density", (density * 1000).toLong()) // 存储为千分比

        // 计算平均度
        val totalDegree = nodes.sumOf { node ->
            topology.getConnectedNodes(node).size
        }
        val avgDegree = if (nodeCount > 0) {
            totalDegree.toDouble() / nodeCount
        } else {
            0.0
        }
        setGauge("network.avg_degree", (avgDegree * 100).toLong()) // 存储为百分比
    }

    /**
     * 监控 Actor
     *
     * @param pid Actor PID
     * @param name Actor 名称
     */
    fun monitorActor(pid: PID, name: String) {
        // 注册 Actor 指标
        registerMetric(Metric("actor.$name.messages", MetricType.COUNTER, "Number of messages processed by the actor"))
        registerMetric(Metric("actor.$name.processing_time", MetricType.TIMER, "Message processing time (ms)"))
        registerMetric(Metric("actor.$name.errors", MetricType.COUNTER, "Number of errors encountered by the actor"))
    }

    /**
     * 生成性能仪表盘 HTML
     *
     * @param title 仪表盘标题
     * @return HTML 内容
     */
    fun generateDashboardHtml(title: String = "Kastrax Actor Performance Dashboard"): String {
        // 准备时间序列数据
        val timeSeriesJson = metrics.keys.joinToString(",") { metricName ->
            val dataPoints = getTimeSeriesData(metricName)
            val dataJson = dataPoints.joinToString(",") { point ->
                """
                {
                  "timestamp": ${point.timestamp.toEpochMilliseconds()},
                  "value": ${point.value}
                }
                """.trimIndent()
            }

            """
            "$metricName": [
              $dataJson
            ]
            """.trimIndent()
        }

        // 准备指标定义数据
        val metricDefinitionsJson = metrics.entries.joinToString(",") { (name, metric) ->
            """
            "$name": {
                "type": "${metric.type}",
                "description": "${metric.description.replace("\"", "\\\"")}"
            }
            """.trimIndent()
        }

        // 准备统计数据
        val statsJson = metrics.entries.joinToString(",") { (name, metric) ->
            val statsData = when (metric.type) {
                MetricType.COUNTER -> """{"value": ${getCounterValue(name)}}"""
                MetricType.GAUGE -> """{"value": ${getGaugeValue(name)}}"""
                MetricType.HISTOGRAM -> {
                    val stats = getHistogramStats(name)
                    if (stats.isEmpty()) {
                        """{"count": 0}"""
                    } else {
                        """
                        {
                          "min": ${stats["min"]},
                          "max": ${stats["max"]},
                          "avg": ${stats["avg"]},
                          "count": ${stats["count"]},
                          "p50": ${stats["p50"]},
                          "p90": ${stats["p90"]},
                          "p99": ${stats["p99"]}
                        }
                        """.trimIndent()
                    }
                }
                MetricType.TIMER -> {
                    val stats = getTimerStats(name)
                    if (stats.isEmpty()) {
                        """{"count": 0}"""
                    } else {
                        """
                        {
                          "min": ${stats["min"]},
                          "max": ${stats["max"]},
                          "avg": ${stats["avg"]},
                          "count": ${stats["count"]},
                          "p50": ${stats["p50"]},
                          "p90": ${stats["p90"]},
                          "p99": ${stats["p99"]}
                        }
                        """.trimIndent()
                    }
                }
            }

            """
            "$name": $statsData
            """.trimIndent()
        }

        // 准备指标定义
        val metricsJson = metrics.values.joinToString(",") { metric ->
            """
            {
              "name": "${metric.name}",
              "type": "${metric.type}",
              "description": "${metric.description}"
            }
            """.trimIndent()
        }

        // 生成 HTML
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>$title</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <script src="https://cdn.jsdelivr.net/npm/moment"></script>
            <script src="https://cdn.jsdelivr.net/npm/chartjs-adapter-moment"></script>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .dashboard-header {
                    background-color: #2c3e50;
                    color: white;
                    padding: 20px;
                    margin-bottom: 20px;
                    border-radius: 5px;
                }
                .dashboard-header h1 {
                    margin: 0;
                }
                .dashboard-header p {
                    margin: 5px 0 0 0;
                    opacity: 0.8;
                }
                .metrics-container {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 20px;
                    margin-bottom: 20px;
                }
                .metric-card {
                    background-color: white;
                    border-radius: 5px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    padding: 15px;
                    flex: 1;
                    min-width: 250px;
                }
                .metric-card h3 {
                    margin-top: 0;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .metric-card p {
                    color: #7f8c8d;
                    margin-bottom: 15px;
                }
                .metric-value {
                    font-size: 24px;
                    font-weight: bold;
                    color: #2980b9;
                }
                .charts-container {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 20px;
                }
                .chart-card {
                    background-color: white;
                    border-radius: 5px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    padding: 15px;
                    flex: 1;
                    min-width: 500px;
                }
                .chart-card h3 {
                    margin-top: 0;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .chart-container {
                    position: relative;
                    height: 300px;
                }
                .stats-table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 15px;
                }
                .stats-table th, .stats-table td {
                    padding: 8px;
                    text-align: left;
                    border-bottom: 1px solid #ddd;
                }
                .stats-table th {
                    background-color: #f2f2f2;
                }
                .footer {
                    text-align: center;
                    margin-top: 30px;
                    color: #7f8c8d;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="dashboard-header">
                <h1>$title</h1>
                <p>Generated at: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now())}</p>
            </div>

            <div class="metrics-container">
                <!-- Metrics cards will be dynamically generated -->
            </div>

            <div class="charts-container">
                <!-- Charts will be dynamically generated -->
            </div>

            <div class="footer">
                Powered by Kastrax Actor Performance Dashboard
            </div>

            <script>
                // Metrics definitions
                const metrics = [
                    $metricsJson
                ];

                // Time series data
                const timeSeriesData = {
                    $timeSeriesJson
                };

                // Stats data
                const statsData = {
                    $statsJson
                };

                // Metric definitions
                const metricDefinitions = {
                    $metricDefinitionsJson
                };

                // Create metrics cards
                const metricsContainer = document.querySelector('.metrics-container');
                Object.keys(statsData).forEach(metricName => {
                    const stats = statsData[metricName] || {};
                    const metricInfo = metricDefinitions[metricName] || { type: 'GAUGE', description: '' };

                    let valueHtml = '';
                    if (metricInfo.type === 'COUNTER' || metricInfo.type === 'GAUGE') {
                        valueHtml = `<div class="metric-value">${stats.value || 0}</div>`;
                    } else {
                        valueHtml = `
                            <table class="stats-table">
                                <tr>
                                    <th>Count</th>
                                    <th>Min</th>
                                    <th>Avg</th>
                                    <th>Max</th>
                                    <th>P90</th>
                                </tr>
                                <tr>
                                    <td>${stats.count || 0}</td>
                                    <td>${stats.min ? stats.min.toFixed(2) : '-'}</td>
                                    <td>${stats.avg ? stats.avg.toFixed(2) : '-'}</td>
                                    <td>${stats.max ? stats.max.toFixed(2) : '-'}</td>
                                    <td>${stats.p90 ? stats.p90.toFixed(2) : '-'}</td>
                                </tr>
                            </table>
                        `;
                    }

                    const card = document.createElement('div');
                    card.className = 'metric-card';
                    card.innerHTML = `
                        <h3>${metricName}</h3>
                        <p>${metricInfo.description}</p>
                        ${valueHtml}
                    `;
                    metricsContainer.appendChild(card);
                });

                // Create charts
                const chartsContainer = document.querySelector('.charts-container');
                Object.keys(timeSeriesData).forEach(metricName => {
                    const data = timeSeriesData[metricName] || [];
                    if (data.length === 0) return;

                    const chartCard = document.createElement('div');
                    chartCard.className = 'chart-card';

                    const chartId = `chart-${metricName.replace(/\./g, '-')}`;
                    chartCard.innerHTML = `
                        <h3>${metricName}</h3>
                        <div class="chart-container">
                            <canvas id="${chartId}"></canvas>
                        </div>
                    `;
                    chartsContainer.appendChild(chartCard);

                    const ctx = document.getElementById(chartId).getContext('2d');
                    new Chart(ctx, {
                        type: 'line',
                        data: {
                            datasets: [{
                                label: metricName,
                                data: data.map(point => ({
                                    x: point.timestamp,
                                    y: point.value
                                })),
                                borderColor: '#3498db',
                                backgroundColor: 'rgba(52, 152, 219, 0.1)',
                                borderWidth: 2,
                                fill: true,
                                tension: 0.1
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            scales: {
                                x: {
                                    type: 'time',
                                    time: {
                                        unit: 'minute'
                                    },
                                    title: {
                                        display: true,
                                        text: 'Time'
                                    }
                                },
                                y: {
                                    beginAtZero: true,
                                    title: {
                                        display: true,
                                        text: 'Value'
                                    }
                                }
                            },
                            plugins: {
                                tooltip: {
                                    mode: 'index',
                                    intersect: false
                                }
                            }
                        }
                    });
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * 保存仪表盘到文件
     *
     * @param outputPath 输出文件路径
     * @param title 仪表盘标题
     * @return 是否成功保存
     */
    fun saveDashboard(outputPath: String, title: String = "Kastrax Actor Performance Dashboard"): Boolean {
        try {
            val html = generateDashboardHtml(title)
            File(outputPath).writeText(html)
            return true
        } catch (e: Exception) {
            println("Error saving dashboard: ${e.message}")
            return false
        }
    }
}
