# KastraX MCP (Model Context Protocol) 实现计划 (实验性)

## 概述

Model Context Protocol (MCP) 是一个开放协议，用于标准化 AI 模型与外部数据源和工具的集成方式。本文档概述了在 KastraX 框架中实现 MCP 的计划，使 KastraX 能够作为 MCP 客户端和服务器，与其他支持 MCP 的应用程序无缝集成。

## 目标

1. 实现 MCP 客户端功能，允许 KastraX 代理连接到外部 MCP 服务器
2. 实现 MCP 服务器功能，允许其他应用程序通过 MCP 协议访问 KastraX 的功能
3. 提供简单的 API 和 DSL，使开发者能够轻松地使用 MCP 功能
4. 支持 MCP 的核心功能：资源、工具和提示
5. 支持多种传输方式：标准输入/输出和 SSE (Server-Sent Events)

## 架构设计

### 模块结构

```
kastrax/
├── kastrax-mcp/                  # MCP 核心模块
│   ├── src/main/kotlin/
│   │   └── ai/kastrax/mcp/
│   │       ├── client/           # MCP 客户端实现
│   │       │   ├── MCPClient.kt  # MCP 客户端接口
│   │       │   ├── StdioClient.kt # 基于标准输入/输出的客户端
│   │       │   └── SSEClient.kt  # 基于 SSE 的客户端
│   │       ├── server/           # MCP 服务器实现
│   │       │   ├── MCPServer.kt  # MCP 服务器接口
│   │       │   ├── StdioServer.kt # 基于标准输入/输出的服务器
│   │       │   └── SSEServer.kt  # 基于 SSE 的服务器
│   │       ├── protocol/         # 协议定义
│   │       │   ├── Message.kt    # 消息定义
│   │       │   ├── Resource.kt   # 资源定义
│   │       │   ├── Tool.kt       # 工具定义
│   │       │   └── Prompt.kt     # 提示定义
│   │       ├── config/           # 配置
│   │       │   └── MCPConfig.kt  # MCP 配置
│   │       └── MCPDsl.kt         # MCP DSL 定义
│   ├── build.gradle.kts          # 构建配置
│   └── README.md                 # 模块文档
├── kastrax-mcp-examples/         # MCP 示例
│   ├── src/main/kotlin/
│   │   └── ai/kastrax/mcp/examples/
│   │       ├── client/           # 客户端示例
│   │       └── server/           # 服务器示例
│   ├── build.gradle.kts          # 构建配置
│   └── README.md                 # 示例文档
└── docs/
    └── mcp/                      # MCP 文档
        ├── client.md             # 客户端使用文档
        ├── server.md             # 服务器使用文档
        └── examples.md           # 示例文档
```

### 核心组件

1. **MCPClient**：MCP 客户端接口，用于连接到 MCP 服务器
2. **MCPServer**：MCP 服务器接口，用于提供 MCP 服务
3. **MCPConfig**：MCP 配置类，用于配置 MCP 客户端和服务器
4. **MCPDsl**：MCP DSL，用于简化 MCP 的使用

## 实现计划

### 阶段 1：基础设施和协议实现

1. 创建 kastrax-mcp 模块
2. 实现 MCP 协议的基本消息结构
3. 实现基于标准输入/输出的传输层
4. 实现基于 SSE 的传输层
5. 实现基本的客户端和服务器接口

### 阶段 2：客户端功能实现

1. 实现资源发现和获取功能
2. 实现工具调用功能
3. 实现提示获取和使用功能
4. 实现与 KastraX 代理的集成

### 阶段 3：服务器功能实现

1. 实现资源提供功能
2. 实现工具提供功能
3. 实现提示提供功能
4. 实现与 KastraX 代理的集成

### 阶段 4：DSL 和易用性改进

1. 实现 MCP DSL，简化配置和使用
2. 实现自动化工具转换
3. 实现资源自动发现
4. 优化错误处理和日志记录

### 阶段 5：文档和示例

1. 编写详细的使用文档
2. 创建示例应用程序
3. 编写教程和指南

## 示例用法

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
        // 或者使用 SSE 连接到服务器
        // sse {
        //     url = "http://localhost:8080/sse"
        //     headers = mapOf("Authorization" to "Bearer your-token")
        // }
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

## 时间线

- **第 1 周**：完成基础设施和协议实现
- **第 2-3 周**：完成客户端功能实现
- **第 4-5 周**：完成服务器功能实现
- **第 6 周**：完成 DSL 和易用性改进
- **第 7-8 周**：完成文档和示例

## 结论

KastraX MCP 模块的基本架构已经实现，包括协议定义、客户端和服务器接口、以及与 KastraX 代理的集成。然而，由于与现有代码库的兼容性问题，该模块目前处于实验性状态。

### 已实现的功能

- ✅ MCP 协议的基本消息结构
- ✅ 资源、工具和提示的数据结构
- ✅ 客户端和服务器接口
- ✅ 与 KastraX 代理的集成接口

### 待实现的功能

- ❌ 基于标准输入/输出的传输层
- ❌ 基于 SSE 的传输层
- ❌ 客户端和服务器的实际实现
- ❌ 与外部 MCP 服务的集成
- ❌ 单元测试和集成测试

### 下一步计划

1. 解决与现有代码库的兼容性问题
2. 实现基于标准输入/输出的传输层
3. 实现客户端和服务器的具体类
4. 添加单元测试
5. 与外部 MCP 服务（如 Claude 桌面版）集成
