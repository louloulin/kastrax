package ai.kastrax.rag.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.DeepSeekProvider
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.rag.llm.LlmClient
import ai.kastrax.rag.llm.LlmProviderClient
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.retrieval.HybridStrategy
import ai.kastrax.rag.tools.RagBenchmarkTool
import ai.kastrax.rag.tools.RagEvaluationTool
import ai.kastrax.rag.tools.RagOptimizationTool
import ai.kastrax.rag.tools.RagTestCase
import ai.kastrax.rag.vectorstore.RagInMemoryVectorStore
import kotlinx.coroutines.runBlocking

/**
 * RAG 评估示例，展示如何使用 RAG 评估和优化工具。
 */
object RagEvaluationExample {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("RAG 评估示例")
        println("=============")

        // 创建 RAG 系统
        val embeddingService = OpenAIEmbeddingService(
            apiKey = System.getenv("OPENAI_API_KEY") ?: "your-api-key",
            model = "text-embedding-3-small"
        )

        val vectorStore = RagInMemoryVectorStore()
        val rag = RAG(vectorStore, embeddingService)

        // 加载文档
        val docsDir = "docs" // 文档目录
        val loader = DirectoryDocumentLoader(docsDir)
        val splitter = RecursiveCharacterTextSplitter(chunkSize = 1000, chunkOverlap = 200)

        println("加载文档...")
        val numDocs = rag.loadDocuments(loader, splitter)
        println("已加载 $numDocs 个文档片段")

        // 创建 LLM 提供商
        val llmProvider = deepSeek {
            apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key"
            model = "deepseek-chat"
        }

        // 创建 LLM 客户端
        val llmClient = LlmProviderClient(llmProvider)

        // 创建 Agent
        val ragAgent = agent {
            name = "RAG 评估 Agent"
            this.model = llmProvider
        }

        // 创建 RAG 评估工具
        val evaluationTool = RagEvaluationTool(rag, llmClient)

        // 创建 RAG 优化工具
        val optimizationTool = RagOptimizationTool(rag, llmClient, evaluationTool)

        // 创建 RAG 基准测试工具
        val benchmarkTool = RagBenchmarkTool(rag, llmClient, evaluationTool)

        // 示例查询
        val queries = listOf(
            "什么是 RAG？",
            "如何使用混合检索？",
            "如何评估 RAG 系统的质量？"
        )

        // 示例测试用例
        val testCases = listOf(
            RagTestCase(
                name = "RAG 定义",
                query = "什么是 RAG？",
                groundTruth = "RAG（检索增强生成）是一种结合检索系统和生成式 AI 的技术，通过从文档中检索相关信息来增强生成模型的回答。"
            ),
            RagTestCase(
                name = "混合检索",
                query = "如何使用混合检索？",
                groundTruth = null
            ),
            RagTestCase(
                name = "RAG 评估",
                query = "如何评估 RAG 系统的质量？",
                groundTruth = null
            )
        )

        // 生成回答的函数
        val generateAnswer: suspend (String, String) -> String = { query, context ->
            val prompt = """
                你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

                请遵循以下准则：
                1. 仅使用提供的上下文信息回答问题
                2. 如果上下文中没有足够的信息，请坦诚地说明你不知道
                3. 不要编造信息或使用你自己的知识
                4. 保持回答简洁、准确和有帮助

                上下文信息：
                $context

                用户问题：
                $query
            """.trimIndent()

            val response = ragAgent.generate(prompt)
            response.text
        }

        // 1. 单个查询评估
        println("\n1. 单个查询评估")
        println("----------------")

        val query = queries[0]
        println("查询: $query")

        val context = rag.generateContext(query)
        val answer = generateAnswer(query, context)

        println("回答: $answer")

        val evaluationResult = evaluationTool.evaluate(query, answer)
        val report = evaluationTool.generateReport(evaluationResult, detailed = true)

        println("\n评估报告:")
        println(report)

        // 2. 批量查询评估
        println("\n2. 批量查询评估")
        println("----------------")

        val batchResults = evaluationTool.evaluateBatch(queries, generateAnswer)
        val batchReport = evaluationTool.generateBatchReport(batchResults)

        println("\n批量评估报告:")
        println(batchReport)

        // 3. RAG 配置优化
        println("\n3. RAG 配置优化")
        println("----------------")

        val optimizationResult = optimizationTool.optimize(queries, generateAnswer)
        val optimizationReport = optimizationTool.generateReport(optimizationResult)

        println("\n优化报告:")
        println(optimizationReport)

        // 4. 基准测试
        println("\n4. 基准测试")
        println("----------------")

        val benchmark = benchmarkTool.createBenchmark(
            name = "RAG 基准测试",
            description = "评估 RAG 系统的质量",
            testCases = testCases
        )

        val benchmarkResult = benchmark.run(generateAnswer)
        val benchmarkReport = benchmark.generateReport(benchmarkResult, detailed = true)

        println("\n基准测试报告:")
        println(benchmarkReport)

        // 5. 使用优化后的配置
        println("\n5. 使用优化后的配置")
        println("----------------")

        val optimizedConfig = optimizationResult.bestConfiguration
        println("最佳配置:")
        println("- useHybridSearch: ${optimizedConfig.useHybridSearch}")
        println("- useEnhancedHybridSearch: ${optimizedConfig.useEnhancedHybridSearch}")
        println("- useQueryEnhancement: ${optimizedConfig.useQueryEnhancement}")
        println("- useReranking: ${optimizedConfig.useReranking}")

        val optimizedResults = evaluationTool.evaluateBatch(queries, generateAnswer, optimizedConfig)
        val optimizedReport = evaluationTool.generateBatchReport(optimizedResults)

        println("\n优化后的评估报告:")
        println(optimizedReport)
    }
}
