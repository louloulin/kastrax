package ai.kastrax.codebase.retrieval.model

import ai.kastrax.codebase.embedding.EmbeddingService
import ai.kastrax.codebase.semantic.memory.ImportanceLevel
import ai.kastrax.codebase.semantic.memory.MemoryType
import ai.kastrax.codebase.semantic.memory.SemanticMemory
import ai.kastrax.codebase.semantic.memory.SemanticMemoryRetriever
import ai.kastrax.codebase.semantic.memory.SemanticMemorySearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 上下文感知检索模型配置
 *
 * @property contextWindowSize 上下文窗口大小
 * @property contextWeight 上下文权重
 * @property recencyDecayFactor 时间衰减因子
 * @property popularityBoostFactor 流行度提升因子
 * @property importanceBoostFactors 重要性提升因子
 * @property typeBoostFactors 类型提升因子
 * @property enableExplanations 是否启用解释
 * @property enableUserFeedbackLearning 是否启用用户反馈学习
 * @property userFeedbackWeight 用户反馈权重
 */
data class ContextAwareRetrievalModelConfig(
    val contextWindowSize: Int = 3,
    val contextWeight: Double = 0.3,
    val recencyDecayFactor: Double = 0.1,
    val popularityBoostFactor: Double = 0.05,
    val importanceBoostFactors: Map<ImportanceLevel, Double> = mapOf(
        ImportanceLevel.CRITICAL to 1.5,
        ImportanceLevel.HIGH to 1.3,
        ImportanceLevel.MEDIUM to 1.0,
        ImportanceLevel.LOW to 0.8,
        ImportanceLevel.UNKNOWN to 0.9
    ),
    val typeBoostFactors: Map<MemoryType, Double> = mapOf(
        MemoryType.CODE_STRUCTURE to 1.2,
        MemoryType.SYMBOL_DEFINITION to 1.3,
        MemoryType.SYMBOL_REFERENCE to 1.1,
        MemoryType.INHERITANCE to 1.4,
        MemoryType.IMPLEMENTATION to 1.4,
        MemoryType.CALL_HIERARCHY to 1.2,
        MemoryType.IMPORT_DEPENDENCY to 1.0,
        MemoryType.LIBRARY_USAGE to 1.1,
        MemoryType.SEMANTIC_RELATION to 1.2,
        MemoryType.CUSTOM to 1.0
    ),
    val enableExplanations: Boolean = true,
    val enableUserFeedbackLearning: Boolean = true,
    val userFeedbackWeight: Double = 0.2
) {
    init {
        require(contextWindowSize >= 0) { "Context window size must be non-negative" }
        require(contextWeight in 0.0..1.0) { "Context weight must be between 0.0 and 1.0" }
        require(recencyDecayFactor > 0.0) { "Recency decay factor must be positive" }
        require(popularityBoostFactor >= 0.0) { "Popularity boost factor must be non-negative" }
        require(userFeedbackWeight in 0.0..1.0) { "User feedback weight must be between 0.0 and 1.0" }
    }
}

/**
 * 上下文感知检索模型
 *
 * 基于当前编辑上下文的相关性评分模型
 *
 * @property memoryRetriever 语义记忆检索器
 * @property contextConfig 上下文配置
 */
class ContextAwareRetrievalModel(
    embeddingService: EmbeddingService,
    private val memoryRetriever: SemanticMemoryRetriever,
    config: RetrievalModelConfig = RetrievalModelConfig(),
    private val contextConfig: ContextAwareRetrievalModelConfig = ContextAwareRetrievalModelConfig()
) : RetrievalModel(embeddingService, config) {
    
    /**
     * 检索记忆
     *
     * @param context 检索上下文
     * @param limit 返回结果的最大数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    override suspend fun retrieve(
        context: RetrievalContext,
        limit: Int,
        minScore: Double
    ): List<RetrievalResult> = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = generateCacheKey(context, limit, minScore)
            
            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = queryCache[cacheKey]
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }
            
            // 执行语义搜索
            val semanticResults = memoryRetriever.semanticSearch(
                query = context.query,
                limit = limit * 2, // 获取更多结果，然后重新排序
                minScore = minScore / 2 // 降低阈值，获取更多候选结果
            )
            
            if (semanticResults.isEmpty()) {
                return@withContext emptyList()
            }
            
            // 计算上下文相关性并重新排序
            val rerankedResults = rerank(semanticResults, context)
            
            // 限制结果数量并过滤低分结果
            val finalResults = rerankedResults
                .filter { it.score >= minScore }
                .take(limit)
            
            // 缓存结果
            if (config.enableCaching) {
                // 如果缓存已满，移除最早的条目
                if (queryCache.size >= config.cacheSize) {
                    val oldestKey = queryCache.keys.firstOrNull()
                    if (oldestKey != null) {
                        queryCache.remove(oldestKey)
                    }
                }
                
                queryCache[cacheKey] = finalResults
            }
            
            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "检索记忆失败: ${e.message}" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 重新排序结果
     *
     * @param results 搜索结果列表
     * @param context 检索上下文
     * @return 重新排序后的结果列表
     */
    private suspend fun rerank(
        results: List<SemanticMemorySearchResult>,
        context: RetrievalContext
    ): List<RetrievalResult> = coroutineScope {
        // 并行计算每个结果的特征和分数
        results.map { result ->
            async {
                // 计算特征
                val features = computeFeatures(result.memory, context)
                
                // 计算最终分数
                val finalScore = computeFinalScore(features)
                
                // 生成解释
                val explanation = if (contextConfig.enableExplanations) {
                    generateExplanation(result.memory, features, finalScore)
                } else {
                    null
                }
                
                // 创建检索结果
                RetrievalResult(
                    memory = result.memory,
                    score = finalScore,
                    features = features,
                    explanation = explanation
                )
            }
        }.awaitAll().sortedByDescending { it.score }
    }
    
    /**
     * 计算特征
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 特征列表
     */
    override suspend fun computeFeatures(
        memory: SemanticMemory,
        context: RetrievalContext
    ): List<RetrievalFeature> = withContext(Dispatchers.IO) {
        val features = mutableListOf<RetrievalFeature>()
        
        // 1. 语义相似度特征
        val semanticWeight = config.featureWeights["semantic"] ?: 0.7
        val semanticScore = calculateSemanticScore(memory, context)
        features.add(RetrievalFeature("semantic", semanticWeight, semanticScore))
        
        // 2. 关键词匹配特征
        val keywordWeight = config.featureWeights["keyword"] ?: 0.2
        val keywordScore = calculateKeywordScore(memory, context)
        features.add(RetrievalFeature("keyword", keywordWeight, keywordScore))
        
        // 3. 时间相关性特征
        val recencyWeight = config.featureWeights["recency"] ?: 0.05
        val recencyScore = calculateRecencyScore(memory)
        features.add(RetrievalFeature("recency", recencyWeight, recencyScore))
        
        // 4. 流行度特征
        val popularityWeight = config.featureWeights["popularity"] ?: 0.05
        val popularityScore = calculatePopularityScore(memory)
        features.add(RetrievalFeature("popularity", popularityWeight, popularityScore))
        
        // 5. 重要性特征
        val importanceWeight = config.featureWeights["importance"] ?: 0.1
        val importanceScore = calculateImportanceScore(memory)
        features.add(RetrievalFeature("importance", importanceWeight, importanceScore))
        
        // 6. 类型相关性特征
        val typeWeight = config.featureWeights["type"] ?: 0.1
        val typeScore = calculateTypeScore(memory)
        features.add(RetrievalFeature("type", typeWeight, typeScore))
        
        // 7. 上下文相关性特征
        val contextWeight = config.featureWeights["context"] ?: 0.2
        val contextScore = calculateContextScore(memory, context)
        features.add(RetrievalFeature("context", contextWeight, contextScore))
        
        // 8. 用户反馈特征
        if (contextConfig.enableUserFeedbackLearning) {
            val feedbackWeight = config.featureWeights["feedback"] ?: 0.1
            val feedbackScore = calculateFeedbackScore(memory, context)
            features.add(RetrievalFeature("feedback", feedbackWeight, feedbackScore))
        }
        
        return@withContext features
    }
    
    /**
     * 计算语义相似度分数
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 语义相似度分数
     */
    private suspend fun calculateSemanticScore(
        memory: SemanticMemory,
        context: RetrievalContext
    ): Double = withContext(Dispatchers.IO) {
        try {
            // 生成查询嵌入向量
            val queryEmbedding = embeddingService.generateEmbedding(
                text = context.query,
                modelName = config.embeddingModelName
            )
            
            // 生成记忆内容嵌入向量
            val memoryEmbedding = embeddingService.generateEmbedding(
                text = memory.content,
                modelName = config.embeddingModelName
            )
            
            // 计算余弦相似度
            val similarity = calculateCosineSimilarity(queryEmbedding, memoryEmbedding)
            
            // 考虑上下文
            if (contextConfig.contextWindowSize > 0 && context.previousQueries.isNotEmpty()) {
                // 获取上下文窗口
                val contextQueries = context.previousQueries
                    .takeLast(contextConfig.contextWindowSize)
                
                // 如果有上下文查询
                if (contextQueries.isNotEmpty()) {
                    // 计算上下文嵌入向量
                    val contextEmbeddings = contextQueries.map { query ->
                        embeddingService.generateEmbedding(
                            text = query,
                            modelName = config.embeddingModelName
                        )
                    }
                    
                    // 计算上下文相似度
                    val contextSimilarities = contextEmbeddings.map { embedding ->
                        calculateCosineSimilarity(embedding, memoryEmbedding)
                    }
                    
                    // 计算平均上下文相似度
                    val avgContextSimilarity = contextSimilarities.average()
                    
                    // 结合当前查询相似度和上下文相似度
                    return@withContext (1 - contextConfig.contextWeight) * similarity +
                            contextConfig.contextWeight * avgContextSimilarity
                }
            }
            
            return@withContext similarity
        } catch (e: Exception) {
            logger.error(e) { "计算语义相似度分数失败: ${e.message}" }
            return@withContext 0.0
        }
    }
    
    /**
     * 计算关键词匹配分数
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 关键词匹配分数
     */
    private fun calculateKeywordScore(
        memory: SemanticMemory,
        context: RetrievalContext
    ): Double {
        try {
            val content = memory.content.lowercase()
            
            // 分词
            val keywords = context.query.split(Regex("\\s+"))
                .filter { it.length > 2 }
                .map { it.lowercase() }
            
            if (keywords.isEmpty()) {
                return 0.0
            }
            
            // 计算每个关键词的匹配次数
            val keywordCounts = keywords.associateWith { keyword ->
                val regex = Regex("\\b${Regex.escape(keyword)}\\b")
                regex.findAll(content).count()
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
            val frequencyBonus = min(totalMatches.toDouble() / 10.0, 0.5)
            
            return keywordScore + frequencyBonus
        } catch (e: Exception) {
            logger.error(e) { "计算关键词匹配分数失败: ${e.message}" }
            return 0.0
        }
    }
    
    /**
     * 计算时间相关性分数
     *
     * @param memory 语义记忆
     * @return 时间相关性分数
     */
    private fun calculateRecencyScore(memory: SemanticMemory): Double {
        try {
            // 计算记忆的年龄（小时）
            val ageHours = Duration.between(memory.creationTime, Instant.now()).toHours().toDouble()
            
            // 使用指数衰减函数
            return exp(-contextConfig.recencyDecayFactor * ageHours / 24.0)
        } catch (e: Exception) {
            logger.error(e) { "计算时间相关性分数失败: ${e.message}" }
            return 0.5 // 默认中等时间相关性
        }
    }
    
    /**
     * 计算流行度分数
     *
     * @param memory 语义记忆
     * @return 流行度分数
     */
    private fun calculatePopularityScore(memory: SemanticMemory): Double {
        try {
            // 基于访问次数计算流行度
            val accessCount = memory.accessCount
            
            // 使用对数函数，避免高访问次数的记忆过度主导
            return min(1.0, contextConfig.popularityBoostFactor * ln(1.0 + accessCount))
        } catch (e: Exception) {
            logger.error(e) { "计算流行度分数失败: ${e.message}" }
            return 0.5 // 默认中等流行度
        }
    }
    
    /**
     * 计算重要性分数
     *
     * @param memory 语义记忆
     * @return 重要性分数
     */
    private fun calculateImportanceScore(memory: SemanticMemory): Double {
        try {
            // 获取重要性提升因子
            val boostFactor = contextConfig.importanceBoostFactors[memory.importance] ?: 1.0
            
            // 将提升因子转换为 0-1 范围的分数
            return min(1.0, boostFactor / 1.5)
        } catch (e: Exception) {
            logger.error(e) { "计算重要性分数失败: ${e.message}" }
            return 0.5 // 默认中等重要性
        }
    }
    
    /**
     * 计算类型相关性分数
     *
     * @param memory 语义记忆
     * @return 类型相关性分数
     */
    private fun calculateTypeScore(memory: SemanticMemory): Double {
        try {
            // 获取类型提升因子
            val boostFactor = contextConfig.typeBoostFactors[memory.type] ?: 1.0
            
            // 将提升因子转换为 0-1 范围的分数
            return min(1.0, boostFactor / 1.5)
        } catch (e: Exception) {
            logger.error(e) { "计算类型相关性分数失败: ${e.message}" }
            return 0.5 // 默认中等类型相关性
        }
    }
    
    /**
     * 计算上下文相关性分数
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 上下文相关性分数
     */
    private fun calculateContextScore(
        memory: SemanticMemory,
        context: RetrievalContext
    ): Double {
        try {
            var contextScore = 0.0
            
            // 如果有当前文件，检查记忆是否与当前文件相关
            if (context.currentFile != null) {
                val isRelatedToCurrentFile = memory.sourceElements.any { element ->
                    element.location.filePath.toString().endsWith(context.currentFile)
                } || memory.sourceSymbols.any { symbol ->
                    symbol.location.filePath.toString().endsWith(context.currentFile)
                }
                
                if (isRelatedToCurrentFile) {
                    contextScore += 0.5
                }
            }
            
            // 如果有选中的文本，检查记忆是否与选中的文本相关
            if (!context.selectedText.isNullOrEmpty()) {
                val selectedTextLower = context.selectedText.lowercase()
                val contentLower = memory.content.lowercase()
                
                if (contentLower.contains(selectedTextLower)) {
                    contextScore += 0.3
                }
                
                // 检查是否包含选中文本中的关键词
                val keywords = selectedTextLower.split(Regex("\\s+"))
                    .filter { it.length > 2 }
                
                val keywordMatchRatio = keywords.count { contentLower.contains(it) }.toDouble() / max(1, keywords.size)
                contextScore += 0.2 * keywordMatchRatio
            }
            
            return min(1.0, contextScore)
        } catch (e: Exception) {
            logger.error(e) { "计算上下文相关性分数失败: ${e.message}" }
            return 0.0
        }
    }
    
    /**
     * 计算用户反馈分数
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 用户反馈分数
     */
    private fun calculateFeedbackScore(
        memory: SemanticMemory,
        context: RetrievalContext
    ): Double {
        try {
            // 如果没有用户反馈，返回中性分数
            if (context.userFeedback.isEmpty()) {
                return 0.5
            }
            
            // 检查是否有针对此记忆的反馈
            val feedback = context.userFeedback[memory.id]
            
            if (feedback != null) {
                // 直接使用反馈分数
                return feedback
            }
            
            // 如果没有针对此记忆的反馈，检查是否有针对类似记忆的反馈
            val similarMemoryFeedbacks = context.userFeedback.filter { (memoryId, _) ->
                // 获取记忆
                val feedbackMemory = context.previousResults.find { it.memory.id == memoryId }?.memory
                
                // 检查是否是同类型的记忆
                feedbackMemory != null && feedbackMemory.type == memory.type
            }
            
            if (similarMemoryFeedbacks.isNotEmpty()) {
                // 计算平均反馈分数
                return similarMemoryFeedbacks.values.average()
            }
            
            return 0.5 // 默认中性反馈
        } catch (e: Exception) {
            logger.error(e) { "计算用户反馈分数失败: ${e.message}" }
            return 0.5
        }
    }
    
    /**
     * 计算余弦相似度
     *
     * @param vec1 向量 1
     * @param vec2 向量 2
     * @return 余弦相似度
     */
    private fun calculateCosineSimilarity(vec1: List<Float>, vec2: List<Float>): Double {
        if (vec1.isEmpty() || vec2.isEmpty() || vec1.size != vec2.size) {
            return 0.0
        }
        
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        
        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
    }
    
    /**
     * 生成缓存键
     *
     * @param context 检索上下文
     * @param limit 返回结果的最大数量
     * @param minScore 最小分数
     * @return 缓存键
     */
    private fun generateCacheKey(
        context: RetrievalContext,
        limit: Int,
        minScore: Double
    ): String {
        val sb = StringBuilder()
        
        sb.append(context.query)
        sb.append(":$limit:$minScore")
        
        // 添加上下文窗口中的查询
        if (contextConfig.contextWindowSize > 0 && context.previousQueries.isNotEmpty()) {
            val contextQueries = context.previousQueries
                .takeLast(contextConfig.contextWindowSize)
                .joinToString("|")
            
            sb.append(":$contextQueries")
        }
        
        // 添加当前文件
        if (context.currentFile != null) {
            sb.append(":${context.currentFile}")
        }
        
        // 添加选中的文本
        if (!context.selectedText.isNullOrEmpty()) {
            sb.append(":${context.selectedText.hashCode()}")
        }
        
        return sb.toString()
    }
    
    /**
     * 自然对数函数，确保非负输入
     *
     * @param x 输入值
     * @return 自然对数值
     */
    private fun ln(x: Double): Double {
        return if (x <= 0.0) 0.0 else Math.log(x)
    }
}
