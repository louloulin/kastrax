package ai.kastrax.core.tools.web

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Web 搜索工具，用于从互联网获取信息。
 */
class WebSearchTool(
    private val apiKey: String? = null,
    private val searchEngine: SearchEngine = SearchEngine.MOCK,
    private val maxResults: Int = 5,
    private val timeout: Duration = Duration.ofSeconds(10)
) : Tool, KastraXBase(name = "Web 搜索",component = "TOOL") {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    override val id: String = "web_search"
    override val description: String = "搜索互联网获取信息"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "搜索查询")
            }
            putJsonObject("num_results") {
                put("type", "integer")
                put("description", "返回结果数量")
                put("default", maxResults)
                put("minimum", 1)
                put("maximum", 10)
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("query"))
        }
    }

    override val outputSchema: JsonObject? = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("results") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("title") {
                            put("type", "string")
                            put("description", "搜索结果标题")
                        }
                        putJsonObject("url") {
                            put("type", "string")
                            put("description", "搜索结果URL")
                        }
                        putJsonObject("snippet") {
                            put("type", "string")
                            put("description", "搜索结果摘要")
                        }
                    }
                }
            }
            putJsonObject("query") {
                put("type", "string")
                put("description", "搜索查询")
            }
            putJsonObject("total_results") {
                put("type", "integer")
                put("description", "结果总数")
            }
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement {
        logger.info { "执行 Web 搜索: $input" }

        val inputObj = input as? JsonObject ?: throw IllegalArgumentException("Input must be a JSON object")
        val query = inputObj["query"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        } ?: throw IllegalArgumentException("Query is required")

        val numResults = inputObj["num_results"]?.let {
            when (it) {
                is JsonPrimitive -> it.content.toIntOrNull() ?: maxResults
                else -> maxResults
            }
        } ?: maxResults

        return when (searchEngine) {
            SearchEngine.GOOGLE -> searchGoogle(query, numResults)
            SearchEngine.BING -> searchBing(query, numResults)
            SearchEngine.MOCK -> mockSearch(query, numResults)
        }
    }

    private suspend fun searchGoogle(query: String, numResults: Int): JsonObject = withContext(Dispatchers.IO) {
        try {
            // 这里应该使用 Google Custom Search API
            // 由于需要 API 密钥，这里使用模拟数据
            logger.info { "使用 Google 搜索: $query" }
            mockSearch(query, numResults)
        } catch (e: Exception) {
            logger.error(e) { "Google 搜索失败" }
            buildJsonObject {
                put("error", "搜索失败: ${e.message}")
            }
        }
    }

    private suspend fun searchBing(query: String, numResults: Int): JsonObject = withContext(Dispatchers.IO) {
        try {
            // 这里应该使用 Bing Search API
            // 由于需要 API 密钥，这里使用模拟数据
            logger.info { "使用 Bing 搜索: $query" }
            mockSearch(query, numResults)
        } catch (e: Exception) {
            logger.error(e) { "Bing 搜索失败" }
            buildJsonObject {
                put("error", "搜索失败: ${e.message}")
            }
        }
    }

    private fun mockSearch(query: String, numResults: Int): JsonObject {
        logger.info { "使用模拟搜索: $query, 结果数量: $numResults" }
        return buildJsonObject {
            putJsonArray("results") {
                for (i in 1..numResults) {
                    add(buildJsonObject {
                        put("title", "搜索结果 $i 关于 \"$query\"")
                        put("url", "https://example.com/result$i")
                        put("snippet", "这是关于 \"$query\" 的第 $i 个搜索结果的摘要。包含了相关信息和上下文。")
                    })
                }
            }
            put("query", query)
            put("total_results", numResults)
        }
    }

    /**
     * 搜索引擎枚举。
     */
    enum class SearchEngine {
        GOOGLE,
        BING,
        MOCK
    }

    companion object {
        /**
         * 创建 Web 搜索工具。
         */
        fun create(
            apiKey: String? = null,
            searchEngine: SearchEngine = SearchEngine.MOCK,
            maxResults: Int = 5
        ): Tool {
            return WebSearchTool(
                apiKey = apiKey,
                searchEngine = searchEngine,
                maxResults = maxResults
            )
        }
    }
}
