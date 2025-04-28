package ai.kastrax.rag.graph

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.jsonObject
import ai.kastrax.core.tools.tool
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 创建 GraphRAG 工具
 *
 * @param vectorStore 向量存储
 * @param embeddingService 嵌入服务
 * @param graphRAG GraphRAG 实例
 * @param name 工具名称
 * @param description 工具描述
 * @return GraphRAG 工具
 */
fun createGraphRAGTool(
    vectorStore: RagVectorStore,
    embeddingService: EmbeddingService,
    graphRAG: GraphRAG = GraphRAG(),
    name: String = "graph_rag",
    description: String = "基于图的检索增强生成工具，可以发现文档片段之间的隐含关系和上下文连接。"
): Tool {
    return tool {
        id = "graph_rag"
        this.name = name
        this.description = description

        inputSchema = jsonObject {
            "type" to "object"
            "properties" to jsonObject {
                "query" to jsonObject {
                    "type" to "string"
                    "description" to "查询文本"
                }
                "topK" to jsonObject {
                    "type" to "integer"
                    "description" to "返回结果的最大数量"
                    "default" to JsonPrimitive(10)
                }
                "randomWalkSteps" to jsonObject {
                    "type" to "integer"
                    "description" to "随机游走步数"
                    "default" to JsonPrimitive(100)
                }
                "restartProb" to jsonObject {
                    "type" to "number"
                    "description" to "重启概率"
                    "default" to JsonPrimitive(0.15)
                }
                "includeMetadata" to jsonObject {
                    "type" to "boolean"
                    "description" to "是否包含元数据"
                    "default" to JsonPrimitive(true)
                }
            }
            "required" to JsonArray(listOf(JsonPrimitive("query")))
        }

        execute = { input ->
            try {
                val query = input.jsonObject["query"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("查询文本不能为空")

                val topK = input.jsonObject["topK"]?.jsonPrimitive?.intOrNull ?: 10
                val randomWalkSteps = input.jsonObject["randomWalkSteps"]?.jsonPrimitive?.intOrNull ?: 100
                val restartProb = input.jsonObject["restartProb"]?.jsonPrimitive?.doubleOrNull ?: 0.15
                val includeMetadata = input.jsonObject["includeMetadata"]?.jsonPrimitive?.booleanOrNull ?: true

                // 模拟文档和嵌入向量
                // 在实际应用中，你需要从向量存储中获取文档
                val documents = listOf(
                    Document(
                        content = "GraphRAG 是一种基于图的检索增强生成技术。",
                        metadata = mapOf("source" to "技术文档")
                    ),
                    Document(
                        content = "图结构可以发现文档片段之间的隐含关系。",
                        metadata = mapOf("source" to "技术文档")
                    ),
                    Document(
                        content = "随机游走算法可以提高检索质量。",
                        metadata = mapOf("source" to "研究论文")
                    )
                )

                if (documents.isEmpty()) {
                    JsonPrimitive("未找到任何文档。")
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
                    JsonPrimitive("未找到相关信息。")
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

                    JsonPrimitive(contextBuilder.toString())
                }
                }
            } catch (e: Exception) {
                logger.error(e) { "GraphRAG 工具调用失败: ${e.message}" }
                JsonPrimitive("检索失败: ${e.message}")
            }
        }
    }
}

/**
 * 创建 GraphRAG 系统和工具
 *
 * @param vectorStore 向量存储
 * @param embeddingService 嵌入服务
 * @param config GraphRAG 配置
 * @return GraphRAG 实例和工具
 */
fun createGraphRAGSystem(
    vectorStore: RagVectorStore,
    embeddingService: EmbeddingService,
    config: GraphRAGConfig = GraphRAGConfig()
): Pair<GraphRAG, Tool> {
    val graphRAG = GraphRAG(config)
    val tool = createGraphRAGTool(vectorStore, embeddingService, graphRAG)
    return graphRAG to tool
}
