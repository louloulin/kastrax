package ai.kastrax.codebase.retrieval.model

import ai.kastrax.codebase.embedding.EmbeddingService
import ai.kastrax.codebase.semantic.memory.SemanticMemory
import ai.kastrax.codebase.semantic.memory.SemanticMemoryRetriever
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 多因素排序模型配置
 *
 * @property factorWeights 因素权重
 * @property boostFactors 提升因素
 * @property penaltyFactors 惩罚因素
 * @property diversityFactor 多样性因素
 * @property noveltyFactor 新颖性因素
 * @property adaptiveWeighting 是否使用自适应权重
 * @property learningRate 学习率
 */
data class MultifactorRankingModelConfig(
    val factorWeights: Map<String, Double> = mapOf(
        "relevance" to 0.5,
        "recency" to 0.1,
        "popularity" to 0.1,
        "specificity" to 0.1,
        "diversity" to 0.1,
        "novelty" to 0.1
    ),
    val boostFactors: Map<String, Double> = mapOf(
        "exact_match" to 1.5,
        "file_match" to 1.3,
        "symbol_match" to 1.2
    ),
    val penaltyFactors: Map<String, Double> = mapOf(
        "duplicate_content" to 0.7,
        "low_quality" to 0.8
    ),
    val diversityFactor: Double = 0.1,
    val noveltyFactor: Double = 0.1,
    val adaptiveWeighting: Boolean = true,
    val learningRate: Double = 0.01
)

/**
 * 多因素排序模型
 *
 * 实现基于多种因素的检索排序算法
 *
 * @property memoryRetriever 语义记忆检索器
 * @property rankingConfig 排序配置
 */
class MultifactorRankingModel(
    embeddingService: EmbeddingService,
    private val memoryRetriever: SemanticMemoryRetriever,
    config: RetrievalModelConfig = RetrievalModelConfig(),
    private val rankingConfig: MultifactorRankingModelConfig = MultifactorRankingModelConfig()
) : RetrievalModel(embeddingService, config) {
    
    // 自适应权重
    private val adaptiveWeights = ConcurrentHashMap<String, Double>()
    
    // 已选择的记忆集合（用于多样性计算）
    private val selectedMemories = ConcurrentHashMap<String, Set<String>>()
    
    init {
        // 初始化自适应权重
        if (rankingConfig.adaptiveWeighting) {
            rankingConfig.factorWeights.forEach { (factor, weight) ->
                adaptiveWeights[factor] = weight
            }
        }
    }
    
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
            val cacheKey = "${context.query}:$limit:$minScore"
            
            // 检查缓存
            if (config.enableCaching) {
                val cachedResult = queryCache[cacheKey]
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }
            
            // 清除之前的选择记忆
            selectedMemories.remove(cacheKey)
            
            // 执行语义搜索
            val semanticResults = memoryRetriever.semanticSearch(
                query = context.query,
                limit = limit * 3, // 获取更多结果，然后重新排序
                minScore = minScore / 2 // 降低阈值，获取更多候选结果
            )
            
            if (semanticResults.isEmpty()) {
                return@withContext emptyList()
            }
            
            // 多因素排序
            val rankedResults = rankResults(semanticResults.map { it.memory }, context, cacheKey)
            
            // 限制结果数量并过滤低分结果
            val finalResults = rankedResults
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
            
            // 更新自适应权重
            if (rankingConfig.adaptiveWeighting && context.userFeedback.isNotEmpty()) {
                updateAdaptiveWeights(context)
            }
            
            return@withContext finalResults
        } catch (e: Exception) {
            logger.error(e) { "检索记忆失败: ${e.message}" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 多因素排序
     *
     * @param memories 记忆列表
     * @param context 检索上下文
     * @param cacheKey 缓存键
     * @return 排序后的结果列表
     */
    private suspend fun rankResults(
        memories: List<SemanticMemory>,
        context: RetrievalContext,
        cacheKey: String
    ): List<RetrievalResult> = coroutineScope {
        // 初始化已选择的记忆集合
        selectedMemories[cacheKey] = emptySet()
        
        // 结果列表
        val results = mutableListOf<RetrievalResult>()
        
        // 候选记忆
        val candidates = memories.toMutableList()
        
        // 逐个选择最佳记忆
        while (candidates.isNotEmpty() && results.size < config.maxResults) {
            // 并行计算每个候选记忆的特征和分数
            val candidateResults = candidates.map { memory ->
                async {
                    // 计算特征
                    val features = computeFeatures(memory, context)
                    
                    // 计算最终分数
                    val finalScore = computeFinalScore(features)
                    
                    // 应用多样性和新颖性调整
                    val adjustedScore = adjustScoreForDiversityAndNovelty(
                        memory,
                        finalScore,
                        results.map { it.memory },
                        cacheKey
                    )
                    
                    // 生成解释
                    val explanation = generateExplanation(memory, features, adjustedScore)
                    
                    // 创建检索结果
                    RetrievalResult(
                        memory = memory,
                        score = adjustedScore,
                        features = features,
                        explanation = explanation
                    )
                }
            }.awaitAll()
            
            // 选择得分最高的候选记忆
            val bestResult = candidateResults.maxByOrNull { it.score }
            
            if (bestResult != null) {
                // 添加到结果列表
                results.add(bestResult)
                
                // 从候选列表中移除
                candidates.remove(bestResult.memory)
                
                // 更新已选择的记忆集合
                val selected = selectedMemories[cacheKey] ?: emptySet()
                selectedMemories[cacheKey] = selected + bestResult.memory.id
            } else {
                // 没有更多候选记忆
                break
            }
        }
        
        return@coroutineScope results
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
        
        // 1. 相关性特征
        val relevanceWeight = getFactorWeight("relevance")
        val relevanceScore = calculateRelevanceScore(memory, context)
        features.add(RetrievalFeature("relevance", relevanceWeight, relevanceScore))
        
        // 2. 时间相关性特征
        val recencyWeight = getFactorWeight("recency")
        val recencyScore = calculateRecencyScore(memory)
        features.add(RetrievalFeature("recency", recencyWeight, recencyScore))
        
        // 3. 流行度特征
        val popularityWeight = getFactorWeight("popularity")
        val popularityScore = calculatePopularityScore(memory)
        features.add(RetrievalFeature("popularity", popularityWeight, popularityScore))
        
        // 4. 特异性特征
        val specificityWeight = getFactorWeight("specificity")
        val specificityScore = calculateSpecificityScore(memory)
        features.add(RetrievalFeature("specificity", specificityWeight, specificityScore))
        
        // 5. 应用提升因素
        applyBoostFactors(memory, context, features)
        
        // 6. 应用惩罚因素
        applyPenaltyFactors(memory, context, features)
        
        return@withContext features
    }
    
    /**
     * 计算相关性分数
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 相关性分数
     */
    private suspend fun calculateRelevanceScore(
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
            return@withContext calculateCosineSimilarity(queryEmbedding, memoryEmbedding)
        } catch (e: Exception) {
            logger.error(e) { "计算相关性分数失败: ${e.message}" }
            return@withContext 0.0
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
            // 获取最后访问时间
            val lastAccessTime = memory.lastAccessTime
            
            // 计算时间差（小时）
            val hoursSinceLastAccess = (System.currentTimeMillis() - lastAccessTime.toEpochMilli()) / (1000.0 * 60 * 60)
            
            // 使用指数衰减函数
            return Math.exp(-0.01 * hoursSinceLastAccess)
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
            return min(1.0, 0.2 * Math.log(1.0 + accessCount))
        } catch (e: Exception) {
            logger.error(e) { "计算流行度分数失败: ${e.message}" }
            return 0.5 // 默认中等流行度
        }
    }
    
    /**
     * 计算特异性分数
     *
     * @param memory 语义记忆
     * @return 特异性分数
     */
    private fun calculateSpecificityScore(memory: SemanticMemory): Double {
        try {
            // 基于内容长度和结构计算特异性
            val contentLength = memory.content.length
            
            // 较长的内容通常更具体
            val lengthScore = min(1.0, contentLength / 1000.0)
            
            // 检查是否包含代码片段或技术细节
            val hasCodeOrDetails = memory.content.contains("```") ||
                    memory.content.contains("class ") ||
                    memory.content.contains("function ") ||
                    memory.content.contains("method ") ||
                    memory.content.contains("import ") ||
                    memory.content.contains("package ")
            
            val detailScore = if (hasCodeOrDetails) 0.8 else 0.4
            
            // 组合分数
            return 0.6 * lengthScore + 0.4 * detailScore
        } catch (e: Exception) {
            logger.error(e) { "计算特异性分数失败: ${e.message}" }
            return 0.5 // 默认中等特异性
        }
    }
    
    /**
     * 应用提升因素
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @param features 特征列表
     */
    private fun applyBoostFactors(
        memory: SemanticMemory,
        context: RetrievalContext,
        features: MutableList<RetrievalFeature>
    ) {
        try {
            // 1. 精确匹配提升
            if (memory.content.contains(context.query)) {
                val boostFactor = rankingConfig.boostFactors["exact_match"] ?: 1.5
                features.add(RetrievalFeature("boost_exact_match", 0.1, boostFactor))
            }
            
            // 2. 文件匹配提升
            if (context.currentFile != null) {
                val isFileMatch = memory.sourceElements.any { element ->
                    element.location.filePath.toString().endsWith(context.currentFile)
                } || memory.sourceSymbols.any { symbol ->
                    symbol.location.filePath.toString().endsWith(context.currentFile)
                }
                
                if (isFileMatch) {
                    val boostFactor = rankingConfig.boostFactors["file_match"] ?: 1.3
                    features.add(RetrievalFeature("boost_file_match", 0.1, boostFactor))
                }
            }
            
            // 3. 符号匹配提升
            if (!context.selectedText.isNullOrEmpty()) {
                val isSymbolMatch = memory.sourceSymbols.any { symbol ->
                    symbol.name.contains(context.selectedText) ||
                            context.selectedText.contains(symbol.name)
                }
                
                if (isSymbolMatch) {
                    val boostFactor = rankingConfig.boostFactors["symbol_match"] ?: 1.2
                    features.add(RetrievalFeature("boost_symbol_match", 0.1, boostFactor))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "应用提升因素失败: ${e.message}" }
        }
    }
    
    /**
     * 应用惩罚因素
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @param features 特征列表
     */
    private fun applyPenaltyFactors(
        memory: SemanticMemory,
        context: RetrievalContext,
        features: MutableList<RetrievalFeature>
    ) {
        try {
            // 1. 重复内容惩罚
            val isDuplicate = context.previousResults.any { result ->
                val similarity = calculateContentSimilarity(memory.content, result.memory.content)
                similarity > 0.8 // 80% 相似度阈值
            }
            
            if (isDuplicate) {
                val penaltyFactor = rankingConfig.penaltyFactors["duplicate_content"] ?: 0.7
                features.add(RetrievalFeature("penalty_duplicate", 0.1, penaltyFactor))
            }
            
            // 2. 低质量内容惩罚
            val isLowQuality = memory.content.length < 50 || // 内容太短
                    !memory.content.contains(" ") || // 没有空格
                    memory.content.count { it == '\n' } < 2 // 少于 2 行
            
            if (isLowQuality) {
                val penaltyFactor = rankingConfig.penaltyFactors["low_quality"] ?: 0.8
                features.add(RetrievalFeature("penalty_low_quality", 0.1, penaltyFactor))
            }
        } catch (e: Exception) {
            logger.error(e) { "应用惩罚因素失败: ${e.message}" }
        }
    }
    
    /**
     * 调整分数以考虑多样性和新颖性
     *
     * @param memory 语义记忆
     * @param score 原始分数
     * @param selectedMemories 已选择的记忆列表
     * @param cacheKey 缓存键
     * @return 调整后的分数
     */
    private suspend fun adjustScoreForDiversityAndNovelty(
        memory: SemanticMemory,
        score: Double,
        selectedMemories: List<SemanticMemory>,
        cacheKey: String
    ): Double = withContext(Dispatchers.IO) {
        try {
            var adjustedScore = score
            
            // 如果没有已选择的记忆，不需要调整
            if (selectedMemories.isEmpty()) {
                return@withContext adjustedScore
            }
            
            // 1. 多样性调整
            val diversityPenalty = calculateDiversityPenalty(memory, selectedMemories)
            adjustedScore *= (1.0 - rankingConfig.diversityFactor * diversityPenalty)
            
            // 2. 新颖性调整
            val noveltyBonus = calculateNoveltyBonus(memory, this@MultifactorRankingModel.selectedMemories[cacheKey] ?: emptySet())
            adjustedScore *= (1.0 + rankingConfig.noveltyFactor * noveltyBonus)
            
            return@withContext adjustedScore
        } catch (e: Exception) {
            logger.error(e) { "调整分数失败: ${e.message}" }
            return@withContext score
        }
    }
    
    /**
     * 计算多样性惩罚
     *
     * @param memory 语义记忆
     * @param selectedMemories 已选择的记忆列表
     * @return 多样性惩罚
     */
    private suspend fun calculateDiversityPenalty(
        memory: SemanticMemory,
        selectedMemories: List<SemanticMemory>
    ): Double = withContext(Dispatchers.IO) {
        try {
            if (selectedMemories.isEmpty()) {
                return@withContext 0.0
            }
            
            // 生成记忆内容嵌入向量
            val memoryEmbedding = embeddingService.generateEmbedding(
                text = memory.content,
                modelName = config.embeddingModelName
            )
            
            // 计算与已选择记忆的最大相似度
            val maxSimilarity = selectedMemories.map { selectedMemory ->
                val selectedEmbedding = embeddingService.generateEmbedding(
                    text = selectedMemory.content,
                    modelName = config.embeddingModelName
                )
                
                calculateCosineSimilarity(memoryEmbedding, selectedEmbedding)
            }.maxOrNull() ?: 0.0
            
            // 返回多样性惩罚
            return@withContext maxSimilarity
        } catch (e: Exception) {
            logger.error(e) { "计算多样性惩罚失败: ${e.message}" }
            return@withContext 0.0
        }
    }
    
    /**
     * 计算新颖性奖励
     *
     * @param memory 语义记忆
     * @param selectedMemoryIds 已选择的记忆 ID 集合
     * @return 新颖性奖励
     */
    private fun calculateNoveltyBonus(
        memory: SemanticMemory,
        selectedMemoryIds: Set<String>
    ): Double {
        try {
            // 检查记忆类型是否已经在已选择的记忆中
            val sameTypeCount = selectedMemoryIds.count { memoryId ->
                val selectedMemory = queryCache.values
                    .flatMap { it }
                    .find { it.memory.id == memoryId }?.memory
                
                selectedMemory?.type == memory.type
            }
            
            // 如果没有相同类型的记忆，给予新颖性奖励
            return if (sameTypeCount == 0) 1.0 else 0.0
        } catch (e: Exception) {
            logger.error(e) { "计算新颖性奖励失败: ${e.message}" }
            return 0.0
        }
    }
    
    /**
     * 计算内容相似度
     *
     * @param content1 内容 1
     * @param content2 内容 2
     * @return 内容相似度
     */
    private fun calculateContentSimilarity(content1: String, content2: String): Double {
        try {
            // 简单的基于词袋的相似度计算
            val words1 = content1.lowercase().split(Regex("\\s+")).toSet()
            val words2 = content2.lowercase().split(Regex("\\s+")).toSet()
            
            val intersection = words1.intersect(words2).size
            val union = words1.union(words2).size
            
            return if (union > 0) intersection.toDouble() / union else 0.0
        } catch (e: Exception) {
            logger.error(e) { "计算内容相似度失败: ${e.message}" }
            return 0.0
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
     * 获取因素权重
     *
     * @param factor 因素名称
     * @return 因素权重
     */
    private fun getFactorWeight(factor: String): Double {
        return if (rankingConfig.adaptiveWeighting) {
            adaptiveWeights[factor] ?: rankingConfig.factorWeights[factor] ?: 0.1
        } else {
            rankingConfig.factorWeights[factor] ?: 0.1
        }
    }
    
    /**
     * 更新自适应权重
     *
     * @param context 检索上下文
     */
    private fun updateAdaptiveWeights(context: RetrievalContext) {
        try {
            // 如果没有用户反馈，不更新权重
            if (context.userFeedback.isEmpty()) {
                return
            }
            
            // 获取用户反馈的平均分数
            val avgFeedback = context.userFeedback.values.average()
            
            // 如果平均分数低于阈值，调整权重
            if (avgFeedback < 0.5) {
                // 增加相关性权重
                adaptiveWeights["relevance"] = min(
                    1.0,
                    (adaptiveWeights["relevance"] ?: rankingConfig.factorWeights["relevance"] ?: 0.5) +
                            rankingConfig.learningRate
                )
                
                // 减少其他权重
                adaptiveWeights.keys.filter { it != "relevance" }.forEach { factor ->
                    adaptiveWeights[factor] = max(
                        0.0,
                        (adaptiveWeights[factor] ?: rankingConfig.factorWeights[factor] ?: 0.1) -
                                rankingConfig.learningRate / (adaptiveWeights.size - 1)
                    )
                }
            }
            
            // 归一化权重
            val sum = adaptiveWeights.values.sum()
            if (sum > 0) {
                adaptiveWeights.keys.forEach { factor ->
                    adaptiveWeights[factor] = (adaptiveWeights[factor] ?: 0.0) / sum
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "更新自适应权重失败: ${e.message}" }
        }
    }
}
