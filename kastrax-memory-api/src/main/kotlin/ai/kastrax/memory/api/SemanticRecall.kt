package ai.kastrax.memory.api

import kotlinx.serialization.Serializable

/**
 * 语义召回配置。
 *
 * @property topK 返回的最相似消息数量
 * @property messageRange 每个结果前后的消息数量
 * @property minScore 最小相似度阈值
 */
@Serializable
data class SemanticRecallConfig(
    val topK: Int = 2,
    val messageRange: Int = 2,
    val minScore: Float = 0.7f
)

/**
 * 语义搜索结果。
 *
 * @property message 内存消息
 * @property score 相似度分数
 */
data class SemanticSearchResult(
    val message: MemoryMessage,
    val score: Float
)

/**
 * 嵌入生成器接口，用于生成文本的嵌入向量。
 */
interface EmbeddingGenerator {
    /**
     * 生成文本的嵌入向量。
     *
     * @param text 要生成嵌入的文本
     * @return 嵌入向量
     */
    suspend fun generateEmbedding(text: String): List<Float>
    
    /**
     * 批量生成文本的嵌入向量。
     *
     * @param texts 要生成嵌入的文本列表
     * @return 嵌入向量列表
     */
    suspend fun generateEmbeddings(texts: List<String>): List<List<Float>>
}

/**
 * 向量存储接口，用于存储和检索嵌入向量。
 */
interface VectorStorage {
    /**
     * 保存嵌入向量。
     *
     * @param id 向量ID
     * @param vector 嵌入向量
     * @param metadata 元数据
     * @return 是否成功保存
     */
    suspend fun saveVector(id: String, vector: List<Float>, metadata: Map<String, String>): Boolean
    
    /**
     * 批量保存嵌入向量。
     *
     * @param vectors 向量ID、嵌入向量和元数据的列表
     * @return 成功保存的向量数量
     */
    suspend fun saveVectors(vectors: List<Triple<String, List<Float>, Map<String, String>>>): Int
    
    /**
     * 搜索相似向量。
     *
     * @param vector 查询向量
     * @param limit 返回的最大结果数量
     * @param minScore 最小相似度阈值
     * @param filter 元数据过滤条件
     * @return 相似向量的ID、分数和元数据
     */
    suspend fun searchVectors(
        vector: List<Float>,
        limit: Int = 10,
        minScore: Float = 0.7f,
        filter: Map<String, String> = emptyMap()
    ): List<Triple<String, Float, Map<String, String>>>
    
    /**
     * 删除向量。
     *
     * @param id 向量ID
     * @return 是否成功删除
     */
    suspend fun deleteVector(id: String): Boolean
    
    /**
     * 批量删除向量。
     *
     * @param ids 向量ID列表
     * @return 成功删除的向量数量
     */
    suspend fun deleteVectors(ids: List<String>): Int
    
    /**
     * 根据元数据过滤条件删除向量。
     *
     * @param filter 元数据过滤条件
     * @return 成功删除的向量数量
     */
    suspend fun deleteVectorsByFilter(filter: Map<String, String>): Int
}

/**
 * 语义内存接口，用于语义搜索和召回。
 */
interface SemanticMemory {
    /**
     * 保存消息并生成嵌入向量。
     *
     * @param message 要保存的消息
     * @param threadId 线程ID
     * @return 保存的消息ID
     */
    suspend fun saveMessage(message: Message, threadId: String): String
    
    /**
     * 语义搜索消息。
     *
     * @param query 搜索查询
     * @param threadId 线程ID
     * @param config 语义召回配置
     * @return 相关消息列表
     */
    suspend fun semanticSearch(
        query: String,
        threadId: String,
        config: SemanticRecallConfig = SemanticRecallConfig()
    ): List<SemanticSearchResult>
    
    /**
     * 获取语义召回消息。
     *
     * @param query 搜索查询
     * @param threadId 线程ID
     * @param config 语义召回配置
     * @return 召回的消息列表
     */
    suspend fun getSemanticRecallMessages(
        query: String,
        threadId: String,
        config: SemanticRecallConfig = SemanticRecallConfig()
    ): List<MemoryMessage>
}
