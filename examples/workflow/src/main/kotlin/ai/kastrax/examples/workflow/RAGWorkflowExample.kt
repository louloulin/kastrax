package ai.kastrax.examples.workflow

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.context.ContextFormat
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.vector.memory.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 简单的 RAG 工作流示例
 */
fun main() = runBlocking {
    println("开始 RAG 工作流示例...")
    
    // 创建嵌入服务
    val embeddingService = object : EmbeddingService() {
        override val dimension: Int = 384
        
        override suspend fun embed(text: String): FloatArray {
            // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
            val textSeed = text.hashCode().toLong()
            val textRandom = java.util.Random(textSeed)

            // 生成随机向量
            val vector = FloatArray(dimension) {
                // 生成 [-1, 1] 范围内的随机浮点数
                textRandom.nextFloat() * 2 - 1
            }

            // 归一化向量
            val norm = kotlin.math.sqrt(vector.sumOf { it * it.toDouble() })
            if (norm > 0) {
                for (i in vector.indices) {
                    vector[i] = (vector[i] / norm).toFloat()
                }
            }

            return vector
        }
        
        override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
            return texts.map { embed(it) }
        }
        
        override fun close() {}
    }
    
    // 创建向量存储
    val vectorStore = InMemoryVectorStore(dimension = 384)
    
    // 创建文档向量存储
    val documentStore = object : DocumentVectorStore {
        override val dimension: Int = vectorStore.dimension
        
        override fun getVectorStore() = vectorStore
        
        override suspend fun addDocuments(documents: List<Document>, embeddingService: EmbeddingService): Boolean {
            val embeddings = embeddingService.embedBatch(documents.map { it.content })
            val ids = documents.map { it.id }
            val metadataList = documents.map { it.metadata }
            
            // 使用 VectorStore 的 upsert 方法添加向量
            val indexName = "default"
            vectorStore.createIndex(indexName, dimension, ai.kastrax.store.SimilarityMetric.COSINE)
            vectorStore.upsert(indexName, embeddings, metadataList, ids)
            return true
        }
        
        override suspend fun addDocuments(documents: List<Document>): Boolean {
            return true // 简化实现
        }
        
        override suspend fun deleteDocuments(ids: List<String>): Boolean {
            // 使用 VectorStore 的 deleteVectors 方法删除向量
            val indexName = "default"
            return vectorStore.deleteVectors(indexName, ids)
        }
        
        override suspend fun similaritySearch(query: String, embeddingService: EmbeddingService, limit: Int): List<DocumentSearchResult> {
            val embedding = embeddingService.embed(query)
            // 使用 VectorStore 的 query 方法查询向量
            val indexName = "default"
            val results = vectorStore.query(indexName, embedding, limit, null, false)
            return results.map { result ->
                val metadata = result.metadata ?: emptyMap()
                val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                DocumentSearchResult(document, result.score)
            }
        }
        
        override suspend fun similaritySearch(embedding: FloatArray, limit: Int): List<DocumentSearchResult> {
            // 使用 VectorStore 的 query 方法查询向量
            val indexName = "default"
            val results = vectorStore.query(indexName, embedding, limit, null, false)
            return results.map { result ->
                val metadata = result.metadata ?: emptyMap()
                val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                DocumentSearchResult(document, result.score)
            }
        }
        
        override suspend fun similaritySearchWithFilter(embedding: FloatArray, filter: Map<String, Any>, limit: Int): List<DocumentSearchResult> {
            // 使用 VectorStore 的 query 方法查询向量
            val indexName = "default"
            val results = vectorStore.query(indexName, embedding, limit, filter, false)
            return results.map { result ->
                val metadata = result.metadata ?: emptyMap()
                val document = Document(id = result.id, content = "Content for ${result.id}", metadata = metadata)
                DocumentSearchResult(document, result.score)
            }
        }
        
        override suspend fun keywordSearch(keywords: List<String>, limit: Int): List<DocumentSearchResult> {
            return emptyList() // 简化实现
        }
        
        override suspend fun metadataSearch(filter: Map<String, Any>, limit: Int): List<DocumentSearchResult> {
            return emptyList() // 简化实现
        }
    }
    
    // 创建示例文档
    val documents = listOf(
        Document(
            id = UUID.randomUUID().toString(),
            content = "Kotlin 是一种在 Java 虚拟机上运行的静态类型编程语言，由 JetBrains 开发。",
            metadata = mapOf("source" to "wiki", "category" to "programming")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "Kotlin 可以编译成 Java 字节码，也可以编译成 JavaScript，方便在没有 JVM 的设备上运行。",
            metadata = mapOf("source" to "wiki", "category" to "programming")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "人工智能是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。",
            metadata = mapOf("source" to "wiki", "category" to "ai")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "机器学习是人工智能的一个分支，它使用各种统计技术，使计算机系统能够'学习'（例如，逐步提高特定任务的性能）而无需明确编程。",
            metadata = mapOf("source" to "wiki", "category" to "ai")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "深度学习是机器学习的一个分支，它基于人工神经网络的结构和功能，尤其是卷积神经网络。",
            metadata = mapOf("source" to "wiki", "category" to "ai")
        )
    )
    
    // 添加文档到向量存储
    documentStore.addDocuments(documents, embeddingService)
    println("已添加 ${documents.size} 个文档到向量存储")
    
    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = IdentityReranker()
    )
    
    // 模拟工作流步骤 1：查询 Kotlin 相关信息
    println("\n工作流步骤 1：查询 Kotlin 相关信息")
    val query1 = "什么是 Kotlin？"
    println("查询: $query1")
    
    val result1 = rag.retrieveContext(
        query = query1,
        limit = 3,
        options = RagProcessOptions(
            contextOptions = ContextBuilderConfig(
                maxTokens = 1000,
                includeMetadata = true,
                format = ContextFormat.TEXT,
                separator = "\n\n"
            )
        )
    )
    
    println("查询结果:")
    println("上下文: ${result1.context}")
    println("文档 ID: ${result1.documents.map { it.id }}")
    
    // 模拟工作流步骤 2：查询人工智能相关信息
    println("\n工作流步骤 2：查询人工智能相关信息")
    val query2 = "什么是人工智能？"
    println("查询: $query2")
    
    val result2 = rag.retrieveContext(
        query = query2,
        limit = 3,
        options = RagProcessOptions(
            contextOptions = ContextBuilderConfig(
                maxTokens = 1000,
                includeMetadata = true,
                format = ContextFormat.TEXT,
                separator = "\n\n"
            )
        )
    )
    
    println("查询结果:")
    println("上下文: ${result2.context}")
    println("文档 ID: ${result2.documents.map { it.id }}")
    
    // 模拟工作流步骤 3：生成综合报告
    println("\n工作流步骤 3：生成综合报告")
    println("基于检索到的信息生成综合报告:")
    println("=== Kotlin 和人工智能技术报告 ===")
    println("1. Kotlin 概述:")
    println("   - ${result1.context}")
    println("2. 人工智能概述:")
    println("   - ${result2.context}")
    println("3. 结论:")
    println("   - Kotlin 是一种现代编程语言，可以用于开发各种应用，包括人工智能应用。")
    println("   - 人工智能技术正在快速发展，包括机器学习和深度学习等分支。")
    println("   - 使用 Kotlin 开发人工智能应用是一个有前景的方向。")
    println("=== 报告结束 ===")
}
