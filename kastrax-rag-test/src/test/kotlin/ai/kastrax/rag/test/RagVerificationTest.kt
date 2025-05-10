package ai.kastrax.rag.test

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.test.RagVerificationApp.MockDocumentVectorStore
import ai.kastrax.rag.test.RagVerificationApp.MockEmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * RAG 验证测试，使用模拟对象验证 RAG 的核心功能。
 */
class RagVerificationTest {

    /**
     * 测试 RAG 搜索功能。
     */
    @Test
    fun testRagSearch() = runBlocking {
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
        val searchResults = rag.search(query, limit = 2)
        
        // 验证检索结果
        assertEquals(2, searchResults.size, "Should return 2 search results")
        assertTrue(searchResults.all { it.document.content.contains("Kotlin") }, "Should return documents about Kotlin")
    }
    
    /**
     * 测试 RAG 生成上下文功能。
     */
    @Test
    fun testRagGenerateContext() = runBlocking {
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
        
        // 测试生成上下文
        val query = "What is Kotlin?"
        val context = rag.generateContext(query, limit = 2)
        
        // 验证上下文
        assertTrue(context.contains("Kotlin"), "Context should contain information about Kotlin")
        assertTrue(context.contains("modern programming language"), "Context should contain specific information from documents")
        assertTrue(context.contains("cross-platform"), "Context should contain specific information from documents")
    }
    
    /**
     * 测试 RAG 检索上下文功能。
     */
    @Test
    fun testRagRetrieveContext() = runBlocking {
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
        
        // 测试检索上下文
        val query = "What is Kotlin?"
        val retrieveResult = rag.retrieveContext(query, limit = 2)
        
        // 验证检索结果
        assertEquals(2, retrieveResult.documents.size, "Should return 2 documents")
        assertTrue(retrieveResult.context.contains("Kotlin"), "Context should contain information about Kotlin")
        assertTrue(retrieveResult.documents.all { it.content.contains("Kotlin") }, "Should return documents about Kotlin")
    }
}
