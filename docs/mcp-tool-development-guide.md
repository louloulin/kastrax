# KastraX MCP 工具开发指南

本指南介绍如何使用 KastraX MCP 框架开发和使用工具。

## 概述

Model Context Protocol (MCP) 是一个用于模型和工具之间通信的协议。KastraX MCP 框架提供了一套完整的工具开发和使用接口，使开发者能够轻松创建和使用 MCP 工具。

## 工具开发

### 1. 创建 MCP 服务器

首先，我们需要创建一个 MCP 服务器，用于提供工具服务：

```kotlin
val server = mcpServer {
    name("MyToolServer")
    version("1.0.0")
    
    // 添加工具
    tool {
        name = "my_tool"
        description = "我的工具描述"
        
        // 添加参数
        parameters {
            parameter {
                name = "param1"
                description = "参数1描述"
                type = "string"
                required = true
            }
            
            parameter {
                name = "param2"
                description = "参数2描述"
                type = "number"
                required = false
            }
        }
        
        // 设置执行函数
        handler { params ->
            val param1 = params["param1"] as? String ?: ""
            val param2 = params["param2"] as? Number ?: 0
            
            // 执行工具逻辑
            "工具执行结果: $param1, $param2"
        }
    }
}
```

### 2. 启动服务器

启动服务器有两种方式：标准输入/输出模式和 SSE 模式：

```kotlin
// 标准输入/输出模式
server.start()

// 或者 SSE 模式
server.startSSE(host = "localhost", port = 8080)
```

### 3. 工具实现最佳实践

#### 参数验证

在工具处理函数中，应该始终验证参数：

```kotlin
handler { params ->
    // 验证必需参数
    val param1 = params["param1"] as? String
    if (param1 == null || param1.isBlank()) {
        return@handler "错误: 参数 param1 是必需的"
    }
    
    // 验证可选参数
    val param2 = params["param2"] as? Number ?: 0
    
    // 执行工具逻辑
    "工具执行结果: $param1, $param2"
}
```

#### 错误处理

工具应该优雅地处理错误，并返回有用的错误消息：

```kotlin
handler { params ->
    try {
        // 执行可能失败的操作
        val result = performOperation(params)
        return@handler result
    } catch (e: Exception) {
        return@handler "错误: ${e.message}"
    }
}
```

#### 异步操作

对于需要异步执行的工具，可以使用协程：

```kotlin
handler { params ->
    // 执行异步操作
    val result = withContext(Dispatchers.IO) {
        performAsyncOperation(params)
    }
    
    return@handler result
}
```

#### 安全性考虑

工具应该考虑安全性，例如验证客户端身份和权限：

```kotlin
handler { params ->
    // 获取客户端 ID
    val clientId = params["_clientId"] as? String
    
    // 检查权限
    if (!canAccessResource(clientId, "resource_id")) {
        return@handler "错误: 权限不足"
    }
    
    // 执行工具逻辑
    "工具执行结果"
}
```

## 工具使用

### 1. 创建 MCP 客户端

要使用 MCP 工具，首先需要创建一个 MCP 客户端：

```kotlin
val client = mcpClient {
    name("MyToolClient")
    version("1.0.0")
    
    // 使用标准输入/输出连接到服务器
    server {
        stdio {
            command = "npx"
            args = listOf("tsx", "tool-server.ts")
            env = mapOf("API_KEY" to "your-api-key")
        }
    }
    
    // 或者使用 SSE 连接到服务器
    // server {
    //     sse {
    //         url = "http://localhost:8080"
    //         headers = mapOf("Authorization" to "Bearer your-token")
    //     }
    // }
}
```

### 2. 连接到服务器

连接到服务器：

```kotlin
// 连接到服务器
client.connect()

// 或者使用客户端密钥进行身份验证
// client.connect("your-client-secret")
```

### 3. 获取工具列表

获取服务器提供的工具列表：

```kotlin
val tools = client.tools()
tools.forEach { tool ->
    println("${tool.name}: ${tool.description}")
}
```

### 4. 调用工具

调用工具：

```kotlin
val result = client.callTool("my_tool", mapOf(
    "param1" to "value1",
    "param2" to 42
))
println("工具执行结果: $result")
```

### 5. 断开连接

使用完毕后，断开与服务器的连接：

```kotlin
client.disconnect()
```

## 与 Agent 集成

KastraX MCP 工具可以与 KastraX Agent 集成，使 Agent 能够使用 MCP 工具：

```kotlin
val agent = agent {
    name = "MCP Tool Agent"
    instructions = "你是一个助手，可以使用 MCP 工具。"
    model = openAi("gpt-4")
    
    // 添加 MCP 工具
    tools {
        mcpTools(client)
    }
}

// 使用 Agent
val response = agent.generate("使用 my_tool 工具，参数为 value1 和 42")
println(response.text)
```

## 安全性和访问控制

### 1. 创建安全配置

创建安全配置：

```kotlin
val security = mcpSecurity {
    config {
        enable(true)
        enableAuthentication(true)
        enableAuthorization(true)
        enableTokenValidation(true)
        secretKey("your-secret-key")
        allowClientIds(listOf("client1", "client2"))
        allowServerIds(listOf("server1", "server2"))
        defaultScope(listOf("resources:read", "tools:use", "prompts:read"))
    }
}
```

### 2. 在服务器中使用安全配置

在服务器中使用安全配置：

```kotlin
val server = mcpServer {
    name("SecureToolServer")
    version("1.0.0")
    serverId("server1")
    
    // 设置安全配置
    security(security.getSecurityConfig())
    
    // 添加工具
    tool {
        // ...
    }
}
```

### 3. 在客户端中使用安全配置

在客户端中使用安全配置：

```kotlin
val client = mcpClient {
    name("SecureToolClient")
    version("1.0.0")
    clientId("client1")
    clientSecret("your-client-secret")
    
    server {
        // ...
    }
    
    // 设置安全配置
    security {
        enable(true)
        enableAuthentication(true)
        enableAuthorization(true)
        enableTokenValidation(true)
    }
}
```

### 4. 使用访问令牌

使用访问令牌：

```kotlin
// 获取访问令牌
val token = client.getAccessToken(
    scope = listOf("resources:read", "tools:use"),
    expiresIn = 3600
)

// 设置访问令牌
client.setAccessToken(token)

// 刷新访问令牌
val newToken = client.refreshAccessToken()

// 吊销访问令牌
client.revokeAccessToken()
```

## 服务发现

### 1. 创建发现服务

创建发现服务：

```kotlin
val discoveryService = mcpDiscoveryService {
    // 添加本地注册表
    registry {
        name("Local Registry")
        description("本地 MCP 服务器注册表")
    }
    
    // 添加远程注册表
    registry {
        name("Remote Registry")
        description("远程 MCP 服务器注册表")
        url("https://example.com/mcp-registry")
    }
}
```

### 2. 注册服务器

注册服务器：

```kotlin
val registry = discoveryService.getRegistry("Local Registry")
registry?.registerServer(
    MCPRegistryEntry(
        id = "my-server",
        name = "My Server",
        description = "My MCP server",
        version = "1.0.0",
        capabilities = ServerCapabilities(
            resources = true,
            tools = true,
            prompts = false
        ),
        schemas = listOf(
            ServerSchema(
                command = "npx",
                args = listOf("tsx", "server.ts"),
                env = mapOf(
                    "API_KEY" to EnvVarSchema(
                        description = "API key",
                        required = true
                    )
                )
            )
        )
    )
)
```

### 3. 发现服务器

发现服务器：

```kotlin
// 发现所有服务器
val allServers = discoveryService.discoverServers()

// 根据查询发现服务器
val weatherServers = discoveryService.discoverServers("weather")

// 根据能力发现服务器
val toolServers = discoveryService.discoverServersByCapabilities(listOf("tools"))
```

### 4. 连接到服务器

连接到服务器：

```kotlin
// 连接到服务器
val server = discoveryService.discoverServers("my-server").firstOrNull()
if (server != null) {
    val client = discoveryService.connectToServer(server)
    
    // 使用客户端
    val tools = client.tools()
    // ...
    
    // 断开连接
    client.disconnect()
}
```

## 示例

完整的示例可以在 `examples/src/mcp` 目录中找到，包括：

- 天气查询 MCP 应用案例 (WeatherMCPExample.kt)
- 股票行情 MCP 应用案例 (StockMCPExample.kt)
- 翻译服务 MCP 应用案例 (TranslationMCPExample.kt)
- 知识库查询 MCP 应用案例 (KnowledgeBaseMCPExample.kt)
- 文件操作 MCP 应用案例 (FileMCPExample.kt)
- 服务发现 MCP 应用案例 (MCPDiscoveryExample.kt)
- 安全 MCP 应用案例 (SecureMCPExample.kt)

## 最佳实践

### 1. 工具设计

- 每个工具应该有一个明确的目的和功能
- 工具名称应该简洁明了，反映其功能
- 工具描述应该详细说明其功能和用途
- 参数应该有清晰的名称、类型和描述
- 工具应该返回有用的结果，包括成功和失败情况

### 2. 错误处理

- 工具应该优雅地处理错误，并返回有用的错误消息
- 工具应该验证参数，确保它们符合预期
- 工具应该处理异常，避免崩溃

### 3. 安全性

- 工具应该验证客户端身份和权限
- 敏感数据应该加密存储和传输
- 工具应该限制资源使用，避免滥用

### 4. 性能

- 工具应该高效执行，避免不必要的计算和 I/O
- 长时间运行的工具应该支持取消操作
- 工具应该考虑并发和线程安全

### 5. 文档

- 工具应该有详细的文档，包括功能、参数和返回值
- 文档应该包含示例和使用场景
- 文档应该说明错误处理和限制
