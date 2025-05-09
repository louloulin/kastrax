package ai.kastrax.rag.benchmark

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.retrieval.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

/**
 * RAG 基准测试结果。
 *
 * @property query 查询
 * @property retrievalTime 检索时间（毫秒）
 * @property contextGenerationTime 上下文生成时间（毫秒）
 * @property totalTime 总时间（毫秒）
 * @property resultCount 结果数量
 * @property contextLength 上下文长度
 */
@Serializable
data class RagBenchmarkResult(
    val query: String,
    val retrievalTime: Long,
    val contextGenerationTime: Long,
    val totalTime: Long,
    val resultCount: Int,
    val contextLength: Int
)

/**
 * RAG 基准测试汇总结果。
 *
 * @property averageRetrievalTime 平均检索时间（毫秒）
 * @property averageContextGenerationTime 平均上下文生成时间（毫秒）
 * @property averageTotalTime 平均总时间（毫秒）
 * @property averageResultCount 平均结果数量
 * @property averageContextLength 平均上下文长度
 * @property queryCount 查询数量
 * @property results 详细结果列表
 */
@Serializable
data class RagBenchmarkSummary(
    val averageRetrievalTime: Double,
    val averageContextGenerationTime: Double,
    val averageTotalTime: Double,
    val averageResultCount: Double,
    val averageContextLength: Double,
    val queryCount: Int,
    val results: List<RagBenchmarkResult>
)

/**
 * RAG 基准测试工具，用于测试 RAG 系统的性能。
 *
 * @property rag RAG 系统
 */
class RagBenchmarkTool(
    private val rag: RAG
) {
    /**
     * 运行基准测试。
     *
     * @param queries 查询列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @param parallel 是否并行执行
     * @return 基准测试汇总结果
     */
    suspend fun runBenchmark(
        queries: List<String>,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null,
        parallel: Boolean = false
    ): RagBenchmarkSummary = withContext(Dispatchers.IO) {
        logger.info { "Running benchmark with ${queries.size} queries" }

        val results = if (parallel) {
            runParallelBenchmark(queries, limit, minScore, options)
        } else {
            runSequentialBenchmark(queries, limit, minScore, options)
        }

        // 计算平均值
        val averageRetrievalTime = results.map { it.retrievalTime }.average()
        val averageContextGenerationTime = results.map { it.contextGenerationTime }.average()
        val averageTotalTime = results.map { it.totalTime }.average()
        val averageResultCount = results.map { it.resultCount }.average()
        val averageContextLength = results.map { it.contextLength }.average()

        logger.info { "Benchmark completed. Average total time: ${averageTotalTime}ms" }

        RagBenchmarkSummary(
            averageRetrievalTime = averageRetrievalTime,
            averageContextGenerationTime = averageContextGenerationTime,
            averageTotalTime = averageTotalTime,
            averageResultCount = averageResultCount,
            averageContextLength = averageContextLength,
            queryCount = queries.size,
            results = results
        )
    }

    /**
     * 运行顺序基准测试。
     *
     * @param queries 查询列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @return 基准测试结果列表
     */
    private suspend fun runSequentialBenchmark(
        queries: List<String>,
        limit: Int,
        minScore: Double,
        options: RagProcessOptions?
    ): List<RagBenchmarkResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RagBenchmarkResult>()

        for ((index, query) in queries.withIndex()) {
            logger.debug { "Running benchmark for query ${index + 1}/${queries.size}: $query" }

            var retrievalTime = 0L
            var contextGenerationTime = 0L
            var totalTime = 0L
            var resultCount = 0
            var contextLength = 0

            totalTime = measureTimeMillis {
                // 测量检索时间
                val searchResults = mutableListOf<ai.kastrax.store.document.DocumentSearchResult>()
                retrievalTime = measureTimeMillis {
                    searchResults.addAll(rag.search(query, limit, minScore, options))
                }
                resultCount = searchResults.size

                // 测量上下文生成时间
                var context = ""
                contextGenerationTime = measureTimeMillis {
                    context = rag.generateContext(query, limit, minScore, options)
                }
                contextLength = context.length
            }

            results.add(
                RagBenchmarkResult(
                    query = query,
                    retrievalTime = retrievalTime,
                    contextGenerationTime = contextGenerationTime,
                    totalTime = totalTime,
                    resultCount = resultCount,
                    contextLength = contextLength
                )
            )
        }

        return@withContext results
    }

    /**
     * 运行并行基准测试。
     *
     * @param queries 查询列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项
     * @return 基准测试结果列表
     */
    private suspend fun runParallelBenchmark(
        queries: List<String>,
        limit: Int,
        minScore: Double,
        options: RagProcessOptions?
    ): List<RagBenchmarkResult> = coroutineScope {
        queries.mapIndexed { index, query ->
            async {
                logger.debug { "Running benchmark for query ${index + 1}/${queries.size}: $query" }

                var retrievalTime = 0L
                var contextGenerationTime = 0L
                var totalTime = 0L
                var resultCount = 0
                var contextLength = 0

                totalTime = measureTimeMillis {
                    // 测量检索时间
                    val searchResults = mutableListOf<ai.kastrax.store.document.DocumentSearchResult>()
                    retrievalTime = measureTimeMillis {
                        searchResults.addAll(rag.search(query, limit, minScore, options))
                    }
                    resultCount = searchResults.size

                    // 测量上下文生成时间
                    var context = ""
                    contextGenerationTime = measureTimeMillis {
                        context = rag.generateContext(query, limit, minScore, options)
                    }
                    contextLength = context.length
                }

                RagBenchmarkResult(
                    query = query,
                    retrievalTime = retrievalTime,
                    contextGenerationTime = contextGenerationTime,
                    totalTime = totalTime,
                    resultCount = resultCount,
                    contextLength = contextLength
                )
            }
        }.awaitAll()
    }

    /**
     * 比较不同配置的性能。
     *
     * @param queries 查询列表
     * @param configurations 配置列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 配置比较结果
     */
    suspend fun compareConfigurations(
        queries: List<String>,
        configurations: List<RagProcessOptions>,
        limit: Int = 5,
        minScore: Double = 0.0
    ): Map<RagProcessOptions, RagBenchmarkSummary> = withContext(Dispatchers.IO) {
        logger.info { "Comparing ${configurations.size} configurations with ${queries.size} queries" }

        val results = mutableMapOf<RagProcessOptions, RagBenchmarkSummary>()

        for ((index, config) in configurations.withIndex()) {
            logger.info { "Testing configuration ${index + 1}/${configurations.size}" }
            val summary = runBenchmark(queries, limit, minScore, config)
            results[config] = summary
        }

        logger.info { "Configuration comparison completed" }
        return@withContext results
    }
}
