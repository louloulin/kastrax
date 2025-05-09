package ai.kastrax.rag.examples

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.HybridOptions
import ai.kastrax.rag.SemanticOptions
import ai.kastrax.rag.QueryEnhancementOptions
import ai.kastrax.rag.ContextOptions
import ai.kastrax.rag.benchmark.RagBenchmarkTool
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.evaluation.RagEvaluationTool
import ai.kastrax.rag.optimization.RagOptimizationTool
import ai.kastrax.rag.reranker.DiversityReranker
import ai.kastrax.rag.reranker.RelevanceReranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import ai.kastrax.store.document.Document
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

/**
 * 高级 RAG 示例，展示 RAG 系统的高级功能。
 */
fun main() = runBlocking {
    println("高级 RAG 示例")
    println("===========")

    // 创建嵌入服务
    val embeddingService = RandomEmbeddingService(dimensions = 384)
    println("创建了嵌入服务，维度: ${embeddingService.dimension()}")

    // 创建向量存储
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
    println("创建了向量存储")

    // 创建文档向量存储适配器
    val documentStore = DocumentVectorStoreAdapter(vectorStore)

    // 创建重排序器
    val diversityReranker = DiversityReranker(embeddingService)
    println("创建了多样性重排序器")

    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = diversityReranker,
        defaultOptions = RagProcessOptions(
            useHybridSearch = true,
            useQueryEnhancement = true,
            hybridOptions = HybridOptions(
                vectorWeight = 0.7,
                keywordWeight = 0.3
            ),
            queryEnhancementOptions = QueryEnhancementOptions(
                useMultiQuery = true,
                useQueryDecomposition = true
            )
        )
    )
    println("创建了 RAG 实例")

    // 创建评估工具
    val evaluationTool = RagEvaluationTool(rag)
    println("创建了评估工具")

    // 创建优化工具
    val optimizationTool = RagOptimizationTool(rag, evaluationTool)
    println("创建了优化工具")

    // 创建基准测试工具
    val benchmarkTool = RagBenchmarkTool(rag)
    println("创建了基准测试工具")

    // 创建测试文档
    val documents = createTestDocuments()
    println("创建了 ${documents.size} 个测试文档")

    // 加载文档
    val loadTime = measureTimeMillis {
        rag.loadDocuments(documents)
    }
    println("加载文档耗时: ${loadTime}ms")

    // 测试查询
    val queries = listOf(
        "人工智能的应用",
        "机器学习与深度学习的区别",
        "自然语言处理的技术",
        "计算机视觉的发展历程",
        "强化学习的原理"
    )

    println("\n测试不同的 RAG 配置:")
    val configurations = listOf(
        RagProcessOptions(
            useHybridSearch = true,
            hybridOptions = HybridOptions(
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        ),
        RagProcessOptions(
            useSemanticSearch = true,
            semanticOptions = SemanticOptions(
                expandQuery = true,
                useSemanticClustering = true
            )
        ),
        RagProcessOptions(
            useQueryEnhancement = true,
            queryEnhancementOptions = QueryEnhancementOptions(
                useMultiQuery = true,
                useQueryDecomposition = true
            )
        ),
        RagProcessOptions(
            useReranking = true,
            reranker = RelevanceReranker(embeddingService)
        )
    )

    for ((index, config) in configurations.withIndex()) {
        println("\n配置 ${index + 1}:")
        println(config)

        val query = queries[index % queries.size]
        println("查询: $query")

        val searchTime = measureTimeMillis {
            val results = rag.search(query, limit = 3, options = config)
            println("结果数量: ${results.size}")
            results.forEachIndexed { i, result ->
                println("${i + 1}. ${result.document.content.take(100)}...")
                println("   分数: ${result.score}")
                println("   来源: ${result.document.metadata["source"]}")
            }
        }
        println("搜索耗时: ${searchTime}ms")

        val contextTime = measureTimeMillis {
            val context = rag.generateContext(query, limit = 3, options = config)
            println("上下文长度: ${context.length}")
            println("上下文预览: ${context.take(200)}...")
        }
        println("上下文生成耗时: ${contextTime}ms")
    }

    println("\n运行基准测试:")
    val benchmarkSummary = benchmarkTool.runBenchmark(
        queries = queries,
        limit = 3,
        parallel = false
    )
    println("基准测试结果:")
    println("平均检索时间: ${benchmarkSummary.averageRetrievalTime}ms")
    println("平均上下文生成时间: ${benchmarkSummary.averageContextGenerationTime}ms")
    println("平均总时间: ${benchmarkSummary.averageTotalTime}ms")
    println("平均结果数量: ${benchmarkSummary.averageResultCount}")
    println("平均上下文长度: ${benchmarkSummary.averageContextLength}")

    println("\n运行并行基准测试:")
    val parallelBenchmarkSummary = benchmarkTool.runBenchmark(
        queries = queries,
        limit = 3,
        parallel = true
    )
    println("并行基准测试结果:")
    println("平均检索时间: ${parallelBenchmarkSummary.averageRetrievalTime}ms")
    println("平均上下文生成时间: ${parallelBenchmarkSummary.averageContextGenerationTime}ms")
    println("平均总时间: ${parallelBenchmarkSummary.averageTotalTime}ms")

    println("\n评估 RAG 系统:")
    val evaluationResult = evaluationTool.evaluateContextRelevance(
        query = "人工智能的应用",
        limit = 3
    )
    println("上下文相关性评分: $evaluationResult")

    println("\n示例完成")
}

/**
 * 创建测试文档。
 *
 * @return 文档列表
 */
private fun createTestDocuments(): List<Document> {
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
