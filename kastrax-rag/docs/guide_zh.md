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

## 案例研究

### 案例 1：构建知识库问答系统

在这个案例中，我们将使用 KastraX RAG 构建一个知识库问答系统，用于回答关于公司产品的问题。

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.PdfDocumentLoader
import ai.kastrax.rag.document.TextSplitter
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import ai.kastrax.rag.reranker.RelevanceReranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = EmbeddingServiceFactory.createFastEmbedKotlinEmbeddingService()
    
    // 创建向量存储
    val vectorStore = VectorStoreFactory.createChromaVectorStore(
        collectionName = "product-knowledge-base",
        url = "http://localhost:8000"
    )
    
    // 创建文档向量存储适配器
    val documentStore = DocumentVectorStoreAdapter(vectorStore)
    
    // 创建重排序器
    val reranker = RelevanceReranker(embeddingService)
    
    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = reranker,
        defaultOptions = RagProcessOptions(
            useHybridSearch = true,
            useReranking = true
        )
    )
    
    // 加载文档
    val pdfLoader = PdfDocumentLoader("path/to/product/manuals")
    val textSplitter = TextSplitter(chunkSize = 1000, chunkOverlap = 200)
    
    val documents = pdfLoader.load()
    val chunks = documents.flatMap { document ->
        textSplitter.split(document)
    }
    
    rag.loadDocuments(chunks)
    
    // 创建问答函数
    val answerQuestion = { query: String ->
        val context = rag.generateContext(query, limit = 5)
        
        // 使用 LLM 生成回答
        val llmClient = createLlmClient()
        llmClient.generateText(
            prompt = "你是一个产品专家助手。基于以下上下文回答问题：\n\n上下文：$context\n\n问题：$query\n\n回答："
        )
    }
    
    // 示例问题
    val questions = listOf(
        "产品 A 的主要功能是什么？",
        "如何解决产品 B 的常见问题？",
        "产品 C 和产品 D 有什么区别？"
    )
    
    questions.forEach { question ->
        println("问题: $question")
        println("回答: ${answerQuestion(question)}")
        println()
    }
}
```

### 案例 2：构建多语言文档搜索系统

在这个案例中，我们将使用 KastraX RAG 构建一个多语言文档搜索系统，支持中文、英文和日文。

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.ParagraphSplitter
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import ai.kastrax.rag.reranker.DiversityReranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = EmbeddingServiceFactory.createDeepseekEmbeddingService(
        apiKey = "your-deepseek-api-key"
    )
    
    // 创建向量存储
    val vectorStore = VectorStoreFactory.createMilvusVectorStore(
        collectionName = "multilingual-documents",
        url = "http://localhost:19530"
    )
    
    // 创建文档向量存储适配器
    val documentStore = DocumentVectorStoreAdapter(vectorStore)
    
    // 创建重排序器
    val reranker = DiversityReranker(embeddingService)
    
    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = reranker,
        defaultOptions = RagProcessOptions(
            useHybridSearch = true,
            useReranking = true
        )
    )
    
    // 加载文档
    val chineseLoader = DirectoryDocumentLoader("path/to/chinese/documents")
    val englishLoader = DirectoryDocumentLoader("path/to/english/documents")
    val japaneseLoader = DirectoryDocumentLoader("path/to/japanese/documents")
    
    val splitter = ParagraphSplitter()
    
    val chineseDocuments = chineseLoader.load().map { it.copy(metadata = it.metadata + mapOf("language" to "zh")) }
    val englishDocuments = englishLoader.load().map { it.copy(metadata = it.metadata + mapOf("language" to "en")) }
    val japaneseDocuments = japaneseLoader.load().map { it.copy(metadata = it.metadata + mapOf("language" to "ja")) }
    
    val allDocuments = chineseDocuments + englishDocuments + japaneseDocuments
    val chunks = allDocuments.flatMap { document ->
        splitter.split(document)
    }
    
    rag.loadDocuments(chunks)
    
    // 创建搜索函数
    val search = { query: String, language: String? ->
        val options = RagProcessOptions(
            useHybridSearch = true,
            useReranking = true,
            rerankingOptions = RerankingOptions(
                useDiversity = true,
                diversityWeight = 0.3,
                useMetadata = language != null,
                metadataFields = if (language != null) listOf("language") else emptyList(),
                metadataWeights = if (language != null) mapOf("language" to 0.5) else emptyMap()
            )
        )
        
        val results = rag.search(query, limit = 10, options = options)
        
        // 如果指定了语言，过滤结果
        val filteredResults = if (language != null) {
            results.filter { it.document.metadata["language"] == language }
        } else {
            results
        }
        
        filteredResults.take(5)
    }
    
    // 示例查询
    val queries = listOf(
        Pair("人工智能的应用", "zh"),
        Pair("Applications of artificial intelligence", "en"),
        Pair("人工知能の応用", "ja"),
        Pair("机器学习", null) // 不指定语言，搜索所有文档
    )
    
    queries.forEach { (query, language) ->
        println("查询: $query")
        println("语言: ${language ?: "所有语言"}")
        
        val results = search(query, language)
        println("结果数量: ${results.size}")
        
        results.forEach { result ->
            println("${result.document.content.take(100)}...")
            println("语言: ${result.document.metadata["language"]}")
            println("分数: ${result.score}")
            println()
        }
        
        println("---")
    }
}
```

### 案例 3：使用图 RAG 构建知识图谱问答系统

在这个案例中，我们将使用 KastraX RAG 的图 RAG 功能构建一个知识图谱问答系统。

```kotlin
import ai.kastrax.rag.graph.GraphRAG
import ai.kastrax.rag.graph.GraphRAGConfig
import ai.kastrax.rag.graph.GraphRAGQueryOptions
import ai.kastrax.rag.graph.GraphRAGTool
import ai.kastrax.rag.document.JsonDocumentLoader
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = EmbeddingServiceFactory.createFastEmbedKotlinEmbeddingService()
    
    // 创建文档加载器
    val documentLoader = JsonDocumentLoader("path/to/knowledge/graph.json")
    
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
    
    // 创建问答函数
    val answerQuestion = { query: String ->
        val result = graphRAGTool.execute(
            query = query,
            topK = 5,
            randomWalkSteps = 100,
            restartProb = 0.15,
            includeMetadata = true
        )
        
        // 使用 LLM 生成回答
        val llmClient = createLlmClient()
        llmClient.generateText(
            prompt = "你是一个知识图谱专家助手。基于以下上下文回答问题：\n\n上下文：${result.content}\n\n问题：$query\n\n回答："
        )
    }
    
    // 示例问题
    val questions = listOf(
        "人工智能和机器学习的关系是什么？",
        "深度学习的主要应用领域有哪些？",
        "自然语言处理和计算机视觉有什么联系？"
    )
    
    questions.forEach { question ->
        println("问题: $question")
        println("回答: ${answerQuestion(question)}")
        println()
    }
}
```
