package ai.kastrax.rag.adapter

import ai.kastrax.rag.document.RagDocument
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * RagVectorStore 适配器，将 kastrax-store 模块中的 VectorStore 类适配到 kastrax-rag 模块中的 RagVectorStore 接口。
 *
 * @property vectorStore kastrax-store 模块中的 VectorStore 类
 * @property indexName 索引名称
 * @property dimension 向量维度
 */
class RagVectorStoreAdapter(
    private val vectorStore: VectorStore,
    private val indexName: String = "rag_index",
    private val dimension: Int = 1536
) : RagVectorStore {

    init {
        // 确保索引存在
        runCatching {
            kotlinx.coroutines.runBlocking {
                vectorStore.createIndex(indexName, dimension)
            }
        }
    }

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @param embeddings 嵌入向量列表
     * @return 是否成功添加
     */
    override suspend fun addDocuments(
        documents: List<RagDocument>,
        embeddings: List<FloatArray>
    ): Boolean = withContext(Dispatchers.IO) {
        if (documents.isEmpty() || embeddings.isEmpty()) {
            return@withContext true
        }

        if (documents.size != embeddings.size) {
            throw IllegalArgumentException("Documents size (${documents.size}) does not match embeddings size (${embeddings.size})")
        }

        try {
            // 转换文档为元数据
            val metadata = documents.map { doc ->
                doc.metadata + ("content" to doc.content)
            }

            // 使用文档 ID 或生成新的 ID
            val ids = documents.map { it.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString() }

            // 添加向量
            vectorStore.upsert(
                indexName = indexName,
                vectors = embeddings,
                metadata = metadata,
                ids = ids
            )

            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error adding documents to vector store" }
            return@withContext false
        }
    }

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @return 是否成功添加
     */
    override suspend fun addDocuments(
        documents: List<RagDocument>,
        embeddingService: EmbeddingService
    ): Boolean = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) {
            return@withContext true
        }

        try {
            // 计算嵌入向量
            val contents = documents.map { it.content }
            val embeddings = embeddingService.embedBatch(contents)

            // 添加文档
            return@withContext addDocuments(documents, embeddings)
        } catch (e: Exception) {
            logger.error(e) { "Error adding documents to vector store" }
            return@withContext false
        }
    }

    /**
     * 删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteDocuments(ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) {
            return@withContext true
        }

        try {
            // 删除向量
            return@withContext vectorStore.deleteVectors(indexName, ids)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting documents from vector store" }
            return@withContext false
        }
    }

    /**
     * 相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int
    ): List<ai.kastrax.rag.model.SearchResult> = withContext(Dispatchers.IO) {
        try {
            // 计算查询嵌入向量
            val embedding = embeddingService.embed(query)

            // 执行向量查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = embedding,
                topK = limit,
                filter = null
            )

            // 转换为 RAG 搜索结果
            return@withContext results.map { result ->
                val content = result.metadata?.get("content") as? String ?: ""
                val metadata = result.metadata?.filterKeys { it != "content" } ?: emptyMap()

                ai.kastrax.rag.model.SearchResult(
                    id = result.id,
                    content = content,
                    score = result.score,
                    metadata = metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error performing similarity search" }
            return@withContext emptyList()
        }
    }

    /**
     * 相似度搜索。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int
    ): List<ai.kastrax.rag.model.SearchResult> = withContext(Dispatchers.IO) {
        try {
            // 执行向量查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = embedding,
                topK = limit,
                filter = null
            )

            // 转换为 RAG 搜索结果
            return@withContext results.map { result ->
                val content = result.metadata?.get("content") as? String ?: ""
                val metadata = result.metadata?.filterKeys { it != "content" } ?: emptyMap()

                ai.kastrax.rag.model.SearchResult(
                    id = result.id,
                    content = content,
                    score = result.score,
                    metadata = metadata
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error performing similarity search" }
            return@withContext emptyList()
        }
    }

    /**
     * 关键词搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int
    ): List<ai.kastrax.rag.model.SearchResult> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            // 构建关键词查询
            val keywordPattern = keywords.joinToString("|") { Regex.escape(it) }
            val regex = Regex(keywordPattern, RegexOption.IGNORE_CASE)

            // 获取所有向量
            val allVectors = vectorStore.listVectors(indexName)

            // 过滤匹配的向量
            val matchingVectors = allVectors.filter { vector ->
                val content = vector.metadata?.get("content") as? String ?: ""
                regex.containsMatchIn(content)
            }

            // 计算分数并转换为 RAG 搜索结果
            return@withContext matchingVectors.map { vector ->
                val content = vector.metadata?.get("content") as? String ?: ""
                val metadata = vector.metadata?.filterKeys { it != "content" } ?: emptyMap()

                // 计算分数：匹配的关键词数量 / 关键词总数
                val matchCount = keywords.count { keyword ->
                    content.contains(keyword, ignoreCase = true)
                }
                val score = matchCount.toDouble() / keywords.size

                ai.kastrax.rag.model.SearchResult(
                    id = vector.id,
                    content = content,
                    score = score,
                    metadata = metadata
                )
            }.sortedByDescending { it.score }.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "Error performing keyword search" }
            return@withContext emptyList()
        }
    }

    /**
     * 元数据搜索。
     *
     * @param filter 过滤条件
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int
    ): List<ai.kastrax.rag.model.SearchResult> = withContext(Dispatchers.IO) {
        if (filter.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            // 获取所有向量
            val allVectors = vectorStore.listVectors(indexName)

            // 过滤匹配的向量
            val matchingVectors = allVectors.filter { vector ->
                filter.all { (key, value) ->
                    vector.metadata?.get(key) == value
                }
            }

            // 转换为 RAG 搜索结果
            return@withContext matchingVectors.map { vector ->
                val content = vector.metadata?.get("content") as? String ?: ""
                val metadata = vector.metadata?.filterKeys { it != "content" } ?: emptyMap()

                ai.kastrax.rag.model.SearchResult(
                    id = vector.id,
                    content = content,
                    score = 1.0, // 元数据搜索的分数固定为 1.0
                    metadata = metadata
                )
            }.take(limit)
        } catch (e: Exception) {
            logger.error(e) { "Error performing metadata search" }
            return@withContext emptyList()
        }
    }

    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    override suspend fun size(): Int = withContext(Dispatchers.IO) {
        try {
            val stats = vectorStore.getIndexStats(indexName)
            return@withContext stats.vectorCount
        } catch (e: Exception) {
            logger.error(e) { "Error getting vector store size" }
            return@withContext 0
        }
    }

    /**
     * 清空向量存储。
     */
    override suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            // 删除索引
            vectorStore.deleteIndex(indexName)
            
            // 重新创建索引
            vectorStore.createIndex(indexName, dimension)
        } catch (e: Exception) {
            logger.error(e) { "Error clearing vector store" }
        }
    }
}
