# 查询转换和增强机制

## 1. 概述

查询转换和增强机制是 KastraX RAG 系统的一个重要组成部分，它通过转换和增强用户的原始查询，提高检索的准确性和多样性。本文档详细介绍了该机制的设计理念、核心组件和使用方法。

## 2. 设计理念

查询转换和增强机制基于以下设计理念：

1. **查询理解**：用户的原始查询可能不够精确或不够完整，通过查询转换可以更好地理解用户的意图。

2. **多样性检索**：通过生成多个查询变体，可以从不同角度检索相关文档，提高结果的多样性。

3. **可扩展性**：设计了灵活的接口和组件，可以轻松添加新的查询转换策略。

4. **可配置性**：提供了丰富的配置选项，可以根据不同的应用场景调整查询转换和增强的行为。

## 3. 核心组件

### 3.1 查询转换器（QueryTransformer）

查询转换器是查询转换和增强机制的核心组件，它定义了转换查询的接口：

```kotlin
interface QueryTransformer {
    suspend fun transform(query: String): String
    suspend fun transformToMultiple(query: String): List<String> = listOf(transform(query))
}
```

我们实现了以下几种查询转换器：

#### 3.1.1 规范化查询转换器（NormalizationQueryTransformer）

规范化查询转换器对查询进行基本的清理和规范化，包括：

- 移除多余的空格
- 移除常见的标点符号
- 转换为小写

示例：

```
输入: "What is artificial intelligence?"
输出: "what is artificial intelligence"
```

#### 3.1.2 同义词查询转换器（SynonymQueryTransformer）

同义词查询转换器通过添加同义词或相关术语来扩展查询：

- 在单一转换模式下，将同义词添加为 OR 表达式
- 在多查询模式下，生成包含不同同义词组合的多个查询变体

示例：

```
输入: "ai and nlp"
单一转换输出: "ai (artificial intelligence OR machine learning) and nlp (natural language processing)"
多查询变体:
  - "ai and nlp"
  - "artificial intelligence and nlp"
  - "machine learning and nlp"
  - "ai and natural language processing"
  - "artificial intelligence and natural language processing"
  - "machine learning and natural language processing"
```

#### 3.1.3 查询分解转换器（DecompositionQueryTransformer）

查询分解转换器将复杂查询分解为多个简单查询：

- 按句子分割复杂查询
- 如果只有一个句子，尝试按逗号或分号分割

示例：

```
输入: "What is artificial intelligence? How does it work? What are its applications?"
输出:
  - "What is artificial intelligence"
  - "How does it work"
  - "What are its applications"
```

#### 3.1.4 LLM 查询转换器（LLMQueryTransformer）

LLM 查询转换器使用大型语言模型重写查询以提高检索效果：

- 可以生成更清晰、更具体的查询
- 可以生成多个查询变体，从不同角度表达相同的信息需求

示例：

```
输入: "tell me about ai"
单一转换输出: "What is artificial intelligence, its history, applications, and current developments?"
多查询变体:
  - "What is artificial intelligence and how does it work?"
  - "What are the main applications and use cases of AI in today's world?"
  - "How has artificial intelligence evolved over time and what are its future prospects?"
```

#### 3.1.5 组合查询转换器（CompositeQueryTransformer）

组合查询转换器按顺序应用多个转换器，可以组合不同转换器的优势：

```kotlin
val transformers = listOf(
    NormalizationQueryTransformer(),
    SynonymQueryTransformer(synonymMap),
    DecompositionQueryTransformer()
)

val transformer = CompositeQueryTransformer(transformers)
```

### 3.2 查询增强检索器（QueryEnhancedRetriever）

查询增强检索器使用查询转换器来增强检索效果：

```kotlin
class QueryEnhancedRetriever(
    private val baseRetriever: Retriever,
    private val queryTransformer: QueryTransformer = NoOpQueryTransformer(),
    private val config: QueryEnhancedRetrieverConfig = QueryEnhancedRetrieverConfig()
) : Retriever
```

它支持两种工作模式：

1. **单一查询模式**：使用转换后的单一查询进行检索
2. **多查询模式**：使用多个查询变体进行检索，然后合并结果

#### 3.2.1 结果合并策略

在多查询模式下，查询增强检索器支持三种结果合并策略：

1. **交错合并（INTERLEAVE）**：从每个查询结果中依次选择一个文档，确保每个查询的结果都有代表。

2. **按分数合并（BY_SCORE）**：选择分数最高的文档，不考虑它们来自哪个查询。

3. **多样性合并（DIVERSITY）**：首先从每个查询中选择最佳结果，然后按分数填充剩余位置，确保结果的多样性。

## 4. 使用方法

### 4.1 基本用法

要使用查询转换和增强机制，只需在创建 RAG 实例时启用查询增强选项：

```kotlin
val rag = RAG(vectorStore, embeddingService)

val results = rag.search(
    "什么是AI",
    limit = 5,
    options = RagProcessOptions(
        useQueryEnhancement = true
    )
)
```

### 4.2 高级配置

可以通过 `QueryEnhancedRetrieverConfig` 配置查询增强检索器的行为：

```kotlin
val results = rag.search(
    "什么是AI",
    limit = 5,
    options = RagProcessOptions(
        useQueryEnhancement = true,
        queryEnhancementOptions = QueryEnhancedRetrieverConfig(
            useMultiQuery = true,
            mergeStrategy = MergeStrategy.DIVERSITY,
            maxQueriesPerRequest = 3
        )
    )
)
```

### 4.3 自定义查询转换器

可以实现自定义的查询转换器，并将其集成到 RAG 系统中：

```kotlin
class MyCustomQueryTransformer : QueryTransformer {
    override suspend fun transform(query: String): String {
        // 自定义转换逻辑
        return "transformed $query"
    }
}

// 在 RAG 类中使用自定义转换器
val queryTransformer = MyCustomQueryTransformer()
val retriever = QueryEnhancedRetriever(baseRetriever, queryTransformer)
```

## 5. 性能考虑

查询转换和增强机制可能会增加检索的延迟，特别是在多查询模式下。以下是一些性能优化建议：

1. **限制查询变体数量**：通过 `maxQueriesPerRequest` 参数限制每个请求的最大查询数。

2. **选择合适的转换器**：不同的转换器有不同的计算复杂度，选择适合您应用场景的转换器。

3. **缓存转换结果**：对于频繁出现的查询，可以缓存其转换结果。

4. **异步处理**：利用 Kotlin 协程的并发能力，并行处理多个查询变体的检索。

## 6. 未来工作

查询转换和增强机制还有以下几个方向可以进一步改进：

1. **查询意图识别**：根据查询的意图选择不同的转换策略。

2. **个性化查询转换**：根据用户的历史查询和偏好调整查询转换。

3. **自适应合并策略**：根据查询和检索结果的特性自动选择最佳的合并策略。

4. **更多预训练模型支持**：集成更多的预训练模型用于查询转换和增强。

## 7. 结论

查询转换和增强机制是提高 RAG 系统检索效果的有效手段。通过转换和增强用户的原始查询，可以更好地理解用户意图，提高检索的准确性和多样性。KastraX RAG 系统提供了灵活的接口和丰富的配置选项，可以根据不同的应用场景定制查询转换和增强的行为。
