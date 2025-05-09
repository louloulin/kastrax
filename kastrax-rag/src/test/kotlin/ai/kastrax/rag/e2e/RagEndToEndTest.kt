package ai.kastrax.rag.e2e

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import ai.kastrax.store.document.Document
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File

/**
 * RAG 端到端测试，测试完整的 RAG 流程。
 * 
 * 注意：这些测试可能需要较长时间运行，默认情况下会被跳过。
 * 要运行这些测试，请设置环境变量 RUN_E2E_TESTS=true。
 */
@EnabledIfEnvironmentVariable(named = "RUN_E2E_TESTS", matches = "true")
class RagEndToEndTest {

    private lateinit var rag: RAG
    private lateinit var embeddingService: RandomEmbeddingService
    private lateinit var testDataDir: File

    @BeforeEach
    fun setup() {
        // 创建嵌入服务
        embeddingService = RandomEmbeddingService(dimensions = 384)

        // 创建向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 创建文档向量存储适配器
        val documentStore = DocumentVectorStoreAdapter(vectorStore)

        // 创建 RAG 实例
        rag = RAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = IdentityReranker()
        )

        // 创建测试数据目录
        testDataDir = createTestDataDirectory()
    }

    @Test
    fun `test complete RAG workflow`() = runBlocking {
        // 步骤 1：加载文档
        val documents = loadTestDocuments()
        rag.loadDocuments(documents)

        // 步骤 2：搜索文档
        val searchResults = rag.search("人工智能的应用", limit = 5)
        assertTrue(searchResults.isNotEmpty(), "Search should return results")

        // 步骤 3：生成上下文
        val context = rag.generateContext("人工智能的应用", limit = 5)
        assertTrue(context.isNotEmpty(), "Context should not be empty")

        // 步骤 4：检索上下文
        val retrieveResult = rag.retrieveContext("人工智能的应用", limit = 5)
        assertNotNull(retrieveResult.context, "Retrieved context should not be null")
        assertTrue(retrieveResult.documents.isNotEmpty(), "Retrieved documents should not be empty")

        // 步骤 5：模拟生成回答
        val answer = simulateAnswerGeneration(retrieveResult.context, "人工智能的应用")
        assertTrue(answer.isNotEmpty(), "Answer should not be empty")

        // 输出结果
        println("End-to-end test results:")
        println("Query: 人工智能的应用")
        println("Context: ${retrieveResult.context.take(200)}...")
        println("Answer: $answer")
    }

    @Test
    fun `test RAG with different configurations`() = runBlocking {
        // 加载文档
        val documents = loadTestDocuments()
        rag.loadDocuments(documents)

        // 测试不同的配置
        val configurations = listOf(
            RagProcessOptions(useHybridSearch = true),
            RagProcessOptions(useSemanticSearch = true),
            RagProcessOptions(useQueryEnhancement = true),
            RagProcessOptions(useReranking = true)
        )

        for (config in configurations) {
            // 搜索文档
            val searchResults = rag.search("人工智能的应用", limit = 5, options = config)
            assertTrue(searchResults.isNotEmpty(), "Search should return results with config: $config")

            // 生成上下文
            val context = rag.generateContext("人工智能的应用", limit = 5, options = config)
            assertTrue(context.isNotEmpty(), "Context should not be empty with config: $config")

            // 模拟生成回答
            val answer = simulateAnswerGeneration(context, "人工智能的应用")
            assertTrue(answer.isNotEmpty(), "Answer should not be empty with config: $config")

            // 输出结果
            println("Configuration: $config")
            println("Results count: ${searchResults.size}")
            println("Context length: ${context.length}")
            println("Answer: ${answer.take(100)}...")
            println()
        }
    }

    @Test
    fun `test RAG with multiple queries`() = runBlocking {
        // 加载文档
        val documents = loadTestDocuments()
        rag.loadDocuments(documents)

        // 测试多个查询
        val queries = listOf(
            "人工智能的定义",
            "机器学习的应用",
            "深度学习的原理",
            "自然语言处理技术",
            "计算机视觉的发展"
        )

        for (query in queries) {
            // 搜索文档
            val searchResults = rag.search(query, limit = 3)
            assertTrue(searchResults.isNotEmpty(), "Search should return results for query: $query")

            // 生成上下文
            val context = rag.generateContext(query, limit = 3)
            assertTrue(context.isNotEmpty(), "Context should not be empty for query: $query")

            // 模拟生成回答
            val answer = simulateAnswerGeneration(context, query)
            assertTrue(answer.isNotEmpty(), "Answer should not be empty for query: $query")

            // 输出结果
            println("Query: $query")
            println("Results count: ${searchResults.size}")
            println("Context length: ${context.length}")
            println("Answer: ${answer.take(100)}...")
            println()
        }
    }

    /**
     * 创建测试数据目录。
     *
     * @return 测试数据目录
     */
    private fun createTestDataDirectory(): File {
        val dir = File("build/test-data")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 加载测试文档。
     *
     * @return 文档列表
     */
    private fun loadTestDocuments(): List<Document> {
        return listOf(
            Document(
                id = "1",
                content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。人工智能的应用包括自然语言处理、计算机视觉、机器人技术和专家系统等。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "2",
                content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。机器学习的应用包括推荐系统、垃圾邮件过滤、欺诈检测和预测分析等。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "3",
                content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。深度学习的应用包括图像识别、语音识别、自然语言处理和游戏等。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "4",
                content = "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。自然语言处理的应用包括机器翻译、情感分析、文本摘要和问答系统等。",
                metadata = mapOf("source" to "NLP百科", "category" to "技术")
            ),
            Document(
                id = "5",
                content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。计算机视觉的应用包括人脸识别、物体检测、自动驾驶和医学影像分析等。",
                metadata = mapOf("source" to "CV百科", "category" to "技术")
            ),
            Document(
                id = "6",
                content = "强化学习是机器学习的一种方法，它通过与环境交互来学习如何做出决策。强化学习的应用包括游戏、机器人控制、资源管理和推荐系统等。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "7",
                content = "神经网络是一种受人脑结构启发的计算模型，由多层神经元组成。神经网络的应用包括模式识别、分类、回归和聚类等。",
                metadata = mapOf("source" to "AI百科", "category" to "技术")
            ),
            Document(
                id = "8",
                content = "大数据与人工智能密切相关，大数据提供了训练人工智能模型所需的数据。大数据和人工智能的结合应用包括预测分析、个性化推荐和异常检测等。",
                metadata = mapOf("source" to "数据科学百科", "category" to "技术")
            ),
            Document(
                id = "9",
                content = "人工智能的伦理问题包括隐私、偏见、透明度和责任等。随着人工智能技术的发展，这些伦理问题变得越来越重要。",
                metadata = mapOf("source" to "AI伦理百科", "category" to "伦理")
            ),
            Document(
                id = "10",
                content = "人工智能的未来发展趋势包括通用人工智能、人机协作、边缘计算和可解释人工智能等。这些趋势将塑造人工智能的未来。",
                metadata = mapOf("source" to "AI趋势报告", "category" to "趋势")
            )
        )
    }

    /**
     * 模拟生成回答。
     *
     * @param context 上下文
     * @param query 查询
     * @return 生成的回答
     */
    private fun simulateAnswerGeneration(context: String, query: String): String {
        // 在实际应用中，这里会调用 LLM 生成回答
        // 这里我们只是模拟一个简单的回答生成过程
        val keywords = query.split(" ")
        val sentences = context.split(". ")
        val relevantSentences = sentences.filter { sentence ->
            keywords.any { keyword -> sentence.contains(keyword, ignoreCase = true) }
        }

        return if (relevantSentences.isNotEmpty()) {
            "根据提供的信息，" + relevantSentences.joinToString(". ") + "。"
        } else {
            "抱歉，我无法根据提供的上下文回答这个问题。"
        }
    }
}
