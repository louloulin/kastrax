# KastraX 专用代理模板

本文档介绍了 KastraX 框架中的专用代理模板功能，这些模板可以帮助开发者快速创建针对特定场景的 AI 代理。

## 1. 专用代理模板概述

KastraX 提供了一系列预设的代理模板，每个模板都针对特定的使用场景进行了优化，包括：

- **客服代理**：专门用于客户服务和支持场景
- **研究助手代理**：专门用于深入研究和信息收集
- **创意写作代理**：专门用于创意内容创作
- **编程助手代理**：专门用于编程和代码相关任务
- **教育辅导代理**：专门用于教育和学习辅导

这些模板不仅提供了针对特定场景的系统提示（instructions），还预设了适合该场景的参数配置，如温度（temperature）和最大步骤数（maxSteps）。

## 2. 使用专用代理模板

### 2.1 基本用法

使用专用代理模板非常简单，只需调用相应的工厂方法：

```kotlin
import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.integrations.openai.openAi

// 创建客服代理
val customerServiceAgent = AgentTemplates.createCustomerServiceAgent(
    name = "客服助手",
    model = openAi("gpt-4o")
)

// 创建研究助手代理
val researchAgent = AgentTemplates.createResearchAssistantAgent(
    name = "研究助手",
    model = openAi("gpt-4o")
)
```

### 2.2 添加工具

您可以为专用代理添加工具，以增强其能力：

```kotlin
import ai.kastrax.core.tools.web.WebSearchTool

// 创建网络搜索工具
val webSearchTool = WebSearchTool(
    apiKey = "your-api-key",
    searchEngineId = "your-search-engine-id"
)

// 创建带有工具的研究助手代理
val researchAgent = AgentTemplates.createResearchAssistantAgent(
    name = "研究助手",
    model = openAi("gpt-4o"),
    additionalTools = mapOf("web_search" to webSearchTool)
)
```

### 2.3 自定义指令

您可以通过提供自定义指令来覆盖模板的默认指令：

```kotlin
val customAgent = AgentTemplates.createProgrammingAssistantAgent(
    name = "Kotlin专家",
    model = openAi("gpt-4o"),
    customInstructions = """
        你是一名Kotlin编程专家，专注于帮助用户解决Kotlin相关的编程问题。
        你应该提供清晰、简洁、高效的Kotlin代码示例，并解释代码的工作原理。
        特别关注Kotlin的协程、DSL和函数式编程特性。
    """.trimIndent()
)
```

### 2.4 添加记忆系统

您可以为专用代理添加记忆系统，以便它能够记住对话历史：

```kotlin
import ai.kastrax.memory.impl.MemoryImpl
import ai.kastrax.memory.impl.InMemoryStorage

// 创建内存系统
val memory = MemoryImpl(InMemoryStorage())

// 创建带有记忆系统的代理
val agent = AgentTemplates.createEducationalTutorAgent(
    name = "教育辅导员",
    model = openAi("gpt-4o"),
    memory = memory
)
```

## 3. 持久化状态和会话管理

KastraX 现在支持持久化的状态和会话管理，可以与专用代理模板结合使用：

```kotlin
import ai.kastrax.core.agent.SQLiteStateManager
import ai.kastrax.core.agent.SQLiteSessionManager
import ai.kastrax.core.agent.AgentGenerateOptions

// 创建持久化的状态和会话管理器
val stateManager = SQLiteStateManager("agent_state.db")
val sessionManager = SQLiteSessionManager("agent_session.db")

// 创建代理
val agent = AgentTemplates.createCustomerServiceAgent(
    name = "客服助手",
    model = openAi("gpt-4o")
)

// 设置状态和会话管理
agent.stateManager = stateManager
agent.sessionManager = sessionManager

// 创建会话
val session = agent.createSession(
    title = "用户会话",
    resourceId = "user-123"
)

// 生成响应
val response = agent.generate(
    "你好，我需要帮助",
    options = AgentGenerateOptions(
        threadId = session.id
    )
)
```

## 4. 可用的专用代理模板

### 4.1 客服代理

```kotlin
val customerServiceAgent = AgentTemplates.createCustomerServiceAgent(
    name = "客服助手",
    model = openAi("gpt-4o")
)
```

**默认配置**：
- 温度（temperature）：0.7
- 最大步骤数（maxSteps）：3

**适用场景**：
- 客户服务和支持
- 问题解答
- 投诉处理
- 产品信息咨询

### 4.2 研究助手代理

```kotlin
val researchAgent = AgentTemplates.createResearchAssistantAgent(
    name = "研究助手",
    model = openAi("gpt-4o")
)
```

**默认配置**：
- 温度（temperature）：0.3
- 最大步骤数（maxSteps）：5

**适用场景**：
- 深入研究和分析
- 信息收集和整理
- 文献综述
- 数据分析

### 4.3 创意写作代理

```kotlin
val creativeWritingAgent = AgentTemplates.createCreativeWritingAgent(
    name = "创意写手",
    model = openAi("gpt-4o")
)
```

**默认配置**：
- 温度（temperature）：0.8
- 最大步骤数（maxSteps）：2

**适用场景**：
- 故事创作
- 诗歌和散文
- 剧本写作
- 创意内容生成

### 4.4 编程助手代理

```kotlin
val programmingAssistantAgent = AgentTemplates.createProgrammingAssistantAgent(
    name = "编程助手",
    model = openAi("gpt-4o")
)
```

**默认配置**：
- 温度（temperature）：0.2
- 最大步骤数（maxSteps）：4

**适用场景**：
- 代码编写和调试
- 算法设计
- 技术问题解答
- 代码优化和重构

### 4.5 教育辅导代理

```kotlin
val educationalTutorAgent = AgentTemplates.createEducationalTutorAgent(
    name = "教育辅导员",
    model = openAi("gpt-4o")
)
```

**默认配置**：
- 温度（temperature）：0.5
- 最大步骤数（maxSteps）：3

**适用场景**：
- 学科辅导
- 概念解释
- 问题解答
- 学习指导

## 5. 最佳实践

1. **选择合适的模板**：根据您的使用场景选择最合适的代理模板。
2. **添加相关工具**：为代理添加与其任务相关的工具，以增强其能力。
3. **自定义指令**：根据您的具体需求调整代理的指令。
4. **使用持久化存储**：对于长期运行的应用，使用持久化的状态和会话管理。
5. **调整参数**：根据需要调整代理的参数，如温度和最大步骤数。

## 6. 示例：创建客服系统

以下是一个完整的示例，展示如何创建一个带有持久化存储的客服系统：

```kotlin
import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.core.agent.SQLiteSessionManager
import ai.kastrax.core.agent.SQLiteStateManager
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建持久化的状态和会话管理器
    val stateManager = SQLiteStateManager("customer_service_state.db")
    val sessionManager = SQLiteSessionManager("customer_service_session.db")

    // 创建客服代理
    val customerServiceAgent = AgentTemplates.createCustomerServiceAgent(
        name = "客服助手",
        model = openAi(
            apiKey = System.getenv("OPENAI_API_KEY"),
            model = "gpt-4o"
        )
    )

    // 设置状态和会话管理
    customerServiceAgent.stateManager = stateManager
    customerServiceAgent.sessionManager = sessionManager

    // 创建会话
    val session = customerServiceAgent.createSession(
        title = "客户咨询",
        resourceId = "customer-123",
        metadata = mapOf("category" to "product-inquiry")
    )

    println("会话已创建: ${session?.id}")

    // 模拟客户询问
    val response = customerServiceAgent.generate(
        "你们的产品有哪些功能？",
        options = AgentGenerateOptions(
            threadId = session?.id
        )
    )

    println("\n客服响应:")
    println(response.text)

    // 模拟后续对话
    val followUpResponse = customerServiceAgent.generate(
        "这个产品适合初学者使用吗？",
        options = AgentGenerateOptions(
            threadId = session?.id
        )
    )

    println("\n客服响应:")
    println(followUpResponse.text)

    // 获取会话消息
    val messages = session?.id?.let { customerServiceAgent.getSessionMessages(it) }
    println("\n会话消息数量: ${messages?.size}")
}
```
