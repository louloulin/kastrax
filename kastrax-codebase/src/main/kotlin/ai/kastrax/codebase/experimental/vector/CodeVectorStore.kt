package ai.kastrax.codebase.vector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 代码向量存储
 *
 * 用于存储和检索代码嵌入向量
 *
 * @property dimension 向量维度
 * @property vectors 向量映射表（ID -> 向量）
 * @property metadata 元数据映射表（ID -> 元数据）
 * @property elements 代码元素映射表（ID -> 代码元素）
 */
class CodeVectorStore(
    private val dimension: Int,
    private val vectors: ConcurrentHashMap<String, List<Float>> = ConcurrentHashMap(),
    private val metadata: ConcurrentHashMap<String, Map<String, Any>> = ConcurrentHashMap(),
    private val elements: ConcurrentHashMap<String, CodeElement> = ConcurrentHashMap()
) {
    /**
     * 添加向量
     *
     * @param id 向量ID
     * @param vector 向量
     * @param element 代码元素
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addVector(
        id: String,
        vector: List<Float>,
        element: CodeElement,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        if (vector.size != dimension) {
            logger.error { "向量维度不匹配: ${vector.size} != $dimension" }
            return false
        }

        vectors[id] = vector
        this.metadata[id] = metadata
        elements[id] = element
        return true
    }

    /**
     * 添加向量
     *
     * @param element 代码元素
     * @param vector 向量
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addVector(
        element: CodeElement,
        vector: List<Float>,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return addVector(element.id, vector, element, metadata)
    }

    /**
     * 批量添加向量
     *
     * @param vectors 向量列表
     * @return 成功添加的向量数量
     */
    fun addVectors(
        vectors: List<Triple<String, List<Float>, CodeElement>>,
        metadata: Map<String, Map<String, Any>> = emptyMap()
    ): Int {
        var count = 0
        vectors.forEach { (id, vector, element) ->
            if (addVector(id, vector, element, metadata[id] ?: emptyMap())) {
                count++
            }
        }
        return count
    }

    /**
     * 删除向量
     *
     * @param id 向量ID
     * @return 是否成功删除
     */
    fun removeVector(id: String): Boolean {
        val removed = vectors.remove(id) != null
        metadata.remove(id)
        elements.remove(id)
        return removed
    }

    /**
     * 批量删除向量
     *
     * @param ids 向量ID列表
     * @return 成功删除的向量数量
     */
    fun removeVectors(ids: List<String>): Int {
        var count = 0
        ids.forEach { id ->
            if (removeVector(id)) {
                count++
            }
        }
        return count
    }

    /**
     * 获取向量
     *
     * @param id 向量ID
     * @return 向量
     */
    fun getVector(id: String): List<Float>? {
        return vectors[id]
    }

    /**
     * 获取元数据
     *
     * @param id 向量ID
     * @return 元数据
     */
    fun getMetadata(id: String): Map<String, Any>? {
        return metadata[id]
    }

    /**
     * 获取代码元素
     *
     * @param id 向量ID
     * @return 代码元素
     */
    fun getElement(id: String): CodeElement? {
        return elements[id]
    }

    /**
     * 获取所有向量ID
     *
     * @return 向量ID列表
     */
    fun getAllIds(): List<String> {
        return vectors.keys.toList()
    }

    /**
     * 获取向量数量
     *
     * @return 向量数量
     */
    fun getVectorCount(): Int {
        return vectors.size
    }

    /**
     * 清空存储
     */
    fun clear() {
        vectors.clear()
        metadata.clear()
        elements.clear()
    }

    /**
     * 相似度搜索
     *
     * @param vector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 相似度结果列表
     */
    fun similaritySearch(
        vector: List<Float>,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((String, Map<String, Any>, CodeElement) -> Boolean)? = null
    ): List<SimilarityResult> {
        if (vector.size != dimension) {
            logger.error { "查询向量维度不匹配: ${vector.size} != $dimension" }
            return emptyList()
        }

        val results = mutableListOf<SimilarityResult>()

        vectors.forEach { (id, vec) ->
            val meta = metadata[id] ?: emptyMap()
            val element = elements[id] ?: return@forEach

            // 应用过滤器
            if (filter != null && !filter(id, meta, element)) {
                return@forEach
            }

            val score = cosineSimilarity(vector, vec)
            if (score >= minScore) {
                results.add(SimilarityResult(id, score, meta, element))
            }
        }

        // 按相似度降序排序并限制结果数量
        return results.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 相似度搜索（按代码元素类型过滤）
     *
     * @param vector 查询向量
     * @param types 代码元素类型列表
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByTypes(
        vector: List<Float>,
        types: List<CodeElementType>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<SimilarityResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, _, element -> element.type in types }
        )
    }

    /**
     * 相似度搜索（按元数据过滤）
     *
     * @param vector 查询向量
     * @param metadataFilter 元数据过滤条件
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByMetadata(
        vector: List<Float>,
        metadataFilter: Map<String, Any>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<SimilarityResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, meta, _ ->
                metadataFilter.all { (key, value) ->
                    meta[key] == value
                }
            }
        )
    }

    /**
     * 相似度搜索（按文件路径过滤）
     *
     * @param vector 查询向量
     * @param filePath 文件路径
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByFilePath(
        vector: List<Float>,
        filePath: String,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<SimilarityResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, _, element ->
                element.location.filePath.toString() == filePath
            }
        )
    }

    /**
     * 相似度搜索（按语言过滤）
     *
     * @param vector 查询向量
     * @param language 编程语言
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByLanguage(
        vector: List<Float>,
        language: String,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<SimilarityResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, _, element ->
                element.language.equals(language, ignoreCase = true)
            }
        )
    }

    /**
     * 相似度搜索（混合查询）
     *
     * @param vector 查询向量
     * @param types 代码元素类型列表
     * @param metadataFilter 元数据过滤条件
     * @param language 编程语言
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchMixed(
        vector: List<Float>,
        types: List<CodeElementType>? = null,
        metadataFilter: Map<String, Any>? = null,
        language: String? = null,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<SimilarityResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, meta, element ->
                (types == null || element.type in types) &&
                (metadataFilter == null || metadataFilter.all { (key, value) -> meta[key] == value }) &&
                (language == null || element.language.equals(language, ignoreCase = true))
            }
        )
    }

    /**
     * 计算余弦相似度
     *
     * @param a 向量A
     * @param b 向量B
     * @return 余弦相似度
     */
    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) {
            throw IllegalArgumentException("向量维度不匹配: ${a.size} != ${b.size}")
        }

        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        if (normA == 0.0f || normB == 0.0f) {
            return 0.0f
        }

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    /**
     * 相似度结果
     *
     * @property id 向量ID
     * @property score 相似度分数
     * @property metadata 元数据
     * @property element 代码元素
     */
    data class SimilarityResult(
        val id: String,
        val score: Float,
        val metadata: Map<String, Any>,
        val element: CodeElement
    )
}
