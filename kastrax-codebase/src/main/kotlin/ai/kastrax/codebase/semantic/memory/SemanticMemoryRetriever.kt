package ai.kastrax.codebase.semantic.memory

import ai.kastrax.codebase.embedding.EmbeddingModel
import ai.kastrax.codebase.embedding.EmbeddingService
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.store.VectorStore
import ai.kastrax.codebase.symbol.model.SymbolNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * 语义记忆检索配置
 *
 * @property embeddingModelName 嵌入模型名称
 * @property vectorStoreName 向量存储名称
 * @property vectorDimension 向量维度
 * @property maxResults 最大结果数
 * @property minScore 最小分数
 * @property hybridSearch 是否使用混合搜索
 * @property hybridWeight 混合权重 (0.0-1.0)，值越大越偏向向量搜索
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 */
data class SemanticMemoryRetrieverConfig(
    val embeddingModelName: String = "default",
    val vectorStoreName: String = "memory",
    val vectorDimension: Int = 1536,
    val maxResults: Int = 10,
    val minScore: Double = 0.7,
    val hybridSearch: Boolean = true,
    val hybridWeight: Double = 0.7,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000
)

/**
 * 语义记忆检索结果
 *
 * @property memory 语义记忆
 * @property score 相似度分数
 */
data class SemanticMemorySearchResult(
    val memory: SemanticMemory,
    val score: Double
)

/**
 * 语义记忆检索引擎
 *
 * 用于检索和查询语义记忆
 *
 * @property memoryStore 语义记忆存储
 * @property embeddingService 嵌入服务
 * @property vectorStore 向量存储
 * @property config 配置
 */
class SemanticMemoryRetriever(
    private val memoryStore: SemanticMemoryStore,
    private val embeddingService: EmbeddingService,
    private val vectorStore: VectorStore,
    private val config: SemanticMemoryRetrieverConfig = SemanticMemoryRetrieverConfig()
) {
    // 查询缓存
    private val queryCache = ConcurrentHashMap<String, List<SemanticMemorySearchResult>>()
    
    // 记忆 ID 到向量 ID 的映射
    private val memoryToVectorId = ConcurrentHashMap<String, String>()
    
    // 向量 ID 到记忆 ID 的映射
    private val vectorToMemoryId = ConcurrentHashMap<String, String>()
    
    // 读写锁
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 初始化检索引擎
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        logger.info { "初始化语义记忆检索引擎" }
        
        // 确保向量存储已创建
        if (!vectorStore.indexExists(config.vectorStoreName)) {
            vectorStore.createIndex(
                indexName = config.vectorStoreName,
                dimension = config.vectorDimension
            )
        }
    }
    
    /**
     * 索引记忆
     *
     * @param memory 语义记忆
     * @return 是否成功索引
     */
    suspend fun indexMemory(memory: SemanticMemory): Boolean = withContext(Dispatchers.IO) {
        try {
            // 生成嵌入向量
            val embedding = embeddingService.generateEmbedding(
                text = memory.content,
                modelName = config.embeddingModelName
            )
            
            // 生成向量 ID
            val vectorId = UUID.randomUUID().toString()
            
            // 保存向量
            vectorStore.saveVector(
                indexName = config.vectorStoreName,
                id = vectorId,
                vector = embedding,
                metadata = mapOf(
                    "memoryId" to memory.id,
                    "type" to memory.type.name,
                    "importance" to memory.importance.name,
                    "creationTime" to memory.creationTime.toString()
                )
            )
            
            // 更新映射
            lock.write {
                memoryToVectorId[memory.id] = vectorId
                vectorToMemoryId[vectorId] = memory.id
            }
            
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "索引记忆失败: ${e.message}" }
            return@withContext false
        }
    }
    
    /**
     * 批量索引记忆
     *
     * @param memories 语义记忆列表
     * @return 成功索引的记忆数量
     */
    suspend fun indexMemories(memories: List<SemanticMemory>): Int = withContext(Dispatchers.IO) {
        if (memories.isEmpty()) {
            return@withContext 0
        }
        
        try {
            // 生成嵌入向量
            val contents = memories.map { it.content }
            val embeddings = embeddingService.generateEmbeddings(
                texts = contents,
                modelName = config.embeddingModelName
            )
            
            // 生成向量 ID
            val vectorIds = List(memories.size) { UUID.randomUUID().toString() }
            
            // 准备元数据
            val metadataList = memories.map { memory ->
                mapOf(
                    "memoryId" to memory.id,
                    "type" to memory.type.name,
                    "importance" to memory.importance.name,
                    "creationTime" to memory.creationTime.toString()
                )
            }
            
            // 保存向量
            vectorStore.saveVectors(
                indexName = config.vectorStoreName,
                ids = vectorIds,
                vectors = embeddings,
                metadata = metadataList
            )
            
            // 更新映射
            lock.write {
                memories.forEachIndexed { index, memory ->
                    memoryToVectorId[memory.id] = vectorIds[index]
                    vectorToMemoryId[vectorIds[index]] = memory.id
                }
            }
            
            return@withContext memories.size
        } catch (e: Exception) {
            logger.error(e) { "批量索引记忆失败: ${e.message}" }
            
            // 尝试逐个索引
            var successCount = 0
            memories.forEach { memory ->
                if (indexMemory(memory)) {
                    successCount++
                }
            }
            
            return@withContext successCount
        }
    }
    
    /**
     * 更新记忆索引
     *
     * @param memory 语义记忆
     * @return 是否成功更新
     */
    suspend fun updateMemoryIndex(memory: SemanticMemory): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取向量 ID
            val vectorId = lock.read { memoryToVectorId[memory.id] }
            
            if (vectorId == null) {
                // 如果不存在，则创建新索引
                return@withContext indexMemory(memory)
            }
            
            // 生成嵌入向量
            val embedding = embeddingService.generateEmbedding(
                text = memory.content,
                modelName = config.embeddingModelName
            )
            
            // 更新向量
            vectorStore.updateVector(
                indexName = config.vectorStoreName,
                id = vectorId,
                vector = embedding,
                metadata = mapOf(
                    "memoryId" to memory.id,
                    "type" to memory.type.name,
                    "importance" to memory.importance.name,
                    "creationTime" to memory.creationTime.toString(),
                    "lastAccessTime" to memory.lastAccessTime.toString(),
                    "accessCount" to memory.accessCount.toString()
                )
            )
            
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "更新记忆索引失败: ${e.message}" }
            return@withContext false
        }
    }
    
    /**
     * 删除记忆索引
     *
     * @param memoryId 记忆 ID
     * @return 是否成功删除
     */
    suspend fun deleteMemoryIndex(memoryId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取向量 ID
            val vectorId = lock.read { memoryToVectorId[memoryId] } ?: return@withContext false
            
            // 删除向量
            vectorStore.deleteVector(
                indexName = config.vectorStoreName,
                id = vectorId
            )
            
            // 更新映射
            lock.write {
                memoryToVectorId.remove(memoryId)
                vectorToMemoryId.remove(vectorId)
            }
            
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "删除记忆索引失败: ${e.message}" }
            return@withContext false
        }
    }
    
    /**
     * 语义搜索
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param filter 过滤条件
     * @return 搜索结果列表
     */
    suspend fun semanticSearch(
        query: String,
        limit: Int = config.maxResults,
        minScore: Double = config.minScore,
        filter: Map<String, String> = emptyMap()
    ): List<SemanticMemorySearchResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = "$query:$limit:$minScore:${filter.hashCode()}"
            
            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = lock.read { queryCache[cacheKey] }
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }
            
            // 生成查询向量
            val queryEmbedding = embeddingService.generateEmbedding(
                text = query,
                modelName = config.embeddingModelName
            )
            
            // 搜索向量
            val searchResults = vectorStore.searchVectors(
                indexName = config.vectorStoreName,
                vector = queryEmbedding,
                limit = limit,
                minScore = minScore,
                filter = filter
            )
            
            // 获取记忆
            val results = searchResults.mapNotNull { (id, score, _) ->
                val memoryId = lock.read { vectorToMemoryId[id] } ?: return@mapNotNull null
                val memory = memoryStore.getMemory(memoryId) ?: return@mapNotNull null
                
                SemanticMemorySearchResult(memory, score)
            }
            
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
                    
                    queryCache[cacheKey] = results
                }
            }
            
            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "语义搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 混合搜索
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param filter 过滤条件
     * @return 搜索结果列表
     */
    suspend fun hybridSearch(
        query: String,
        limit: Int = config.maxResults,
        minScore: Double = config.minScore,
        filter: Map<String, String> = emptyMap()
    ): List<SemanticMemorySearchResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = "hybrid:$query:$limit:$minScore:${filter.hashCode()}"
            
            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = lock.read { queryCache[cacheKey] }
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }
            
            // 并行执行向量搜索和关键词搜索
            val (vectorResults, keywordResults) = coroutineScope {
                val vectorSearch = async {
                    semanticSearch(query, limit * 2, minScore, filter)
                }
                
                val keywordSearch = async {
                    keywordSearch(query, limit * 2, filter)
                }
                
                Pair(vectorSearch.await(), keywordSearch.await())
            }
            
            // 合并结果
            val mergedResults = mergeResults(
                vectorResults,
                keywordResults,
                limit,
                config.hybridWeight
            )
            
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
                    
                    queryCache[cacheKey] = mergedResults
                }
            }
            
            return@withContext mergedResults
        } catch (e: Exception) {
            logger.error(e) { "混合搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 关键词搜索
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param filter 过滤条件
     * @return 搜索结果列表
     */
    private suspend fun keywordSearch(
        query: String,
        limit: Int,
        filter: Map<String, String> = emptyMap()
    ): List<SemanticMemorySearchResult> = withContext(Dispatchers.IO) {
        try {
            // 分词
            val keywords = query.split(Regex("\\s+")).filter { it.length > 2 }
            
            if (keywords.isEmpty()) {
                return@withContext emptyList()
            }
            
            // 获取所有记忆
            val allMemories = memoryStore.getAllMemories()
            
            // 应用过滤器
            val filteredMemories = if (filter.isNotEmpty()) {
                allMemories.filter { memory ->
                    filter.all { (key, value) ->
                        when (key) {
                            "type" -> memory.type.name == value
                            "importance" -> memory.importance.name == value
                            else -> memory.metadata[key]?.toString() == value
                        }
                    }
                }
            } else {
                allMemories
            }
            
            // 计算相似度分数
            val scoredMemories = filteredMemories.map { memory ->
                val score = calculateKeywordScore(memory.content, keywords)
                SemanticMemorySearchResult(memory, score)
            }
            
            // 排序并限制结果数量
            return@withContext scoredMemories
                .filter { it.score >= 0.1 } // 过滤掉分数太低的结果
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "关键词搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 计算关键词分数
     *
     * @param text 文本
     * @param keywords 关键词列表
     * @return 分数
     */
    private fun calculateKeywordScore(text: String, keywords: List<String>): Double {
        val lowerText = text.lowercase()
        
        // 计算每个关键词的匹配次数
        val keywordCounts = keywords.associateWith { keyword ->
            val regex = Regex("\\b${Regex.escape(keyword.lowercase())}\\b")
            regex.findAll(lowerText).count()
        }
        
        // 计算总匹配次数
        val totalMatches = keywordCounts.values.sum()
        
        // 计算匹配的关键词数量
        val matchedKeywords = keywordCounts.count { it.value > 0 }
        
        // 计算分数
        val keywordScore = if (keywords.isNotEmpty()) {
            matchedKeywords.toDouble() / keywords.size
        } else {
            0.0
        }
        
        // 考虑匹配次数的影响
        val frequencyBonus = minOf(totalMatches.toDouble() / 10.0, 0.5)
        
        return keywordScore + frequencyBonus
    }
    
    /**
     * 合并结果
     *
     * @param vectorResults 向量搜索结果
     * @param keywordResults 关键词搜索结果
     * @param limit 返回结果的最大数量
     * @param vectorWeight 向量权重
     * @return 合并后的结果
     */
    private fun mergeResults(
        vectorResults: List<SemanticMemorySearchResult>,
        keywordResults: List<SemanticMemorySearchResult>,
        limit: Int,
        vectorWeight: Double = 0.7
    ): List<SemanticMemorySearchResult> {
        // 创建记忆 ID 到结果的映射
        val resultMap = mutableMapOf<String, SemanticMemorySearchResult>()
        
        // 处理向量搜索结果
        vectorResults.forEach { result ->
            resultMap[result.memory.id] = result
        }
        
        // 处理关键词搜索结果
        keywordResults.forEach { result ->
            val existingResult = resultMap[result.memory.id]
            if (existingResult != null) {
                // 合并分数
                val combinedScore = existingResult.score * vectorWeight + result.score * (1 - vectorWeight)
                resultMap[result.memory.id] = SemanticMemorySearchResult(result.memory, combinedScore)
            } else {
                // 调整分数
                val adjustedScore = result.score * (1 - vectorWeight)
                resultMap[result.memory.id] = SemanticMemorySearchResult(result.memory, adjustedScore)
            }
        }
        
        // 排序并限制结果数量
        return resultMap.values
            .sortedByDescending { it.score }
            .take(limit)
    }
    
    /**
     * 根据代码元素查找记忆
     *
     * @param element 代码元素
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findMemoriesByElement(element: CodeElement, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findMemoriesByElement(element.id).take(limit)
    }
    
    /**
     * 根据符号查找记忆
     *
     * @param symbol 符号节点
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findMemoriesBySymbol(symbol: SymbolNode, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findMemoriesBySymbol(symbol.id).take(limit)
    }
    
    /**
     * 根据文件路径查找记忆
     *
     * @param filePath 文件路径
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findMemoriesByFile(filePath: Path, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findMemoriesByFile(filePath).take(limit)
    }
    
    /**
     * 根据类型查找记忆
     *
     * @param type 记忆类型
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findMemoriesByType(type: MemoryType, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findMemoriesByType(type).take(limit)
    }
    
    /**
     * 根据重要性查找记忆
     *
     * @param importance 重要性级别
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findMemoriesByImportance(importance: ImportanceLevel, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findMemoriesByImportance(importance).take(limit)
    }
    
    /**
     * 查找相关记忆
     *
     * @param memoryId 记忆 ID
     * @param limit 返回结果的最大数量
     * @return 记忆列表
     */
    fun findRelatedMemories(memoryId: String, limit: Int = config.maxResults): List<SemanticMemory> {
        return memoryStore.findRelatedMemories(memoryId).take(limit)
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
