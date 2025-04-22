package ai.kastrax.observability.metrics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricsTest {
    @Test
    fun testCounter() {
        // 创建指标注册表
        val registry = DefaultMetricRegistry()

        // 创建计数器
        val counter = registry.counter(
            name = "test_counter",
            description = "Test counter",
            tags = mapOf("env" to "test")
        )

        // 增加计数器值
        counter.increment()
        counter.increment(2)

        // 验证计数器值
        assertEquals(3, counter.getCount())

        // 验证指标属性
        assertEquals("test_counter", counter.getName())
        assertEquals("Test counter", counter.getDescription())
        assertEquals(mapOf("env" to "test"), counter.getTags())
        assertEquals(MetricType.COUNTER, counter.getType())
    }

    @Test
    fun testGauge() {
        // 创建指标注册表
        val registry = DefaultMetricRegistry()

        // 创建仪表
        val gauge = registry.gauge(
            name = "test_gauge",
            description = "Test gauge",
            tags = mapOf("env" to "test")
        )

        // 设置仪表值
        gauge.setValue(42.0)

        // 验证仪表值
        assertEquals(42.0, gauge.getValue())

        // 更新仪表值
        gauge.setValue(123.45)

        // 验证更新后的仪表值
        assertEquals(123.45, gauge.getValue())
    }

    @Test
    fun testHistogram() {
        // 创建指标注册表
        val registry = DefaultMetricRegistry()

        // 创建直方图
        val histogram = registry.histogram(
            name = "test_histogram",
            description = "Test histogram",
            tags = mapOf("env" to "test")
        )

        // 记录值
        for (i in 1..100) {
            histogram.record(i.toLong())
        }

        // 获取摘要
        val summary = histogram.getSummary()

        // 验证摘要
        assertEquals(100, summary.count)
        assertEquals(5050, summary.sum)
        assertEquals(1, summary.min)
        assertEquals(100, summary.max)
        assertEquals(50.5, summary.mean)

        // 验证百分位数
        assertTrue(summary.percentiles.containsKey(0.5))
        assertTrue(summary.percentiles.containsKey(0.9))
        assertTrue(summary.percentiles.containsKey(0.99))

        val p50 = summary.percentiles[0.5] ?: 0
        val p90 = summary.percentiles[0.9] ?: 0
        val p99 = summary.percentiles[0.99] ?: 0

        assertTrue(p50 >= 50)
        assertTrue(p90 >= 90)
        assertTrue(p99 >= 99)
    }

    @Test
    fun testTimer() {
        // 创建指标注册表
        val registry = DefaultMetricRegistry()

        // 创建计时器
        val timer = registry.timer(
            name = "test_timer",
            description = "Test timer",
            tags = mapOf("env" to "test")
        )

        // 记录持续时间
        timer.record(100)
        timer.record(200)
        timer.record(300)

        // 使用上下文记录持续时间
        timer.start().use {
            Thread.sleep(10) // 模拟操作
        }

        // 获取摘要
        val summary = timer.getSummary()

        // 验证摘要
        assertEquals(4, summary.count)
        assertTrue(summary.totalTime >= 610)
        assertTrue(summary.min >= 10)
        assertTrue(summary.max >= 300)
        assertTrue(summary.mean > 0)

        // 验证百分位数
        assertTrue(summary.percentiles.containsKey(0.5))
        assertTrue(summary.percentiles.containsKey(0.9))
        assertTrue(summary.percentiles.containsKey(0.99))
    }

    @Test
    fun testMetricsSystem() {
        // 重置指标系统
        MetricsSystem.setMetricRegistry(DefaultMetricRegistry())

        // 创建指标
        val counter = MetricsSystem.counter("system_counter", "System counter")
        val gauge = MetricsSystem.gauge("system_gauge", "System gauge")
        val histogram = MetricsSystem.histogram("system_histogram", "System histogram")
        val timer = MetricsSystem.timer("system_timer", "System timer")

        // 使用指标
        counter.increment(5)
        gauge.setValue(42.0)
        histogram.record(100)
        timer.record(200)

        // 验证指标
        assertEquals(5, counter.getCount())
        assertEquals(42.0, gauge.getValue())
        assertEquals(1, histogram.getSummary().count)
        assertEquals(1, timer.getSummary().count)

        // 验证指标注册表
        val registry = MetricsSystem.getMetricRegistry()
        val metrics = registry.getMetrics()
        assertEquals(4, metrics.size)

        // 验证获取指标
        val retrievedCounter = registry.getMetric("system_counter")
        assertTrue(retrievedCounter is Counter)
        assertEquals(5, (retrievedCounter as Counter).getCount())
    }
}
