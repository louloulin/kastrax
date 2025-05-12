package ai.kastrax.rag.multimodal

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.vector.memory.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

/**
 * 多模态 RAG 测试类。
 */
class MultimodalRagTest {

    private lateinit var embeddingService: MultimodalEmbeddingService
    private lateinit var multimodalRag: MultimodalRAG
    private lateinit var documents: List<MultimodalDocument>

    @BeforeEach
    fun setup() = runBlocking {
        // 创建模拟的多模态嵌入服务
        embeddingService = mock(MultimodalEmbeddingService::class.java)

        // 设置嵌入服务的行为
        `when`(embeddingService.dimension).thenReturn(384)
        whenever(embeddingService.embed(any())).thenReturn(FloatArray(384) { 0f })
        whenever(embeddingService.embedBatch(any())).thenReturn(List(5) { FloatArray(384) { 0f } })
        whenever(embeddingService.embedImage(any())).thenReturn(FloatArray(384) { 0f })
        whenever(embeddingService.embedAudio(any())).thenReturn(FloatArray(384) { 0f })
        whenever(embeddingService.embedVideo(any())).thenReturn(FloatArray(384) { 0f })
        whenever(embeddingService.embedMultimodalDocument(any())).thenReturn(FloatArray(384) { 0f })
        whenever(embeddingService.embedMultimodalDocuments(any())).thenReturn(List(5) { FloatArray(384) { 0f } })

        // 创建向量存储
        val vectorStore = InMemoryVectorStore(dimension = 384)

        // 创建文档向量存储
        val documentStore = object : DocumentVectorStore {
            override val dimension: Int = vectorStore.dimension

            override fun getVectorStore() = vectorStore

            override suspend fun addDocuments(documents: List<Document>, embeddingService: EmbeddingService): Boolean {
                val embeddings = embeddingService.embedBatch(documents.map { it.content })
                val ids = documents.map { it.id }
                val metadataList = documents.map { it.metadata }
                // 使用 VectorStore 的 upsert 方法添加向量
                val indexName = "default"
                vectorStore.createIndex(indexName, dimension, ai.kastrax.store.SimilarityMetric.COSINE)
                vectorStore.upsert(indexName, embeddings, metadataList, ids)
                return true
            }

            override suspend fun addDocuments(documents: List<Document>): Boolean {
                return true // 简化实现
            }

            override suspend fun deleteDocuments(ids: List<String>): Boolean {
                // 使用 VectorStore 的 deleteVectors 方法删除向量
                val indexName = "default"
                return vectorStore.deleteVectors(indexName, ids)
            }

            override suspend fun similaritySearch(query: String, embeddingService: EmbeddingService, limit: Int): List<DocumentSearchResult> {
                val embedding = embeddingService.embed(query)
                // 使用 VectorStore 的 query 方法查询向量
                val indexName = "default"
                val results = vectorStore.query(indexName, embedding, limit, null, false)
                return results.map { result ->
                    val metadata = result.metadata ?: emptyMap()
                    val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                    DocumentSearchResult(document, result.score)
                }
            }

            override suspend fun similaritySearch(embedding: FloatArray, limit: Int): List<DocumentSearchResult> {
                // 使用 VectorStore 的 query 方法查询向量
                val indexName = "default"
                val results = vectorStore.query(indexName, embedding, limit, null, false)
                return results.map { result ->
                    val metadata = result.metadata ?: emptyMap()
                    val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                    DocumentSearchResult(document, result.score)
                }
            }

            override suspend fun similaritySearchWithFilter(embedding: FloatArray, filter: Map<String, Any>, limit: Int): List<DocumentSearchResult> {
                // 使用 VectorStore 的 query 方法查询向量
                val indexName = "default"
                val results = vectorStore.query(indexName, embedding, limit, filter, false)
                return results.map { result ->
                    val metadata = result.metadata ?: emptyMap()
                    val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                    DocumentSearchResult(document, result.score)
                }
            }

            override suspend fun keywordSearch(keywords: List<String>, limit: Int): List<DocumentSearchResult> {
                return emptyList() // 简化实现
            }

            override suspend fun metadataSearch(filter: Map<String, Any>, limit: Int): List<DocumentSearchResult> {
                return emptyList() // 简化实现
            }
        }

        // 创建多模态 RAG 实例
        multimodalRag = MultimodalRAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = IdentityReranker()
        )

        // 创建测试文档
        documents = listOf(
            MultimodalDocument(
                id = "1",
                content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
                mediaUrls = listOf("https://example.com/images/ai.jpg"),
                mediaType = MultimodalDocumentType.IMAGE,
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            MultimodalDocument(
                id = "2",
                content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
                mediaUrls = listOf("https://example.com/images/machine_learning.jpg"),
                mediaType = MultimodalDocumentType.IMAGE,
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            MultimodalDocument(
                id = "3",
                content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                mediaUrls = listOf("https://example.com/images/deep_learning.jpg"),
                mediaType = MultimodalDocumentType.IMAGE,
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            MultimodalDocument(
                id = "4",
                content = "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
                mediaUrls = listOf("https://example.com/audio/nlp_intro.mp3"),
                mediaType = MultimodalDocumentType.AUDIO,
                metadata = mapOf("source" to "NLP百科", "category" to "技术")
            ),
            MultimodalDocument(
                id = "5",
                content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。",
                mediaUrls = listOf("https://example.com/videos/computer_vision.mp4"),
                mediaType = MultimodalDocumentType.VIDEO,
                metadata = mapOf("source" to "CV百科", "category" to "技术")
            )
        )
    }

    @Test
    fun `test loading multimodal documents`() = runBlocking {
        // 加载多模态文档
        val result = multimodalRag.loadMultimodalDocuments(documents)

        // 验证结果
        assertTrue(result)
    }

    @Test
    fun `test multimodal search with text only`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 执行纯文本查询
        val results = multimodalRag.multimodalSearch("人工智能", limit = 3)

        // 验证结果
        assertEquals(3, results.size)
    }

    @Test
    fun `test multimodal search with text and image`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 执行多模态查询（文本 + 图像）
        val results = multimodalRag.multimodalSearch(
            textQuery = "深度学习",
            imageUrl = "https://example.com/images/neural_network.jpg",
            limit = 3
        )

        // 验证结果
        assertEquals(3, results.size)
    }

    @Test
    fun `test multimodal search with text and audio`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 执行多模态查询（文本 + 音频）
        val results = multimodalRag.multimodalSearch(
            textQuery = "自然语言处理",
            audioUrl = "https://example.com/audio/speech.mp3",
            limit = 3
        )

        // 验证结果
        assertEquals(3, results.size)
    }

    @Test
    fun `test multimodal search with text and video`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 执行多模态查询（文本 + 视频）
        val results = multimodalRag.multimodalSearch(
            textQuery = "计算机视觉",
            videoUrl = "https://example.com/videos/vision.mp4",
            limit = 3
        )

        // 验证结果
        assertEquals(3, results.size)
    }

    @Test
    fun `test multimodal search with all modalities`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 执行多模态查询（文本 + 图像 + 音频 + 视频）
        val results = multimodalRag.multimodalSearch(
            textQuery = "人工智能技术",
            imageUrl = "https://example.com/images/ai_tech.jpg",
            audioUrl = "https://example.com/audio/ai_tech.mp3",
            videoUrl = "https://example.com/videos/ai_tech.mp4",
            limit = 3
        )

        // 验证结果
        assertEquals(3, results.size)
    }

    @Test
    fun `test generate multimodal context`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 生成多模态上下文
        val context = multimodalRag.generateMultimodalContext(
            textQuery = "机器学习和深度学习的区别",
            imageUrl = "https://example.com/images/ml_vs_dl.jpg",
            limit = 3
        )

        // 验证结果
        assertNotNull(context)
        assertTrue(context.isNotEmpty())
    }

    @Test
    fun `test retrieve multimodal context`() = runBlocking {
        // 加载多模态文档
        multimodalRag.loadMultimodalDocuments(documents)

        // 检索多模态上下文
        val result = multimodalRag.retrieveMultimodalContext(
            textQuery = "计算机视觉的应用",
            videoUrl = "https://example.com/videos/cv_applications.mp4",
            limit = 3
        )

        // 验证结果
        assertNotNull(result)
        assertNotNull(result.context)
        assertTrue(result.context.isNotEmpty())
        assertEquals(3, result.documents.size)
    }
}
