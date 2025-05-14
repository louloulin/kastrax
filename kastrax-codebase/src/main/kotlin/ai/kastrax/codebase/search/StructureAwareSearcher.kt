package ai.kastrax.codebase.search

import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 结构感知搜索器配置
 *
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 * @property defaultLimit 默认限制数量
 * @property defaultMinScore 默认最小分数
 * @property relationBoostFactor 关系提升因子
 * @property parentBoostFactor 父元素提升因子
 * @property childBoostFactor 子元素提升因子
 * @property siblingBoostFactor 兄弟元素提升因子
 * @property usageBoostFactor 使用关系提升因子
 * @property implementationBoostFactor 实现关系提升因子
 * @property overrideBoostFactor 重写关系提升因子
 */
data class StructureAwareSearcherConfig(
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100,
    val defaultLimit: Int = 20,
    val defaultMinScore: Float = 0.5f,
    val relationBoostFactor: Float = 1.2f,
    val parentBoostFactor: Float = 1.3f,
    val childBoostFactor: Float = 1.1f,
    val siblingBoostFactor: Float = 1.05f,
    val usageBoostFactor: Float = 1.2f,
    val implementationBoostFactor: Float = 1.3f,
    val overrideBoostFactor: Float = 1.2f
)

/**
 * 结构感知搜索结果
 *
 * @property element 代码元素
 * @property score 分数
 * @property relatedElements 相关元素
 * @property relationTypes 关系类型
 */
data class StructureAwareSearchResult(
    val element: CodeElement,
    val score: Float,
    val relatedElements: List<CodeElement> = emptyList(),
    val relationTypes: Map<String, String> = emptyMap()
)

/**
 * 结构感知搜索器
 *
 * 基于代码结构的搜索器，考虑代码元素之间的关系
 *
 * @property codeIndexer 代码索引器
 * @property config 配置
 */
class StructureAwareSearcher(
    private val codeIndexer: CodeIndexer,
    private val config: StructureAwareSearcherConfig = StructureAwareSearcherConfig()
) {
    // 缓存
    private val cache = ConcurrentHashMap<String, List<StructureAwareSearchResult>>()

    /**
     * 搜索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @param types 元素类型
     * @return 搜索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = config.defaultLimit,
        minScore: Float = config.defaultMinScore,
        types: Set<CodeElementType>? = null
    ): List<StructureAwareSearchResult> = withContext(Dispatchers.Default) {
        logger.info { "开始结构感知搜索: $query" }

        // 检查缓存
        val cacheKey = "$query:$limit:$minScore:${types?.joinToString(",")}"
        if (config.enableCaching && cache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取搜索结果: $query" }
            return@withContext cache[cacheKey]!!
        }

        try {
            // 获取所有代码元素
            val allElements = codeIndexer.getAllElements()
            logger.debug { "搜索范围: ${allElements.size} 个代码元素" }

            // 过滤元素类型
            val filteredElements = if (types != null && types.isNotEmpty()) {
                allElements.filter { it.type in types }
            } else {
                allElements
            }

            // 计算每个元素的初始分数
            val initialScores = calculateInitialScores(filteredElements.toList(), query)

            // 应用结构感知提升
            val boostedScores = applyStructureBoost(initialScores)

            // 创建搜索结果
            val results = boostedScores.map { (element, score) ->
                val relatedElements = findRelatedElements(element)
                val relationTypes = mapRelationTypes(element, relatedElements)
                StructureAwareSearchResult(element, score, relatedElements, relationTypes)
            }

            // 过滤和排序结果
            val filteredResults = results
                .filter { it.score >= minScore }
                .sortedByDescending { it.score }
                .take(limit)

            logger.info { "结构感知搜索完成: $query, 找到 ${filteredResults.size} 个结果" }

            // 缓存结果
            if (config.enableCaching) {
                // 维护缓存大小
                if (cache.size >= config.maxCacheSize) {
                    // 简单的缓存淘汰策略：随机移除一个
                    val keyToRemove = cache.keys.firstOrNull()
                    if (keyToRemove != null) {
                        cache.remove(keyToRemove)
                    }
                }
                cache[cacheKey] = filteredResults
            }

            return@withContext filteredResults
        } catch (e: Exception) {
            logger.error(e) { "结构感知搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 计算初始分数
     *
     * @param elements 元素列表
     * @param query 查询
     * @return 元素到分数的映射
     */
    private fun calculateInitialScores(elements: List<CodeElement>, query: String): Map<CodeElement, Float> {
        val queryTerms = query.lowercase().split(Regex("\\s+"))
            .filter { it.length > 1 } // 过滤掉太短的词

        if (queryTerms.isEmpty()) {
            return emptyMap()
        }

        return elements.associateWith { element ->
            var score = 0f

            // 名称匹配
            val nameScore = calculateTermMatchScore(element.name.lowercase(), queryTerms)
            score += nameScore * 2.0f

            // 限定名匹配
            val qualifiedNameScore = calculateTermMatchScore(element.qualifiedName.lowercase(), queryTerms)
            score += qualifiedNameScore * 1.5f

            // 文档匹配
            if (element.documentation.isNotEmpty()) {
                val docScore = calculateTermMatchScore(element.documentation.lowercase(), queryTerms)
                score += docScore * 1.2f
            }

            // 根据元素类型调整分数
            score *= getElementTypeBoost(element.type)

            score
        }
    }

    /**
     * 应用结构提升
     *
     * @param initialScores 初始分数
     * @return 提升后的分数
     */
    private fun applyStructureBoost(initialScores: Map<CodeElement, Float>): Map<CodeElement, Float> {
        val boostedScores = initialScores.toMutableMap()

        // 对每个元素应用结构提升
        for ((element, score) in initialScores) {
            // 提升父元素分数
            element.parent?.let { parent ->
                val parentScore = boostedScores[parent] ?: 0f
                if (parentScore > 0) {
                    boostedScores[parent] = parentScore * config.parentBoostFactor
                }
            }

            // 提升子元素分数
            for (child in element.children) {
                val childScore = boostedScores[child] ?: 0f
                if (childScore > 0) {
                    boostedScores[child] = childScore * config.childBoostFactor
                }
            }

            // 提升兄弟元素分数
            element.parent?.children?.filter { it != element }?.forEach { sibling ->
                val siblingScore = boostedScores[sibling] ?: 0f
                if (siblingScore > 0) {
                    boostedScores[sibling] = siblingScore * config.siblingBoostFactor
                }
            }

            // 提升使用关系元素分数
            element.metadata["usedBy"]?.let { usedBy ->
                if (usedBy is List<*>) {
                    usedBy.filterIsInstance<CodeElement>().forEach { user ->
                        val userScore = boostedScores[user] ?: 0f
                        if (userScore > 0) {
                            boostedScores[user] = userScore * config.usageBoostFactor
                        }
                    }
                }
            }

            // 提升实现关系元素分数
            element.metadata["implements"]?.let { implements ->
                if (implements is List<*>) {
                    implements.filterIsInstance<CodeElement>().forEach { impl ->
                        val implScore = boostedScores[impl] ?: 0f
                        if (implScore > 0) {
                            boostedScores[impl] = implScore * config.implementationBoostFactor
                        }
                    }
                }
            }

            // 提升重写关系元素分数
            element.metadata["overrides"]?.let { overrides ->
                if (overrides is List<*>) {
                    overrides.filterIsInstance<CodeElement>().forEach { over ->
                        val overScore = boostedScores[over] ?: 0f
                        if (overScore > 0) {
                            boostedScores[over] = overScore * config.overrideBoostFactor
                        }
                    }
                }
            }
        }

        return boostedScores
    }

    /**
     * 查找相关元素
     *
     * @param element 代码元素
     * @return 相关元素列表
     */
    private fun findRelatedElements(element: CodeElement): List<CodeElement> {
        val relatedElements = mutableListOf<CodeElement>()

        // 添加父元素
        element.parent?.let { relatedElements.add(it) }

        // 添加子元素
        relatedElements.addAll(element.children)

        // 添加使用关系元素
        element.metadata["usedBy"]?.let { usedBy ->
            if (usedBy is List<*>) {
                relatedElements.addAll(usedBy.filterIsInstance<CodeElement>())
            }
        }

        // 添加实现关系元素
        element.metadata["implements"]?.let { implements ->
            if (implements is List<*>) {
                relatedElements.addAll(implements.filterIsInstance<CodeElement>())
            }
        }

        // 添加重写关系元素
        element.metadata["overrides"]?.let { overrides ->
            if (overrides is List<*>) {
                relatedElements.addAll(overrides.filterIsInstance<CodeElement>())
            }
        }

        return relatedElements.distinct()
    }

    /**
     * 映射关系类型
     *
     * @param element 代码元素
     * @param relatedElements 相关元素列表
     * @return 关系类型映射
     */
    private fun mapRelationTypes(element: CodeElement, relatedElements: List<CodeElement>): Map<String, String> {
        val relationTypes = mutableMapOf<String, String>()

        for (related in relatedElements) {
            val relationType = when {
                related == element.parent -> "parent"
                element.children.contains(related) -> "child"
                element.parent?.children?.contains(related) == true -> "sibling"
                element.metadata["usedBy"]?.let { usedBy ->
                    if (usedBy is List<*>) {
                        usedBy.filterIsInstance<CodeElement>().contains(related)
                    } else false
                } == true -> "usedBy"
                element.metadata["implements"]?.let { implements ->
                    if (implements is List<*>) {
                        implements.filterIsInstance<CodeElement>().contains(related)
                    } else false
                } == true -> "implements"
                element.metadata["overrides"]?.let { overrides ->
                    if (overrides is List<*>) {
                        overrides.filterIsInstance<CodeElement>().contains(related)
                    } else false
                } == true -> "overrides"
                else -> "related"
            }

            relationTypes[related.id] = relationType
        }

        return relationTypes
    }

    /**
     * 计算词匹配分数
     *
     * @param text 文本
     * @param queryTerms 查询词列表
     * @return 分数
     */
    private fun calculateTermMatchScore(text: String, queryTerms: List<String>): Float {
        if (text.isEmpty() || queryTerms.isEmpty()) {
            return 0f
        }

        var score = 0f
        val textLower = text.lowercase()

        for (term in queryTerms) {
            // 精确匹配
            if (textLower == term) {
                score += 1.0f
                continue
            }

            // 单词边界匹配
            val wordBoundaryPattern = Regex("\\b$term\\b")
            if (wordBoundaryPattern.containsMatchIn(textLower)) {
                score += 0.8f
                continue
            }

            // 前缀匹配
            if (textLower.startsWith(term)) {
                score += 0.6f
                continue
            }

            // 子串匹配
            if (textLower.contains(term)) {
                score += 0.4f
                continue
            }

            // 模糊匹配（简单实现）
            if (textLower.length >= 4 && term.length >= 3) {
                val threshold = 0.7f // 相似度阈值
                val similarity = calculateJaccardSimilarity(textLower, term)
                if (similarity >= threshold) {
                    score += 0.2f * similarity
                }
            }
        }

        // 归一化分数
        return score / queryTerms.size
    }

    /**
     * 计算Jaccard相似度
     *
     * @param s1 字符串1
     * @param s2 字符串2
     * @return 相似度
     */
    private fun calculateJaccardSimilarity(s1: String, s2: String): Float {
        val set1 = s1.windowed(2).toSet()
        val set2 = s2.windowed(2).toSet()

        val intersection = set1.intersect(set2)
        val union = set1.union(set2)

        return if (union.isEmpty()) 0f else intersection.size.toFloat() / union.size
    }

    /**
     * 获取元素类型提升因子
     *
     * @param type 元素类型
     * @return 提升因子
     */
    private fun getElementTypeBoost(type: CodeElementType): Float {
        return when (type) {
            CodeElementType.CLASS -> 1.2f
            CodeElementType.INTERFACE -> 1.2f
            CodeElementType.METHOD -> 1.1f
            CodeElementType.FUNCTION -> 1.1f
            CodeElementType.FIELD -> 1.0f
            CodeElementType.PROPERTY -> 1.0f
            CodeElementType.ENUM -> 1.0f
            CodeElementType.ANNOTATION -> 1.0f
            CodeElementType.CONSTRUCTOR -> 1.0f
            CodeElementType.PARAMETER -> 0.8f
            CodeElementType.FILE -> 0.7f
            else -> 1.0f
        }
    }

    /**
     * 转换为检索结果
     *
     * @param result 结构感知搜索结果
     * @return 检索结果
     */
    fun convertToRetrievalResult(result: StructureAwareSearchResult): RetrievalResult {
        return RetrievalResult(
            element = result.element,
            score = result.score.toDouble(),
            explanation = "结构感知搜索结果，分数: ${result.score}，相关元素: ${result.relatedElements.size}"
        )
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        logger.info { "结构感知搜索器缓存已清除" }
    }
}
