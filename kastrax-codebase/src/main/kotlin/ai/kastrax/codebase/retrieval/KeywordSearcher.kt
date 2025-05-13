package ai.kastrax.codebase.retrieval

import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 关键词搜索器配置
 *
 * @property enableCaching 是否启用缓存
 * @property maxCacheSize 最大缓存大小
 * @property defaultLimit 默认限制数量
 * @property defaultMinScore 默认最小分数
 * @property searchInNames 是否在名称中搜索
 * @property searchInQualifiedNames 是否在限定名中搜索
 * @property searchInDocumentation 是否在文档中搜索
 * @property searchInContent 是否在内容中搜索
 * @property boostFactorForName 名称的提升因子
 * @property boostFactorForQualifiedName 限定名的提升因子
 * @property boostFactorForDocumentation 文档的提升因子
 * @property boostFactorForContent 内容的提升因子
 */
data class KeywordSearcherConfig(
    val enableCaching: Boolean = true,
    val maxCacheSize: Int = 100,
    val defaultLimit: Int = 20,
    val defaultMinScore: Float = 0.5f,
    val searchInNames: Boolean = true,
    val searchInQualifiedNames: Boolean = true,
    val searchInDocumentation: Boolean = true,
    val searchInContent: Boolean = true,
    val boostFactorForName: Float = 2.0f,
    val boostFactorForQualifiedName: Float = 1.5f,
    val boostFactorForDocumentation: Float = 1.2f,
    val boostFactorForContent: Float = 1.0f
)

/**
 * 关键词搜索结果
 *
 * @property element 代码元素
 * @property score 分数
 * @property highlights 高亮信息
 */
data class KeywordSearchResult(
    val element: CodeElement,
    val score: Float,
    val highlights: Map<String, List<String>> = emptyMap()
)

/**
 * 关键词搜索器
 *
 * 基于关键词的代码搜索器
 *
 * @property codeIndexer 代码索引器
 * @property config 配置
 */
class KeywordSearcher(
    private val codeIndexer: CodeIndexer,
    private val config: KeywordSearcherConfig = KeywordSearcherConfig()
) {
    // 缓存
    private val cache = ConcurrentHashMap<String, List<KeywordSearchResult>>()

    /**
     * 搜索
     *
     * @param query 查询
     * @param limit 限制数量
     * @param minScore 最小分数
     * @return 搜索结果列表
     */
    suspend fun search(
        query: String,
        limit: Int = config.defaultLimit,
        minScore: Float = config.defaultMinScore
    ): List<KeywordSearchResult> = withContext(Dispatchers.Default) {
        logger.info { "开始关键词搜索: $query" }

        // 检查缓存
        val cacheKey = "$query:$limit:$minScore"
        if (config.enableCaching && cache.containsKey(cacheKey)) {
            logger.debug { "从缓存中获取搜索结果: $query" }
            return@withContext cache[cacheKey]!!
        }

        try {
            // 预处理查询
            val processedQuery = preprocessQuery(query)
            val queryTerms = processedQuery.split(Regex("\\s+"))
                .filter { it.length > 1 } // 过滤掉太短的词

            if (queryTerms.isEmpty()) {
                logger.warn { "查询词为空: $query" }
                return@withContext emptyList()
            }

            // 获取所有代码元素
            val allElements = codeIndexer.getAllElements()
            logger.debug { "搜索范围: ${allElements.size} 个代码元素" }

            // 计算每个元素的分数
            val scoredElements = allElements.map { element ->
                val score = calculateScore(element, queryTerms)
                val highlights = if (score >= minScore) {
                    generateHighlights(element, queryTerms)
                } else {
                    emptyMap()
                }
                KeywordSearchResult(element, score, highlights)
            }

            // 过滤和排序结果
            val results = scoredElements
                .filter { it.score >= minScore }
                .sortedByDescending { it.score }
                .take(limit)

            logger.info { "关键词搜索完成: $query, 找到 ${results.size} 个结果" }

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
                cache[cacheKey] = results
            }

            return@withContext results
        } catch (e: Exception) {
            logger.error(e) { "关键词搜索时发生错误: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 预处理查询
     *
     * @param query 查询
     * @return 预处理后的查询
     */
    private fun preprocessQuery(query: String): String {
        // 转换为小写
        var processedQuery = query.lowercase()

        // 移除特殊字符
        processedQuery = processedQuery.replace(Regex("[^a-z0-9_\\s]"), " ")

        // 规范化空白
        processedQuery = processedQuery.replace(Regex("\\s+"), " ").trim()

        return processedQuery
    }

    /**
     * 计算分数
     *
     * @param element 代码元素
     * @param queryTerms 查询词列表
     * @return 分数
     */
    private fun calculateScore(element: CodeElement, queryTerms: List<String>): Float {
        var score = 0f

        // 在名称中搜索
        if (config.searchInNames) {
            val nameScore = calculateTermMatchScore(element.name.lowercase(), queryTerms)
            score += nameScore * config.boostFactorForName
        }

        // 在限定名中搜索
        if (config.searchInQualifiedNames) {
            val qualifiedNameScore = calculateTermMatchScore(element.qualifiedName.lowercase(), queryTerms)
            score += qualifiedNameScore * config.boostFactorForQualifiedName
        }

        // 在文档中搜索
        if (config.searchInDocumentation && element.documentation.isNotEmpty()) {
            val docScore = calculateTermMatchScore(element.documentation.lowercase(), queryTerms)
            score += docScore * config.boostFactorForDocumentation
        }

        // 在内容中搜索
        if (config.searchInContent) {
            // 获取元素内容
            val content = getElementContent(element).lowercase()
            val contentScore = calculateTermMatchScore(content, queryTerms)
            score += contentScore * config.boostFactorForContent
        }

        // 根据元素类型调整分数
        score *= getElementTypeBoost(element.type)

        return score
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
     * 获取元素内容
     *
     * @param element 代码元素
     * @return 内容
     */
    private fun getElementContent(element: CodeElement): String {
        val sb = StringBuilder()

        // 添加名称和限定名
        sb.append(element.name).append(" ")
        sb.append(element.qualifiedName).append(" ")

        // 添加文档
        if (element.documentation.isNotEmpty()) {
            sb.append(element.documentation).append(" ")
        }

        // 添加元数据
        element.metadata.forEach { (key, value) ->
            sb.append("$key: $value ").append(" ")
        }

        // 添加子元素信息
        element.children.forEach { child ->
            sb.append(child.name).append(" ")
            if (child.documentation.isNotEmpty()) {
                sb.append(child.documentation).append(" ")
            }
        }

        return sb.toString()
    }

    /**
     * 生成高亮信息
     *
     * @param element 代码元素
     * @param queryTerms 查询词列表
     * @return 高亮信息
     */
    private fun generateHighlights(element: CodeElement, queryTerms: List<String>): Map<String, List<String>> {
        val highlights = mutableMapOf<String, MutableList<String>>()

        // 高亮名称
        if (config.searchInNames) {
            val nameHighlights = findHighlights(element.name, queryTerms)
            if (nameHighlights.isNotEmpty()) {
                highlights["name"] = nameHighlights.toMutableList()
            }
        }

        // 高亮限定名
        if (config.searchInQualifiedNames) {
            val qualifiedNameHighlights = findHighlights(element.qualifiedName, queryTerms)
            if (qualifiedNameHighlights.isNotEmpty()) {
                highlights["qualifiedName"] = qualifiedNameHighlights.toMutableList()
            }
        }

        // 高亮文档
        if (config.searchInDocumentation && element.documentation.isNotEmpty()) {
            val docHighlights = findHighlights(element.documentation, queryTerms)
            if (docHighlights.isNotEmpty()) {
                highlights["documentation"] = docHighlights.toMutableList()
            }
        }

        return highlights
    }

    /**
     * 查找高亮
     *
     * @param text 文本
     * @param queryTerms 查询词列表
     * @return 高亮列表
     */
    private fun findHighlights(text: String, queryTerms: List<String>): List<String> {
        if (text.isEmpty() || queryTerms.isEmpty()) {
            return emptyList()
        }

        val highlights = mutableListOf<String>()
        val textLower = text.lowercase()

        for (term in queryTerms) {
            // 查找匹配
            val index = textLower.indexOf(term)
            if (index >= 0) {
                // 提取上下文
                val contextStart = (index - 20).coerceAtLeast(0)
                val contextEnd = (index + term.length + 20).coerceAtMost(text.length)
                val context = text.substring(contextStart, contextEnd)

                // 添加省略号
                val prefix = if (contextStart > 0) "..." else ""
                val suffix = if (contextEnd < text.length) "..." else ""

                highlights.add("$prefix$context$suffix")
            }
        }

        return highlights
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        cache.clear()
        logger.info { "关键词搜索器缓存已清除" }
    }
}
