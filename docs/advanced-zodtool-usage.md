# ZodTool 高级用法指南

本文档介绍了 ZodTool 的高级用法和最佳实践。

## 目录

1. [处理复杂数据结构](#处理复杂数据结构)
2. [数据转换和验证](#数据转换和验证)
3. [错误处理和自定义错误消息](#错误处理和自定义错误消息)
4. [异步操作](#异步操作)
5. [与其他系统集成](#与其他系统集成)
6. [性能优化](#性能优化)
7. [测试策略](#测试策略)

## 处理复杂数据结构

### 嵌套对象

ZodTool 可以轻松处理嵌套对象：

```kotlin
// 创建地址模式
val addressSchema = objectInput("Address") {
    stringField("street", "Street address")
    stringField("city", "City name")
    stringField("zipCode", "ZIP/Postal code")
    stringField("country", "Country name")
}

// 创建用户模式
val userSchema = objectInput("User") {
    stringField("name", "User name")
    numberField("age", "User age")
    field("address", addressSchema, "User address")
}
```

### 数组和集合

处理数组和集合：

```kotlin
// 字符串数组
val tagsSchema = arrayInput(stringInput("Tag"), "User tags")

// 对象数组
val friendsSchema = arrayInput(userSchema, "User friends")

// 在对象中使用数组
val userWithTagsSchema = objectInput("User with tags") {
    stringField("name", "User name")
    arrayField("tags", stringInput("Tag"), "User tags")
}
```

### 映射和动态对象

处理映射和动态对象：

```kotlin
// 具有动态属性的对象
val preferencesSchema = objectInput("User preferences") {
    // 允许任何字符串键
    // 值可以是字符串、数字或布尔值
    catchall = unionInput(
        stringInput(),
        numberInput(),
        booleanInput()
    )
}

// 在对象中使用动态属性
val userWithPrefsSchema = objectInput("User with preferences") {
    stringField("name", "User name")
    field("preferences", preferencesSchema, "User preferences")
}
```

### 联合类型

处理联合类型（可以是多种类型之一）：

```kotlin
// 可以是字符串或数字的字段
val idSchema = unionInput(
    stringInput("String ID"),
    numberInput("Numeric ID")
)

// 在对象中使用联合类型
val entitySchema = objectInput("Entity") {
    field("id", idSchema, "Entity ID")
    stringField("name", "Entity name")
}
```

### 可选字段

处理可选字段：

```kotlin
val userSchema = objectInput("User") {
    stringField("name", "User name", required = true)
    stringField("middleName", "User middle name", required = false)
    numberField("age", "User age", required = true)
}
```

### 默认值

设置默认值：

```kotlin
// 使用默认值
val userSchema = objectInput("User") {
    stringField("name", "User name")
    numberField("age", "User age", required = false)
}.default(mapOf("age" to 18))
```

## 数据转换和验证

### 基本验证

ZodTool 提供了多种内置验证：

```kotlin
val userSchema = objectInput("User") {
    stringField("name", "User name") {
        minLength = 2
        maxLength = 100
    }
    stringField("email", "User email") {
        email = true
    }
    stringField("website", "User website") {
        url = true
    }
    stringField("id", "User ID") {
        pattern = "^[a-zA-Z0-9_-]+$"
    }
    numberField("age", "User age") {
        min = 0.0
        max = 120.0
        multipleOf = 1.0 // 整数
    }
}
```

### 自定义验证

使用 `refine` 和 `superRefine` 进行自定义验证：

```kotlin
val passwordSchema = stringInput("Password")
    .refine({ password ->
        password.length >= 8 &&
        password.any { it.isDigit() } &&
        password.any { it.isUpperCase() } &&
        password.any { it.isLowerCase() } &&
        password.any { !it.isLetterOrDigit() }
    }, "Password must be at least 8 characters long and contain at least one digit, one uppercase letter, one lowercase letter, and one special character")

val userSchema = objectInput("User") {
    stringField("username", "Username")
    field("password", passwordSchema, "Password")
}
```

### 数据转换

使用 `transform` 转换数据：

```kotlin
// 将字符串转换为日期
val dateSchema = stringInput("Date (YYYY-MM-DD)")
    .refine({ it.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$")) }, "Invalid date format")
    .transform { dateString ->
        val (year, month, day) = dateString.split("-").map { it.toInt() }
        java.time.LocalDate.of(year, month, day)
    }

// 在对象中使用转换后的日期
val eventSchema = objectInput("Event") {
    stringField("name", "Event name")
    field("date", dateSchema, "Event date")
}
```

### 数据类集成

将模式与数据类集成：

```kotlin
// 定义数据类
data class User(
    val name: String,
    val email: String,
    val age: Int
)

// 创建模式并转换为数据类
val userSchema = objectInput("User") {
    stringField("name", "User name")
    stringField("email", "User email") {
        email = true
    }
    numberField("age", "User age")
}.transform { input ->
    User(
        name = input["name"] as String,
        email = input["email"] as String,
        age = (input["age"] as Number).toInt()
    )
}

// 创建使用数据类的工具
val userTool = zodTool<User, String> {
    id = "user_greeter"
    name = "User Greeter"
    description = "Greets a user"
    inputSchema = userSchema
    outputSchema = stringOutput("Greeting message")
    
    execute = { user ->
        "Hello, ${user.name}! You are ${user.age} years old."
    }
}
```

## 错误处理和自定义错误消息

### 基本错误处理

使用 `safeParse` 进行错误处理：

```kotlin
val input = mapOf(
    "name" to "John",
    "email" to "invalid-email",
    "age" to -5
)

val result = userSchema.safeParse(input)
when (result) {
    is SchemaResult.Success -> {
        val user = result.data
        println("Valid user: $user")
    }
    is SchemaResult.Failure -> {
        val error = result.error
        println("Invalid user: $error")
    }
}
```

### 自定义错误消息

使用 `refine` 和 `superRefine` 提供自定义错误消息：

```kotlin
val ageSchema = numberInput("Age")
    .refine({ age -> age >= 18 }, "You must be at least 18 years old")
    .refine({ age -> age <= 65 }, "You must be at most 65 years old")

// 更复杂的验证逻辑
val passwordSchema = stringInput("Password")
    .superRefine { password, ctx ->
        if (password.length < 8) {
            ctx.addIssue("Password must be at least 8 characters long")
        }
        if (!password.any { it.isDigit() }) {
            ctx.addIssue("Password must contain at least one digit")
        }
        if (!password.any { it.isUpperCase() }) {
            ctx.addIssue("Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            ctx.addIssue("Password must contain at least one lowercase letter")
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            ctx.addIssue("Password must contain at least one special character")
        }
    }
```

## 异步操作

ZodTool 支持异步操作：

```kotlin
val userTool = zodTool<String, User> {
    id = "user_fetcher"
    name = "User Fetcher"
    description = "Fetches a user by ID"
    inputSchema = stringInput("User ID")
    outputSchema = userSchema
    
    execute = { userId ->
        // 模拟异步操作
        delay(1000)
        
        // 模拟从数据库获取用户
        when (userId) {
            "user1" -> User("John Doe", "john.doe@example.com", 30)
            "user2" -> User("Jane Smith", "jane.smith@example.com", 25)
            else -> throw IllegalArgumentException("User not found: $userId")
        }
    }
}
```

## 与其他系统集成

### 与 JSON API 集成

```kotlin
val apiTool = zodTool<Map<String, Any?>, User> {
    id = "api_client"
    name = "API Client"
    description = "Calls an external API"
    inputSchema = objectInput("API request") {
        stringField("endpoint", "API endpoint")
        objectField("params", "Request parameters", required = false) {
            catchall = unionInput(
                stringInput(),
                numberInput(),
                booleanInput()
            )
        }
    }
    outputSchema = userSchema
    
    execute = { request ->
        val endpoint = request["endpoint"] as String
        val params = request["params"] as? Map<String, Any?>
        
        // 使用 Ktor 客户端调用 API
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }
        
        val response = client.get(endpoint) {
            params?.forEach { (key, value) ->
                parameter(key, value.toString())
            }
        }
        
        // 解析响应
        val jsonResponse = response.body<JsonElement>()
        userSchema.parseJson(jsonResponse)
    }
}
```

### 与数据库集成

```kotlin
val dbTool = zodTool<Map<String, Any?>, List<User>> {
    id = "db_query"
    name = "Database Query"
    description = "Queries the database"
    inputSchema = objectInput("Query parameters") {
        stringField("table", "Table name")
        objectField("filters", "Query filters", required = false) {
            catchall = unionInput(
                stringInput(),
                numberInput(),
                booleanInput()
            )
        }
        numberField("limit", "Result limit", required = false)
        numberField("offset", "Result offset", required = false)
    }
    outputSchema = arrayOutput(userSchema, "List of users")
    
    execute = { params ->
        val table = params["table"] as String
        val filters = params["filters"] as? Map<String, Any?>
        val limit = (params["limit"] as? Number)?.toInt()
        val offset = (params["offset"] as? Number)?.toInt()
        
        // 构建查询
        var query = "SELECT * FROM $table"
        
        if (filters != null && filters.isNotEmpty()) {
            query += " WHERE " + filters.entries.joinToString(" AND ") { (key, value) ->
                when (value) {
                    is String -> "$key = '$value'"
                    else -> "$key = $value"
                }
            }
        }
        
        if (limit != null) {
            query += " LIMIT $limit"
        }
        
        if (offset != null) {
            query += " OFFSET $offset"
        }
        
        // 执行查询（模拟）
        println("Executing query: $query")
        
        // 返回模拟结果
        listOf(
            User("John Doe", "john.doe@example.com", 30),
            User("Jane Smith", "jane.smith@example.com", 25)
        )
    }
}
```

## 性能优化

### 重用模式

为了提高性能，应该重用模式定义：

```kotlin
// 定义一次，多处使用
val nameSchema = stringInput("Name").refine({ it.length >= 2 }, "Name too short")
val emailSchema = stringInput("Email").refine({ it.contains("@") }, "Invalid email")

val userSchema = objectInput("User") {
    field("name", nameSchema, "User name")
    field("email", emailSchema, "User email")
}

val employeeSchema = objectInput("Employee") {
    field("name", nameSchema, "Employee name")
    field("email", emailSchema, "Employee email")
    stringField("department", "Department")
}
```

### 延迟验证

对于大型对象，可以延迟验证：

```kotlin
val userTool = zodTool<Map<String, Any?>, String> {
    id = "user_processor"
    name = "User Processor"
    description = "Processes user data"
    inputSchema = objectInput("User data") {
        // 只验证必要的字段
        stringField("id", "User ID")
        // 其他字段不验证
        catchall = anyInput()
    }
    outputSchema = stringOutput("Processing result")
    
    execute = { userData ->
        val userId = userData["id"] as String
        
        // 只在需要时验证特定字段
        if (userData.containsKey("email")) {
            val email = userData["email"] as? String
            if (email != null) {
                val emailSchema = stringInput("Email") {
                    email = true
                }
                val result = emailSchema.safeParse(email)
                if (result is SchemaResult.Failure) {
                    return@execute "Invalid email: ${result.error}"
                }
            }
        }
        
        // 处理用户数据
        "Processed user $userId"
    }
}
```

## 测试策略

### 单元测试

为 ZodTool 编写单元测试：

```kotlin
class UserToolTest {
    @Test
    fun `should validate user input`() {
        val userTool = createUserTool()
        
        // 有效输入
        val validInput = mapOf(
            "name" to "John Doe",
            "email" to "john.doe@example.com",
            "age" to 30
        )
        
        val validResult = userTool.inputSchema.safeParse(validInput)
        assertTrue(validResult is SchemaResult.Success)
        
        // 无效输入
        val invalidInput = mapOf(
            "name" to "J", // 太短
            "email" to "invalid-email", // 无效邮箱
            "age" to -5 // 负数年龄
        )
        
        val invalidResult = userTool.inputSchema.safeParse(invalidInput)
        assertTrue(invalidResult is SchemaResult.Failure)
        
        // 检查错误消息
        val error = (invalidResult as SchemaResult.Failure).error
        assertTrue(error.contains("name"))
        assertTrue(error.contains("email"))
        assertTrue(error.contains("age"))
    }
    
    @Test
    fun `should execute tool with valid input`() = runBlocking {
        val userTool = createUserTool()
        
        val input = mapOf(
            "name" to "John Doe",
            "email" to "john.doe@example.com",
            "age" to 30
        )
        
        val output = userTool.execute(input)
        assertEquals("Hello, John Doe! Your email is john.doe@example.com and you are 30 years old.", output)
    }
    
    private fun createUserTool(): ZodTool<Map<String, Any?>, String> {
        return zodTool {
            id = "user_greeter"
            name = "User Greeter"
            description = "Greets a user"
            
            inputSchema = objectInput("User") {
                stringField("name", "User name") {
                    minLength = 2
                }
                stringField("email", "User email") {
                    email = true
                }
                numberField("age", "User age") {
                    min = 0.0
                }
            }
            
            outputSchema = stringOutput("Greeting message")
            
            execute = { input ->
                val name = input["name"] as String
                val email = input["email"] as String
                val age = (input["age"] as Number).toInt()
                
                "Hello, $name! Your email is $email and you are $age years old."
            }
        }
    }
}
```

### 集成测试

编写集成测试：

```kotlin
class UserToolIntegrationTest {
    @Test
    fun `should integrate with other tools`() = runBlocking {
        // 创建用户创建工具
        val userCreatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
            id = "user_creator"
            name = "User Creator"
            description = "Creates a new user"
            
            inputSchema = objectInput("User creation data") {
                stringField("name", "User name")
                stringField("email", "User email")
                numberField("age", "User age")
            }
            
            outputSchema = objectOutput("Created user") {
                stringField("id", "User ID")
                stringField("name", "User name")
                stringField("email", "User email")
                numberField("age", "User age")
                stringField("createdAt", "Creation timestamp")
            }
            
            execute = { input ->
                val name = input["name"] as String
                val email = input["email"] as String
                val age = (input["age"] as Number).toInt()
                
                // 模拟用户创建
                mapOf(
                    "id" to "user_${System.currentTimeMillis()}",
                    "name" to name,
                    "email" to email,
                    "age" to age,
                    "createdAt" to java.time.Instant.now().toString()
                )
            }
        }
        
        // 创建用户通知工具
        val userNotifierTool = zodTool<Map<String, Any?>, String> {
            id = "user_notifier"
            name = "User Notifier"
            description = "Sends a notification to a user"
            
            inputSchema = objectInput("Notification data") {
                stringField("userId", "User ID")
                stringField("message", "Notification message")
            }
            
            outputSchema = stringOutput("Notification result")
            
            execute = { input ->
                val userId = input["userId"] as String
                val message = input["message"] as String
                
                // 模拟发送通知
                "Notification sent to user $userId: $message"
            }
        }
        
        // 集成测试
        val userData = mapOf(
            "name" to "John Doe",
            "email" to "john.doe@example.com",
            "age" to 30
        )
        
        // 创建用户
        val createdUser = userCreatorTool.execute(userData)
        
        // 发送通知
        val notificationData = mapOf(
            "userId" to createdUser["id"],
            "message" to "Welcome to our platform, ${createdUser["name"]}!"
        )
        
        val notificationResult = userNotifierTool.execute(notificationData)
        
        // 验证结果
        assertTrue(notificationResult.contains("Notification sent"))
        assertTrue(notificationResult.contains(createdUser["id"] as String))
    }
}
```

## 总结

ZodTool 提供了强大的类型安全和验证功能，可以用于构建复杂的工具和应用程序。通过使用本指南中的高级特性和最佳实践，您可以充分利用 ZodTool 的功能，创建更健壮、更可维护的代码。
