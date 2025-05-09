# KastraX RAG 扩展指南

本指南提供了如何扩展 KastraX RAG 模块的详细说明。

## 扩展文档加载器

KastraX RAG 提供了 `DocumentLoader` 接口，您可以实现这个接口来创建自定义的文档加载器。

### 实现 DocumentLoader 接口

```kotlin
import ai.kastrax.store.document.Document

class CustomDocumentLoader : DocumentLoader {
    override suspend fun load(): List<Document> {
        // 实现自定义的文档加载逻辑
        val documents = mutableListOf<Document>()
        
        // 加载文档...
        
        return documents
    }
}
```

### 示例：数据库文档加载器

```kotlin
import ai.kastrax.store.document.Document
import java.sql.Connection
import java.sql.DriverManager

class DatabaseDocumentLoader(
    private val url: String,
    private val username: String,
    private val password: String,
    private val query: String
) : DocumentLoader {
    override suspend fun load(): List<Document> {
        val documents = mutableListOf<Document>()
        
        DriverManager.getConnection(url, username, password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(query).use { resultSet ->
                    while (resultSet.next()) {
                        val id = resultSet.getString("id")
                        val content = resultSet.getString("content")
                        val metadata = mutableMapOf<String, Any>()
                        
                        // 获取元数据...
                        
                        documents.add(
                            Document(
                                id = id,
                                content = content,
                                metadata = metadata
                            )
                        )
                    }
                }
            }
        }
        
        return documents
    }
}
```

## 扩展文档分割器

KastraX RAG 提供了 `DocumentSplitter` 接口，您可以实现这个接口来创建自定义的文档分割器。

### 实现 DocumentSplitter 接口

```kotlin
import ai.kastrax.store.document.Document

class CustomDocumentSplitter : DocumentSplitter {
    override fun split(document: Document): List<Document> {
        // 实现自定义的文档分割逻辑
        val chunks = mutableListOf<Document>()
        
        // 分割文档...
        
        return chunks
    }
}
```

### 示例：基于标题的分割器

```kotlin
import ai.kastrax.store.document.Document

class HeadingDocumentSplitter : DocumentSplitter {
    override fun split(document: Document): List<Document> {
        val chunks = mutableListOf<Document>()
        val content = document.content
        
        // 使用正则表达式匹配标题
        val headingPattern = Regex("#+\\s+(.+)")
        val headings = headingPattern.findAll(content)
        
        // 如果没有找到标题，返回原始文档
        if (!headings.any()) {
            return listOf(document)
        }
        
        // 分割文档
        val headingIndices = headings.map { it.range.first }.toList()
        
        for (i in headingIndices.indices) {
            val start = headingIndices[i]
            val end = if (i < headingIndices.size - 1) headingIndices[i + 1] else content.length
            
            val chunkContent = content.substring(start, end).trim()
            
            chunks.add(
                Document(
                    id = "${document.id}-${i + 1}",
                    content = chunkContent,
                    metadata = document.metadata + mapOf("chunk_index" to i + 1)
                )
            )
        }
        
        return chunks
    }
}
```

## 扩展嵌入服务

KastraX RAG 提供了 `EmbeddingService` 接口，您可以实现这个接口来创建自定义的嵌入服务。

### 实现 EmbeddingService 接口

```kotlin
class CustomEmbeddingService : EmbeddingService {
    override suspend fun embed(text: String): FloatArray {
        // 实现自定义的嵌入逻辑
        val embedding = FloatArray(dimension())
        
        // 生成嵌入向量...
        
        return embedding
    }
    
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        // 实现自定义的批量嵌入逻辑
        return texts.map { embed(it) }
    }
    
    override fun dimension(): Int {
        // 返回嵌入向量的维度
        return 1536
    }
}
```

### 示例：HuggingFace 嵌入服务

```kotlin
import ai.kastrax.rag.embedding.EmbeddingService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

class HuggingFaceEmbeddingService(
    private val apiKey: String,
    private val model: String = "sentence-transformers/all-MiniLM-L6-v2",
    private val dimensions: Int = 384
) : EmbeddingService {
    private val client = HttpClient()
    private val baseUrl = "https://api-inference.huggingface.co/models/$model"
    
    override suspend fun embed(text: String): FloatArray {
        val response = client.post(baseUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(Json.encodeToString(JsonObject(mapOf("inputs" to JsonPrimitive(text)))))
        }
        
        val responseBody = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
        
        return FloatArray(dimensions) { i ->
            jsonArray[0].jsonArray[i].jsonPrimitive.float
        }
    }
    
    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        val response = client.post(baseUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $apiKey")
                append(HttpHeaders.ContentType, "application/json")
            }
            setBody(Json.encodeToString(JsonObject(mapOf("inputs" to JsonArray(texts.map { JsonPrimitive(it) })))))
        }
        
        val responseBody = response.bodyAsText()
        val jsonArray = Json.parseToJsonElement(responseBody).jsonArray
        
        return jsonArray.map { embeddingArray ->
            FloatArray(dimensions) { i ->
                embeddingArray.jsonArray[i].jsonPrimitive.float
            }
        }
    }
    
    override fun dimension(): Int {
        return dimensions
    }
}
```

## 扩展向量存储

KastraX RAG 提供了 `VectorStore` 接口，您可以实现这个接口来创建自定义的向量存储。

### 实现 VectorStore 接口

```kotlin
import ai.kastrax.store.vector.VectorStore
import ai.kastrax.store.vector.VectorSearchResult

class CustomVectorStore : VectorStore {
    override suspend fun addVector(id: String, vector: FloatArray, metadata: Map<String, Any>): Boolean {
        // 实现自定义的向量添加逻辑
        return true
    }
    
    override suspend fun addVectors(vectors: List<Triple<String, FloatArray, Map<String, Any>>>): Boolean {
        // 实现自定义的批量向量添加逻辑
        return true
    }
    
    override suspend fun searchVector(vector: FloatArray, limit: Int, minScore: Double): List<VectorSearchResult> {
        // 实现自定义的向量搜索逻辑
        val results = mutableListOf<VectorSearchResult>()
        
        // 搜索向量...
        
        return results
    }
    
    override suspend fun deleteVector(id: String): Boolean {
        // 实现自定义的向量删除逻辑
        return true
    }
    
    override suspend fun clear(): Boolean {
        // 实现自定义的清空逻辑
        return true
    }
}
```

### 示例：Redis 向量存储

```kotlin
import ai.kastrax.store.vector.VectorStore
import ai.kastrax.store.vector.VectorSearchResult
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.ScanParams
import kotlinx.serialization.json.*

class RedisVectorStore(
    private val host: String = "localhost",
    private val port: Int = 6379,
    private val password: String? = null,
    private val prefix: String = "vector:"
) : VectorStore {
    private val pool = JedisPool(JedisPoolConfig(), host, port, 2000, password)
    
    override suspend fun addVector(id: String, vector: FloatArray, metadata: Map<String, Any>): Boolean {
        pool.resource.use { jedis ->
            // 存储向量
            jedis.set("$prefix$id:vector", vector.joinToString(","))
            
            // 存储元数据
            val metadataJson = Json.encodeToString(JsonObject(metadata.mapValues { JsonPrimitive(it.value.toString()) }))
            jedis.set("$prefix$id:metadata", metadataJson)
            
            return true
        }
    }
    
    override suspend fun addVectors(vectors: List<Triple<String, FloatArray, Map<String, Any>>>): Boolean {
        pool.resource.use { jedis ->
            jedis.multi().use { transaction ->
                for ((id, vector, metadata) in vectors) {
                    // 存储向量
                    transaction.set("$prefix$id:vector", vector.joinToString(","))
                    
                    // 存储元数据
                    val metadataJson = Json.encodeToString(JsonObject(metadata.mapValues { JsonPrimitive(it.value.toString()) }))
                    transaction.set("$prefix$id:metadata", metadataJson)
                }
                
                transaction.exec()
            }
            
            return true
        }
    }
    
    override suspend fun searchVector(vector: FloatArray, limit: Int, minScore: Double): List<VectorSearchResult> {
        pool.resource.use { jedis ->
            val results = mutableListOf<VectorSearchResult>()
            
            // 获取所有向量
            val scanParams = ScanParams().match("$prefix*:vector")
            var cursor = "0"
            
            do {
                val scanResult = jedis.scan(cursor, scanParams)
                cursor = scanResult.cursor
                
                for (key in scanResult.result) {
                    val id = key.removePrefix("$prefix").removeSuffix(":vector")
                    val storedVectorStr = jedis.get(key)
                    
                    if (storedVectorStr != null) {
                        val storedVector = storedVectorStr.split(",").map { it.toFloat() }.toFloatArray()
                        
                        // 计算余弦相似度
                        val similarity = cosineSimilarity(vector, storedVector)
                        
                        if (similarity >= minScore) {
                            // 获取元数据
                            val metadataJson = jedis.get("$prefix$id:metadata")
                            val metadata = if (metadataJson != null) {
                                val jsonObject = Json.parseToJsonElement(metadataJson).jsonObject
                                jsonObject.mapValues { it.value.jsonPrimitive.content }
                            } else {
                                emptyMap()
                            }
                            
                            results.add(VectorSearchResult(id, similarity, metadata))
                        }
                    }
                }
            } while (cursor != "0")
            
            // 按相似度排序并限制结果数量
            return results.sortedByDescending { it.score }.take(limit)
        }
    }
    
    override suspend fun deleteVector(id: String): Boolean {
        pool.resource.use { jedis ->
            jedis.del("$prefix$id:vector", "$prefix$id:metadata")
            return true
        }
    }
    
    override suspend fun clear(): Boolean {
        pool.resource.use { jedis ->
            val scanParams = ScanParams().match("$prefix*")
            var cursor = "0"
            
            do {
                val scanResult = jedis.scan(cursor, scanParams)
                cursor = scanResult.cursor
                
                if (scanResult.result.isNotEmpty()) {
                    jedis.del(*scanResult.result.toTypedArray())
                }
            } while (cursor != "0")
            
            return true
        }
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))
    }
}
```

## 扩展检索器

KastraX RAG 提供了 `Retriever` 接口，您可以实现这个接口来创建自定义的检索器。

### 实现 Retriever 接口

```kotlin
import ai.kastrax.store.document.DocumentSearchResult

class CustomRetriever : Retriever {
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<DocumentSearchResult> {
        // 实现自定义的检索逻辑
        val results = mutableListOf<DocumentSearchResult>()
        
        // 检索文档...
        
        return results
    }
}
```

### 示例：基于 BM25 的检索器

```kotlin
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.rag.retrieval.Retriever
import java.util.HashMap

class BM25Retriever(
    private val documents: List<Document>,
    private val k1: Double = 1.5,
    private val b: Double = 0.75
) : Retriever {
    private val documentTermFrequencies = mutableMapOf<String, Map<String, Int>>()
    private val documentLengths = mutableMapOf<String, Int>()
    private val termDocumentFrequencies = mutableMapOf<String, Int>()
    private val averageDocumentLength: Double
    
    init {
        // 预处理文档
        for (document in documents) {
            val terms = tokenize(document.content)
            val termFrequencies = terms.groupingBy { it }.eachCount()
            
            documentTermFrequencies[document.id] = termFrequencies
            documentLengths[document.id] = terms.size
            
            for (term in termFrequencies.keys) {
                termDocumentFrequencies[term] = termDocumentFrequencies.getOrDefault(term, 0) + 1
            }
        }
        
        averageDocumentLength = documentLengths.values.average()
    }
    
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<DocumentSearchResult> {
        val queryTerms = tokenize(query)
        val scores = mutableMapOf<String, Double>()
        
        for (document in documents) {
            val docId = document.id
            val docLength = documentLengths[docId] ?: continue
            val termFrequencies = documentTermFrequencies[docId] ?: continue
            
            var score = 0.0
            
            for (term in queryTerms) {
                val tf = termFrequencies[term] ?: 0
                val df = termDocumentFrequencies[term] ?: 0
                
                if (tf > 0 && df > 0) {
                    val idf = Math.log(1.0 + (documents.size - df + 0.5) / (df + 0.5))
                    val numerator = tf * (k1 + 1)
                    val denominator = tf + k1 * (1 - b + b * docLength / averageDocumentLength)
                    
                    score += idf * numerator / denominator
                }
            }
            
            if (score >= minScore) {
                scores[docId] = score
            }
        }
        
        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (docId, score) ->
                val document = documents.first { it.id == docId }
                DocumentSearchResult(document, score)
            }
    }
    
    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace("[^\\p{L}\\p{N}\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }
}
```

## 扩展重排序器

KastraX RAG 提供了 `Reranker` 接口，您可以实现这个接口来创建自定义的重排序器。

### 实现 Reranker 接口

```kotlin
import ai.kastrax.store.document.DocumentSearchResult

class CustomReranker : Reranker {
    override suspend fun rerank(query: String, results: List<DocumentSearchResult>): List<DocumentSearchResult> {
        // 实现自定义的重排序逻辑
        val rerankedResults = mutableListOf<DocumentSearchResult>()
        
        // 重排序结果...
        
        return rerankedResults
    }
}
```

### 示例：基于元数据的重排序器

```kotlin
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.rag.reranker.Reranker

class MetadataReranker(
    private val fieldWeights: Map<String, Double>
) : Reranker {
    override suspend fun rerank(query: String, results: List<DocumentSearchResult>): List<DocumentSearchResult> {
        return results.map { result ->
            val document = result.document
            val originalScore = result.score
            
            // 计算元数据分数
            var metadataScore = 0.0
            
            for ((field, weight) in fieldWeights) {
                val value = document.metadata[field]?.toString()
                
                if (value != null && query.contains(value, ignoreCase = true)) {
                    metadataScore += weight
                }
            }
            
            // 组合原始分数和元数据分数
            val combinedScore = originalScore + metadataScore
            
            DocumentSearchResult(document, combinedScore)
        }.sortedByDescending { it.score }
    }
}
```

## 扩展上下文构建器

KastraX RAG 提供了 `ContextBuilder` 类，您可以扩展这个类来创建自定义的上下文构建器。

### 扩展 ContextBuilder 类

```kotlin
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig

class CustomContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig()
) : ContextBuilder(config) {
    override fun buildContext(query: String, results: List<DocumentSearchResult>): String {
        // 实现自定义的上下文构建逻辑
        val contextBuilder = StringBuilder()
        
        // 构建上下文...
        
        return contextBuilder.toString()
    }
}
```

### 示例：基于模板的上下文构建器

```kotlin
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.rag.context.ContextBuilder
import ai.kastrax.rag.context.ContextBuilderConfig

class TemplateContextBuilder(
    private val config: ContextBuilderConfig = ContextBuilderConfig(),
    private val template: String = "{{query}}\n\n{{context}}\n\n{{sources}}"
) : ContextBuilder(config) {
    override fun buildContext(query: String, results: List<DocumentSearchResult>): String {
        // 构建上下文
        val contextBuilder = StringBuilder()
        
        for ((index, result) in results.withIndex()) {
            val document = result.document
            
            contextBuilder.append("${index + 1}. ${document.content}")
            contextBuilder.append(config.separator)
        }
        
        // 构建来源
        val sourcesBuilder = StringBuilder()
        
        if (config.includeMetadata) {
            for ((index, result) in results.withIndex()) {
                val document = result.document
                
                sourcesBuilder.append("${index + 1}. ")
                
                if (config.metadataFields.isEmpty()) {
                    // 包含所有元数据
                    sourcesBuilder.append(document.metadata.entries.joinToString(", ") { "${it.key}: ${it.value}" })
                } else {
                    // 仅包含指定的元数据字段
                    sourcesBuilder.append(config.metadataFields.mapNotNull { field ->
                        document.metadata[field]?.let { "$field: $it" }
                    }.joinToString(", "))
                }
                
                sourcesBuilder.append("\n")
            }
        }
        
        // 应用模板
        return template
            .replace("{{query}}", query)
            .replace("{{context}}", contextBuilder.toString())
            .replace("{{sources}}", sourcesBuilder.toString())
    }
}
```

## 创建自定义 RAG 系统

您可以组合上述扩展来创建自定义的 RAG 系统。

### 示例：创建自定义 RAG 系统

```kotlin
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.DocumentSplitter
import ai.kastrax.rag.document.ParagraphSplitter
import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.embedding.EmbeddingServiceFactory
import ai.kastrax.rag.reranker.Reranker
import ai.kastrax.rag.store.DocumentVectorStoreAdapter
import ai.kastrax.rag.store.VectorStoreFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建自定义组件
    val documentLoader = DirectoryDocumentLoader("path/to/documents")
    val documentSplitter = ParagraphSplitter()
    val embeddingService = EmbeddingServiceFactory.createFastEmbedKotlinEmbeddingService()
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()
    val documentStore = DocumentVectorStoreAdapter(vectorStore)
    val reranker = MetadataReranker(mapOf("source" to 0.5, "category" to 0.3))
    
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
    
    // 加载和处理文档
    val documents = documentLoader.load()
    val chunks = documents.flatMap { document ->
        documentSplitter.split(document)
    }
    
    rag.loadDocuments(chunks)
    
    // 使用 RAG 系统
    val query = "人工智能的应用"
    val results = rag.search(query, limit = 5)
    val context = rag.generateContext(query, limit = 5)
    
    println("搜索结果:")
    results.forEach { result ->
        println("${result.document.content} (分数: ${result.score})")
    }
    
    println("\n生成的上下文:")
    println(context)
}
```

## 最佳实践

### 性能优化

- 使用缓存嵌入服务缓存嵌入向量
- 使用本地嵌入服务减少 API 调用
- 使用并行处理提高性能
- 使用向量索引加速检索

### 代码组织

- 将自定义组件放在单独的包中
- 使用依赖注入管理组件依赖
- 使用工厂模式创建组件
- 使用接口定义组件契约

### 错误处理

- 使用异常处理捕获和处理错误
- 使用日志记录错误信息
- 使用重试机制处理临时错误
- 使用降级策略处理服务不可用的情况

### 测试

- 为自定义组件编写单元测试
- 使用模拟对象测试组件交互
- 编写集成测试验证系统行为
- 使用性能测试评估系统性能
