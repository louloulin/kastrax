package ai.kastrax.rag.graph

import ai.kastrax.rag.document.DocumentLoader
import ai.kastrax.store.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonPrimitive

private val logger = KotlinLogging.logger {}

/**
 * GraphRAG 工具，用于基于图的检索增强生成
 *
 * @property documentLoader 文档加载器
 * @property embeddingService 嵌入服务
 * @property graphRAG GraphRAG 实例
 */
class GraphRAGTool(
    private val documentLoader: DocumentLoader,
    private val embeddingService: EmbeddingService,
    private val graphRAG: GraphRAG = GraphRAG(
        GraphRAGConfig(
            dimension = embeddingService.dimension(),
            threshold = 0.7,
            bidirectional = true
        )
    )
) {
    /**
     * 执行 GraphRAG 查询
     *
     * @param query 查询文本
     * @param topK 返回的最大结果数
     * @param randomWalkSteps 随机游走步数
     * @param restartProb 重启概率
     * @param includeMetadata 是否包含元数据
     * @return 查询结果
     */
    suspend fun execute(
        query: String,
        topK: Int = 10,
        randomWalkSteps: Int = 100,
        restartProb: Double = 0.15,
        includeMetadata: Boolean = true
    ): JsonPrimitive {
        try {
            logger.debug { "执行 GraphRAG 查询: $query" }

            // 加载文档
            val documents = documentLoader.load()

            if (documents.isEmpty()) {
                return JsonPrimitive("未找到任何文档。")
            } else {
                // 为每个文档生成嵌入向量
                val embeddings = documents.map { document ->
                    embeddingService.embed(document.content)
                }

                // 创建图
                graphRAG.clear()
                graphRAG.createGraph(documents, embeddings)

                // 为查询生成嵌入向量
                val queryEmbedding = embeddingService.embed(query)

                // 查询图
                val queryOptions = GraphRAGQueryOptions(
                    topK = topK,
                    randomWalkSteps = randomWalkSteps,
                    restartProb = restartProb
                )
                val results = graphRAG.query(queryEmbedding, queryOptions)

                if (results.isEmpty()) {
                    return JsonPrimitive("未找到相关信息。")
                } else {
                    // 构建上下文
                    val contextBuilder = StringBuilder()
                    contextBuilder.append("以下是与查询相关的信息：\n\n")

                    results.forEach { result ->
                        contextBuilder.append(result.content)
                        contextBuilder.append("\n\n")
                    }

                    if (includeMetadata) {
                        contextBuilder.append("\n来源：\n")
                        results.forEachIndexed { index, result ->
                            val source = result.metadata["source"] ?: "未知来源"
                            contextBuilder.append("${index + 1}. $source (相似度: ${String.format("%.2f", result.score)})\n")
                        }
                    }

                    return JsonPrimitive(contextBuilder.toString())
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "GraphRAG 工具调用失败: ${e.message}" }
            return JsonPrimitive("检索失败: ${e.message}")
        }
    }
}
