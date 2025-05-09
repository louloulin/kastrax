# KastraX RAG API 文档

本文档提供了 KastraX RAG 模块的 API 参考。

## 核心类

### RAG

`RAG` 是检索增强生成系统的主要类，它提供了从向量存储中检索相关文档并生成增强的上下文的功能。

```kotlin
class RAG(
    val documentStore: DocumentStore,
    val embeddingService: EmbeddingService,
    val reranker: Reranker = IdentityReranker(),
    val defaultOptions: RagProcessOptions = RagProcessOptions()
)
```

#### 主要方法

- `loadDocuments(documents: List<Document>)`: 加载文档到向量存储中
- `search(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): List<DocumentSearchResult>`: 搜索相关文档
- `generateContext(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): String`: 生成上下文
- `retrieveContext(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): RetrieveContextResult`: 检索上下文和相关文档

### RagProcessOptions

`RagProcessOptions` 用于配置 RAG 系统的检索和重排序过程。

```kotlin
data class RagProcessOptions(
    val useHybridSearch: Boolean = false,
    val useSemanticRetrieval: Boolean = false,
    val useReranking: Boolean = true,
    val useQueryEnhancement: Boolean = false,
    val hybridOptions: HybridOptions = HybridOptions(),
    val semanticOptions: SemanticOptions = SemanticOptions(),
    val rerankingOptions: RerankingOptions = RerankingOptions(),
    val queryEnhancementOptions: QueryEnhancementOptions = QueryEnhancementOptions(),
    val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)
```

## 文档处理

### DocumentLoader

`DocumentLoader` 是一个接口，用于加载文档。

```kotlin
interface DocumentLoader {
    suspend fun load(): List<Document>
}
```

#### 实现类

- `FileDocumentLoader`: 从文件加载文档
- `DirectoryDocumentLoader`: 从目录加载文档
- `PdfDocumentLoader`: 从 PDF 文件加载文档
- `TextDocumentLoader`: 从文本文件加载文档
- `JsonDocumentLoader`: 从 JSON 文件加载文档
- `CsvDocumentLoader`: 从 CSV 文件加载文档

### DocumentSplitter

`DocumentSplitter` 是一个接口，用于将文档分割成更小的片段。

```kotlin
interface DocumentSplitter {
    fun split(document: Document): List<Document>
}
```

#### 实现类

- `TextSplitter`: 基于文本的分割器
- `RecursiveCharacterTextSplitter`: 递归字符文本分割器
- `SentenceSplitter`: 基于句子的分割器
- `ParagraphSplitter`: 基于段落的分割器

### DocumentCleaner

`DocumentCleaner` 是一个接口，用于清理文档内容。

```kotlin
interface DocumentCleaner {
    fun clean(text: String): String
}
```

#### 实现类

- `BasicDocumentCleaner`: 基本文档清理器
- `HtmlDocumentCleaner`: HTML 文档清理器
- `MarkdownDocumentCleaner`: Markdown 文档清理器

## 检索

### Retriever

`Retriever` 是一个接口，用于从向量存储中检索相关文档。

```kotlin
interface Retriever {
    suspend fun retrieve(query: String, limit: Int = 5, minScore: Double = 0.0): List<DocumentSearchResult>
}
```

#### 实现类

- `VectorRetriever`: 基于向量的检索器
- `KeywordRetriever`: 基于关键词的检索器
- `HybridRetriever`: 混合检索器，结合向量和关键词检索
- `EnhancedHybridRetriever`: 增强的混合检索器，支持更多的检索策略

### Reranker

`Reranker` 是一个接口，用于重新排序检索结果。

```kotlin
interface Reranker {
    suspend fun rerank(query: String, results: List<DocumentSearchResult>): List<DocumentSearchResult>
}
```

#### 实现类

- `IdentityReranker`: 不进行重排序
- `RelevanceReranker`: 基于相关性的重排序器
- `DiversityReranker`: 基于多样性的重排序器
- `CrossEncoderReranker`: 基于交叉编码器的重排序器

## 嵌入

### EmbeddingService

`EmbeddingService` 是一个接口，用于生成文本的嵌入向量。

```kotlin
interface EmbeddingService {
    suspend fun embed(text: String): FloatArray
    suspend fun embedBatch(texts: List<String>): List<FloatArray>
    fun dimension(): Int
}
```

#### 实现类

- `RandomEmbeddingService`: 生成随机嵌入向量，用于测试
- `OpenAIEmbeddingService`: 使用 OpenAI API 生成嵌入向量
- `DeepseekEmbeddingService`: 使用 Deepseek API 生成嵌入向量
- `FastEmbedKotlinEmbeddingService`: 使用 fastembed-kotlin 库在本地生成嵌入向量
- `CachedEmbeddingService`: 缓存嵌入向量，提高性能

## 上下文构建

### ContextBuilder

`ContextBuilder` 用于构建上下文。

```kotlin
class ContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig()
)
```

#### 主要方法

- `buildContext(query: String, results: List<DocumentSearchResult>): String`: 构建上下文

### ContextBuilderConfig

`ContextBuilderConfig` 用于配置上下文构建过程。

```kotlin
data class ContextBuilderConfig(
    val maxTokens: Int = 4000,
    val format: ContextFormat = ContextFormat.TEXT,
    val includeMetadata: Boolean = false,
    val metadataFields: List<String> = emptyList(),
    val separator: String = "\n\n"
)
```

## 图 RAG

### GraphRAG

`GraphRAG` 是基于图的检索增强生成系统。

```kotlin
class GraphRAG(
    private val config: GraphRAGConfig = GraphRAGConfig()
)
```

#### 主要方法

- `addNode(node: GraphNode)`: 添加节点
- `addEdge(edge: GraphEdge)`: 添加边
- `createGraph(documents: List<Document>, embeddings: List<FloatArray>)`: 创建图
- `query(query: FloatArray, options: GraphRAGQueryOptions = GraphRAGQueryOptions()): List<RankedNode>`: 查询图
- `toSearchResults(rankedNodes: List<RankedNode>): List<SearchResult>`: 将排序节点转换为搜索结果

### GraphRAGTool

`GraphRAGTool` 是 GraphRAG 的工具类，提供了更高级的功能。

```kotlin
class GraphRAGTool(
    private val documentLoader: DocumentLoader,
    private val embeddingService: EmbeddingService,
    private val graphRAG: GraphRAG = GraphRAG(
        GraphRAGConfig(
            dimension = embeddingService.dimension(),
            threshold = 0.7,
            bidirectional = true
        )
    )
)
```

#### 主要方法

- `execute(query: String, topK: Int = 10, randomWalkSteps: Int = 100, restartProb: Double = 0.15, includeMetadata: Boolean = true): JsonPrimitive`: 执行 GraphRAG 查询

## 评估和优化

### RagEvaluationTool

`RagEvaluationTool` 用于评估 RAG 系统的性能。

```kotlin
class RagEvaluationTool(
    private val rag: RAG,
    private val llmClient: LlmClient
)
```

#### 主要方法

- `evaluateRetrievalPrecision(query: String, relevantDocIds: List<String>, limit: Int = 5): Double`: 评估检索精度
- `evaluateContextRelevance(query: String, limit: Int = 5): Double`: 评估上下文相关性
- `evaluateAnswerQuality(query: String, expectedAnswer: String, limit: Int = 5): Double`: 评估答案质量
- `evaluateHallucination(query: String, limit: Int = 5): Double`: 评估幻觉
- `runFullEvaluation(query: String, expectedAnswer: String, relevantDocIds: List<String>, limit: Int = 5): RagEvaluationResult`: 运行完整评估

### RagOptimizationTool

`RagOptimizationTool` 用于优化 RAG 系统的配置。

```kotlin
class RagOptimizationTool(
    private val rag: RAG,
    private val evaluationTool: RagEvaluationTool
)
```

#### 主要方法

- `optimize(queries: List<String>, generateAnswer: suspend (String, String) -> String, groundTruths: List<String>? = null, optimizationOptions: RagOptimizationOptions = RagOptimizationOptions()): RagOptimizationResult`: 优化 RAG 系统的配置

### RagBenchmarkTool

`RagBenchmarkTool` 用于测试 RAG 系统的性能。

```kotlin
class RagBenchmarkTool(
    private val rag: RAG
)
```

#### 主要方法

- `runBenchmark(queries: List<String>, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null, parallel: Boolean = false): RagBenchmarkSummary`: 运行基准测试
- `compareConfigurations(queries: List<String>, configurations: List<RagProcessOptions>, limit: Int = 5, minScore: Double = 0.0): Map<RagProcessOptions, RagBenchmarkSummary>`: 比较不同配置的性能
