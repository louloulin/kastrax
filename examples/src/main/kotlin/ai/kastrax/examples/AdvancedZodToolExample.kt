package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

/**
 * 地址数据类。
 */
data class Address(
    val street: String,
    val city: String,
    val zipCode: String,
    val country: String
)

/**
 * 联系方式数据类。
 */
data class Contact(
    val email: String,
    val phone: String?,
    val address: Address
)

/**
 * 用户数据类。
 */
data class AdvancedUser(
    val id: String,
    val name: String,
    val age: Int,
    val contact: Contact,
    val tags: List<String>,
    val preferences: Map<String, Any>
)

/**
 * 用户搜索结果数据类。
 */
data class UserSearchResult(
    val users: List<AdvancedUser>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * 高级 ZodTool 示例。
 */
fun main() {
    // 创建地址模式
    val addressSchema = objectInput("Address") {
        stringField("street", "Street address") {
            minLength = 3
            maxLength = 100
        }
        stringField("city", "City name") {
            minLength = 2
            maxLength = 50
        }
        stringField("zipCode", "ZIP/Postal code") {
            pattern = "^[0-9]{5}(-[0-9]{4})?\$"
        }
        stringField("country", "Country name") {
            minLength = 2
            maxLength = 50
        }
    }.transform { input ->
        Address(
            street = input["street"] as String,
            city = input["city"] as String,
            zipCode = input["zipCode"] as String,
            country = input["country"] as String
        )
    }
    
    // 创建联系方式模式
    val contactSchema = objectInput("Contact information") {
        stringField("email", "Email address") {
            email = true
        }
        stringField("phone", "Phone number", required = false) {
            pattern = "^\\+?[0-9]{10,15}\$"
        }
        field("address", addressSchema, "Physical address")
    }.transform { input ->
        Contact(
            email = input["email"] as String,
            phone = input["phone"] as String?,
            address = input["address"] as Address
        )
    }
    
    // 创建用户模式
    val userSchema = objectInput("User") {
        stringField("id", "User ID") {
            minLength = 3
            maxLength = 50
        }
        stringField("name", "User name") {
            minLength = 2
            maxLength = 100
        }
        numberField("age", "User age") {
            min = 0.0
            max = 120.0
        }
        field("contact", contactSchema, "Contact information")
        arrayField("tags", stringInput("Tag"), "User tags") {
            minLength = 0
            maxLength = 10
        }
        objectField("preferences", "User preferences") {
            // 动态字段，可以包含任何键值对
            catchall = unionInput(
                stringInput(),
                numberInput(),
                booleanInput()
            )
        }
    }.transform { input ->
        AdvancedUser(
            id = input["id"] as String,
            name = input["name"] as String,
            age = (input["age"] as Number).toInt(),
            contact = input["contact"] as Contact,
            tags = input["tags"] as List<String>,
            preferences = input["preferences"] as Map<String, Any>
        )
    }
    
    // 创建用户搜索参数模式
    val searchParamsSchema = objectInput("Search parameters") {
        stringField("query", "Search query", required = false)
        numberField("page", "Page number", required = false) {
            min = 1.0
        }
        numberField("pageSize", "Page size", required = false) {
            min = 1.0
            max = 100.0
        }
        arrayField("tags", stringInput("Tag"), "Filter by tags", required = false)
        numberField("minAge", "Minimum age", required = false) {
            min = 0.0
        }
        numberField("maxAge", "Maximum age", required = false) {
            max = 120.0
        }
    }
    
    // 创建用户搜索结果模式
    val searchResultSchema = objectOutput("Search result") {
        arrayField("users", userSchema, "List of users")
        numberField("totalCount", "Total number of users")
        numberField("page", "Current page")
        numberField("pageSize", "Page size")
    }.transform { output ->
        UserSearchResult(
            users = output["users"] as List<AdvancedUser>,
            totalCount = (output["totalCount"] as Number).toInt(),
            page = (output["page"] as Number).toInt(),
            pageSize = (output["pageSize"] as Number).toInt()
        )
    }
    
    // 创建用户搜索工具
    val userSearchTool = zodTool<Map<String, Any?>, UserSearchResult> {
        id = "user_search"
        name = "User Search"
        description = "Search for users based on various criteria"
        inputSchema = searchParamsSchema
        outputSchema = searchResultSchema
        
        execute = { params ->
            // 模拟用户数据
            val allUsers = listOf(
                AdvancedUser(
                    id = "user1",
                    name = "John Doe",
                    age = 30,
                    contact = Contact(
                        email = "john.doe@example.com",
                        phone = "+1234567890",
                        address = Address(
                            street = "123 Main St",
                            city = "New York",
                            zipCode = "10001",
                            country = "USA"
                        )
                    ),
                    tags = listOf("premium", "active"),
                    preferences = mapOf(
                        "theme" to "dark",
                        "notifications" to true,
                        "maxItems" to 50
                    )
                ),
                AdvancedUser(
                    id = "user2",
                    name = "Jane Smith",
                    age = 25,
                    contact = Contact(
                        email = "jane.smith@example.com",
                        phone = null,
                        address = Address(
                            street = "456 Park Ave",
                            city = "Boston",
                            zipCode = "02108",
                            country = "USA"
                        )
                    ),
                    tags = listOf("free", "inactive"),
                    preferences = mapOf(
                        "theme" to "light",
                        "notifications" to false,
                        "maxItems" to 20
                    )
                ),
                AdvancedUser(
                    id = "user3",
                    name = "Bob Johnson",
                    age = 45,
                    contact = Contact(
                        email = "bob.johnson@example.com",
                        phone = "+9876543210",
                        address = Address(
                            street = "789 Broadway",
                            city = "Chicago",
                            zipCode = "60601",
                            country = "USA"
                        )
                    ),
                    tags = listOf("premium", "active", "admin"),
                    preferences = mapOf(
                        "theme" to "system",
                        "notifications" to true,
                        "maxItems" to 100
                    )
                )
            )
            
            // 应用搜索过滤器
            var filteredUsers = allUsers
            
            // 按查询过滤
            val query = params["query"] as String?
            if (query != null && query.isNotEmpty()) {
                filteredUsers = filteredUsers.filter { 
                    it.name.contains(query, ignoreCase = true) || 
                    it.id.contains(query, ignoreCase = true) 
                }
            }
            
            // 按标签过滤
            val tags = params["tags"] as List<String>?
            if (tags != null && tags.isNotEmpty()) {
                filteredUsers = filteredUsers.filter { user ->
                    tags.any { tag -> user.tags.contains(tag) }
                }
            }
            
            // 按年龄过滤
            val minAge = params["minAge"] as Number?
            if (minAge != null) {
                filteredUsers = filteredUsers.filter { it.age >= minAge.toInt() }
            }
            
            val maxAge = params["maxAge"] as Number?
            if (maxAge != null) {
                filteredUsers = filteredUsers.filter { it.age <= maxAge.toInt() }
            }
            
            // 分页
            val page = (params["page"] as? Number)?.toInt() ?: 1
            val pageSize = (params["pageSize"] as? Number)?.toInt() ?: 10
            
            val totalCount = filteredUsers.size
            val paginatedUsers = filteredUsers
                .drop((page - 1) * pageSize)
                .take(pageSize)
            
            UserSearchResult(
                users = paginatedUsers,
                totalCount = totalCount,
                page = page,
                pageSize = pageSize
            )
        }
    }
    
    // 使用工具
    val searchParams = mapOf(
        "query" to "john",
        "page" to 1,
        "pageSize" to 10,
        "tags" to listOf("premium"),
        "minAge" to 18
    )
    
    // 验证输入
    val validationResult = userSearchTool.inputSchema.safeParse(searchParams)
    if (validationResult is SchemaResult.Success) {
        println("Search parameters are valid")
    } else {
        println("Search parameters are invalid: ${(validationResult as SchemaResult.Failure).error}")
        return
    }
    
    // 执行搜索
    val searchResult = runBlocking {
        userSearchTool.execute(searchParams)
    }
    
    // 打印结果
    println("Search results:")
    println("Total users: ${searchResult.totalCount}")
    println("Page: ${searchResult.page} of ${(searchResult.totalCount + searchResult.pageSize - 1) / searchResult.pageSize}")
    println("Users:")
    
    searchResult.users.forEach { user ->
        println("\nUser ID: ${user.id}")
        println("Name: ${user.name}")
        println("Age: ${user.age}")
        println("Email: ${user.contact.email}")
        println("Phone: ${user.contact.phone ?: "N/A"}")
        println("Address: ${user.contact.address.street}, ${user.contact.address.city}, ${user.contact.address.zipCode}, ${user.contact.address.country}")
        println("Tags: ${user.tags.joinToString(", ")}")
        println("Preferences:")
        user.preferences.forEach { (key, value) ->
            println("  $key: $value")
        }
    }
}
