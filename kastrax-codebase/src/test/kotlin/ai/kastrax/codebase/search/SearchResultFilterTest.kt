package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SearchResultFilterTest {

    private lateinit var filter: SearchResultFilter
    private lateinit var testResults: List<RetrievalResult>
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建过滤器
        filter = SearchResultFilter(
            SearchResultFilterConfig(
                enableTypeFiltering = true,
                enableVisibilityFiltering = true,
                enablePathFiltering = true,
                enableScoreFiltering = true,
                defaultMinScore = 0.5
            )
        )
        
        // 创建测试结果
        val elements = listOf(
            // 类
            CodeElement(
                id = "1",
                name = "TestClass",
                qualifiedName = "com.example.TestClass",
                type = CodeElementType.CLASS,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = tempDir.resolve("src/main/kotlin/com/example/TestClass.kt").toString(),
                    startLine = 1,
                    endLine = 10,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "public class TestClass { ... }",
                documentation = "This is a test class",
                metadata = mutableMapOf()
            ),
            // 接口
            CodeElement(
                id = "2",
                name = "TestInterface",
                qualifiedName = "com.example.TestInterface",
                type = CodeElementType.INTERFACE,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = tempDir.resolve("src/main/kotlin/com/example/TestInterface.kt").toString(),
                    startLine = 1,
                    endLine = 5,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "public interface TestInterface { ... }",
                documentation = "This is a test interface",
                metadata = mutableMapOf()
            ),
            // 方法
            CodeElement(
                id = "3",
                name = "testMethod",
                qualifiedName = "com.example.TestClass.testMethod",
                type = CodeElementType.METHOD,
                visibility = Visibility.PRIVATE,
                location = Location(
                    filePath = tempDir.resolve("src/main/kotlin/com/example/TestClass.kt").toString(),
                    startLine = 3,
                    endLine = 5,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "private void testMethod() { ... }",
                documentation = "This is a test method",
                metadata = mutableMapOf()
            ),
            // 测试类
            CodeElement(
                id = "4",
                name = "TestClassTest",
                qualifiedName = "com.example.TestClassTest",
                type = CodeElementType.CLASS,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = tempDir.resolve("src/test/kotlin/com/example/TestClassTest.kt").toString(),
                    startLine = 1,
                    endLine = 10,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "public class TestClassTest { ... }",
                documentation = "This is a test class test",
                metadata = mutableMapOf()
            )
        )
        
        // 创建测试结果
        testResults = elements.mapIndexed { index, element ->
            RetrievalResult(
                element = element,
                score = 1.0 - (index * 0.2),
                explanation = "Test result ${index + 1}"
            )
        }
    }
    
    @Test
    fun testFilterByType() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            types = setOf(CodeElementType.CLASS)
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(2, filteredResults.size)
        assertTrue(filteredResults.all { it.element.type == CodeElementType.CLASS })
    }
    
    @Test
    fun testFilterByVisibility() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            visibilities = setOf(Visibility.PUBLIC)
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(3, filteredResults.size)
        assertTrue(filteredResults.all { it.element.visibility == Visibility.PUBLIC })
    }
    
    @Test
    fun testFilterByPath() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            includePaths = listOf("src/test")
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(1, filteredResults.size)
        assertTrue(filteredResults.all { it.element.location.filePath.contains("src/test") })
    }
    
    @Test
    fun testFilterByExcludePath() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            excludePaths = listOf("src/test")
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(3, filteredResults.size)
        assertTrue(filteredResults.none { it.element.location.filePath.contains("src/test") })
    }
    
    @Test
    fun testFilterByScore() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            minScore = 0.7
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(2, filteredResults.size)
        assertTrue(filteredResults.all { it.score >= 0.7 })
    }
    
    @Test
    fun testFilterByPattern() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            includePatterns = listOf("Interface")
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(1, filteredResults.size)
        assertEquals("TestInterface", filteredResults.first().element.name)
    }
    
    @Test
    fun testFilterByExcludePattern() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            excludePatterns = listOf("Interface")
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(3, filteredResults.size)
        assertTrue(filteredResults.none { it.element.name.contains("Interface") })
    }
    
    @Test
    fun testFilterWithMultipleCriteria() {
        // 创建过滤条件
        val criteria = filter.createFilterCriteria(
            types = setOf(CodeElementType.CLASS),
            visibilities = setOf(Visibility.PUBLIC),
            excludePaths = listOf("src/test")
        )
        
        // 执行过滤
        val filteredResults = filter.filterResults(testResults, criteria)
        
        // 验证结果
        assertEquals(1, filteredResults.size)
        assertEquals("TestClass", filteredResults.first().element.name)
    }
}
