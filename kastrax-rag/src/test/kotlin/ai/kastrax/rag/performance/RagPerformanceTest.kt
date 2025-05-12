package ai.kastrax.rag.performance

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.HybridOptions
import ai.kastrax.rag.SemanticOptions
import ai.kastrax.rag.QueryEnhancementOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.benchmark.RagBenchmarkTool
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.vector.memory.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.system.measureTimeMillis

/**
 * RAG 性能测试，测试 RAG 系统的性能。
 *
 * 注意：这些测试可能需要较长时间运行，默认情况下会被跳过。
 * 要运行这些测试，请设置环境变量 RUN_PERFORMANCE_TESTS=true。
 */
@EnabledIfEnvironmentVariable(named = "RUN_PERFORMANCE_TESTS", matches = "true")
class RagPerformanceTest {

    private lateinit var rag: RAG
    private lateinit var embeddingService: EmbeddingService
    private lateinit var benchmarkTool: RagBenchmarkTool
    private lateinit var documents: List<Document>
    private lateinit var queries: List<String>

    @BeforeEach
    fun setup() {
        // 创建嵌入服务
        embeddingService = object : EmbeddingService() {
            override val dimension: Int = 384

            override suspend fun embed(text: String): FloatArray {
                // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
                val textSeed = text.hashCode().toLong()
                val textRandom = java.util.Random(textSeed)

                // 生成随机向量
                val vector = FloatArray(dimension) {
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

        // 创建基准测试工具
        benchmarkTool = RagBenchmarkTool(rag)

        // 创建测试文档
        documents = generateTestDocuments(100)

        // 创建测试查询
        queries = listOf(
            "人工智能的定义",
            "机器学习的应用",
            "深度学习的原理",
            "自然语言处理技术",
            "计算机视觉的发展",
            "强化学习的优势",
            "神经网络的结构",
            "大数据与人工智能",
            "人工智能的伦理问题",
            "人工智能的未来发展"
        )

        // 加载文档
        runBlocking {
            rag.loadDocuments(documents, null)
        }
    }

    @Test
    fun `test search performance`() = runBlocking {
        // 执行搜索性能测试
        val times = mutableListOf<Long>()

        repeat(10) {
            val time = measureTimeMillis {
                rag.search("人工智能", limit = 5)
            }
            times.add(time)
        }

        // 计算平均时间
        val averageTime = times.average()
        println("Average search time: ${averageTime}ms")

        // 验证性能
        assertTrue(averageTime < 1000, "Search should complete in less than 1000ms")
    }

    @Test
    fun `test context generation performance`() = runBlocking {
        // 执行上下文生成性能测试
        val times = mutableListOf<Long>()

        repeat(10) {
            val time = measureTimeMillis {
                rag.generateContext("人工智能", limit = 5)
            }
            times.add(time)
        }

        // 计算平均时间
        val averageTime = times.average()
        println("Average context generation time: ${averageTime}ms")

        // 验证性能
        assertTrue(averageTime < 1000, "Context generation should complete in less than 1000ms")
    }

    @Test
    fun `test benchmark tool`() = runBlocking {
        // 执行基准测试
        val summary = benchmarkTool.runBenchmark(queries, limit = 5)

        // 输出结果
        println("Benchmark results:")
        println("Average retrieval time: ${summary.averageRetrievalTime}ms")
        println("Average context generation time: ${summary.averageContextGenerationTime}ms")
        println("Average total time: ${summary.averageTotalTime}ms")
        println("Average result count: ${summary.averageResultCount}")
        println("Average context length: ${summary.averageContextLength}")

        // 验证性能
        assertTrue(summary.averageTotalTime < 2000, "Total processing should complete in less than 2000ms")
    }

    @Test
    fun `test configuration comparison`() = runBlocking {
        // 创建配置列表
        val configurations = listOf(
            RagProcessOptions(),
            RagProcessOptions(
                useHybridSearch = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                )
            ),
            RagProcessOptions(
                useSemanticRetrieval = true,
                semanticOptions = SemanticOptions(
                    useChunking = true,
                    chunkSize = 1000,
                    chunkOverlap = 200
                )
            ),
            RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = true,
                    useDecomposition = true,
                    useNormalization = true
                )
            )
        )

        // 执行配置比较
        val results = benchmarkTool.compareConfigurations(
            queries = queries.take(3),
            configurations = configurations
        )

        // 输出结果
        println("Configuration comparison results:")
        results.forEach { (config, summary) ->
            println("Configuration: $config")
            println("Average total time: ${summary.averageTotalTime}ms")
            println("Average result count: ${summary.averageResultCount}")
            println("Average context length: ${summary.averageContextLength}")
            println()
        }

        // 验证结果
        assertTrue(results.isNotEmpty(), "Should have comparison results")
    }

    @Test
    fun `test parallel benchmark`() = runBlocking {
        // 执行并行基准测试
        val summary = benchmarkTool.runBenchmark(queries, limit = 5, parallel = true)

        // 输出结果
        println("Parallel benchmark results:")
        println("Average retrieval time: ${summary.averageRetrievalTime}ms")
        println("Average context generation time: ${summary.averageContextGenerationTime}ms")
        println("Average total time: ${summary.averageTotalTime}ms")

        // 验证性能
        assertTrue(summary.averageTotalTime < 2000, "Total processing should complete in less than 2000ms")
    }

    @Test
    fun `test document loading performance`() = runBlocking {
        // 创建大量文档
        val largeDocumentSet = generateTestDocuments(1000)

        // 测量加载时间
        val loadTime = measureTimeMillis {
            rag.loadDocuments(largeDocumentSet, null)
        }

        println("Document loading time for 1000 documents: ${loadTime}ms")

        // 验证性能
        assertTrue(loadTime < 10000, "Loading 1000 documents should complete in less than 10000ms")
    }

    /**
     * 生成测试文档。
     *
     * @param count 文档数量
     * @return 文档列表
     */
    private fun generateTestDocuments(count: Int): List<Document> {
        val documents = mutableListOf<Document>()
        val categories = listOf("技术", "科学", "教育", "健康", "商业")
        val sources = listOf("百科全书", "学术论文", "新闻", "博客", "书籍")

        for (i in 1..count) {
            val category = categories[i % categories.size]
            val source = sources[i % sources.size]
            val content = when (category) {
                "技术" -> generateTechContent(i)
                "科学" -> generateScienceContent(i)
                "教育" -> generateEducationContent(i)
                "健康" -> generateHealthContent(i)
                else -> generateBusinessContent(i)
            }

            documents.add(
                Document(
                    id = i.toString(),
                    content = content,
                    metadata = mapOf("category" to category, "source" to source)
                )
            )
        }

        return documents
    }

    private fun generateTechContent(index: Int): String {
        val topics = listOf(
            "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
            "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
            "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。"
        )
        return topics[index % topics.size] + " 这是文档 #$index 的内容。"
    }

    private fun generateScienceContent(index: Int): String {
        val topics = listOf(
            "量子力学是物理学的一个基本理论，它描述了物质和能量在最小尺度上的行为。",
            "相对论是爱因斯坦提出的物理学理论，它改变了我们对时间、空间和引力的理解。",
            "基因组学是研究生物体基因组的科学，它帮助我们理解生物的遗传信息。",
            "天体物理学是研究宇宙中天体的物理性质的科学，包括恒星、行星和星系。",
            "气候科学研究地球气候系统的长期模式和变化，包括全球变暖的影响。"
        )
        return topics[index % topics.size] + " 这是文档 #$index 的内容。"
    }

    private fun generateEducationContent(index: Int): String {
        val topics = listOf(
            "教育心理学研究学习过程和教育环境中的心理因素，帮助改进教学方法。",
            "课程设计是创建有效教育课程的过程，包括确定目标、内容和评估方法。",
            "教育技术是使用技术工具和资源来改进教学和学习的实践。",
            "特殊教育专注于满足有特殊需求的学生的教育需求，包括学习障碍和天赋学生。",
            "教育政策是指导教育系统运作的原则和指导方针，由政府和教育机构制定。"
        )
        return topics[index % topics.size] + " 这是文档 #$index 的内容。"
    }

    private fun generateHealthContent(index: Int): String {
        val topics = listOf(
            "营养学是研究食物和营养物质如何影响健康和疾病的科学。",
            "心理健康包括情绪、心理和社会福祉，影响我们的思考、感受和行为。",
            "运动医学专注于预防和治疗与运动和锻炼相关的伤害和疾病。",
            "公共卫生关注保护和改善社区和人口健康的措施。",
            "慢性疾病管理是指管理长期健康状况的策略，如糖尿病和心脏病。"
        )
        return topics[index % topics.size] + " 这是文档 #$index 的内容。"
    }

    private fun generateBusinessContent(index: Int): String {
        val topics = listOf(
            "市场营销是识别和满足客户需求的过程，包括产品推广和销售策略。",
            "财务管理涉及规划、组织、控制和监控组织的财务资源。",
            "人力资源管理是管理组织中的人员的策略和实践，包括招聘和培训。",
            "战略管理是制定和实施组织目标和计划的过程，以实现长期成功。",
            "企业家精神是识别机会并创建新企业的能力，涉及创新和风险承担。"
        )
        return topics[index % topics.size] + " 这是文档 #$index 的内容。"
    }
}
