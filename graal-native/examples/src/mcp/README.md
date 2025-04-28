# KastraX MCP 应用案例

本目录包含了 5 个完整的 Model Context Protocol (MCP) 应用案例，展示了如何使用 KastraX MCP 模块创建 MCP 服务器和客户端，以及如何实现各种功能。

## 案例概述

### 1. 天气查询 MCP 应用案例 (WeatherMCPExample.kt)

这个案例展示了如何创建一个提供天气查询功能的 MCP 服务器，以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。

**功能特点：**
- 获取指定城市的当前天气
- 获取指定城市的天气预报
- 支持多个城市的天气查询

### 2. 股票行情 MCP 应用案例 (StockMCPExample.kt)

这个案例展示了如何创建一个提供股票行情查询功能的 MCP 服务器，以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。

**功能特点：**
- 获取指定股票的当前价格
- 获取指定股票的历史价格数据
- 获取市场概览

### 3. 翻译服务 MCP 应用案例 (TranslationMCPExample.kt)

这个案例展示了如何创建一个提供多语言翻译功能的 MCP 服务器，以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。

**功能特点：**
- 将文本从一种语言翻译到另一种语言
- 检测文本的语言
- 获取支持的语言列表

### 4. 知识库查询 MCP 应用案例 (KnowledgeBaseMCPExample.kt)

这个案例展示了如何创建一个提供知识库查询功能的 MCP 服务器，以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。

**功能特点：**
- 获取可用的知识库列表
- 在指定知识库中搜索信息
- 获取指定文档的内容

### 5. 文件操作 MCP 应用案例 (FileMCPExample.kt)

这个案例展示了如何创建一个提供文件操作功能的 MCP 服务器，以及如何使用 MCP 客户端连接到该服务器并调用其提供的工具。

**功能特点：**
- 列出目录内容
- 创建目录
- 读写文本文件
- 获取文件信息
- 复制、移动和删除文件

## 如何运行

每个案例都是一个独立的 Kotlin 文件，可以直接运行。案例中包含了服务器和客户端的完整实现，运行时会自动启动服务器，然后创建客户端连接到服务器并执行各种操作。

```bash
# 运行天气查询案例
./gradlew run -PmainClass=ai.kastrax.examples.mcp.WeatherMCPExampleKt

# 运行股票行情案例
./gradlew run -PmainClass=ai.kastrax.examples.mcp.StockMCPExampleKt

# 运行翻译服务案例
./gradlew run -PmainClass=ai.kastrax.examples.mcp.TranslationMCPExampleKt

# 运行知识库查询案例
./gradlew run -PmainClass=ai.kastrax.examples.mcp.KnowledgeBaseMCPExampleKt

# 运行文件操作案例
./gradlew run -PmainClass=ai.kastrax.examples.mcp.FileMCPExampleKt
```

## 与 KastraX Agent 集成

这些 MCP 服务器可以与 KastraX Agent 集成，使 Agent 能够使用这些服务提供的工具。以下是一个简单的集成示例：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 MCP 客户端
    val weatherClient = mcpClient {
        name("WeatherClient")
        version("1.0.0")
        server {
            sse {
                url = "http://localhost:8081"
            }
        }
    }
    
    // 连接到服务器
    weatherClient.connect()
    
    try {
        // 创建一个使用天气工具的代理
        val agent = agent {
            name = "天气助手"
            instructions = "你是一个天气助手，可以查询各地的天气情况。"
            model = openAi("gpt-4")
            tools {
                mcpTools(weatherClient)
            }
        }
        
        // 使用代理
        val response = agent.generate("请告诉我北京和上海的天气。")
        println("Agent response: ${response.text}")
    } finally {
        // 断开连接
        weatherClient.disconnect()
    }
}
```

## 扩展和自定义

这些案例可以作为创建自己的 MCP 服务器和客户端的起点。您可以：

1. 添加更多工具到现有服务器
2. 创建新的专用 MCP 服务器
3. 将多个 MCP 服务器组合使用
4. 将 MCP 工具与其他 KastraX 工具结合使用

## 注意事项

- 这些案例中的服务器实现使用了模拟数据，实际应用中应该连接到真实的数据源
- 服务器和客户端之间的通信使用了 SSE (Server-Sent Events)，也可以使用标准输入/输出方式
- 为了简化示例，错误处理相对简单，实际应用中应该有更完善的错误处理机制
