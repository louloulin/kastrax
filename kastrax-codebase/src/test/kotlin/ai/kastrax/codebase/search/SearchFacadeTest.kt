package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.embedding.CodeEmbeddingServiceConfig
import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SearchFacadeTest {

    private lateinit var codeIndexer: CodeIndexer
    private lateinit var vectorStore: CodeVectorStore
    private lateinit var embeddingService: CodeEmbeddingService
    private lateinit var searchFacade: SearchFacade

    @BeforeEach
    fun setUp(@TempDir tempDir: Path) {
        // 创建模拟对象
        codeIndexer = mockk()
        vectorStore = mockk()

        val baseEmbeddingService = mockk<EmbeddingService>()
        embeddingService = CodeEmbeddingService(
            baseEmbeddingService = baseEmbeddingService,
            config = CodeEmbeddingServiceConfig(embeddingDimension = 384)
        )

        // 配置模拟对象行为
        val testElements = createTestElements(tempDir)

        coEvery { codeIndexer.getAllElements() } returns testElements
        coEvery { codeIndexer.getElementsByType(any<String>()) } returns testElements.filter { it.type == CodeElementType.CLASS }
        coEvery { codeIndexer.getElementsByType(any<CodeElementType>()) } returns testElements.filter { it.type == CodeElementType.CLASS }
        coEvery { codeIndexer.getElementsByFilePath(any()) } returns testElements
        // 使用自定义函数来模拟 getElementsByFilePath
        every { runBlocking { codeIndexer.getAllElements() } } returns testElements
        every { runBlocking { codeIndexer.getElementsByType(any<String>()) } } returns testElements.filter { it.type == CodeElementType.CLASS }
        every { runBlocking { codeIndexer.getElementsByType(any<CodeElementType>()) } } returns testElements.filter { it.type == CodeElementType.CLASS }

        coEvery { baseEmbeddingService.embed(any()) } returns FloatArray(384) { 0.1f }

        coEvery {
            vectorStore.similaritySearch(
                vector = any(),
                limit = any(),
                minScore = any()
            )
        } returns testElements.map {
            CodeSearchResult(it, 0.8)
        }

        // 创建测试对象
        searchFacade = SearchFacade(
            codeIndexer = codeIndexer,
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            config = SearchFacadeConfig(
                enableCaching = true,
                maxCacheSize = 10,
                enableHighlighting = true,
                enablePagination = true,
                enableFiltering = true,
                enableHistoryTracking = true,
                enableStructureAwareSearch = true
            )
        )
    }

    @Test
    fun testSearch() = runBlocking {
        // 执行搜索
        val request = SearchRequest(
            query = "test",
            paths = emptyList(),
            type = SearchType.VECTOR,
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.5
            )
        )

        val response = searchFacade.search(request)

        // 验证结果
        assertNotNull(response)
        assertTrue(response.results.isNotEmpty())
        assertEquals("test", response.query)
        assertTrue(response.metadata.containsKey("searchTime"))
        assertTrue(response.metadata.containsKey("totalResults"))
    }

    @Test
    fun testSearchByFilePath(@TempDir tempDir: Path) = runBlocking {
        // 执行搜索
        val results = searchFacade.searchByFilePath(tempDir.resolve("TestClass.java"))

        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.location.filePath.contains("Test") })
    }

    @Test
    fun testSearchByType() = runBlocking {
        // 执行搜索
        val results = searchFacade.searchByType(CodeElementType.CLASS)

        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == CodeElementType.CLASS })
    }

    @Test
    fun testSearchByName() = runBlocking {
        // 配置模拟对象行为
        coEvery { codeIndexer.getAllElements() } returns listOf(
            createCodeElement("TestClass", CodeElementType.CLASS),
            createCodeElement("AnotherClass", CodeElementType.CLASS),
            createCodeElement("TestMethod", CodeElementType.METHOD)
        )

        // 执行搜索
        val results = searchFacade.searchByName("Test", exactMatch = false)

        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.name.contains("Test") })
    }

    @Test
    fun testClearCache() {
        // 执行缓存清除
        searchFacade.clearCache()

        // 无法直接验证缓存是否被清除，但至少确保方法不会抛出异常
    }

    // 辅助方法

    private fun createTestElements(tempDir: Path): List<CodeElement> {
        return listOf(
            createCodeElement("TestClass", CodeElementType.CLASS, tempDir.resolve("TestClass.java").toString()),
            createCodeElement("AnotherClass", CodeElementType.CLASS, tempDir.resolve("AnotherClass.java").toString()),
            createCodeElement("TestMethod", CodeElementType.METHOD, tempDir.resolve("TestClass.java").toString()),
            createCodeElement("AnotherMethod", CodeElementType.METHOD, tempDir.resolve("AnotherClass.java").toString())
        )
    }

    private fun createCodeElement(
        name: String,
        type: CodeElementType,
        filePath: String = "test.java"
    ): CodeElement {
        return CodeElement(
            id = "$name-${System.currentTimeMillis()}",
            name = name,
            qualifiedName = "com.example.$name",
            type = type,
            location = Location(
                filePath = filePath,
                startLine = 1,
                endLine = 10,
                startColumn = 1,
                endColumn = 20
            ),
            visibility = Visibility.PUBLIC,
            documentation = "Documentation for $name",
            metadata = mutableMapOf(
                "author" to "Test Author",
                "lastModified" to System.currentTimeMillis()
            )
        )
    }

    @Test
    fun testStructureAwareSearch() = runBlocking {
        // 执行结构感知搜索
        val request = SearchRequest(
            query = "test",
            paths = emptyList(),
            type = SearchType.STRUCTURE,
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.5
            )
        )

        val response = searchFacade.search(request)

        // 验证结果
        assertNotNull(response)
        assertTrue(response.results.isNotEmpty())
        assertEquals(SearchType.STRUCTURE, response.metadata["searchType"])
    }

    @Test
    fun testSearchWithHighlighting() = runBlocking {
        // 创建测试文件
        val testFile = Files.createTempFile("test", ".kt")
        Files.writeString(testFile, """
            package com.example

            /**
             * This is a test class
             */
            class TestClass {
                /**
                 * This is a test method
                 */
                fun testMethod() {
                    // This is a test comment
                    val testVariable = "test value"
                    println(testVariable)
                }
            }
        """.trimIndent())

        // 执行搜索
        val request = SearchRequest(
            query = "test",
            paths = listOf(testFile),
            type = SearchType.TEXT,
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.5
            )
        )

        val response = searchFacade.search(request)

        // 验证结果
        assertNotNull(response)
        assertTrue(response.results.isNotEmpty())
        assertTrue(response.highlightResults.isNotEmpty())
    }

    @Test
    fun testSearchWithPagination() = runBlocking {
        // 执行搜索
        val request = SearchRequest(
            query = "test",
            paths = emptyList(),
            type = SearchType.HYBRID,
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.5,
                "page" to 1,
                "pageSize" to 2
            )
        )

        val response = searchFacade.search(request)

        // 验证结果
        assertNotNull(response)
        assertNotNull(response.pagedResult)
        assertEquals(1, response.pagedResult?.pageNumber)
        assertEquals(2, response.pagedResult?.pageSize)
    }

    @Test
    fun testSearchWithFiltering() = runBlocking {
        // 执行搜索
        val request = SearchRequest(
            query = "test",
            paths = emptyList(),
            type = SearchType.HYBRID,
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.5,
                "types" to setOf(CodeElementType.CLASS),
                "visibilities" to setOf(Visibility.PUBLIC)
            )
        )

        val response = searchFacade.search(request)

        // 验证结果
        assertNotNull(response)
        assertTrue(response.results.isNotEmpty())
        assertTrue(response.results.all { it.element.type == CodeElementType.CLASS })
        assertTrue(response.results.all { it.element.visibility == Visibility.PUBLIC })
    }

    @Test
    fun testSearchHistory() = runBlocking {
        // 执行多次搜索
        for (i in 1..3) {
            val request = SearchRequest(
                query = "test query $i",
                paths = emptyList(),
                type = SearchType.HYBRID
            )

            searchFacade.search(request)
        }

        // 获取搜索历史
        val history = searchFacade.getSearchHistory()

        // 验证结果
        assertNotNull(history)
        assertEquals(3, history.size)
        assertEquals("test query 3", history[0].query)
    }

    @Test
    fun testStreamSearch() = runBlocking {
        // 执行流式搜索
        val request = SearchRequest(
            query = "test",
            paths = emptyList(),
            type = SearchType.HYBRID
        )

        val results = mutableListOf<RetrievalResult>()
        searchFacade.streamSearch(request).collect { result ->
            results.add(result)
        }

        // 验证结果
        assertTrue(results.isNotEmpty())
    }
}
