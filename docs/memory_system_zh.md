# KastraX 内存系统详解

KastraX 内存系统提供了强大而灵活的对话历史管理功能，支持多种存储后端、工作内存、语义搜索和内存压缩等高级特性。本文档详细介绍了内存系统的架构、组件和使用方法。

## 1. 内存系统架构

KastraX 内存系统由以下核心组件组成：

### 1.1 核心接口

- **Memory**：基本内存接口，定义了存储和检索消息的方法
- **WorkingMemory**：工作内存接口，用于管理对话上下文中的重要信息
- **MemoryCompressor**：内存压缩器接口，用于压缩和摘要长对话
- **MemoryProcessor**：内存处理器接口，用于过滤和转换消息

### 1.2 存储后端

KastraX 支持多种存储后端，包括：

- **InMemoryStorage**：基于内存的存储，适用于开发和测试
- **RedisStorage**：基于Redis的存储，支持分布式部署和高可用性
- **PostgresStorage**：基于PostgreSQL的存储，支持关系型数据库存储和查询

### 1.3 高级功能

- **工作内存**：跟踪和管理对话上下文中的重要信息
- **内存压缩**：自动压缩长对话，生成摘要
- **语义搜索**：基于语义相似度搜索相关消息
- **过期和清理**：自动清理过期的内存记录

## 2. 使用内存系统

### 2.1 基本用法

```kotlin
// 创建基本内存实例
val memory = MemoryFactory.createMemory {
    storage(MemoryFactory.createInMemoryStorage())
    lastMessages(10)
}

// 创建线程
val threadId = memory.createThread("示例对话")

// 保存消息
memory.saveMessage(
    SimpleMessage(
        role = MessageRole.USER,
        content = "你好，我叫张三。"
    ),
    threadId
)

// 获取消息
val messages = memory.getMessages(threadId, limit = 10)
```

### 2.2 使用语义搜索

语义搜索允许Agent基于语义相似度搜索相关消息，而不仅仅是关键词匹配。

```kotlin
// 创建带语义搜索的内存实例
val memory = enhancedMemory {
    lastMessages(10)
    semanticRecall(true)
    embeddingGenerator(OpenAIEmbeddingGenerator(apiKey))
    vectorStorage(InMemoryVectorStorage())
}

// 执行语义搜索
val results = memory.semanticSearch(
    query = "机器学习和深度学习的区别",
    threadId = threadId,
    config = SemanticRecallConfig(topK = 5, minScore = 0.7f)
)

// 获取语义回忆消息
val recallMessages = memory.getSemanticRecallMessages(
    query = "机器学习和深度学习的区别",
    threadId = threadId,
    config = SemanticRecallConfig(topK = 5, messageRange = 1)
)
```

### 2.3 使用混合搜索

混合搜索结合了语义搜索和关键词匹配，提供更准确的搜索结果。

```kotlin
// 创建带混合搜索的内存实例
val memory = enhancedMemory {
    lastMessages(10)
    semanticRecall(true)
    embeddingGenerator(OpenAIEmbeddingGenerator(apiKey))
    vectorStorage(InMemoryVectorStorage())
    hybridSearch(true)
    searchWeights(0.3f, 0.7f)  // 关键词权重和语义权重
}
```

### 2.4 使用相关性重排序

相关性重排序可以提高搜索结果的质量，确保结果的多样性和上下文相关性。

```kotlin
// 创建带重排序的内存实例
val memory = enhancedMemory {
    lastMessages(10)
    semanticRecall(true)
    embeddingGenerator(OpenAIEmbeddingGenerator(apiKey))
    vectorStorage(InMemoryVectorStorage())
    hybridSearch(true)
    standardReranker()  // 使用标准重排序器
}

// 或者自定义重排序器
val memory = enhancedMemory {
    // ...
    reranker(
        SemanticSearchFactory.createCompositeReranker(
            listOf(
                SemanticSearchFactory.createDiversityReranker(0.3f),
                SemanticSearchFactory.createContextAwareReranker(0.2f)
            )
        )
    )
}
```

### 2.5 使用工作内存

工作内存允许Agent跟踪和管理对话上下文中的重要信息，如用户信息、对话主题等。

```kotlin
// 创建带工作内存的内存实例
val memory = enhancedMemory {
    lastMessages(10)
    workingMemory(
        WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TEXT_STREAM,
            template = """
                # 用户信息
                - 姓名: 未知
                - 位置: 未知
                - 偏好: 未知

                # 对话上下文
                - 主题: 人工智能
                - 目标: 学习
            """.trimIndent()
        )
    )
}

// 创建使用工作内存的Agent
val agent = agent {
    name = "WorkingMemoryAgent"
    instructions = """
        你是一个具有工作内存能力的助手。你可以记住用户的信息和对话上下文。

        请注意工作内存中的信息，并在回答时参考这些信息。如果你了解到用户的新信息，
        请更新工作内存中的相应部分。
    """.trimIndent()
    model = openAi("gpt-4o")
    memory = memory
}
```

KastraX 支持两种工作内存模式：

1. **文本流模式（TEXT_STREAM）**：工作内存作为系统消息的一部分发送给模型
2. **工具调用模式（TOOL_CALL）**：提供一个工具让模型更新工作内存

### 2.6 使用内存压缩

内存压缩功能可以自动压缩长对话，生成摘要，减少上下文窗口的使用。

```kotlin
// 创建LLM内存压缩器
val memoryCompressor = LlmMemoryCompressor(llm)

// 创建带内存压缩的内存实例
val memory = enhancedMemory {
    lastMessages(10)
    memoryCompressor(memoryCompressor)
    compressionConfig(
        MemoryCompressionConfig(
            enabled = true,
            threshold = 50,  // 当消息数量达到50条时触发压缩
            targetSize = 10,  // 压缩后保留10条消息
            preserveSystemMessages = true,
            preserveRecentMessages = 5
        )
    )
}
```

### 2.7 使用Redis存储后端

```kotlin
// 创建Redis连接池
val jedisPool = JedisPool(JedisPoolConfig(), "localhost", 6379)

// 创建使用Redis存储的内存实例
val memory = enhancedMemory {
    storage(jedisPool)
    lastMessages(100)
    workingMemory(
        WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TEXT_STREAM
        )
    )
}
```

### 2.8 使用PostgreSQL存储后端

```kotlin
// 创建数据源
val dataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/kastrax"
    username = "postgres"
    password = "password"
    maximumPoolSize = 10
}

// 创建使用PostgreSQL存储的内存实例
val memory = enhancedMemory {
    storage(dataSource)
    lastMessages(100)
    workingMemory(
        WorkingMemoryConfig(
            enabled = true,
            mode = WorkingMemoryMode.TOOL_CALL
        )
    )
}
```

## 3. 高级配置

### 3.1 工作内存配置

```kotlin
val workingMemoryConfig = WorkingMemoryConfig(
    enabled = true,                        // 启用工作内存
    mode = WorkingMemoryMode.TEXT_STREAM,  // 使用文本流模式
    template = """
        # 用户信息
        - 姓名: 未知
        - 位置: 未知
        - 偏好: 未知

        # 对话上下文
        - 主题: 未知
        - 目标: 未知
    """.trimIndent()
)
```

### 3.2 内存压缩配置

```kotlin
val compressionConfig = MemoryCompressionConfig(
    enabled = true,              // 启用内存压缩
    threshold = 100,             // 触发压缩的消息数阈值
    targetSize = 20,             // 压缩后的目标消息数
    preserveSystemMessages = true,  // 保留系统消息
    preserveRecentMessages = 10     // 保留最近的消息数量
)
```

### 3.3 内存处理器

KastraX 提供了多种内存处理器，用于过滤和转换消息：

```kotlin
// 创建带处理器的内存实例
val memory = enhancedMemory {
    lastMessages(100)

    // 添加令牌限制器，限制总令牌数为4000
    processor(TokenLimiter(4000))

    // 添加工具调用过滤器，过滤掉工具调用消息
    processor(ToolCallFilter())
}
```

## 4. 最佳实践

### 4.1 生产环境配置

对于生产环境，我们建议：

1. 使用Redis或PostgreSQL存储后端，而不是内存存储
2. 启用内存压缩，设置合理的阈值和目标大小
3. 使用令牌限制器，避免超出模型的上下文窗口限制
4. 配置适当的过期和清理策略，定期清理过期的内存记录

### 4.2 性能优化

1. 对于高并发场景，使用Redis集群或PostgreSQL的连接池
2. 设置合理的缓存策略，减少数据库访问
3. 使用异步操作，避免阻塞主线程
4. 定期监控内存使用情况，及时清理不需要的数据

### 4.3 安全考虑

1. 使用线程ID和资源ID验证，确保数据安全
2. 实现适当的访问控制，限制敏感数据的访问
3. 加密存储敏感信息，如用户个人信息
4. 定期备份数据，防止数据丢失

## 5. 示例应用

KastraX 提供了多个示例应用，展示如何使用内存系统：

1. **基本内存示例**：展示基本的内存操作
2. **工作内存示例**：展示如何使用工作内存功能
3. **内存压缩示例**：展示如何使用内存压缩功能
4. **Redis存储示例**：展示如何使用Redis存储后端
5. **PostgreSQL存储示例**：展示如何使用PostgreSQL存储后端

这些示例可以在`examples/src/main/kotlin/ai/kastrax/examples/memory`目录中找到。

## 6. 总结

KastraX 内存系统提供了强大而灵活的对话历史管理功能，支持多种存储后端、工作内存、语义搜索和内存压缩等高级特性。通过合理配置和使用这些功能，可以构建出具有持久记忆和上下文理解能力的智能Agent。
