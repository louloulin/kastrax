package ai.kastrax.store.fusion

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import ai.kastrax.store.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 查询融合策略。
 */
enum class FusionStrategy {
    /**
     * 递归融合策略，将多个查询结果合并，并使用合并后的结果进行新的查询。
     */
    RECURSIVE,

    /**
     * 加权融合策略，将多个查询结果按权重合并。
     */
    WEIGHTED,

    /**
     * 最大融合策略，取多个查询结果中分数最高的结果。
     */
    MAX_SCORE,

    /**
     * 平均融合策略，取多个查询结果中分数的平均值。
     */
    AVERAGE
}

/**
 * 查询融合选项。
 *
 * @property strategy 融合策略
 * @property weights 查询权重
 * @property recursiveDepth 递归深度
 * @property minScore 最小分数
 */
data class FusionOptions(
    val strategy: FusionStrategy = FusionStrategy.WEIGHTED,
    val weights: List<Double>? = null,
    val recursiveDepth: Int = 1,
    val minScore: Double = 0.0
)

/**
 * 查询融合工具类，用于融合多个查询结果。
 */
object QueryFusion {

    /**
     * 融合多个查询。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queries 查询列表
     * @param embeddingService 嵌入服务
     * @param topK 返回结果的最大数量
     * @param options 融合选项
     * @return 融合后的查询结果
     */
    suspend fun fuseQueries(
        vectorStore: VectorStore,
        indexName: String,
        queries: List<String>,
        embeddingService: EmbeddingService,
        topK: Int = 5,
        options: FusionOptions = FusionOptions()
    ): List<SearchResult> = coroutineScope {
        if (queries.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // 计算查询嵌入
        val queryEmbeddings = queries.map { query ->
            async { embeddingService.embed(query) }
        }.awaitAll()

        // 融合查询嵌入
        fuseQueryEmbeddings(
            vectorStore = vectorStore,
            indexName = indexName,
            queryEmbeddings = queryEmbeddings,
            topK = topK,
            options = options
        )
    }

    /**
     * 融合多个查询嵌入。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queryEmbeddings 查询嵌入列表
     * @param topK 返回结果的最大数量
     * @param options 融合选项
     * @return 融合后的查询结果
     */
    suspend fun fuseQueryEmbeddings(
        vectorStore: VectorStore,
        indexName: String,
        queryEmbeddings: List<FloatArray>,
        topK: Int = 5,
        options: FusionOptions = FusionOptions()
    ): List<SearchResult> = coroutineScope {
        if (queryEmbeddings.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // 根据策略选择融合方法
        when (options.strategy) {
            FusionStrategy.RECURSIVE -> recursiveFusion(
                vectorStore = vectorStore,
                indexName = indexName,
                queryEmbeddings = queryEmbeddings,
                topK = topK,
                depth = options.recursiveDepth,
                minScore = options.minScore
            )
            FusionStrategy.WEIGHTED -> weightedFusion(
                vectorStore = vectorStore,
                indexName = indexName,
                queryEmbeddings = queryEmbeddings,
                topK = topK,
                weights = options.weights,
                minScore = options.minScore
            )
            FusionStrategy.MAX_SCORE -> maxScoreFusion(
                vectorStore = vectorStore,
                indexName = indexName,
                queryEmbeddings = queryEmbeddings,
                topK = topK,
                minScore = options.minScore
            )
            FusionStrategy.AVERAGE -> averageFusion(
                vectorStore = vectorStore,
                indexName = indexName,
                queryEmbeddings = queryEmbeddings,
                topK = topK,
                minScore = options.minScore
            )
        }
    }

    /**
     * 递归融合策略。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queryEmbeddings 查询嵌入列表
     * @param topK 返回结果的最大数量
     * @param depth 递归深度
     * @param minScore 最小分数
     * @return 融合后的查询结果
     */
    private suspend fun recursiveFusion(
        vectorStore: VectorStore,
        indexName: String,
        queryEmbeddings: List<FloatArray>,
        topK: Int,
        depth: Int,
        minScore: Double
    ): List<SearchResult> = coroutineScope {
        if (depth <= 0 || queryEmbeddings.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // 第一次查询
        val initialResults = queryEmbeddings.map { queryEmbedding ->
            async { vectorStore.query(indexName, queryEmbedding, topK) }
        }.awaitAll().flatten()

        if (depth == 1 || initialResults.isEmpty()) {
            return@coroutineScope initialResults
                .filter { it.score >= minScore }
                .sortedByDescending { it.score }
                .take(topK)
        }

        // 获取前 topK 个结果的向量
        val topResults = initialResults
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topK)

        // 提取向量
        val resultVectors = topResults.mapNotNull { it.vector }
        if (resultVectors.isEmpty() || resultVectors.size < topResults.size) {
            // 如果没有向量，则返回初始结果
            return@coroutineScope topResults
        }

        // 递归查询
        val nextResults = recursiveFusion(
            vectorStore = vectorStore,
            indexName = indexName,
            queryEmbeddings = resultVectors,
            topK = topK,
            depth = depth - 1,
            minScore = minScore
        )

        // 合并结果
        val allResults = (topResults + nextResults).distinctBy { it.id }
        allResults
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * 加权融合策略。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queryEmbeddings 查询嵌入列表
     * @param topK 返回结果的最大数量
     * @param weights 查询权重
     * @param minScore 最小分数
     * @return 融合后的查询结果
     */
    private suspend fun weightedFusion(
        vectorStore: VectorStore,
        indexName: String,
        queryEmbeddings: List<FloatArray>,
        topK: Int,
        weights: List<Double>?,
        minScore: Double
    ): List<SearchResult> = coroutineScope {
        // 计算权重
        val normalizedWeights = if (weights != null && weights.size == queryEmbeddings.size) {
            val sum = weights.sum()
            if (sum > 0) weights.map { it / sum } else List(queryEmbeddings.size) { 1.0 / queryEmbeddings.size }
        } else {
            List(queryEmbeddings.size) { 1.0 / queryEmbeddings.size }
        }

        // 并行查询
        val results = queryEmbeddings.mapIndexed { index, queryEmbedding ->
            val weight = normalizedWeights[index]
            async {
                val queryResults = vectorStore.query(indexName, queryEmbedding, topK * 2)
                queryResults.map { it.copy(score = it.score * weight) }
            }
        }.awaitAll()

        // 合并结果
        val idToResults = mutableMapOf<String, MutableList<SearchResult>>()
        results.flatten().forEach { result ->
            idToResults.getOrPut(result.id) { mutableListOf() }.add(result)
        }

        // 计算加权分数
        val fusedResults = idToResults.map { (id, resultList) ->
            val totalScore = resultList.sumOf { it.score }
            val metadata = resultList.firstOrNull()?.metadata
            val vector = resultList.firstOrNull()?.vector
            SearchResult(id, totalScore, vector, metadata ?: emptyMap())
        }

        // 过滤、排序并限制结果数量
        fusedResults
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * 最大分数融合策略。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queryEmbeddings 查询嵌入列表
     * @param topK 返回结果的最大数量
     * @param minScore 最小分数
     * @return 融合后的查询结果
     */
    private suspend fun maxScoreFusion(
        vectorStore: VectorStore,
        indexName: String,
        queryEmbeddings: List<FloatArray>,
        topK: Int,
        minScore: Double
    ): List<SearchResult> = coroutineScope {
        // 并行查询
        val results = queryEmbeddings.map { queryEmbedding ->
            async { vectorStore.query(indexName, queryEmbedding, topK * 2) }
        }.awaitAll()

        // 合并结果
        val idToMaxScore = mutableMapOf<String, SearchResult>()
        results.flatten().forEach { result ->
            val existing = idToMaxScore[result.id]
            if (existing == null || existing.score < result.score) {
                idToMaxScore[result.id] = result
            }
        }

        // 过滤、排序并限制结果数量
        idToMaxScore.values
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topK)
    }

    /**
     * 平均分数融合策略。
     *
     * @param vectorStore 向量存储
     * @param indexName 索引名称
     * @param queryEmbeddings 查询嵌入列表
     * @param topK 返回结果的最大数量
     * @param minScore 最小分数
     * @return 融合后的查询结果
     */
    private suspend fun averageFusion(
        vectorStore: VectorStore,
        indexName: String,
        queryEmbeddings: List<FloatArray>,
        topK: Int,
        minScore: Double
    ): List<SearchResult> = coroutineScope {
        // 并行查询
        val results = queryEmbeddings.map { queryEmbedding ->
            async { vectorStore.query(indexName, queryEmbedding, topK * 2) }
        }.awaitAll()

        // 合并结果
        val idToResults = mutableMapOf<String, MutableList<SearchResult>>()
        results.flatten().forEach { result ->
            idToResults.getOrPut(result.id) { mutableListOf() }.add(result)
        }

        // 计算平均分数
        val fusedResults = idToResults.map { (id, resultList) ->
            val avgScore = resultList.sumOf { it.score } / resultList.size
            val metadata = resultList.firstOrNull()?.metadata
            val vector = resultList.firstOrNull()?.vector
            SearchResult(id, avgScore, vector, metadata ?: emptyMap())
        }

        // 过滤、排序并限制结果数量
        fusedResults
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(topK)
    }
}
