# KastraX 架构概述

## 整体架构

KastraX 是一个基于 Kotlin 的现代 AI 代理框架，专为构建强类型、模块化和高性能的 AI 应用而设计。其架构基于以下核心原则：

### 分层架构

KastraX 采用清晰的分层架构，从低级基础设施到高级应用功能：

```
高级应用层
    │
    ▼
代理系统层 (Agent System)
    │
    ▼
LLM 抽象层 (LLM Abstraction)
    │
    ▼
工具与集成层 (Tools & Integrations)
    │
    ▼
核心基础设施层 (Core Infrastructure)
```

### 模块化设计

KastraX 使用模块化设计，每个模块专注于特定功能，具有清晰的边界和接口：

- **核心模块** (kastrax-core): 基础框架组件和通用抽象
- **内存系统** (kastrax-memory-api/impl): 管理代理的记忆和上下文
- **集成模块** (kastrax-integrations): 与各种LLM提供商的集成
- **RAG系统** (kastrax-rag): 检索增强生成功能
- **代理通信** (kastrax-a2a/a2x): 代理间通信和外部系统集成
- **向量存储** (kastrax-store): 多种向量数据库的集成
- **AI到数据库** (kastrax-ai2db): 自然语言到SQL查询转换

## 核心系统详解

### 代理系统 (Agent System)

KastraX 代理系统提供了创建和管理 AI 代理的框架。代理是由 LLM 驱动的实体，能够:

- 处理用户输入
- 执行工具调用
- 维护会话状态
- 生成智能响应

代理使用Kotlin DSL创建，提供强类型接口和流畅的API：

```kotlin
val agent = agent {
    name = "研究助手"
    instructions = "你是一个专业的研究助手，帮助用户查找和分析学术信息。"
    model = openAi("gpt-4o")

    tools {
        // 工具定义
    }

    memory {
        // 记忆配置
    }
}
```

### LLM 抽象层 (LLM Abstraction)

LLM 抽象层提供了统一的接口，用于与不同的 LLM 提供商交互：

- OpenAI (GPT-3.5, GPT-4)
- Anthropic (Claude)
- DeepSeek
- Google (Gemini)
- 其他提供商

这种抽象使得在不同模型间切换变得容易，同时保持一致的API：

```kotlin
// OpenAI模型配置
val openAiModel = openAi {
    model = "gpt-4o"
    temperature = 0.7
    maxTokens = 2000
}

// Anthropic模型配置
val anthropicModel = anthropic {
    model = "claude-3-opus-20240229"
    temperature = 0.7
    maxTokens = 2000
}
```

### 工具系统 (Tool System)

工具系统允许代理与外部系统和服务交互。工具使用 Zod 模式定义输入和输出，确保类型安全：

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"
    
    input {
        field("expression", Zod.string()) {
            description = "要计算的数学表达式，如 '2 + 2'"
        }
    }
    
    output {
        field("result", Zod.number()) {
            description = "计算结果"
        }
    }
    
    execute { input ->
        // 执行计算逻辑
        val expression = input.get<String>("expression")
        val result = evaluateExpression(expression)
        mapOf("result" to result)
    }
}
```

### 记忆系统 (Memory System)

记忆系统负责存储和检索代理的对话历史和上下文信息，支持多种记忆类型：

- **短期记忆**: 当前会话上下文
- **长期记忆**: 向量数据库支持的持久化记忆
- **工作记忆**: 特定任务的临时记忆

```kotlin
val agentWithMemory = agent {
    // 基本配置
    
    memory {
        shortTerm {
            maxItems = 10
        }
        
        longTerm {
            vectorStore = chromaStore("memory-db")
        }
    }
}
```

### RAG系统 (Retrieval-Augmented Generation)

RAG系统增强了代理的知识，通过从外部文档检索相关信息来提供更准确的回答：

- 文档处理和分块
- 向量嵌入生成
- 相似度检索
- 上下文增强

```kotlin
val ragAgent = agent {
    // 基本配置
    
    rag {
        documentSource = fileSystem("docs/")
        embedder = openAiEmbedder("text-embedding-3-large")
        vectorStore = pinecone("my-index")
        chunkSize = 500
        chunkOverlap = 50
    }
}
```

### 代理间通信 (A2A/A2X)

代理间通信系统允许多个代理协同工作，通过定义明确的通信协议和工作流：

- **A2A**: 代理到代理通信
- **A2X**: 代理到外部系统通信

```kotlin
val workflow = agentWorkflow {
    val researcher = researchAgent()
    val writer = writerAgent()
    val editor = editorAgent()
    
    step("research", researcher)
    step("draft", writer)
    step("edit", editor)
    
    flow {
        "research" to "draft"
        "draft" to "edit"
    }
}
```

### 向量存储 (Vector Storage)

向量存储系统支持多种向量数据库，用于存储和检索嵌入向量：

- 内存存储 (开发和测试)
- Chroma
- Pinecone
- Qdrant
- PostgreSQL (pgvector)
- LanceDB

```kotlin
val chromaDb = chromaStore {
    collectionName = "documents"
    persistDirectory = "data/chroma"
}

val pineconeDb = pineconeStore {
    apiKey = System.getenv("PINECONE_API_KEY")
    environment = "us-west4-gcp"
    index = "my-index"
}
```

### AI到数据库系统 (AI2DB)

AI2DB系统将自然语言转换为SQL查询，允许用户通过对话与数据库交互：

- 模式理解
- 查询生成
- 查询验证和优化
- 结果处理和展示

```kotlin
val dbAgent = ai2dbAgent {
    databaseConnector {
        type = PostgreSQL
        url = "jdbc:postgresql://localhost:5432/mydb"
        username = "user"
        password = "pass"
    }
    
    schemaAnalyzer {
        cacheEnabled = true
    }
}

val result = dbAgent.query("查找去年销售额超过100万的客户")
```

## 技术栈

KastraX 构建在现代Kotlin和JVM技术之上：

- **Kotlin 2.1+**: 利用最新语言特性
- **协程**: 异步和非阻塞编程
- **Ktor**: 网络和HTTP客户端
- **kotlinx.serialization**: 结构化数据序列化
- **Kotlin DSL**: 声明式API设计
- **kactor**: 基于Actor模型的并发

## 扩展性设计

KastraX 设计为高度可扩展的框架：

1. **插件系统**: 允许添加新功能而无需修改核心代码
2. **抽象接口**: 所有组件都基于接口设计，便于替换实现
3. **自定义集成点**: 明确定义的集成点，用于添加新的模型或工具
4. **事件系统**: 通过事件驱动架构实现松耦合扩展

## 部署选项

KastraX 支持多种部署场景：

- **JVM应用**: 作为标准JVM应用运行
- **服务器部署**: 使用kastrax-server模块作为微服务
- **容器化**: Docker容器化部署
- **FaaS**: 作为无服务器函数部署
- **GraalVM原生镜像**: 用于低延迟和资源效率场景

## 性能考虑

KastraX 通过以下策略优化性能：

- **延迟加载**: 按需加载资源
- **连接池**: 管理外部系统连接
- **缓存**: 多级缓存减少冗余操作
- **资源监控**: 内置资源使用监控
- **流式处理**: 使用流式API减少延迟

## 安全最佳实践

KastraX 遵循安全最佳实践：

- **输入验证**: 严格验证所有输入
- **API密钥管理**: 安全存储和管理API密钥
- **最小权限原则**: 代理仅获取所需的最小权限
- **沙箱执行**: 工具在安全上下文中执行
- **数据隐私**: 遵循数据处理最佳实践
