package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 用户数据类
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int,
    val tags: List<String> = emptyList(),
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 用户搜索参数数据类
 */
data class UserSearchParams(
    val query: String,
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val tags: List<String>? = null,
    val page: Int = 1,
    val pageSize: Int = 10
)

/**
 * 用户搜索结果数据类
 */
data class UserSearchResult(
    val users: List<User>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val searchTime: String
)

/**
 * ZodTool 高级示例 - 用户搜索工具
 * 
 * 这个示例展示了如何使用 ZodTool 创建一个处理复杂数据结构的工具
 */
fun main() = runBlocking {
    println("ZodTool 高级示例 - 用户搜索工具")
    println("-------------------------------")
    
    // 模拟用户数据库
    val users = listOf(
        User(
            id = "user1",
            name = "张三",
            email = "zhangsan@example.com",
            age = 28,
            tags = listOf("premium", "active"),
            createdAt = LocalDateTime.now().minusDays(30)
        ),
        User(
            id = "user2",
            name = "李四",
            email = "lisi@example.com",
            age = 35,
            tags = listOf("premium", "admin"),
            createdAt = LocalDateTime.now().minusDays(60)
        ),
        User(
            id = "user3",
            name = "王五",
            email = "wangwu@example.com",
            age = 22,
            tags = listOf("new", "active"),
            createdAt = LocalDateTime.now().minusDays(5)
        ),
        User(
            id = "user4",
            name = "赵六",
            email = "zhaoliu@example.com",
            age = 42,
            tags = listOf("premium", "inactive"),
            createdAt = LocalDateTime.now().minusDays(120)
        ),
        User(
            id = "user5",
            name = "钱七",
            email = "qianqi@example.com",
            age = 19,
            tags = listOf("new"),
            createdAt = LocalDateTime.now().minusDays(2)
        )
    )
    
    // 创建用户搜索工具
    val userSearchTool = zodTool<UserSearchParams, UserSearchResult> {
        id = "user_search"
        name = "用户搜索"
        description = "根据各种条件搜索用户"
        
        // 定义输入模式
        val searchParamsSchema = objectInput("搜索参数") {
            stringField("query", "搜索查询") {
                description = "用于搜索用户名和邮箱的关键词"
                minLength = 1
                maxLength = 100
            }
            numberField("minAge", "最小年龄") {
                description = "用户的最小年龄（可选）"
                min = 0.0
                max = 120.0
                optional = true
            }
            numberField("maxAge", "最大年龄") {
                description = "用户的最大年龄（可选）"
                min = 0.0
                max = 120.0
                optional = true
            }
            arrayField("tags", stringInput("标签"), "用户标签") {
                description = "要筛选的用户标签列表（可选）"
                optional = true
            }
            numberField("page", "页码") {
                description = "结果的页码（默认为1）"
                min = 1.0
                default = 1.0
                optional = true
            }
            numberField("pageSize", "每页数量") {
                description = "每页的结果数量（默认为10）"
                min = 1.0
                max = 100.0
                default = 10.0
                optional = true
            }
        }.transform { input ->
            UserSearchParams(
                query = input["query"] as String,
                minAge = (input["minAge"] as? Number)?.toInt(),
                maxAge = (input["maxAge"] as? Number)?.toInt(),
                tags = input["tags"] as? List<String>,
                page = (input["page"] as? Number)?.toInt() ?: 1,
                pageSize = (input["pageSize"] as? Number)?.toInt() ?: 10
            )
        }
        
        // 定义输出模式
        val searchResultSchema = objectOutput("搜索结果") {
            arrayField("users", objectOutput("用户") {
                stringField("id", "用户ID")
                stringField("name", "用户名")
                stringField("email", "电子邮箱")
                numberField("age", "年龄")
                arrayField("tags", stringOutput(), "标签")
                stringField("createdAt", "创建时间")
            }, "用户列表")
            numberField("totalCount", "总用户数")
            numberField("page", "当前页码")
            numberField("pageSize", "每页数量")
            stringField("searchTime", "搜索时间")
        }.transform { output ->
            val usersList = (output["users"] as List<Map<String, Any?>>).map { userData ->
                User(
                    id = userData["id"] as String,
                    name = userData["name"] as String,
                    email = userData["email"] as String,
                    age = (userData["age"] as Number).toInt(),
                    tags = userData["tags"] as List<String>,
                    createdAt = LocalDateTime.parse(
                        userData["createdAt"] as String,
                        DateTimeFormatter.ISO_DATE_TIME
                    )
                )
            }
            
            UserSearchResult(
                users = usersList,
                totalCount = (output["totalCount"] as Number).toInt(),
                page = (output["page"] as Number).toInt(),
                pageSize = (output["pageSize"] as Number).toInt(),
                searchTime = output["searchTime"] as String
            )
        }
        
        // 设置输入和输出模式
        @Suppress("UNCHECKED_CAST")
        inputSchema = searchParamsSchema as Schema<UserSearchParams, UserSearchParams>
        
        @Suppress("UNCHECKED_CAST")
        outputSchema = searchResultSchema as Schema<UserSearchResult, UserSearchResult>
        
        // 实现执行逻辑
        execute = { params ->
            println("执行用户搜索，参数: $params")
            
            // 根据查询过滤用户
            var filteredUsers = users.filter { user ->
                val queryLower = params.query.lowercase()
                user.name.lowercase().contains(queryLower) || 
                user.email.lowercase().contains(queryLower)
            }
            
            // 应用年龄过滤
            params.minAge?.let { minAge ->
                filteredUsers = filteredUsers.filter { it.age >= minAge }
            }
            
            params.maxAge?.let { maxAge ->
                filteredUsers = filteredUsers.filter { it.age <= maxAge }
            }
            
            // 应用标签过滤
            params.tags?.let { tags ->
                if (tags.isNotEmpty()) {
                    filteredUsers = filteredUsers.filter { user ->
                        user.tags.any { it in tags }
                    }
                }
            }
            
            // 计算总数
            val totalCount = filteredUsers.size
            
            // 应用分页
            val startIndex = (params.page - 1) * params.pageSize
            val endIndex = minOf(startIndex + params.pageSize, filteredUsers.size)
            val paginatedUsers = if (startIndex < filteredUsers.size) {
                filteredUsers.subList(startIndex, endIndex)
            } else {
                emptyList()
            }
            
            // 转换用户列表为输出格式
            val userMaps = paginatedUsers.map { user ->
                mapOf(
                    "id" to user.id,
                    "name" to user.name,
                    "email" to user.email,
                    "age" to user.age,
                    "tags" to user.tags,
                    "createdAt" to user.createdAt.format(DateTimeFormatter.ISO_DATE_TIME)
                )
            }
            
            // 创建搜索结果
            val searchTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            
            val result = mapOf(
                "users" to userMaps,
                "totalCount" to totalCount,
                "page" to params.page,
                "pageSize" to params.pageSize,
                "searchTime" to searchTime
            )
            
            // 使用输出模式转换结果
            @Suppress("UNCHECKED_CAST")
            outputSchema.parse(result as UserSearchResult)
        }
    }
    
    // 使用用户搜索工具
    val searchParams = UserSearchParams(
        query = "张",
        minAge = 20,
        tags = listOf("premium"),
        page = 1,
        pageSize = 10
    )
    
    println("\n执行用户搜索:")
    val searchResult = userSearchTool.execute(searchParams)
    
    println("\n搜索结果:")
    println("总用户数: ${searchResult.totalCount}")
    println("当前页码: ${searchResult.page}")
    println("每页数量: ${searchResult.pageSize}")
    println("搜索时间: ${searchResult.searchTime}")
    println("找到的用户:")
    
    searchResult.users.forEachIndexed { index, user ->
        println("\n用户 ${index + 1}:")
        println("  ID: ${user.id}")
        println("  姓名: ${user.name}")
        println("  邮箱: ${user.email}")
        println("  年龄: ${user.age}")
        println("  标签: ${user.tags.joinToString(", ")}")
        println("  创建时间: ${user.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
    }
    
    // 将 ZodTool 转换为传统 Tool
    val legacyTool = userSearchTool.toTool()
    println("\n将 ZodTool 转换为传统 Tool:")
    println("Tool ID: ${legacyTool.id}")
    println("Tool Name: ${legacyTool.name}")
    println("Tool Description: ${legacyTool.description}")
    
    println("\nZodTool 高级示例完成")
}
