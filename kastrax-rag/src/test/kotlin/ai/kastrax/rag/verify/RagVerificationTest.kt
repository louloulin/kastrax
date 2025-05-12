package ai.kastrax.rag.verify

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

/**
 * RAG 验证测试，使用模拟对象验证 RAG 的核心功能。
 */
class RagVerificationTest {

    /**
     * 测试 RAG 搜索功能。
     */
    @Test
    fun testRagSearch() = runBlocking {
        // 创建测试文档
        val documents = listOf(
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

        // 创建模拟的嵌入服务
        val embeddingService = mock(EmbeddingService::class.java)
        `when`(embeddingService.embed(any())).thenReturn(FloatArray(128) { 0.1f })
        `when`(embeddingService.embedBatch(any<List<String>>())).thenReturn(List(3) { FloatArray(128) { 0.1f } })
        `when`(embeddingService.dimension).thenReturn(128)

        // 创建模拟的文档向量存储
        val documentStore = mock(DocumentVectorStore::class.java)
        `when`(documentStore.dimension).thenReturn(128)
        `when`(documentStore.similaritySearch(any(), any<EmbeddingService>(), any()))
            .thenReturn(listOf(
                DocumentSearchResult(documents[0], 0.9),
                DocumentSearchResult(documents[1], 0.8)
            ))

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
        assertTrue(searchResults.all { it.document.id in listOf("1", "2") }, "Should return documents about Kotlin")
    }

    /**
     * 测试 RAG 生成上下文功能。
     */
    @Test
    fun testRagGenerateContext() = runBlocking {
        // 创建测试文档
        val documents = listOf(
            Document(
                id = "1",
                content = "Kotlin is a modern programming language that makes developers happier.",
                metadata = mapOf("source" to "kotlin-docs")
            ),
            Document(
                id = "2",
                content = "Kotlin is a cross-platform, statically typed, general-purpose programming language with type inference.",
                metadata = mapOf("source" to "kotlin-docs")
            )
        )

        // 创建模拟的嵌入服务
        val embeddingService = mock(EmbeddingService::class.java)
        `when`(embeddingService.embed(any())).thenReturn(FloatArray(128) { 0.1f })
        `when`(embeddingService.dimension).thenReturn(128)

        // 创建模拟的文档向量存储
        val documentStore = mock(DocumentVectorStore::class.java)
        `when`(documentStore.dimension).thenReturn(128)
        `when`(documentStore.similaritySearch(any(), any<EmbeddingService>(), any()))
            .thenReturn(listOf(
                DocumentSearchResult(documents[0], 0.9),
                DocumentSearchResult(documents[1], 0.8)
            ))

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
}
