package ai.kastrax.a2a.monitoring

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 指标类型
 */
enum class MetricType {
    /**
     * 计数器
     */
    COUNTER,
    
    /**
     * 仪表
     */
    GAUGE,
    
    /**
     * 直方图
     */
    HISTOGRAM,
    
    /**
     * 计时器
     */
    TIMER
}

/**
 * 指标事件
 */
data class MetricEvent(
    /**
     * 指标名称
     */
    val name: String,
    
    /**
     * 指标类型
     */
    val type: MetricType,
    
    /**
     * 指标值
     */
    val value: Double,
    
    /**
     * 标签
     */
    val tags: Map<String, String> = emptyMap(),
    
    /**
     * 时间戳
     */
    val timestamp: Instant = Instant.now()
)

/**
 * 日志级别
 */
enum class LogLevel {
    /**
     * 调试
     */
    DEBUG,
    
    /**
     * 信息
     */
    INFO,
    
    /**
     * 警告
     */
    WARN,
    
    /**
     * 错误
     */
    ERROR
}

/**
 * 日志事件
 */
data class LogEvent(
    /**
     * 日志级别
     */
    val level: LogLevel,
    
    /**
     * 消息
     */
    val message: String,
    
    /**
     * 标签
     */
    val tags: Map<String, String> = emptyMap(),
    
    /**
     * 时间戳
     */
    val timestamp: Instant = Instant.now()
)

/**
 * 跟踪事件
 */
data class TraceEvent(
    /**
     * 跟踪 ID
     */
    val traceId: String,
    
    /**
     * 跨度 ID
     */
    val spanId: String,
    
    /**
     * 父跨度 ID
     */
    val parentSpanId: String? = null,
    
    /**
     * 操作名称
     */
    val operation: String,
    
    /**
     * 开始时间
     */
    val startTime: Instant,
    
    /**
     * 结束时间
     */
    val endTime: Instant? = null,
    
    /**
     * 标签
     */
    val tags: Map<String, String> = emptyMap(),
    
    /**
     * 事件
     */
    val events: List<Pair<Instant, String>> = emptyList()
)

/**
 * 监控事件
 */
sealed class MonitoringEvent {
    /**
     * 指标事件
     */
    data class Metric(val event: MetricEvent) : MonitoringEvent()
    
    /**
     * 日志事件
     */
    data class Log(val event: LogEvent) : MonitoringEvent()
    
    /**
     * 跟踪事件
     */
    data class Trace(val event: TraceEvent) : MonitoringEvent()
}

/**
 * A2A 监控服务，用于收集和报告指标、日志和跟踪
 */
class A2AMonitoringService(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * 计数器映射
     */
    private val counters = ConcurrentHashMap<String, AtomicLong>()
    
    /**
     * 仪表映射
     */
    private val gauges = ConcurrentHashMap<String, AtomicLong>()
    
    /**
     * 直方图映射
     */
    private val histograms = ConcurrentHashMap<String, MutableList<Double>>()
    
    /**
     * 计时器映射
     */
    private val timers = ConcurrentHashMap<String, MutableList<Double>>()
    
    /**
     * 活动跟踪映射
     */
    private val activeTraces = ConcurrentHashMap<String, TraceEvent>()
    
    /**
     * 事件流
     */
    private val _events = MutableSharedFlow<MonitoringEvent>(replay = 0)
    
    /**
     * 事件流（只读）
     */
    val events: SharedFlow<MonitoringEvent> = _events.asSharedFlow()
    
    /**
     * 增加计数器
     */
    suspend fun incrementCounter(
        name: String,
        value: Long = 1,
        tags: Map<String, String> = emptyMap()
    ) {
        val counter = counters.computeIfAbsent(name) { AtomicLong(0) }
        val newValue = counter.addAndGet(value)
        
        _events.emit(
            MonitoringEvent.Metric(
                MetricEvent(
                    name = name,
                    type = MetricType.COUNTER,
                    value = newValue.toDouble(),
                    tags = tags
                )
            )
        )
    }
    
    /**
     * 设置仪表值
     */
    suspend fun setGauge(
        name: String,
        value: Double,
        tags: Map<String, String> = emptyMap()
    ) {
        val gauge = gauges.computeIfAbsent(name) { AtomicLong(0) }
        gauge.set(value.toLong())
        
        _events.emit(
            MonitoringEvent.Metric(
                MetricEvent(
                    name = name,
                    type = MetricType.GAUGE,
                    value = value,
                    tags = tags
                )
            )
        )
    }
    
    /**
     * 记录直方图值
     */
    suspend fun recordHistogram(
        name: String,
        value: Double,
        tags: Map<String, String> = emptyMap()
    ) {
        val histogram = histograms.computeIfAbsent(name) { mutableListOf() }
        synchronized(histogram) {
            histogram.add(value)
        }
        
        _events.emit(
            MonitoringEvent.Metric(
                MetricEvent(
                    name = name,
                    type = MetricType.HISTOGRAM,
                    value = value,
                    tags = tags
                )
            )
        )
    }
    
    /**
     * 记录计时器值
     */
    suspend fun recordTimer(
        name: String,
        durationMs: Double,
        tags: Map<String, String> = emptyMap()
    ) {
        val timer = timers.computeIfAbsent(name) { mutableListOf() }
        synchronized(timer) {
            timer.add(durationMs)
        }
        
        _events.emit(
            MonitoringEvent.Metric(
                MetricEvent(
                    name = name,
                    type = MetricType.TIMER,
                    value = durationMs,
                    tags = tags
                )
            )
        )
    }
    
    /**
     * 记录日志
     */
    suspend fun log(
        level: LogLevel,
        message: String,
        tags: Map<String, String> = emptyMap()
    ) {
        _events.emit(
            MonitoringEvent.Log(
                LogEvent(
                    level = level,
                    message = message,
                    tags = tags
                )
            )
        )
    }
    
    /**
     * 开始跟踪
     */
    suspend fun startTrace(
        traceId: String,
        spanId: String,
        parentSpanId: String? = null,
        operation: String,
        tags: Map<String, String> = emptyMap()
    ) {
        val trace = TraceEvent(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            operation = operation,
            startTime = Instant.now(),
            tags = tags
        )
        
        activeTraces[spanId] = trace
        
        _events.emit(MonitoringEvent.Trace(trace))
    }
    
    /**
     * 结束跟踪
     */
    suspend fun endTrace(
        spanId: String,
        tags: Map<String, String> = emptyMap()
    ) {
        val trace = activeTraces.remove(spanId) ?: return
        
        val updatedTrace = trace.copy(
            endTime = Instant.now(),
            tags = trace.tags + tags
        )
        
        _events.emit(MonitoringEvent.Trace(updatedTrace))
    }
    
    /**
     * 添加跟踪事件
     */
    suspend fun addTraceEvent(
        spanId: String,
        event: String
    ) {
        val trace = activeTraces[spanId] ?: return
        
        val events = trace.events + (Instant.now() to event)
        val updatedTrace = trace.copy(events = events)
        
        activeTraces[spanId] = updatedTrace
        
        _events.emit(MonitoringEvent.Trace(updatedTrace))
    }
    
    /**
     * 获取计数器值
     */
    fun getCounter(name: String): Long {
        return counters[name]?.get() ?: 0
    }
    
    /**
     * 获取仪表值
     */
    fun getGauge(name: String): Double {
        return gauges[name]?.get()?.toDouble() ?: 0.0
    }
    
    /**
     * 获取直方图统计信息
     */
    fun getHistogramStats(name: String): HistogramStats {
        val histogram = histograms[name] ?: return HistogramStats(0.0, 0.0, 0.0, 0.0, 0.0, 0)
        
        synchronized(histogram) {
            if (histogram.isEmpty()) {
                return HistogramStats(0.0, 0.0, 0.0, 0.0, 0.0, 0)
            }
            
            val sorted = histogram.sorted()
            val count = sorted.size
            val min = sorted.first()
            val max = sorted.last()
            val mean = sorted.average()
            val median = if (count % 2 == 0) {
                (sorted[count / 2 - 1] + sorted[count / 2]) / 2
            } else {
                sorted[count / 2]
            }
            val p95 = sorted[(count * 0.95).toInt().coerceAtMost(count - 1)]
            
            return HistogramStats(min, max, mean, median, p95, count)
        }
    }
    
    /**
     * 获取计时器统计信息
     */
    fun getTimerStats(name: String): HistogramStats {
        return getHistogramStats(name)
    }
    
    /**
     * 获取活动跟踪
     */
    fun getActiveTrace(spanId: String): TraceEvent? {
        return activeTraces[spanId]
    }
    
    /**
     * 获取所有活动跟踪
     */
    fun getAllActiveTraces(): List<TraceEvent> {
        return activeTraces.values.toList()
    }
    
    /**
     * 重置所有指标
     */
    fun resetMetrics() {
        counters.clear()
        gauges.clear()
        histograms.clear()
        timers.clear()
    }
}

/**
 * 直方图统计信息
 */
data class HistogramStats(
    /**
     * 最小值
     */
    val min: Double,
    
    /**
     * 最大值
     */
    val max: Double,
    
    /**
     * 平均值
     */
    val mean: Double,
    
    /**
     * 中位数
     */
    val median: Double,
    
    /**
     * 95 百分位
     */
    val p95: Double,
    
    /**
     * 计数
     */
    val count: Int
)

/**
 * 监控 DSL 构建器
 */
class MonitoringBuilder {
    /**
     * 监控服务
     */
    private val service = A2AMonitoringService()
    
    /**
     * 指标处理器
     */
    private val metricHandlers = mutableListOf<suspend (MetricEvent) -> Unit>()
    
    /**
     * 日志处理器
     */
    private val logHandlers = mutableListOf<suspend (LogEvent) -> Unit>()
    
    /**
     * 跟踪处理器
     */
    private val traceHandlers = mutableListOf<suspend (TraceEvent) -> Unit>()
    
    /**
     * 添加指标处理器
     */
    fun onMetric(handler: suspend (MetricEvent) -> Unit) {
        metricHandlers.add(handler)
    }
    
    /**
     * 添加日志处理器
     */
    fun onLog(handler: suspend (LogEvent) -> Unit) {
        logHandlers.add(handler)
    }
    
    /**
     * 添加跟踪处理器
     */
    fun onTrace(handler: suspend (TraceEvent) -> Unit) {
        traceHandlers.add(handler)
    }
    
    /**
     * 构建监控服务
     */
    fun build(): A2AMonitoringService {
        // 启动事件处理
        CoroutineScope(Dispatchers.Default).launch {
            service.events.collect { event ->
                when (event) {
                    is MonitoringEvent.Metric -> {
                        metricHandlers.forEach { handler ->
                            try {
                                handler(event.event)
                            } catch (e: Exception) {
                                // 忽略处理器异常
                            }
                        }
                    }
                    is MonitoringEvent.Log -> {
                        logHandlers.forEach { handler ->
                            try {
                                handler(event.event)
                            } catch (e: Exception) {
                                // 忽略处理器异常
                            }
                        }
                    }
                    is MonitoringEvent.Trace -> {
                        traceHandlers.forEach { handler ->
                            try {
                                handler(event.event)
                            } catch (e: Exception) {
                                // 忽略处理器异常
                            }
                        }
                    }
                }
            }
        }
        
        return service
    }
}

/**
 * 创建监控服务的 DSL 函数
 */
fun monitoring(init: MonitoringBuilder.() -> Unit): A2AMonitoringService {
    val builder = MonitoringBuilder()
    builder.init()
    return builder.build()
}
