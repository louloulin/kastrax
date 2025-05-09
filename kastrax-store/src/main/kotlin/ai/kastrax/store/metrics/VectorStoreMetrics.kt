package ai.kastrax.store.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

/**
 * 向量存储指标类型。
 */
enum class MetricType {
    /**
     * 查询操作。
     */
    QUERY,

    /**
     * 添加向量操作。
     */
    UPSERT,

    /**
     * 删除向量操作。
     */
    DELETE,

    /**
     * 创建索引操作。
     */
    CREATE_INDEX,

    /**
     * 删除索引操作。
     */
    DELETE_INDEX,

    /**
     * 更新向量操作。
     */
    UPDATE_VECTOR,

    /**
     * 批量添加向量操作。
     */
    BATCH_UPSERT,

    /**
     * 获取索引信息操作。
     */
    DESCRIBE_INDEX,

    /**
     * 列出索引操作。
     */
    LIST_INDEXES
}

/**
 * 向量存储操作指标。
 *
 * @property count 操作次数
 * @property totalDuration 总耗时
 * @property minDuration 最小耗时
 * @property maxDuration 最大耗时
 * @property errorCount 错误次数
 */
data class OperationMetric(
    val count: Long = 0,
    val totalDuration: Duration = Duration.ZERO,
    val minDuration: Duration = Duration.INFINITE,
    val maxDuration: Duration = Duration.ZERO,
    val errorCount: Long = 0
) {
    /**
     * 平均耗时。
     */
    val avgDuration: Duration
        get() = if (count > 0) totalDuration / count else Duration.ZERO
}

/**
 * 向量存储指标收集器。
 */
object VectorStoreMetrics {
    private val metrics = ConcurrentHashMap<String, ConcurrentHashMap<MetricType, MutableMetric>>()
    private val globalMetrics = ConcurrentHashMap<MetricType, MutableMetric>()

    /**
     * 记录操作开始。
     *
     * @param type 操作类型
     * @param indexName 索引名称
     * @return 操作上下文
     */
    fun recordOperationStart(type: MetricType, indexName: String? = null): OperationContext {
        val startTime = TimeSource.Monotonic.markNow()
        return OperationContext(type, indexName, startTime)
    }

    /**
     * 记录操作结束。
     *
     * @param context 操作上下文
     * @param isSuccess 是否成功
     */
    fun recordOperationEnd(context: OperationContext, isSuccess: Boolean) {
        val duration = context.startTime.elapsedNow()
        
        // 更新全局指标
        val globalMetric = globalMetrics.computeIfAbsent(context.type) { MutableMetric() }
        globalMetric.update(duration, isSuccess)
        
        // 更新索引指标
        if (context.indexName != null) {
            val indexMetrics = metrics.computeIfAbsent(context.indexName) { ConcurrentHashMap() }
            val indexMetric = indexMetrics.computeIfAbsent(context.type) { MutableMetric() }
            indexMetric.update(duration, isSuccess)
        }
    }

    /**
     * 获取全局指标。
     *
     * @return 全局指标
     */
    fun getGlobalMetrics(): Map<MetricType, OperationMetric> {
        return globalMetrics.mapValues { it.value.toOperationMetric() }
    }

    /**
     * 获取索引指标。
     *
     * @param indexName 索引名称
     * @return 索引指标
     */
    fun getIndexMetrics(indexName: String): Map<MetricType, OperationMetric> {
        return metrics[indexName]?.mapValues { it.value.toOperationMetric() } ?: emptyMap()
    }

    /**
     * 获取所有索引指标。
     *
     * @return 所有索引指标
     */
    fun getAllIndexMetrics(): Map<String, Map<MetricType, OperationMetric>> {
        return metrics.mapValues { (_, metrics) ->
            metrics.mapValues { it.value.toOperationMetric() }
        }
    }

    /**
     * 重置指标。
     */
    fun reset() {
        metrics.clear()
        globalMetrics.clear()
    }

    /**
     * 可变指标。
     */
    private class MutableMetric {
        private val count = AtomicLong(0)
        private val totalDuration = AtomicLong(0)
        private val minDuration = AtomicLong(Long.MAX_VALUE)
        private val maxDuration = AtomicLong(0)
        private val errorCount = AtomicLong(0)

        /**
         * 更新指标。
         *
         * @param duration 耗时
         * @param isSuccess 是否成功
         */
        fun update(duration: Duration, isSuccess: Boolean) {
            count.incrementAndGet()
            val durationNanos = duration.inWholeNanoseconds
            totalDuration.addAndGet(durationNanos)
            
            // 更新最小耗时
            var currentMin = minDuration.get()
            while (durationNanos < currentMin) {
                if (minDuration.compareAndSet(currentMin, durationNanos)) {
                    break
                }
                currentMin = minDuration.get()
            }
            
            // 更新最大耗时
            var currentMax = maxDuration.get()
            while (durationNanos > currentMax) {
                if (maxDuration.compareAndSet(currentMax, durationNanos)) {
                    break
                }
                currentMax = maxDuration.get()
            }
            
            // 更新错误次数
            if (!isSuccess) {
                errorCount.incrementAndGet()
            }
        }

        /**
         * 转换为操作指标。
         *
         * @return 操作指标
         */
        fun toOperationMetric(): OperationMetric {
            return OperationMetric(
                count = count.get(),
                totalDuration = Duration.nanoseconds(totalDuration.get()),
                minDuration = if (minDuration.get() == Long.MAX_VALUE) Duration.ZERO else Duration.nanoseconds(minDuration.get()),
                maxDuration = Duration.nanoseconds(maxDuration.get()),
                errorCount = errorCount.get()
            )
        }
    }
}

/**
 * 操作上下文。
 *
 * @property type 操作类型
 * @property indexName 索引名称
 * @property startTime 开始时间
 */
data class OperationContext(
    val type: MetricType,
    val indexName: String?,
    val startTime: TimeSource.Monotonic.ValueTimeMark
)
