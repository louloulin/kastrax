package ai.kastrax.store.health

import ai.kastrax.store.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.TimeSource

private val logger = KotlinLogging.logger {}

/**
 * 健康检查状态。
 */
enum class HealthStatus {
    /**
     * 健康。
     */
    HEALTHY,

    /**
     * 不健康。
     */
    UNHEALTHY,

    /**
     * 降级。
     */
    DEGRADED,

    /**
     * 未知。
     */
    UNKNOWN
}

/**
 * 健康检查结果。
 *
 * @property status 健康状态
 * @property message 消息
 * @property details 详细信息
 * @property timestamp 时间戳
 * @property duration 耗时
 */
data class HealthCheckResult(
    val status: HealthStatus,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Duration = Duration.ZERO
)

/**
 * 索引健康检查结果。
 *
 * @property indexName 索引名称
 * @property status 健康状态
 * @property message 消息
 * @property details 详细信息
 * @property timestamp 时间戳
 * @property duration 耗时
 */
data class IndexHealthCheckResult(
    val indexName: String,
    val status: HealthStatus,
    val message: String,
    val details: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Duration = Duration.ZERO
)

/**
 * 向量存储健康检查。
 */
object VectorStoreHealthCheck {

    /**
     * 检查向量存储健康状态。
     *
     * @param vectorStore 向量存储
     * @return 健康检查结果
     */
    suspend fun checkHealth(vectorStore: VectorStore): HealthCheckResult = withContext(Dispatchers.IO) {
        val startTime = TimeSource.Monotonic.markNow()
        
        try {
            // 列出索引
            val indexes = vectorStore.listIndexes()
            
            if (indexes.isEmpty()) {
                return@withContext HealthCheckResult(
                    status = HealthStatus.HEALTHY,
                    message = "Vector store is healthy but has no indexes",
                    details = mapOf("indexCount" to 0),
                    duration = startTime.elapsedNow()
                )
            }
            
            // 检查每个索引的健康状态
            val indexResults = checkIndexesHealth(vectorStore, indexes)
            
            // 计算总体健康状态
            val overallStatus = calculateOverallStatus(indexResults)
            
            return@withContext HealthCheckResult(
                status = overallStatus,
                message = getStatusMessage(overallStatus, indexResults),
                details = mapOf(
                    "indexCount" to indexes.size,
                    "indexResults" to indexResults
                ),
                duration = startTime.elapsedNow()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error checking vector store health" }
            
            return@withContext HealthCheckResult(
                status = HealthStatus.UNHEALTHY,
                message = "Error checking vector store health: ${e.message}",
                details = mapOf("error" to e.toString()),
                duration = startTime.elapsedNow()
            )
        }
    }

    /**
     * 检查索引健康状态。
     *
     * @param vectorStore 向量存储
     * @param indexNames 索引名称列表
     * @return 索引健康检查结果列表
     */
    suspend fun checkIndexesHealth(
        vectorStore: VectorStore,
        indexNames: List<String>
    ): List<IndexHealthCheckResult> = coroutineScope {
        // 并行检查每个索引
        indexNames.map { indexName ->
            async {
                checkIndexHealth(vectorStore, indexName)
            }
        }.awaitAll()
    }

    /**
     * 检查索引健康状态。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @return 索引健康检查结果
     */
    suspend fun checkIndexHealth(
        vectorStore: VectorStore,
        indexName: String
    ): IndexHealthCheckResult = withContext(Dispatchers.IO) {
        val startTime = TimeSource.Monotonic.markNow()
        
        try {
            // 获取索引信息
            val stats = vectorStore.describeIndex(indexName)
            
            // 执行简单查询
            val testVector = FloatArray(stats.dimension) { 0f }
            testVector[0] = 1f
            
            val queryStartTime = TimeSource.Monotonic.markNow()
            val queryResults = vectorStore.query(indexName, testVector, 1)
            val queryDuration = queryStartTime.elapsedNow()
            
            // 检查查询性能
            val status = when {
                queryDuration > Duration.parse("1s") -> HealthStatus.DEGRADED
                else -> HealthStatus.HEALTHY
            }
            
            return@withContext IndexHealthCheckResult(
                indexName = indexName,
                status = status,
                message = "Index $indexName is ${status.name.lowercase()}",
                details = mapOf(
                    "dimension" to stats.dimension,
                    "count" to stats.count,
                    "metric" to stats.metric,
                    "queryDuration" to queryDuration.toString(),
                    "queryResultCount" to queryResults.size
                ),
                duration = startTime.elapsedNow()
            )
        } catch (e: Exception) {
            logger.error(e) { "Error checking index $indexName health" }
            
            return@withContext IndexHealthCheckResult(
                indexName = indexName,
                status = HealthStatus.UNHEALTHY,
                message = "Error checking index $indexName health: ${e.message}",
                details = mapOf("error" to e.toString()),
                duration = startTime.elapsedNow()
            )
        }
    }

    /**
     * 计算总体健康状态。
     *
     * @param indexResults 索引健康检查结果列表
     * @return 总体健康状态
     */
    private fun calculateOverallStatus(indexResults: List<IndexHealthCheckResult>): HealthStatus {
        if (indexResults.isEmpty()) {
            return HealthStatus.UNKNOWN
        }
        
        val statusCounts = indexResults.groupBy { it.status }
        
        return when {
            statusCounts[HealthStatus.UNHEALTHY]?.isNotEmpty() == true -> HealthStatus.UNHEALTHY
            statusCounts[HealthStatus.DEGRADED]?.isNotEmpty() == true -> HealthStatus.DEGRADED
            statusCounts[HealthStatus.HEALTHY]?.size == indexResults.size -> HealthStatus.HEALTHY
            else -> HealthStatus.DEGRADED
        }
    }

    /**
     * 获取状态消息。
     *
     * @param status 健康状态
     * @param indexResults 索引健康检查结果列表
     * @return 状态消息
     */
    private fun getStatusMessage(
        status: HealthStatus,
        indexResults: List<IndexHealthCheckResult>
    ): String {
        val statusCounts = indexResults.groupBy { it.status }
        
        return when (status) {
            HealthStatus.HEALTHY -> "All indexes are healthy"
            HealthStatus.DEGRADED -> {
                val degradedCount = statusCounts[HealthStatus.DEGRADED]?.size ?: 0
                val unhealthyCount = statusCounts[HealthStatus.UNHEALTHY]?.size ?: 0
                "Vector store is degraded: $degradedCount degraded, $unhealthyCount unhealthy indexes"
            }
            HealthStatus.UNHEALTHY -> {
                val unhealthyCount = statusCounts[HealthStatus.UNHEALTHY]?.size ?: 0
                "Vector store is unhealthy: $unhealthyCount unhealthy indexes"
            }
            HealthStatus.UNKNOWN -> "Vector store health status is unknown"
        }
    }
}
