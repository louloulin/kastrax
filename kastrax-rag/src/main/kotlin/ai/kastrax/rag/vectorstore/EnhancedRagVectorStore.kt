package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 增强的RAG向量存储，提供高级索引和查询功能。
 */
class EnhancedRagVectorStore : EnhancedVectorStore {
    private val documents = mutableMapOf<String, RagDocument>()
    private val embeddings = mutableMapOf<String, FloatArray>()
    private val metadataIndex = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
    private val createdAt = System.currentTimeMillis()
    private var updatedAt = createdAt
    private var status = EnhancedVectorStore.IndexStatus.ACTIVE
    private val indexOptions = EnhancedVectorStore.IndexOptions()

    override suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, String>
    ): String {
        val id = java.util.UUID.randomUUID().toString()
        val ragDocument = RagDocument(id, document, metadata)

        documents[id] = ragDocument
        embeddings[id] = embedding

        // 更新元数据索引
        updateMetadataIndex(id, metadata)

        updatedAt = System.currentTimeMillis()
        return id
    }

    override suspend fun addDocument(
        document: String,
        embeddingService: EmbeddingService,
        metadata: Map<String, String>
    ): String {
        val embedding = embeddingService.embed(document)
        return addDocument(document, embedding, metadata)
    }

    override suspend fun addDocuments(
        documents: List<String>,
        embeddings: List<FloatArray>,
        metadataList: List<Map<String, String>>
    ): List<String> {
        if (documents.size != embeddings.size || documents.size != metadataList.size) {
            throw IllegalArgumentException("Documents, embeddings, and metadata lists must have the same size")
        }

        return documents.zip(embeddings).zip(metadataList).map { (pair, metadata) ->
            val (document, embedding) = pair
            addDocument(document, embedding, metadata)
        }
    }

    override suspend fun addDocuments(
        documents: List<String>,
        embeddingService: EmbeddingService,
        metadataList: List<Map<String, String>>
    ): List<String> {
        if (documents.size != metadataList.size) {
            throw IllegalArgumentException("Documents and metadata lists must have the same size")
        }

        return documents.zip(metadataList).map { (document, metadata) ->
            addDocument(document, embeddingService, metadata)
        }
    }

    override suspend fun getDocument(id: String): RagDocument? {
        return documents[id]
    }

    override suspend fun getDocumentByContent(content: String): RagDocument? {
        // 在增强的向量存储中，我们需要遍历所有文档来查找匹配的内容
        return documents.values.firstOrNull { it.content == content }
    }

    override suspend fun getEmbedding(id: String): FloatArray? {
        return embeddings[id]
    }

    override suspend fun deleteDocument(id: String): Boolean {
        val document = documents.remove(id)
        val embedding = embeddings.remove(id)

        // 从元数据索引中删除
        for ((field, valueMap) in metadataIndex) {
            for ((value, docIds) in valueMap) {
                docIds.remove(id)
                if (docIds.isEmpty()) {
                    valueMap.remove(value)
                }
            }
            if (valueMap.isEmpty()) {
                metadataIndex.remove(field)
            }
        }

        updatedAt = System.currentTimeMillis()
        return document != null || embedding != null
    }

    override suspend fun clear() {
        documents.clear()
        embeddings.clear()
        metadataIndex.clear()
        updatedAt = System.currentTimeMillis()
    }

    override suspend fun size(): Int {
        return documents.size
    }

    override suspend fun similaritySearch(
        query: String,
        embeddingService: EmbeddingService,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        val queryEmbedding = embeddingService.embed(query)

        if (embeddings.isEmpty()) {
            return emptyList()
        }

        // 计算相似度
        val results = embeddings.entries.map { (id, embedding) ->
            val similarity = calculateSimilarity(queryEmbedding, embedding)
            SearchResult(documents[id]!!, similarity)
        }

        // 过滤、排序并限制结果
        return results
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
    }

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

        // 过滤、排序并限制结果
        return results
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    override suspend fun metadataSearch(filter: Map<String, Any>, limit: Int): List<SearchResult> {
        if (documents.isEmpty() || filter.isEmpty()) {
            return emptyList()
        }

        // 应用元数据过滤器
        val filteredIds = applyMetadataFilter(filter)

        if (filteredIds.isEmpty()) {
            return emptyList()
        }

        // 获取匹配的文档
        val results = filteredIds.mapNotNull { id ->
            val document = documents[id] ?: return@mapNotNull null
            SearchResult(document, 1.0)
        }

        // 限制结果数量
        return results.take(limit)
    }

    override suspend fun advancedSearch(
        query: String,
        embeddingService: EmbeddingService,
        options: EnhancedVectorStore.QueryOptions
    ): List<SearchResult> {
        val queryEmbedding = embeddingService.embed(query)
        return advancedSearch(queryEmbedding, options)
    }

    override suspend fun advancedSearch(
        embedding: FloatArray,
        options: EnhancedVectorStore.QueryOptions
    ): List<SearchResult> {
        if (embeddings.isEmpty()) {
            return emptyList()
        }

        // 应用元数据过滤器
        val filteredIds = if (options.filter.isNotEmpty()) {
            applyMetadataFilter(options.filter)
        } else {
            documents.keys
        }

        if (filteredIds.isEmpty()) {
            return emptyList()
        }

        // 计算相似度
        val results = filteredIds.mapNotNull { id ->
            val docEmbedding = embeddings[id] ?: return@mapNotNull null
            val document = documents[id] ?: return@mapNotNull null

            val similarity = calculateSimilarity(embedding, docEmbedding)
            SearchResult(document, similarity)
        }

        // 过滤、排序并限制结果
        val filteredResults = results
            .filter { it.score >= options.minScore }
            .sortedByDescending { it.score }

        // 应用偏移量和限制
        val paginatedResults = filteredResults
            .drop(options.offset)
            .take(options.limit)

        // 如果需要重排序，先获取更多结果，然后重排序
        return if (options.rerank) {
            val rerankResults = filteredResults
                .drop(options.offset)
                .take(options.rerankSize)

            // 简单的重排序策略：根据文档长度和相似度重新计算分数
            rerankResults.map { result ->
                val lengthFactor = 1.0 - kotlin.math.exp(-result.document.content.length / 1000.0)
                val newScore = result.score * 0.8 + lengthFactor * 0.2
                SearchResult(result.document, newScore)
            }.sortedByDescending { it.score }
                .take(options.limit)
        } else {
            paginatedResults
        }
    }

    override suspend fun hybridSearch(
        query: String,
        embeddingService: EmbeddingService,
        keywords: List<String>,
        options: EnhancedVectorStore.QueryOptions
    ): List<SearchResult> {
        if (embeddings.isEmpty() || query.isEmpty()) {
            return emptyList()
        }

        // 执行向量搜索
        val vectorResults = advancedSearch(
            query,
            embeddingService,
            options.copy(limit = options.rerankSize)
        )

        // 如果没有关键词，直接返回向量搜索结果
        if (keywords.isEmpty()) {
            return vectorResults.take(options.limit)
        }

        // 执行关键词搜索
        val keywordResults = keywordSearch(keywords, options.rerankSize)

        // 合并结果
        val mergedResults = mergeSearchResults(
            vectorResults,
            keywordResults,
            options.hybridAlpha
        )

        // 应用偏移量和限制
        return mergedResults
            .drop(options.offset)
            .take(options.limit)
    }

    override suspend fun createIndex(options: EnhancedVectorStore.IndexOptions): Boolean {
        status = EnhancedVectorStore.IndexStatus.CREATING

        try {
            // 对于内存向量存储，创建索引只是更新索引选项

            // 更新索引状态
            status = EnhancedVectorStore.IndexStatus.ACTIVE
            updatedAt = System.currentTimeMillis()

            return true
        } catch (e: Exception) {
            logger.error(e) { "Error creating index" }
            status = EnhancedVectorStore.IndexStatus.UNAVAILABLE
            return false
        }
    }

    override suspend fun deleteIndex(): Boolean {
        status = EnhancedVectorStore.IndexStatus.DELETING

        try {
            // 清空所有数据
            documents.clear()
            embeddings.clear()
            metadataIndex.clear()

            // 更新索引状态
            status = EnhancedVectorStore.IndexStatus.ACTIVE
            updatedAt = System.currentTimeMillis()

            return true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting index" }
            status = EnhancedVectorStore.IndexStatus.UNAVAILABLE
            return false
        }
    }

    override suspend fun optimizeIndex(options: EnhancedVectorStore.IndexOptions): Boolean {
        status = EnhancedVectorStore.IndexStatus.OPTIMIZING

        try {
            // 对于内存向量存储，优化索引只是重建元数据索引
            rebuildMetadataIndex()

            // 更新索引状态
            status = EnhancedVectorStore.IndexStatus.ACTIVE
            updatedAt = System.currentTimeMillis()

            return true
        } catch (e: Exception) {
            logger.error(e) { "Error optimizing index" }
            status = EnhancedVectorStore.IndexStatus.UNAVAILABLE
            return false
        }
    }

    override suspend fun getIndexStats(): EnhancedVectorStore.IndexStats {
        return EnhancedVectorStore.IndexStats(
            documentCount = documents.size,
            indexSize = calculateIndexSize(),
            dimensions = indexOptions.dimensions,
            createdAt = createdAt,
            updatedAt = updatedAt,
            indexName = indexOptions.indexName,
            indexType = "in-memory",
            indexVersion = "1.0.0",
            status = status,
            memoryUsage = calculateIndexSize(),
            diskUsage = 0L  // 内存向量存储没有磁盘使用量
        )
    }

    override suspend fun getDocumentCount(): Int {
        return documents.size
    }

    override suspend fun getIndexSize(): Long {
        return calculateIndexSize()
    }

    override fun getIndexName(): String {
        return indexOptions.indexName
    }

    override fun getIndexType(): String {
        return "in-memory"
    }

    override fun getIndexVersion(): String {
        return "1.0.0"
    }

    override suspend fun getIndexStatus(): EnhancedVectorStore.IndexStatus {
        return status
    }

    /**
     * 计算两个向量的相似度。
     *
     * @param vec1 第一个向量
     * @param vec2 第二个向量
     * @return 相似度分数
     */
    private fun calculateSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) {
            return 0.0
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        if (norm1 <= 0.0 || norm2 <= 0.0) {
            return 0.0
        }

        return dotProduct / (kotlin.math.sqrt(norm1) * kotlin.math.sqrt(norm2))
    }

    /**
     * 更新元数据索引。
     *
     * @param id 文档ID
     * @param metadata 元数据
     */
    private fun updateMetadataIndex(id: String, metadata: Map<String, String>) {
        for ((field, value) in metadata) {
            // 获取或创建字段索引
            val fieldIndex = metadataIndex.getOrPut(field) { mutableMapOf() }

            // 获取或创建值索引
            val valueIndex = fieldIndex.getOrPut(value) { mutableSetOf() }

            // 添加文档ID到值索引
            valueIndex.add(id)
        }
    }

    /**
     * 重建元数据索引。
     */
    private fun rebuildMetadataIndex() {
        metadataIndex.clear()

        for ((id, document) in documents) {
            updateMetadataIndex(id, document.metadata)
        }
    }

    /**
     * 应用元数据过滤器。
     *
     * @param filter 元数据过滤器
     * @return 匹配的文档ID集合
     */
    private fun applyMetadataFilter(filter: Map<String, Any>): Set<String> {
        if (filter.isEmpty()) {
            return documents.keys
        }

        var result: Set<String>? = null

        for ((field, value) in filter) {
            // 获取字段索引
            val fieldIndex = metadataIndex[field]

            // 如果字段不存在，返回空集合
            if (fieldIndex == null) {
                return emptySet()
            }

            // 获取匹配的文档ID
            val matchingIds = when (value) {
                is String -> {
                    fieldIndex[value]?.toSet() ?: emptySet()
                }
                is List<*> -> {
                    value.mapNotNull { it?.toString() }
                        .flatMap { fieldIndex[it]?.toSet() ?: emptySet() }
                        .toSet()
                }
                else -> {
                    fieldIndex[value.toString()]?.toSet() ?: emptySet()
                }
            }

            // 如果没有匹配的文档，返回空集合
            if (matchingIds.isEmpty()) {
                return emptySet()
            }

            // 交集
            result = if (result == null) {
                matchingIds
            } else {
                result.intersect(matchingIds)
            }

            // 如果交集为空，提前返回
            if (result.isEmpty()) {
                return emptySet()
            }
        }

        return result ?: documents.keys
    }

    /**
     * 合并搜索结果。
     *
     * @param vectorResults 向量搜索结果
     * @param keywordResults 关键词搜索结果
     * @param alpha 向量搜索权重
     * @return 合并后的搜索结果
     */
    private fun mergeSearchResults(
        vectorResults: List<SearchResult>,
        keywordResults: List<SearchResult>,
        alpha: Double
    ): List<SearchResult> {
        val resultMap = mutableMapOf<String, SearchResult>()

        // 添加向量搜索结果
        for (result in vectorResults) {
            resultMap[result.document.id] = result
        }

        // 合并关键词搜索结果
        for (result in keywordResults) {
            val id = result.document.id
            val existing = resultMap[id]

            if (existing != null) {
                // 合并分数
                val combinedScore = alpha * existing.score + (1 - alpha) * result.score
                resultMap[id] = SearchResult(existing.document, combinedScore)
            } else {
                // 添加新结果
                resultMap[id] = SearchResult(result.document, (1 - alpha) * result.score)
            }
        }

        // 排序并返回
        return resultMap.values.sortedByDescending { it.score }
    }

    /**
     * 计算索引大小。
     *
     * @return 索引大小（字节）
     */
    private fun calculateIndexSize(): Long {
        // 估算文档大小
        val documentsSize = documents.values.sumOf { document ->
            document.content.length * 2L +  // 每个字符 2 字节
                document.metadata.entries.sumOf { (key, value) ->
                    key.length * 2L + value.length * 2L
                }
        }

        // 估算向量大小
        val embeddingsSize = embeddings.values.size * indexOptions.dimensions * 4L  // 每个浮点数 4 字节

        // 估算元数据索引大小
        val metadataIndexSize = metadataIndex.entries.sumOf { (field, valueMap) ->
            field.length * 2L +
                valueMap.entries.sumOf { (value, docIds) ->
                    value.length * 2L + docIds.size * 36L  // 每个 UUID 36 字节
                }
        }

        return documentsSize + embeddingsSize + metadataIndexSize
    }
}
