# KastraX RAG 系统重排序功能

## 1. 概述

重排序（Reranking）是 RAG（检索增强生成）系统中的一个关键组件，它可以提高检索结果的质量。在基于向量相似度的初步检索之后，重排序器可以使用更复杂的策略对结果进行二次排序，以提高最终结果的相关性和质量。

KastraX RAG 系统现已实现了灵活的重排序功能，支持多种重排序策略，并可以根据需要进行组合。

## 2. 重排序器接口

所有重排序器都实现了 `Reranker` 接口：

```kotlin
interface Reranker {
    /**
     * 对搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult>
}
```

## 3. 内置重排序器

KastraX RAG 系统提供了以下内置重排序器：

### 3.1 IdentityReranker（恒等重排序器）

最简单的重排序器，保持原始排序不变。当你不需要重排序但需要保持接口一致性时，可以使用此重排序器。

```kotlin
val reranker = IdentityReranker()
```

### 3.2 KeywordMatchReranker（关键词匹配重排序器）

基于查询关键词在文档中的出现次数进行重排序。文档中包含更多查询关键词的结果会被排在前面。

```kotlin
val reranker = KeywordMatchReranker(
    keywordWeight = 0.7,     // 关键词匹配的权重
    originalScoreWeight = 0.3 // 原始分数的权重
)
```

参数说明：
- `keywordWeight`：关键词匹配的权重，默认为 0.5
- `originalScoreWeight`：原始分数的权重，默认为 0.5

### 3.3 MetadataReranker（元数据重排序器）

基于文档元数据进行重排序。例如，可以根据文档的日期、相关性分数或其他自定义元数据字段进行排序。

```kotlin
val reranker = MetadataReranker(
    metadataKey = "date",        // 用于排序的元数据键
    ascending = false,           // 是否按升序排序
    metadataWeight = 0.8,        // 元数据的权重
    originalScoreWeight = 0.2     // 原始分数的权重
)
```

参数说明：
- `metadataKey`：用于重排序的元数据键
- `ascending`：是否按升序排序，默认为 false（降序）
- `metadataWeight`：元数据的权重，默认为 0.5
- `originalScoreWeight`：原始分数的权重，默认为 0.5

### 3.4 CompositeReranker（组合重排序器）

按顺序应用多个重排序器。这允许你创建复杂的重排序策略，例如先按关键词匹配，然后按日期排序。

```kotlin
val reranker = CompositeReranker(
    KeywordMatchReranker(keywordWeight = 0.7, originalScoreWeight = 0.3),
    MetadataReranker(metadataKey = "date", ascending = false)
)
```

## 4. 在 RAG 系统中使用重排序

在 RAG 系统中使用重排序非常简单，只需在创建 RAG 实例时指定重排序器：

```kotlin
// 创建向量存储和嵌入服务
val vectorStore = InMemoryVectorStore()
val embeddingService = RandomEmbeddingService()

// 创建关键词匹配重排序器
val reranker = KeywordMatchReranker(
    keywordWeight = 0.7,
    originalScoreWeight = 0.3
)

// 创建带有重排序器的 RAG
val rag = RAG(vectorStore, embeddingService, reranker)

// 使用 RAG 进行搜索
val results = rag.search("查询文本")

// 生成上下文
val context = rag.generateContext("查询文本")
```

你也可以在搜索或生成上下文时选择是否应用重排序：

```kotlin
// 不应用重排序进行搜索
val resultsWithoutReranking = rag.search("查询文本", applyReranking = false)

// 不应用重排序生成上下文
val contextWithoutReranking = rag.generateContext("查询文本", applyReranking = false)
```

## 5. 自定义重排序器

你可以通过实现 `Reranker` 接口来创建自定义重排序器：

```kotlin
class CustomReranker : Reranker {
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        // 实现自定义重排序逻辑
        return results.sortedBy { /* 自定义排序逻辑 */ }
    }
}
```

## 6. 最佳实践

1. **组合重排序器**：对于复杂的应用场景，考虑使用 `CompositeReranker` 组合多个重排序策略。

2. **平衡权重**：在使用 `KeywordMatchReranker` 或 `MetadataReranker` 时，调整权重以平衡原始相似度分数和其他因素。

3. **性能考虑**：重排序会增加额外的计算开销。对于大量结果，考虑先限制初始检索的结果数量，然后再应用重排序。

4. **测试不同策略**：不同的应用场景可能需要不同的重排序策略。测试多种策略并评估结果质量。

## 7. 示例

### 7.1 基于关键词和日期的组合重排序

```kotlin
// 创建组合重排序器：先按关键词匹配，再按日期排序
val reranker = CompositeReranker(
    KeywordMatchReranker(keywordWeight = 0.7, originalScoreWeight = 0.3),
    MetadataReranker(metadataKey = "date", ascending = false)
)

// 创建 RAG
val rag = RAG(vectorStore, embeddingService, reranker)

// 搜索
val results = rag.search("人工智能和机器学习")
```

### 7.2 基于元数据的重排序

```kotlin
// 创建基于相关性的重排序器
val reranker = MetadataReranker(
    metadataKey = "relevance",
    ascending = false,
    metadataWeight = 0.9,
    originalScoreWeight = 0.1
)

// 创建 RAG
val rag = RAG(vectorStore, embeddingService, reranker)

// 生成上下文
val context = rag.generateContext("查询文本")
```

## 8. 总结

KastraX RAG 系统的重排序功能提供了灵活且强大的方式来提高检索结果的质量。通过使用不同的重排序策略，你可以根据应用场景的需求优化检索结果，提供更相关、更有用的信息给用户。
