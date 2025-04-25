package ai.kastrax.app.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

private val logger = KotlinLogging.logger {}

/**
 * 搜索工具。
 * 用于搜索信息。
 */
val searchTool = tool {
    // 设置 ID 和名称
    id = "search"
    name = "搜索工具"

    // 设置描述
    description = "一个搜索工具，可以搜索信息"

    // 定义输入模式
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "搜索查询")
            }
            putJsonObject("limit") {
                put("type", "number")
                put("description", "结果数量限制")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("query"))
        }
    }

    // 执行搜索
    execute = { input ->
        try {
            val inputStr = input.toString()
            val query = if (inputStr.contains("query")) {
                inputStr.substringAfter("query").substringAfter(":").substringAfter("\"").substringBefore("\"")
            } else {
                "未知查询"
            }

            val limit = if (inputStr.contains("limit")) {
                inputStr.substringAfter("limit").substringAfter(":").substringBefore(",").trim().toIntOrNull() ?: 5
            } else {
                5
            }

            logger.info { "执行搜索: $query, 限制: $limit" }

            // 模拟搜索结果
            val results = simulateSearch(query, limit)

            buildJsonObject {
                put("query", query)
                put("totalResults", results.size)
                putJsonArray("results") {
                    results.forEach { result ->
                        add(buildJsonObject {
                            put("title", result["title"] ?: "")
                            put("url", result["url"] ?: "")
                            put("snippet", result["snippet"] ?: "")
                        })
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "搜索时发生错误" }
            buildJsonObject {
                put("query", "")
                put("totalResults", 0)
                putJsonArray("results") {}
                put("error", "搜索错误: ${e.message}")
            }
        }
    }
}

/**
 * 模拟搜索结果。
 */
private fun simulateSearch(query: String, limit: Int): List<Map<String, String>> {
    // 在实际应用中，这里应该调用真实的搜索 API
    // 例如 Google Search API、Bing Search API 等

    // 模拟数据
    val results = mutableListOf<Map<String, String>>()

    for (i in 1..limit) {
        results.add(mapOf(
            "title" to "搜索结果 $i 关于 \"$query\"",
            "url" to "https://example.com/result$i?q=$query",
            "snippet" to "这是关于 \"$query\" 的第 $i 个搜索结果的摘要。包含一些相关信息和上下文。"
        ))
    }

    return results
}
