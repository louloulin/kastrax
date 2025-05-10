package ai.kastrax.rag.examples

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking

/**
 * 简单的 RAG 验证示例，使用模拟对象验证 RAG 的核心功能。
 */
object RagVerificationExample {

    /**
     * 模拟的嵌入服务，用于测试。
     */
    class MockEmbeddingService : EmbeddingService {
        /**
         * 嵌入向量的维度。
         */
        override fun dimension(): Int = 128
        
        /**
         * 嵌入文本。
         *
         * @param text 文本
         * @return 嵌入向量
         */
        override suspend fun embed(text: String): FloatArray {
            // 返回一个固定的嵌入向量
            return FloatArray(dimension()) { 0.1f }
        }
        
        /**
         * 批量嵌入文本。
         *
         * @param texts 文本列表
         * @return 嵌入向量列表
         */
        override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
            // 返回固定的嵌入向量列表
            return texts.map { FloatArray(dimension()) { 0.1f } }
        }
        
        /**
         * 关闭资源。
         */
        override fun close() {
            // 无需关闭资源
        }
    }

    /**
     * 模拟的文档向量存储，用于测试。
     */
    class MockDocumentVectorStore : DocumentVectorStore {
        /**
         * 向量维度。
         */
        override val dimension: Int = 128
        
        /**
         * 文档列表。
         */
        private val documents = listOf(
            Document(
                id = "1",
                content = "Kotlin is a modern programming language that makes developers happier.",
                metadata = mapOf("source" to "kotlin-docs")
            ),
            Document(
                id = "2",
                content = "Kotlin is a cross-platform, statically typed, general-purpose programming language with type inference.",
                metadata = mapOf("source" to "kotlin-docs")
            ),
            Document(
                id = "3",
                content = "Python is an interpreted, high-level, general-purpose programming language.",
                metadata = mapOf("source" to "python-docs")
            )
        )
        
        /**
         * 获取底层向量存储。
         *
         * @return 向量存储
         */
        override fun getVectorStore(): ai.kastrax.store.VectorStore {
            throw UnsupportedOperationException("Not implemented")
        }
        
        /**
         * 添加文档。
         *
         * @param documents 文档列表
         * @param embeddingService 嵌入服务
         * @return 是否成功添加
         */
        override suspend fun addDocuments(
            documents: List<Document>,
            embeddingService: EmbeddingService
        ): Boolean {
            return true
        }
        
        /**
         * 添加文档（已嵌入）。
         *
         * @param documents 文档列表
         * @return 是否成功添加
         */
        override suspend fun addDocuments(
            documents: List<Document>
        ): Boolean {
            return true
        }
        
        /**
         * 删除文档。
         *
         * @param ids 文档 ID 列表
         * @return 是否成功删除
         */
        override suspend fun deleteDocuments(
            ids: List<String>
        ): Boolean {
            return true
        }
        
        /**
         * 相似度搜索。
         *
         * @param query 查询文本
         * @param embeddingService 嵌入服务
         * @param limit 返回结果的最大数量
         * @return 搜索结果列表
         */
        override suspend fun similaritySearch(
            query: String,
            embeddingService: EmbeddingService,
            limit: Int
        ): List<DocumentSearchResult> {
            // 根据查询内容返回不同的结果
            return if (query.contains("kotlin", ignoreCase = true)) {
                listOf(
                    DocumentSearchResult(documents[0], 0.9),
                    DocumentSearchResult(documents[1], 0.8)
                ).take(limit)
            } else if (query.contains("python", ignoreCase = true)) {
                listOf(
                    DocumentSearchResult(documents[2], 0.9)
                ).take(limit)
            } else {
                documents.mapIndexed { index, document ->
                    DocumentSearchResult(document, 0.9 - index * 0.1)
                }.take(limit)
            }
        }
        
        /**
         * 相似度搜索（已嵌入）。
         *
         * @param embedding 查询嵌入
         * @param limit 返回结果的最大数量
         * @return 搜索结果列表
         */
        override suspend fun similaritySearch(
            embedding: FloatArray,
            limit: Int
        ): List<DocumentSearchResult> {
            return documents.mapIndexed { index, document ->
                DocumentSearchResult(document, 0.9 - index * 0.1)
            }.take(limit)
        }
        
        /**
         * 带过滤器的相似度搜索。
         *
         * @param embedding 查询嵌入
         * @param filter 过滤条件
         * @param limit 返回结果的最大数量
         * @return 搜索结果列表
         */
        override suspend fun similaritySearchWithFilter(
            embedding: FloatArray,
            filter: Map<String, Any>,
            limit: Int
        ): List<DocumentSearchResult> {
            return documents.filter { document ->
                filter.all { (key, value) ->
                    document.metadata[key] == value
                }
            }.mapIndexed { index, document ->
                DocumentSearchResult(document, 0.9 - index * 0.1)
            }.take(limit)
        }
        
        /**
         * 关键词搜索。
         *
         * @param keywords 关键词列表
         * @param limit 返回结果的最大数量
         * @return 搜索结果列表
         */
        override suspend fun keywordSearch(
            keywords: List<String>,
            limit: Int
        ): List<DocumentSearchResult> {
            return documents.filter { document ->
                keywords.any { keyword ->
                    document.content.contains(keyword, ignoreCase = true)
                }
            }.mapIndexed { index, document ->
                DocumentSearchResult(document, 0.9 - index * 0.1)
            }.take(limit)
        }
        
        /**
         * 元数据搜索。
         *
         * @param filter 过滤条件
         * @param limit 返回结果的最大数量
         * @return 搜索结果列表
         */
        override suspend fun metadataSearch(
            filter: Map<String, Any>,
            limit: Int
        ): List<DocumentSearchResult> {
            return documents.filter { document ->
                filter.all { (key, value) ->
                    document.metadata[key] == value
                }
            }.mapIndexed { index, document ->
                DocumentSearchResult(document, 0.9 - index * 0.1)
            }.take(limit)
        }
    }

    /**
     * 主函数。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建模拟的嵌入服务
        val embeddingService = MockEmbeddingService()
        
        // 创建模拟的文档向量存储
        val documentStore = MockDocumentVectorStore()
        
        // 创建 RAG 实例
        val rag = RAG(
            documentStore = documentStore,
            embeddingService = embeddingService,
            reranker = IdentityReranker(),
            defaultOptions = RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 1000,
                    separator = "\n\n"
                )
            )
        )
        
        // 测试检索
        val query = "What is Kotlin?"
        println("Query: $query")
        
        val searchResults = rag.search(query, limit = 2)
        println("\nSearch Results:")
        searchResults.forEachIndexed { index, result ->
            println("${index + 1}. ${result.document.content} (Score: ${result.score})")
        }
        
        // 测试生成上下文
        val context = rag.generateContext(query, limit = 2)
        println("\nGenerated Context:")
        println(context)
        
        // 测试检索上下文
        val retrieveResult = rag.retrieveContext(query, limit = 2)
        println("\nRetrieved Context:")
        println(retrieveResult.context)
        println("\nRetrieved Documents:")
        retrieveResult.documents.forEachIndexed { index, document ->
            println("${index + 1}. ${document.content}")
        }
    }
}
