package ai.kastrax.observability.metrics

import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAdder

/**
 * 默认计数器实现。
 *
 * @property name 指标名称
 * @property description 指标描述
 * @property tags 指标标签
 */
class DefaultCounter(
    private val name: String,
    private val description: String,
    private val tags: Map<String, String>
) : Counter {
    private val count = LongAdder()

    override fun getName(): String {
        return name
    }

    override fun getType(): MetricType {
        return MetricType.COUNTER
    }

    override fun getTags(): Map<String, String> {
        return tags
    }

    override fun getDescription(): String {
        return description
    }

    override fun increment(amount: Long) {
        if (amount <= 0) {
            throw IllegalArgumentException("Counter increment amount must be positive")
        }
        count.add(amount)
    }

    override fun getCount(): Long {
        return count.sum()
    }
}

/**
 * 默认仪表实现。
 *
 * @property name 指标名称
 * @property description 指标描述
 * @property tags 指标标签
 */
class DefaultGauge(
    private val name: String,
    private val description: String,
    private val tags: Map<String, String>
) : Gauge {
    private val value = DoubleAdder()

    override fun getName(): String {
        return name
    }

    override fun getType(): MetricType {
        return MetricType.GAUGE
    }

    override fun getTags(): Map<String, String> {
        return tags
    }

    override fun getDescription(): String {
        return description
    }

    override fun setValue(value: Double) {
        this.value.reset()
        this.value.add(value)
    }

    override fun getValue(): Double {
        return value.sum()
    }
}

/**
 * 默认直方图实现。
 *
 * @property name 指标名称
 * @property description 指标描述
 * @property tags 指标标签
 */
class DefaultHistogram(
    private val name: String,
    private val description: String,
    private val tags: Map<String, String>
) : Histogram {
    private val count = LongAdder()
    private val sum = LongAdder()
    private val min = AtomicLong(Long.MAX_VALUE)
    private val max = AtomicLong(Long.MIN_VALUE)
    private val values = ConcurrentSkipListMap<Long, AtomicLong>()
    private val percentiles = doubleArrayOf(0.5, 0.75, 0.9, 0.95, 0.99, 0.999)

    override fun getName(): String {
        return name
    }

    override fun getType(): MetricType {
        return MetricType.HISTOGRAM
    }

    override fun getTags(): Map<String, String> {
        return tags
    }

    override fun getDescription(): String {
        return description
    }

    override fun record(value: Long) {
        count.increment()
        sum.add(value)
        
        // 更新最小值
        var currentMin = min.get()
        while (value < currentMin) {
            if (min.compareAndSet(currentMin, value)) {
                break
            }
            currentMin = min.get()
        }
        
        // 更新最大值
        var currentMax = max.get()
        while (value > currentMax) {
            if (max.compareAndSet(currentMax, value)) {
                break
            }
            currentMax = max.get()
        }
        
        // 更新值分布
        values.computeIfAbsent(value) { AtomicLong(0) }.incrementAndGet()
    }

    override fun getSummary(): HistogramSummary {
        val currentCount = count.sum()
        if (currentCount == 0L) {
            return HistogramSummary(
                count = 0,
                sum = 0,
                min = 0,
                max = 0,
                mean = 0.0,
                percentiles = percentiles.associateWith { 0L }
            )
        }

        val currentSum = sum.sum()
        val currentMin = min.get().takeIf { it != Long.MAX_VALUE } ?: 0L
        val currentMax = max.get().takeIf { it != Long.MIN_VALUE } ?: 0L
        val currentMean = if (currentCount > 0) currentSum.toDouble() / currentCount else 0.0

        // 计算百分位数
        val percentileValues = mutableMapOf<Double, Long>()
        if (currentCount > 0) {
            val snapshot = values.entries.associate { it.key to it.value.get() }
            var total = 0L
            var prevValue = 0L

            for ((value, count) in snapshot.entries.sortedBy { it.key }) {
                val prevTotal = total
                total += count

                for (p in percentiles) {
                    val threshold = (p * currentCount).toLong()
                    if (prevTotal < threshold && total >= threshold) {
                        percentileValues[p] = value
                    }
                }

                prevValue = value
            }

            // 确保所有百分位数都有值
            for (p in percentiles) {
                if (p !in percentileValues) {
                    percentileValues[p] = prevValue
                }
            }
        } else {
            for (p in percentiles) {
                percentileValues[p] = 0L
            }
        }

        return HistogramSummary(
            count = currentCount,
            sum = currentSum,
            min = currentMin,
            max = currentMax,
            mean = currentMean,
            percentiles = percentileValues
        )
    }
}

/**
 * 默认计时器实现。
 *
 * @property name 指标名称
 * @property description 指标描述
 * @property tags 指标标签
 */
class DefaultTimer(
    private val name: String,
    private val description: String,
    private val tags: Map<String, String>
) : Timer {
    private val histogram = DefaultHistogram(name, description, tags)

    override fun getName(): String {
        return name
    }

    override fun getType(): MetricType {
        return MetricType.TIMER
    }

    override fun getTags(): Map<String, String> {
        return tags
    }

    override fun getDescription(): String {
        return description
    }

    override fun record(durationMs: Long) {
        histogram.record(durationMs)
    }

    override fun start(): TimerContext {
        val startTime = System.currentTimeMillis()
        return object : TimerContext {
            private var stopped = false

            override fun stop(): Long {
                if (stopped) {
                    throw IllegalStateException("Timer already stopped")
                }
                stopped = true
                val duration = System.currentTimeMillis() - startTime
                record(duration)
                return duration
            }

            override fun close() {
                if (!stopped) {
                    stop()
                }
            }
        }
    }

    override fun getSummary(): TimerSummary {
        val histogramSummary = histogram.getSummary()
        return TimerSummary(
            count = histogramSummary.count,
            totalTime = histogramSummary.sum,
            min = histogramSummary.min,
            max = histogramSummary.max,
            mean = histogramSummary.mean,
            percentiles = histogramSummary.percentiles
        )
    }
}
