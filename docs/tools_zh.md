# KastraX 工具系统详解

工具系统是 KastraX 框架的核心组件之一，它允许 AI 代理与外部系统交互并执行特定任务。本文档详细介绍了如何创建、配置和使用工具，特别是采用了受 Zod 启发的类型验证系统来简化输入和输出处理。

## 1. 工具的基本概念

在 KastraX 中，工具（Tool）是一个可以被 AI 代理调用的功能单元，它具有以下特点：

- **唯一标识符**：每个工具都有一个唯一的 ID
- **名称和描述**：用于向 AI 模型解释工具的功能
- **输入模式**：定义工具接受的参数格式
- **输出模式**：定义工具返回的结果格式
- **执行逻辑**：实现工具的具体功能

## 2. 创建基本工具（优化版）

KastraX 提供了一个受 Zod 启发的类型验证系统，使工具的创建更加简洁和类型安全：

```kotlin
import ai.kastrax.core.tools.tool
import ai.kastrax.core.schema.*

// 创建一个简单的天气工具
val weatherTool = tool {
    id = "weather"
    name = "天气查询"
    description = "获取指定城市的天气信息"

    // 使用类型安全的模式构建器定义输入
    input {
        obj {
            field("city", string()) {
                description = "要查询天气的城市名称"
                required = true
            }
            field("units", string()) {
                description = "温度单位（celsius 或 fahrenheit）"
                default = "celsius"
                enum("celsius", "fahrenheit")
            }
        }
    }

    // 使用类型安全的模式构建器定义输出
    output {
        obj {
            field("temperature", number()) {
                description = "温度"
            }
            field("condition", string()) {
                description = "天气状况"
            }
            field("humidity", integer()) {
                description = "湿度百分比"
                minimum = 0
                maximum = 100
            }
        }
    }

    // 执行逻辑，使用类型安全的输入参数
    execute { input ->
        val city = input.getString("city")
        val units = input.getString("units", "celsius")

        // 获取天气数据的实现
        val weatherData = getWeatherData(city, units)

        // 返回类型安全的输出
        output {
            "temperature" to weatherData.temperature
            "condition" to weatherData.condition
            "humidity" to weatherData.humidity
        }
    }
}
```

## 3. 类型安全的模式系统

KastraX 的模式系统受到 Zod 的启发，提供了类型安全的方式来定义和验证数据结构：

### 3.1 基本类型

```kotlin
// 字符串
val stringSchema = string() {
    minLength = 3
    maxLength = 100
    pattern = "^[a-zA-Z0-9]+$"
    description = "只允许字母和数字"
}

// 数字
val numberSchema = number() {
    minimum = 0.0
    maximum = 100.0
    multipleOf = 0.5
    description = "0 到 100 之间的数字，步长为 0.5"
}

// 整数
val integerSchema = integer() {
    minimum = 1
    maximum = 10
    description = "1 到 10 之间的整数"
}

// 布尔值
val booleanSchema = boolean() {
    description = "真或假"
}

// 枚举
val enumSchema = enum("small", "medium", "large") {
    description = "尺寸选项"
}
```

### 3.2 复合类型

```kotlin
// 对象
val personSchema = obj {
    field("name", string()) {
        description = "人名"
        required = true
    }
    field("age", integer()) {
        description = "年龄"
        minimum = 0
    }
    field("email", string()) {
        description = "电子邮件"
        pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    }
}

// 数组
val stringArraySchema = array(string()) {
    minItems = 1
    maxItems = 10
    description = "字符串数组，最少 1 项，最多 10 项"
}

// 嵌套对象
val addressSchema = obj {
    field("street", string()) {
        required = true
    }
    field("city", string()) {
        required = true
    }
    field("zipCode", string()) {
        pattern = "^\\d{5}(-\\d{4})?$"
    }
}

val userSchema = obj {
    field("name", string()) {
        required = true
    }
    field("address", addressSchema) {
        required = true
    }
}
```

### 3.3 联合类型和交叉类型

```kotlin
// 联合类型（或）
val idSchema = union(string(), integer()) {
    description = "ID 可以是字符串或整数"
}

// 交叉类型（与）
val basePersonSchema = obj {
    field("name", string()) {
        required = true
    }
    field("age", integer()) {
        required = true
    }
}

val employeeExtensionSchema = obj {
    field("employeeId", string()) {
        required = true
    }
    field("department", string()) {
        required = true
    }
}

val employeeSchema = intersection(basePersonSchema, employeeExtensionSchema) {
    description = "员工信息，包含基本个人信息和员工特定信息"
}
```

## 4. 输入验证和类型转换

KastraX 的模式系统不仅可以验证输入，还可以自动转换类型：

```kotlin
// 定义输入模式
val inputSchema = obj {
    field("id", integer()) {
        description = "用户 ID"
        required = true
    }
    field("name", string()) {
        description = "用户名"
        required = true
    }
    field("active", boolean()) {
        description = "是否激活"
        default = true
    }
}

// 验证和转换输入
fun processInput(jsonInput: String) {
    val input = inputSchema.parse(jsonInput)

    // 类型安全的访问
    val id: Int = input.getInteger("id")
    val name: String = input.getString("name")
    val active: Boolean = input.getBoolean("active", true)

    // 处理数据...
}
```

## 5. 工具执行和错误处理

使用类型安全的模式系统，工具执行变得更加简单和安全：

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"

    input {
        obj {
            field("expression", string()) {
                description = "要计算的数学表达式"
                required = true
            }
        }
    }

    output {
        obj {
            field("result", number()) {
                description = "计算结果"
            }
            field("error", string()) {
                description = "错误信息（如果有）"
                optional = true
            }
        }
    }

    execute { input ->
        try {
            val expression = input.getString("expression")
            val result = evaluateExpression(expression)

            output {
                "result" to result
            }
        } catch (e: Exception) {
            output {
                "result" to 0.0
                "error" to e.message
            }
        }
    }
}
```

## 6. 在代理中使用工具

创建工具后，可以将其添加到代理中：

```kotlin
val myAgent = agent {
    name = "助手"
    instructions = "你是一个有帮助的助手，可以查询天气信息和执行计算。"
    model = openAi("gpt-4o")

    tools {
        tool(weatherTool)
        tool(calculatorTool)
        // 可以添加多个工具
    }
}
```

## 7. 工具调用结果处理

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
            // 类型安全的访问结果
            val output = result.result.asObject()
            val temperature = output.getNumber("temperature")
            val condition = output.getString("condition")

            println("温度: $temperature°C")
            println("天气状况: $condition")
        } else {
            println("错误: ${result?.error ?: "未知错误"}")
        }
    }
}
```

## 8. 高级工具示例

### 8.1 搜索工具

```kotlin
val searchTool = tool {
    id = "search"
    name = "网络搜索"
    description = "搜索互联网上的信息"

    input {
        obj {
            field("query", string()) {
                description = "搜索查询"
                required = true
                minLength = 2
            }
            field("limit", integer()) {
                description = "返回结果数量"
                default = 5
                minimum = 1
                maximum = 10
            }
        }
    }

    output {
        obj {
            field("results", array(obj {
                field("title", string()) {
                    description = "结果标题"
                }
                field("url", string()) {
                    description = "结果 URL"
                }
                field("snippet", string()) {
                    description = "结果摘要"
                }
            })) {
                description = "搜索结果列表"
            }
        }
    }

    execute { input ->
        val query = input.getString("query")
        val limit = input.getInteger("limit", 5)

        val searchResults = searchService.search(query, limit)

        output {
            "results" to searchResults.map { result ->
                mapOf(
                    "title" to result.title,
                    "url" to result.url,
                    "snippet" to result.snippet
                )
            }
        }
    }
}
```

### 8.2 数据库查询工具

```kotlin
val databaseTool = tool {
    id = "database"
    name = "数据库查询"
    description = "查询数据库中的信息"

    input {
        obj {
            field("query", string()) {
                description = "SQL 查询语句（仅支持 SELECT）"
                required = true
                minLength = 10
            }
            field("limit", integer()) {
                description = "最大返回行数"
                default = 10
                minimum = 1
                maximum = 100
            }
        }
    }

    output {
        obj {
            field("results", array(obj {})) {
                description = "查询结果行"
            }
            field("rowCount", integer()) {
                description = "结果行数"
            }
            field("error", string()) {
                description = "错误信息（如果有）"
                optional = true
            }
        }
    }

    execute { input ->
        val query = input.getString("query")
        val limit = input.getInteger("limit", 10)

        // 安全检查，防止危险的 SQL 注入
        if (query.contains("DROP") || query.contains("DELETE") || !query.startsWith("SELECT")) {
            return@execute output {
                "results" to emptyList<Map<String, Any>>()
                "rowCount" to 0
                "error" to "只允许 SELECT 查询"
            }
        }

        try {
            val results = databaseService.executeQuery(query, limit)

            output {
                "results" to results.map { row -> row.toMap() }
                "rowCount" to results.size
            }
        } catch (e: Exception) {
            output {
                "results" to emptyList<Map<String, Any>>()
                "rowCount" to 0
                "error" to "查询执行错误: ${e.message}"
            }
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

## 11. 与 Zod 的比较

KastraX 的模式系统受到 TypeScript 的 Zod 库的启发，但针对 Kotlin 和 AI 代理用例进行了优化：

| 特性 | Zod (TypeScript) | KastraX Schema (Kotlin) |
|------|-----------------|------------------------|
| 语法 | 函数链式调用 | DSL 构建器 |
| 类型安全 | 通过 TypeScript 类型推断 | 通过 Kotlin 类型系统 |
| 验证时机 | 运行时 | 运行时 |
| 错误处理 | 错误对象 | 异常和结果类型 |
| 自定义验证 | 支持 | 支持 |
| 类型转换 | 支持 | 支持 |
| 默认值 | 支持 | 支持 |
| 集成 | 与 TypeScript 生态系统集成 | 与 Kotlin 和 JVM 生态系统集成 |

### Zod 示例（TypeScript）：

```typescript
import { z } from "zod";

const userSchema = z.object({
  name: z.string().min(2),
  age: z.number().int().min(0).optional(),
  email: z.string().email(),
  role: z.enum(["admin", "user", "guest"])
});

type User = z.infer<typeof userSchema>;

function processUser(data: unknown) {
  const user = userSchema.parse(data);
  // user 是类型安全的 User 类型
  console.log(user.name);
}
```

### KastraX Schema 示例（Kotlin）：

```kotlin
val userSchema = obj {
    field("name", string()) {
        minLength = 2
        required = true
    }
    field("age", integer()) {
        minimum = 0
        optional = true
    }
    field("email", string()) {
        pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        required = true
    }
    field("role", enum("admin", "user", "guest")) {
        required = true
    }
}

fun processUser(data: JsonElement) {
    val user = userSchema.parse(data)
    // user 是类型安全的对象
    println(user.getString("name"))
}
```

## 12. 工具开发技巧

1. **使用依赖注入**：通过构造函数注入服务依赖，而不是在工具内部创建它们
2. **缓存结果**：对于昂贵的操作，考虑缓存结果以提高性能
3. **限制资源使用**：实施超时和资源限制，防止工具执行时间过长或消耗过多资源
4. **记录工具调用**：记录工具调用和结果，用于调试和审计
5. **版本控制**：随着工具的演变，考虑实施版本控制机制

## 13. 结论

KastraX 的工具系统通过受 Zod 启发的类型验证系统，提供了一种简洁、类型安全的方式来定义和使用工具。这种方法不仅简化了工具的创建和使用，还提高了代码的可靠性和可维护性。

通过使用类型安全的模式系统，您可以：

1. 减少运行时错误
2. 提高代码可读性
3. 获得更好的 IDE 支持
4. 简化输入验证和类型转换
5. 创建更健壮的 AI 代理应用程序

无论您是构建简单的工具还是复杂的集成，KastraX 的工具系统都能提供所需的灵活性和类型安全性。
