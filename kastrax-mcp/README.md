# KastraX MCP (实验性)

KastraX MCP 是 [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) 在 KastraX 框架中的实现。它允许 KastraX 代理与支持 MCP 的应用程序和服务进行无缝集成。

> **注意：该模块目前处于实验性状态，尚未准备好用于生产环境。**

## 功能

- **MCP 客户端**：连接到 MCP 服务器，获取资源、调用工具和使用提示
- **MCP 服务器**：提供 MCP 服务，允许其他应用程序访问 KastraX 的功能
- **多种传输方式**：支持标准输入/输出和 SSE (Server-Sent Events) 传输
- **与 KastraX 代理集成**：将 MCP 工具和资源与 KastraX 代理无缝集成
- **简单的 DSL**：提供简单的 DSL，使开发者能够轻松地使用 MCP 功能

## 安装

在项目的 `build.gradle.kts` 文件中添加以下依赖：

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-mcp:0.1.0")
}
```

## 使用方法

### 客户端示例

```kotlin
// 创建 MCP 客户端
val mcpClient = mcpClient {
    name = "kastrax-client"
    server {
        // 使用标准输入/输出连接到服务器
        stdio {
            command = "npx"
            args = listOf("tsx", "weather-server.ts")
            env = mapOf("API_KEY" to "your-api-key")
        }
    }
}

// 连接到服务器
mcpClient.connect()

// 获取可用资源
val resources = mcpClient.resources()
println("Available resources: ${resources.joinToString()}")

// 获取可用工具
val tools = mcpClient.tools()
println("Available tools: ${tools.joinToString()}")

// 调用工具
val result = mcpClient.callTool("getWeather", mapOf("location" to "New York"))
println("Weather in New York: $result")

// 断开连接
mcpClient.disconnect()
```

### 服务器示例

```kotlin
// 创建 MCP 服务器
val mcpServer = mcpServer {
    name = "kastrax-server"
    version = "1.0.0"

    // 添加资源
    resource {
        name = "documentation"
        description = "KastraX 文档"
        content = "# KastraX\n\nKastraX 是一个强大的 AI 代理框架..."
    }

    // 添加工具
    tool {
        name = "getWeather"
        description = "获取指定位置的天气信息"
        parameters {
            parameter {
                name = "location"
                type = "string"
                description = "位置名称，如 'New York'"
                required = true
            }
        }
        handler { params ->
            val location = params["location"] as String
            // 获取天气信息的逻辑
            "{\"location\": \"$location\", \"temperature\": 25, \"condition\": \"Sunny\"}"
        }
    }
}

// 启动服务器（标准输入/输出模式）
mcpServer.start()

// 或者启动 SSE 服务器
// mcpServer.startSSE(port = 8080)
```

### 与 KastraX 代理集成

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

### 配置多个 MCP 服务器

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

// 创建 MCP 客户端
val stockPriceClient = mcpClient {
    name = "stock-price-client"
    server {
        stdio {
            command = "npx"
            args = listOf("tsx", "stock-price.ts")
            env = mapOf("API_KEY" to "your-api-key")
        }
    }
}

val weatherClient = mcpClient {
    name = "weather-client"
    server {
        sse {
            url = "http://localhost:8080/sse"
            headers = mapOf("Authorization" to "Bearer your-token")
        }
    }
}

// 创建一个使用多个 MCP 工具的代理
val agent = agent {
    name = "Multi-tool Assistant"
    instructions = "你是一个助手，可以提供股票价格和天气信息。"

    // 使用 OpenAI 模型
    model = openAi("gpt-4")

    // 添加 MCP 工具
    tools {
        mcpTools(stockPriceClient)
        mcpTools(weatherClient)
    }
}

// 使用代理
val response = agent.generate("纽约的天气怎么样？苹果公司的股票价格是多少？")
println(response.text)
```

## 文档

更多详细信息，请参阅 [KastraX MCP 文档](https://kastrax.ai/docs/mcp/)。

## 许可证

KastraX MCP 使用 Apache 2.0 许可证。详情请参阅 [LICENSE](LICENSE) 文件。
