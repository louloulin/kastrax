# KastraX MCP 客户端

本文档介绍如何使用 KastraX MCP 客户端连接到 MCP 服务器并与之交互。

## 概述

KastraX MCP 客户端允许 KastraX 代理连接到支持 MCP 的服务器，获取资源、调用工具和使用提示。它支持两种传输方式：标准输入/输出和 SSE (Server-Sent Events)。

## 创建客户端

使用 `mcpClient` 函数创建 MCP 客户端：

```kotlin
val mcpClient = mcpClient {
    name = "kastrax-client"
    version = "1.0.0" // 可选，默认为 "1.0.0"
    
    server {
        // 使用标准输入/输出连接到服务器
        stdio {
            command = "npx"
            args = listOf("tsx", "weather-server.ts")
            env = mapOf("API_KEY" to "your-api-key")
        }
        
        // 或者使用 SSE 连接到服务器
        // sse {
        //     url = "http://localhost:8080/sse"
        //     headers = mapOf("Authorization" to "Bearer your-token")
        // }
    }
    
    // 设置超时时间（毫秒），可选，默认为 60000（60 秒）
    timeout(30000)
}
```

## 连接和断开

在使用客户端之前，需要先连接到服务器：

```kotlin
// 连接到服务器
mcpClient.connect()

// 使用客户端...

// 断开连接
mcpClient.disconnect()
```

建议使用 `use` 函数自动管理连接和断开：

```kotlin
mcpClient.use { client ->
    // 使用客户端...
}
```

## 获取资源

获取服务器提供的资源列表：

```kotlin
val resources = mcpClient.resources()
println("Available resources: ${resources.joinToString()}")
```

获取资源内容：

```kotlin
val resourceContent = mcpClient.getResource("documentation")
println("Documentation: $resourceContent")
```

## 调用工具

获取服务器提供的工具列表：

```kotlin
val tools = mcpClient.tools()
println("Available tools: ${tools.joinToString()}")
```

调用工具：

```kotlin
val result = mcpClient.callTool("getWeather", mapOf("location" to "New York"))
println("Weather in New York: $result")
```

## 使用提示

获取服务器提供的提示列表：

```kotlin
val prompts = mcpClient.prompts()
println("Available prompts: ${prompts.joinToString()}")
```

获取提示内容：

```kotlin
val promptContent = mcpClient.getPrompt("weatherPrompt")
println("Weather prompt: $promptContent")
```

## 与 KastraX 代理集成

将 MCP 工具与 KastraX 代理集成：

```kotlin
// 创建一个使用 MCP 工具的代理
val agent = agent {
    name = "Weather Assistant"
    instructions = "你是一个天气助手，可以提供天气信息。"
    
    // 使用 OpenAI 模型
    model = openAi("gpt-4")
    
    // 添加 MCP 工具
    tools {
        mcpTools(mcpClient)
    }
}

// 使用代理
val response = agent.generate("纽约的天气怎么样？")
println(response.text)
```

## 配置多个 MCP 服务器

使用 `MCPConfiguration` 管理多个 MCP 服务器：

```kotlin
// 创建 MCP 配置
val mcpConfig = mcpConfig {
    // 添加股票价格服务器
    stdioServer("stockPrice") {
        command("npx")
        args("tsx", "stock-price.ts")
        env("API_KEY", "your-api-key")
    }
    
    // 添加天气服务器
    sseServer("weather") {
        url("http://localhost:8080/sse")
        header("Authorization", "Bearer your-token")
    }
}

// 创建 MCP 客户端管理器
val mcpManager = MCPManager(mcpConfig)

// 获取所有工具集
val toolsets = mcpManager.getToolsets()

// 创建一个使用多个 MCP 工具的代理
val agent = agent {
    name = "Multi-tool Assistant"
    instructions = "你是一个助手，可以提供股票价格和天气信息。"
    
    // 使用 OpenAI 模型
    model = openAi("gpt-4")
}

// 使用代理，传入工具集
val response = agent.generate("纽约的天气怎么样？苹果公司的股票价格是多少？", toolsets = toolsets)
println(response.text)
```

## 错误处理

MCP 客户端可能会抛出以下异常：

- `MCPConnectionException`：连接到服务器时发生错误
- `MCPRequestException`：发送请求时发生错误
- `MCPResponseException`：接收响应时发生错误
- `ResourceNotFoundException`：请求的资源不存在
- `ToolNotFoundException`：请求的工具不存在
- `PromptNotFoundException`：请求的提示不存在
- `ToolExecutionException`：执行工具时发生错误

建议使用 try-catch 块处理这些异常：

```kotlin
try {
    val result = mcpClient.callTool("getWeather", mapOf("location" to "New York"))
    println("Weather in New York: $result")
} catch (e: ToolNotFoundException) {
    println("Tool not found: ${e.message}")
} catch (e: ToolExecutionException) {
    println("Tool execution failed: ${e.message}")
} catch (e: MCPException) {
    println("MCP error: ${e.message}")
}
```

## 高级用法

### 自定义超时

设置客户端的超时时间：

```kotlin
val mcpClient = mcpClient {
    name = "kastrax-client"
    server {
        stdio {
            command = "npx"
            args = listOf("tsx", "weather-server.ts")
        }
    }
    timeout(30000) // 30 秒
}
```

### 检查服务器能力

检查服务器是否支持特定功能：

```kotlin
if (mcpClient.supportsCapability("tools")) {
    println("Server supports tools")
}

if (mcpClient.supportsCapability("resources")) {
    println("Server supports resources")
}

if (mcpClient.supportsCapability("prompts")) {
    println("Server supports prompts")
}

if (mcpClient.supportsCapability("sampling")) {
    println("Server supports sampling")
}
```

### 使用 MCP 工具集

获取 MCP 工具集，然后将其传递给代理：

```kotlin
// 获取 MCP 工具集
val mcpToolset = mcpClient.getToolset()

// 创建一个使用 MCP 工具集的代理
val agent = agent {
    name = "Weather Assistant"
    instructions = "你是一个天气助手，可以提供天气信息。"
    model = openAi("gpt-4")
}

// 使用代理，传入工具集
val response = agent.generate("纽约的天气怎么样？", toolsets = mapOf("weather" to mcpToolset))
println(response.text)
```

## 示例

### 连接到 Claude 桌面版 MCP 服务器

```kotlin
val claudeClient = mcpClient {
    name = "claude-client"
    server {
        stdio {
            command = "claude"
            args = listOf("mcp")
        }
    }
}

claudeClient.use { client ->
    val resources = client.resources()
    println("Available resources: ${resources.joinToString()}")
    
    val tools = client.tools()
    println("Available tools: ${tools.joinToString()}")
    
    val agent = agent {
        name = "Claude Assistant"
        instructions = "你是一个助手，可以使用 Claude 提供的工具。"
        model = openAi("gpt-4")
        tools {
            mcpTools(client)
        }
    }
    
    val response = agent.generate("请帮我查找一下关于人工智能的最新研究。")
    println(response.text)
}
```

### 连接到 GitHub MCP 服务器

```kotlin
val githubClient = mcpClient {
    name = "github-client"
    server {
        sse {
            url = "https://api.github.com/mcp/sse"
            headers = mapOf(
                "Authorization" to "Bearer your-github-token",
                "Accept" to "application/vnd.github.v3+json"
            )
        }
    }
}

githubClient.use { client ->
    val tools = client.tools()
    println("Available GitHub tools: ${tools.joinToString()}")
    
    val result = client.callTool("searchRepositories", mapOf("query" to "language:kotlin stars:>1000"))
    println("Kotlin repositories with more than 1000 stars: $result")
}
```

## 参考

- [MCP 官方文档](https://modelcontextprotocol.io/)
- [KastraX MCP 服务器文档](server.md)
- [KastraX MCP 示例](examples.md)
