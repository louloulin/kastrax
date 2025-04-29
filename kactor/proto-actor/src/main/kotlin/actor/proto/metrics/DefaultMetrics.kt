package actor.proto.metrics

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.ConcurrentHashMap

/**
 * 默认计数器实现
 */
class DefaultCounter(override val name: String, override val tags: Map<String, String>) : Counter {
    private val count = AtomicLong(0)
    
    override fun inc(value: Long) {
        count.addAndGet(value)
    }
    
    /**
     * 获取当前计数值
     */
    fun value(): Long = count.get()
}

/**
 * 默认仪表实现
 */
class DefaultGauge(override val name: String, override val tags: Map<String, String>) : Gauge {
    private val value = AtomicLong(0)
    
    override fun set(value: Double) {
        this.value.set(value.toLong())
    }
    
    /**
     * 获取当前仪表值
     */
    fun value(): Double = value.get().toDouble()
}

/**
 * 默认直方图实现
 */
class DefaultHistogram(override val name: String, override val tags: Map<String, String>) : Histogram {
    private val observations = ConcurrentHashMap<Double, AtomicLong>()
    private val count = AtomicLong(0)
    private val sum = DoubleAdder()
    
    override fun observe(value: Double) {
        count.incrementAndGet()
        sum.add(value)
        observations.computeIfAbsent(value) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * 获取观察值的数量
     */
    fun count(): Long = count.get()
    
    /**
     * 获取观察值的总和
     */
    fun sum(): Double = sum.sum()
    
    /**
     * 获取所有观察值及其出现次数
     */
    fun values(): Map<Double, Long> = observations.mapValues { it.value.get() }
}
