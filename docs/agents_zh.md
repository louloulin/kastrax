# KastraX 代理系统详解

代理系统是 KastraX 框架的核心，它提供了创建和管理 AI 代理的功能。本文档详细介绍了如何创建、配置和使用代理。

## 1. 代理的基本概念

在 KastraX 中，代理（Agent）是一个由大型语言模型（LLM）驱动的智能实体，它可以：

- 理解和响应用户输入
- 使用工具执行任务
- 维护对话上下文
- 生成结构化输出

代理的行为由以下几个关键要素决定：

- **指令**：定义代理的角色、能力和限制
- **模型**：提供代理的智能核心（如 GPT-4、Claude 等）
- **工具**：扩展代理的能力，允许它执行特定任务
- **内存**：存储和检索对话历史和上下文信息

## 2. 创建基本代理

使用 KastraX 的 DSL 可以轻松创建代理：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi

// 创建一个简单的代理
val myAgent = agent {
    name = "助手"
    instructions = """
        你是一个有帮助的助手，能够回答用户的问题。
        始终保持礼貌和专业。
        如果你不知道答案，坦率地承认，不要编造信息。
    """.trimIndent()
    model = openAi("gpt-4o")
}
```

## 3. 代理指令

指令（Instructions）是定义代理行为的关键。良好的指令应该：

- 明确代理的角色和身份
- 定义代理的能力和限制
- 提供行为准则和交互风格
- 包含处理特殊情况的指导

### 3.1 指令示例

#### 客服代理

```kotlin
instructions = """
    你是一家科技公司的客服代表。
    
    你的职责：
    1. 礼貌、专业地回答客户问题
    2. 解决产品相关的技术问题
    3. 处理退款和换货请求
    
    准则：
    - 始终保持耐心和同理心
    - 不要使用技术行话，除非客户先使用
    - 如果无法解决问题，提供升级到人工客服的选项
    
    产品知识：
    - 我们的主要产品是 XYZ 智能手机
    - 常见问题包括电池问题、软件更新和连接问题
    
    公司政策：
    - 30 天退款保证
    - 1 年有限保修
    - 免费软件更新
""".trimIndent()
```

#### 教育辅导代理

```kotlin
instructions = """
    你是一位数学教师，专门辅导高中学生。
    
    你的教学方法：
    1. 引导学生思考，而不是直接给出答案
    2. 提供逐步解释，确保学生理解每个步骤
    3. 使用类比和实际例子使概念更容易理解
    
    教学风格：
    - 鼓励和积极，即使学生犯错
    - 耐心，愿意多次解释复杂概念
    - 适应不同的学习风格
    
    专业知识：
    - 代数、几何、三角学、微积分
    - 能够解决各种难度的数学问题
    
    限制：
    - 不要解答与数学无关的问题
    - 不要完成学生的作业，而是教他们如何解决
""".trimIndent()
```

## 4. 代理模型

KastraX 支持多种 LLM 提供商，每种提供商都有不同的模型选项。选择合适的模型取决于您的需求、预算和性能要求。

### 4.1 OpenAI 模型

```kotlin
// 使用 OpenAI 的 GPT-4o 模型
model = openAi("gpt-4o")

// 使用 OpenAI 的 GPT-3.5 Turbo 模型（更经济）
model = openAi("gpt-3.5-turbo")

// 自定义 API 密钥和基础 URL
model = openAi(
    apiKey = "your-api-key",
    model = "gpt-4o",
    baseUrl = "https://custom-endpoint.openai.azure.com"
)
```

### 4.2 Anthropic 模型（即将推出）

```kotlin
// 使用 Anthropic 的 Claude 3 Opus 模型
model = anthropic("claude-3-opus-20240229")

// 使用 Anthropic 的 Claude 3 Sonnet 模型
model = anthropic("claude-3-sonnet-20240229")
```

### 4.3 Google Gemini 模型（即将推出）

```kotlin
// 使用 Google 的 Gemini Pro 模型
model = gemini("gemini-pro")

// 使用 Google 的 Gemini Ultra 模型
model = gemini("gemini-ultra")
```

## 5. 代理工具

工具扩展了代理的能力，允许它执行特定任务。您可以向代理添加一个或多个工具：

```kotlin
val myAgent = agent {
    name = "多功能助手"
    instructions = "你是一个多功能助手，能够搜索信息和执行计算。"
    model = openAi("gpt-4o")
    
    tools {
        tool(searchTool)
        tool(calculatorTool)
        tool(weatherTool)
    }
}
```

有关工具的详细信息，请参阅[工具系统详解](tools_zh.md)。

## 6. 代理内存（即将推出）

内存系统允许代理存储和检索对话历史和上下文信息：

```kotlin
val myAgent = agent {
    name = "记忆助手"
    instructions = "你是一个有记忆能力的助手，能够记住之前的对话。"
    model = openAi("gpt-4o")
    
    memory {
        storage = inMemoryStorage()
        lastMessages = 10
        semanticRecall = true
    }
}
```

## 7. 使用代理

### 7.1 生成回复

```kotlin
// 生成回复
val response = myAgent.generate("你好，请告诉我关于人工智能的信息。")
println(response.text)

// 使用多轮对话
val messages = listOf(
    LlmMessage(role = LlmMessageRole.USER, content = "你好，我叫张三。"),
    LlmMessage(role = LlmMessageRole.ASSISTANT, content = "你好，张三！很高兴认识你。有什么我可以帮助你的吗？"),
    LlmMessage(role = LlmMessageRole.USER, content = "我想了解一下人工智能。")
)
val response = myAgent.generate(messages)
println(response.text)
```

### 7.2 流式响应

对于需要实时显示生成内容的应用程序，您可以使用流式响应：

```kotlin
// 使用流式响应
val response = myAgent.stream("讲一个关于人工智能的故事。")
response.textStream?.collect { chunk ->
    print(chunk) // 实时打印每个文本块
}
```

### 7.3 处理工具调用

当代理使用工具时，您可以获取和处理工具调用的结果：

```kotlin
val response = myAgent.generate("今天北京的天气怎么样？")

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

## 8. 高级代理配置

### 8.1 生成选项

您可以通过 `AgentGenerateOptions` 自定义代理的生成行为：

```kotlin
val options = AgentGenerateOptions(
    maxSteps = 3,           // 最大工具调用步骤数
    temperature = 0.5,      // 控制随机性（0.0 到 1.0）
    maxTokens = 1000,       // 最大生成令牌数
    executeTools = true,    // 是否执行工具调用
    onStepFinish = { step ->
        // 步骤完成回调
        println("步骤完成: ${step.text}")
        println("工具调用: ${step.toolCalls.size}")
    }
)

val response = myAgent.generate("分析最近的经济数据并计算增长率。", options)
```

### 8.2 结构化输出

您可以要求代理生成结构化输出：

```kotlin
val outputSchema = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject("summary") {
            put("type", "string")
            put("description", "文本摘要")
        }
        putJsonObject("sentiment") {
            put("type", "string")
            put("description", "情感分析（正面、负面或中性）")
        }
        putJsonObject("keyPoints") {
            put("type", "array")
            putJsonObject("items") {
                put("type", "string")
            }
            put("description", "关键点列表")
        }
    }
}

val options = AgentGenerateOptions(
    output = outputSchema
)

val response = myAgent.generate("分析以下文本：'公司第二季度业绩超出预期，收入增长 15%，但成本也上升了 10%。'", options)

// 获取结构化输出
val result = response.result as? JsonObject
if (result != null) {
    val summary = result["summary"]?.jsonPrimitive?.content
    val sentiment = result["sentiment"]?.jsonPrimitive?.content
    val keyPoints = result["keyPoints"]?.jsonArray?.map { it.jsonPrimitive.content }
    
    println("摘要: $summary")
    println("情感: $sentiment")
    println("关键点:")
    keyPoints?.forEach { println("- $it") }
}
```

## 9. 多代理系统

您可以创建多个代理并让它们协同工作：

```kotlin
// 创建多个专业代理
val researchAgent = agent {
    name = "研究员"
    instructions = "你是一个研究专家，负责收集和分析信息。"
    model = openAi("gpt-4o")
    tools {
        tool(searchTool)
    }
}

val writingAgent = agent {
    name = "作家"
    instructions = "你是一个专业作家，负责创作高质量的内容。"
    model = openAi("gpt-4o")
}

val factCheckAgent = agent {
    name = "事实核查员"
    instructions = "你是一个事实核查专家，负责验证信息的准确性。"
    model = openAi("gpt-3.5-turbo")
    tools {
        tool(searchTool)
    }
}

// 使用多代理系统
fun createArticle(topic: String): String {
    // 1. 研究阶段
    val researchResponse = researchAgent.generate("收集关于 '$topic' 的详细信息，包括关键事实、数据和观点。")
    val researchResults = researchResponse.text
    
    // 2. 写作阶段
    val writingPrompt = """
        基于以下研究结果，创作一篇关于 '$topic' 的高质量文章：
        
        $researchResults
        
        文章应该包括引言、主体和结论，并使用清晰、引人入胜的语言。
    """.trimIndent()
    
    val writingResponse = writingAgent.generate(writingPrompt)
    val article = writingResponse.text
    
    // 3. 事实核查阶段
    val factCheckPrompt = """
        请核查以下文章中的事实准确性：
        
        $article
        
        列出任何可能不准确或需要验证的陈述。
    """.trimIndent()
    
    val factCheckResponse = factCheckAgent.generate(factCheckPrompt)
    val factCheckResults = factCheckResponse.text
    
    // 4. 修订阶段
    val revisionPrompt = """
        请根据以下事实核查结果修订文章：
        
        原文：
        $article
        
        事实核查结果：
        $factCheckResults
        
        提供修订后的最终文章。
    """.trimIndent()
    
    val revisionResponse = writingAgent.generate(revisionPrompt)
    return revisionResponse.text
}

// 使用多代理系统创建文章
val article = createArticle("人工智能的伦理挑战")
println(article)
```

## 10. 最佳实践

1. **明确的指令**：提供详细、明确的指令，定义代理的角色、能力和限制
2. **适当的模型选择**：根据任务复杂性和预算选择合适的模型
3. **工具组合**：为代理提供完成任务所需的工具组合
4. **错误处理**：实施适当的错误处理机制，确保代理能够优雅地处理异常情况
5. **安全考虑**：实施适当的安全措施，防止代理执行危险操作
6. **性能优化**：优化代理配置，减少不必要的 API 调用和延迟
7. **测试和评估**：定期测试和评估代理的性能，确保它符合预期

## 11. 代理开发技巧

1. **迭代开发**：从简单的代理开始，逐步添加功能和复杂性
2. **指令工程**：花时间优化代理指令，这对代理性能有显著影响
3. **用户反馈**：收集和整合用户反馈，不断改进代理
4. **监控和日志**：实施监控和日志记录，以便调试和优化
5. **A/B 测试**：对不同的代理配置进行 A/B 测试，找出最佳方案
