# KastraX RAG

KastraX RAG 是一个用 Kotlin 编写的检索增强生成（Retrieval-Augmented Generation）系统，它提供了一套灵活、高效的工具，用于构建基于文档检索的生成式 AI 应用。

## 特性

- **多种文档处理**：支持加载、分割和处理各种格式的文档
- **灵活的嵌入选项**：支持多种嵌入模型和服务
- **多样化的检索策略**：
  - 基本的 Top-K 检索
  - 语义检索，支持查询扩展和语义聚类
  - 混合检索，结合向量搜索和关键词搜索
  - 查询增强检索，通过查询转换和多查询策略提高检索效果
- **可定制的重排序**：支持多种重排序策略，包括相关性重排序和 LLM 重排序
- **上下文构建**：智能构建适合 LLM 输入的上下文

## 快速开始

### 安装

将 KastraX RAG 添加到您的项目中：

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-rag:1.0.0")
}
```

### 基本用法

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.FastEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore

// 创建向量存储和嵌入服务
val vectorStore = InMemoryVectorStore()
val embeddingService = FastEmbeddingService()

// 创建 RAG 系统
val rag = RAG(vectorStore, embeddingService)

// 加载文档
val documents = listOf(
    Document("这是第一个文档的内容", mapOf("source" to "doc1.txt")),
    Document("这是第二个文档的内容", mapOf("source" to "doc2.txt"))
)
rag.loadDocuments(documents)

// 搜索相关文档
val results = rag.search("查询内容", limit = 5)

// 生成回答
val answer = rag.generateAnswer("用户问题", "system prompt")
```

## 高级功能

### 查询转换和增强

KastraX RAG 提供了强大的查询转换和增强机制，可以提高检索的准确性和多样性：

```kotlin
// 使用查询增强
val results = rag.search(
    "什么是AI",
    limit = 5,
    options = RagProcessOptions(
        useQueryEnhancement = true,
        queryEnhancementOptions = QueryEnhancedRetrieverConfig(
            useMultiQuery = true,
            mergeStrategy = MergeStrategy.DIVERSITY
        )
    )
)
```

查询增强支持多种转换策略：

- **规范化**：清理和规范化查询文本
- **同义词扩展**：添加同义词或相关术语
- **查询分解**：将复杂查询分解为多个简单查询
- **LLM 重写**：使用大型语言模型重写查询

### 混合检索

结合向量搜索和关键词搜索，提高检索效果：

```kotlin
val results = rag.search(
    "查询内容",
    limit = 5,
    options = RagProcessOptions(
        useHybridSearch = true,
        hybridOptions = HybridRetrieverConfig(
            vectorWeight = 0.7,
            keywordWeight = 0.3
        )
    )
)
```

### 语义检索

使用语义理解增强检索结果：

```kotlin
val results = rag.search(
    "查询内容",
    limit = 5,
    options = RagProcessOptions(
        useSemanticRetrieval = true,
        semanticOptions = SemanticRetrieverConfig(
            expandQuery = true,
            useSemanticClustering = true
        )
    )
)
```

## 文档

详细文档请参阅 [docs](./docs/) 目录：

- [文档处理](./docs/document-processing.md)
- [嵌入和向量存储](./docs/embedding-and-vector-store.md)
- [检索策略](./docs/retrieval-strategies.md)
- [重排序](./docs/reranking.md)
- [查询转换和增强](./docs/query-enhancement.md)
- [上下文构建](./docs/context-building.md)

## 贡献

欢迎贡献代码、报告问题或提出改进建议。请参阅 [CONTRIBUTING.md](./CONTRIBUTING.md) 了解更多信息。

## 许可证

KastraX RAG 使用 [Apache 2.0 许可证](./LICENSE)。
