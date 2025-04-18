# KastraX 工具系统详解

工具系统是 KastraX 框架的核心组件之一，它允许 AI 代理与外部系统交互并执行特定任务。本文档详细介绍了如何创建、配置和使用工具。

## 1. 工具的基本概念

在 KastraX 中，工具（Tool）是一个可以被 AI 代理调用的功能单元，它具有以下特点：

- **唯一标识符**：每个工具都有一个唯一的 ID
- **名称和描述**：用于向 AI 模型解释工具的功能
- **输入模式**：定义工具接受的参数格式
- **输出模式**：定义工具返回的结果格式
- **执行逻辑**：实现工具的具体功能

## 2. 创建基本工具

使用 KastraX 的 DSL 可以轻松创建工具：

```kotlin
import ai.kastrax.core.tools.tool
import kotlinx.serialization.json.*

// 创建一个简单的天气工具
val weatherTool = tool {
    id = "weather"
    name = "天气查询"
    description = "获取指定城市的天气信息"
    
    // 定义输入模式
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("city") {
                put("type", "string")
                put("description", "要查询天气的城市名称")
            }
        }
        putJsonArray("required") {
            add("city")
        }
    }
    
    // 定义输出模式
    outputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("temperature") {
                put("type", "number")
                put("description", "温度（摄氏度）")
            }
            putJsonObject("condition") {
                put("type", "string")
                put("description", "天气状况")
            }
        }
    }
    
    // 实现执行逻辑
    execute = { input ->
        val city = input.jsonObject["city"]?.jsonPrimitive?.content ?: ""
        
        // 这里应该是实际的天气 API 调用
        // 为了示例，我们返回模拟数据
        buildJsonObject {
            put("temperature", 25)
            put("condition", "晴天")
        }
    }
}
```

## 3. 输入和输出模式

工具的输入和输出模式使用 JSON Schema 定义，这有助于 AI 模型理解如何正确调用工具。

### 3.1 常见的输入模式类型

#### 字符串输入

```kotlin
inputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("query") {
            put("type", "string")
            put("description", "搜索查询")
        }
    }
    putJsonArray("required") {
        add("query")
    }
}
```

#### 数字输入

```kotlin
inputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("amount") {
            put("type", "number")
            put("description", "金额")
        }
    }
    putJsonArray("required") {
        add("amount")
    }
}
```

#### 布尔输入

```kotlin
inputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("includeDetails") {
            put("type", "boolean")
            put("description", "是否包含详细信息")
        }
    }
}
```

#### 枚举输入

```kotlin
inputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("sortOrder") {
            put("type", "string")
            put("description", "排序顺序")
            putJsonArray("enum") {
                add("ascending")
                add("descending")
            }
        }
    }
}
```

#### 复杂对象输入

```kotlin
inputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("user") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("name") {
                    put("type", "string")
                    put("description", "用户名")
                }
                putJsonObject("age") {
                    put("type", "integer")
                    put("description", "年龄")
                }
            }
        }
    }
}
```

## 4. 工具执行逻辑

工具的执行逻辑是一个 lambda 函数，它接收输入参数并返回结果：

```kotlin
execute = { input ->
    // 从输入中提取参数
    val param1 = input.jsonObject["param1"]?.jsonPrimitive?.content
    val param2 = input.jsonObject["param2"]?.jsonPrimitive?.int
    
    // 执行业务逻辑
    val result = someBusinessLogic(param1, param2)
    
    // 返回结果
    buildJsonObject {
        put("result", result)
    }
}
```

## 5. 异步工具

对于需要执行异步操作的工具，可以利用 Kotlin 的协程：

```kotlin
execute = { input ->
    val query = input.jsonObject["query"]?.jsonPrimitive?.content ?: ""
    
    // 异步调用外部 API
    val response = httpClient.get("https://api.example.com/search?q=$query")
    val data = response.body<SearchResponse>()
    
    // 处理结果并返回
    buildJsonObject {
        put("totalResults", data.totalResults)
        putJsonArray("items") {
            data.items.forEach { item ->
                addJsonObject {
                    put("title", item.title)
                    put("url", item.url)
                }
            }
        }
    }
}
```

## 6. 工具错误处理

工具执行过程中可能会遇到错误，应当妥善处理：

```kotlin
execute = { input ->
    try {
        val city = input.jsonObject["city"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("城市参数不能为空")
        
        val weatherData = weatherService.getWeather(city)
        
        buildJsonObject {
            put("temperature", weatherData.temperature)
            put("condition", weatherData.condition)
        }
    } catch (e: Exception) {
        // 返回错误信息
        buildJsonObject {
            put("error", true)
            put("message", "获取天气信息失败: ${e.message}")
        }
    }
}
```

## 7. 在代理中使用工具

创建工具后，可以将其添加到代理中：

```kotlin
val myAgent = agent {
    name = "助手"
    instructions = "你是一个有帮助的助手，可以查询天气信息。"
    model = openAi("gpt-3.5-turbo")
    
    tools {
        tool(weatherTool)
        tool(calculatorTool)
        // 可以添加多个工具
    }
}
```

## 8. 工具调用结果处理

当代理调用工具时，您可以获取和处理工具调用的结果：

```kotlin
val response = myAgent.generate("北京今天的天气怎么样？")

// 检查是否有工具调用
if (response.toolCalls.isNotEmpty()) {
    println("工具调用情况：")
    response.toolCalls.forEach { toolCall ->
        println("工具: ${toolCall.name}")
        println("参数: ${toolCall.arguments}")
        
        // 获取工具调用结果
        val result = response.toolResults[toolCall.id]
        if (result != null && result.success) {
            println("结果: ${result.result}")
        } else {
            println("错误: ${result?.error ?: "未知错误"}")
        }
    }
}
```

## 9. 常见工具示例

### 9.1 搜索工具

```kotlin
val searchTool = tool {
    id = "search"
    name = "网络搜索"
    description = "搜索互联网上的信息"
    
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "搜索查询")
            }
            putJsonObject("limit") {
                put("type", "integer")
                put("description", "返回结果数量")
                put("default", 5)
            }
        }
        putJsonArray("required") {
            add("query")
        }
    }
    
    execute = { input ->
        val query = input.jsonObject["query"]?.jsonPrimitive?.content ?: ""
        val limit = input.jsonObject["limit"]?.jsonPrimitive?.int ?: 5
        
        val searchResults = searchService.search(query, limit)
        
        buildJsonObject {
            putJsonArray("results") {
                searchResults.forEach { result ->
                    addJsonObject {
                        put("title", result.title)
                        put("url", result.url)
                        put("snippet", result.snippet)
                    }
                }
            }
        }
    }
}
```

### 9.2 数据库查询工具

```kotlin
val databaseTool = tool {
    id = "database"
    name = "数据库查询"
    description = "查询数据库中的信息"
    
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
                put("description", "SQL 查询语句")
            }
        }
        putJsonArray("required") {
            add("query")
        }
    }
    
    execute = { input ->
        val query = input.jsonObject["query"]?.jsonPrimitive?.content ?: ""
        
        // 安全检查，防止危险的 SQL 注入
        if (query.contains("DROP") || query.contains("DELETE") || !query.startsWith("SELECT")) {
            return@tool buildJsonObject {
                put("error", true)
                put("message", "只允许 SELECT 查询")
            }
        }
        
        val results = databaseService.executeQuery(query)
        
        buildJsonObject {
            putJsonArray("results") {
                results.forEach { row ->
                    addJsonObject {
                        row.forEach { (column, value) ->
                            put(column, value.toString())
                        }
                    }
                }
            }
            put("rowCount", results.size)
        }
    }
}
```

## 10. 最佳实践

1. **明确的描述**：为工具提供清晰、详细的描述，帮助 AI 模型理解何时以及如何使用它
2. **严格的输入验证**：验证所有输入参数，确保它们符合预期
3. **优雅的错误处理**：妥善处理所有可能的错误情况，并提供有用的错误消息
4. **安全性考虑**：对于可能涉及敏感操作的工具（如数据库查询），实施适当的安全检查
5. **模块化设计**：将复杂的工具拆分为更小、更专注的工具
6. **测试覆盖**：为工具编写全面的单元测试，确保它们按预期工作

## 11. 工具开发技巧

1. **使用依赖注入**：通过构造函数注入服务依赖，而不是在工具内部创建它们
2. **缓存结果**：对于昂贵的操作，考虑缓存结果以提高性能
3. **限制资源使用**：实施超时和资源限制，防止工具执行时间过长或消耗过多资源
4. **记录工具调用**：记录工具调用和结果，用于调试和审计
5. **版本控制**：随着工具的演变，考虑实施版本控制机制
