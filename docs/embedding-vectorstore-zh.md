# 嵌入和向量存储

## 1. 概述

嵌入和向量存储是 KastraX RAG 系统的核心组件，它们负责将文本转换为向量表示，并提供高效的相似性搜索功能。本文档详细介绍了 KastraX 中实现的嵌入服务和向量存储功能，包括 Hugging Face 嵌入服务、FAISS 向量存储和混合搜索功能。

## 2. 嵌入服务 (EmbeddingService)

### 2.1 功能介绍

嵌入服务负责将文本转换为向量表示，这些向量捕捉了文本的语义信息，使得可以通过向量相似性来衡量文本的语义相似性。KastraX 提供了多种嵌入服务实现，支持不同的模型和 API。

### 2.2 接口定义

```kotlin
interface EmbeddingService {
    suspend fun embed(text: String): Embedding
    suspend fun embedBatch(texts: List<String>): List<Embedding>
}
```

### 2.3 Hugging Face 嵌入服务

Hugging Face 嵌入服务使用 Hugging Face Inference API 生成文本嵌入向量。它支持多种嵌入模型，并提供了重试机制和错误处理。

#### 2.3.1 主要特点

- 支持多种 Hugging Face 嵌入模型
- 提供批量嵌入功能
- 内置重试机制和错误处理
- 支持自定义超时和重试参数

#### 2.3.2 使用示例

```kotlin
// 创建 Hugging Face 嵌入服务
val embeddingService = HuggingFaceEmbeddingService(
    apiKey = "your-api-key",
    modelId = "sentence-transformers/all-MiniLM-L6-v2",
    maxRetries = 3,
    timeout = 30000
)

// 嵌入单个文本
val text = "这是一个测试文本"
val embedding = embeddingService.embed(text)

// 嵌入多个文本
val texts = listOf("文本1", "文本2", "文本3")
val embeddings = embeddingService.embedBatch(texts)
```

### 2.4 随机嵌入服务

随机嵌入服务生成随机嵌入向量，主要用于测试和开发。它对于相同的输入文本总是生成相同的向量，确保测试的一致性。

#### 2.4.1 主要特点

- 生成随机但一致的嵌入向量
- 支持自定义向量维度和随机种子
- 适用于测试和开发环境

#### 2.4.2 使用示例

```kotlin
// 创建随机嵌入服务
val embeddingService = RandomEmbeddingService(
    dimensions = 384,
    seed = 42
)

// 嵌入文本
val text = "这是一个测试文本"
val embedding = embeddingService.embed(text)
```

## 3. 向量存储 (VectorStore)

### 3.1 功能介绍

向量存储负责存储嵌入向量和相关文档，并提供高效的相似性搜索功能。KastraX 提供了多种向量存储实现，支持不同的存储和索引方式。

### 3.2 接口定义

```kotlin
interface VectorStore {
    suspend fun addEmbeddedDocuments(documents: List<EmbeddedDocument>): Int
    suspend fun similaritySearch(embedding: Embedding, limit: Int, minScore: Double): List<SearchResult>
    suspend fun count(): Int
    suspend fun clear()
}
```

### 3.3 FAISS 向量存储

FAISS (Facebook AI Similarity Search) 向量存储使用 FAISS 库提供高效的相似性搜索功能。它支持精确搜索和近似搜索，并提供了索引持久化功能。

#### 3.3.1 主要特点

- 高效的相似性搜索
- 支持精确搜索 (Flat) 和近似搜索 (IVFFlat)
- 支持索引持久化
- 支持自定义距离度量和索引参数

#### 3.3.2 使用示例

```kotlin
// 创建 FAISS 向量存储
val vectorStore = FaissVectorStore(
    dimension = 384,
    indexType = "Flat",
    metric = "IP"
)

// 添加嵌入文档
val documents = listOf(
    EmbeddedDocument(Document("文档1"), embedding1),
    EmbeddedDocument(Document("文档2"), embedding2),
    EmbeddedDocument(Document("文档3"), embedding3)
)
vectorStore.addEmbeddedDocuments(documents)

// 搜索相似文档
val queryEmbedding = embeddingService.embed("查询文本")
val results = vectorStore.similaritySearch(queryEmbedding, limit = 5, minScore = 0.7)

// 保存和加载索引
vectorStore.saveIndex("index.faiss")
vectorStore.loadIndex("index.faiss")

// 释放资源
vectorStore.close()
```

### 3.4 内存向量存储

内存向量存储将嵌入向量和文档存储在内存中，适用于小型数据集和开发环境。它提供了简单的相似性搜索功能，但不支持持久化。

#### 3.4.1 主要特点

- 简单易用
- 适用于小型数据集和开发环境
- 不需要额外的依赖

#### 3.4.2 使用示例

```kotlin
// 创建内存向量存储
val vectorStore = InMemoryVectorStore()

// 添加嵌入文档
val documents = listOf(
    EmbeddedDocument(Document("文档1"), embedding1),
    EmbeddedDocument(Document("文档2"), embedding2),
    EmbeddedDocument(Document("文档3"), embedding3)
)
vectorStore.addEmbeddedDocuments(documents)

// 搜索相似文档
val queryEmbedding = embeddingService.embed("查询文本")
val results = vectorStore.similaritySearch(queryEmbedding, limit = 5, minScore = 0.7)
```

## 4. 混合搜索 (HybridSearch)

### 4.1 功能介绍

混合搜索结合了向量相似性搜索和其他搜索方法，如元数据过滤和关键词匹配，提供更精确和灵活的搜索功能。

### 4.2 元数据过滤

元数据过滤允许根据文档的元数据属性过滤搜索结果，例如文档类型、日期、作者等。

#### 4.2.1 使用示例

```kotlin
// 创建元数据过滤器
val filter: MetadataFilter = { metadata ->
    val category = metadata["category"] as? String
    val date = metadata["date"] as? String
    category == "技术" && date?.startsWith("2023") == true
}

// 执行混合搜索
val queryEmbedding = embeddingService.embed("查询文本")
val results = HybridSearch.hybridSearch(
    vectorStore = vectorStore,
    embedding = queryEmbedding,
    filter = filter,
    limit = 5,
    minScore = 0.7
)
```

### 4.3 关键词匹配

关键词匹配结合了向量相似性和关键词匹配分数，提供更精确的搜索结果。

#### 4.3.1 使用示例

```kotlin
// 执行混合关键词搜索
val query = "人工智能和机器学习"
val keywords = listOf("人工智能", "机器学习", "深度学习")

val results = HybridSearch.hybridKeywordSearch(
    vectorStore = vectorStore,
    text = query,
    embeddingService = embeddingService,
    keywords = keywords,
    vectorWeight = 0.7,
    keywordWeight = 0.3,
    limit = 5,
    minScore = 0.7
)

// 转换为标准搜索结果
val standardResults = results.toSearchResults()
```

## 5. 最佳实践

### 5.1 嵌入服务选择

- 对于生产环境，推荐使用 `FastEmbedEmbeddingService` 或 `HuggingFaceEmbeddingService`
- 对于测试和开发环境，可以使用 `RandomEmbeddingService`
- 根据需要的语言支持和嵌入质量选择合适的模型

### 5.2 向量存储选择

- 对于小型数据集和开发环境，可以使用 `InMemoryVectorStore`
- 对于大型数据集和生产环境，推荐使用 `FaissVectorStore`
- 如果需要持久化，使用 `FaissVectorStore` 的保存和加载功能

### 5.3 搜索策略

- 对于简单搜索，直接使用 `vectorStore.similaritySearch`
- 对于需要元数据过滤的搜索，使用 `HybridSearch.hybridSearch`
- 对于需要关键词匹配的搜索，使用 `HybridSearch.hybridKeywordSearch`
- 根据需要调整相似度阈值和结果数量

### 5.4 性能优化

- 对于批量处理，使用 `embedBatch` 而不是多次调用 `embed`
- 对于 FAISS 向量存储，根据数据集大小和查询需求选择合适的索引类型
- 对于频繁查询的场景，考虑使用缓存机制

## 6. 安装和配置

### 6.1 依赖项

要使用 KastraX 的嵌入和向量存储功能，需要添加以下依赖项：

```kotlin
// Kotlin 协程
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

// HTTP 客户端（用于 API 调用）
implementation("io.ktor:ktor-client-core:2.3.4")
implementation("io.ktor:ktor-client-cio:2.3.4")
implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
implementation("io.ktor:ktor-client-auth:2.3.4")

// 向量相似度计算
implementation("org.apache.commons:commons-math3:3.6.1")

// FAISS（可选，用于向量搜索）
// 注意：FAISS JNI 绑定需要单独安装
compileOnly(files("libs/faiss-jni.jar"))
```

### 6.2 FAISS 安装

要使用 FAISS 向量存储，需要安装 FAISS 库和 JNI 绑定：

1. 安装 FAISS 库：
   ```bash
   # 使用 conda
   conda install -c pytorch faiss-cpu
   
   # 或使用 pip
   pip install faiss-cpu
   ```

2. 编译 JNI 绑定：
   ```bash
   # 克隆 FAISS 仓库
   git clone https://github.com/facebookresearch/faiss.git
   cd faiss
   
   # 编译 JNI 绑定
   cmake -B build -DFAISS_ENABLE_PYTHON=OFF -DFAISS_ENABLE_GPU=OFF -DFAISS_ENABLE_JNI=ON
   cmake --build build --config Release
   
   # 安装
   cmake --install build
   ```

3. 将生成的 `faiss_jni.jar` 和 `libfaiss_jni.so`（或 `.dll`、`.dylib`）添加到项目中。

### 6.3 API 密钥配置

对于使用外部 API 的嵌入服务，需要配置 API 密钥：

```kotlin
// 使用环境变量
val apiKey = System.getenv("HUGGINGFACE_API_KEY")

// 或使用配置文件
val properties = Properties()
properties.load(FileInputStream("config.properties"))
val apiKey = properties.getProperty("huggingface.api.key")

// 创建嵌入服务
val embeddingService = HuggingFaceEmbeddingService(apiKey)
```

## 7. 总结

嵌入和向量存储是 KastraX RAG 系统的核心组件，它们提供了将文本转换为向量表示并进行高效相似性搜索的功能。通过选择合适的嵌入服务和向量存储实现，以及使用混合搜索功能，可以构建高效、精确的检索系统。

KastraX 提供了多种嵌入服务和向量存储实现，支持不同的使用场景和需求。无论是小型开发项目还是大型生产系统，都可以找到合适的组件和配置。
