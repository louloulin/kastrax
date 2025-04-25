# KastraX MCP 服务器

本文档介绍如何使用 KastraX MCP 服务器提供 MCP 服务，允许其他应用程序访问 KastraX 的功能。

## 概述

KastraX MCP 服务器允许您创建符合 MCP 协议的服务器，提供资源、工具和提示。它支持两种传输方式：标准输入/输出和 SSE (Server-Sent Events)。

## 创建服务器

使用 `mcpServer` 函数创建 MCP 服务器：

```kotlin
val mcpServer = mcpServer {
    name = "kastrax-server"
    version = "1.0.0" // 可选，默认为 "1.0.0"
    
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
    
    // 添加提示
    prompt {
        name = "weatherPrompt"
        description = "天气查询提示"
        content = "请提供{{location}}的天气信息。"
        parameters {
            parameter {
                name = "location"
                type = "string"
                description = "位置名称"
                required = true
            }
        }
    }
}
```

## 启动和停止服务器

### 标准输入/输出模式

```kotlin
// 启动服务器（标准输入/输出模式）
mcpServer.start()

// 服务器将一直运行，直到收到关闭请求或程序终止
```

### SSE 模式

```kotlin
// 启动 SSE 服务器
mcpServer.startSSE(host = "localhost", port = 8080)

// 服务器将一直运行，直到调用 stop 方法或程序终止
// mcpServer.stop()
```

## 添加资源

资源是服务器提供的数据，可以是文本、图像、音频等。

### 静态资源

```kotlin
// 添加静态资源
resource {
    name = "documentation"
    description = "KastraX 文档"
    content = "# KastraX\n\nKastraX 是一个强大的 AI 代理框架..."
}
```

### 动态资源

```kotlin
// 添加动态资源
resource {
    name = "userGuide"
    description = "KastraX 用户指南"
    // 使用动态内容提供者
    contentProvider { resourceId ->
        // 从文件、数据库或其他来源获取内容
        val file = java.io.File("docs/user-guide.md")
        file.readText()
    }
}
```

### 资源类型

资源可以有不同的类型：

```kotlin
resource {
    name = "documentation"
    description = "KastraX 文档"
    type = ResourceType.MARKDOWN
    content = "# KastraX\n\nKastraX 是一个强大的 AI 代理框架..."
}

resource {
    name = "logo"
    description = "KastraX 徽标"
    type = ResourceType.IMAGE
    content = "data:image/png;base64,..."
}

resource {
    name = "config"
    description = "KastraX 配置"
    type = ResourceType.JSON
    content = "{\"version\": \"1.0.0\", \"name\": \"KastraX\"}"
}
```

## 添加工具

工具是服务器提供的功能，可以被客户端调用。

### 简单工具

```kotlin
// 添加简单工具
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
```

### 复杂工具

```kotlin
// 添加复杂工具
tool {
    name = "searchDatabase"
    description = "搜索数据库"
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
        parameter {
            name = "offset"
            type = "integer"
            description = "结果偏移量"
            required = false
        }
    }
    handler { params ->
        val query = params["query"] as String
        val limit = params["limit"] as? Int ?: 10
        val offset = params["offset"] as? Int ?: 0
        
        // 搜索数据库的逻辑
        val results = database.search(query, limit, offset)
        
        // 返回 JSON 字符串
        Json.encodeToString(results)
    }
}
```

### 异步工具

```kotlin
// 添加异步工具
tool {
    name = "longRunningTask"
    description = "执行长时间运行的任务"
    parameters {
        parameter {
            name = "taskId"
            type = "string"
            description = "任务 ID"
            required = true
        }
    }
    asyncHandler { params, progress ->
        val taskId = params["taskId"] as String
        
        // 报告进度
        progress(0, "Starting task")
        
        // 执行长时间运行的任务
        for (i in 1..10) {
            // 模拟工作
            delay(1000)
            
            // 报告进度
            progress(i * 10, "Processing step $i")
        }
        
        // 返回结果
        "Task $taskId completed successfully"
    }
}
```

## 添加提示

提示是服务器提供的模板，可以被客户端使用。

### 简单提示

```kotlin
// 添加简单提示
prompt {
    name = "greeting"
    description = "问候提示"
    content = "你好，{{name}}！"
    parameters {
        parameter {
            name = "name"
            type = "string"
            description = "用户名称"
            required = true
        }
    }
}
```

### 复杂提示

```kotlin
// 添加复杂提示
prompt {
    name = "emailTemplate"
    description = "电子邮件模板"
    content = """
        主题：{{subject}}
        
        尊敬的 {{recipient}}，
        
        {{content}}
        
        此致
        敬礼
        
        {{sender}}
    """.trimIndent()
    parameters {
        parameter {
            name = "subject"
            type = "string"
            description = "邮件主题"
            required = true
        }
        parameter {
            name = "recipient"
            type = "string"
            description = "收件人"
            required = true
        }
        parameter {
            name = "content"
            type = "string"
            description = "邮件内容"
            required = true
        }
        parameter {
            name = "sender"
            type = "string"
            description = "发件人"
            required = true
        }
    }
}
```

## 与 KastraX 代理集成

将 KastraX 代理的功能暴露为 MCP 工具：

```kotlin
// 创建一个 KastraX 代理
val weatherAgent = agent {
    name = "Weather Agent"
    instructions = "你是一个天气助手，可以提供天气信息。"
    model = openAi("gpt-4")
}

// 创建 MCP 服务器，暴露代理功能
val mcpServer = mcpServer {
    name = "kastrax-agent-server"
    version = "1.0.0"
    
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
```

## 高级用法

### 自定义资源内容提供者

```kotlin
// 创建自定义资源内容提供者
class FileResourceContentProvider(private val basePath: String) : ResourceContentProvider {
    override suspend fun getResourceContent(resourceId: String): String {
        val file = java.io.File("$basePath/$resourceId")
        if (!file.exists()) {
            throw ResourceNotFoundException(resourceId)
        }
        return file.readText()
    }
}

// 使用自定义资源内容提供者
val fileResourceProvider = FileResourceContentProvider("docs")

// 创建 MCP 服务器
val mcpServer = mcpServer {
    name = "kastrax-docs-server"
    
    // 添加资源
    resource {
        name = "userGuide"
        description = "KastraX 用户指南"
        contentProvider = fileResourceProvider
    }
    
    resource {
        name = "apiReference"
        description = "KastraX API 参考"
        contentProvider = fileResourceProvider
    }
}
```

### 自定义工具处理器

```kotlin
// 创建自定义工具处理器
class DatabaseToolHandler(private val database: Database) : ToolHandler {
    override suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String {
        return when (toolId) {
            "search" -> {
                val query = parameters["query"] as String
                val limit = parameters["limit"] as? Int ?: 10
                val results = database.search(query, limit)
                Json.encodeToString(results)
            }
            "get" -> {
                val id = parameters["id"] as String
                val item = database.get(id)
                Json.encodeToString(item)
            }
            else -> throw ToolNotFoundException(toolId)
        }
    }
}

// 使用自定义工具处理器
val databaseToolHandler = DatabaseToolHandler(database)

// 创建 MCP 服务器
val mcpServer = mcpServer {
    name = "kastrax-database-server"
    
    // 添加工具
    tool {
        name = "search"
        description = "搜索数据库"
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
        handler = databaseToolHandler
    }
    
    tool {
        name = "get"
        description = "获取数据库项目"
        parameters {
            parameter {
                name = "id"
                type = "string"
                description = "项目 ID"
                required = true
            }
        }
        handler = databaseToolHandler
    }
}
```

### 自定义提示渲染器

```kotlin
// 创建自定义提示渲染器
class TemplatePromptRenderer : PromptRenderer {
    override suspend fun renderPrompt(template: String, parameters: Map<String, String>): String {
        var result = template
        for ((key, value) in parameters) {
            result = result.replace("{{$key}}", value)
        }
        return result
    }
}

// 使用自定义提示渲染器
val templatePromptRenderer = TemplatePromptRenderer()

// 创建 MCP 服务器
val mcpServer = mcpServer {
    name = "kastrax-template-server"
    
    // 添加提示
    prompt {
        name = "greeting"
        description = "问候提示"
        content = "你好，{{name}}！"
        parameters {
            parameter {
                name = "name"
                type = "string"
                description = "用户名称"
                required = true
            }
        }
        renderer = templatePromptRenderer
    }
}
```

## 示例

### 文档服务器

```kotlin
// 创建文档服务器
val docsServer = mcpServer {
    name = "kastrax-docs-server"
    
    // 添加文档资源
    resource {
        name = "introduction"
        description = "KastraX 介绍"
        type = ResourceType.MARKDOWN
        content = "# KastraX\n\nKastraX 是一个强大的 AI 代理框架..."
    }
    
    resource {
        name = "quickstart"
        description = "KastraX 快速入门"
        type = ResourceType.MARKDOWN
        content = "# 快速入门\n\n## 安装\n\n```kotlin\ndependencies {\n    implementation(\"ai.kastrax:kastrax-core:0.1.0\")\n}\n```\n\n## 使用\n\n```kotlin\nval agent = agent {\n    name = \"Hello Agent\"\n    instructions = \"你是一个友好的助手。\"\n    model = openAi(\"gpt-4\")\n}\n\nval response = agent.generate(\"你好！\")\nprintln(response.text)\n```"
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
            "搜索结果：关于 \"$query\" 的文档..."
        }
    }
}

// 启动服务器
docsServer.startSSE(port = 8080)
```

### 天气服务器

```kotlin
// 创建天气服务器
val weatherServer = mcpServer {
    name = "kastrax-weather-server"
    
    // 添加天气工具
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
    
    tool {
        name = "getWeatherForecast"
        description = "获取指定位置的天气预报"
        parameters {
            parameter {
                name = "location"
                type = "string"
                description = "位置名称，如 'New York'"
                required = true
            }
            parameter {
                name = "days"
                type = "integer"
                description = "预报天数"
                required = false
            }
        }
        handler { params ->
            val location = params["location"] as String
            val days = params["days"] as? Int ?: 5
            // 获取天气预报的逻辑
            "{\"location\": \"$location\", \"forecast\": [{\"day\": 1, \"temperature\": 25, \"condition\": \"Sunny\"}, ...]}"
        }
    }
}

// 启动服务器
weatherServer.start() // 标准输入/输出模式
```

## 参考

- [MCP 官方文档](https://modelcontextprotocol.io/)
- [KastraX MCP 客户端文档](client.md)
- [KastraX MCP 示例](examples.md)
