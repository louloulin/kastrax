package ai.kastrax.store.adapter

import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 文档向量存储适配器，将 VectorStore 适配为 DocumentVectorStore。
 *
 * @property vectorStore 向量存储
 * @property indexName 索引名称
 * @property dimension 向量维度
 */
class DocumentVectorStoreAdapter(
    private val vectorStore: VectorStore,
    private val indexName: String = "document_index",
    override val dimension: Int = 1536
) : DocumentVectorStore {

    /**
     * 文档缓存，用于存储已添加的文档。
     */
    private val documents = ConcurrentHashMap<String, Document>()

    /**
     * 获取底层向量存储。
     * 
     * @return 向量存储
     */
    override fun getVectorStore(): VectorStore {
        return vectorStore
    }

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @param embeddingService 嵌入服务
     * @return 是否成功添加
     */
    override suspend fun addDocuments(
        documents: List<Document>,
        embeddingService: EmbeddingService
    ): Boolean {
        if (documents.isEmpty()) {
            return true
        }

        try {
            // 确保索引存在
            ensureIndexExists()

            // 过滤出没有嵌入向量的文档
            val docsWithoutEmbeddings = documents.filter { it.embedding == null }
            val docsWithEmbeddings = documents.filter { it.embedding != null }

            // 为没有嵌入向量的文档生成嵌入向量
            val embeddings = if (docsWithoutEmbeddings.isNotEmpty()) {
                embeddingService.embedBatch(docsWithoutEmbeddings.map { it.content })
            } else {
                emptyList()
            }

            // 合并文档
            val allDocs = docsWithEmbeddings.toMutableList()
            docsWithoutEmbeddings.forEachIndexed { index, doc ->
                allDocs.add(doc.copy(embedding = embeddings.getOrNull(index)))
            }

            // 添加文档到向量存储
            return addDocumentsInternal(allDocs)
        } catch (e: Exception) {
            logger.error(e) { "Error adding documents: ${e.message}" }
            return false
        }
    }

    /**
     * 添加文档。
     *
     * @param documents 文档列表
     * @return 是否成功添加
     */
    override suspend fun addDocuments(documents: List<Document>): Boolean {
        if (documents.isEmpty()) {
            return true
        }

        // 过滤出有嵌入向量的文档
        val docsWithEmbeddings = documents.filter { it.embedding != null }
        if (docsWithEmbeddings.isEmpty()) {
            logger.warn { "No documents with embeddings to add" }
            return false
        }

        try {
            // 确保索引存在
            ensureIndexExists()

            // 添加文档到向量存储
            return addDocumentsInternal(docsWithEmbeddings)
        } catch (e: Exception) {
            logger.error(e) { "Error adding documents: ${e.message}" }
            return false
        }
    }

    /**
     * 从索引中删除文档。
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteDocuments(ids: List<String>): Boolean {
        if (ids.isEmpty()) {
            return true
        }

        try {
            // 从向量存储中删除文档
            val result = vectorStore.deleteVectors(indexName, ids)

            // 从缓存中删除文档
            ids.forEach { documents.remove(it) }

            return result
        } catch (e: Exception) {
            logger.error(e) { "Error deleting documents: ${e.message}" }
            return false
        }
    }

    /**
     * 使用查询文本进行相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int
    ): List<Document> {
        // 计算查询的嵌入向量
        val queryEmbedding = embeddingService.embed(query)
        return similaritySearch(queryEmbedding, limit)
    }
    
    /**
     * 使用嵌入向量进行相似度搜索。
     *
     * @param embedding 嵌入向量
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    override suspend fun similaritySearch(
        embedding: FloatArray,
        limit: Int
    ): List<Document> {
        try {
            // 使用向量存储进行查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = embedding,
                topK = limit
            )

            // 转换为 Document
            return results.mapNotNull { result ->
                val document = documents[result.id]
                if (document != null) {
                    // 更新分数
                    val updatedMetadata = document.metadata.toMutableMap()
                    updatedMetadata["score"] = result.score
                    document.copy(metadata = updatedMetadata)
                } else {
                    // 如果文档不在缓存中，则创建新文档
                    val content = result.metadata?.get("content") as? String ?: ""
                    Document(
                        id = result.id,
                        content = content,
                        metadata = (result.metadata?.mapValues { it.value } ?: emptyMap()) + mapOf("score" to result.score),
                        embedding = result.vector
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching documents: ${e.message}" }
            return emptyList()
        }
    }
    
    /**
     * 使用嵌入向量和过滤器进行相似度搜索。
     *
     * @param embedding 嵌入向量
     * @param filter 过滤器
     * @param limit 返回结果的最大数量
     * @return 文档列表，按相似度降序排序
     */
    override suspend fun similaritySearchWithFilter(
        embedding: FloatArray,
        filter: Map<String, Any>,
        limit: Int
    ): List<Document> {
        try {
            // 使用向量存储进行查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = embedding,
                topK = limit,
                filter = filter
            )

            // 转换为 Document
            return results.mapNotNull { result ->
                val document = documents[result.id]
                if (document != null) {
                    // 更新分数
                    val updatedMetadata = document.metadata.toMutableMap()
                    updatedMetadata["score"] = result.score
                    document.copy(metadata = updatedMetadata)
                } else {
                    // 如果文档不在缓存中，则创建新文档
                    val content = result.metadata?.get("content") as? String ?: ""
                    Document(
                        id = result.id,
                        content = content,
                        metadata = (result.metadata?.mapValues { it.value } ?: emptyMap()) + mapOf("score" to result.score),
                        embedding = result.vector
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching documents with filter: ${e.message}" }
            return emptyList()
        }
    }

    /**
     * 使用关键词进行搜索。
     *
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @return 文档列表，按匹配度降序排序
     */
    override suspend fun keywordSearch(
        keywords: List<String>,
        limit: Int
    ): List<Document> {
        if (documents.isEmpty() || keywords.isEmpty()) {
            return emptyList()
        }

        // 实现简单的关键词搜索
        val results = documents.values
            .filter { doc ->
                keywords.any { keyword ->
                    doc.content.contains(keyword, ignoreCase = true) ||
                            doc.metadata.values.any { 
                                it.toString().contains(keyword, ignoreCase = true) 
                            }
                }
            }
            .map { doc ->
                // 计算简单的匹配分数
                val score = keywords.count { keyword ->
                    doc.content.contains(keyword, ignoreCase = true) ||
                            doc.metadata.values.any { 
                                it.toString().contains(keyword, ignoreCase = true) 
                            }
                }.toDouble() / keywords.size
                
                // 更新分数
                val updatedMetadata = doc.metadata.toMutableMap()
                updatedMetadata["score"] = score
                doc.copy(metadata = updatedMetadata)
            }
            .filter { (it.metadata["score"] as Double) > 0.0 }
            .sortedByDescending { it.metadata["score"] as Double }
            .take(limit)

        return results
    }

    /**
     * 使用元数据过滤器进行搜索。
     *
     * @param filter 元数据过滤器
     * @param limit 返回结果的最大数量
     * @return 文档列表
     */
    override suspend fun metadataSearch(
        filter: Map<String, Any>,
        limit: Int
    ): List<Document> {
        if (documents.isEmpty() || filter.isEmpty()) {
            return emptyList()
        }

        try {
            // 创建一个测试向量（实际上我们只关心元数据过滤）
            val testVector = FloatArray(dimension) { 0f }
            testVector[0] = 1f

            // 使用向量存储进行查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = testVector,
                topK = limit,
                filter = filter
            )

            // 转换为 Document
            return results.mapNotNull { result ->
                val document = documents[result.id]
                if (document != null) {
                    // 更新分数
                    val updatedMetadata = document.metadata.toMutableMap()
                    updatedMetadata["score"] = 1.0 // 元数据搜索不考虑相似度分数
                    document.copy(metadata = updatedMetadata)
                } else {
                    // 如果文档不在缓存中，则创建新文档
                    val content = result.metadata?.get("content") as? String ?: ""
                    Document(
                        id = result.id,
                        content = content,
                        metadata = (result.metadata?.mapValues { it.value } ?: emptyMap()) + mapOf("score" to 1.0),
                        embedding = result.vector
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during metadata search: ${e.message}" }

            // 如果向量存储查询失败，回退到内存过滤
            return documents.values
                .filter { doc ->
                    filter.all { (key, value) ->
                        doc.metadata[key]?.toString()?.equals(value.toString(), ignoreCase = true) == true
                    }
                }
                .take(limit)
                .map { doc ->
                    // 更新分数
                    val updatedMetadata = doc.metadata.toMutableMap()
                    updatedMetadata["score"] = 1.0
                    doc.copy(metadata = updatedMetadata)
                }
        }
    }

    /**
     * 确保索引存在。
     */
    private suspend fun ensureIndexExists() {
        try {
            // 获取所有索引
            val indexes = vectorStore.listIndexes()
            
            // 如果索引不存在，则创建
            if (!indexes.contains(indexName)) {
                vectorStore.createIndex(indexName, dimension)
                logger.info { "Created index $indexName with dimension $dimension" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error ensuring index exists: ${e.message}" }
            throw e
        }
    }

    /**
     * 添加文档到向量存储。
     *
     * @param documents 文档列表
     * @return 是否成功添加
     */
    private suspend fun addDocumentsInternal(documents: List<Document>): Boolean {
        if (documents.isEmpty()) {
            return true
        }

        // 准备向量和元数据
        val vectors = documents.mapNotNull { it.embedding }
        if (vectors.size != documents.size) {
            logger.warn { "Some documents do not have embeddings" }
            return false
        }

        val metadata = documents.map { doc ->
            val meta = doc.metadata.toMutableMap()
            meta["content"] = doc.content
            meta
        }

        val ids = documents.map { it.id }

        // 添加向量到向量存储
        val result = vectorStore.upsert(
            indexName = indexName,
            vectors = vectors,
            metadata = metadata,
            ids = ids
        )

        // 更新缓存
        documents.forEach { this.documents[it.id] = it }

        return result.size == documents.size
    }
}
