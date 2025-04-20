# ZodTool 安全最佳实践

本文档提供了使用 ZodTool 时的安全最佳实践，帮助开发者构建安全可靠的应用程序。

## 目录

1. [输入验证](#输入验证)
2. [数据清理](#数据清理)
3. [错误处理](#错误处理)
4. [权限控制](#权限控制)
5. [敏感数据处理](#敏感数据处理)
6. [安全集成](#安全集成)
7. [审计和日志](#审计和日志)
8. [安全测试](#安全测试)

## 输入验证

### 严格验证所有输入

始终对所有输入进行严格验证，不信任任何外部数据：

```kotlin
val userSchema = objectInput("User") {
    stringField("name", "User name") {
        minLength = 1
        maxLength = 100
        pattern = "^[a-zA-Z0-9 ]+$" // 只允许字母、数字和空格
    }
    stringField("email", "User email") {
        email = true // 验证电子邮件格式
    }
    numberField("age", "User age") {
        min = 0.0
        max = 120.0
    }
}
```

### 验证数据类型

确保输入数据类型正确，防止类型混淆攻击：

```kotlin
val secureSchema = objectInput("Secure input") {
    // 明确指定类型
    stringField("id", "ID")
    numberField("amount", "Amount")
    booleanField("active", "Active status")
    
    // 不允许额外字段
    strict = true
}
```

### 限制输入大小

限制输入大小，防止拒绝服务攻击：

```kotlin
val secureSchema = objectInput("Secure input") {
    stringField("content", "Content") {
        maxLength = 10000 // 限制字符串长度
    }
    arrayField("items", stringInput("Item"), "Items") {
        maxLength = 100 // 限制数组长度
    }
}
```

### 验证嵌套数据

确保验证所有嵌套数据，不留安全漏洞：

```kotlin
val addressSchema = objectInput("Address") {
    stringField("street", "Street") {
        maxLength = 100
    }
    stringField("city", "City") {
        maxLength = 50
    }
    stringField("zipCode", "ZIP code") {
        pattern = "^[0-9]{5}(-[0-9]{4})?$" // 验证 ZIP 码格式
    }
}

val userSchema = objectInput("User") {
    stringField("name", "User name") {
        maxLength = 100
    }
    field("address", addressSchema, "User address") // 验证嵌套对象
}
```

## 数据清理

### 清理用户输入

清理用户输入，防止注入攻击：

```kotlin
val secureSchema = stringInput("User input")
    .transform { input ->
        // 清理 HTML 标签
        input.replace(Regex("<[^>]*>"), "")
    }
```

### 规范化数据

规范化数据，确保一致性和安全性：

```kotlin
val emailSchema = stringInput("Email")
    .transform { email ->
        // 规范化电子邮件地址
        email.trim().lowercase()
    }
```

### 过滤敏感字符

过滤敏感字符，防止跨站脚本攻击：

```kotlin
val secureSchema = stringInput("User input")
    .transform { input ->
        // 转义 HTML 特殊字符
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }
```

## 错误处理

### 安全的错误消息

提供安全的错误消息，不泄露敏感信息：

```kotlin
val secureTool = zodTool<String, String> {
    // ...
    execute = { input ->
        try {
            processInput(input)
        } catch (e: Exception) {
            // 不返回详细的异常信息
            "处理请求时出错，请稍后再试"
        }
    }
}
```

### 集中处理错误

集中处理错误，确保一致的错误处理：

```kotlin
// 集中的错误处理函数
fun handleError(error: Throwable): String {
    // 记录详细错误信息
    logger.error("Error: ${error.message}", error)
    
    // 返回安全的错误消息
    return when (error) {
        is IllegalArgumentException -> "输入参数无效"
        is SecurityException -> "权限不足"
        else -> "处理请求时出错，请稍后再试"
    }
}

val secureTool = zodTool<String, String> {
    // ...
    execute = { input ->
        try {
            processInput(input)
        } catch (e: Exception) {
            handleError(e)
        }
    }
}
```

### 验证错误处理

安全地处理验证错误：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    inputSchema = objectInput("Secure input") {
        // ...
    }
    
    execute = { input ->
        // 输入已经通过验证，可以安全处理
        processInput(input)
    }
}

// 使用工具
val input = getInput()
val result = secureTool.inputSchema.safeParse(input)

when (result) {
    is SchemaResult.Success -> {
        // 处理成功情况
        val output = runBlocking {
            secureTool.execute(result.data)
        }
        // ...
    }
    is SchemaResult.Failure -> {
        // 处理验证失败
        // 不返回详细的错误信息
        "输入验证失败，请检查输入并重试"
    }
}
```

## 权限控制

### 实现访问控制

在工具执行前检查权限：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 检查权限
        val userId = getCurrentUserId()
        if (!hasPermission(userId, "execute_tool")) {
            throw SecurityException("权限不足")
        }
        
        // 执行工具
        processInput(input)
    }
}
```

### 基于角色的访问控制

实现基于角色的访问控制：

```kotlin
// 检查用户角色
fun checkRole(userId: String, requiredRole: String): Boolean {
    val userRoles = getUserRoles(userId)
    return requiredRole in userRoles
}

val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 检查用户角色
        val userId = getCurrentUserId()
        if (!checkRole(userId, "admin")) {
            throw SecurityException("需要管理员权限")
        }
        
        // 执行工具
        processInput(input)
    }
}
```

### 数据级权限控制

实现数据级权限控制：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 获取当前用户
        val userId = getCurrentUserId()
        
        // 检查数据所有权
        val resourceId = input["resourceId"] as String
        if (!isResourceOwner(userId, resourceId) && !isAdmin(userId)) {
            throw SecurityException("无权访问此资源")
        }
        
        // 执行工具
        processInput(input)
    }
}
```

## 敏感数据处理

### 保护敏感数据

保护敏感数据，不在日志或错误消息中泄露：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 获取敏感数据
        val password = input["password"] as String
        
        try {
            // 处理敏感数据
            processCredentials(input["username"] as String, password)
        } catch (e: Exception) {
            // 不记录敏感数据
            logger.error("处理凭据时出错", e)
            throw e
        } finally {
            // 清除敏感数据
            // 在 Kotlin 中无法直接清除字符串，但可以确保不保留引用
        }
        
        "处理成功"
    }
}
```

### 数据加密

加密敏感数据：

```kotlin
val secureTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
    // ...
    execute = { input ->
        // 获取敏感数据
        val creditCard = input["creditCard"] as String
        
        // 加密敏感数据
        val encryptedCreditCard = encryptData(creditCard)
        
        // 返回安全的结果
        mapOf(
            "status" to "success",
            "encryptedData" to encryptedCreditCard
        )
    }
}

// 加密数据
fun encryptData(data: String): String {
    // 使用安全的加密算法
    // ...
    return "encrypted_data"
}
```

### 数据最小化

只收集和处理必要的数据：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    inputSchema = objectInput("User registration") {
        // 只收集必要的字段
        stringField("username", "Username")
        stringField("email", "Email")
        // 不收集不必要的个人信息
    }
    
    execute = { input ->
        // 处理最小化的数据
        registerUser(input)
    }
}
```

## 安全集成

### 安全的 API 集成

安全地与外部 API 集成：

```kotlin
val secureTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
    // ...
    execute = { input ->
        // 创建安全的 HTTP 客户端
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            
            // 配置安全选项
            install(HttpTimeout) {
                requestTimeoutMillis = 5000 // 设置超时
            }
            
            // 配置 TLS
            engine {
                https {
                    trustManager = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                        init(null as KeyStore?)
                    }.trustManagers.first() as X509TrustManager
                }
            }
        }
        
        try {
            // 发送安全的 API 请求
            val response = client.post("https://api.example.com/data") {
                contentType(ContentType.Application.Json)
                setBody(input)
            }
            
            // 验证响应
            val responseBody = response.body<Map<String, Any?>>()
            validateResponse(responseBody)
            
            responseBody
        } finally {
            client.close()
        }
    }
}

// 验证响应
fun validateResponse(response: Map<String, Any?>) {
    // 验证响应数据
    // ...
}
```

### 安全的数据库集成

安全地与数据库集成：

```kotlin
val secureTool = zodTool<Map<String, Any?>, List<Map<String, Any?>>> {
    // ...
    execute = { input ->
        // 使用参数化查询，防止 SQL 注入
        val query = "SELECT * FROM users WHERE username = ?"
        val username = input["username"] as String
        
        // 执行查询
        executeQuery(query, listOf(username))
    }
}

// 执行参数化查询
fun executeQuery(query: String, params: List<Any>): List<Map<String, Any?>> {
    // 使用参数化查询执行 SQL
    // ...
    return emptyList()
}
```

### 安全的文件操作

安全地处理文件：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 获取文件路径
        val filePath = input["filePath"] as String
        
        // 验证文件路径，防止目录遍历攻击
        if (!isValidFilePath(filePath)) {
            throw SecurityException("无效的文件路径")
        }
        
        // 安全地读取文件
        val content = File(filePath).readText()
        
        // 处理文件内容
        processFileContent(content)
    }
}

// 验证文件路径
fun isValidFilePath(path: String): Boolean {
    // 检查路径是否在允许的目录中
    val file = File(path)
    val canonicalPath = file.canonicalPath
    val allowedDirectory = File("/allowed/directory").canonicalPath
    
    return canonicalPath.startsWith(allowedDirectory)
}
```

## 审计和日志

### 安全日志记录

记录安全相关事件：

```kotlin
val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 记录工具执行
        val userId = getCurrentUserId()
        logger.info("用户 $userId 执行工具 ${this.id}")
        
        try {
            // 执行工具
            val result = processInput(input)
            
            // 记录成功执行
            logger.info("用户 $userId 成功执行工具 ${this.id}")
            
            result
        } catch (e: Exception) {
            // 记录执行失败
            logger.error("用户 $userId 执行工具 ${this.id} 失败: ${e.message}")
            throw e
        }
    }
}
```

### 审计跟踪

实现审计跟踪：

```kotlin
// 审计记录
data class AuditRecord(
    val userId: String,
    val action: String,
    val resource: String,
    val timestamp: Long,
    val success: Boolean,
    val details: String
)

// 记录审计
fun recordAudit(userId: String, action: String, resource: String, success: Boolean, details: String) {
    val record = AuditRecord(
        userId = userId,
        action = action,
        resource = resource,
        timestamp = System.currentTimeMillis(),
        success = success,
        details = details
    )
    
    // 保存审计记录
    saveAuditRecord(record)
}

val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 获取用户和资源信息
        val userId = getCurrentUserId()
        val resourceId = input["resourceId"] as String
        
        try {
            // 执行工具
            val result = processInput(input)
            
            // 记录成功审计
            recordAudit(
                userId = userId,
                action = "execute_tool",
                resource = resourceId,
                success = true,
                details = "成功执行工具 ${this.id}"
            )
            
            result
        } catch (e: Exception) {
            // 记录失败审计
            recordAudit(
                userId = userId,
                action = "execute_tool",
                resource = resourceId,
                success = false,
                details = "执行工具 ${this.id} 失败: ${e.message}"
            )
            throw e
        }
    }
}
```

### 监控异常活动

监控异常活动：

```kotlin
// 检测异常活动
fun detectAnomalies(userId: String, action: String): Boolean {
    // 获取用户最近的活动
    val recentActivities = getRecentActivities(userId)
    
    // 检测异常模式
    // ...
    
    return false // 返回是否检测到异常
}

val secureTool = zodTool<Map<String, Any?>, String> {
    // ...
    execute = { input ->
        // 获取用户信息
        val userId = getCurrentUserId()
        
        // 检测异常活动
        if (detectAnomalies(userId, "execute_tool")) {
            // 记录异常活动
            logger.warn("检测到用户 $userId 的异常活动")
            
            // 可能需要额外的验证或限制
            requireAdditionalVerification(userId)
        }
        
        // 执行工具
        processInput(input)
    }
}
```

## 安全测试

### 输入模糊测试

对工具进行输入模糊测试：

```kotlin
@Test
fun `test tool with fuzzy inputs`() {
    val tool = createSecureTool()
    
    // 生成模糊输入
    val fuzzyInputs = generateFuzzyInputs()
    
    // 测试每个模糊输入
    for (input in fuzzyInputs) {
        val result = tool.inputSchema.safeParse(input)
        
        // 验证工具正确处理无效输入
        if (result is SchemaResult.Success) {
            try {
                runBlocking {
                    tool.execute(result.data)
                }
            } catch (e: Exception) {
                // 确保异常被正确处理
                // ...
            }
        }
    }
}

// 生成模糊输入
fun generateFuzzyInputs(): List<Map<String, Any?>> {
    return listOf(
        mapOf("username" to "admin'; DROP TABLE users; --"),
        mapOf("username" to "<script>alert('XSS')</script>"),
        mapOf("username" to "a".repeat(10000)),
        // 更多模糊输入...
    )
}
```

### 安全漏洞测试

测试常见安全漏洞：

```kotlin
@Test
fun `test SQL injection`() {
    val tool = createDatabaseTool()
    
    // SQL 注入测试
    val sqlInjectionInputs = listOf(
        "admin' OR '1'='1",
        "admin'; DROP TABLE users; --",
        "admin' UNION SELECT username, password FROM users; --"
    )
    
    // 测试每个 SQL 注入输入
    for (input in sqlInjectionInputs) {
        val result = tool.inputSchema.safeParse(mapOf("username" to input))
        
        // 验证 SQL 注入被阻止
        if (result is SchemaResult.Success) {
            try {
                runBlocking {
                    tool.execute(result.data)
                }
                fail("SQL 注入未被阻止: $input")
            } catch (e: Exception) {
                // 确保异常被正确处理
                // ...
            }
        }
    }
}

@Test
fun `test XSS`() {
    val tool = createWebTool()
    
    // XSS 测试
    val xssInputs = listOf(
        "<script>alert('XSS')</script>",
        "<img src='x' onerror='alert(\"XSS\")'>",
        "javascript:alert('XSS')"
    )
    
    // 测试每个 XSS 输入
    for (input in xssInputs) {
        val result = tool.inputSchema.safeParse(mapOf("content" to input))
        
        // 验证 XSS 被阻止
        if (result is SchemaResult.Success) {
            val output = runBlocking {
                tool.execute(result.data)
            }
            
            // 确保输出不包含 XSS 载荷
            assertFalse(output.contains("<script>"))
            assertFalse(output.contains("onerror="))
            assertFalse(output.contains("javascript:"))
        }
    }
}
```

### 权限测试

测试权限控制：

```kotlin
@Test
fun `test permission control`() {
    val tool = createSecureTool()
    
    // 测试无权限用户
    setCurrentUser("unprivileged_user")
    
    val input = mapOf("resourceId" to "restricted_resource")
    val result = tool.inputSchema.safeParse(input)
    
    // 验证权限控制
    if (result is SchemaResult.Success) {
        try {
            runBlocking {
                tool.execute(result.data)
            }
            fail("权限控制未生效")
        } catch (e: SecurityException) {
            // 预期的异常
            assertEquals("无权访问此资源", e.message)
        }
    }
    
    // 测试有权限用户
    setCurrentUser("admin_user")
    
    val adminResult = runBlocking {
        tool.execute(result.data)
    }
    
    // 验证有权限用户可以访问
    assertNotNull(adminResult)
}
```

## 总结

安全是一个持续的过程，需要在开发、测试和部署的各个阶段考虑。通过应用本文档中的最佳实践，您可以构建更安全的 ZodTool 实现，保护您的应用程序和用户数据。

记住，安全不仅仅是技术问题，还涉及人员、流程和策略。确保您的团队了解安全最佳实践，并在整个开发生命周期中应用这些实践。
