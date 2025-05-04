# Kastrax DSL 设计

## DSL 概述

Kastrax 框架采用领域特定语言（Domain-Specific Language，DSL）设计，提供了一种声明式、流畅的 API，使开发者能够以简洁、直观的方式创建和配置 AI Agent 应用。本文档介绍 Kastrax 的 DSL 设计原则和主要 DSL 组件。

## DSL 设计原则

Kastrax 的 DSL 设计遵循以下原则：

1. **声明式**：关注"做什么"而不是"怎么做"
2. **类型安全**：利用 Kotlin 的类型系统提供编译时检查
3. **流畅接口**：支持链式调用和函数式编程风格
4. **一致性**：在不同组件中保持一致的设计风格
5. **可扩展**：易于扩展和自定义

## 主要 DSL 组件

### 1. Agent DSL

Agent DSL 用于创建和配置 AI Agent，提供了一种简洁的方式来定义 Agent 的行为和特性。

```kotlin
val myAgent = agent {
    name("MyAssistant")
    description("一个有帮助的助手")
    
    // 配置 LLM
    model = deepSeek {
        apiKey("your-deepseek-api-key")
        model(DeepSeekModel.DEEPSEEK_CHAT)
        temperature(0.7)
    }
    
    // 添加工具
    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }
    
    // 配置记忆
    memory = memory {
        lastMessages(10)
        semanticRecall(true)
        embeddingGenerator(openAIEmbedding)
    }
}
```

Agent DSL 的主要组件包括：

- **基本属性**：名称、描述、指令等
- **模型配置**：LLM 提供者、模型参数等
- **工具配置**：添加和配置工具
- **记忆配置**：配置记忆系统
- **生成选项**：配置生成行为

### 2. 工具 DSL

工具 DSL 用于创建和配置工具，支持类型安全的参数定义和执行逻辑。

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"
    
    // 定义输入模式
    inputSchema = jsonObject {
        "operation" to JsonPrimitive("加法、减法、乘法或除法")
        "a" to JsonPrimitive(0.0)
        "b" to JsonPrimitive(0.0)
    }
    
    // 定义输出模式
    outputSchema = jsonObject {
        "result" to JsonPrimitive(0.0)
    }
    
    // 执行逻辑
    execute = { input ->
        val operation = input.jsonObject["operation"]?.jsonPrimitive?.content ?: "add"
        val a = input.jsonObject["a"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        val b = input.jsonObject["b"]?.jsonPrimitive?.doubleOrNull ?: 0.0
        
        val result = when (operation) {
            "add" -> a + b
            "subtract" -> a - b
            "multiply" -> a * b
            "divide" -> a / b
            else -> throw IllegalArgumentException("Unknown operation: $operation")
        }
        
        jsonObject {
            "result" to JsonPrimitive(result)
        }
    }
}
```

Kastrax 还提供了基于 Zod 的类型安全工具 DSL：

```kotlin
val calculatorTool = zodTool<CalculatorInput, CalculatorOutput> {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"
    
    // 使用 Zod 风格的模式定义
    inputSchema = objectInput("计算器输入") {
        stringField("operation", "操作类型") {
            enum("add", "subtract", "multiply", "divide")
        }
        numberField("a", "第一个操作数")
        numberField("b", "第二个操作数")
    }
    
    outputSchema = objectOutput("计算器输出") {
        numberField("result", "计算结果")
    }
    
    // 类型安全的执行逻辑
    execute = { input ->
        val result = when (input.operation) {
            "add" -> input.a + input.b
            "subtract" -> input.a - input.b
            "multiply" -> input.a * input.b
            "divide" -> input.a / input.b
            else -> throw IllegalArgumentException("Unknown operation: ${input.operation}")
        }
        
        CalculatorOutput(result)
    }
}
```

### 3. 记忆 DSL

记忆 DSL 用于配置 Agent 的记忆系统，包括记忆类型、存储方式和处理器。

```kotlin
val agentMemory = memory {
    // 配置最近消息数量
    lastMessages(10)
    
    // 启用语义召回
    semanticRecall(true)
    
    // 配置嵌入生成器
    embeddingGenerator(openAIEmbedding)
    
    // 配置向量存储
    vectorStorage(inMemoryVectorStorage)
    
    // 添加记忆处理器
    processor(summaryProcessor)
    
    // 配置工作记忆
    workingMemory {
        capacity(5)
        decayRate(0.1)
    }
    
    // 配置优先级
    priorityConfig {
        decayFactor(0.9)
        cleanupThreshold(0.2)
    }
}
```

### 4. RAG DSL

RAG DSL 用于配置检索增强生成系统，包括文档处理、向量存储和检索策略。

```kotlin
val ragSystem = rag {
    // 配置向量存储
    vectorStore(inMemoryVectorStore)
    
    // 配置嵌入服务
    embeddingService(openAIEmbedding)
    
    // 配置重排序器
    reranker(relevanceReranker)
    
    // 配置检索选项
    options {
        // 使用混合检索
        useHybridSearch(true)
        
        // 配置混合检索选项
        hybridOptions {
            keywordWeight(0.3)
            expandLimit(1.5)
        }
        
        // 使用上下文感知重排序
        useContextAwareReranking(true)
        
        // 配置上下文构建
        contextOptions {
            maxTokens(3000)
            template("以下是与查询相关的文档：\n\n{{documents}}\n\n根据以上文档回答问题：{{query}}")
        }
    }
}
```

### 5. 工作流 DSL

工作流 DSL 用于定义和配置工作流，包括步骤定义、变量映射和执行选项。

```kotlin
val myWorkflow = workflow {
    name("数据处理工作流")
    description("处理和分析数据的工作流")
    
    // 定义步骤
    steps {
        // 数据加载步骤
        step("loadData") {
            name = "加载数据"
            description = "从源加载数据"
            
            // 定义输入变量
            variables = mapOf(
                "source" to VariableReference("$.input.dataSource")
            )
            
            // 执行逻辑
            execute = { context ->
                val source = context.resolveReference(variables["source"]!!) as String
                val data = loadDataFromSource(source)
                
                WorkflowStepResult(
                    success = true,
                    output = mapOf("data" to data)
                )
            }
        }
        
        // 数据处理步骤
        step("processData") {
            name = "处理数据"
            description = "处理和转换数据"
            
            // 定义前置步骤
            after = listOf("loadData")
            
            // 定义输入变量
            variables = mapOf(
                "data" to VariableReference("$.steps.loadData.output.data")
            )
            
            // 执行逻辑
            execute = { context ->
                val data = context.resolveReference(variables["data"]!!) as List<Any>
                val processedData = processData(data)
                
                WorkflowStepResult(
                    success = true,
                    output = mapOf("processedData" to processedData)
                )
            }
        }
        
        // 数据分析步骤
        step("analyzeData") {
            name = "分析数据"
            description = "分析处理后的数据"
            
            // 定义前置步骤
            after = listOf("processData")
            
            // 定义输入变量
            variables = mapOf(
                "processedData" to VariableReference("$.steps.processData.output.processedData")
            )
            
            // 执行逻辑
            execute = { context ->
                val processedData = context.resolveReference(variables["processedData"]!!) as List<Any>
                val analysis = analyzeData(processedData)
                
                WorkflowStepResult(
                    success = true,
                    output = mapOf("analysis" to analysis)
                )
            }
        }
    }
    
    // 定义输出映射
    outputMappings = mapOf(
        "result" to OutputMapping("$.steps.analyzeData.output.analysis")
    )
}
```

### 6. Actor DSL

Actor DSL 用于创建和配置 Actor 化的 Agent，结合了 Agent DSL 和 Actor 配置。

```kotlin
val agentPid = system.actorAgent {
    // Agent 配置部分
    agent {
        name = "助手"
        instructions = "你是一个有帮助的助手。"
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY"))
        }
        tools {
            tool(calculatorTool)
        }
    }

    // Actor 配置部分
    actor {
        // 监督策略
        oneForOneStrategy {
            maxRetries = 3
            withinTimeRange = 1.minutes
        }
        // 邮箱类型
        unboundedMailbox()
    }
}
```

Actor DSL 还支持创建 Agent 网络：

```kotlin
val network = system.agentNetwork {
    // 添加 Agent
    agent("assistant") {
        agent {
            name = "助手"
            instructions = "你是一个有帮助的助手。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY"))
            }
        }
    }
    
    agent("researcher") {
        agent {
            name = "研究员"
            instructions = "你是一个专业的研究员。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY"))
            }
        }
    }
    
    // 配置协调者
    coordinator {
        agent {
            name = "协调者"
            instructions = "你是一个团队协调者。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY"))
            }
        }
    }
}
```

## DSL 实现机制

Kastrax 的 DSL 实现主要基于 Kotlin 的以下特性：

1. **函数字面量与接收者**：使用 `apply`、`with` 等高阶函数创建 DSL 上下文
2. **扩展函数**：扩展现有类型，提供额外功能
3. **中缀表示法**：使 DSL 更加自然和易读
4. **Lambda 表达式**：简洁地定义行为
5. **构建器模式**：使用构建器创建复杂对象

以下是一个简化的 DSL 实现示例：

```kotlin
// Agent DSL 实现
class AgentBuilder {
    var name: String = ""
    var instructions: String = ""
    lateinit var model: LlmProvider
    var tools: MutableMap<String, Tool> = mutableMapOf()
    var memory: Memory? = null
    
    // 设置名称
    fun name(value: String) = apply { name = value }
    
    // 设置指令
    fun instructions(value: String) = apply { instructions = value }
    
    // 添加工具
    fun tools(block: ToolsBuilder.() -> Unit) {
        val builder = ToolsBuilder()
        builder.block()
        tools.putAll(builder.tools)
    }
    
    // 构建 Agent
    fun build(): Agent {
        return LLMAgent(
            name = name,
            instructions = instructions,
            model = model,
            tools = tools,
            memory = memory
        )
    }
}

// 工具集合构建器
class ToolsBuilder {
    val tools = mutableMapOf<String, Tool>()
    
    // 添加工具
    fun tool(tool: Tool) {
        tools[tool.id] = tool
    }
}

// DSL 入口函数
fun agent(block: AgentBuilder.() -> Unit): Agent {
    val builder = AgentBuilder()
    builder.block()
    return builder.build()
}
```

## 总结

Kastrax 的 DSL 设计提供了一种声明式、流畅的方式来创建和配置 AI Agent 应用。通过 Agent DSL、工具 DSL、记忆 DSL、RAG DSL、工作流 DSL 和 Actor DSL，开发者可以以简洁、直观的方式定义复杂的 AI 应用行为。这种设计不仅提高了代码的可读性和可维护性，还利用了 Kotlin 的类型安全特性，减少了运行时错误。
