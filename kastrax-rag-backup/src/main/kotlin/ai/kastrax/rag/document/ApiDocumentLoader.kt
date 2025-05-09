package ai.kastrax.rag.document

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

/**
 * API文档加载器，从REST API加载文档。
 *
 * @property url API的URL
 * @property method HTTP方法，默认为GET
 * @property headers HTTP请求头
 * @property queryParams 查询参数
 * @property body 请求体，用于POST、PUT等方法
 * @property metadata 要添加到文档的元数据
 * @property documentPerItem 是否为响应中的每个数组项创建一个文档，默认为false（整个响应作为一个文档）
 * @property itemsPath 当documentPerItem为true时，指向数组的JSON路径，默认为null（表示响应本身是一个数组）
 * @property contentFields 要包含在内容中的字段列表，默认为null（包含所有字段）
 * @property timeout 请求超时时间（毫秒），默认为30000
 * @property maxDepth 递归解析JSON的最大深度，默认为5
 */
class ApiDocumentLoader(
    private val url: String,
    private val method: HttpMethod = HttpMethod.Get,
    private val headers: Map<String, String> = emptyMap(),
    private val queryParams: Map<String, String> = emptyMap(),
    private val body: Any? = null,
    private val metadata: Map<String, Any> = emptyMap(),
    private val documentPerItem: Boolean = false,
    private val itemsPath: String? = null,
    private val contentFields: List<String>? = null,
    private val timeout: Long = 30000,
    private val maxDepth: Int = 5
) : DocumentLoader {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }
    }

    override suspend fun load(): List<Document> {
        logger.debug { "Loading documents from API: $url" }

        return try {
            // 发送请求
            val response = httpClient.request(url) {
                method = this@ApiDocumentLoader.method

                // 添加请求头
                headers {
                    this@ApiDocumentLoader.headers.entries.forEach { entry ->
                        append(entry.key, entry.value)
                    }
                    if (!this@ApiDocumentLoader.headers.containsKey("Accept")) {
                        append("Accept", "application/json")
                    }
                }

                // 添加查询参数
                url {
                    this@ApiDocumentLoader.queryParams.entries.forEach { entry ->
                        parameters.append(entry.key, entry.value)
                    }
                }

                // 添加请求体
                body?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }

            // 检查响应状态
            if (!response.status.isSuccess()) {
                logger.error { "API request failed: ${response.status}" }
                return emptyList()
            }

            // 获取响应体
            val responseBody = response.bodyAsText()

            // 基础元数据
            val baseMetadata = mutableMapOf(
                "source" to url,
                "http_method" to method.value,
                "http_status" to response.status.value,
                "http_status_description" to response.status.description,
                "content_type" to (response.headers["Content-Type"] ?: "")
            )

            // 合并用户提供的元数据
            val fullMetadata = baseMetadata + metadata

            // 解析JSON响应
            val rootNode = objectMapper.readTree(responseBody)

            if (documentPerItem) {
                // 为每个项创建一个文档
                val itemsNode = if (itemsPath != null) {
                    // 使用路径获取数组
                    val pathParts = itemsPath.split(".")
                    var currentNode = rootNode

                    for (part in pathParts) {
                        currentNode = currentNode.get(part) ?: break
                    }

                    currentNode
                } else {
                    // 响应本身是一个数组
                    rootNode
                }

                if (itemsNode.isArray) {
                    val documents = mutableListOf<Document>()

                    itemsNode.forEachIndexed { index, item ->
                        val itemContent = nodeToText(item, 0)
                        val itemMetadata = fullMetadata + mapOf(
                            "item_index" to index,
                            "item_number" to (index + 1)
                        )

                        documents.add(Document(itemContent, itemMetadata))
                    }

                    documents
                } else {
                    logger.warn { "Expected array but got ${itemsNode.nodeType} for path: $itemsPath" }
                    // 回退到整个响应作为一个文档
                    val content = nodeToText(rootNode, 0)
                    listOf(Document(content, fullMetadata))
                }
            } else {
                // 整个响应作为一个文档
                val content = nodeToText(rootNode, 0)
                listOf(Document(content, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents from API: $url" }
            emptyList()
        } finally {
            httpClient.close()
        }
    }

    /**
     * 将JSON节点转换为文本。
     */
    private fun nodeToText(node: JsonNode, depth: Int): String {
        if (depth > maxDepth) {
            return "[嵌套过深]"
        }

        return when {
            node.isObject -> {
                val fields = node.fields().asSequence().toList()
                val filteredFields = if (contentFields != null && depth == 0) {
                    fields.filter { contentFields.contains(it.key) }
                } else {
                    fields
                }

                filteredFields.joinToString("\n") { (key, value) ->
                    "$key: ${nodeToText(value, depth + 1)}"
                }
            }
            node.isArray -> {
                node.map { nodeToText(it, depth + 1) }.joinToString(", ")
            }
            node.isTextual -> node.asText()
            node.isNumber -> node.asText()
            node.isBoolean -> node.asText()
            node.isNull -> "null"
            else -> node.toString()
        }
    }
}
