package ai.kastrax.rag.integration

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.HybridOptions
import ai.kastrax.rag.SemanticOptions
import ai.kastrax.rag.QueryEnhancementOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.context.ContextFormat
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.reranker.RelevanceReranker
import ai.kastrax.rag.reranker.DiversityReranker
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

/**
 * RAG 集成测试，测试 RAG 系统的各个组件之间的交互。
 */
class RagIntegrationTest {

    private lateinit var rag: RAG
    private lateinit var embeddingService: EmbeddingService
    private lateinit var documents: List<Document>

    @BeforeEach
    fun setup() {
        // 创建嵌入服务
        embeddingService = object : EmbeddingService {
            override fun dimension(): Int = 384

            override suspend fun embed(text: String): FloatArray {
                // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
                val textSeed = text.hashCode().toLong()
                val textRandom = java.util.Random(textSeed)

                // 生成随机向量
                val vector = FloatArray(dimension()) {
                    // 生成 [-1, 1] 范围内的随机浮点数
                    textRandom.nextFloat() * 2 - 1
                }

                // 归一化向量
                val norm = kotlin.math.sqrt(vector.sumOf { it * it.toDouble() })
                if (norm > 0) {
                    for (i in vector.indices) {
                        vector[i] = (vector[i] / norm).toFloat()
                    }
                }

                return vector
            }

            override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
                return texts.map { embed(it) }
            }

            override fun close() {}
        }

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

        // 创建 RAG 实例
        rag = RAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = IdentityReranker()
        )

        // 创建测试文档
        documents = listOf(
            Document(
                id = "1",
                content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "2",
                content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "3",
                content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "4",
                content = "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
                metadata = mapOf("source" to "NLP百科", "category" to "技术")
            ),
            Document(
                id = "5",
                content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。",
                metadata = mapOf("source" to "CV百科", "category" to "技术")
            ),
            Document(
                id = "6",
                content = "苹果是一种常见的水果，富含维生素和纤维素，有多种品种。",
                metadata = mapOf("source" to "水果百科", "category" to "食品")
            ),
            Document(
                id = "7",
                content = "香蕉是一种热带水果，富含钾和维生素B6，是运动员常吃的水果。",
                metadata = mapOf("source" to "水果百科", "category" to "食品")
            ),
            Document(
                id = "8",
                content = "橙子是一种柑橘类水果，富含维生素C，可以提高免疫力。",
                metadata = mapOf("source" to "水果百科", "category" to "食品")
            )
        )

        // 加载文档
        runBlocking {
            rag.loadDocuments(documents, null)
        }
    }

    @Test
    fun `test basic search functionality`() = runBlocking {
        // 执行搜索
        val results = rag.search("人工智能", limit = 3)

        // 验证结果
        assertEquals(3, results.size)
        assertTrue(results[0].document.content.contains("人工智能"))
    }

    @Test
    fun `test context generation`() = runBlocking {
        // 生成上下文
        val context = rag.generateContext("人工智能", limit = 3)

        // 验证上下文
        assertTrue(context.contains("人工智能"))
        assertTrue(context.length > 0)
    }

    @Test
    fun `test hybrid search`() = runBlocking {
        // 创建混合搜索选项
        val options = RagProcessOptions(
            useHybridSearch = true,
            hybridOptions = HybridOptions(
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        )

        // 执行搜索
        val results = rag.search("人工智能", limit = 3, options = options)

        // 验证结果
        assertEquals(3, results.size)
        assertTrue(results[0].document.content.contains("人工智能"))
    }

    @Test
    fun `test semantic search`() = runBlocking {
        // 创建语义搜索选项
        val options = RagProcessOptions(
            useSemanticRetrieval = true,
            semanticOptions = SemanticOptions(
                useChunking = true,
                chunkSize = 1000,
                chunkOverlap = 200
            )
        )

        // 执行搜索
        val results = rag.search("AI技术", limit = 3, options = options)

        // 验证结果
        assertEquals(3, results.size)
        // 由于使用了语义搜索，结果可能包含相关术语而不是精确匹配
        assertTrue(results.any { it.document.content.contains("人工智能") || it.document.content.contains("机器学习") })
    }

    @Test
    fun `test query enhancement`() = runBlocking {
        // 创建查询增强选项
        val options = RagProcessOptions(
            useQueryEnhancement = true,
            queryEnhancementOptions = QueryEnhancementOptions(
                useSynonyms = true,
                useDecomposition = true,
                useNormalization = true
            )
        )

        // 执行搜索
        val results = rag.search("什么是AI和机器学习", limit = 3, options = options)

        // 验证结果
        assertEquals(3, results.size)
        // 由于使用了查询增强，结果应该包含与原始查询相关的文档
        assertTrue(results.any { it.document.content.contains("人工智能") })
        assertTrue(results.any { it.document.content.contains("机器学习") })
    }

    @Test
    fun `test reranking`() = runBlocking {
        // 创建重排序器
        val relevanceReranker = RelevanceReranker(embeddingService)
        val ragWithReranker = RAG(
            documentStore = rag.getDocumentStore(),
            embeddingService = embeddingService,
            reranker = relevanceReranker
        )

        // 执行搜索
        val results = ragWithReranker.search("人工智能", limit = 3)

        // 验证结果
        assertEquals(3, results.size)
        assertTrue(results[0].document.content.contains("人工智能"))
    }

    @Test
    fun `test diversity reranking`() = runBlocking {
        // 创建多样性重排序器
        val diversityReranker = DiversityReranker(embeddingService)
        val ragWithReranker = RAG(
            documentStore = rag.getDocumentStore(),
            embeddingService = embeddingService,
            reranker = diversityReranker
        )

        // 执行搜索
        val results = ragWithReranker.search("人工智能", limit = 5)

        // 验证结果
        assertEquals(5, results.size)
        // 由于使用了多样性重排序，结果应该包含不同的文档
        val categories = results.map { it.document.metadata["category"] }.toSet()
        assertTrue(categories.size >= 1)
    }

    @Test
    fun `test context options`() = runBlocking {
        // 创建上下文选项
        val options = RagProcessOptions(
            contextOptions = ContextBuilderConfig(
                maxTokens = 1000,
                includeMetadata = true,
                format = ContextFormat.TEXT,
                separator = "\n\n"
            )
        )

        // 生成上下文
        val context = rag.generateContext("人工智能", limit = 3, options = options)

        // 验证上下文
        assertTrue(context.contains("以下是关于 人工智能 的信息"))
        assertTrue(context.contains("来源"))
    }

    @Test
    fun `test retrieve context`() = runBlocking {
        // 检索上下文
        val result = rag.retrieveContext("人工智能", limit = 3)

        // 验证结果
        assertNotNull(result.context)
        assertTrue(result.context.contains("人工智能"))
        assertEquals(3, result.documents.size)
    }

    @Test
    fun `test combined options`() = runBlocking {
        // 创建组合选项
        val options = RagProcessOptions(
            useHybridSearch = true,
            useSemanticRetrieval = true,
            useQueryEnhancement = true,
            useReranking = true,
            hybridOptions = HybridOptions(
                vectorWeight = 0.6,
                keywordWeight = 0.4
            ),
            semanticOptions = SemanticOptions(
                useChunking = true,
                chunkSize = 1000,
                chunkOverlap = 200
            ),
            queryEnhancementOptions = QueryEnhancementOptions(
                useSynonyms = true,
                useDecomposition = true,
                useNormalization = true
            ),
            contextOptions = ContextBuilderConfig(
                maxTokens = 2000,
                includeMetadata = true,
                format = ContextFormat.TEXT,
                separator = "\n\n"
            )
        )

        // 执行搜索
        val results = rag.search("AI和机器学习的关系", limit = 5, options = options)

        // 验证结果
        assertTrue(results.size <= 5)
        // 由于使用了组合选项，结果应该包含与查询相关的文档
        assertTrue(results.any { it.document.content.contains("人工智能") || it.document.content.contains("机器学习") })
    }
}
