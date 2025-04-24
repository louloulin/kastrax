package ai.kastrax.app.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 搜索工具。
 * 使用 ZodTool 实现的搜索工具，可以搜索信息。
 */
val searchTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
    id = "search"
    name = "Search Tool"
    description = "一个搜索工具，可以搜索信息"
    
    // 定义输入模式
    inputSchema = objectInput("Search Input") {
        stringField("query", "搜索查询") {
            minLength = 2
            maxLength = 200
        }
        numberField("limit", "结果数量限制", required = false) {
            min = 1.0
            max = 10.0
        }
    }
    
    // 定义输出模式
    outputSchema = objectOutput("Search Results") {
        stringField("query", "搜索查询")
        numberField("totalResults", "结果总数")
        arrayField("results", "搜索结果") {
            objectField {
                stringField("title", "结果标题")
                stringField("url", "结果 URL")
                stringField("snippet", "结果摘要", required = false)
            }
        }
    }
    
    // 执行搜索
    execute = { input ->
        try {
            val query = input["query"] as String
            val limit = (input["limit"] as? Number)?.toInt() ?: 5
            
            logger.info { "执行搜索: $query, 限制: $limit" }
            
            // 模拟搜索结果
            val results = simulateSearch(query, limit)
            
            mapOf(
                "query" to query,
                "totalResults" to results.size,
                "results" to results
            )
        } catch (e: Exception) {
            logger.error(e) { "搜索时发生错误" }
            mapOf(
                "query" to (input["query"] as? String ?: ""),
                "totalResults" to 0,
                "results" to emptyList<Map<String, String>>(),
                "error" to "搜索错误: ${e.message}"
            )
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
