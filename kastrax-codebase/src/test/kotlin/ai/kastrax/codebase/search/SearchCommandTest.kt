package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.search.HighlightInfo
import ai.kastrax.codebase.vector.CodeVectorStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class SearchCommandTest {

    private lateinit var searchCommand: SearchCommand
    private lateinit var searchFacade: SearchFacade
    private lateinit var codeIndexer: CodeIndexer
    private lateinit var vectorStore: CodeVectorStore
    private lateinit var embeddingService: CodeEmbeddingService
    private lateinit var testElements: List<CodeElement>

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建测试文件
        val testFile = Files.createFile(tempDir.resolve("TestClass.kt"))
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

        // 创建测试元素
        testElements = listOf(
            CodeElement(
                id = "1",
                name = "TestClass",
                qualifiedName = "com.example.TestClass",
                type = CodeElementType.CLASS,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = testFile.toString(),
                    startLine = 5,
                    endLine = 14,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "class TestClass { ... }",
                documentation = "This is a test class",
                metadata = mutableMapOf()
            ),
            CodeElement(
                id = "2",
                name = "testMethod",
                qualifiedName = "com.example.TestClass.testMethod",
                type = CodeElementType.METHOD,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = testFile.toString(),
                    startLine = 9,
                    endLine = 13,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "fun testMethod() { ... }",
                documentation = "This is a test method",
                metadata = mutableMapOf()
            )
        )

        // 创建模拟对象
        codeIndexer = mockk()
        vectorStore = mockk()
        embeddingService = mockk()

        // 配置模拟对象
        coEvery { codeIndexer.getElementsByType(any<String>()) } returns testElements.filter { it.type == CodeElementType.CLASS }
        coEvery { codeIndexer.getElementsByType(any<CodeElementType>()) } returns testElements.filter { it.type == CodeElementType.CLASS }
        coEvery { codeIndexer.getElementsByFilePath(any()) } returns testElements
        every { runBlocking { codeIndexer.getAllElements() } } returns testElements

        // 创建搜索门面
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

        // 创建搜索命令
        searchCommand = SearchCommand(
            searchFacade = searchFacade,
            config = SearchCommandConfig(
                defaultSearchType = SearchType.TEXT,
                defaultLimit = 10,
                defaultMinScore = 0.5,
                contextLines = 2,
                formatAsMarkdown = true,
                includeHighlights = true,
                includePagination = false
            )
        )

        // 配置搜索门面的模拟行为
        coEvery {
            searchFacade.search(any())
        } returns SearchResponse(
            query = "test",
            results = listOf(
                RetrievalResult(
                    element = testElements[0],
                    score = 0.9,
                    explanation = "Test result"
                ),
                RetrievalResult(
                    element = testElements[1],
                    score = 0.8,
                    explanation = "Test result"
                )
            ),
            metadata = mapOf(
                "searchTime" to 10L,
                "totalResults" to 2,
                "searchType" to SearchType.TEXT
            ),
            highlightResults = listOf(
                HighlightResult(
                    element = testElements[0],
                    score = 0.9,
                    highlights = listOf(
                        HighlightInfo(
                            lineNumber = 5,
                            columnStart = 6,
                            columnEnd = 15,
                            lineContent = "class TestClass {",
                            beforeContext = listOf(),
                            afterContext = listOf()
                        )
                    )
                ),
                HighlightResult(
                    element = testElements[1],
                    score = 0.8,
                    highlights = listOf(
                        HighlightInfo(
                            lineNumber = 9,
                            columnStart = 4,
                            columnEnd = 14,
                            lineContent = "fun testMethod() {",
                            beforeContext = listOf(),
                            afterContext = listOf()
                        )
                    )
                )
            ),
            executionTimeMs = 10L,
            timestamp = System.currentTimeMillis()
        )
    }

    @Test
    fun testExecute() = runBlocking {
        // 执行搜索命令
        val result = searchCommand.execute(
            query = "test",
            searchType = SearchType.TEXT,
            basePath = tempDir.toString()
        )

        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("搜索结果: test"))
        assertTrue(result.contains("TestClass"))
        assertTrue(result.contains("testMethod"))
    }

    @Test
    fun testExecuteWithOptions() = runBlocking {
        // 执行搜索命令
        val result = searchCommand.execute(
            query = "test",
            searchType = SearchType.HYBRID,
            basePath = tempDir.toString(),
            options = mapOf(
                "limit" to 5,
                "minScore" to 0.7,
                "contextLines" to 3
            )
        )

        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("搜索结果: test"))
        assertTrue(result.contains("TestClass"))
        assertTrue(result.contains("testMethod"))
    }

    @Test
    fun testExecuteWithPlainTextFormat() = runBlocking {
        // 创建纯文本格式的搜索命令
        val plainTextSearchCommand = SearchCommand(
            searchFacade = searchFacade,
            config = SearchCommandConfig(
                formatAsMarkdown = false
            )
        )

        // 执行搜索命令
        val result = plainTextSearchCommand.execute(
            query = "test",
            searchType = SearchType.TEXT,
            basePath = tempDir.toString()
        )

        // 验证结果
        assertNotNull(result)
        assertTrue(result.contains("搜索结果: test"))
        assertTrue(result.contains("TestClass"))
        assertTrue(result.contains("testMethod"))
        assertFalse(result.contains("```")) // 不应该包含 Markdown 格式
    }
}
