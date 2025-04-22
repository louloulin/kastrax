package ai.kastrax.observability

import ai.kastrax.observability.logging.LoggingConfig
import ai.kastrax.observability.metrics.MetricsConfig
import ai.kastrax.observability.tracing.TracingConfig
import ai.kastrax.observability.tracing.TracingSystem

/**
 * 可观测性配置类。
 */
class ObservabilityConfig {
    var logging: LoggingConfig = LoggingConfig()
    var metrics: MetricsConfig = MetricsConfig()
    var tracing: TracingConfig = TracingConfig()

    /**
     * 应用可观测性配置。
     */
    fun apply() {
        // 应用日志配置
        logging.apply()

        // 应用指标配置
        metrics.apply()

        // 应用跟踪配置
        val tracer = tracing.apply()
        TracingSystem.setTracer(tracer)
    }
}

/**
 * 可观测性系统。
 */
object ObservabilitySystem {
    private var config: ObservabilityConfig = ObservabilityConfig()

    /**
     * 初始化可观测性系统。
     *
     * @param config 可观测性配置
     */
    fun init(config: ObservabilityConfig) {
        this.config = config
        config.apply()
    }

    /**
     * 获取可观测性配置。
     *
     * @return 可观测性配置
     */
    fun getConfig(): ObservabilityConfig {
        return config
    }
}
