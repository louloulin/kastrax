package ai.kastrax.codebase.search

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.embedding.CodeEmbeddingServiceConfig
import ai.kastrax.codebase.indexing.CodeIndexer
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
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeSearchServiceTest {
    
    private lateinit var codeIndexer: CodeIndexer
    private lateinit var vectorStore: CodeVectorStore
    private lateinit var embeddingService: CodeEmbeddingService
    private lateinit var searchService: CodeSearchService
    
    @BeforeEach
    fun setUp(@TempDir tempDir: Path) {
        // 创建模拟对象
        codeIndexer = mockk()
        vectorStore = mockk()
        
        val baseEmbeddingService = mockk<EmbeddingService>()
        embeddingService = CodeEmbeddingService(
            baseEmbeddingService = baseEmbeddingService,
            config = CodeEmbeddingServiceConfig(dimension = 384)
        )
        
        // 配置模拟对象行为
        val testElements = createTestElements(tempDir)
        
        every { codeIndexer.getAllElements() } returns testElements
        every { codeIndexer.getElementsByType(any()) } returns testElements.filter { it.type == CodeElementType.CLASS }
        every { codeIndexer.getElementsByFilePath(any()) } returns testElements.filter { it.location.filePath.contains("Test") }
        
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
        searchService = CodeSearchService(
            codeIndexer = codeIndexer,
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            config = CodeSearchServiceConfig(
                defaultLimit = 10,
                defaultMinScore = 0.5,
                vectorWeight = 0.7,
                keywordWeight = 0.3,
                enableHybridSearch = true,
                enableFuzzySearch = true,
                enableTypeFiltering = true
            )
        )
    }
    
    @Test
    fun testSearch() = runBlocking {
        // 执行搜索
        val results = searchService.search(
            query = "test",
            limit = 5,
            minScore = 0.5,
            types = setOf(CodeElementType.CLASS),
            searchMode = SearchMode.HYBRID
        )
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.element.type == CodeElementType.CLASS })
    }
    
    @Test
    fun testSearchSemantic() = runBlocking {
        // 执行搜索
        val results = searchService.search(
            query = "test",
            limit = 5,
            minScore = 0.5,
            searchMode = SearchMode.SEMANTIC
        )
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
    }
    
    @Test
    fun testSearchKeyword() = runBlocking {
        // 执行搜索
        val results = searchService.search(
            query = "test",
            limit = 5,
            minScore = 0.5,
            searchMode = SearchMode.KEYWORD
        )
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
    }
    
    @Test
    fun testSearchByFilePath(@TempDir tempDir: Path) = runBlocking {
        // 执行搜索
        val results = searchService.searchByFilePath(tempDir.resolve("TestClass.java"))
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.location.filePath.contains("Test") })
    }
    
    @Test
    fun testSearchByType() = runBlocking {
        // 执行搜索
        val results = searchService.searchByType(CodeElementType.CLASS)
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.type == CodeElementType.CLASS })
    }
    
    @Test
    fun testSearchByName() = runBlocking {
        // 配置模拟对象行为
        every { codeIndexer.getAllElements() } returns listOf(
            createCodeElement("TestClass", CodeElementType.CLASS),
            createCodeElement("AnotherClass", CodeElementType.CLASS),
            createCodeElement("TestMethod", CodeElementType.METHOD)
        )
        
        // 执行搜索
        val results = searchService.searchByName("Test", exactMatch = false)
        
        // 验证结果
        assertNotNull(results)
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.name.contains("Test") })
    }
    
    @Test
    fun testClose() {
        // 执行关闭
        searchService.close()
        
        // 无法直接验证资源是否被关闭，但至少确保方法不会抛出异常
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
            metadata = mapOf(
                "author" to "Test Author",
                "lastModified" to System.currentTimeMillis()
            )
        )
    }
}
