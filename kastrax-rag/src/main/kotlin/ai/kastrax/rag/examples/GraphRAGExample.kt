package ai.kastrax.rag.examples

import ai.kastrax.store.document.Document
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import ai.kastrax.rag.graph.GraphRAG
import ai.kastrax.rag.graph.GraphRAGConfig
import ai.kastrax.rag.graph.GraphRAGQueryOptions
import kotlinx.coroutines.runBlocking

/**
 * GraphRAG 示例应用程序。
 * 这个示例展示了如何使用 GraphRAG 进行基于图的检索增强生成。
 */
fun main() {
    println("GraphRAG 示例")
    println("===========")

    runBlocking {
        // 创建嵌入服务
        val embeddingService = EmbeddingServiceFactory.createRandomEmbeddingService(dimensions = 3)

        // 创建 GraphRAG
        val graphRAG = GraphRAG(
            GraphRAGConfig(
                dimension = 3,
                threshold = 0.7,
                bidirectional = true
            )
        )

        // 创建示例文档
        val documents = listOf(
            Document(
                id = "1",
                content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
                metadata = mapOf("source" to "AI百科")
            ),
            Document(
                id = "2",
                content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
                metadata = mapOf("source" to "AI百科")
            ),
            Document(
                id = "3",
                content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。",
                metadata = mapOf("source" to "AI百科")
            ),
            Document(
                id = "4",
                content = "自然语言处理是人工智能的一个分支，专注于使计算机理解和生成人类语言。",
                metadata = mapOf("source" to "NLP百科")
            ),
            Document(
                id = "5",
                content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息。",
                metadata = mapOf("source" to "CV百科")
            )
        )

        // 为文档生成嵌入向量
        println("\n生成文档嵌入向量...")
        val embeddings = documents.map { document ->
            embeddingService.embed(document.content)
        }

        // 创建图
        println("\n创建知识图谱...")
        graphRAG.createGraph(documents, embeddings)

        // 显示图的统计信息
        val nodes = graphRAG.getNodes()
        val edges = graphRAG.getEdges()
        println("图统计信息:")
        println("- 节点数量: ${nodes.size}")
        println("- 边数量: ${edges.size}")

        // 执行查询
        val query = "机器学习"
        println("\n执行查询: \"$query\"")
        val queryEmbedding = embeddingService.embed(query)

        val results = graphRAG.query(
            queryEmbedding,
            GraphRAGQueryOptions(
                topK = 3,
                randomWalkSteps = 100,
                restartProb = 0.15
            )
        )

        // 显示结果
        println("\n查询结果:")
        results.forEachIndexed { index, result ->
            println("${index + 1}. ${result.content}")
            println("   来源: ${result.metadata["source"]}")
            println("   相似度: ${String.format("%.4f", result.score)}")
            println()
        }

        // 转换为搜索结果
        val searchResults = graphRAG.toSearchResults(results)
        println("\n搜索结果:")
        for (index in searchResults.indices) {
            val result = searchResults[index]
            println("${index + 1}. ${result.document.content}")
            println("   来源: ${result.document.metadata["source"]}")
            println("   相似度: ${String.format("%.4f", result.score)}")
            println()
        }
    }
}
