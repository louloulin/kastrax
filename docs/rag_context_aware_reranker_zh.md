# KastraX RAG 系统上下文感知重排序

## 1. 概述

上下文感知重排序是 RAG（检索增强生成）系统中的一项高级功能，它能够根据查询的上下文信息来优化检索结果的排序。与传统的重排序方法相比，上下文感知重排序考虑了更广泛的信息，如会话历史、用户偏好或当前任务的背景，从而提供更加相关和个性化的检索结果。

KastraX RAG 系统现已实现了上下文感知重排序功能，支持多种配置选项，以满足不同的应用场景和需求。

## 2. 上下文感知重排序器

KastraX RAG 系统使用 `ContextAwareReranker` 类来实现上下文感知重排序。它提供了多种配置选项，可以根据需要进行自定义。

```kotlin
// 创建上下文感知重排序器
val reranker = ContextAwareReranker(
    embeddingService = embeddingService,
    config = ContextAwareRerankerConfig(
        contextWeight = 0.6,      // 上下文的权重
        queryWeight = 0.4,        // 查询的权重
        originalScoreWeight = 0.3, // 原始分数的权重
        maxContextLength = 1000    // 最大上下文长度
    )
)
```

### 2.1 配置选项

`ContextAwareRerankerConfig` 提供了以下配置选项：

- `contextWeight`：上下文的权重，默认为 0.6
- `queryWeight`：查询的权重，默认为 0.4
- `originalScoreWeight`：原始分数的权重，默认为 0.3
- `maxContextLength`：最大上下文长度，默认为 1000

通过调整这些权重，你可以控制上下文、查询和原始分数对最终排序的影响程度。

## 3. 在 RAG 系统中使用上下文感知重排序

KastraX RAG 系统提供了多种方法来使用上下文感知重排序，你可以根据需要选择合适的方法。

### 3.1 创建 RAG 实例时指定重排序器

```kotlin
// 创建上下文感知重排序器
val reranker = ContextAwareReranker(
    embeddingService = embeddingService,
    config = ContextAwareRerankerConfig(
        contextWeight = 0.6,
        queryWeight = 0.4,
        originalScoreWeight = 0.3
    )
)

// 创建 RAG 实例
val rag = RAG(
    vectorStore = vectorStore,
    embeddingService = embeddingService,
    reranker = reranker,
    defaultOptions = RagProcessOptions(
        useContextAwareReranking = true
    )
)
```

### 3.2 使用选项启用上下文感知重排序

```kotlin
// 创建自定义选项，包括上下文感知重排序
val options = RagProcessOptions(
    useContextAwareReranking = true,
    contextAwareRerankingOptions = ContextAwareRerankerConfig(
        contextWeight = 0.7,
        queryWeight = 0.3,
        originalScoreWeight = 0.2
    )
)

// 使用选项进行搜索
val results = rag.search("查询文本", 5, 0.0, options)
```

## 4. 上下文信息的来源

在实际应用中，上下文信息可以来自多种来源，如：

- **会话历史**：用户与系统的历史交互记录
- **用户偏好**：用户的兴趣、习惯或设置
- **当前任务**：用户正在执行的任务或目标
- **环境信息**：时间、位置或设备类型等

KastraX RAG 系统提供了灵活的接口，允许你自定义上下文信息的获取方式。默认情况下，系统使用一个空字符串作为上下文，你可以通过扩展 `RAG` 类的 `getContextForReranking` 方法来提供自定义的上下文信息。

```kotlin
class CustomRAG(
    vectorStore: RagVectorStore,
    embeddingService: EmbeddingService,
    reranker: Reranker,
    defaultOptions: RagProcessOptions
) : RAG(vectorStore, embeddingService, reranker, defaultOptions) {
    
    override fun getContextForReranking(query: String, options: RagProcessOptions): String {
        // 从会话历史、用户偏好或其他来源获取上下文信息
        return "自定义上下文信息"
    }
}
```

## 5. 工作原理

上下文感知重排序器的工作原理如下：

1. 计算查询和上下文的嵌入向量
2. 计算每个文档与查询和上下文的相似度
3. 根据配置的权重，计算每个文档的组合分数
4. 按组合分数降序排序文档

组合分数的计算公式为：

```
combinedScore = (queryDocSimilarity * queryWeight + contextDocSimilarity * contextWeight + originalScore * originalScoreWeight) / (queryWeight + contextWeight + originalScoreWeight)
```

其中：
- `queryDocSimilarity` 是文档与查询的相似度
- `contextDocSimilarity` 是文档与上下文的相似度
- `originalScore` 是文档的原始分数

## 6. 最佳实践

### 6.1 调整权重

根据你的应用场景，调整 `contextWeight`、`queryWeight` 和 `originalScoreWeight` 的值：

- 如果你希望结果更加个性化，增加 `contextWeight`
- 如果你希望结果更加准确，增加 `queryWeight`
- 如果你希望保留原始排序的影响，增加 `originalScoreWeight`

### 6.2 提供有意义的上下文

确保提供的上下文信息与当前查询相关，并且包含有用的信息。过长或无关的上下文可能会降低重排序的效果。

### 6.3 结合其他重排序策略

考虑将上下文感知重排序与其他重排序策略结合使用，如关键词匹配重排序或多样性重排序，以获得更好的结果。

```kotlin
// 创建组合重排序器
val reranker = CompositeReranker(
    ContextAwareReranker(embeddingService),
    KeywordMatchReranker(),
    DiversityReranker(embeddingService)
)
```

## 7. 示例

### 7.1 基本用法

```kotlin
// 创建向量存储和嵌入服务
val vectorStore = InMemoryVectorStore()
val embeddingService = RandomEmbeddingService()

// 创建上下文感知重排序器
val reranker = ContextAwareReranker(
    embeddingService = embeddingService,
    config = ContextAwareRerankerConfig(
        contextWeight = 0.6,
        queryWeight = 0.4,
        originalScoreWeight = 0.3
    )
)

// 创建 RAG 实例
val rag = RAG(
    vectorStore = vectorStore,
    embeddingService = embeddingService,
    reranker = reranker,
    defaultOptions = RagProcessOptions(
        useContextAwareReranking = true
    )
)

// 使用上下文感知重排序进行搜索
val query = "人工智能和机器学习"
val results = rag.search(query, 5, 0.0)
```

### 7.2 自定义上下文

```kotlin
class ConversationalRAG(
    vectorStore: RagVectorStore,
    embeddingService: EmbeddingService,
    reranker: Reranker,
    private val conversationHistory: List<String>
) : RAG(vectorStore, embeddingService, reranker) {
    
    override fun getContextForReranking(query: String, options: RagProcessOptions): String {
        // 使用会话历史作为上下文
        return conversationHistory.joinToString("\n")
    }
}

// 创建会话历史
val conversationHistory = listOf(
    "用户: 什么是深度学习？",
    "系统: 深度学习是机器学习的一种方法，它使用神经网络来模拟人类大脑的学习过程。",
    "用户: 它与传统机器学习有什么区别？"
)

// 创建会话式 RAG
val rag = ConversationalRAG(
    vectorStore = vectorStore,
    embeddingService = embeddingService,
    reranker = ContextAwareReranker(embeddingService),
    conversationHistory = conversationHistory
)

// 使用上下文感知重排序进行搜索
val query = "深度学习的应用"
val results = rag.search(query, 5, 0.0)
```

完整的示例代码可以在 `kastrax-rag/src/main/kotlin/ai/kastrax/rag/examples/ContextAwareRerankerExample.kt` 文件中找到。
