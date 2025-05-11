package ai.kastrax.codebase.retrieval.context

import ai.kastrax.codebase.embedding.EmbeddingService
import ai.kastrax.codebase.retrieval.model.RetrievalContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 查询意图类型
 */
enum class QueryIntentType {
    DEFINITION,          // 定义查询
    IMPLEMENTATION,      // 实现查询
    USAGE,               // 使用查询
    INHERITANCE,         // 继承查询
    DEPENDENCY,          // 依赖查询
    ARCHITECTURE,        // 架构查询
    DOCUMENTATION,       // 文档查询
    EXAMPLE,             // 示例查询
    DEBUGGING,           // 调试查询
    REFACTORING,         // 重构查询
    GENERAL              // 通用查询
}

/**
 * 查询意图
 *
 * @property type 意图类型
 * @property confidence 置信度
 * @property entities 实体列表
 * @property metadata 元数据
 */
data class QueryIntent(
    val type: QueryIntentType,
    val confidence: Double,
    val entities: Map<String, String> = emptyMap(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 上下文选择结果
 *
 * @property selectedContexts 选择的上下文列表
 * @property relevanceScores 相关性分数
 * @property intent 查询意图
 */
data class ContextSelectionResult(
    val selectedContexts: List<ContextElement>,
    val relevanceScores: Map<String, Double>,
    val intent: QueryIntent
)

/**
 * 意图检测器配置
 *
 * @property embeddingModelName 嵌入模型名称
 * @property intentPatterns 意图模式
 * @property confidenceThreshold 置信度阈值
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 */
data class IntentDetectorConfig(
    val embeddingModelName: String = "default",
    val intentPatterns: Map<QueryIntentType, List<String>> = mapOf(
        QueryIntentType.DEFINITION to listOf(
            "what is", "define", "meaning of", "definition of", "explain", "describe",
            "tell me about", "what does", "what are", "class", "interface", "enum"
        ),
        QueryIntentType.IMPLEMENTATION to listOf(
            "how to implement", "implementation of", "how is", "how does", "code for",
            "source code", "method", "function", "implement", "implementation"
        ),
        QueryIntentType.USAGE to listOf(
            "how to use", "usage of", "example of", "using", "usage", "example",
            "how can I use", "how do I use", "call", "invoke"
        ),
        QueryIntentType.INHERITANCE to listOf(
            "inherit", "inheritance", "extends", "subclass", "superclass", "parent class",
            "child class", "derived class", "base class", "hierarchy"
        ),
        QueryIntentType.DEPENDENCY to listOf(
            "depend", "dependency", "import", "require", "include", "library",
            "module", "package", "dependency graph", "depends on"
        ),
        QueryIntentType.ARCHITECTURE to listOf(
            "architecture", "design", "structure", "pattern", "organization", "layout",
            "system design", "component", "module", "layer"
        ),
        QueryIntentType.DOCUMENTATION to listOf(
            "documentation", "document", "comment", "javadoc", "kdoc", "doc",
            "readme", "guide", "manual", "help"
        ),
        QueryIntentType.EXAMPLE to listOf(
            "example", "sample", "demo", "illustration", "show me", "demonstrate",
            "how to", "tutorial", "guide", "walkthrough"
        ),
        QueryIntentType.DEBUGGING to listOf(
            "debug", "error", "exception", "bug", "issue", "problem", "fix",
            "troubleshoot", "diagnose", "resolve"
        ),
        QueryIntentType.REFACTORING to listOf(
            "refactor", "improve", "optimize", "clean", "restructure", "rewrite",
            "redesign", "better way", "best practice", "pattern"
        )
    ),
    val confidenceThreshold: Double = 0.6,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000
)

/**
 * 上下文选择器配置
 *
 * @property maxContextsPerLevel 每个级别的最大上下文数
 * @property levelWeights 级别权重
 * @property typeWeights 类型权重
 * @property intentTypeWeights 意图类型权重
 * @property minRelevanceScore 最小相关性分数
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 */
data class ContextSelectorConfig(
    val maxContextsPerLevel: Map<ContextLevel, Int> = mapOf(
        ContextLevel.GLOBAL to 1,
        ContextLevel.PROJECT to 2,
        ContextLevel.MODULE to 3,
        ContextLevel.PACKAGE to 3,
        ContextLevel.FILE to 5,
        ContextLevel.CLASS to 5,
        ContextLevel.METHOD to 3,
        ContextLevel.LOCAL to 2
    ),
    val levelWeights: Map<ContextLevel, Double> = mapOf(
        ContextLevel.GLOBAL to 0.1,
        ContextLevel.PROJECT to 0.2,
        ContextLevel.MODULE to 0.3,
        ContextLevel.PACKAGE to 0.4,
        ContextLevel.FILE to 0.6,
        ContextLevel.CLASS to 0.8,
        ContextLevel.METHOD to 1.0,
        ContextLevel.LOCAL to 0.9
    ),
    val typeWeights: Map<ContextType, Double> = mapOf(
        ContextType.CODE to 1.0,
        ContextType.DOCUMENTATION to 0.8,
        ContextType.COMMENT to 0.7,
        ContextType.QUERY to 0.6,
        ContextType.HISTORY to 0.5,
        ContextType.USER_FEEDBACK to 0.9,
        ContextType.CUSTOM to 0.5
    ),
    val intentTypeWeights: Map<QueryIntentType, Map<ContextLevel, Double>> = mapOf(
        QueryIntentType.DEFINITION to mapOf(
            ContextLevel.CLASS to 1.0,
            ContextLevel.PACKAGE to 0.8,
            ContextLevel.FILE to 0.7
        ),
        QueryIntentType.IMPLEMENTATION to mapOf(
            ContextLevel.METHOD to 1.0,
            ContextLevel.CLASS to 0.9,
            ContextLevel.FILE to 0.7
        ),
        QueryIntentType.USAGE to mapOf(
            ContextLevel.METHOD to 1.0,
            ContextLevel.CLASS to 0.8,
            ContextLevel.FILE to 0.6
        ),
        QueryIntentType.INHERITANCE to mapOf(
            ContextLevel.CLASS to 1.0,
            ContextLevel.PACKAGE to 0.7,
            ContextLevel.MODULE to 0.5
        ),
        QueryIntentType.DEPENDENCY to mapOf(
            ContextLevel.MODULE to 1.0,
            ContextLevel.PACKAGE to 0.9,
            ContextLevel.FILE to 0.7
        ),
        QueryIntentType.ARCHITECTURE to mapOf(
            ContextLevel.PROJECT to 1.0,
            ContextLevel.MODULE to 0.9,
            ContextLevel.PACKAGE to 0.7
        ),
        QueryIntentType.DOCUMENTATION to mapOf(
            ContextLevel.FILE to 1.0,
            ContextLevel.CLASS to 0.9,
            ContextLevel.METHOD to 0.8
        ),
        QueryIntentType.EXAMPLE to mapOf(
            ContextLevel.METHOD to 1.0,
            ContextLevel.CLASS to 0.8,
            ContextLevel.FILE to 0.7
        ),
        QueryIntentType.DEBUGGING to mapOf(
            ContextLevel.METHOD to 1.0,
            ContextLevel.LOCAL to 0.9,
            ContextLevel.CLASS to 0.7
        ),
        QueryIntentType.REFACTORING to mapOf(
            ContextLevel.CLASS to 1.0,
            ContextLevel.METHOD to 0.9,
            ContextLevel.FILE to 0.8
        ),
        QueryIntentType.GENERAL to mapOf(
            ContextLevel.FILE to 1.0,
            ContextLevel.CLASS to 0.9,
            ContextLevel.METHOD to 0.8,
            ContextLevel.PACKAGE to 0.7,
            ContextLevel.MODULE to 0.6,
            ContextLevel.PROJECT to 0.5,
            ContextLevel.GLOBAL to 0.4,
            ContextLevel.LOCAL to 0.3
        )
    ),
    val minRelevanceScore: Double = 0.3,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000
)

/**
 * 基于查询意图的上下文选择器
 *
 * @property contextHierarchy 上下文层次结构
 * @property embeddingService 嵌入服务
 * @property intentDetectorConfig 意图检测器配置
 * @property selectorConfig 选择器配置
 */
class IntentBasedContextSelector(
    private val contextHierarchy: ContextHierarchy,
    private val embeddingService: EmbeddingService,
    private val intentDetectorConfig: IntentDetectorConfig = IntentDetectorConfig(),
    private val selectorConfig: ContextSelectorConfig = ContextSelectorConfig()
) {
    // 意图缓存
    private val intentCache = ConcurrentHashMap<String, QueryIntent>()
    
    // 上下文选择缓存
    private val selectionCache = ConcurrentHashMap<String, ContextSelectionResult>()
    
    /**
     * 选择上下文
     *
     * @param retrievalContext 检索上下文
     * @return 上下文选择结果
     */
    suspend fun selectContexts(retrievalContext: RetrievalContext): ContextSelectionResult = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = generateCacheKey(retrievalContext)
            
            // 检查缓存
            if (selectorConfig.enableCaching) {
                val cachedResult = selectionCache[cacheKey]
                if (cachedResult != null) {
                    return@withContext cachedResult
                }
            }
            
            // 检测查询意图
            val intent = detectQueryIntent(retrievalContext.query)
            
            // 选择相关上下文
            val (selectedContexts, relevanceScores) = selectRelevantContexts(retrievalContext, intent)
            
            // 创建结果
            val result = ContextSelectionResult(
                selectedContexts = selectedContexts,
                relevanceScores = relevanceScores,
                intent = intent
            )
            
            // 缓存结果
            if (selectorConfig.enableCaching) {
                // 如果缓存已满，移除最早的条目
                if (selectionCache.size >= selectorConfig.cacheSize) {
                    val oldestKey = selectionCache.keys.firstOrNull()
                    if (oldestKey != null) {
                        selectionCache.remove(oldestKey)
                    }
                }
                
                selectionCache[cacheKey] = result
            }
            
            return@withContext result
        } catch (e: Exception) {
            logger.error(e) { "选择上下文失败: ${e.message}" }
            
            // 返回空结果
            return@withContext ContextSelectionResult(
                selectedContexts = emptyList(),
                relevanceScores = emptyMap(),
                intent = QueryIntent(
                    type = QueryIntentType.GENERAL,
                    confidence = 0.0
                )
            )
        }
    }
    
    /**
     * 检测查询意图
     *
     * @param query 查询文本
     * @return 查询意图
     */
    suspend fun detectQueryIntent(query: String): QueryIntent = withContext(Dispatchers.IO) {
        try {
            // 生成缓存键
            val cacheKey = query.lowercase().trim()
            
            // 检查缓存
            if (intentDetectorConfig.enableCaching) {
                val cachedIntent = intentCache[cacheKey]
                if (cachedIntent != null) {
                    return@withContext cachedIntent
                }
            }
            
            // 提取实体
            val entities = extractEntities(query)
            
            // 计算每种意图类型的分数
            val intentScores = mutableMapOf<QueryIntentType, Double>()
            
            // 基于模式匹配计算分数
            intentDetectorConfig.intentPatterns.forEach { (intentType, patterns) ->
                val queryLower = query.lowercase()
                val patternMatches = patterns.count { pattern ->
                    queryLower.contains(pattern.lowercase())
                }
                
                if (patternMatches > 0) {
                    val score = min(1.0, patternMatches.toDouble() / patterns.size)
                    intentScores[intentType] = score
                }
            }
            
            // 如果没有匹配的模式，使用通用意图
            if (intentScores.isEmpty()) {
                intentScores[QueryIntentType.GENERAL] = 0.5
            }
            
            // 选择得分最高的意图
            val (bestIntentType, bestScore) = intentScores.maxByOrNull { it.value }
                ?: (QueryIntentType.GENERAL to 0.5)
            
            // 创建查询意图
            val intent = QueryIntent(
                type = bestIntentType,
                confidence = bestScore,
                entities = entities
            )
            
            // 缓存意图
            if (intentDetectorConfig.enableCaching) {
                // 如果缓存已满，移除最早的条目
                if (intentCache.size >= intentDetectorConfig.cacheSize) {
                    val oldestKey = intentCache.keys.firstOrNull()
                    if (oldestKey != null) {
                        intentCache.remove(oldestKey)
                    }
                }
                
                intentCache[cacheKey] = intent
            }
            
            return@withContext intent
        } catch (e: Exception) {
            logger.error(e) { "检测查询意图失败: ${e.message}" }
            
            // 返回通用意图
            return@withContext QueryIntent(
                type = QueryIntentType.GENERAL,
                confidence = 0.0
            )
        }
    }
    
    /**
     * 提取实体
     *
     * @param query 查询文本
     * @return 实体映射
     */
    private fun extractEntities(query: String): Map<String, String> {
        val entities = mutableMapOf<String, String>()
        
        // 提取类名（大写字母开头的单词）
        val classNameRegex = Regex("\\b[A-Z][a-zA-Z0-9]*\\b")
        val classNames = classNameRegex.findAll(query).map { it.value }.toList()
        
        if (classNames.isNotEmpty()) {
            entities["class"] = classNames.joinToString(", ")
        }
        
        // 提取方法名（小写字母开头，后跟括号的单词）
        val methodNameRegex = Regex("\\b[a-z][a-zA-Z0-9]*\\(\\)")
        val methodNames = methodNameRegex.findAll(query).map { it.value }.toList()
        
        if (methodNames.isNotEmpty()) {
            entities["method"] = methodNames.joinToString(", ")
        }
        
        // 提取包名（点分隔的小写单词）
        val packageNameRegex = Regex("\\b[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)+\\b")
        val packageNames = packageNameRegex.findAll(query).map { it.value }.toList()
        
        if (packageNames.isNotEmpty()) {
            entities["package"] = packageNames.joinToString(", ")
        }
        
        return entities
    }
    
    /**
     * 选择相关上下文
     *
     * @param retrievalContext 检索上下文
     * @param intent 查询意图
     * @return 选择的上下文列表和相关性分数
     */
    private suspend fun selectRelevantContexts(
        retrievalContext: RetrievalContext,
        intent: QueryIntent
    ): Pair<List<ContextElement>, Map<String, Double>> = withContext(Dispatchers.IO) {
        // 获取所有上下文
        val allContexts = contextHierarchy.getAllContexts()
        
        if (allContexts.isEmpty()) {
            return@withContext Pair(emptyList(), emptyMap())
        }
        
        // 计算每个上下文的相关性分数
        val contextScores = mutableMapOf<String, Double>()
        
        for (context in allContexts) {
            // 计算基础相关性分数
            val baseScore = calculateBaseRelevanceScore(context, retrievalContext)
            
            // 应用级别权重
            val levelWeight = selectorConfig.levelWeights[context.level] ?: 0.5
            
            // 应用类型权重
            val typeWeight = selectorConfig.typeWeights[context.type] ?: 0.5
            
            // 应用意图类型权重
            val intentTypeWeight = selectorConfig.intentTypeWeights[intent.type]
                ?.get(context.level) ?: 0.5
            
            // 计算最终分数
            val finalScore = baseScore * levelWeight * typeWeight * intentTypeWeight
            
            // 存储分数
            contextScores[context.id] = finalScore
        }
        
        // 按级别分组上下文
        val contextsByLevel = allContexts.groupBy { it.level }
        
        // 选择每个级别的最相关上下文
        val selectedContexts = mutableListOf<ContextElement>()
        
        for ((level, contexts) in contextsByLevel) {
            // 获取该级别的最大上下文数
            val maxContexts = selectorConfig.maxContextsPerLevel[level] ?: 1
            
            // 按相关性分数排序并选择前 N 个
            val topContexts = contexts
                .sortedByDescending { contextScores[it.id] ?: 0.0 }
                .filter { (contextScores[it.id] ?: 0.0) >= selectorConfig.minRelevanceScore }
                .take(maxContexts)
            
            selectedContexts.addAll(topContexts)
        }
        
        // 提取选择的上下文的分数
        val relevanceScores = selectedContexts.associate { it.id to (contextScores[it.id] ?: 0.0) }
        
        return@withContext Pair(selectedContexts, relevanceScores)
    }
    
    /**
     * 计算基础相关性分数
     *
     * @param context 上下文元素
     * @param retrievalContext 检索上下文
     * @return 基础相关性分数
     */
    private suspend fun calculateBaseRelevanceScore(
        context: ContextElement,
        retrievalContext: RetrievalContext
    ): Double = withContext(Dispatchers.IO) {
        try {
            var score = 0.0
            
            // 1. 基于内容相似度的分数
            val contentSimilarity = calculateContentSimilarity(context.content, retrievalContext.query)
            score += 0.5 * contentSimilarity
            
            // 2. 基于当前文件的分数
            if (retrievalContext.currentFile != null && context.path != null) {
                val isCurrentFile = context.path.toString().endsWith(retrievalContext.currentFile)
                if (isCurrentFile) {
                    score += 0.3
                }
            }
            
            // 3. 基于选中文本的分数
            if (!retrievalContext.selectedText.isNullOrEmpty()) {
                val containsSelectedText = context.content.contains(retrievalContext.selectedText)
                if (containsSelectedText) {
                    score += 0.2
                }
            }
            
            // 4. 基于历史查询的分数
            if (retrievalContext.previousQueries.isNotEmpty()) {
                val lastQuery = retrievalContext.previousQueries.last()
                val queryContextSimilarity = calculateContentSimilarity(context.content, lastQuery)
                score += 0.1 * queryContextSimilarity
            }
            
            // 确保分数在 0-1 范围内
            return@withContext min(1.0, max(0.0, score))
        } catch (e: Exception) {
            logger.error(e) { "计算基础相关性分数失败: ${e.message}" }
            return@withContext 0.0
        }
    }
    
    /**
     * 计算内容相似度
     *
     * @param content1 内容 1
     * @param content2 内容 2
     * @return 内容相似度
     */
    private suspend fun calculateContentSimilarity(
        content1: String,
        content2: String
    ): Double = withContext(Dispatchers.IO) {
        try {
            // 生成嵌入向量
            val embedding1 = embeddingService.generateEmbedding(
                text = content1,
                modelName = intentDetectorConfig.embeddingModelName
            )
            
            val embedding2 = embeddingService.generateEmbedding(
                text = content2,
                modelName = intentDetectorConfig.embeddingModelName
            )
            
            // 计算余弦相似度
            return@withContext calculateCosineSimilarity(embedding1, embedding2)
        } catch (e: Exception) {
            logger.error(e) { "计算内容相似度失败: ${e.message}" }
            
            // 回退到简单的词袋相似度
            val words1 = content1.lowercase().split(Regex("\\s+")).toSet()
            val words2 = content2.lowercase().split(Regex("\\s+")).toSet()
            
            val intersection = words1.intersect(words2).size
            val union = words1.union(words2).size
            
            return@withContext if (union > 0) intersection.toDouble() / union else 0.0
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
     * @param retrievalContext 检索上下文
     * @return 缓存键
     */
    private fun generateCacheKey(retrievalContext: RetrievalContext): String {
        val sb = StringBuilder()
        
        sb.append(retrievalContext.query)
        
        if (retrievalContext.currentFile != null) {
            sb.append(":${retrievalContext.currentFile}")
        }
        
        if (!retrievalContext.selectedText.isNullOrEmpty()) {
            sb.append(":${retrievalContext.selectedText}")
        }
        
        if (retrievalContext.previousQueries.isNotEmpty()) {
            sb.append(":${retrievalContext.previousQueries.last()}")
        }
        
        return sb.toString()
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        intentCache.clear()
        selectionCache.clear()
    }
}
