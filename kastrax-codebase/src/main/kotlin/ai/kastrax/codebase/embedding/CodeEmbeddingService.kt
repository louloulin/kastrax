package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.TimeSource
import java.io.Closeable

private val logger = KotlinLogging.logger {}

/**
 * 代码嵌入服务配置
 *
 * @property cacheSize 缓存大小
 * @property cacheExpirationDuration 缓存过期时间
 * @property batchSize 批处理大小
 * @property useGpu 是否使用 GPU
 * @property modelVersion 模型版本
 */
data class CodeEmbeddingServiceConfig(
    val cacheSize: Int = 10000,
    val cacheExpirationDuration: Duration = Duration.parse("24h"),
    val batchSize: Int = 32,
    val useGpu: Boolean = true,
    val modelVersion: String = "latest"
)

/**
 * 代码嵌入服务
 *
 * 为代码文件生成高质量的嵌入向量
 *
 * @property baseEmbeddingService 基础嵌入服务
 * @property config 配置
 */
class CodeEmbeddingService(
    private val baseEmbeddingService: EmbeddingService,
    private val config: CodeEmbeddingServiceConfig = CodeEmbeddingServiceConfig()
) : Closeable {

    // 嵌入缓存
    private val embeddingCache = ConcurrentHashMap<String, CachedEmbedding>()

    // 缓存统计
    private var cacheHits = 0
    private var cacheMisses = 0

    /**
     * 嵌入维度
     */
    val dimension: Int = 1536 // 使用固定值替代引用

    /**
     * 嵌入单个文本
     *
     * @param text 文本
     * @return 嵌入向量
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        // 计算文本的哈希值作为缓存键
        val cacheKey = text.hashCode().toString()

        // 检查缓存
        val cachedEmbedding = embeddingCache[cacheKey]
        if (cachedEmbedding != null && !cachedEmbedding.isExpired()) {
            // 缓存命中
            cacheHits++
            logger.debug { "嵌入缓存命中: $cacheKey" }
            return@withContext cachedEmbedding.embedding
        }

        // 缓存未命中
        cacheMisses++

        // 预处理代码文本
        val processedText = preprocessCode(text)

        // 生成嵌入
        val embedding = baseEmbeddingService.embed(processedText)

        // 缓存嵌入
        cacheEmbedding(cacheKey, embedding)

        // 如果缓存大小超过限制，清理过期条目
        if (embeddingCache.size > config.cacheSize) {
            cleanupCache()
        }

        return@withContext embedding
    }

    /**
     * 批量嵌入文本
     *
     * @param texts 文本列表
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.Default) {
        // 将文本分成已缓存和未缓存两部分
        val cachedResults = mutableMapOf<Int, FloatArray>()
        val textsToEmbed = mutableListOf<Pair<Int, String>>()

        texts.forEachIndexed { index, text ->
            val cacheKey = text.hashCode().toString()
            val cachedEmbedding = embeddingCache[cacheKey]

            if (cachedEmbedding != null && !cachedEmbedding.isExpired()) {
                // 缓存命中
                cacheHits++
                cachedResults[index] = cachedEmbedding.embedding
            } else {
                // 缓存未命中
                cacheMisses++
                textsToEmbed.add(index to text)
            }
        }

        // 如果所有文本都已缓存，直接返回结果
        if (textsToEmbed.isEmpty()) {
            return@withContext texts.indices.map { cachedResults[it]!! }
        }

        // 预处理未缓存的代码文本
        val processedTexts = textsToEmbed.map { (_, text) -> preprocessCode(text) }

        // 分批处理未缓存的文本
        val batchResults = mutableListOf<Pair<Int, FloatArray>>()
        processedTexts.chunked(config.batchSize).forEachIndexed { batchIndex, batch ->
            val batchStartIndex = batchIndex * config.batchSize

            // 生成嵌入
            val embeddings = baseEmbeddingService.embedBatch(batch)

            // 将结果与原始索引关联
            embeddings.forEachIndexed { i, embedding ->
                val originalIndex = textsToEmbed[batchStartIndex + i].first
                batchResults.add(originalIndex to embedding)

                // 缓存嵌入
                val cacheKey = texts[originalIndex].hashCode().toString()
                cacheEmbedding(cacheKey, embedding)
            }
        }

        // 合并缓存结果和新生成的结果
        val results = mutableMapOf<Int, FloatArray>()
        results.putAll(cachedResults)
        batchResults.forEach { (index, embedding) -> results[index] = embedding }

        // 如果缓存大小超过限制，清理过期条目
        if (embeddingCache.size > config.cacheSize) {
            cleanupCache()
        }

        // 按原始顺序返回结果
        return@withContext texts.indices.map { results[it]!! }
    }

    /**
     * 预处理代码文本
     *
     * @param code 代码文本
     * @return 预处理后的代码文本
     */
    private fun preprocessCode(code: String): String {
        // 移除注释
        var processedCode = removeComments(code)

        // 规范化空白字符
        processedCode = normalizeWhitespace(processedCode)

        // 移除不必要的字符
        processedCode = removeUnnecessaryCharacters(processedCode)

        return processedCode
    }

    /**
     * 移除注释
     *
     * @param code 代码文本
     * @return 移除注释后的代码文本
     */
    private fun removeComments(code: String): String {
        // 移除 Java/Kotlin 风格的多行注释
        var result = code.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")

        // 移除 Java/Kotlin 风格的单行注释
        result = result.replace(Regex("//.*"), "")

        // 移除 Python 风格的单行注释
        result = result.replace(Regex("#.*"), "")

        return result
    }

    /**
     * 规范化空白字符
     *
     * @param code 代码文本
     * @return 规范化空白字符后的代码文本
     */
    private fun normalizeWhitespace(code: String): String {
        // 将多个空白字符替换为单个空格
        var result = code.replace(Regex("\\s+"), " ")

        // 移除行首和行尾的空白字符
        result = result.trim()

        return result
    }

    /**
     * 移除不必要的字符
     *
     * @param code 代码文本
     * @return 移除不必要的字符后的代码文本
     */
    private fun removeUnnecessaryCharacters(code: String): String {
        // 保留代码的基本结构，但移除一些不必要的字符
        return code
    }

    /**
     * 缓存嵌入
     *
     * @param key 缓存键
     * @param embedding 嵌入向量
     */
    private fun cacheEmbedding(key: String, embedding: FloatArray) {
        val cachedEmbedding = CachedEmbedding(
            embedding = embedding,
            timestamp = TimeSource.Monotonic.markNow()
        )

        embeddingCache[key] = cachedEmbedding
    }

    /**
     * 清理缓存
     */
    private fun cleanupCache() {
        // 移除过期条目
        val expiredKeys = embeddingCache.entries
            .filter { it.value.isExpired() }
            .map { it.key }

        expiredKeys.forEach { embeddingCache.remove(it) }

        // 如果仍然超过大小限制，移除最旧的条目
        if (embeddingCache.size > config.cacheSize) {
            val oldestKeys = embeddingCache.entries
                .sortedBy { it.value.timestamp.elapsedNow() }
                .take(embeddingCache.size - config.cacheSize)
                .map { it.key }

            oldestKeys.forEach { embeddingCache.remove(it) }
        }

        logger.debug { "清理缓存: 移除 ${expiredKeys.size + (embeddingCache.size - config.cacheSize).coerceAtLeast(0)} 个条目, 剩余 ${embeddingCache.size} 个条目" }
    }

    /**
     * 获取缓存统计
     *
     * @return 缓存命中率
     */
    fun getCacheStats(): Double {
        val total = cacheHits + cacheMisses
        return if (total > 0) {
            cacheHits.toDouble() / total
        } else {
            0.0
        }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        embeddingCache.clear()
        cacheHits = 0
        cacheMisses = 0
        logger.debug { "清除嵌入缓存" }
    }

    /**
     * 缓存的嵌入
     *
     * @property embedding 嵌入向量
     * @property timestamp 时间戳
     */
    private data class CachedEmbedding(
        val embedding: FloatArray,
        val timestamp: TimeSource.Monotonic.ValueTimeMark
    ) {
        /**
         * 检查是否过期
         *
         * @return 是否过期
         */
        fun isExpired(): Boolean {
            return timestamp.elapsedNow() > CodeEmbeddingServiceConfig().cacheExpirationDuration
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CachedEmbedding

            if (!embedding.contentEquals(other.embedding)) return false
            if (timestamp != other.timestamp) return false

            return true
        }

        override fun hashCode(): Int {
            var result = embedding.contentHashCode()
            result = 31 * result + timestamp.hashCode()
            return result
        }
    }

    /**
     * 关闭资源
     */
    override fun close() {
        embeddingCache.clear()
    }

    companion object {
        /**
         * 创建代码嵌入服务
         *
         * @param baseEmbeddingService 基础嵌入服务
         * @param config 配置
         * @return 代码嵌入服务
         */
        fun create(
            baseEmbeddingService: EmbeddingService,
            config: CodeEmbeddingServiceConfig = CodeEmbeddingServiceConfig()
        ): CodeEmbeddingService {
            return CodeEmbeddingService(
                baseEmbeddingService = baseEmbeddingService,
                config = config
            )
        }
    }
}
