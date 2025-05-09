# KastraX RAG 使用指南

本指南提供了如何使用 KastraX RAG 模块的详细说明。

## 快速开始

### 基本用法

以下是使用 KastraX RAG 的基本示例：

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import ai.kastrax.store.document.Document
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = EmbeddingServiceFactory.createRandomEmbeddingService()
    
    // 创建向量存储
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
    
    // 创建文档向量存储适配器
    val documentStore = DocumentVectorStoreAdapter(vectorStore)
    
    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = IdentityReranker()
    )
    
    // 创建文档
    val documents = listOf(
        Document(
            id = "1",
            content = "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            metadata = mapOf("source" to "AI百科")
        ),
        Document(
            id = "2",
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            metadata = mapOf("source" to "AI百科")
        )
    )
    
    // 加载文档
    rag.loadDocuments(documents)
    
    // 搜索文档
    val results = rag.search("人工智能", limit = 3)
    println("搜索结果:")
    results.forEach { result ->
        println("${result.document.content} (分数: ${result.score})")
    }
    
    // 生成上下文
    val context = rag.generateContext("人工智能", limit = 3)
    println("\n生成的上下文:")
    println(context)
}
```

### 使用不同的嵌入服务

KastraX RAG 支持多种嵌入服务：

```kotlin
// 使用 OpenAI 嵌入服务
val openaiEmbeddingService = EmbeddingServiceFactory.createOpenAIEmbeddingService(
    apiKey = "your-openai-api-key"
)

// 使用 Deepseek 嵌入服务
val deepseekEmbeddingService = EmbeddingServiceFactory.createDeepseekEmbeddingService(
    apiKey = "your-deepseek-api-key"
)

// 使用 FastEmbed Kotlin 嵌入服务（本地嵌入）
val fastEmbedService = EmbeddingServiceFactory.createFastEmbedKotlinEmbeddingService()

// 使用缓存嵌入服务
val cachedEmbeddingService = EmbeddingServiceFactory.createCachedEmbeddingService(
    delegate = openaiEmbeddingService
)
```

### 使用不同的向量存储

KastraX RAG 支持多种向量存储：

```kotlin
// 使用内存向量存储
val inMemoryVectorStore = VectorStoreFactory.createInMemoryVectorStore()

// 使用 Chroma 向量存储
val chromaVectorStore = VectorStoreFactory.createChromaVectorStore(
    collectionName = "my-collection",
    url = "http://localhost:8000"
)

// 使用 Qdrant 向量存储
val qdrantVectorStore = VectorStoreFactory.createQdrantVectorStore(
    collectionName = "my-collection",
    url = "http://localhost:6333"
)

// 使用 Milvus 向量存储
val milvusVectorStore = VectorStoreFactory.createMilvusVectorStore(
    collectionName = "my-collection",
    url = "http://localhost:19530"
)
```

### 使用不同的重排序器

KastraX RAG 支持多种重排序器：

```kotlin
// 使用身份重排序器（不进行重排序）
val identityReranker = IdentityReranker()

// 使用相关性重排序器
val relevanceReranker = RelevanceReranker(embeddingService)

// 使用多样性重排序器
val diversityReranker = DiversityReranker(embeddingService)

// 使用交叉编码器重排序器
val crossEncoderReranker = CrossEncoderReranker(llmClient)
```

## 高级用法

### 配置 RAG 处理选项

您可以使用 `RagProcessOptions` 来配置 RAG 系统的检索和重排序过程：

```kotlin
val options = RagProcessOptions(
    useHybridSearch = true,
    useSemanticRetrieval = true,
    useReranking = true,
    useQueryEnhancement = true,
    hybridOptions = HybridOptions(
        vectorWeight = 0.7,
        keywordWeight = 0.3
    ),
    semanticOptions = SemanticOptions(
        useChunking = true,
        chunkSize = 1000,
        chunkOverlap = 200
    ),
    rerankingOptions = RerankingOptions(
        useDiversity = true,
        diversityWeight = 0.3,
        useMetadata = true,
        metadataFields = listOf("source", "category"),
        metadataWeights = mapOf("source" to 0.5, "category" to 0.5)
    ),
    queryEnhancementOptions = QueryEnhancementOptions(
        useSynonyms = true,
        useDecomposition = true,
        useNormalization = true
    ),
    contextOptions = ContextBuilderConfig(
        maxTokens = 2000,
        format = ContextFormat.MARKDOWN,
        includeMetadata = true,
        metadataFields = listOf("source"),
        separator = "\n\n"
    )
)

// 使用配置选项
val results = rag.search("人工智能", limit = 5, options = options)
val context = rag.generateContext("人工智能", limit = 5, options = options)
```

### 使用图 RAG

KastraX RAG 支持基于图的检索增强生成：

```kotlin
import ai.kastrax.rag.graph.GraphRAG
import ai.kastrax.rag.graph.GraphRAGConfig
import ai.kastrax.rag.graph.GraphRAGQueryOptions
import ai.kastrax.rag.graph.GraphRAGTool
import ai.kastrax.rag.document.DirectoryDocumentLoader
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = EmbeddingServiceFactory.createRandomEmbeddingService()
    
    // 创建文档加载器
    val documentLoader = DirectoryDocumentLoader("path/to/documents")
    
    // 创建 GraphRAG 配置
    val config = GraphRAGConfig(
        dimension = embeddingService.dimension(),
        threshold = 0.7,
        bidirectional = true
    )
    
    // 创建 GraphRAG 实例
    val graphRAG = GraphRAG(config)
    
    // 创建 GraphRAGTool
    val graphRAGTool = GraphRAGTool(
        documentLoader = documentLoader,
        embeddingService = embeddingService,
        graphRAG = graphRAG
    )
    
    // 执行查询
    val result = graphRAGTool.execute(
        query = "人工智能",
        topK = 5,
        randomWalkSteps = 100,
        restartProb = 0.15,
        includeMetadata = true
    )
    
    println(result.content)
}
```

### 评估和优化 RAG 系统

KastraX RAG 提供了评估和优化 RAG 系统的工具：

```kotlin
import ai.kastrax.rag.evaluation.RagEvaluationTool
import ai.kastrax.rag.optimization.RagOptimizationTool
import ai.kastrax.rag.benchmark.RagBenchmarkTool
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建 RAG 实例
    val rag = createRagInstance()
    
    // 创建 LLM 客户端
    val llmClient = createLlmClient()
    
    // 创建评估工具
    val evaluationTool = RagEvaluationTool(rag, llmClient)
    
    // 评估上下文相关性
    val relevanceScore = evaluationTool.evaluateContextRelevance("人工智能", limit = 5)
    println("上下文相关性分数: $relevanceScore")
    
    // 创建优化工具
    val optimizationTool = RagOptimizationTool(rag, evaluationTool)
    
    // 定义查询和参考答案
    val queries = listOf("人工智能是什么？", "机器学习的应用有哪些？")
    val groundTruths = listOf(
        "人工智能是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
        "机器学习的应用包括推荐系统、垃圾邮件过滤、欺诈检测和预测分析等。"
    )
    
    // 定义生成回答的函数
    val generateAnswer = { query: String, context: String ->
        llmClient.generateText(
            prompt = "基于以下上下文回答问题：\n\n上下文：$context\n\n问题：$query\n\n回答："
        )
    }
    
    // 优化 RAG 系统的配置
    val optimizationResult = optimizationTool.optimize(
        queries = queries,
        generateAnswer = generateAnswer,
        groundTruths = groundTruths
    )
    
    println("最佳配置: ${optimizationResult.bestConfig}")
    println("最佳分数: ${optimizationResult.bestScore}")
    
    // 创建基准测试工具
    val benchmarkTool = RagBenchmarkTool(rag)
    
    // 运行基准测试
    val benchmarkSummary = benchmarkTool.runBenchmark(
        queries = queries,
        limit = 5,
        parallel = true
    )
    
    println("平均检索时间: ${benchmarkSummary.averageRetrievalTime}ms")
    println("平均上下文生成时间: ${benchmarkSummary.averageContextGenerationTime}ms")
    println("平均总时间: ${benchmarkSummary.averageTotalTime}ms")
}
```

## 最佳实践

### 文档处理

- 使用适当的文档分割器将长文档分割成更小的片段
- 使用文档清理器清理文档内容，去除不必要的格式和噪声
- 为文档添加有用的元数据，如来源、类别和日期

### 检索策略

- 对于一般用途，使用混合检索（向量 + 关键词）
- 对于需要高精度的场景，使用语义检索
- 对于需要高召回率的场景，使用查询增强

### 重排序策略

- 对于需要高相关性的场景，使用相关性重排序器
- 对于需要多样性的场景，使用多样性重排序器
- 对于需要高精度的场景，使用交叉编码器重排序器

### 性能优化

- 使用缓存嵌入服务缓存嵌入向量
- 使用本地嵌入服务（如 FastEmbedKotlinEmbeddingService）减少 API 调用
- 使用并行处理提高性能
- 使用基准测试工具找出性能瓶颈

### 评估和优化

- 使用评估工具评估 RAG 系统的性能
- 使用优化工具找到最佳配置
- 定期重新评估和优化 RAG 系统
