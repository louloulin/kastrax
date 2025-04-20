# KastraX: 现代 AI 代理框架

KastraX 是一个用 Kotlin 构建的现代 AI 代理框架，受到 Kastra 和 Mastra 的启发。它提供了一套全面的工具和抽象，用于构建 AI 驱动的应用程序，注重类型安全、模块化和开发者体验。

> 注意：我们对 Mastra 框架进行了详细分析，并基于分析结果设计了 KastraX。详细分析请参见 [Mastra 分析与 KastraX 设计](mastra_analysis_zh.md)。

## 1. 核心设计原则

1. **Kotlin 优先**: 利用 Kotlin 的语言特性（协程、DSL、扩展函数）提供流畅的开发体验
2. **类型安全**: 在整个框架中提供强类型支持，在编译时捕获错误
3. **模块化**: 设计模块化架构，明确关注点分离
4. **可扩展性**: 使框架易于扩展和定制，适应特定用例
5. **性能**: 优化性能和资源效率
6. **开发者体验**: 提供直观的 API 和全面的文档

## 2. 项目结构

KastraX 遵循模块化架构，包含以下组件：

```
kastrax/
├── kastrax-core/               # 核心框架组件 ✅
├── kastrax-memory-api/         # 内存系统接口 ✅
├── kastrax-memory-impl/        # 内存系统实现 ✅
├── kastrax-rag/                # 检索增强生成
├── kastrax-cli/                # 命令行工具
├── kastrax-evals/              # 评估框架
├── kastrax-deployer/           # 部署工具
├── kastrax-voice/              # 语音功能
├── kastrax-integrations/       # 第三方集成
│   ├── kastrax-openai/         # OpenAI 集成 ✅
│   ├── kastrax-anthropic/      # Anthropic 集成
│   ├── kastrax-gemini/         # Google Gemini 集成
│   └── kastrax-mistral/        # Mistral 集成
└── examples/                   # 示例应用 ✅
```

## 3. 核心组件

### 3.1 代理系统 (Agent System) ✅

代理系统是 KastraX 的核心组件，提供了创建 AI 代理的灵活而强大的方式。

```kotlin
// 使用 DSL 创建代理
val myAgent = agent {
    name = "助手"
    instructions = "你是一个有帮助的助手。"
    model = openAi("gpt-4o")

    // 添加工具
    tools {
        tool("calculator") {
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
    }
}

// 使用代理
val response = myAgent.generate("法国的首都是什么？")
println(response.text)

// 流式响应
myAgent.stream("讲个故事") { chunk ->
    print(chunk)
}
```

### 3.2 LLM 抽象层 ✅

KastraX 提供了统一的接口，用于与不同的 LLM 提供商交互。

```kotlin
// LLM 提供商接口
interface LlmProvider {
    suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): LlmResponse

    suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): Flow<String>

    suspend fun embedText(text: String): List<Float>
}

// 创建提供商的工厂方法
val openAiProvider = openAi(
    model = "gpt-4o",
    // API 密钥从环境变量 OPENAI_API_KEY 获取
)
```

### 3.3 工具系统 (Tool System) ✅

工具系统允许代理与外部系统交互并执行特定任务。

```kotlin
// 使用 DSL 创建工具
val weatherTool = tool {
    id = "weather"
    name = "天气信息"
    description = "获取位置的当前天气信息"

    // 使用类型安全的模式构建器定义输入
    input {
        obj {
            field("location", string()) {
                description = "城市和国家"
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
            field("conditions", string()) {
                description = "天气状况"
            }
            field("humidity", integer()) {
                description = "湿度百分比"
                minimum = 0
                maximum = 100
            }
        }
    }

    // 实现执行逻辑
    execute { input ->
        val location = input.getString("location")
        val units = input.getString("units", "celsius")

        // 获取天气数据的实现
        val weatherData = getWeatherData(location, units)

        // 返回类型安全的输出
        output {
            "temperature" to weatherData.temperature
            "conditions" to weatherData.conditions
            "humidity" to weatherData.humidity
        }
    }
}
```

### 3.4 内存系统 (Memory System) ✅

内存系统提供对话历史的持久存储和检索。

```kotlin
// 内存接口
interface Memory {
    suspend fun saveMessage(message: LlmMessage, threadId: String)
    suspend fun getMessages(threadId: String, limit: Int = 10): List<LlmMessage>
    suspend fun searchMessages(query: String, threadId: String): List<LlmMessage>
    suspend fun createThread(title: String? = null): String
    suspend fun deleteThread(threadId: String)
}

// 使用 DSL 创建内存
val memory = memory {
    storage = inMemoryStorage()
    lastMessages = 10
    semanticRecall = true
}
```

### 3.5 工作流系统 (Workflow System) ✅

工作流系统支持创建复杂的多步骤流程。

```kotlin
// 使用 DSL 创建工作流
val myWorkflow = workflow {
    name = "content-creation"
    description = "生成和审核内容"

    // 定义输入模式
    inputSchema {
        property("topic", String::class) {
            description = "创建内容的主题"
            required = true
        }
        property("tone", String::class) {
            description = "内容的语调"
            defaultValue = "informative"
        }
    }

    // 定义输出模式
    outputSchema {
        property("content", String::class)
        property("qualityScore", Float::class)
    }

    // 定义步骤
    step("generate-content") {
        execute { context ->
            val topic = context.input.getString("topic")
            val tone = context.input.getString("tone")

            val contentAgent = getAgent("content-creator")
            val response = contentAgent.generate(
                "创建关于 $topic 的内容，语调为 $tone。"
            )

            jsonOutput {
                "content" to response.text
            }
        }
    }

    step("review-content") {
        after("generate-content")
        execute { context ->
            val content = context.getStepOutput("generate-content")
                .getString("content")

            val reviewAgent = getAgent("content-reviewer")
            val response = reviewAgent.generate(
                "审核这个内容并提供 0 到 1 的质量分数：$content"
            )

            jsonOutput {
                "review" to response.text
                "qualityScore" to extractScore(response.text)
            }
        }
    }

    // 映射输出
    output {
        "content" from { context ->
            context.getStepOutput("generate-content").getString("content")
        }
        "qualityScore" from { context ->
            context.getStepOutput("review-content").getFloat("qualityScore")
        }
    }
}

// 运行工作流
val result = myWorkflow.execute {
    "topic" to "人工智能"
    "tone" to "educational"
}
```

## 4. 构建系统

KastraX 使用带有 Kotlin DSL 的 Gradle 进行构建管理。

```kotlin
// 根 build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.20" apply false
    kotlin("plugin.serialization") version "1.9.20" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
}

allprojects {
    group = "ai.kastrax"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

## 5. 实现计划

### 5.1 第一阶段：核心框架（优先级：高）

1. **kastrax-core** ✅
   - 实现基础接口和抽象 ✅
   - 创建带有 DSL 的 Agent 系统 ✅
   - 开发 Tool 系统 ✅
   - 构建 LLM 抽象层 ✅
   - 实现 Workflow 引擎 ✅

2. **kastrax-memory** ✅
   - 设计内存接口 ✅
   - 实现内存存储 ✅
   - 创建持久层 ✅
   - 添加语义搜索功能 ✅

3. **kastrax-integrations**
   - 实现 OpenAI 集成 ✅
   - 实现 Anthropic 集成 ⏳
   - 实现 Google Gemini 集成 ⏳
   - 实现 Mistral 集成 ⏳

### 5.2 第二阶段：高级功能（优先级：中）

1. **kastrax-rag**
   - 实现文档处理 ✅
   - 创建嵌入服务 ✅
   - 构建向量存储集成 ✅
   - 开发重排序策略 ✅

2. **kastrax-evals**
   - 设计评估框架
   - 实现常见评估器
   - 创建评估报告

3. **kastrax-cli**
   - 开发项目脚手架
   - 创建交互式游乐场
   - 实现部署命令

### 5.3 第三阶段：专业组件（优先级：低）

1. **kastrax-deployer**
   - 实现无服务器部署
   - 创建容器部署
   - 构建 API 网关

2. **kastrax-voice**
   - 实现文本到语音
   - 创建语音到文本
   - 构建语音代理接口

## 6. 与 Kastra 和 Mastra 的比较

### 6.1 相对于 Kastra 的改进

1. **增强的 DSL**: 更直观和全面的 DSL，用于定义代理、工具和工作流
2. **更好的类型安全**: 整个框架中更强的类型检查和推断
3. **改进的内存系统**: 更灵活和强大的内存系统，具有更好的语义搜索
4. **高级 RAG**: 全面的 RAG 系统，包括文档处理、嵌入和重排序
5. **工作流引擎**: 更强大的工作流引擎，具有更好的错误处理和可视化

### 6.2 相对于 Mastra 的改进

1. **Kotlin 优势**: 利用 Kotlin 的语言特性，实现更好的并发和类型安全
2. **简化的 API**: 更一致和直观的 API 设计
3. **更好的集成**: 组件之间更紧密的集成
4. **性能**: 通过 Kotlin 的高效协程实现更好的性能
5. **JVM 生态系统**: 访问丰富的 JVM 库和工具生态系统

## 7. 待办事项列表（按优先级）

### 7.1 高优先级

1. 设置项目结构和构建系统 ✅
2. 实现核心接口和抽象 ✅
3. 创建 LLM 提供商抽象和实现 ✅
4. 开发带有 DSL 的 Agent 系统 ✅
5. 实现 Tool 系统 ✅
6. 创建基本的 Memory 系统 ✅
7. 开发简单的 Workflow 引擎 ✅
8. 为核心组件编写全面的测试 ✅
9. 为核心功能创建文档 ✅

### 7.2 中优先级

1. 使用语义搜索增强 Memory 系统 ✅
2. 实现 RAG 系统 ✅
3. 开发评估框架
4. 创建项目管理的 CLI 工具
5. 实现更多 LLM 提供商集成 ⏳
6. 添加高级工作流功能
7. 创建示例应用程序 ✅
8. 改进错误处理和遥测

### 7.3 低优先级

1. 实现部署工具
2. 开发语音功能
3. 为工作流创建可视化工具
4. 添加更多专业工具和集成
5. 实现高级 RAG 功能 ✅
6. 创建性能优化工具
7. 开发监控和分析
8. 构建社区和贡献指南

## 8. 结论

KastraX 将 Kastra 和 Mastra 的最佳方面结合到一个现代、强大且对开发者友好的 AI 代理框架中。通过利用 Kotlin 的语言特性和 JVM 生态系统，KastraX 为构建复杂的 AI 应用程序提供了坚实的基础，注重类型安全、模块化和性能。

模块化架构允许开发者只使用他们需要的组件，而全面的 DSL 使创建和配置代理、工具和工作流变得容易。通过在整个框架中使用强类型，开发者可以在编译时捕获错误并获得更好的 IDE 支持。

KastraX 设计为可扩展的，允许开发者为特定用例定制和扩展框架。全面的文档和示例使开始使用和有效学习如何使用框架变得容易。
