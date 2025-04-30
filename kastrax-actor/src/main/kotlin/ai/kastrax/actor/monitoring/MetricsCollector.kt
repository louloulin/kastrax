package ai.kastrax.actor.monitoring

import actor.proto.ActorSystem
import actor.proto.PID
import ai.kastrax.actor.network.AgentNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 指标收集器，用于收集和聚合分布式 Agent 的指标
 */
class MetricsCollector(private val system: ActorSystem) {

    /**
     * 指标数据
     */
    private val metrics = ConcurrentHashMap<String, MutableList<MetricDataPoint>>()

    /**
     * 指标聚合器
     */
    private val aggregators = ConcurrentHashMap<String, MetricAggregator>()

    /**
     * 收集任务
     */
    private var collectionJob: Job? = null

    /**
     * 收集间隔（毫秒）
     */
    private var collectionIntervalMs = 5000L

    /**
     * 最大保留的数据点数量
     */
    private val maxDataPoints = 1000

    /**
     * 开始收集指标
     *
     * @param intervalMs 收集间隔（毫秒）
     */
    fun startCollection(intervalMs: Long = 5000L) {
        if (collectionJob != null) {
            return
        }

        collectionIntervalMs = intervalMs

        collectionJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                collectMetrics()
                delay(collectionIntervalMs)
            }
        }
    }

    /**
     * 停止收集指标
     */
    fun stopCollection() {
        collectionJob?.cancel()
        collectionJob = null
    }

    /**
     * 收集指标
     */
    private suspend fun collectMetrics() {
        // 收集系统指标
        collectSystemMetrics()

        // 收集聚合指标
        aggregateMetrics()
    }

    /**
     * 收集系统指标
     */
    private fun collectSystemMetrics() {
        // 收集 JVM 指标
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        val memoryUsage = if (maxMemory > 0) (usedMemory.toDouble() / maxMemory) * 100 else 0.0

        recordMetric("system.memory.used_mb", usedMemory.toDouble())
        recordMetric("system.memory.max_mb", maxMemory.toDouble())
        recordMetric("system.memory.usage_percent", memoryUsage)
        recordMetric("system.cpu.available_processors", runtime.availableProcessors().toDouble())

        // 收集线程指标
        val threadCount = Thread.activeCount()
        recordMetric("system.threads.count", threadCount.toDouble())
    }

    /**
     * 聚合指标
     */
    private fun aggregateMetrics() {
        aggregators.forEach { (name, aggregator) ->
            val value = aggregator.invoke()
            recordMetric(name, value)
        }
    }

    /**
     * 记录指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 标签
     */
    fun recordMetric(name: String, value: Double, tags: Map<String, String> = emptyMap()) {
        val dataPoints = metrics.getOrPut(name) { mutableListOf() }

        synchronized(dataPoints) {
            dataPoints.add(MetricDataPoint(Clock.System.now(), value, tags))

            // 限制数据点数量
            if (dataPoints.size > maxDataPoints) {
                dataPoints.removeAt(0)
            }
        }
    }

    /**
     * 注册指标聚合器
     *
     * @param name 指标名称
     * @param aggregator 聚合器
     */
    fun registerAggregator(name: String, aggregator: MetricAggregator) {
        aggregators[name] = aggregator
    }

    /**
     * 监控 Agent 网络
     *
     * @param network Agent 网络
     */
    fun monitorAgentNetwork(network: AgentNetwork) {
        // 注册网络指标聚合器
        registerAggregator("network.nodes") {
            network.getTopology().getAllNodes().size.toDouble()
        }

        registerAggregator("network.edges") {
            network.getTopology().getAllEdges().size.toDouble()
        }

        registerAggregator("network.density") {
            val topology = network.getTopology()
            val nodes = topology.getAllNodes()
            val edges = topology.getAllEdges()

            val nodeCount = nodes.size
            if (nodeCount > 1) {
                (2.0 * edges.size) / (nodeCount * (nodeCount - 1))
            } else {
                0.0
            }
        }

        registerAggregator("network.avg_degree") {
            val topology = network.getTopology()
            val nodes = topology.getAllNodes()

            val totalDegree = nodes.sumOf { node ->
                topology.getConnectedNodes(node).size
            }

            val nodeCount = nodes.size
            if (nodeCount > 0) {
                totalDegree.toDouble() / nodeCount
            } else {
                0.0
            }
        }
    }

    /**
     * 获取指标数据
     *
     * @param name 指标名称
     * @param limit 限制返回的数据点数量，默认为所有
     * @return 数据点列表
     */
    fun getMetricData(name: String, limit: Int = Int.MAX_VALUE): List<MetricDataPoint> {
        val dataPoints = metrics[name] ?: return emptyList()

        synchronized(dataPoints) {
            return if (limit >= dataPoints.size) {
                dataPoints.toList()
            } else {
                dataPoints.takeLast(limit)
            }
        }
    }

    /**
     * 获取所有指标名称
     *
     * @return 指标名称列表
     */
    fun getMetricNames(): List<String> {
        return metrics.keys.toList()
    }

    /**
     * 获取指标的最新值
     *
     * @param name 指标名称
     * @return 最新值，如果没有数据则返回 null
     */
    fun getLatestMetricValue(name: String): Double? {
        val dataPoints = metrics[name] ?: return null

        synchronized(dataPoints) {
            return dataPoints.lastOrNull()?.value
        }
    }

    /**
     * 获取指标的统计信息
     *
     * @param name 指标名称
     * @return 统计信息映射，包含 min, max, avg, count
     */
    fun getMetricStats(name: String): Map<String, Double> {
        val dataPoints = metrics[name] ?: return emptyMap()

        synchronized(dataPoints) {
            if (dataPoints.isEmpty()) return emptyMap()

            val values = dataPoints.map { it.value }
            val min = values.minOrNull() ?: 0.0
            val max = values.maxOrNull() ?: 0.0
            val avg = values.average()
            val count = values.size.toDouble()

            return mapOf(
                "min" to min,
                "max" to max,
                "avg" to avg,
                "count" to count
            )
        }
    }

    /**
     * 导出指标数据到 CSV 文件
     *
     * @param outputPath 输出文件路径
     * @param metricNames 要导出的指标名称列表，默认为所有
     * @return 是否成功导出
     */
    fun exportToCsv(outputPath: String, metricNames: List<String>? = null): Boolean {
        try {
            val names = metricNames ?: getMetricNames()
            if (names.isEmpty()) return false

            val sb = StringBuilder()

            // 添加标题行
            sb.append("timestamp,metric_name,value,tags\n")

            // 添加数据行
            names.forEach { name ->
                val dataPoints = getMetricData(name)
                dataPoints.forEach { point ->
                    val timestamp = point.timestamp.toString()
                    val tags = point.tags.entries.joinToString(";") { "${it.key}=${it.value}" }
                    sb.append("$timestamp,$name,${point.value},\"$tags\"\n")
                }
            }

            File(outputPath).writeText(sb.toString())
            return true
        } catch (e: Exception) {
            println("Error exporting metrics to CSV: ${e.message}")
            return false
        }
    }

    /**
     * 生成指标报告 HTML
     *
     * @param title 报告标题
     * @return HTML 内容
     */
    fun generateReportHtml(title: String = "Kastrax Actor Metrics Report"): String {
        // 准备指标数据
        val metricNames = getMetricNames()

        // 准备时间序列数据
        val timeSeriesJson = metricNames.joinToString(",") { metricName ->
            val dataPoints = getMetricData(metricName, 100) // 限制为最近 100 个数据点
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

        // 准备统计数据
        val statsJson = metricNames.joinToString(",") { name ->
            val stats = getMetricStats(name)
            if (stats.isEmpty()) {
                """
                "$name": {"count": 0}
                """.trimIndent()
            } else {
                """
                "$name": {
                  "min": ${stats["min"]},
                  "max": ${stats["max"]},
                  "avg": ${stats["avg"]},
                  "count": ${stats["count"]}
                }
                """.trimIndent()
            }
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
                .header {
                    background-color: #2c3e50;
                    color: white;
                    padding: 20px;
                    margin-bottom: 20px;
                    border-radius: 5px;
                }
                .header h1 {
                    margin: 0;
                }
                .header p {
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
                    min-width: 300px;
                }
                .metric-card h3 {
                    margin-top: 0;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .chart-container {
                    position: relative;
                    height: 200px;
                    margin-bottom: 15px;
                }
                .stats-table {
                    width: 100%;
                    border-collapse: collapse;
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
            <div class="header">
                <h1>$title</h1>
                <p>Generated at: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime.now())}</p>
            </div>

            <div class="metrics-container">
                <!-- Metrics cards will be dynamically generated -->
            </div>

            <div class="footer">
                Powered by Kastrax Actor Metrics Collector
            </div>

            <script>
                // Time series data
                const timeSeriesData = {
                    $timeSeriesJson
                };

                // Stats data
                const statsData = {
                    $statsJson
                };

                // Create metrics cards
                const metricsContainer = document.querySelector('.metrics-container');
                Object.keys(timeSeriesData).forEach(metricName => {
                    const data = timeSeriesData[metricName];
                    const stats = statsData[metricName] || {};

                    const card = document.createElement('div');
                    card.className = 'metric-card';

                    val chartId = "chart-${metricName.replace(".", "-")}"

                    card.innerHTML = `
                        <h3>${metricName}</h3>
                        <div class="chart-container">
                            <canvas id="${chartId}"></canvas>
                        </div>
                        <table class="stats-table">
                            <tr>
                                <th>Min</th>
                                <th>Avg</th>
                                <th>Max</th>
                                <th>Count</th>
                            </tr>
                            <tr>
                                <td>${stats["min"]?.let { String.format("%.2f", it) } ?: "-"}</td>
                                <td>${stats["avg"]?.let { String.format("%.2f", it) } ?: "-"}</td>
                                <td>${stats["max"]?.let { String.format("%.2f", it) } ?: "-"}</td>
                                <td>${stats["count"] ?: 0}</td>
                            </tr>
                        </table>
                    `;

                    metricsContainer.appendChild(card);

                    // Create chart
                    if (data.length > 0) {
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
                                    legend: {
                                        display: false
                                    },
                                    tooltip: {
                                        mode: 'index',
                                        intersect: false
                                    }
                                }
                            }
                        });
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * 保存指标报告到文件
     *
     * @param outputPath 输出文件路径
     * @param title 报告标题
     * @return 是否成功保存
     */
    fun saveReport(outputPath: String, title: String = "Kastrax Actor Metrics Report"): Boolean {
        try {
            val html = generateReportHtml(title)
            File(outputPath).writeText(html)
            return true
        } catch (e: Exception) {
            println("Error saving report: ${e.message}")
            return false
        }
    }
}

/**
 * 指标聚合器函数类型
 */
typealias MetricAggregator = () -> Double
