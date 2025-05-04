# Kastrax 配置系统

## 配置系统概述

Kastrax 配置系统提供了一种灵活、可扩展的方式来配置框架的各个组件。通过配置系统，开发者可以自定义 Agent 行为、LLM 参数、工具设置、记忆系统等，而无需修改代码。本文档介绍 Kastrax 配置系统的设计原则、配置方式和最佳实践。

## 配置方式

Kastrax 支持多种配置方式，包括：

1. **代码配置**：通过 DSL 和构建器在代码中直接配置
2. **配置文件**：使用 JSON、YAML 或 HOCON 格式的配置文件
3. **环境变量**：通过环境变量设置配置项
4. **系统属性**：通过 JVM 系统属性设置配置项
5. **运行时配置**：在运行时动态修改配置

### 代码配置

代码配置是最直接的配置方式，通过 DSL 和构建器在代码中设置配置项：

```kotlin
val myAgent = agent {
    name("MyAssistant")
    instructions("你是一个有帮助的助手。")
    
    model = deepSeek {
        apiKey("your-deepseek-api-key")
        model(DeepSeekModel.DEEPSEEK_CHAT)
        temperature(0.7)
        maxTokens(2000)
    }
    
    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }
    
    memory = memory {
        lastMessages(10)
        semanticRecall(true)
    }
    
    defaultGenerateOptions = AgentGenerateOptions(
        maxSteps = 5,
        temperature = 0.7,
        maxTokens = 1000
    )
}
```

### 配置文件

Kastrax 支持使用配置文件来设置配置项，支持 JSON、YAML 和 HOCON 格式：

**JSON 格式**：

```json
{
  "agent": {
    "name": "MyAssistant",
    "instructions": "你是一个有帮助的助手。",
    "model": {
      "provider": "deepseek",
      "apiKey": "${DEEPSEEK_API_KEY}",
      "modelName": "deepseek-chat",
      "temperature": 0.7,
      "maxTokens": 2000
    },
    "tools": ["calculator", "weather"],
    "memory": {
      "lastMessages": 10,
      "semanticRecall": true
    },
    "defaultGenerateOptions": {
      "maxSteps": 5,
      "temperature": 0.7,
      "maxTokens": 1000
    }
  }
}
```

**YAML 格式**：

```yaml
agent:
  name: MyAssistant
  instructions: 你是一个有帮助的助手。
  model:
    provider: deepseek
    apiKey: ${DEEPSEEK_API_KEY}
    modelName: deepseek-chat
    temperature: 0.7
    maxTokens: 2000
  tools:
    - calculator
    - weather
  memory:
    lastMessages: 10
    semanticRecall: true
  defaultGenerateOptions:
    maxSteps: 5
    temperature: 0.7
    maxTokens: 1000
```

**HOCON 格式**：

```hocon
agent {
  name = "MyAssistant"
  instructions = "你是一个有帮助的助手。"
  model {
    provider = "deepseek"
    apiKey = ${DEEPSEEK_API_KEY}
    modelName = "deepseek-chat"
    temperature = 0.7
    maxTokens = 2000
  }
  tools = ["calculator", "weather"]
  memory {
    lastMessages = 10
    semanticRecall = true
  }
  defaultGenerateOptions {
    maxSteps = 5
    temperature = 0.7
    maxTokens = 1000
  }
}
```

加载配置文件：

```kotlin
// 加载 JSON 配置
val config = ConfigLoader.loadJson("config.json")

// 加载 YAML 配置
val config = ConfigLoader.loadYaml("config.yaml")

// 加载 HOCON 配置
val config = ConfigLoader.loadHocon("config.conf")

// 使用配置创建 Agent
val myAgent = AgentFactory.createFromConfig(config.getConfig("agent"))
```

### 环境变量

Kastrax 支持通过环境变量设置配置项，特别适合敏感信息如 API 密钥：

```kotlin
val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "default-key"

val myAgent = agent {
    model = deepSeek {
        apiKey(apiKey)
        // 其他配置...
    }
}
```

配置文件中也可以引用环境变量：

```json
{
  "agent": {
    "model": {
      "apiKey": "${DEEPSEEK_API_KEY}"
    }
  }
}
```

### 系统属性

Kastrax 支持通过 JVM 系统属性设置配置项：

```kotlin
val debugMode = System.getProperty("kastrax.debug", "false").toBoolean()

val myAgent = agent {
    // 根据系统属性设置调试模式
    if (debugMode) {
        // 启用调试功能
    }
}
```

### 运行时配置

Kastrax 支持在运行时动态修改某些配置项：

```kotlin
// 创建 Agent
val myAgent = agent {
    // 初始配置...
}

// 运行时修改生成选项
val response = myAgent.generate(
    prompt = "你好",
    options = AgentGenerateOptions(
        temperature = 0.9,
        maxTokens = 500
    )
)
```

## 配置优先级

当多种配置方式同时存在时，Kastrax 按照以下优先级处理：

1. 代码中的显式配置（最高优先级）
2. 运行时传入的配置
3. 系统属性
4. 环境变量
5. 配置文件
6. 默认值（最低优先级）

## 配置类别

Kastrax 的配置项可以分为以下几类：

### 1. 全局配置

全局配置影响整个 Kastrax 框架的行为：

```kotlin
val kastrax = KastraXBuilder()
    .logLevel(LogLevel.INFO)
    .defaultLlmProvider(deepSeekProvider)
    .build()
```

### 2. Agent 配置

Agent 配置定义了 Agent 的行为和特性：

```kotlin
val myAgent = agent {
    name("MyAssistant")
    instructions("你是一个有帮助的助手。")
    model = deepSeek { /* ... */ }
    tools { /* ... */ }
    memory = memory { /* ... */ }
    defaultGenerateOptions = AgentGenerateOptions(/* ... */)
}
```

### 3. LLM 配置

LLM 配置定义了语言模型的参数和行为：

```kotlin
val deepSeekProvider = deepSeek {
    apiKey("your-deepseek-api-key")
    model(DeepSeekModel.DEEPSEEK_CHAT)
    temperature(0.7)
    maxTokens(2000)
    topP(0.95)
    presencePenalty(0.0)
    frequencyPenalty(0.0)
    stopSequences(listOf("User:", "Assistant:"))
}
```

### 4. 记忆配置

记忆配置定义了 Agent 的记忆系统：

```kotlin
val agentMemory = memory {
    lastMessages(10)
    semanticRecall(true)
    embeddingGenerator(openAIEmbedding)
    vectorStorage(inMemoryVectorStorage)
    processor(summaryProcessor)
    workingMemory {
        capacity(5)
        decayRate(0.1)
    }
}
```

### 5. 工具配置

工具配置定义了 Agent 可用的工具：

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"
    // 其他配置...
}
```

### 6. RAG 配置

RAG 配置定义了检索增强生成系统的行为：

```kotlin
val ragSystem = rag {
    vectorStore(inMemoryVectorStore)
    embeddingService(openAIEmbedding)
    reranker(relevanceReranker)
    options {
        useHybridSearch(true)
        hybridOptions {
            keywordWeight(0.3)
            expandLimit(1.5)
        }
        useContextAwareReranking(true)
        contextOptions {
            maxTokens(3000)
            template("以下是与查询相关的文档：\n\n{{documents}}\n\n根据以上文档回答问题：{{query}}")
        }
    }
}
```

### 7. 工作流配置

工作流配置定义了工作流的步骤和行为：

```kotlin
val myWorkflow = workflow {
    name("数据处理工作流")
    description("处理和分析数据的工作流")
    steps {
        // 步骤配置...
    }
    outputMappings = mapOf(/* ... */)
}
```

### 8. Actor 配置

Actor 配置定义了 Actor 系统的行为：

```kotlin
val agentPid = system.actorAgent {
    agent {
        // Agent 配置...
    }
    actor {
        oneForOneStrategy {
            maxRetries = 3
            withinTimeRange = 1.minutes
        }
        unboundedMailbox()
    }
}
```

## 配置验证

Kastrax 提供了配置验证机制，确保配置项的正确性：

```kotlin
// 验证配置
val validationResult = ConfigValidator.validate(config)
if (!validationResult.isValid) {
    // 处理验证错误
    validationResult.errors.forEach { error ->
        println("配置错误: ${error.path} - ${error.message}")
    }
}
```

## 配置模板

Kastrax 提供了常见场景的配置模板，可以作为起点快速配置：

```kotlin
// 使用对话助手模板
val assistantAgent = AgentTemplates.createAssistant(
    name = "MyAssistant",
    apiKey = "your-deepseek-api-key"
)

// 使用 RAG 模板
val ragAgent = AgentTemplates.createRagAgent(
    name = "DocumentAssistant",
    apiKey = "your-deepseek-api-key",
    documentPath = "docs/"
)
```

## 最佳实践

### 1. 分离敏感信息

将 API 密钥等敏感信息与代码分离，使用环境变量或安全的配置管理系统：

```kotlin
val apiKey = System.getenv("DEEPSEEK_API_KEY")
    ?: throw IllegalStateException("DEEPSEEK_API_KEY environment variable is required")
```

### 2. 使用配置文件管理复杂配置

对于复杂的配置，使用配置文件而不是硬编码：

```kotlin
val config = ConfigLoader.loadYaml("agent-config.yaml")
val myAgent = AgentFactory.createFromConfig(config)
```

### 3. 提供合理的默认值

为配置项提供合理的默认值，减少用户配置负担：

```kotlin
val temperature = config.getDouble("temperature", 0.7)
val maxTokens = config.getInt("maxTokens", 2000)
```

### 4. 使用环境特定配置

为不同环境（开发、测试、生产）提供不同的配置：

```kotlin
val environment = System.getenv("KASTRAX_ENV") ?: "development"
val configFile = "config.$environment.yaml"
val config = ConfigLoader.loadYaml(configFile)
```

### 5. 记录配置变更

记录配置变更，便于调试和审计：

```kotlin
logger.info("Agent configuration: name=${config.name}, model=${config.model}")
```

## 总结

Kastrax 配置系统提供了多种配置方式，包括代码配置、配置文件、环境变量、系统属性和运行时配置。通过这些方式，开发者可以灵活地配置 Kastrax 框架的各个组件，满足不同场景的需求。遵循最佳实践，可以使配置更加安全、可维护和可扩展。
