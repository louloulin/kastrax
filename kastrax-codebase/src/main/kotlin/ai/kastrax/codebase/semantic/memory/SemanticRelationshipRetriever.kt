package ai.kastrax.codebase.semantic.memory

import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * 语义关系检索器配置
 *
 * @property maxResults 最大结果数量
 * @property minScore 最小分数
 * @property maxDepth 最大关系深度
 * @property relationshipWeights 关系类型权重
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 */
data class SemanticRelationshipRetrieverConfig(
    val maxResults: Int = 10,
    val minScore: Double = 0.7,
    val maxDepth: Int = 3,
    val relationshipWeights: Map<SemanticRelationshipType, Double> = defaultRelationshipWeights(),
    val enableCaching: Boolean = true,
    val cacheSize: Int = 100
)

/**
 * 默认关系类型权重
 *
 * @return 关系类型权重映射
 */
fun defaultRelationshipWeights(): Map<SemanticRelationshipType, Double> {
    return mapOf(
        SemanticRelationshipType.IMPORTS to 0.9,
        SemanticRelationshipType.USES to 0.8,
        SemanticRelationshipType.EXTENDS to 0.8,
        SemanticRelationshipType.IMPLEMENTS to 0.8,
        SemanticRelationshipType.CALLS to 0.7,
        SemanticRelationshipType.REFERENCES to 0.7,
        SemanticRelationshipType.SIMILAR_TO to 0.6,
        SemanticRelationshipType.DEPENDS_ON to 0.6,
        SemanticRelationshipType.PART_OF to 0.5,
        SemanticRelationshipType.RELATED_TO to 0.4
    )
}

/**
 * 语义关系检索结果
 *
 * @property memory 语义记忆
 * @property score 相似度分数
 * @property relationshipPath 关系路径
 */
data class SemanticRelationshipSearchResult(
    val memory: SemanticMemory,
    val score: Double,
    val relationshipPath: List<SemanticRelationship> = emptyList()
)

/**
 * 语义关系检索器
 *
 * 基于语义关系图进行检索
 *
 * @property memoryStore 语义记忆存储
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class SemanticRelationshipRetriever(
    private val memoryStore: SemanticMemoryStore,
    private val embeddingService: EmbeddingService,
    private val config: SemanticRelationshipRetrieverConfig = SemanticRelationshipRetrieverConfig()
) {
    // 查询缓存
    private val queryCache = ConcurrentHashMap<String, List<SemanticRelationshipSearchResult>>()

    // 读写锁
    private val lock = ReentrantReadWriteLock()

    /**
     * 基于关系检索
     *
     * @param startMemoryId 起始记忆ID
     * @param limit 返回结果的最大数量
     * @param maxDepth 最大关系深度
     * @return 检索结果列表
     */
    suspend fun retrieveByRelationship(
        startMemoryId: String,
        limit: Int = config.maxResults,
        maxDepth: Int = config.maxDepth
    ): List<SemanticRelationshipSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = "rel:$startMemoryId:$limit:$maxDepth"

            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = lock.read { queryCache[cacheKey] }
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }

            // 获取起始记忆
            val startMemory = memoryStore.getMemory(startMemoryId) ?: return@withContext emptyList()

            // 执行广度优先搜索
            val visited = mutableSetOf<String>()
            val queue = ArrayDeque<Pair<String, List<SemanticRelationship>>>()
            val results = mutableListOf<SemanticRelationshipSearchResult>()

            // 添加起始节点
            queue.add(Pair(startMemoryId, emptyList()))
            visited.add(startMemoryId)

            // 广度优先搜索
            while (queue.isNotEmpty() && results.size < limit * 2) {
                val (currentId, path) = queue.removeFirst()

                // 如果达到最大深度，跳过
                if (path.size >= maxDepth) {
                    continue
                }

                // 获取当前记忆的关系
                val relationships = memoryStore.getRelationships(currentId)

                // 遍历关系
                for (relationship in relationships) {
                    val targetId = relationship.targetId

                    // 如果已访问过，跳过
                    if (targetId in visited) {
                        continue
                    }

                    // 标记为已访问
                    visited.add(targetId)

                    // 获取目标记忆
                    val targetMemory = memoryStore.getMemory(targetId) ?: continue

                    // 计算分数
                    val relationshipScore = calculateRelationshipScore(relationship, path.size + 1)

                    // 添加到结果
                    val newPath = path + relationship
                    results.add(
                        SemanticRelationshipSearchResult(
                            memory = targetMemory,
                            score = relationshipScore,
                            relationshipPath = newPath
                        )
                    )

                    // 添加到队列
                    queue.add(Pair(targetId, newPath))
                }
            }

            // 排序并限制结果数量
            val sortedResults = results
                .sortedByDescending { it.score }
                .take(limit)

            // 缓存结果
            if (config.enableCaching) {
                lock.write {
                    // 如果缓存已满，移除最早的条目
                    if (queryCache.size >= config.cacheSize) {
                        val oldestKey = queryCache.keys.firstOrNull()
                        if (oldestKey != null) {
                            queryCache.remove(oldestKey)
                        }
                    }

                    queryCache[cacheKey] = sortedResults
                }
            }

            return@withContext sortedResults
        } catch (e: Exception) {
            logger.error(e) { "基于关系检索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 混合检索（结合语义相似度和关系）
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun hybridRetrieve(
        query: String,
        limit: Int = config.maxResults,
        minScore: Double = config.minScore
    ): List<SemanticRelationshipSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = "hybrid:$query:$limit:$minScore"

            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = lock.read { queryCache[cacheKey] }
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }

            // 并行执行向量搜索和关系扩展
            val results = coroutineScope {
                // 首先执行向量搜索
                val vectorSearchResults = async {
                    semanticSearch(query, limit * 2, minScore)
                }

                // 获取向量搜索结果
                val vectorResults = vectorSearchResults.await()

                // 如果没有结果，返回空列表
                if (vectorResults.isEmpty()) {
                    return@coroutineScope emptyList()
                }

                // 对每个向量搜索结果执行关系扩展
                val expandedResults = vectorResults.flatMap { result ->
                    val relationshipResults = retrieveByRelationship(
                        startMemoryId = result.memory.id,
                        limit = 5,
                        maxDepth = 2
                    )

                    // 合并向量搜索结果和关系扩展结果
                    listOf(result) + relationshipResults
                }

                // 去重
                expandedResults.distinctBy { it.memory.id }
            }

            // 排序并限制结果数量
            val sortedResults = results
                .sortedByDescending { it.score }
                .take(limit)

            // 缓存结果
            if (config.enableCaching) {
                lock.write {
                    // 如果缓存已满，移除最早的条目
                    if (queryCache.size >= config.cacheSize) {
                        val oldestKey = queryCache.keys.firstOrNull()
                        if (oldestKey != null) {
                            queryCache.remove(oldestKey)
                        }
                    }

                    queryCache[cacheKey] = sortedResults
                }
            }

            return@withContext sortedResults
        } catch (e: Exception) {
            logger.error(e) { "混合检索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 语义搜索
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun semanticSearch(
        query: String,
        limit: Int = config.maxResults,
        minScore: Double = config.minScore
    ): List<SemanticRelationshipSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = "semantic:$query:$limit:$minScore"

            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = lock.read { queryCache[cacheKey] }
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }

            // 生成查询向量
            val queryEmbedding = embeddingService.embed(query)

            // 获取所有记忆
            val memories = memoryStore.getAllMemories()

            // 计算相似度
            val results = memories.mapNotNull { memory ->
                // 如果记忆已有向量，直接使用
                val memoryVector = memory.semanticVector ?: return@mapNotNull null

                // 计算余弦相似度
                val similarity = cosineSimilarity(queryEmbedding, memoryVector)

                // 如果相似度低于阈值，跳过
                if (similarity < minScore) {
                    return@mapNotNull null
                }

                SemanticRelationshipSearchResult(
                    memory = memory,
                    score = similarity
                )
            }

            // 排序并限制结果数量
            val sortedResults = results
                .sortedByDescending { it.score }
                .take(limit)

            // 缓存结果
            if (config.enableCaching) {
                lock.write {
                    // 如果缓存已满，移除最早的条目
                    if (queryCache.size >= config.cacheSize) {
                        val oldestKey = queryCache.keys.firstOrNull()
                        if (oldestKey != null) {
                            queryCache.remove(oldestKey)
                        }
                    }

                    queryCache[cacheKey] = sortedResults
                }
            }

            return@withContext sortedResults
        } catch (e: Exception) {
            logger.error(e) { "语义搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 计算关系分数
     *
     * @param relationship 语义关系
     * @param depth 深度
     * @return 分数
     */
    private fun calculateRelationshipScore(relationship: SemanticRelationship, depth: Int): Double {
        // 获取关系类型权重
        val typeWeight = config.relationshipWeights[relationship.type] ?: 0.5

        // 考虑关系强度
        val strengthWeight = when (relationship.strength) {
            RelationshipStrength.STRONG -> 1.0
            RelationshipStrength.MEDIUM -> 0.7
            RelationshipStrength.WEAK -> 0.4
        }

        // 考虑深度衰减
        val depthDecay = 1.0 / depth

        // 计算最终分数
        return typeWeight * strengthWeight * depthDecay * relationship.weight
    }

    /**
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) {
            throw IllegalArgumentException("向量维度不匹配: ${vec1.size} vs ${vec2.size}")
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        // 避免除以零
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        lock.write {
            queryCache.clear()
        }
    }
}
