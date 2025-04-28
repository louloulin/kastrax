package ai.kastrax.rag.realtime

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.jsonObject
import ai.kastrax.core.tools.tool
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.vectorstore.RagVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * 创建实时 RAG 工具
 *
 * @param realTimeRag 实时 RAG 实例
 * @param name 工具名称
 * @param description 工具描述
 * @return 实时 RAG 工具
 */
fun createRealTimeRagTool(
    realTimeRag: RealTimeRag,
    name: String = "real_time_rag",
    description: String = "实时检索增强生成工具，用于从实时更新的文档集合中检索相关信息并生成上下文。"
): Tool {
    return tool {
        id = "real_time_rag"
        this.name = name
        this.description = description

        inputSchema = jsonObject {
            "type" to "object"
            "properties" to jsonObject {
                "query" to jsonObject {
                    "type" to "string"
                    "description" to "查询文本"
                }
                "limit" to jsonObject {
                    "type" to "integer"
                    "description" to "返回结果的最大数量"
                    "default" to JsonPrimitive(5)
                }
                "min_score" to jsonObject {
                    "type" to "number"
                    "description" to "最小相似度分数"
                    "default" to JsonPrimitive(0.0)
                }
                "include_metadata" to jsonObject {
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

                val limit = input.jsonObject["limit"]?.jsonPrimitive?.intOrNull ?: 5
                val minScore = input.jsonObject["min_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val includeMetadata = input.jsonObject["include_metadata"]?.jsonPrimitive?.booleanOrNull ?: true

                // 执行检索
                val searchResults = realTimeRag.search(query, limit, minScore.toDouble())

                if (searchResults.isEmpty()) {
                    JsonPrimitive("未找到相关信息。")
                } else {
                    val contextBuilder = StringBuilder()
                    contextBuilder.append("以下是与查询相关的信息：\n\n")

                    // 构建上下文
                    searchResults.forEach { result ->
                        contextBuilder.append(result.document.content)
                        contextBuilder.append("\n\n")
                    }

                    if (includeMetadata) {
                        contextBuilder.append("\n来源：\n")
                        searchResults.forEachIndexed { index, result ->
                            val metadata = result.document.metadata
                            val source = metadata["source"] ?: "未知来源"
                            contextBuilder.append("${index + 1}. $source (相似度: ${String.format("%.2f", result.score)})\n")
                        }
                    }

                    JsonPrimitive(contextBuilder.toString())
                }
            } catch (e: Exception) {
                logger.error(e) { "实时 RAG 工具调用失败: ${e.message}" }
                JsonPrimitive("检索失败: ${e.message}")
            }
        }
    }
}

/**
 * 创建实时 RAG 文档管理工具
 *
 * @param realTimeRag 实时 RAG 实例
 * @param name 工具名称
 * @param description 工具描述
 * @return 实时 RAG 文档管理工具
 */
fun createRealTimeRagDocumentTool(
    realTimeRag: RealTimeRag,
    name: String = "real_time_rag_document",
    description: String = "实时 RAG 文档管理工具，用于添加、更新和删除实时 RAG 系统中的文档。"
): Tool {
    return tool {
        id = "real_time_rag_document"
        this.name = name
        this.description = description

        inputSchema = jsonObject {
            "type" to "object"
            "properties" to jsonObject {
                "operation" to jsonObject {
                    "type" to "string"
                    "description" to "操作类型，可选值：add（添加）、update（更新）、delete（删除）"
                    "enum" to JsonArray(listOf(
                        JsonPrimitive("add"),
                        JsonPrimitive("update"),
                        JsonPrimitive("delete")
                    ))
                }
                "content" to jsonObject {
                    "type" to "string"
                    "description" to "文档内容"
                }
                "id" to jsonObject {
                    "type" to "string"
                    "description" to "文档 ID，如果不提供则自动生成"
                }
                "metadata" to jsonObject {
                    "type" to "object"
                    "description" to "文档元数据"
                }
            }
            "required" to JsonArray(listOf(
                JsonPrimitive("operation"),
                JsonPrimitive("content")
            ))
        }

        execute = { input ->
            try {
                val operation = input.jsonObject["operation"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("操作类型不能为空")

                val content = input.jsonObject["content"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("文档内容不能为空")

                val id = input.jsonObject["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()

                val metadata = input.jsonObject["metadata"]?.jsonObject?.let { jsonObj ->
                    jsonObj.entries.associate { (key, value) ->
                        key to (value.jsonPrimitive.contentOrNull ?: "")
                    }
                } ?: emptyMap()

                val document = Document(content, metadata)

                val result = when (operation.lowercase()) {
                    "add" -> realTimeRag.addDocument(document)
                    "update" -> realTimeRag.updateDocument(document)
                    "delete" -> realTimeRag.deleteDocument(document)
                    else -> throw IllegalArgumentException("不支持的操作类型: $operation")
                }

                val operationName = when (operation.lowercase()) {
                    "add" -> "添加"
                    "update" -> "更新"
                    "delete" -> "删除"
                    else -> operation
                }

                if (result) {
                    JsonPrimitive("文档${operationName}成功，ID: $id")
                } else {
                    JsonPrimitive("文档${operationName}失败")
                }
            } catch (e: Exception) {
                logger.error(e) { "实时 RAG 文档工具调用失败: ${e.message}" }
                JsonPrimitive("操作失败: ${e.message}")
            }
        }
    }
}

/**
 * 创建实时 RAG 系统和相关工具
 *
 * @param vectorStore 向量存储
 * @param embeddingService 嵌入服务
 * @param reranker 重排序器
 * @param config 实时 RAG 配置
 * @return 实时 RAG 系统和相关工具
 */
fun createRealTimeRagSystem(
    vectorStore: RagVectorStore,
    embeddingService: EmbeddingService,
    reranker: Reranker? = null,
    config: RealTimeRagConfig = RealTimeRagConfig()
): Pair<RealTimeRag, List<Tool>> {
    val realTimeRag = realTimeRag {
        vectorStore(vectorStore)
        embeddingService(embeddingService)
        if (reranker != null) {
            reranker(reranker)
        }
        config(config)
    }

    // 启动实时 RAG 系统
    realTimeRag.start()

    // 创建工具
    val ragTool = createRealTimeRagTool(realTimeRag)
    val documentTool = createRealTimeRagDocumentTool(realTimeRag)

    return realTimeRag to listOf(ragTool, documentTool)
}
