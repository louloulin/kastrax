package ai.kastrax.observability.metrics

/**
 * 指标配置类。
 */
class MetricsConfig {
    var enabled: Boolean = true
    var console: ConsoleReporterConfig? = null
    var json: JsonReporterConfig? = null

    /**
     * 控制台报告器配置。
     */
    class ConsoleReporterConfig {
        var enabled: Boolean = false
        var intervalSeconds: Long = 60
    }

    /**
     * JSON 报告器配置。
     */
    class JsonReporterConfig {
        var enabled: Boolean = false
        var directory: String = "metrics"
        var filePrefix: String = "metrics"
        var intervalSeconds: Long = 60
    }

    /**
     * 应用指标配置。
     */
    fun apply() {
        if (!enabled) {
            return
        }

        val registry = DefaultMetricRegistry()
        MetricsSystem.setMetricRegistry(registry)

        // 控制台报告器
        console?.let {
            if (it.enabled) {
                val reporter = ConsoleMetricReporter(
                    registry = registry,
                    intervalSeconds = it.intervalSeconds
                )
                reporter.start()
            }
        }

        // JSON 报告器
        json?.let {
            if (it.enabled) {
                val reporter = JsonMetricReporter(
                    registry = registry,
                    directory = it.directory,
                    filePrefix = it.filePrefix,
                    intervalSeconds = it.intervalSeconds
                )
                reporter.start()
            }
        }
    }
}
