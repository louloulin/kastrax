package ai.kastrax.codebase.semantic.memory

import ai.kastrax.store.embedding.EmbeddingService
import java.nio.file.Path
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.symbol.model.SymbolNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 语义相似度计算器配置
 *
 * @property minSimilarityScore 最小相似度分数
 * @property maxSimilarMemoriesPerElement 每个元素的最大相似记忆数
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 * @property maxConcurrentTasks 最大并发任务数
 */
data class SemanticSimilarityCalculatorConfig(
    val minSimilarityScore: Double = 0.7,
    val maxSimilarMemoriesPerElement: Int = 5,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000,
    val maxConcurrentTasks: Int = 10
)

/**
 * 相似度结果
 *
 * @property element1 元素1
 * @property element2 元素2
 * @property score 相似度分数
 */
data class SimilarityResult(
    val element1: Any,
    val element2: Any,
    val score: Double
)

/**
 * 语义相似度计算器
 *
 * 计算代码元素和符号节点之间的语义相似度
 *
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
class SemanticSimilarityCalculator(
    private val embeddingService: EmbeddingService,
    private val config: SemanticSimilarityCalculatorConfig = SemanticSimilarityCalculatorConfig()
) {
    // 元素向量缓存
    private val elementVectorCache = ConcurrentHashMap<String, FloatArray>()

    // 符号向量缓存
    private val symbolVectorCache = ConcurrentHashMap<String, FloatArray>()

    // 相似度缓存
    private val similarityCache = ConcurrentHashMap<String, Double>()

    /**
     * 计算代码元素之间的相似度
     *
     * @param element1 元素1
     * @param element2 元素2
     * @return 相似度分数
     */
    suspend fun calculateSimilarity(element1: CodeElement, element2: CodeElement): Double = withContext(Dispatchers.Default) {
        try {
            // 生成缓存键
            val cacheKey = "${element1.id}:${element2.id}"
            val reverseCacheKey = "${element2.id}:${element1.id}"

            // 检查缓存
            if (config.enableCaching) {
                similarityCache[cacheKey]?.let { return@withContext it }
                similarityCache[reverseCacheKey]?.let { return@withContext it }
            }

            // 获取元素向量
            val vector1 = getElementVector(element1)
            val vector2 = getElementVector(element2)

            // 计算余弦相似度
            val similarity = cosineSimilarity(vector1, vector2)

            // 缓存结果
            if (config.enableCaching) {
                // 如果缓存已满，移除一些条目
                if (similarityCache.size >= config.cacheSize) {
                    val keysToRemove = similarityCache.keys.take(config.cacheSize / 10)
                    keysToRemove.forEach { similarityCache.remove(it) }
                }

                similarityCache[cacheKey] = similarity
            }

            return@withContext similarity
        } catch (e: Exception) {
            logger.error(e) { "计算代码元素相似度时出错: ${element1.id}, ${element2.id}, ${e.message}" }
            return@withContext 0.0
        }
    }

    /**
     * 计算符号节点之间的相似度
     *
     * @param symbol1 符号1
     * @param symbol2 符号2
     * @return 相似度分数
     */
    suspend fun calculateSimilarity(symbol1: SymbolNode, symbol2: SymbolNode): Double = withContext(Dispatchers.Default) {
        try {
            // 生成缓存键
            val cacheKey = "sym:${symbol1.id}:${symbol2.id}"
            val reverseCacheKey = "sym:${symbol2.id}:${symbol1.id}"

            // 检查缓存
            if (config.enableCaching) {
                similarityCache[cacheKey]?.let { return@withContext it }
                similarityCache[reverseCacheKey]?.let { return@withContext it }
            }

            // 获取符号向量
            val vector1 = getSymbolVector(symbol1)
            val vector2 = getSymbolVector(symbol2)

            // 计算余弦相似度
            val similarity = cosineSimilarity(vector1, vector2)

            // 缓存结果
            if (config.enableCaching) {
                // 如果缓存已满，移除一些条目
                if (similarityCache.size >= config.cacheSize) {
                    val keysToRemove = similarityCache.keys.take(config.cacheSize / 10)
                    keysToRemove.forEach { similarityCache.remove(it) }
                }

                similarityCache[cacheKey] = similarity
            }

            return@withContext similarity
        } catch (e: Exception) {
            logger.error(e) { "计算符号节点相似度时出错: ${symbol1.id}, ${symbol2.id}, ${e.message}" }
            return@withContext 0.0
        }
    }

    /**
     * 计算代码元素和符号节点之间的相似度
     *
     * @param element 代码元素
     * @param symbol 符号节点
     * @return 相似度分数
     */
    suspend fun calculateSimilarity(element: CodeElement, symbol: SymbolNode): Double = withContext(Dispatchers.Default) {
        try {
            // 生成缓存键
            val cacheKey = "mix:${element.id}:${symbol.id}"
            val reverseCacheKey = "mix:${symbol.id}:${element.id}"

            // 检查缓存
            if (config.enableCaching) {
                similarityCache[cacheKey]?.let { return@withContext it }
                similarityCache[reverseCacheKey]?.let { return@withContext it }
            }

            // 获取向量
            val vector1 = getElementVector(element)
            val vector2 = getSymbolVector(symbol)

            // 计算余弦相似度
            val similarity = cosineSimilarity(vector1, vector2)

            // 缓存结果
            if (config.enableCaching) {
                // 如果缓存已满，移除一些条目
                if (similarityCache.size >= config.cacheSize) {
                    val keysToRemove = similarityCache.keys.take(config.cacheSize / 10)
                    keysToRemove.forEach { similarityCache.remove(it) }
                }

                similarityCache[cacheKey] = similarity
            }

            return@withContext similarity
        } catch (e: Exception) {
            logger.error(e) { "计算代码元素和符号节点相似度时出错: ${element.id}, ${symbol.id}, ${e.message}" }
            return@withContext 0.0
        }
    }

    /**
     * 查找相似代码元素
     *
     * @param element 代码元素
     * @param candidates 候选元素列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    suspend fun findSimilarElements(
        element: CodeElement,
        candidates: List<CodeElement>,
        limit: Int = config.maxSimilarMemoriesPerElement,
        minScore: Double = config.minSimilarityScore
    ): List<SimilarityResult> = withContext(Dispatchers.Default) {
        try {
            // 获取元素向量
            val elementVector = getElementVector(element)

            // 并行计算相似度
            val results = coroutineScope {
                candidates.chunked(config.maxConcurrentTasks).flatMap { chunk ->
                    chunk.map { candidate ->
                        async {
                            // 跳过自身
                            if (candidate.id == element.id) {
                                return@async null
                            }

                            // 获取候选元素向量
                            val candidateVector = getElementVector(candidate)

                            // 计算相似度
                            val similarity = cosineSimilarity(elementVector, candidateVector)

                            // 如果相似度低于阈值，跳过
                            if (similarity < minScore) {
                                return@async null
                            }

                            SimilarityResult(element, candidate, similarity)
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            // 排序并限制结果数量
            return@withContext results
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "查找相似代码元素时出错: ${element.id}, ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 查找相似符号节点
     *
     * @param symbol 符号节点
     * @param candidates 候选符号列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    suspend fun findSimilarSymbols(
        symbol: SymbolNode,
        candidates: List<SymbolNode>,
        limit: Int = config.maxSimilarMemoriesPerElement,
        minScore: Double = config.minSimilarityScore
    ): List<SimilarityResult> = withContext(Dispatchers.Default) {
        try {
            // 获取符号向量
            val symbolVector = getSymbolVector(symbol)

            // 并行计算相似度
            val results = coroutineScope {
                candidates.chunked(config.maxConcurrentTasks).flatMap { chunk ->
                    chunk.map { candidate ->
                        async {
                            // 跳过自身
                            if (candidate.id == symbol.id) {
                                return@async null
                            }

                            // 获取候选符号向量
                            val candidateVector = getSymbolVector(candidate)

                            // 计算相似度
                            val similarity = cosineSimilarity(symbolVector, candidateVector)

                            // 如果相似度低于阈值，跳过
                            if (similarity < minScore) {
                                return@async null
                            }

                            SimilarityResult(symbol, candidate, similarity)
                        }
                    }.awaitAll().filterNotNull()
                }
            }

            // 排序并限制结果数量
            return@withContext results
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            logger.error(e) { "查找相似符号节点时出错: ${symbol.id}, ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 生成相似度记忆
     *
     * @param similarityResults 相似度结果列表
     * @return 语义记忆列表
     */
    fun generateSimilarityMemories(similarityResults: List<SimilarityResult>): List<SemanticMemory> {
        val memories = mutableListOf<SemanticMemory>()

        similarityResults.forEach { result ->
            val (element1, element2, score) = result

            // 根据元素类型生成不同的记忆
            val memory = when {
                element1 is CodeElement && element2 is CodeElement -> {
                    generateCodeElementSimilarityMemory(element1, element2, score)
                }
                element1 is SymbolNode && element2 is SymbolNode -> {
                    generateSymbolSimilarityMemory(element1, element2, score)
                }
                element1 is CodeElement && element2 is SymbolNode -> {
                    generateMixedSimilarityMemory(element1, element2, score)
                }
                element1 is SymbolNode && element2 is CodeElement -> {
                    generateMixedSimilarityMemory(element2, element1, score)
                }
                else -> null
            }

            memory?.let { memories.add(it) }
        }

        return memories
    }

    /**
     * 生成代码元素相似度记忆
     *
     * @param element1 元素1
     * @param element2 元素2
     * @param score 相似度分数
     * @return 语义记忆
     */
    private fun generateCodeElementSimilarityMemory(
        element1: CodeElement,
        element2: CodeElement,
        score: Double
    ): SemanticMemory {
        val content = buildString {
            append("代码元素相似度: ${element1.name} 和 ${element2.name}")
            append("\n- 类型: ${element1.type.name.lowercase()} 和 ${element2.type.name.lowercase()}")
            append("\n- 相似度分数: ${String.format("%.2f", score)}")

            // 添加一些相似性描述
            if (element1.type == element2.type) {
                append("\n- 两者都是 ${element1.type.name.lowercase()} 类型")
            }

            // 添加位置信息
            append("\n- 位置: ${element1.location.filePath.toString().substringAfterLast('/')} 和 ${element2.location.filePath.toString().substringAfterLast('/')}")
        }

        val importance = when {
            score >= 0.9 -> ImportanceLevel.HIGH
            score >= 0.8 -> ImportanceLevel.MEDIUM
            else -> ImportanceLevel.LOW
        }

        return SemanticMemory(
            type = MemoryType.SEMANTIC_SIMILARITY,
            content = content,
            sourceElements = listOf(element1, element2),
            importance = importance,
            metadata = mutableMapOf(
                "similarityScore" to score,
                "element1Type" to element1.type.name,
                "element2Type" to element2.type.name
            )
        )
    }

    /**
     * 生成符号相似度记忆
     *
     * @param symbol1 符号1
     * @param symbol2 符号2
     * @param score 相似度分数
     * @return 语义记忆
     */
    private fun generateSymbolSimilarityMemory(
        symbol1: SymbolNode,
        symbol2: SymbolNode,
        score: Double
    ): SemanticMemory {
        val content = buildString {
            append("符号相似度: ${symbol1.name} 和 ${symbol2.name}")
            append("\n- 类型: ${symbol1.type.name.lowercase()} 和 ${symbol2.type.name.lowercase()}")
            append("\n- 相似度分数: ${String.format("%.2f", score)}")

            // 添加一些相似性描述
            if (symbol1.type == symbol2.type) {
                append("\n- 两者都是 ${symbol1.type.name.lowercase()} 类型")
            }

            // 添加限定名
            append("\n- 限定名: ${symbol1.qualifiedName} 和 ${symbol2.qualifiedName}")
        }

        val importance = when {
            score >= 0.9 -> ImportanceLevel.HIGH
            score >= 0.8 -> ImportanceLevel.MEDIUM
            else -> ImportanceLevel.LOW
        }

        return SemanticMemory(
            type = MemoryType.SEMANTIC_SIMILARITY,
            content = content,
            sourceSymbols = listOf(symbol1, symbol2),
            importance = importance,
            metadata = mutableMapOf(
                "similarityScore" to score,
                "symbol1Type" to symbol1.type.name,
                "symbol2Type" to symbol2.type.name
            )
        )
    }

    /**
     * 生成混合相似度记忆
     *
     * @param element 代码元素
     * @param symbol 符号节点
     * @param score 相似度分数
     * @return 语义记忆
     */
    private fun generateMixedSimilarityMemory(
        element: CodeElement,
        symbol: SymbolNode,
        score: Double
    ): SemanticMemory {
        val content = buildString {
            append("代码元素和符号相似度: ${element.name} 和 ${symbol.name}")
            append("\n- 类型: ${element.type.name.lowercase()} 和 ${symbol.type.name.lowercase()}")
            append("\n- 相似度分数: ${String.format("%.2f", score)}")

            // 添加位置信息
            append("\n- 元素位置: ${element.location.filePath.toString().substringAfterLast('/')}")
            append("\n- 符号限定名: ${symbol.qualifiedName}")
        }

        val importance = when {
            score >= 0.9 -> ImportanceLevel.HIGH
            score >= 0.8 -> ImportanceLevel.MEDIUM
            else -> ImportanceLevel.LOW
        }

        return SemanticMemory(
            type = MemoryType.SEMANTIC_SIMILARITY,
            content = content,
            sourceElements = listOf(element),
            sourceSymbols = listOf(symbol),
            importance = importance,
            metadata = mutableMapOf(
                "similarityScore" to score,
                "elementType" to element.type.name,
                "symbolType" to symbol.type.name
            )
        )
    }

    /**
     * 获取代码元素向量
     *
     * @param element 代码元素
     * @return 向量
     */
    private suspend fun getElementVector(element: CodeElement): FloatArray = withContext(Dispatchers.Default) {
        // 检查缓存
        elementVectorCache[element.id]?.let { return@withContext it }

        // 生成元素文本
        val text = buildString {
            append("${element.type.name.lowercase()}: ${element.name}")

            // 添加限定名
            if (element.qualifiedName != element.name) {
                append("\n限定名: ${element.qualifiedName}")
            }

            // 添加内容（如果有）
            element.metadata["content"]?.let { content ->
                if (content is String && content.isNotEmpty()) {
                    append("\n内容: $content")
                }
            }
        }

        // 生成向量
        val vector = embeddingService.embed(text)

        // 缓存向量
        if (config.enableCaching) {
            // 如果缓存已满，移除一些条目
            if (elementVectorCache.size >= config.cacheSize) {
                val keysToRemove = elementVectorCache.keys.take(config.cacheSize / 10)
                keysToRemove.forEach { elementVectorCache.remove(it) }
            }

            elementVectorCache[element.id] = vector
        }

        return@withContext vector
    }

    /**
     * 获取符号节点向量
     *
     * @param symbol 符号节点
     * @return 向量
     */
    private suspend fun getSymbolVector(symbol: SymbolNode): FloatArray = withContext(Dispatchers.Default) {
        // 检查缓存
        symbolVectorCache[symbol.id]?.let { return@withContext it }

        // 生成符号文本
        val text = buildString {
            append("${symbol.type.name.lowercase()}: ${symbol.name}")

            // 添加限定名
            if (symbol.qualifiedName != symbol.name) {
                append("\n限定名: ${symbol.qualifiedName}")
            }

            // 添加描述（如果有）
            symbol.metadata["description"]?.let { description ->
                if (description is String && description.isNotEmpty()) {
                    append("\n描述: $description")
                }
            }
        }

        // 生成向量
        val vector = embeddingService.embed(text)

        // 缓存向量
        if (config.enableCaching) {
            // 如果缓存已满，移除一些条目
            if (symbolVectorCache.size >= config.cacheSize) {
                val keysToRemove = symbolVectorCache.keys.take(config.cacheSize / 10)
                keysToRemove.forEach { symbolVectorCache.remove(it) }
            }

            symbolVectorCache[symbol.id] = vector
        }

        return@withContext vector
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
        elementVectorCache.clear()
        symbolVectorCache.clear()
        similarityCache.clear()
    }
}
