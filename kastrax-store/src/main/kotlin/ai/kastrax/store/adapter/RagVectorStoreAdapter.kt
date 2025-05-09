package ai.kastrax.store.adapter

import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.RagDocument
import ai.kastrax.store.document.RagVectorStore
import ai.kastrax.store.document.SearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * RAG 向量存储适配器，将 VectorStore 适配为 RagVectorStore。
 *
 * @property vectorStore 向量存储
 * @property indexName 索引名称
 * @property dimension 向量维度
 */
class RagVectorStoreAdapter(
    private val vectorStore: VectorStore,
    private val indexName: String = "rag_index",
    private val dimension: Int = 1536
) : RagVectorStore {

    // 文档缓存
    private val documentCache = ConcurrentHashMap<String, RagDocument>()

    // 内容到 ID 的映射
    private val contentToId = ConcurrentHashMap<String, String>()

    init {
        // 确保索引存在
        runBlocking {
            runCatching {
                vectorStore.createIndex(indexName, dimension)
            }
        }
    }

    /**
     * 添加文档。
     *
     * @param document 文档内容
     * @param embedding 文档嵌入向量
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    override suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, Any>
    ): String = withContext(Dispatchers.IO) {
        // 检查文档是否已存在
        val existingId = contentToId[document]
        if (existingId != null) {
            logger.debug { "Document already exists with ID $existingId" }
            return@withContext existingId
        }

        // 生成文档 ID
        val id = UUID.randomUUID().toString()

        // 添加向量
        vectorStore.upsert(
            indexName = indexName,
            vectors = listOf(embedding),
            metadata = listOf(metadata + ("content" to document)),
            ids = listOf(id)
        )

        // 缓存文档
        val ragDocument = RagDocument(id, document, metadata)
        documentCache[id] = ragDocument
        contentToId[document] = id

        logger.debug { "Added document with ID $id" }
        return@withContext id
    }

    /**
     * 添加文档。
     *
     * @param document 文档内容
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    override suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, Any>
    ): String = withContext(Dispatchers.IO) {
        // 计算嵌入向量
        val embedding = embeddingService.embed(document)

        // 添加文档
        return@withContext addDocument(document, embedding, metadata)
    }

    /**
     * 批量添加文档。
     *
     * @param documents 文档内容列表
     * @param embeddings 文档嵌入向量列表
     * @param metadata 文档元数据列表
     * @return 文档 ID 列表
     */
    override suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadata: List<Map<String, Any>>
    ): List<String> = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) {
            return@withContext emptyList()
        }

        // 确保嵌入向量列表长度与文档列表长度相同
        if (embeddings.size != documents.size) {
            throw IllegalArgumentException("Embeddings size (${embeddings.size}) does not match documents size (${documents.size})")
        }

        // 确保元数据列表长度与文档列表长度相同
        val normalizedMetadata = if (metadata.size == documents.size) {
            metadata
        } else {
            List(documents.size) { i -> metadata.getOrElse(i) { emptyMap() } }
        }

        // 生成文档 ID
        val ids = documents.map { document ->
            contentToId[document] ?: UUID.randomUUID().toString()
        }

        // 添加向量
        val enhancedMetadata = normalizedMetadata.mapIndexed { i, meta ->
            meta + ("content" to documents[i])
        }
        vectorStore.upsert(
            indexName = indexName,
            vectors = embeddings,
            metadata = enhancedMetadata,
            ids = ids
        )

        // 缓存文档
        for (i in documents.indices) {
            val ragDocument = RagDocument(ids[i], documents[i], normalizedMetadata[i])
            documentCache[ids[i]] = ragDocument
            contentToId[documents[i]] = ids[i]
        }

        logger.debug { "Added ${documents.size} documents" }
        return@withContext ids
    }

    /**
     * 批量添加文档。
     *
     * @param documents 文档内容列表
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据列表
     * @return 文档 ID 列表
     */
    override suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadata: List<Map<String, Any>>
    ): List<String> = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) {
            return@withContext emptyList()
        }

        // 计算嵌入向量
        val embeddings = embeddingService.embedBatch(documents)

        // 添加文档
        return@withContext addDocuments(documents, embeddings, metadata)
    }

    /**
     * 获取文档。
     *
     * @param id 文档 ID
     * @return 文档
     */
    override suspend fun getDocument(id: String): RagDocument? = withContext(Dispatchers.IO) {
        // 从缓存中获取
        documentCache[id]
    }

    /**
     * 根据内容获取文档。
     *
     * @param content 文档内容
     * @return 文档
     */
    override suspend fun getDocumentByContent(content: String): RagDocument? = withContext(Dispatchers.IO) {
        // 从缓存中获取
        val id = contentToId[content]
        id?.let { documentCache[it] }
    }

    /**
     * 删除文档。
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    override suspend fun deleteDocument(id: String): Boolean = withContext(Dispatchers.IO) {
        // 从向量存储中删除
        val deleted = vectorStore.deleteVectors(indexName, listOf(id))

        // 从缓存中删除
        val document = documentCache.remove(id)
        document?.let { contentToId.remove(it.content) }

        logger.debug { "Deleted document with ID $id" }
        return@withContext deleted
    }

    /**
     * 批量删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteDocuments(ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) {
            return@withContext true
        }

        // 从向量存储中删除
        val deleted = vectorStore.deleteVectors(indexName, ids)

        // 从缓存中删除
        for (id in ids) {
            val document = documentCache.remove(id)
            document?.let { contentToId.remove(it.content) }
        }

        logger.debug { "Deleted ${ids.size} documents" }
        return@withContext deleted
    }

    /**
     * 相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int,
        minScore: Double
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        // 计算查询嵌入向量
        val embedding = embeddingService.embed(query)

        // 执行相似度搜索
        return@withContext similaritySearch(embedding, limit, minScore)
    }

    /**
     * 相似度搜索。
     *
     * @param embedding 查询嵌入向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    override suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int,
        minScore: Double
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        // 执行向量查询
        val results = vectorStore.query(
            indexName = indexName,
            queryVector = embedding,
            topK = limit,
            filter = null
        ).filter { it.score >= minScore }

        // 转换为 RAG 搜索结果
        return@withContext results.map { result ->
            val content = result.metadata?.get("content") as? String ?: ""
            val metadata = result.metadata?.filterKeys { it != "content" } ?: emptyMap()
            val document = RagDocument(result.id, content, metadata)

            // 缓存文档
            documentCache[result.id] = document
            contentToId[content] = result.id

            SearchResult(document, result.score)
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
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) {
            return@withContext emptyList()
        }

        // 构建关键词查询
        val keywordPattern = keywords.joinToString("|") { Regex.escape(it) }
        val regex = Regex(keywordPattern, RegexOption.IGNORE_CASE)

        // 从缓存中搜索
        val matchingDocuments = documentCache.values
            .filter { document -> regex.containsMatchIn(document.content) }
            .take(limit)

        // 计算分数
        return@withContext matchingDocuments.map { document ->
            // 简单的分数计算：匹配的关键词数量 / 关键词总数
            val matchCount = keywords.count { keyword ->
                document.content.contains(keyword, ignoreCase = true)
            }
            val score = matchCount.toDouble() / keywords.size

            SearchResult(document, score)
        }.sortedByDescending { it.score }
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
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (filter.isEmpty()) {
            return@withContext emptyList()
        }

        // 从缓存中搜索
        val matchingDocuments = documentCache.values
            .filter { document ->
                filter.all { (key, value) ->
                    document.metadata.containsKey(key) && document.metadata[key] == value
                }
            }
            .take(limit)

        // 返回结果
        return@withContext matchingDocuments.map { document ->
            SearchResult(document, 1.0) // 元数据搜索的分数固定为 1.0
        }
    }

    /**
     * 清空存储。
     */
    override suspend fun clear() = withContext(Dispatchers.IO) {
        // 删除索引
        vectorStore.deleteIndex(indexName)

        // 清空缓存
        documentCache.clear()
        contentToId.clear()

        // 重新创建索引
        vectorStore.createIndex(indexName, dimension)

        logger.debug { "Cleared RAG vector store" }
    }
}
