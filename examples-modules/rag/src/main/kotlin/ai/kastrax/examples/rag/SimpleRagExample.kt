package ai.kastrax.examples.rag

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
 * 简单的 RAG 示例
 */
fun main() = runBlocking {
    println("开始 RAG 示例...")
    
    // 创建嵌入服务
    val embeddingService = object : EmbeddingService {
        override fun dimension(): Int = 384
        
        override suspend fun embed(text: String): FloatArray {
            // 使用文本的哈希码作为随机数生成器的种子，以确保相同的文本生成相同的嵌入
            val textSeed = text.hashCode().toLong()
            val textRandom = java.util.Random(textSeed)

            // 生成随机向量
            val vector = FloatArray(dimension()) {
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
    
    println("Hello, Kastrax RAG!")
    println("这是一个简单的RAG示例，用于测试编译和运行。")
}
