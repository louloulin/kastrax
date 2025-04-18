# KastraX 快速入门指南

本指南将帮助您快速上手 KastraX 框架，创建您的第一个 AI 代理应用程序。

## 1. 安装

首先，在您的 Gradle 项目中添加 KastraX 依赖。在 `build.gradle.kts` 文件中添加：

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-openai:0.1.0") // 可选，如果您需要使用 OpenAI
}
```

## 2. 创建您的第一个代理

下面是一个简单的例子，展示如何创建一个基本的 AI 代理：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi

fun main() {
    // 创建一个简单的代理
    val myAgent = agent {
        name = "助手"
        instructions = "你是一个有帮助的助手，能够回答用户的问题。"
        model = openAi(
            model = "gpt-3.5-turbo",
            // API 密钥从环境变量 OPENAI_API_KEY 获取
        )
    }

    // 使用代理生成回复
    val response = myAgent.generate("你好，请告诉我关于人工智能的信息。")
    println(response.text)
}
```

确保您已经设置了 `OPENAI_API_KEY` 环境变量，或者直接在代码中提供：

```kotlin
model = openAi(
    apiKey = "your-api-key-here",
    model = "gpt-3.5-turbo"
)
```

## 3. 添加工具

让我们为代理添加一个简单的计算器工具：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.core.schema.*
import ai.kastrax.integrations.openai.openAi

fun main() {
    // 创建一个计算器工具
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
            }
        }

        execute { input ->
            val expression = input.getString("expression")
            val result = evaluateExpression(expression)
            output {
                "result" to result
            }
        }
    }

    // 创建一个带有工具的代理
    val myAgent = agent {
        name = "计算助手"
        instructions = """
            你是一个数学助手，能够解决数学问题。
            当被要求执行计算时，使用计算器工具。
            逐步解释你的推理过程。
        """.trimIndent()
        model = openAi(
            model = "gpt-3.5-turbo",
            // API 密钥从环境变量 OPENAI_API_KEY 获取
        )
        tools {
            tool(calculatorTool)
        }
    }

    // 使用代理
    val response = myAgent.generate("计算 (15 + 5) * 2 的结果是多少？")
    println(response.text)

    // 显示工具使用情况
    if (response.toolCalls.isNotEmpty()) {
        println("\n工具使用情况：")
        response.toolCalls.forEachIndexed { index, toolCall ->
            println("  ${index + 1}. ${toolCall.name}: ${toolCall.arguments}")
            val result = response.toolResults[toolCall.id]
            if (result != null) {
                println("     结果: ${result.result}")
            }
        }
    }
}

// 简单的表达式计算函数
private fun evaluateExpression(expression: String): Int {
    // 这是一个非常简化的计算器实现
    try {
        // 处理加法
        if ("+" in expression) {
            val parts = expression.split("+")
            return parts.sumOf { it.trim().toInt() }
        }

        // 处理减法
        if ("-" in expression) {
            val parts = expression.split("-")
            return parts.first().trim().toInt() - parts.drop(1).sumOf { it.trim().toInt() }
        }

        // 处理乘法
        if ("*" in expression) {
            val parts = expression.split("*")
            return parts.fold(1) { acc, part -> acc * part.trim().toInt() }
        }

        // 处理除法
        if ("/" in expression) {
            val parts = expression.split("/")
            return parts.drop(1).fold(parts.first().trim().toInt()) { acc, part ->
                if (part.trim().toInt() != 0) acc / part.trim().toInt() else acc
            }
        }

        // 如果没有运算符，直接返回数字
        return expression.trim().toInt()
    } catch (e: Exception) {
        return 0
    }
}
```

## 4. 流式响应

对于需要实时显示生成内容的应用程序，您可以使用流式响应：

```kotlin
// 使用流式响应
val response = myAgent.stream("讲一个关于人工智能的故事。")
response.textStream?.collect { chunk ->
    print(chunk) // 实时打印每个文本块
}
```

## 5. 下一步

现在您已经了解了 KastraX 的基础知识，您可以：

- 探索更多[高级工具](tools_zh.md)的创建和使用
- 学习如何使用[内存系统](memory_zh.md)来保持对话上下文
- 了解如何创建和运行[工作流](workflows_zh.md)
- 查看[完整 API 文档](api_zh.md)了解更多详情

## 6. 示例项目

查看 `examples` 目录中的示例项目，了解更多使用场景和最佳实践。
