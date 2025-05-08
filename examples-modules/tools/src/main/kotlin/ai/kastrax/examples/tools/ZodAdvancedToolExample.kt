package ai.kastrax.examples.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.regex.Pattern

/**
 * 搜索请求数据类
 */
data class SearchRequest(
    val query: String,
    val filters: Map<String, String> = emptyMap(),
    val page: Int = 1,
    val pageSize: Int = 10,
    val sortBy: String? = null,
    val sortOrder: String? = "asc"
)

/**
 * 搜索结果数据类
 */
data class SearchResult(
    val items: List<SearchItem>,
    val totalItems: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 搜索项数据类
 */
data class SearchItem(
    val id: String,
    val title: String,
    val description: String,
    val tags: List<String>,
    val createdAt: Instant,
    val score: Double
)

/**
 * Zod 高级工具示例
 */
fun main() = runBlocking {
    println("Zod 高级工具示例")
    println("===============")

    // 创建搜索工具
    val searchTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "advanced_search"
        name = "高级搜索"
        description = "执行高级搜索并返回分页结果"

        // 定义输入模式
        inputSchema = obj {
            field("query", string { minLength = 1 })
            field("filters", obj {
                // 使用 catchall 允许任意字符串键和值
                catchall(string())
            }, required = false)
            field("page", number { min = 1.0; int() }, required = false)
            field("pageSize", number { min = 1.0; max = 100.0; int() }, required = false)
            field("sortBy", string(), required = false)
            field("sortOrder", string {
                enum("asc", "desc")
            }, required = false)
        }

        // 定义输出模式
        outputSchema = obj {
            field("items", array(obj {
                field("id", string())
                field("title", string())
                field("description", string())
                field("tags", array(string()))
                field("createdAt", string())
                field("score", number())
            }))
            field("totalItems", number())
            field("page", number())
            field("pageSize", number())
            field("totalPages", number())
        }

        // 实现执行逻辑
        execute = { input ->
            // 解析输入
            val query = input["query"] as String
            val filters = (input["filters"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap<String, String>()
            val page = (input["page"] as? Number)?.toInt() ?: 1
            val pageSize = (input["pageSize"] as? Number)?.toInt() ?: 10
            val sortBy = input["sortBy"] as? String
            val sortOrder = input["sortOrder"] as? String ?: "asc"

            // 模拟搜索操作
            println("执行搜索: $query")
            println("过滤条件: $filters")
            println("分页: 第${page}页，每页${pageSize}项")
            if (sortBy != null) {
                println("排序: 按${sortBy} ${sortOrder}序排列")
            }

            // 生成模拟搜索结果
            val totalItems = 100
            val totalPages = (totalItems + pageSize - 1) / pageSize

            // 创建模拟项目
            val items = (1..minOf(pageSize, totalItems - (page - 1) * pageSize)).map { i ->
                val itemIndex = (page - 1) * pageSize + i
                mapOf(
                    "id" to "item-$itemIndex",
                    "title" to "搜索结果 #$itemIndex",
                    "description" to "这是关于'${query}'的搜索结果 #$itemIndex 的描述",
                    "tags" to listOf("tag1", "tag2", if (itemIndex % 2 == 0) "even" else "odd"),
                    "createdAt" to Instant.now().minusSeconds((itemIndex * 3600).toLong()).toString(),
                    "score" to (1.0 - (itemIndex.toDouble() / totalItems))
                )
            }

            mapOf(
                "items" to items,
                "totalItems" to totalItems,
                "page" to page,
                "pageSize" to pageSize,
                "totalPages" to totalPages
            )
        }
    }

    // 测试搜索工具
    println("\n基本搜索测试:")
    val basicRequest = mapOf(
        "query" to "人工智能"
    )

    val basicResult = searchTool.execute(basicRequest)
    println("找到 ${basicResult["totalItems"]} 个结果，共 ${basicResult["totalPages"]} 页")
    println("当前页: ${basicResult["page"]}，每页 ${basicResult["pageSize"]} 项")
    val basicItems = basicResult["items"] as? List<*>
    println("第一个结果: ${basicItems?.firstOrNull()}")

    println("\n高级搜索测试:")
    val advancedRequest = mapOf(
        "query" to "机器学习",
        "filters" to mapOf(
            "category" to "教程",
            "level" to "高级"
        ),
        "page" to 2,
        "pageSize" to 5,
        "sortBy" to "relevance",
        "sortOrder" to "desc"
    )

    val advancedResult = searchTool.execute(advancedRequest)
    println("找到 ${advancedResult["totalItems"]} 个结果，共 ${advancedResult["totalPages"]} 页")
    println("当前页: ${advancedResult["page"]}，每页 ${advancedResult["pageSize"]} 项")
    val advancedItems = advancedResult["items"] as? List<*>
    println("结果数量: ${advancedItems?.size}")
    println("第一个结果: ${advancedItems?.firstOrNull()}")

    println("\nZod 高级工具示例完成")
}
