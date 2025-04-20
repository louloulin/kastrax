# RAG 系统设计与使用指南

## 概述

RAG（检索增强生成）系统是 kastrax 框架的核心组件之一，它允许开发者从各种来源加载文档，将其转换为向量表示，并在生成回答时检索相关信息。RAG 系统通过将检索与生成相结合，提高了生成内容的准确性、相关性和可靠性。

## 核心概念

### 文档 (Document)

文档是 RAG 系统的基本数据单元，包含内容和元数据：

- **内容**：文档的文本内容
- **元数据**：与文档相关的附加信息，如标题、来源、作者等

### 文档加载器 (DocumentLoader)

文档加载器负责从各种来源加载文档，如文件、网页、数据库等：

- **TextFileDocumentLoader**：从文本文件加载文档
- **HtmlFileDocumentLoader**：从 HTML 文件加载文档
- **WebPageDocumentLoader**：从网页加载文档
- **DirectoryDocumentLoader**：从目录加载多个文档

### 文档分割器 (DocumentSplitter)

文档分割器将长文档分割成更小的块，以便更有效地检索和处理：

- **CharacterTextSplitter**：按字符数量分割文档
- **ParagraphTextSplitter**：按段落分割文档
- **RecursiveCharacterTextSplitter**：递归地使用多个分隔符分割文档

### 嵌入 (Embedding)

嵌入是文本的向量表示，用于计算文本之间的相似度：

- **Embedding**：表示文本的嵌入向量
- **EmbeddedDocument**：带有嵌入向量的文档

### 嵌入服务 (EmbeddingService)

嵌入服务负责生成文本的嵌入向量：

- **OpenAIEmbeddingService**：使用 OpenAI API 生成嵌入向量
- **FastEmbedEmbeddingService**：使用本地模型生成嵌入向量，无需外部 API
- **RandomEmbeddingService**：生成随机嵌入向量，用于测试和开发

### 向量存储 (VectorStore)

向量存储负责存储和检索嵌入文档：

- **InMemoryVectorStore**：将嵌入文档存储在内存中

### RAG 系统 (RAG)

RAG 系统是上述组件的集成，提供了从文档中检索信息并生成增强上下文的功能。

## RAG 系统架构

RAG 系统的架构由以下主要组件组成：

1. **文档处理**：加载和分割文档
2. **嵌入生成**：将文档转换为向量表示
3. **向量存储**：存储和检索嵌入文档
4. **上下文生成**：根据查询检索相关文档并生成增强上下文

## 使用指南

### 创建 RAG 系统

```kotlin
// 创建向量存储
val vectorStore = InMemoryVectorStore()

// 选项 1：使用 OpenAI 嵌入服务（需要 API 密钥）
val openAIEmbeddingService = OpenAIEmbeddingService(apiKey = "your-api-key")
val ragWithOpenAI = RAG(vectorStore, openAIEmbeddingService)

// 选项 2：使用 FastEmbed 嵌入服务（本地模型，无需 API 密钥）
val fastEmbedService = FastEmbedEmbeddingService(
    modelId = "BAAI/bge-small-zh-v1.5",  // 中文小型模型
    maxLength = 512
)
val ragWithFastEmbed = RAG(vectorStore, fastEmbedService)

// 选项 3：使用随机嵌入服务（用于测试）
val randomEmbeddingService = RandomEmbeddingService(dimensions = 1536)
val ragWithRandom = RAG(vectorStore, randomEmbeddingService)
```

### 加载文档

```kotlin
// 创建文档分割器
val splitter = RecursiveCharacterTextSplitter(
    chunkSize = 1000,
    chunkOverlap = 200
)

// 从文件加载文档
val fileLoader = TextFileDocumentLoader("path/to/document.txt")
rag.loadDocuments(fileLoader, splitter)

// 从网页加载文档
val webLoader = WebPageDocumentLoader("https://example.com")
rag.loadDocuments(webLoader, splitter)

// 从目录加载文档
val directoryLoader = DirectoryDocumentLoader(
    directory = File("path/to/documents"),
    recursive = true,
    fileExtensions = listOf("txt", "md", "html")
)
rag.loadDocuments(directoryLoader, splitter)
```

### 搜索相关文档

```kotlin
// 搜索相关文档
val results = rag.search(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5
)

// 处理搜索结果
for (result in results) {
    println("文档: ${result.document.content}")
    println("相似度: ${result.score}")
    println("元数据: ${result.document.metadata}")
    println()
}
```

### 生成增强上下文

```kotlin
// 生成增强上下文
val context = rag.generateContext(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5
)

// 生成带元数据的增强上下文
val contextWithMetadata = rag.generateContextWithMetadata(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5,
    includeMetadata = true,
    metadataKeys = listOf("title", "source")
)
```

### 与代理集成

```kotlin
// 创建 RAG 代理
val ragAgent = agent {
    name = "RAG Agent"
    instructions = """
        你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

        上下文信息：
        {{context}}

        用户问题：
        {{question}}
    """.trimIndent()
    model = openai
}

// 检索相关上下文
val context = rag.generateContextWithMetadata(
    query = "人工智能的应用",
    limit = 5,
    minScore = 0.5
)

// 构建提示
val prompt = ragAgent.instructions
    .replace("{{context}}", context)
    .replace("{{question}}", "人工智能在医疗领域有哪些应用？")

// 生成回答
val response = ragAgent.generate(prompt)
```

### 与工作流集成

```kotlin
// 创建研究工作流
val researchWorkflow = workflow {
    name = "research-workflow"
    description = "研究和报告生成工作流"

    step(researchAgent) {
        id = "research"
        name = "研究"
        description = "从上下文中提取相关信息"
        variables = mapOf(
            "context" to variable("$.input.context"),
            "question" to variable("$.input.question")
        )
    }

    step(analysisAgent) {
        id = "analysis"
        name = "分析"
        description = "分析研究结果"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text"),
            "question" to variable("$.input.question")
        )
    }

    step(reportAgent) {
        id = "report"
        name = "报告生成"
        description = "生成最终报告"
        after("research", "analysis")
        variables = mapOf(
            "research" to variable("$.steps.research.output.text"),
            "analysis" to variable("$.steps.analysis.output.text"),
            "question" to variable("$.input.question")
        )
    }
}

// 检索相关上下文
val context = rag.generateContextWithMetadata(
    query = "人工智能的应用",
    limit = 10,
    minScore = 0.5
)

// 准备工作流输入
val input = mapOf(
    "context" to context,
    "question" to "人工智能在医疗领域有哪些应用？"
)

// 执行工作流
val result = researchWorkflow.execute(input)
```

## 高级功能

### 自定义嵌入服务

你可以创建自定义嵌入服务，实现 `EmbeddingService` 接口：

```kotlin
class CustomEmbeddingService : EmbeddingService {
    override suspend fun embed(text: String): Embedding {
        // 实现嵌入逻辑
    }
}
```

### 使用 FastEmbed 嵌入服务

FastEmbed 嵌入服务使用本地模型生成嵌入向量，无需外部 API：

```kotlin
// 创建 FastEmbed 嵌入服务
val fastEmbedService = FastEmbedEmbeddingService(
    modelId = "BAAI/bge-small-zh-v1.5",  // 中文小型模型
    maxLength = 512,                    // 最大文本长度
    cacheDir = "~/.cache/kastrax/models", // 模型缓存目录
    device = Device.cpu()                // 使用 CPU，也可以使用 GPU
)

// 使用完毕后关闭服务，释放资源
fastEmbedService.close()
```

支持的模型包括：

- `BAAI/bge-small-zh-v1.5`：中文小型模型，嵌入维度为 384
- `BAAI/bge-base-zh-v1.5`：中文中型模型，嵌入维度为 768
- `BAAI/bge-small-en-v1.5`：英文小型模型，嵌入维度为 384
- `BAAI/bge-base-en-v1.5`：英文中型模型，嵌入维度为 768
- `sentence-transformers/all-MiniLM-L6-v2`：多语言小型模型，嵌入维度为 384

### 自定义向量存储

你可以创建自定义向量存储，实现 `VectorStore` 接口：

```kotlin
class CustomVectorStore : VectorStore {
    override suspend fun addEmbeddedDocuments(documents: List<EmbeddedDocument>): Int {
        // 实现添加文档逻辑
    }

    override suspend fun similaritySearch(
        embedding: Embedding,
        limit: Int,
        minScore: Double
    ): List<SearchResult> {
        // 实现搜索逻辑
    }

    override suspend fun count(): Int {
        // 实现计数逻辑
    }

    override suspend fun clear() {
        // 实现清空逻辑
    }
}
```

### 自定义文档分割器

你可以创建自定义文档分割器，实现 `DocumentSplitter` 接口：

```kotlin
class CustomDocumentSplitter : DocumentSplitter {
    override fun split(document: Document): List<Document> {
        // 实现分割逻辑
    }
}
```

## 最佳实践

1. **文档分割**：将长文档分割成适当大小的块，通常在 500-1000 个字符之间，以便更有效地检索
2. **重叠分割**：使用重叠分割，确保上下文连续性，通常重叠 10-20% 的内容
3. **元数据**：添加丰富的元数据，如标题、来源、作者等，以便更好地理解和引用文档
4. **相似度阈值**：设置适当的相似度阈值（minScore），过滤掉不相关的文档
5. **结果数量**：限制返回结果的数量（limit），避免信息过载
6. **上下文长度**：注意生成的上下文长度，确保不超过模型的最大输入长度

## 示例应用

### 问答系统

使用 RAG 系统创建问答系统，从文档中检索信息并回答问题：

```kotlin
// 创建 RAG 系统
val rag = RAG(vectorStore, embeddingService)

// 加载文档
rag.loadDocuments(directoryLoader, splitter)

// 创建问答代理
val qaAgent = agent {
    name = "QA Agent"
    instructions = """
        你是一个问答助手，使用提供的上下文回答问题。

        上下文：
        {{context}}

        问题：
        {{question}}
    """.trimIndent()
    model = openai
}

// 回答问题
val question = "人工智能在医疗领域有哪些应用？"
val context = rag.generateContextWithMetadata(question)
val prompt = qaAgent.instructions
    .replace("{{context}}", context)
    .replace("{{question}}", question)
val answer = qaAgent.generate(prompt).text
```

### 研究助手

使用 RAG 系统创建研究助手，帮助用户研究特定主题：

```kotlin
// 创建 RAG 系统
val rag = RAG(vectorStore, embeddingService)

// 加载文档
rag.loadDocuments(directoryLoader, splitter)

// 创建研究工作流
val researchWorkflow = workflow {
    // ... 研究工作流步骤 ...
}

// 执行研究
val topic = "人工智能在医疗领域的应用"
val context = rag.generateContextWithMetadata(topic)
val input = mapOf(
    "context" to context,
    "topic" to topic
)
val result = researchWorkflow.execute(input)
```

## 总结

RAG 系统是 kastrax 框架中的强大组件，它通过将检索与生成相结合，提高了生成内容的准确性、相关性和可靠性。通过使用 RAG 系统，开发者可以创建能够利用大量文档知识的智能应用，如问答系统、研究助手、内容生成器等。
