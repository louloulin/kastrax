package ai.kastrax.codebase.embedding

import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * 嵌入模型版本
 *
 * @property version 版本号
 * @property service 嵌入服务
 * @property createdAt 创建时间
 */
data class EmbeddingModelVersion(
    val version: String,
    val service: EmbeddingService,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 嵌入模型管理器配置
 *
 * @property defaultVersion 默认版本
 * @property transitionPeriodMs 过渡期（毫秒）
 * @property cacheSize 缓存大小
 */
data class EmbeddingModelManagerConfig(
    val defaultVersion: String = "latest",
    val transitionPeriodMs: Long = 24 * 60 * 60 * 1000, // 24小时
    val cacheSize: Int = 10000
)

/**
 * 嵌入模型管理器
 *
 * 管理嵌入模型的版本和平滑升级
 *
 * @property config 配置
 */
class EmbeddingModelManager(private val config: EmbeddingModelManagerConfig = EmbeddingModelManagerConfig()) {

    // 模型版本映射
    private val modelVersions = ConcurrentHashMap<String, EmbeddingModelVersion>()

    // 当前活动版本
    private val activeVersion = AtomicReference<String>(config.defaultVersion)

    // 过渡版本
    private val transitionVersion = AtomicReference<String?>(null)

    // 过渡开始时间
    private var transitionStartTime: Long = 0

    // 互斥锁，用于版本管理操作
    private val mutex = Mutex()

    // 嵌入缓存
    private val embeddingCache = ConcurrentHashMap<String, Map<String, FloatArray>>()

    /**
     * 注册模型版本
     *
     * @param version 版本号
     * @param service 嵌入服务
     * @param setAsActive 是否设置为活动版本
     * @return 是否成功注册
     */
    suspend fun registerModelVersion(
        version: String,
        service: EmbeddingService,
        setAsActive: Boolean = false
    ): Boolean = mutex.withLock {
        if (modelVersions.containsKey(version)) {
            logger.warn { "模型版本已存在: $version" }
            return@withLock false
        }

        val modelVersion = EmbeddingModelVersion(
            version = version,
            service = service
        )

        modelVersions[version] = modelVersion
        logger.info { "注册模型版本: $version" }

        if (setAsActive) {
            setActiveVersion(version)
        }

        return@withLock true
    }

    /**
     * 设置活动版本
     *
     * @param version 版本号
     * @param smoothTransition 是否平滑过渡
     * @return 是否成功设置
     */
    suspend fun setActiveVersion(
        version: String,
        smoothTransition: Boolean = true
    ): Boolean = mutex.withLock {
        if (!modelVersions.containsKey(version)) {
            logger.error { "模型版本不存在: $version" }
            return@withLock false
        }

        val currentActive = activeVersion.get()

        if (version == currentActive) {
            logger.warn { "版本 $version 已经是活动版本" }
            return@withLock true
        }

        if (smoothTransition) {
            // 设置过渡版本
            transitionVersion.set(version)
            transitionStartTime = System.currentTimeMillis()

            logger.info { "开始平滑过渡到版本: $version, 过渡期: ${config.transitionPeriodMs}ms" }
        } else {
            // 直接设置活动版本
            activeVersion.set(version)
            transitionVersion.set(null)

            logger.info { "设置活动版本: $version" }
        }

        return@withLock true
    }

    /**
     * 获取模型版本
     *
     * @param version 版本号，如果为 null，则使用活动版本
     * @return 模型版本
     */
    fun getModelVersion(version: String? = null): EmbeddingModelVersion? {
        val targetVersion = version ?: getEffectiveVersion()
        return modelVersions[targetVersion]
    }

    /**
     * 获取有效版本
     *
     * @return 有效版本号
     */
    private fun getEffectiveVersion(): String {
        val transition = transitionVersion.get()

        if (transition != null) {
            val elapsed = System.currentTimeMillis() - transitionStartTime

            if (elapsed >= config.transitionPeriodMs) {
                // 过渡期结束，更新活动版本
                activeVersion.set(transition)
                transitionVersion.set(null)

                logger.info { "过渡期结束，活动版本更新为: $transition" }

                return transition
            } else {
                // 在过渡期内，根据概率选择版本
                val transitionProgress = elapsed.toDouble() / config.transitionPeriodMs

                return if (Math.random() < transitionProgress) {
                    // 使用新版本
                    transition
                } else {
                    // 使用旧版本
                    activeVersion.get()
                }
            }
        }

        return activeVersion.get()
    }

    /**
     * 嵌入文本
     *
     * @param text 文本
     * @param version 版本号，如果为 null，则使用活动版本
     * @return 嵌入向量
     */
    suspend fun embed(
        text: String,
        version: String? = null
    ): FloatArray = withContext(Dispatchers.Default) {
        val targetVersion = version ?: getEffectiveVersion()
        val modelVersion = getModelVersion(targetVersion)
            ?: throw IllegalArgumentException("模型版本不存在: $targetVersion")

        // 计算文本的哈希值作为缓存键
        val cacheKey = text.hashCode().toString()

        // 检查缓存
        val versionCache = embeddingCache[cacheKey]
        if (versionCache != null && versionCache.containsKey(targetVersion)) {
            return@withContext versionCache[targetVersion]!!
        }

        // 生成嵌入
        val embedding = modelVersion.service.embed(text)

        // 更新缓存
        updateCache(cacheKey, targetVersion, embedding)

        return@withContext embedding
    }

    /**
     * 批量嵌入文本
     *
     * @param texts 文本列表
     * @param version 版本号，如果为 null，则使用活动版本
     * @return 嵌入向量列表
     */
    suspend fun embedBatch(
        texts: List<String>,
        version: String? = null
    ): List<FloatArray> = withContext(Dispatchers.Default) {
        val targetVersion = version ?: getEffectiveVersion()
        val modelVersion = getModelVersion(targetVersion)
            ?: throw IllegalArgumentException("模型版本不存在: $targetVersion")

        // 将文本分成已缓存和未缓存两部分
        val cachedResults = mutableMapOf<Int, FloatArray>()
        val textsToEmbed = mutableListOf<Pair<Int, String>>()

        texts.forEachIndexed { index, text ->
            val cacheKey = text.hashCode().toString()
            val versionCache = embeddingCache[cacheKey]

            if (versionCache != null && versionCache.containsKey(targetVersion)) {
                cachedResults[index] = versionCache[targetVersion]!!
            } else {
                textsToEmbed.add(index to text)
            }
        }

        // 如果所有文本都已缓存，直接返回结果
        if (textsToEmbed.isEmpty()) {
            return@withContext texts.indices.map { cachedResults[it]!! }
        }

        // 生成嵌入
        val embedTexts = textsToEmbed.map { it.second }
        val embeddings = modelVersion.service.embedBatch(embedTexts)

        // 更新缓存
        textsToEmbed.forEachIndexed { i, (index, text) ->
            val cacheKey = text.hashCode().toString()
            updateCache(cacheKey, targetVersion, embeddings[i])
            cachedResults[index] = embeddings[i]
        }

        // 按原始顺序返回结果
        return@withContext texts.indices.map { cachedResults[it]!! }
    }

    /**
     * 更新缓存
     *
     * @param cacheKey 缓存键
     * @param version 版本号
     * @param embedding 嵌入向量
     */
    private fun updateCache(cacheKey: String, version: String, embedding: FloatArray) {
        val versionCache = embeddingCache.getOrPut(cacheKey) { mutableMapOf() }.toMutableMap()
        versionCache[version] = embedding
        embeddingCache[cacheKey] = versionCache

        // 如果缓存大小超过限制，清理缓存
        if (embeddingCache.size > config.cacheSize) {
            cleanupCache()
        }
    }

    /**
     * 清理缓存
     */
    private fun cleanupCache() {
        // 移除最旧的条目
        val keysToRemove = embeddingCache.keys.take(embeddingCache.size - config.cacheSize)
        keysToRemove.forEach { embeddingCache.remove(it) }

        logger.debug { "清理嵌入缓存: 移除 ${keysToRemove.size} 个条目, 剩余 ${embeddingCache.size} 个条目" }
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        embeddingCache.clear()
        logger.debug { "清除嵌入缓存" }
    }

    /**
     * 获取所有模型版本
     *
     * @return 模型版本列表
     */
    fun getAllModelVersions(): List<EmbeddingModelVersion> {
        return modelVersions.values.toList()
    }

    /**
     * 获取活动版本
     *
     * @return 活动版本号
     */
    fun getActiveVersion(): String {
        return activeVersion.get()
    }

    /**
     * 获取过渡版本
     *
     * @return 过渡版本号，如果没有过渡版本则返回 null
     */
    fun getTransitionVersion(): String? {
        return transitionVersion.get()
    }

    /**
     * 获取过渡进度
     *
     * @return 过渡进度（0.0 - 1.0），如果没有过渡则返回 null
     */
    fun getTransitionProgress(): Double? {
        val transition = transitionVersion.get() ?: return null

        val elapsed = System.currentTimeMillis() - transitionStartTime
        return (elapsed.toDouble() / config.transitionPeriodMs).coerceIn(0.0, 1.0)
    }
}
