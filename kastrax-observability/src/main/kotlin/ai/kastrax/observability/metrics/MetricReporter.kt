package ai.kastrax.observability.metrics

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 指标报告器接口。
 */
interface MetricReporter {
    /**
     * 报告指标。
     *
     * @param metrics 指标集合
     */
    fun report(metrics: Collection<Metric>)

    /**
     * 启动报告器。
     */
    fun start()

    /**
     * 停止报告器。
     */
    fun stop()
}

/**
 * 控制台指标报告器。
 *
 * @property registry 指标注册表
 * @property intervalSeconds 报告间隔（秒）
 */
class ConsoleMetricReporter(
    private val registry: MetricRegistry,
    private val intervalSeconds: Long = 60
) : MetricReporter {
    private var executor: ScheduledExecutorService? = null

    override fun report(metrics: Collection<Metric>) {
        println("=== Metrics Report ===")
        println("Timestamp: ${System.currentTimeMillis()}")
        println("Total metrics: ${metrics.size}")
        println()

        // 按类型分组报告
        val metricsByType = metrics.groupBy { it.getType() }

        // 报告计数器
        metricsByType[MetricType.COUNTER]?.let { counters ->
            println("=== Counters (${counters.size}) ===")
            counters.sortedBy { it.getName() }.forEach { metric ->
                val counter = metric as Counter
                val tags = formatTags(metric.getTags())
                println("${metric.getName()}$tags: ${counter.getCount()}")
            }
            println()
        }

        // 报告仪表
        metricsByType[MetricType.GAUGE]?.let { gauges ->
            println("=== Gauges (${gauges.size}) ===")
            gauges.sortedBy { it.getName() }.forEach { metric ->
                val gauge = metric as Gauge
                val tags = formatTags(metric.getTags())
                println("${metric.getName()}$tags: ${gauge.getValue()}")
            }
            println()
        }

        // 报告直方图
        metricsByType[MetricType.HISTOGRAM]?.let { histograms ->
            println("=== Histograms (${histograms.size}) ===")
            histograms.sortedBy { it.getName() }.forEach { metric ->
                val histogram = metric as Histogram
                val summary = histogram.getSummary()
                val tags = formatTags(metric.getTags())
                println("${metric.getName()}$tags:")
                println("  count: ${summary.count}")
                println("  sum: ${summary.sum}")
                println("  min: ${summary.min}")
                println("  max: ${summary.max}")
                println("  mean: ${summary.mean}")
                println("  p50: ${summary.percentiles[0.5]}")
                println("  p75: ${summary.percentiles[0.75]}")
                println("  p90: ${summary.percentiles[0.9]}")
                println("  p95: ${summary.percentiles[0.95]}")
                println("  p99: ${summary.percentiles[0.99]}")
                println("  p999: ${summary.percentiles[0.999]}")
            }
            println()
        }

        // 报告计时器
        metricsByType[MetricType.TIMER]?.let { timers ->
            println("=== Timers (${timers.size}) ===")
            timers.sortedBy { it.getName() }.forEach { metric ->
                val timer = metric as Timer
                val summary = timer.getSummary()
                val tags = formatTags(metric.getTags())
                println("${metric.getName()}$tags:")
                println("  count: ${summary.count}")
                println("  totalTime: ${summary.totalTime} ms")
                println("  min: ${summary.min} ms")
                println("  max: ${summary.max} ms")
                println("  mean: ${summary.mean} ms")
                println("  p50: ${summary.percentiles[0.5]} ms")
                println("  p75: ${summary.percentiles[0.75]} ms")
                println("  p90: ${summary.percentiles[0.9]} ms")
                println("  p95: ${summary.percentiles[0.95]} ms")
                println("  p99: ${summary.percentiles[0.99]} ms")
                println("  p999: ${summary.percentiles[0.999]} ms")
            }
            println()
        }

        println("=== End of Metrics Report ===")
    }

    override fun start() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor { r ->
                val thread = Thread(r, "MetricReporter-Worker")
                thread.isDaemon = true
                thread
            }
            executor?.scheduleAtFixedRate({
                try {
                    report(registry.getMetrics())
                } catch (e: Exception) {
                    System.err.println("Error reporting metrics: ${e.message}")
                    e.printStackTrace()
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        executor?.shutdown()
        try {
            if (executor?.awaitTermination(5, TimeUnit.SECONDS) != true) {
                executor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor?.shutdownNow()
        }
        executor = null
    }

    private fun formatTags(tags: Map<String, String>): String {
        if (tags.isEmpty()) {
            return ""
        }
        return tags.entries.joinToString(", ", " [", "]") { "${it.key}=${it.value}" }
    }
}

/**
 * JSON 指标报告器。
 *
 * @property registry 指标注册表
 * @property directory 报告文件目录
 * @property filePrefix 报告文件前缀
 * @property intervalSeconds 报告间隔（秒）
 */
class JsonMetricReporter(
    private val registry: MetricRegistry,
    private val directory: String,
    private val filePrefix: String = "metrics",
    private val intervalSeconds: Long = 60
) : MetricReporter {
    private var executor: ScheduledExecutorService? = null

    override fun report(metrics: Collection<Metric>) {
        val timestamp = System.currentTimeMillis()
        val reportFile = java.io.File(directory, "${filePrefix}-${timestamp}.json")

        // 创建目录
        reportFile.parentFile?.mkdirs()

        // 写入报告
        reportFile.bufferedWriter().use { writer ->
            writer.write("{\n")
            writer.write("  \"timestamp\": $timestamp,\n")
            writer.write("  \"metrics\": [\n")

            // 写入指标
            metrics.forEachIndexed { index, metric ->
                if (index > 0) {
                    writer.write(",\n")
                }
                writer.write("    ${formatMetricJson(metric)}")
            }

            writer.write("\n  ]\n")
            writer.write("}\n")
        }
    }

    override fun start() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor { r ->
                val thread = Thread(r, "JsonMetricReporter-Worker")
                thread.isDaemon = true
                thread
            }
            executor?.scheduleAtFixedRate({
                try {
                    report(registry.getMetrics())
                } catch (e: Exception) {
                    System.err.println("Error reporting metrics: ${e.message}")
                    e.printStackTrace()
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        executor?.shutdown()
        try {
            if (executor?.awaitTermination(5, TimeUnit.SECONDS) != true) {
                executor?.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor?.shutdownNow()
        }
        executor = null
    }

    private fun formatMetricJson(metric: Metric): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("      \"name\": \"${escapeJson(metric.getName())}\",\n")
        sb.append("      \"type\": \"${metric.getType()}\",\n")
        sb.append("      \"description\": \"${escapeJson(metric.getDescription())}\",\n")

        // 添加标签
        sb.append("      \"tags\": {\n")
        metric.getTags().entries.forEachIndexed { index, (key, value) ->
            if (index > 0) {
                sb.append(",\n")
            }
            sb.append("        \"${escapeJson(key)}\": \"${escapeJson(value)}\"")
        }
        sb.append("\n      },\n")

        // 添加指标值
        sb.append("      \"value\": ")
        when (metric) {
            is Counter -> {
                sb.append("{\n")
                sb.append("        \"count\": ${metric.getCount()}\n")
                sb.append("      }")
            }
            is Gauge -> {
                sb.append("{\n")
                sb.append("        \"value\": ${metric.getValue()}\n")
                sb.append("      }")
            }
            is Histogram -> {
                val summary = metric.getSummary()
                sb.append("{\n")
                sb.append("        \"count\": ${summary.count},\n")
                sb.append("        \"sum\": ${summary.sum},\n")
                sb.append("        \"min\": ${summary.min},\n")
                sb.append("        \"max\": ${summary.max},\n")
                sb.append("        \"mean\": ${summary.mean},\n")
                sb.append("        \"percentiles\": {\n")
                summary.percentiles.entries.forEachIndexed { index, (percentile, value) ->
                    if (index > 0) {
                        sb.append(",\n")
                    }
                    sb.append("          \"p${(percentile * 100).toInt()}\": $value")
                }
                sb.append("\n        }\n")
                sb.append("      }")
            }
            is Timer -> {
                val summary = metric.getSummary()
                sb.append("{\n")
                sb.append("        \"count\": ${summary.count},\n")
                sb.append("        \"totalTime\": ${summary.totalTime},\n")
                sb.append("        \"min\": ${summary.min},\n")
                sb.append("        \"max\": ${summary.max},\n")
                sb.append("        \"mean\": ${summary.mean},\n")
                sb.append("        \"percentiles\": {\n")
                summary.percentiles.entries.forEachIndexed { index, (percentile, value) ->
                    if (index > 0) {
                        sb.append(",\n")
                    }
                    sb.append("          \"p${(percentile * 100).toInt()}\": $value")
                }
                sb.append("\n        }\n")
                sb.append("      }")
            }
            else -> sb.append("null")
        }

        sb.append("\n    }")
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f") // Form feed character
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
