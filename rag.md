# KastraX RAG 模块重构计划

## 背景

KastraX RAG 模块需要重构，以适配新的 vector store 架构。当前的 RAG 模块与 vector store 模块存在循环依赖问题，需要重新设计架构，使 RAG 模块依赖于 vector store 模块，而不是反过来。

## 目标

1. 重构 KastraX RAG 模块，使其基于新的 vector store 架构
2. 消除 RAG 模块与 vector store 模块之间的循环依赖
3. 保留现有 RAG 模块的功能，包括文档加载、文档分割、检索、重排序等
4. 提供更好的扩展性和可维护性

## 架构设计

### 模块依赖关系

```
kastrax-core
    |
    v
kastrax-store
    |
    v
kastrax-rag
```

### 核心接口和类

#### 1. 文档模型

使用 kastrax-store 模块中的 Document 类作为基础文档模型：

```kotlin
data class Document(
    val id: String,
    val content: String,
    val metadata: Map<String, Any> = emptyMap()
)
```

#### 2. 搜索结果模型

使用 kastrax-store 模块中的 SearchResult 类作为基础搜索结果模型：

```kotlin
data class SearchResult(
    val id: String,
    val score: Double,
    val vector: FloatArray? = null,
    val metadata: Map<String, Any>? = null
)
```

#### 3. 文档向量存储接口

使用 kastrax-store 模块中的 DocumentVectorStore 接口：

```kotlin
interface DocumentVectorStore {
    val dimension: Int
    fun getVectorStore(): VectorStore
    suspend fun addDocuments(documents: List<Document>, embeddingService: EmbeddingService): Boolean
    suspend fun addDocuments(documents: List<Document>): Boolean
    suspend fun deleteDocuments(ids: List<String>): Boolean
    suspend fun similaritySearch(query: String, embeddingService: EmbeddingService, limit: Int = 5): List<DocumentSearchResult>
    suspend fun similaritySearch(embedding: FloatArray, limit: Int = 5): List<DocumentSearchResult>
    suspend fun similaritySearchWithFilter(embedding: FloatArray, filter: Map<String, Any>, limit: Int = 5): List<DocumentSearchResult>
    suspend fun keywordSearch(keywords: List<String>, limit: Int = 5): List<DocumentSearchResult>
    suspend fun metadataSearch(filter: Map<String, Any>, limit: Int = 5): List<DocumentSearchResult>
}
```

#### 4. RAG 类

重新设计 RAG 类，使其使用 DocumentVectorStore 接口：

```kotlin
class RAG(
    private val documentStore: DocumentVectorStore,
    private val embeddingService: EmbeddingService,
    private val reranker: Reranker = IdentityReranker(),
    private val defaultOptions: RagProcessOptions = RagProcessOptions()
)
```

## 实现计划

### 1. 文档处理模块

#### 1.1 文档加载器

保留现有的文档加载器实现，但修改其返回类型为 kastrax-store 模块中的 Document 类：

- TextDocumentLoader
- PdfDocumentLoader
- CsvDocumentLoader
- JsonDocumentLoader
- XmlDocumentLoader
- MarkdownDocumentLoader
- HtmlDocumentLoader
- WebPageDocumentLoader
- DirectoryDocumentLoader
- DatabaseDocumentLoader
- ApiDocumentLoader
- RssFeedDocumentLoader
- YouTubeSubtitleDocumentLoader

#### 1.2 文档分割器

保留现有的文档分割器实现，但修改其参数和返回类型为 kastrax-store 模块中的 Document 类：

- TextSplitter
- SemanticDocumentSplitter
- RecursiveCharacterTextSplitter
- TokenTextSplitter

#### 1.3 文档转换器

保留现有的文档转换器实现，但修改其参数和返回类型为 kastrax-store 模块中的 Document 类：

- DocumentCleaner
- DocumentNormalizer
- HtmlToTextConverter
- MetadataTransformer
- TableExtractor
- TextCleaner
- CompositeDocumentTransformer

### 2. 检索模块

#### 2.1 检索器接口

```kotlin
interface Retriever {
    suspend fun retrieve(query: String, limit: Int = 5, minScore: Double = 0.0): List<DocumentSearchResult>
}
```

#### 2.2 检索器实现

- VectorStoreRetriever：基于向量存储的检索器
- KeywordRetriever：基于关键词的检索器
- HybridRetriever：混合检索器，结合向量检索和关键词检索
- QueryEnhancedRetriever：查询增强检索器
- SemanticRetriever：语义检索器

### 3. 重排序模块

#### 3.1 重排序器接口

```kotlin
interface Reranker {
    suspend fun rerank(query: String, results: List<DocumentSearchResult>): List<DocumentSearchResult>
}
```

#### 3.2 重排序器实现

- IdentityReranker：恒等重排序器，不改变检索结果的顺序
- RelevanceReranker：相关性重排序器，基于相似度重排序
- DiversityReranker：多样性重排序器，增加结果的多样性
- LlmReranker：基于 LLM 的重排序器
- ContextAwareReranker：上下文感知重排序器
- EnhancedMetadataReranker：增强元数据重排序器

### 4. 上下文构建模块

#### 4.1 上下文构建器

```kotlin
class ContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig()
) {
    suspend fun buildContext(query: String, results: List<DocumentSearchResult>): String
}
```

#### 4.2 上下文构建配置

```kotlin
data class ContextBuilderConfig(
    val maxTokens: Int = 4000,
    val format: ContextFormat = ContextFormat.TEXT,
    val includeMetadata: Boolean = false,
    val metadataFields: List<String> = emptyList(),
    val separator: String = "\n\n"
)
```

### 5. RAG 主类

```kotlin
class RAG(
    private val documentStore: DocumentVectorStore,
    private val embeddingService: EmbeddingService,
    private val reranker: Reranker = IdentityReranker(),
    private val defaultOptions: RagProcessOptions = RagProcessOptions()
) {
    suspend fun loadDocuments(loader: DocumentLoader, splitter: DocumentSplitter? = null): Int
    suspend fun search(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): List<DocumentSearchResult>
    suspend fun generateContext(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): String
    suspend fun retrieveContext(query: String, limit: Int = 5, minScore: Double = 0.0, options: RagProcessOptions? = null): RetrieveContextResult
}
```

### 6. 高级功能模块

#### 6.1 实时 RAG

```kotlin
class RealTimeRag(
    private val rag: RAG,
    private val config: RealTimeRagConfig = RealTimeRagConfig()
) {
    suspend fun addDocument(document: Document): String
    suspend fun search(query: String, limit: Int = 5): List<DocumentSearchResult>
    suspend fun generateContext(query: String, limit: Int = 5): String
    suspend fun retrieveContext(query: String, limit: Int = 5): RetrieveContextResult
}
```

#### 6.2 图 RAG

```kotlin
class GraphRAG(
    private val rag: RAG,
    private val config: GraphRAGConfig = GraphRAGConfig()
) {
    suspend fun addDocument(document: Document): String
    suspend fun addRelation(sourceId: String, targetId: String, relation: String, weight: Double = 1.0): Boolean
    suspend fun search(query: String, limit: Int = 5): List<DocumentSearchResult>
    suspend fun generateContext(query: String, limit: Int = 5): String
    suspend fun retrieveContext(query: String, limit: Int = 5): RetrieveContextResult
}
```

#### 6.3 评估工具

```kotlin
class RagEvaluationTool(
    private val rag: RAG,
    private val llmClient: LlmClient
) {
    suspend fun evaluateRetrievalPrecision(query: String, relevantDocIds: List<String>, limit: Int = 5): Double
    suspend fun evaluateContextRelevance(query: String, limit: Int = 5): Double
    suspend fun evaluateAnswerQuality(query: String, expectedAnswer: String, limit: Int = 5): Double
    suspend fun evaluateHallucination(query: String, limit: Int = 5): Double
}
```

## 实现步骤

1. 创建基础接口和类 (已完成)
2. 实现文档处理模块 (已完成 TextFileLoader 和 TextSplitter)
3. 实现检索模块 (已完成 VectorStoreRetriever, KeywordRetriever 和 HybridRetriever)
4. 实现重排序模块 (已完成 IdentityReranker)
5. 实现上下文构建模块 (已完成 ContextBuilder)
6. 实现 RAG 主类 (已完成)
7. 实现高级功能模块 (待实现)
8. 编写单元测试 (已完成 RAGTest)
9. 编写集成测试 (待实现)
10. 编写示例代码 (已完成 SimpleRagExample)
11. 编写文档 (待实现)

## 测试计划

1. 单元测试：测试各个模块的功能
2. 集成测试：测试模块之间的交互
3. 性能测试：测试 RAG 系统的性能
4. 端到端测试：测试完整的 RAG 流程

## 文档计划

1. API 文档：详细描述各个接口和类的功能
2. 使用指南：提供使用 RAG 模块的示例代码
3. 架构文档：描述 RAG 模块的架构设计
4. 扩展指南：描述如何扩展 RAG 模块的功能

## 时间计划

1. 基础接口和类：1 天 (已完成)
2. 文档处理模块：2 天 (部分完成)
3. 检索模块：2 天 (已完成)
4. 重排序模块：2 天 (部分完成)
5. 上下文构建模块：1 天 (已完成)
6. RAG 主类：1 天 (已完成)
7. 高级功能模块：3 天 (待实现)
8. 测试：2 天 (部分完成)
9. 文档：1 天 (待实现)

总计：15 天

## 当前进度

我们已经完成了 RAG 模块的基础功能，包括：

1. 创建了基础接口和类
2. 实现了文档处理模块的部分功能（TextFileLoader 和 TextSplitter）
3. 实现了检索模块（VectorStoreRetriever, KeywordRetriever 和 HybridRetriever）
4. 实现了重排序模块的部分功能（IdentityReranker）
5. 实现了上下文构建模块（ContextBuilder）
6. 实现了 RAG 主类
7. 编写了单元测试（RAGTest）
8. 编写了示例代码（SimpleRagExample）

下一步计划：

1. 实现更多的文档处理模块（PdfDocumentLoader, CsvDocumentLoader 等）
2. 实现更多的重排序模块（RelevanceReranker, DiversityReranker 等）
3. 实现高级功能模块（RealTimeRag, GraphRAG 等）
4. 编写集成测试
5. 编写文档
