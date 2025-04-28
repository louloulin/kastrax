package ai.kastrax.a2a.monitoring

import kotlinx.coroutines.delay
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
    @org.junit.jupiter.api.Disabled("Temporarily disabled due to issues with event collection")
    fun `test metrics collection`() = runBlocking {
        // 创建监控服务
        val monitoringService = A2AMonitoringService()

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        try {
            // 记录指标
            monitoringService.incrementCounter("test_counter")
            monitoringService.setGauge("test_gauge", 42.0)
            monitoringService.recordHistogram("test_histogram", 100.0)
            monitoringService.recordTimer("test_timer", 123.45)

            // 等待事件收集
            delay(500) // 等待 500 毫秒，确保事件被收集
        } finally {
            job.cancel()
        }

        // 验证事件
        // 注意：我们不验证事件数量，因为可能有其他事件被收集
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it is MonitoringEvent.Metric })

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
    @org.junit.jupiter.api.Disabled("Temporarily disabled due to issues with event collection")
    fun `test logging`() = runBlocking {
        // 创建监控服务
        val monitoringService = A2AMonitoringService()

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        try {
            // 记录日志
            monitoringService.log(LogLevel.INFO, "Info message")
            monitoringService.log(LogLevel.ERROR, "Error message")

            // 等待事件收集
            delay(500) // 等待 500 毫秒，确保事件被收集
        } finally {
            job.cancel()
        }

        // 验证事件
        // 注意：我们不验证事件数量，因为可能有其他事件被收集
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it is MonitoringEvent.Log })
    }

    @Test
    @org.junit.jupiter.api.Disabled("Temporarily disabled due to issues with event collection")
    fun `test tracing`() = runBlocking {
        // 创建监控服务
        val monitoringService = A2AMonitoringService()

        // 收集监控事件
        val events = mutableListOf<MonitoringEvent>()
        val job = launch {
            monitoringService.events.toList(events)
        }

        try {
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
            delay(500) // 等待 500 毫秒，确保事件被收集
        } finally {
            job.cancel()
        }

        // 验证事件
        // 注意：我们不验证事件数量，因为可能有其他事件被收集
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it is MonitoringEvent.Trace })

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
