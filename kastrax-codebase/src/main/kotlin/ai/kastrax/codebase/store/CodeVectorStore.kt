package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 代码向量存储配置
 *
 * @property maxVectors 最大向量数量
 * @property dimension 向量维度
 * @property distanceThreshold 距离阈值
 * @property indexName 索引名称
 * @property userId 用户ID
 */
data class CodeVectorStoreConfig(
    val maxVectors: Int = 1000000,
    val dimension: Int = 1536,
    val distanceThreshold: Double = 0.75,
    val indexName: String = "code_vectors",
    val userId: String? = null
)

/**
 * 代码向量
 *
 * @property id 向量ID
 * @property vector 向量数据
 * @property metadata 元数据
 * @property timestamp 时间戳
 */
data class CodeVector(
    val id: String,
    val vector: FloatArray,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as CodeVector
        
        if (id != other.id) return false
        if (!vector.contentEquals(other.vector)) return false
        if (metadata != other.metadata) return false
        if (timestamp != other.timestamp) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * 搜索结果
 *
 * @property vector 向量
 * @property score 相似度分数
 * @property distance 距离
 */
data class CodeSearchResult(
    val vector: CodeVector,
    val score: Double,
    val distance: Double
)

/**
 * 代码向量存储
 *
 * 用于存储和检索代码嵌入向量
 *
 * @property baseVectorStore 基础向量存储
 * @property config 配置
 */
class CodeVectorStore(
    private val baseVectorStore: VectorStore,
    private val config: CodeVectorStoreConfig
) {
    // 向量映射
    private val vectors = ConcurrentHashMap<String, CodeVector>()
    
    // 互斥锁，用于写操作
    private val mutex = Mutex()
    
    /**
     * 添加向量
     *
     * @param vector 向量数据
     * @param metadata 元数据
     * @return 向量ID
     */
    suspend fun addVector(
        vector: FloatArray,
        metadata: Map<String, Any> = emptyMap()
    ): String = mutex.withLock {
        // 验证向量维度
        require(vector.size == config.dimension) {
            "向量维度不匹配: ${vector.size} != ${config.dimension}"
        }
        
        // 生成向量ID
        val id = UUID.randomUUID().toString()
        
        // 创建代码向量
        val codeVector = CodeVector(
            id = id,
            vector = vector,
            metadata = metadata
        )
        
        // 添加到映射
        vectors[id] = codeVector
        
        // 添加到基础向量存储
        val success = baseVectorStore.addVector(id, vector, metadata)
        
        if (!success) {
            // 如果添加失败，从映射中移除
            vectors.remove(id)
            throw IllegalStateException("添加向量到基础存储失败")
        }
        
        // 如果向量数量超过限制，移除最旧的向量
        if (vectors.size > config.maxVectors) {
            removeOldestVectors(vectors.size - config.maxVectors)
        }
        
        return@withLock id
    }
    
    /**
     * 批量添加向量
     *
     * @param vectors 向量数据列表
     * @param metadata 元数据列表
     * @return 向量ID列表
     */
    suspend fun addVectors(
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = List(vectors.size) { emptyMap() }
    ): List<String> = mutex.withLock {
        require(vectors.size == metadata.size) {
            "向量数量与元数据数量不匹配: ${vectors.size} != ${metadata.size}"
        }
        
        // 验证向量维度
        vectors.forEachIndexed { index, vector ->
            require(vector.size == config.dimension) {
                "向量 $index 维度不匹配: ${vector.size} != ${config.dimension}"
            }
        }
        
        // 生成向量ID
        val ids = List(vectors.size) { UUID.randomUUID().toString() }
        
        // 创建代码向量
        val codeVectors = ids.zip(vectors).zip(metadata) { (id, vector), meta ->
            CodeVector(
                id = id,
                vector = vector,
                metadata = meta
            )
        }
        
        // 添加到映射
        codeVectors.forEach { codeVector ->
            this.vectors[codeVector.id] = codeVector
        }
        
        // 添加到基础向量存储
        val success = baseVectorStore.addVectors(
            ids = ids,
            vectors = vectors,
            metadata = metadata
        )
        
        if (!success) {
            // 如果添加失败，从映射中移除
            codeVectors.forEach { codeVector ->
                this.vectors.remove(codeVector.id)
            }
            throw IllegalStateException("批量添加向量到基础存储失败")
        }
        
        // 如果向量数量超过限制，移除最旧的向量
        if (this.vectors.size > config.maxVectors) {
            removeOldestVectors(this.vectors.size - config.maxVectors)
        }
        
        return@withLock ids
    }
    
    /**
     * 添加嵌入
     *
     * @param text 文本
     * @param metadata 元数据
     * @param embeddingService 嵌入服务
     * @return 向量ID
     */
    suspend fun addEmbedding(
        text: String,
        metadata: Map<String, Any> = emptyMap(),
        embeddingService: EmbeddingService
    ): String = withContext(Dispatchers.Default) {
        // 生成嵌入
        val embedding = embeddingService.embed(text)
        
        // 添加向量
        return@withContext addVector(embedding, metadata)
    }
    
    /**
     * 批量添加嵌入
     *
     * @param texts 文本列表
     * @param metadata 元数据列表
     * @param embeddingService 嵌入服务
     * @return 向量ID列表
     */
    suspend fun addEmbeddings(
        texts: List<String>,
        metadata: List<Map<String, Any>> = List(texts.size) { emptyMap() },
        embeddingService: EmbeddingService
    ): List<String> = withContext(Dispatchers.Default) {
        require(texts.size == metadata.size) {
            "文本数量与元数据数量不匹配: ${texts.size} != ${metadata.size}"
        }
        
        // 生成嵌入
        val embeddings = embeddingService.embedBatch(texts)
        
        // 添加向量
        return@withContext addVectors(embeddings, metadata)
    }
    
    /**
     * 搜索相似向量
     *
     * @param vector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun searchVector(
        vector: FloatArray,
        limit: Int = 10,
        minScore: Double = 0.0
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        // 验证向量维度
        require(vector.size == config.dimension) {
            "向量维度不匹配: ${vector.size} != ${config.dimension}"
        }
        
        // 搜索基础向量存储
        val results = baseVectorStore.searchVector(vector, limit, minScore)
        
        // 转换为代码搜索结果
        return@withContext results.mapNotNull { result ->
            val codeVector = vectors[result.id] ?: return@mapNotNull null
            
            // 计算相似度分数
            val distance = result.distance
            val score = 1.0 - distance
            
            // 如果分数低于阈值，跳过
            if (score < config.distanceThreshold) {
                return@mapNotNull null
            }
            
            CodeSearchResult(
                vector = codeVector,
                score = score,
                distance = distance
            )
        }
    }
    
    /**
     * 搜索嵌入
     *
     * @param text 查询文本
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param embeddingService 嵌入服务
     * @return 搜索结果列表
     */
    suspend fun searchEmbedding(
        text: String,
        limit: Int = 10,
        minScore: Double = 0.0,
        embeddingService: EmbeddingService
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        // 生成嵌入
        val embedding = embeddingService.embed(text)
        
        // 搜索向量
        return@withContext searchVector(embedding, limit, minScore)
    }
    
    /**
     * 删除向量
     *
     * @param id 向量ID
     * @return 是否成功删除
     */
    suspend fun deleteVector(id: String): Boolean = mutex.withLock {
        // 从映射中移除
        val removed = vectors.remove(id) != null
        
        // 从基础向量存储中删除
        val success = baseVectorStore.deleteVector(id)
        
        return@withLock removed && success
    }
    
    /**
     * 批量删除向量
     *
     * @param ids 向量ID列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(ids: List<String>): Boolean = mutex.withLock {
        // 从映射中移除
        val removed = ids.map { id -> vectors.remove(id) != null }
        
        // 从基础向量存储中删除
        val success = baseVectorStore.deleteVectors(ids)
        
        return@withLock removed.all { it } && success
    }
    
    /**
     * 获取向量
     *
     * @param id 向量ID
     * @return 代码向量，如果不存在则返回 null
     */
    suspend fun getVector(id: String): CodeVector? = withContext(Dispatchers.Default) {
        return@withContext vectors[id]
    }
    
    /**
     * 批量获取向量
     *
     * @param ids 向量ID列表
     * @return 代码向量列表
     */
    suspend fun getVectors(ids: List<String>): List<CodeVector> = withContext(Dispatchers.Default) {
        return@withContext ids.mapNotNull { id -> vectors[id] }
    }
    
    /**
     * 获取所有向量
     *
     * @return 代码向量列表
     */
    suspend fun getAllVectors(): List<CodeVector> = withContext(Dispatchers.Default) {
        return@withContext vectors.values.toList()
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
     * 清空向量存储
     *
     * @return 是否成功清空
     */
    suspend fun clear(): Boolean = mutex.withLock {
        // 清空映射
        vectors.clear()
        
        // 清空基础向量存储
        return@withLock baseVectorStore.clear()
    }
    
    /**
     * 移除最旧的向量
     *
     * @param count 要移除的向量数量
     */
    private suspend fun removeOldestVectors(count: Int) {
        if (count <= 0) {
            return
        }
        
        // 按时间戳排序，获取最旧的向量
        val oldestVectors = vectors.values
            .sortedBy { it.timestamp }
            .take(count)
        
        // 删除这些向量
        val ids = oldestVectors.map { it.id }
        deleteVectors(ids)
        
        logger.debug { "移除 $count 个最旧的向量" }
    }
    
    /**
     * 计算余弦相似度
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        require(a.size == b.size) { "向量维度不匹配: ${a.size} != ${b.size}" }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0.0
        }
    }
}
