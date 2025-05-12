package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 分片向量存储配置
 *
 * @property shardCount 分片数量
 * @property replicaCount 副本数量
 * @property maxVectorsPerShard 每个分片的最大向量数量
 * @property consistencyLevel 一致性级别
 */
data class ShardedVectorStoreConfig(
    val shardCount: Int = 4,
    val replicaCount: Int = 1,
    val maxVectorsPerShard: Int = 250000,
    val consistencyLevel: ConsistencyLevel = ConsistencyLevel.QUORUM
)

/**
 * 一致性级别
 */
enum class ConsistencyLevel {
    /**
     * 任意一个副本
     */
    ANY,
    
    /**
     * 大多数副本
     */
    QUORUM,
    
    /**
     * 所有副本
     */
    ALL
}

/**
 * 分片信息
 *
 * @property shardId 分片ID
 * @property vectorCount 向量数量
 */
data class ShardInfo(
    val shardId: Int,
    val vectorCount: Int
)

/**
 * 分片向量存储
 *
 * 实现向量索引分片和分布式存储
 *
 * @property shardStoreFactory 分片存储工厂
 * @property config 配置
 */
class ShardedVectorStore(
    private val shardStoreFactory: (Int) -> VectorStore,
    private val config: ShardedVectorStoreConfig = ShardedVectorStoreConfig()
) {
    // 分片存储映射
    private val shardStores = ConcurrentHashMap<Int, List<CodeVectorStore>>()
    
    // 分片向量计数
    private val shardVectorCounts = ConcurrentHashMap<Int, AtomicInteger>()
    
    // 向量到分片的映射
    private val vectorToShard = ConcurrentHashMap<String, Int>()
    
    // 互斥锁，用于分片管理
    private val mutex = Mutex()
    
    init {
        // 初始化分片
        for (shardId in 0 until config.shardCount) {
            val replicas = List(config.replicaCount) { replicaId ->
                val baseStore = shardStoreFactory(shardId * config.replicaCount + replicaId)
                CodeVectorStore(
                    baseVectorStore = baseStore,
                    config = CodeVectorStoreConfig(
                        maxVectors = config.maxVectorsPerShard,
                        indexName = "shard_${shardId}_replica_${replicaId}"
                    )
                )
            }
            shardStores[shardId] = replicas
            shardVectorCounts[shardId] = AtomicInteger(0)
        }
    }
    
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
        // 选择分片
        val shardId = selectShardForAdd()
        
        // 获取分片存储
        val shardReplicas = shardStores[shardId] ?: throw IllegalStateException("分片不存在: $shardId")
        
        // 添加向量到所有副本
        val ids = coroutineScope {
            shardReplicas.map { store ->
                async {
                    store.addVector(vector, metadata)
                }
            }.awaitAll()
        }
        
        // 验证所有副本返回相同的ID
        val id = ids.first()
        if (ids.any { it != id }) {
            throw IllegalStateException("副本返回的ID不一致: $ids")
        }
        
        // 更新向量到分片的映射
        vectorToShard[id] = shardId
        
        // 更新分片向量计数
        shardVectorCounts[shardId]?.incrementAndGet()
        
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
        
        if (vectors.isEmpty()) {
            return@withLock emptyList()
        }
        
        // 按分片分组向量
        val shardGroups = vectors.indices.groupBy { selectShardForAdd() }
        
        // 添加向量到每个分片
        val allIds = mutableListOf<Pair<Int, String>>()
        
        coroutineScope {
            shardGroups.map { (shardId, indices) ->
                async {
                    // 获取分片存储
                    val shardReplicas = shardStores[shardId] ?: throw IllegalStateException("分片不存在: $shardId")
                    
                    // 提取该分片的向量和元数据
                    val shardVectors = indices.map { vectors[it] }
                    val shardMetadata = indices.map { metadata[it] }
                    
                    // 添加向量到所有副本
                    val replicaIds = shardReplicas.map { store ->
                        async {
                            store.addVectors(shardVectors, shardMetadata)
                        }
                    }.awaitAll()
                    
                    // 验证所有副本返回相同的ID
                    val ids = replicaIds.first()
                    for (i in 1 until replicaIds.size) {
                        if (replicaIds[i] != ids) {
                            throw IllegalStateException("副本返回的ID不一致")
                        }
                    }
                    
                    // 更新向量到分片的映射
                    ids.forEach { id ->
                        vectorToShard[id] = shardId
                    }
                    
                    // 更新分片向量计数
                    shardVectorCounts[shardId]?.addAndGet(ids.size)
                    
                    // 返回索引和ID的对应关系
                    indices.zip(ids).map { (index, id) -> index to id }
                }
            }.awaitAll().forEach { pairs ->
                allIds.addAll(pairs)
            }
        }
        
        // 按原始顺序返回ID
        return@withLock allIds.sortedBy { it.first }.map { it.second }
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
     * 搜索向量
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
        // 在所有分片中搜索
        val allResults = coroutineScope {
            shardStores.map { (shardId, replicas) ->
                async {
                    // 选择一个副本进行搜索
                    val replica = selectReplicaForSearch(replicas)
                    
                    // 搜索向量
                    val results = replica.searchVector(vector, limit, minScore)
                    
                    // 返回结果
                    results
                }
            }.awaitAll().flatten()
        }
        
        // 合并结果并按分数排序
        return@withContext allResults
            .sortedByDescending { it.score }
            .take(limit)
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
        // 获取向量所在的分片
        val shardId = vectorToShard[id] ?: return@withLock false
        
        // 获取分片存储
        val shardReplicas = shardStores[shardId] ?: return@withLock false
        
        // 从所有副本中删除向量
        val results = coroutineScope {
            shardReplicas.map { store ->
                async {
                    store.deleteVector(id)
                }
            }.awaitAll()
        }
        
        // 根据一致性级别检查结果
        val success = when (config.consistencyLevel) {
            ConsistencyLevel.ANY -> results.any { it }
            ConsistencyLevel.QUORUM -> results.count { it } > shardReplicas.size / 2
            ConsistencyLevel.ALL -> results.all { it }
        }
        
        if (success) {
            // 从映射中移除
            vectorToShard.remove(id)
            
            // 更新分片向量计数
            shardVectorCounts[shardId]?.decrementAndGet()
        }
        
        return@withLock success
    }
    
    /**
     * 批量删除向量
     *
     * @param ids 向量ID列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(ids: List<String>): Boolean = mutex.withLock {
        if (ids.isEmpty()) {
            return@withLock true
        }
        
        // 按分片分组向量ID
        val shardGroups = ids.groupBy { vectorToShard[it] ?: -1 }
        
        // 删除每个分片中的向量
        val results = coroutineScope {
            shardGroups.map { (shardId, shardIds) ->
                async {
                    if (shardId == -1) {
                        // 向量不存在
                        return@async false
                    }
                    
                    // 获取分片存储
                    val shardReplicas = shardStores[shardId] ?: return@async false
                    
                    // 从所有副本中删除向量
                    val replicaResults = shardReplicas.map { store ->
                        async {
                            store.deleteVectors(shardIds)
                        }
                    }.awaitAll()
                    
                    // 根据一致性级别检查结果
                    val success = when (config.consistencyLevel) {
                        ConsistencyLevel.ANY -> replicaResults.any { it }
                        ConsistencyLevel.QUORUM -> replicaResults.count { it } > shardReplicas.size / 2
                        ConsistencyLevel.ALL -> replicaResults.all { it }
                    }
                    
                    if (success) {
                        // 从映射中移除
                        shardIds.forEach { id ->
                            vectorToShard.remove(id)
                        }
                        
                        // 更新分片向量计数
                        shardVectorCounts[shardId]?.addAndGet(-shardIds.size)
                    }
                    
                    success
                }
            }.awaitAll()
        }
        
        // 所有分片操作都成功才算成功
        return@withLock results.all { it }
    }
    
    /**
     * 获取向量
     *
     * @param id 向量ID
     * @return 代码向量，如果不存在则返回 null
     */
    suspend fun getVector(id: String): CodeVector? = withContext(Dispatchers.Default) {
        // 获取向量所在的分片
        val shardId = vectorToShard[id] ?: return@withContext null
        
        // 获取分片存储
        val shardReplicas = shardStores[shardId] ?: return@withContext null
        
        // 选择一个副本获取向量
        val replica = selectReplicaForSearch(shardReplicas)
        
        // 获取向量
        return@withContext replica.getVector(id)
    }
    
    /**
     * 批量获取向量
     *
     * @param ids 向量ID列表
     * @return 代码向量列表
     */
    suspend fun getVectors(ids: List<String>): List<CodeVector> = withContext(Dispatchers.Default) {
        if (ids.isEmpty()) {
            return@withContext emptyList()
        }
        
        // 按分片分组向量ID
        val shardGroups = ids.groupBy { vectorToShard[it] ?: -1 }
        
        // 获取每个分片中的向量
        val results = coroutineScope {
            shardGroups.map { (shardId, shardIds) ->
                async {
                    if (shardId == -1) {
                        // 向量不存在
                        return@async emptyList<CodeVector>()
                    }
                    
                    // 获取分片存储
                    val shardReplicas = shardStores[shardId] ?: return@async emptyList<CodeVector>()
                    
                    // 选择一个副本获取向量
                    val replica = selectReplicaForSearch(shardReplicas)
                    
                    // 获取向量
                    replica.getVectors(shardIds)
                }
            }.awaitAll().flatten()
        }
        
        return@withContext results
    }
    
    /**
     * 获取分片信息
     *
     * @return 分片信息列表
     */
    fun getShardInfo(): List<ShardInfo> {
        return shardVectorCounts.map { (shardId, count) ->
            ShardInfo(
                shardId = shardId,
                vectorCount = count.get()
            )
        }
    }
    
    /**
     * 获取向量数量
     *
     * @return 向量数量
     */
    fun getVectorCount(): Int {
        return shardVectorCounts.values.sumOf { it.get() }
    }
    
    /**
     * 清空存储
     *
     * @return 是否成功清空
     */
    suspend fun clear(): Boolean = mutex.withLock {
        // 清空所有分片
        val results = coroutineScope {
            shardStores.values.flatten().map { store ->
                async {
                    store.clear()
                }
            }.awaitAll()
        }
        
        // 清空映射
        vectorToShard.clear()
        
        // 重置分片向量计数
        shardVectorCounts.forEach { (shardId, count) ->
            count.set(0)
        }
        
        // 所有分片操作都成功才算成功
        return@withLock results.all { it }
    }
    
    /**
     * 选择用于添加的分片
     *
     * @return 分片ID
     */
    private fun selectShardForAdd(): Int {
        // 选择向量数量最少的分片
        return shardVectorCounts.entries
            .minByOrNull { it.value.get() }
            ?.key ?: 0
    }
    
    /**
     * 选择用于搜索的副本
     *
     * @param replicas 副本列表
     * @return 副本
     */
    private fun selectReplicaForSearch(replicas: List<CodeVectorStore>): CodeVectorStore {
        // 随机选择一个副本
        return replicas.random()
    }
}
