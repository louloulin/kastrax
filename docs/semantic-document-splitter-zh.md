# 语义文档分块器 (SemanticDocumentSplitter)

## 1. 概述

语义文档分块器是 KastraX RAG 系统的一个重要组件，它基于文本的语义相似性将文档分割成更小的块。与传统的基于字符或分隔符的分块方法不同，语义分块器能够识别文本的语义结构，将语义相似的内容组合在一起，从而生成更有意义和连贯的文本块。

## 2. 工作原理

语义文档分块器的工作原理如下：

1. **初始分割**：首先使用传统的分隔符（如段落、句号等）将文档分割成小片段
2. **嵌入计算**：使用嵌入服务为每个片段生成嵌入向量
3. **相似度计算**：计算相邻片段之间的余弦相似度
4. **合并片段**：基于相似度阈值和块大小限制，将语义相似的片段合并成更大的块
5. **后处理**：确保所有块都不超过最大块大小，并添加适当的元数据

## 3. 主要特点

- **语义感知**：基于文本的语义内容而不仅仅是字符数量进行分块
- **可调节的相似度阈值**：通过调整相似度阈值控制分块的粒度
- **灵活的初始分隔符**：支持自定义初始分隔符列表
- **元数据增强**：自动添加分块相关的元数据
- **错误处理**：在嵌入服务失败时自动回退到递归字符分块

## 4. 使用方法

### 4.1 基本用法

```kotlin
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.SemanticDocumentSplitter
import ai.kastrax.rag.embedding.FastEmbedEmbeddingService

// 创建嵌入服务
val embeddingService = FastEmbedEmbeddingService.create()

// 创建语义分块器
val splitter = SemanticDocumentSplitter(
    embeddingService = embeddingService,
    chunkSize = 1000,
    chunkOverlap = 200,
    similarityThreshold = 0.7
)

// 分割文档
val document = Document("这是要分割的长文本内容...")
val chunks = splitter.split(document)

// 处理分割后的块
chunks.forEach { chunk ->
    println("块内容: ${chunk.content}")
    println("块元数据: ${chunk.metadata}")
}
```

### 4.2 配置选项

语义分块器提供了多种配置选项：

```kotlin
val splitter = SemanticDocumentSplitter(
    embeddingService = embeddingService,  // 嵌入服务，用于计算文本的嵌入向量
    chunkSize = 1000,                     // 每个块的目标大小（字符数）
    chunkOverlap = 200,                   // 相邻块之间的重叠字符数
    similarityThreshold = 0.7,            // 相似性阈值，用于确定是否合并片段
    initialSeparators = listOf("\n\n", "\n", ". ", "! ", "? "),  // 初始分隔符列表
    addMetadata = true                    // 是否添加分割相关的元数据
)
```

### 4.3 与 RAG 系统集成

语义分块器可以与 KastraX 的 RAG 系统无缝集成：

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.TextFileDocumentLoader
import ai.kastrax.rag.embedding.FastEmbedEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore

// 创建 RAG 系统
val embeddingService = FastEmbedEmbeddingService.create()
val vectorStore = InMemoryVectorStore()
val rag = RAG(vectorStore, embeddingService)

// 加载并分块文档
val loader = TextFileDocumentLoader("document.txt")
val splitter = SemanticDocumentSplitter(embeddingService)
val documents = loader.load()
val chunks = documents.flatMap { splitter.split(it) }

// 添加到 RAG 系统
rag.addDocuments(chunks)

// 搜索相关文档
val results = rag.search("查询内容", limit = 5)
```

## 5. 调优指南

### 5.1 相似度阈值调整

相似度阈值是控制分块粒度的关键参数：

- **高阈值（如 0.8-0.9）**：只有非常相似的片段才会被合并，产生更多、更小的块
- **中等阈值（如 0.6-0.7）**：平衡块大小和语义连贯性
- **低阈值（如 0.3-0.5）**：更宽松的合并条件，产生更少、更大的块

### 5.2 初始分隔符选择

初始分隔符的选择会影响初始分割的粒度：

- 对于结构化文档，使用段落分隔符（`\n\n`）作为主要分隔符
- 对于连续文本，使用句号、问号等标点符号
- 对于特定领域文档，可以添加领域特定的分隔符

### 5.3 块大小和重叠设置

- **块大小**：根据您的嵌入模型和检索需求调整
  - 较小的块（300-500字符）适合精确检索
  - 较大的块（1000-2000字符）提供更多上下文
- **块重叠**：通常设置为块大小的 10%-20%
  - 重叠过大会增加存储和处理成本
  - 重叠过小可能导致上下文丢失

## 6. 性能考虑

### 6.1 计算成本

语义分块比传统分块方法计算成本更高，因为：

- 需要为每个初始片段生成嵌入向量
- 需要计算片段之间的相似度
- 可能需要多次重新计算合并片段的嵌入

### 6.2 优化策略

- 对于非常长的文档，先使用传统方法进行粗粒度分块，再应用语义分块
- 考虑使用批量嵌入而不是单个嵌入，以提高效率
- 对于实时应用，可以预先处理文档并缓存结果

### 6.3 内存使用

- 处理大型文档时，语义分块可能消耗大量内存
- 如果内存是瓶颈，考虑增加初始分块的粒度或使用流式处理

## 7. 错误处理

语义分块器内置了健壮的错误处理机制：

- 如果嵌入服务失败，会自动回退到递归字符分块
- 如果初始分割产生空片段，会自动过滤掉
- 如果合并后的片段超过最大块大小，会使用字符分块进一步分割

## 8. 示例场景

### 8.1 长篇文章分块

```kotlin
// 加载长篇文章
val loader = TextFileDocumentLoader("article.txt")
val document = loader.load().first()

// 使用语义分块
val splitter = SemanticDocumentSplitter(
    embeddingService = embeddingService,
    chunkSize = 1500,  // 较大的块大小，适合长篇文章
    similarityThreshold = 0.6  // 中等阈值，平衡块大小和语义连贯性
)

val chunks = splitter.split(document)
```

### 8.2 技术文档分块

```kotlin
// 加载技术文档
val loader = MarkdownDocumentLoader("technical_doc.md")
val document = loader.load().first()

// 使用语义分块，针对技术文档优化
val splitter = SemanticDocumentSplitter(
    embeddingService = embeddingService,
    chunkSize = 800,  // 较小的块大小，适合精确检索
    similarityThreshold = 0.75,  // 较高阈值，保持技术概念的完整性
    initialSeparators = listOf("\n\n", "\n", "## ", "### ", ". ")  // 包含 Markdown 标题分隔符
)

val chunks = splitter.split(document)
```

## 9. 与其他分块器的比较

| 分块器类型 | 优点 | 缺点 | 适用场景 |
|------------|------|------|----------|
| 字符分块 | 简单、快速、低资源消耗 | 可能切断语义单元 | 简单文本、资源受限环境 |
| 段落分块 | 保持段落完整性、中等复杂度 | 段落长度不一致 | 结构化文档、文章 |
| 递归字符分块 | 灵活、可处理各种文本 | 不考虑语义 | 通用场景、混合文本 |
| 语义分块 | 保持语义连贯性、智能分块 | 计算成本高、依赖嵌入服务 | 高质量 RAG、需要语义连贯性的场景 |

## 10. 总结

语义文档分块器是 KastraX RAG 系统中的高级组件，它通过考虑文本的语义内容而不仅仅是形式结构来分割文档。这种方法产生的文本块更有意义、更连贯，从而提高了检索和生成的质量。虽然计算成本较高，但在需要高质量 RAG 结果的场景中，这种投资是值得的。

通过调整相似度阈值、初始分隔符和块大小等参数，您可以根据特定需求优化语义分块器的行为，在精确检索和提供足够上下文之间取得平衡。
