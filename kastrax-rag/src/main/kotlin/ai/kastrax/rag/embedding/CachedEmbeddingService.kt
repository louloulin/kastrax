package ai.kastrax.rag.embedding

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 缓存嵌入服务，缓存嵌入向量以减少 API 调用。
 *
 * @property delegate 委托的嵌入服务
 * @property cacheSize 缓存大小
 */
class CachedEmbeddingService(
    private val delegate: EmbeddingService,
    private val cacheSize: Int = 1000
) : EmbeddingService {
    private val cache = ConcurrentHashMap<String, FloatArray>()
    private val lruKeys = ConcurrentHashMap.newKeySet<String>()

    /**
     * 计算文本的嵌入向量。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        // 检查缓存
        cache[text]?.let {
            // 更新 LRU 顺序
            lruKeys.remove(text)
            lruKeys.add(text)
            return@withContext it
        }

        // 调用委托服务
        val embedding = delegate.embed(text)

        // 更新缓存
        updateCache(text, embedding)

        return@withContext embedding
    }

    /**
     * 批量计算文本的嵌入向量。
     *
     * @param texts 输入文本列表
     * @return 嵌入向量列表
     */
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        if (texts.isEmpty()) {
            return@withContext emptyList()
        }

        // 分离缓存命中和未命中的文本
        val cachedResults = mutableMapOf<Int, FloatArray>()
        val missingTexts = mutableListOf<String>()
        val missingIndices = mutableListOf<Int>()

        texts.forEachIndexed { index, text ->
            cache[text]?.let {
                cachedResults[index] = it
                // 更新 LRU 顺序
                lruKeys.remove(text)
                lruKeys.add(text)
            } ?: run {
                missingTexts.add(text)
                missingIndices.add(index)
            }
        }

        // 如果所有文本都在缓存中，直接返回
        if (missingTexts.isEmpty()) {
            return@withContext texts.mapIndexed { index, _ -> cachedResults[index]!! }
        }

        // 调用委托服务获取未缓存的嵌入
        val newEmbeddings = delegate.embedBatch(missingTexts)

        // 更新缓存
        missingTexts.zip(newEmbeddings).forEach { (text, embedding) ->
            updateCache(text, embedding)
        }

        // 合并结果
        val results = arrayOfNulls<FloatArray>(texts.size)
        cachedResults.forEach { (index, embedding) ->
            results[index] = embedding
        }
        missingIndices.zip(newEmbeddings).forEach { (index, embedding) ->
            results[index] = embedding
        }

        return@withContext results.filterNotNull()
    }

    /**
     * 获取嵌入向量的维度。
     *
     * @return 嵌入向量的维度
     */
    override fun dimension(): Int {
        return delegate.dimension()
    }

    /**
     * 关闭资源。
     */
    override fun close() {
        delegate.close()
    }

    /**
     * 更新缓存。
     *
     * @param text 文本
     * @param embedding 嵌入向量
     */
    private fun updateCache(text: String, embedding: FloatArray) {
        // 如果缓存已满，移除最久未使用的项
        if (cache.size >= cacheSize && !cache.containsKey(text)) {
            val oldestKey = lruKeys.firstOrNull()
            if (oldestKey != null) {
                lruKeys.remove(oldestKey)
                cache.remove(oldestKey)
            }
        }

        // 添加新项
        cache[text] = embedding
        lruKeys.add(text)
    }
}
