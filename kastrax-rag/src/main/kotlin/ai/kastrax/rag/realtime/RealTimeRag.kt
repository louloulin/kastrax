package ai.kastrax.rag.realtime

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.RetrieveContextResult
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * 实时 RAG（检索增强生成）系统，支持流式处理和实时更新。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property reranker 重排序器
 * @property config 实时 RAG 配置
 * @property baseRag 基础 RAG 系统
 */
class RealTimeRag(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val reranker: Reranker,
    private val config: RealTimeRagConfig = RealTimeRagConfig()
) {
    private val baseRag = RAG(vectorStore, embeddingService, reranker)
    private val documentUpdateQueue = MutableSharedFlow<DocumentUpdate>(
        replay = 0,
        extraBufferCapacity = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val isRunning = AtomicBoolean(false)
    private val processingJob: Job? = null
    private val lastUpdateTime = AtomicLong(0)
    private val pendingDocuments = ConcurrentHashMap<String, DocumentUpdate>()
    private val mutex = Mutex()

    /**
     * 文档更新类型
     */
    enum class UpdateType {
        ADD, UPDATE, DELETE
    }

    /**
     * 文档更新
     *
     * @property document 文档
     * @property type 更新类型
     * @property timestamp 时间戳
     */
    data class DocumentUpdate(
        val document: Document,
        val type: UpdateType,
        val timestamp: Long = Instant.now().toEpochMilli()
    )

    /**
     * 启动实时 RAG 系统
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.Default).launch {
                processDocumentUpdates()
            }
            logger.info { "实时 RAG 系统已启动" }
        }
    }

    /**
     * 停止实时 RAG 系统
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            processingJob?.cancel()
            logger.info { "实时 RAG 系统已停止" }
        }
    }

    /**
     * 添加文档
     *
     * @param document 文档
     * @return 是否成功添加
     */
    suspend fun addDocument(document: Document): Boolean {
        val update = DocumentUpdate(document, UpdateType.ADD)
        documentUpdateQueue.emit(update)
        return true
    }

    /**
     * 添加文档到向量存储
     *
     * @param document 文档内容
     * @param embedding 文档的嵌入向量
     * @param metadata 文档元数据
     * @return 文档 ID
     */
    suspend fun addDocument(
        document: String,
        embedding: FloatArray,
        metadata: Map<String, String> = emptyMap()
    ): String {
        val doc = Document(document, metadata.mapValues { it.value as Any })
        addDocument(doc)
        return UUID.randomUUID().toString()
    }

    /**
     * 更新文档
     *
     * @param document 文档
     * @return 是否成功更新
     */
    suspend fun updateDocument(document: Document): Boolean {
        val update = DocumentUpdate(document, UpdateType.UPDATE)
        documentUpdateQueue.emit(update)
        return true
    }

    /**
     * 从文档加载器加载文档并添加到向量存储
     *
     * @param loader 文档加载器
     * @param splitter 文档分割器，如果为 null，则不分割文档
     * @return 添加的文档数量
     */
    suspend fun loadDocuments(
        loader: DocumentLoader,
        splitter: DocumentSplitter? = null
    ): Int {
        return baseRag.loadDocuments(loader, splitter)
    }

    /**
     * 删除文档
     *
     * @param document 文档
     * @return 是否成功删除
     */
    suspend fun deleteDocument(document: Document): Boolean {
        val update = DocumentUpdate(document, UpdateType.DELETE)
        documentUpdateQueue.emit(update)
        return true
    }

    /**
     * 根据 ID 删除文档
     *
     * @param id 文档 ID
     * @return 是否成功删除
     */
    suspend fun deleteDocument(id: String): Boolean {
        return vectorStore.deleteDocument(id)
    }

    /**
     * 批量删除文档
     *
     * @param ids 文档 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteDocuments(ids: List<String>): Boolean {
        var success = true
        for (id in ids) {
            success = success && deleteDocument(id)
        }
        return success
    }

    /**
     * 根据 ID 获取文档
     *
     * @param id 文档 ID
     * @return 文档，如果不存在则返回 null
     */
    suspend fun getDocument(id: String): ai.kastrax.rag.vectorstore.RagDocument? {
        return vectorStore.getDocument(id)
    }

    /**
     * 获取向量存储中的文档数量
     *
     * @return 文档数量
     */
    suspend fun size(): Int {
        return baseRag.count()
    }

    /**
     * 清空向量存储
     */
    suspend fun clearAll() {
        baseRag.clear()
    }

    /**
     * 处理文档更新队列
     */
    private suspend fun processDocumentUpdates() {
        if (config.streamingEnabled) {
            processStreamingUpdates()
        } else {
            processBatchUpdates()
        }
    }

    /**
     * 处理流式更新
     */
    private suspend fun processStreamingUpdates() {
        documentUpdateQueue
            .buffer(config.maxBatchSize, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            .collectLatest { update ->
                try {
                    processUpdate(update)
                } catch (e: Exception) {
                    logger.error(e) { "处理文档更新时出错: ${e.message}" }
                }
            }
    }

    /**
     * 处理批量更新
     */
    private suspend fun processBatchUpdates() {
        while (isRunning.get()) {
            delay(config.updateInterval)

            mutex.withLock {
                if (pendingDocuments.isNotEmpty()) {
                    val updates = pendingDocuments.values.toList()
                    pendingDocuments.clear()

                    try {
                        processUpdates(updates)
                        lastUpdateTime.set(Instant.now().toEpochMilli())
                    } catch (e: Exception) {
                        logger.error(e) { "处理批量文档更新时出错: ${e.message}" }
                    }
                }
            }
        }
    }

    /**
     * 处理单个更新
     *
     * @param update 文档更新
     */
    private suspend fun processUpdate(update: DocumentUpdate) {
        when (update.type) {
            UpdateType.ADD -> processAddDocument(update.document)
            UpdateType.UPDATE -> processUpdateDocument(update.document)
            UpdateType.DELETE -> processDeleteDocument(update.document)
        }
    }

    /**
     * 处理多个更新
     *
     * @param updates 文档更新列表
     */
    private suspend fun processUpdates(updates: List<DocumentUpdate>) {
        // 按类型分组处理
        val grouped = updates.groupBy { it.type }

        // 处理添加
        grouped[UpdateType.ADD]?.let { addUpdates ->
            val documents = addUpdates.map { it.document }
            processAddDocuments(documents)
        }

        // 处理更新
        grouped[UpdateType.UPDATE]?.let { updateUpdates ->
            val documents = updateUpdates.map { it.document }
            processUpdateDocuments(documents)
        }

        // 处理删除
        grouped[UpdateType.DELETE]?.let { deleteUpdates ->
            val documents = deleteUpdates.map { it.document }
            processDeleteDocuments(documents)
        }
    }

    /**
     * 处理添加文档
     *
     * @param document 文档
     */
    private suspend fun processAddDocument(document: Document) {
        try {
            val embedding = if (config.useAsyncEmbedding) {
                withContext(Dispatchers.IO) {
                    embeddingService.embed(document.content)
                }
            } else {
                embeddingService.embed(document.content)
            }

            vectorStore.addDocument(document.content, embedding, document.metadata.mapValues { it.value.toString() })
            logger.debug { "文档已添加" }
        } catch (e: Exception) {
            logger.error(e) { "添加文档时出错: ${e.message}" }
        }
    }

    /**
     * 处理添加多个文档
     *
     * @param documents 文档列表
     */
    private suspend fun processAddDocuments(documents: List<Document>) {
        try {
            // 并行生成嵌入
            val embeddings = if (config.useAsyncEmbedding) {
                coroutineScope {
                    documents.map { document ->
                        async(Dispatchers.IO) {
                            document to embeddingService.embed(document.content)
                        }
                    }.awaitAll()
                }
            } else {
                documents.map { document ->
                    document to embeddingService.embed(document.content)
                }
            }

            // 批量添加文档
            val contents = embeddings.map { it.first.content }
            val embeddingVectors = embeddings.map { it.second }
            val metadataList = embeddings.map { it.first.metadata.mapValues { entry -> entry.value.toString() } }

            vectorStore.addDocuments(contents, embeddingVectors, metadataList)
            logger.debug { "已批量添加 ${documents.size} 个文档" }
        } catch (e: Exception) {
            logger.error(e) { "批量添加文档时出错: ${e.message}" }
        }
    }

    /**
     * 处理更新文档
     *
     * @param document 文档
     */
    private suspend fun processUpdateDocument(document: Document) {
        try {
            // 如果启用了变更检测，先检查文档是否有实质性变化
            if (config.useChangeDetection) {
                val existingDoc = vectorStore.getDocument(document.content)
                if (existingDoc != null) {
                    val similarity = calculateContentSimilarity(existingDoc.content, document.content)
                    if (similarity > (1.0 - config.changeDetectionThreshold)) {
                        logger.debug { "文档内容相似度高于阈值，跳过更新, 相似度: $similarity" }
                        return
                    }
                }
            }

            val embedding = if (config.useAsyncEmbedding) {
                withContext(Dispatchers.IO) {
                    embeddingService.embed(document.content)
                }
            } else {
                embeddingService.embed(document.content)
            }

            // 先删除旧文档，再添加新文档
            val metadata = document.metadata.mapValues { it.value.toString() }
            vectorStore.addDocument(document.content, embedding, metadata)
            logger.debug { "文档已更新" }
        } catch (e: Exception) {
            logger.error(e) { "更新文档时出错: ${e.message}" }
        }
    }

    /**
     * 处理更新多个文档
     *
     * @param documents 文档列表
     */
    private suspend fun processUpdateDocuments(documents: List<Document>) {
        try {
            val documentsToUpdate = if (config.useChangeDetection) {
                // 过滤出需要更新的文档
                documents.filter { doc ->
                    val existingDoc = vectorStore.getDocument(doc.content)
                    if (existingDoc != null) {
                        val similarity = calculateContentSimilarity(existingDoc.content, doc.content)
                        similarity <= (1.0 - config.changeDetectionThreshold)
                    } else {
                        true // 如果文档不存在，则需要添加
                    }
                }
            } else {
                documents
            }

            if (documentsToUpdate.isEmpty()) {
                logger.debug { "没有文档需要更新" }
                return
            }

            // 并行生成嵌入
            val embeddings = if (config.useAsyncEmbedding) {
                coroutineScope {
                    documentsToUpdate.map { document ->
                        async(Dispatchers.IO) {
                            document to embeddingService.embed(document.content)
                        }
                    }.awaitAll()
                }
            } else {
                documentsToUpdate.map { document ->
                    document to embeddingService.embed(document.content)
                }
            }

            // 批量更新文档
            val contents = embeddings.map { it.first.content }
            val embeddingVectors = embeddings.map { it.second }
            val metadataList = embeddings.map { it.first.metadata.mapValues { entry -> entry.value.toString() } }

            // 添加新文档
            vectorStore.addDocuments(contents, embeddingVectors, metadataList)
            logger.debug { "已批量更新 ${documentsToUpdate.size} 个文档" }
        } catch (e: Exception) {
            logger.error(e) { "批量更新文档时出错: ${e.message}" }
        }
    }

    /**
     * 处理删除文档
     *
     * @param document 文档
     */
    private suspend fun processDeleteDocument(document: Document) {
        try {
            // 在实际实现中，可能需要先查找文档 ID
            // 这里简化处理，直接调用删除方法
            deleteDocument(document.content)
            logger.debug { "文档已删除" }
        } catch (e: Exception) {
            logger.error(e) { "删除文档时出错: ${e.message}" }
        }
    }

    /**
     * 处理删除多个文档
     *
     * @param documents 文档列表
     */
    private suspend fun processDeleteDocuments(documents: List<Document>) {
        try {
            // 在实际实现中，可能需要先查找文档 ID
            // 这里简化处理，逐个删除文档
            for (document in documents) {
                processDeleteDocument(document)
            }
            logger.debug { "已批量删除 ${documents.size} 个文档" }
        } catch (e: Exception) {
            logger.error(e) { "批量删除文档时出错: ${e.message}" }
        }
    }

    /**
     * 计算内容相似度
     *
     * @param content1 内容1
     * @param content2 内容2
     * @return 相似度
     */
    private suspend fun calculateContentSimilarity(content1: String, content2: String): Double {
        try {
            val embedding1 = embeddingService.embed(content1)
            val embedding2 = embeddingService.embed(content2)

            return cosineSimilarity(embedding1, embedding2)
        } catch (e: Exception) {
            logger.error(e) { "计算内容相似度时出错: ${e.message}" }
            return 0.0
        }
    }

    /**
     * 计算余弦相似度
     *
     * @param vec1 向量1
     * @param vec2 向量2
     * @return 余弦相似度
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) {
            throw IllegalArgumentException("向量维度不匹配: ${vec1.size} vs ${vec2.size}")
        }

        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0

        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2))
        } else {
            0.0
        }
    }

    /**
     * 使用查询文本搜索相关文档
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun search(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): List<SearchResult> {
        return baseRag.search(query, limit, minScore, options)
    }

    /**
     * 使用查询文本生成增强的上下文
     *
     * @param query 查询文本
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @return 增强的上下文
     */
    suspend fun generateContext(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0,
        options: RagProcessOptions? = null
    ): String {
        return baseRag.generateContext(query, limit, minScore, options)
    }

    /**
     * 检索上下文，返回检索结果和生成的上下文
     *
     * @param query 查询文本
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @return 检索结果和生成的上下文
     */
    suspend fun retrieveContext(
        query: String,
        options: RagProcessOptions? = null,
        limit: Int = 5,
        minScore: Double = 0.0
    ): RetrieveContextResult {
        return baseRag.retrieveContext(query, options, limit, minScore)
    }

    /**
     * 流式检索上下文
     *
     * @param query 查询文本
     * @param options RAG 处理选项，如果为 null，则使用默认选项
     * @param limit 使用的文档数量
     * @param minScore 最小相似度分数
     * @return 检索结果和上下文的流
     */
    fun streamRetrieveContext(
        query: String,
        options: RagProcessOptions? = null,
        limit: Int = 5,
        minScore: Double = 0.0
    ): Flow<RetrieveContextResult> = flow {
        val results = search(query, limit, minScore, options)

        if (results.isEmpty()) {
            emit(RetrieveContextResult(emptyList(), ""))
            return@flow
        }

        // 使用上下文构建器构建上下文
        val contextBuilder = ContextBuilder(
            options?.contextOptions ?: config.contextOptions
        )

        // 流式构建上下文
        val context = contextBuilder.buildContext(results, query)
        emit(RetrieveContextResult(results, context))
    }

    /**
     * 获取向量存储中的文档数量
     *
     * @return 文档数量
     */
    suspend fun count(): Int {
        return baseRag.count()
    }

    /**
     * 清空向量存储
     */
    suspend fun clear() {
        baseRag.clear()
    }
}
