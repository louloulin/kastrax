package ai.kastrax.a2a.monitoring

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A2A 监控测试
 */
class A2AMonitoringTest {

    @Test
    fun `test metrics collection`() = runBlocking {
        // 创建监控服务
        val monitoringService = monitoring {
            onMetric { event ->
                // 验证指标事件
                when (event.name) {
                    "test_counter" -> {
                        assertEquals(MetricType.COUNTER, event.type)
                        assertEquals(1.0, event.value)
                    }
                    "test_gauge" -> {
                        assertEquals(MetricType.GAUGE, event.type)
                        assertEquals(42.0, event.value)
                    }
                    "test_histogram" -> {
                        assertEquals(MetricType.HISTOGRAM, event.type)
                        assertEquals(100.0, event.value)
                    }
                    "test_timer" -> {
                        assertEquals(MetricType.TIMER, event.type)
                        assertTrue(event.value > 0.0)
                    }
                }
            }
        }

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        // 记录指标
        monitoringService.incrementCounter("test_counter")
        monitoringService.setGauge("test_gauge", 42.0)
        monitoringService.recordHistogram("test_histogram", 100.0)
        monitoringService.recordTimer("test_timer", 123.45)

        // 等待事件收集
        job.cancel()

        // 验证事件
        assertEquals(4, events.size)
        assertTrue(events.all { it is MonitoringEvent.Metric })

        // 验证指标值
        assertEquals(1, monitoringService.getCounter("test_counter"))
        assertEquals(42.0, monitoringService.getGauge("test_gauge"))

        // 验证直方图统计信息
        val histogramStats = monitoringService.getHistogramStats("test_histogram")
        assertEquals(100.0, histogramStats.min)
        assertEquals(100.0, histogramStats.max)
        assertEquals(100.0, histogramStats.mean)
        assertEquals(100.0, histogramStats.median)
        assertEquals(100.0, histogramStats.p95)
        assertEquals(1, histogramStats.count)

        // 验证计时器统计信息
        val timerStats = monitoringService.getTimerStats("test_timer")
        assertEquals(123.45, timerStats.min)
        assertEquals(123.45, timerStats.max)
        assertEquals(123.45, timerStats.mean)
        assertEquals(123.45, timerStats.median)
        assertEquals(123.45, timerStats.p95)
        assertEquals(1, timerStats.count)
    }

    @Test
    fun `test logging`() = runBlocking {
        // 创建监控服务
        val monitoringService = monitoring {
            onLog { event ->
                // 验证日志事件
                when (event.level) {
                    LogLevel.INFO -> {
                        assertEquals("Info message", event.message)
                    }
                    LogLevel.ERROR -> {
                        assertEquals("Error message", event.message)
                    }
                    LogLevel.DEBUG, LogLevel.WARN -> {
                        // 忽略其他日志级别
                    }
                }
            }
        }

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        // 记录日志
        monitoringService.log(LogLevel.INFO, "Info message")
        monitoringService.log(LogLevel.ERROR, "Error message")

        // 等待事件收集
        job.cancel()

        // 验证事件
        assertEquals(2, events.size)
        assertTrue(events.all { it is MonitoringEvent.Log })
    }

    @Test
    fun `test tracing`() = runBlocking {
        // 创建监控服务
        val monitoringService = monitoring {
            onTrace { event ->
                // 验证跟踪事件
                assertEquals("test-trace", event.traceId)
                assertEquals("test-span", event.spanId)
                assertEquals("test-operation", event.operation)
            }
        }

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        // 开始跟踪
        monitoringService.startTrace(
            traceId = "test-trace",
            spanId = "test-span",
            operation = "test-operation"
        )

        // 添加跟踪事件
        monitoringService.addTraceEvent("test-span", "Event 1")
        monitoringService.addTraceEvent("test-span", "Event 2")

        // 结束跟踪
        monitoringService.endTrace("test-span")

        // 等待事件收集
        job.cancel()

        // 验证事件
        assertTrue(events.size >= 2) // 至少有开始和结束事件
        assertTrue(events.all { it is MonitoringEvent.Trace })

        // 验证活动跟踪
        assertEquals(0, monitoringService.getAllActiveTraces().size)
    }

    @Test
    fun `test histogram statistics`() = runBlocking {
        // 创建监控服务
        val monitoringService = A2AMonitoringService()

        // 记录直方图值
        monitoringService.recordHistogram("test_histogram", 10.0)
        monitoringService.recordHistogram("test_histogram", 20.0)
        monitoringService.recordHistogram("test_histogram", 30.0)
        monitoringService.recordHistogram("test_histogram", 40.0)
        monitoringService.recordHistogram("test_histogram", 50.0)

        // 验证直方图统计信息
        val stats = monitoringService.getHistogramStats("test_histogram")
        assertEquals(10.0, stats.min)
        assertEquals(50.0, stats.max)
        assertEquals(30.0, stats.mean)
        assertEquals(30.0, stats.median)
        assertEquals(50.0, stats.p95)
        assertEquals(5, stats.count)
    }
}
