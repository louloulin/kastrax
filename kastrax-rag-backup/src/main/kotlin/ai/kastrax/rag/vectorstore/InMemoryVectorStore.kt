package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * 基于内存的向量存储实现。
 */
open class InMemoryVectorStore : RagVectorStore {
    // 文档 ID 生成器
    private val idGenerator = AtomicLong(0)

    // 文档存储
    private val documents = ConcurrentHashMap<String, RagDocument>()

    // 向量存储
    private val vectors = ConcurrentHashMap<String, FloatArray>()

    // 文档内容到 ID 的映射，用于去重
    private val contentToId = ConcurrentHashMap<String, String>()

    /**
     * 添加文档到向量存储。
     *
     * @param document 文档
     * @param embedding 文档的嵌入向量
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    override suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, String>
    ): String {
        // 检查文档是否已存在
        val existingId = contentToId[document]
        if (existingId != null) {
            logger.debug { "Document already exists with ID: $existingId" }
            return existingId
        }

        // 生成新的文档 ID
        val id = generateId()

        // 创建文档对象
        val ragDocument = RagDocument(
            id = id,
            content = document,
            metadata = metadata
        )

        // 存储文档和向量
        documents[id] = ragDocument
        vectors[id] = embedding
        contentToId[document] = id

        logger.debug { "Added document with ID: $id" }
        return id
    }

    /**
     * 添加文档到向量存储，并使用嵌入服务计算嵌入向量。
     *
     * @param document 文档
     * @param embeddingService 嵌入服务
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    override suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, String>
    ): String {
        // 检查文档是否已存在
        val existingId = contentToId[document]
        if (existingId != null) {
            logger.debug { "Document already exists with ID: $existingId" }
            return existingId
        }

        // 计算文档的嵌入向量
        val embedding = embeddingService.embed(document)

        // 添加文档和向量
        return addDocument(document, embedding, metadata)
    }

    /**
     * 批量添加文档到向量存储。
     *
     * @param documents 文档列表
     * @param embeddings 嵌入向量列表
     * @param metadataList 元数据列表
     * @return 文档 ID 列表
     */
    override suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadataList: List<Map<String, String>>
    ): List<String> {
        require(documents.size == embeddings.size) { "Documents and embeddings must have the same size" }
        require(documents.size == metadataList.size) { "Documents and metadata must have the same size" }

        return documents.mapIndexed { index, document ->
            addDocument(document, embeddings[index], metadataList[index])
        }
    }

    /**
     * 批量添加文档到向量存储，并使用嵌入服务计算嵌入向量。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @param metadataList 元数据列表
     * @return 文档 ID 列表
     */
    override suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadataList: List<Map<String, String>>
    ): List<String> {
        require(documents.size == metadataList.size) { "Documents and metadata must have the same size" }

        // 计算所有文档的嵌入向量
        val embeddings = documents.map { document ->
            embeddingService.embed(document)
        }

        // 添加文档和向量
        return addDocuments(documents, embeddings, metadataList)
    }

    /**
     * 根据 ID 获取文档。
     *
     * @param id 文档 ID
     * @return 文档，如果不存在则返回 null
     */
    override suspend fun getDocument(id: String): RagDocument? {
        return documents[id]
    }

    /**
     * 根据内容获取文档。
     *
     * @param content 文档内容
     * @return 文档，如果不存在则返回 null
     */
    override suspend fun getDocumentByContent(content: String): RagDocument? {
        val id = contentToId[content]
        return id?.let { documents[it] }
    }

    /**
     * 根据 ID 获取文档的嵌入向量。
     *
     * @param id 文档 ID
     * @return 嵌入向量，如果不存在则返回 null
     */
    override suspend fun getEmbedding(id: String): FloatArray? {
        return vectors[id]
    }

    /**
     * 根据 ID 删除文档。
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    override suspend fun deleteDocument(id: String): Boolean {
        val document = documents.remove(id)
        if (document != null) {
            vectors.remove(id)
            contentToId.remove(document.content)
            logger.debug { "Deleted document with ID: $id" }
            return true
        }
        return false
    }

    /**
     * 清空向量存储。
     */
    override suspend fun clear() {
        documents.clear()
        vectors.clear()
        contentToId.clear()
        logger.debug { "Cleared vector store" }
    }

    /**
     * 获取向量存储中的文档数量。
     *
     * @return 文档数量
     */
    override suspend fun size(): Int {
        return documents.size
    }

    /**
     * 使用查询文本进行相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        if (documents.isEmpty()) {
            logger.debug { "Vector store is empty" }
            return emptyList()
        }

        // 计算查询的嵌入向量
        val queryEmbedding = embeddingService.embed(query)

        // 计算查询向量与所有文档向量的相似度
        val results = vectors.entries.map { (id, embedding) ->
            val similarity = ai.kastrax.rag.util.cosineSimilarity(queryEmbedding, embedding)
            SearchResult(documents[id]!!, similarity)
        }

        // 过滤、排序并限制结果数量
        return results
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 使用关键词进行搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表，按匹配度降序排序
     */
    override suspend fun keywordSearch(keywords: List<String>, limit: Int): List<SearchResult> {
        if (documents.isEmpty() || keywords.isEmpty()) {
            return emptyList()
        }

        // 将关键词转换为小写
        val lowercaseKeywords = keywords.map { it.lowercase() }

        // 计算每个文档的关键词匹配度
        val results = documents.values.map { document ->
            val content = document.content.lowercase()

            // 计算匹配的关键词数量
            val matchCount = lowercaseKeywords.count { keyword ->
                content.contains(keyword)
            }

            // 计算匹配度分数
            val score = if (lowercaseKeywords.isEmpty()) 0.0 else matchCount.toDouble() / lowercaseKeywords.size

            SearchResult(document, score)
        }

        // 过滤、排序并限制结果数量
        return results
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 使用元数据过滤器进行搜索。
     *
     * @param filter 元数据过滤器
     * @param limit 返回结果的最大数量
     * @return 搜索结果列表
     */
    override suspend fun metadataSearch(filter: Map<String, Any>, limit: Int): List<SearchResult> {
        if (documents.isEmpty() || filter.isEmpty()) {
            return emptyList()
        }

        // 过滤文档
        val filteredDocuments = documents.values.filter { document ->
            matchesFilter(document.metadata, filter)
        }

        // 为每个文档计算匹配分数
        val results = filteredDocuments.map { document ->
            val matchScore = calculateFilterMatchScore(document.metadata, filter)
            SearchResult(document, matchScore)
        }

        // 排序并限制结果数量
        return results
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 检查元数据是否匹配过滤器。
     *
     * @param metadata 元数据
     * @param filter 过滤器
     * @return 是否匹配
     */
    private fun matchesFilter(metadata: Map<String, String>, filter: Map<String, Any>): Boolean {
        // 所有过滤条件都必须匹配
        return filter.all { (key, value) ->
            when (value) {
                is String -> metadata[key]?.equals(value, ignoreCase = true) ?: false
                is Number -> metadata[key]?.toDoubleOrNull() == value.toDouble()
                is Boolean -> metadata[key]?.toBoolean() == value
                is List<*> -> {
                    val metadataValue = metadata[key] ?: return@all false
                    value.any { it.toString().equals(metadataValue, ignoreCase = true) }
                }
                else -> false
            }
        }
    }

    /**
     * 计算元数据与过滤器的匹配分数。
     *
     * @param metadata 元数据
     * @param filter 过滤器
     * @return 匹配分数
     */
    private fun calculateFilterMatchScore(metadata: Map<String, String>, filter: Map<String, Any>): Double {
        if (filter.isEmpty()) return 0.0

        // 计算匹配的过滤条件数量
        val matchCount = filter.count { (key, value) ->
            when (value) {
                is String -> metadata[key]?.equals(value, ignoreCase = true) ?: false
                is Number -> metadata[key]?.toDoubleOrNull() == value.toDouble()
                is Boolean -> metadata[key]?.toBoolean() == value
                is List<*> -> {
                    val metadataValue = metadata[key] ?: return@count false
                    value.any { it.toString().equals(metadataValue, ignoreCase = true) }
                }
                else -> false
            }
        }

        // 计算匹配分数
        return matchCount.toDouble() / filter.size
    }

    /**
     * 生成唯一的文档 ID。
     *
     * @return 文档 ID
     */
    private fun generateId(): String {
        return "doc_${idGenerator.incrementAndGet()}"
    }
}
