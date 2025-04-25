# KastraX MCP 示例

本文档提供了一些使用 KastraX MCP 的示例，帮助您快速上手。

## 基础示例

### 简单的 MCP 客户端

```kotlin
import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 MCP 客户端
    val mcpClient = mcpClient {
        name = "simple-client"
        server {
            stdio {
                command = "npx"
                args = listOf("tsx", "simple-server.ts")
            }
        }
    }
    
    // 连接到服务器
    mcpClient.connect()
    
    try {
        // 获取可用资源
        val resources = mcpClient.resources()
        println("Available resources: ${resources.joinToString()}")
        
        // 获取可用工具
        val tools = mcpClient.tools()
        println("Available tools: ${tools.joinToString()}")
        
        // 调用工具
        val result = mcpClient.callTool("echo", mapOf("text" to "Hello, MCP!"))
        println("Echo result: $result")
    } finally {
        // 断开连接
        mcpClient.disconnect()
    }
}
```

### 简单的 MCP 服务器

```kotlin
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 MCP 服务器
    val mcpServer = mcpServer {
        name = "simple-server"
        
        // 添加资源
        resource {
            name = "greeting"
            description = "问候消息"
            content = "Hello, MCP!"
        }
        
        // 添加工具
        tool {
            name = "echo"
            description = "回显输入的文本"
            parameters {
                parameter {
                    name = "text"
                    type = "string"
                    description = "要回显的文本"
                    required = true
                }
            }
            handler { params ->
                val text = params["text"] as String
                text
            }
        }
    }
    
    // 启动服务器
    mcpServer.start()
}
```

## 与 KastraX 代理集成

### 使用 MCP 工具的代理

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 MCP 客户端
    val weatherClient = mcpClient {
        name = "weather-client"
        server {
            stdio {
                command = "npx"
                args = listOf("tsx", "weather-server.ts")
                env = mapOf("API_KEY" to "your-api-key")
            }
        }
    }
    
    // 连接到服务器
    weatherClient.connect()
    
    try {
        // 创建一个使用 MCP 工具的代理
        val agent = agent {
            name = "Weather Assistant"
            instructions = "你是一个天气助手，可以提供天气信息。"
            
            // 使用 OpenAI 模型
            model = openAi("gpt-4")
            
            // 添加 MCP 工具
            tools {
                mcpTools(weatherClient)
            }
        }
        
        // 使用代理
        val response = agent.generate("纽约的天气怎么样？")
        println("Agent response: ${response.text}")
    } finally {
        // 断开连接
        weatherClient.disconnect()
    }
}
```

### 暴露代理功能为 MCP 服务

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建一个 KastraX 代理
    val weatherAgent = agent {
        name = "Weather Agent"
        instructions = "你是一个天气助手，可以提供天气信息。"
        model = openAi("gpt-4")
    }
    
    // 创建 MCP 服务器，暴露代理功能
    val mcpServer = mcpServer {
        name = "agent-server"
        
        // 添加代理工具
        tool {
            name = "askWeatherAgent"
            description = "向天气代理询问天气信息"
            parameters {
                parameter {
                    name = "question"
                    type = "string"
                    description = "关于天气的问题"
                    required = true
                }
            }
            handler { params ->
                val question = params["question"] as String
                
                // 使用代理生成回答
                val response = weatherAgent.generate(question)
                
                // 返回代理的回答
                response.text
            }
        }
    }
    
    // 启动服务器
    mcpServer.startSSE(port = 8080)
    
    println("Agent server is running at http://localhost:8080")
    println("Press Enter to stop the server...")
    readLine()
    
    // 停止服务器
    mcpServer.stop()
}
```

## 高级示例

### 多服务器配置

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.config.mcpConfig
import ai.kastrax.mcp.MCPManager
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
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
    
    // 创建 MCP 管理器
    val mcpManager = MCPManager(mcpConfig)
    
    try {
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
        val response = agent.generate(
            "纽约的天气怎么样？苹果公司的股票价格是多少？",
            toolsets = toolsets
        )
        println("Agent response: ${response.text}")
    } finally {
        // 断开所有连接
        mcpManager.disconnectAll()
    }
}
```

### 文档服务器

```kotlin
import ai.kastrax.mcp.protocol.ResourceType
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() = runBlocking {
    // 创建文档服务器
    val docsServer = mcpServer {
        name = "kastrax-docs-server"
        
        // 添加文档资源
        val docsDir = File("docs")
        docsDir.listFiles()?.filter { it.extension == "md" }?.forEach { file ->
            resource {
                name = file.nameWithoutExtension
                description = "KastraX ${file.nameWithoutExtension} 文档"
                type = ResourceType.MARKDOWN
                content = file.readText()
            }
        }
        
        // 添加搜索工具
        tool {
            name = "searchDocs"
            description = "搜索文档"
            parameters {
                parameter {
                    name = "query"
                    type = "string"
                    description = "搜索查询"
                    required = true
                }
            }
            handler { params ->
                val query = params["query"] as String
                
                // 搜索文档的逻辑
                val results = docsDir.listFiles()?.filter { it.extension == "md" }
                    ?.map { file -> file.nameWithoutExtension to file.readText() }
                    ?.filter { (_, content) -> content.contains(query, ignoreCase = true) }
                    ?.map { (name, content) ->
                        val snippet = content.lines()
                            .filter { it.contains(query, ignoreCase = true) }
                            .take(3)
                            .joinToString("\n")
                        
                        "文档: $name\n摘要: $snippet"
                    }
                    ?.joinToString("\n\n")
                    ?: "未找到结果"
                
                results
            }
        }
    }
    
    // 启动服务器
    docsServer.startSSE(port = 8080)
    
    println("Documentation server is running at http://localhost:8080")
    println("Press Enter to stop the server...")
    readLine()
    
    // 停止服务器
    docsServer.stop()
}
```

### 与 Claude 桌面版集成

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 MCP 客户端，连接到 Claude 桌面版
    val claudeClient = mcpClient {
        name = "claude-client"
        server {
            stdio {
                command = "claude"
                args = listOf("mcp")
            }
        }
    }
    
    // 连接到服务器
    claudeClient.connect()
    
    try {
        // 获取可用资源
        val resources = claudeClient.resources()
        println("Available resources: ${resources.joinToString()}")
        
        // 获取可用工具
        val tools = claudeClient.tools()
        println("Available tools: ${tools.joinToString()}")
        
        // 创建一个使用 Claude 工具的代理
        val agent = agent {
            name = "Claude Assistant"
            instructions = "你是一个助手，可以使用 Claude 提供的工具。"
            model = openAi("gpt-4")
            tools {
                mcpTools(claudeClient)
            }
        }
        
        // 使用代理
        val response = agent.generate("请帮我查找一下关于人工智能的最新研究。")
        println("Agent response: ${response.text}")
    } finally {
        // 断开连接
        claudeClient.disconnect()
    }
}
```

### 与 GitHub 集成

```kotlin
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

fun main() = runBlocking {
    // 连接到 GitHub
    val github = GitHubBuilder()
        .withOAuthToken(System.getenv("GITHUB_TOKEN"))
        .build()
    
    // 创建 GitHub MCP 服务器
    val githubServer = mcpServer {
        name = "github-server"
        
        // 添加搜索仓库工具
        tool {
            name = "searchRepositories"
            description = "搜索 GitHub 仓库"
            parameters {
                parameter {
                    name = "query"
                    type = "string"
                    description = "搜索查询"
                    required = true
                }
                parameter {
                    name = "limit"
                    type = "integer"
                    description = "结果数量限制"
                    required = false
                }
            }
            handler { params ->
                val query = params["query"] as String
                val limit = params["limit"] as? Int ?: 10
                
                // 搜索 GitHub 仓库
                val searchResult = github.searchRepositories()
                    .q(query)
                    .list()
                    .take(limit)
                    .map { repo ->
                        mapOf(
                            "name" to repo.name,
                            "fullName" to repo.fullName,
                            "description" to (repo.description ?: ""),
                            "url" to repo.htmlUrl.toString(),
                            "stars" to repo.stargazersCount,
                            "forks" to repo.forksCount,
                            "language" to (repo.language ?: "")
                        )
                    }
                
                // 返回 JSON 字符串
                kotlinx.serialization.json.Json.encodeToString(searchResult)
            }
        }
        
        // 添加获取仓库工具
        tool {
            name = "getRepository"
            description = "获取 GitHub 仓库信息"
            parameters {
                parameter {
                    name = "owner"
                    type = "string"
                    description = "仓库所有者"
                    required = true
                }
                parameter {
                    name = "repo"
                    type = "string"
                    description = "仓库名称"
                    required = true
                }
            }
            handler { params ->
                val owner = params["owner"] as String
                val repo = params["repo"] as String
                
                // 获取 GitHub 仓库
                val repository = github.getRepository("$owner/$repo")
                
                // 返回仓库信息
                mapOf(
                    "name" to repository.name,
                    "fullName" to repository.fullName,
                    "description" to (repository.description ?: ""),
                    "url" to repository.htmlUrl.toString(),
                    "stars" to repository.stargazersCount,
                    "forks" to repository.forksCount,
                    "language" to (repository.language ?: ""),
                    "createdAt" to repository.createdAt.toString(),
                    "updatedAt" to repository.updatedAt.toString(),
                    "topics" to repository.topics
                ).toString()
            }
        }
    }
    
    // 启动服务器
    githubServer.startSSE(port = 8080)
    
    println("GitHub server is running at http://localhost:8080")
    println("Press Enter to stop the server...")
    readLine()
    
    // 停止服务器
    githubServer.stop()
}
```

## 参考

- [MCP 官方文档](https://modelcontextprotocol.io/)
- [KastraX MCP 客户端文档](client.md)
- [KastraX MCP 服务器文档](server.md)
