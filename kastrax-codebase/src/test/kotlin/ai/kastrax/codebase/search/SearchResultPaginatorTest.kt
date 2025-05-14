package ai.kastrax.codebase.search

import ai.kastrax.codebase.retrieval.model.RetrievalResult
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SearchResultPaginatorTest {

    private lateinit var paginator: SearchResultPaginator
    private lateinit var testResults: List<RetrievalResult>
    
    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建分页器
        paginator = SearchResultPaginator(
            PaginationConfig(
                defaultPageSize = 5,
                maxPageSize = 20
            )
        )
        
        // 创建测试结果
        testResults = (1..25).map { i ->
            val element = CodeElement(
                id = i.toString(),
                name = "TestElement$i",
                qualifiedName = "com.example.TestElement$i",
                type = CodeElementType.CLASS,
                visibility = Visibility.PUBLIC,
                location = Location(
                    filePath = tempDir.resolve("test$i.kt").toString(),
                    startLine = 1,
                    endLine = 10,
                    startColumn = 0,
                    endColumn = 0
                ),
                content = "class TestElement$i { ... }",
                documentation = "This is test element $i",
                metadata = mutableMapOf()
            )
            
            RetrievalResult(
                element = element,
                score = 1.0 - (i * 0.02),
                explanation = "Test result $i"
            )
        }
    }
    
    @Test
    fun testPaginate() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 1, 5)
        
        // 验证结果
        assertEquals(5, pagedResult.items.size)
        assertEquals(25, pagedResult.totalItems)
        assertEquals(1, pagedResult.pageNumber)
        assertEquals(5, pagedResult.pageSize)
        assertEquals(5, pagedResult.totalPages)
        assertTrue(pagedResult.hasNext)
        assertFalse(pagedResult.hasPrevious)
        
        // 验证项目顺序
        assertEquals("1", pagedResult.items.first().element.id)
        assertEquals("5", pagedResult.items.last().element.id)
    }
    
    @Test
    fun testPaginateLastPage() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 5, 5)
        
        // 验证结果
        assertEquals(5, pagedResult.items.size)
        assertEquals(25, pagedResult.totalItems)
        assertEquals(5, pagedResult.pageNumber)
        assertEquals(5, pagedResult.pageSize)
        assertEquals(5, pagedResult.totalPages)
        assertFalse(pagedResult.hasNext)
        assertTrue(pagedResult.hasPrevious)
        
        // 验证项目顺序
        assertEquals("21", pagedResult.items.first().element.id)
        assertEquals("25", pagedResult.items.last().element.id)
    }
    
    @Test
    fun testPaginateEmptyResults() {
        // 执行分页
        val pagedResult = paginator.paginate(emptyList<RetrievalResult>(), 1, 5)
        
        // 验证结果
        assertEquals(0, pagedResult.items.size)
        assertEquals(0, pagedResult.totalItems)
        assertEquals(1, pagedResult.pageNumber)
        assertEquals(5, pagedResult.pageSize)
        assertEquals(1, pagedResult.totalPages)
        assertFalse(pagedResult.hasNext)
        assertFalse(pagedResult.hasPrevious)
    }
    
    @Test
    fun testPaginateInvalidPageNumber() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, -1, 5)
        
        // 验证结果
        assertEquals(5, pagedResult.items.size)
        assertEquals(25, pagedResult.totalItems)
        assertEquals(1, pagedResult.pageNumber) // 应该使用有效的页码
        assertEquals(5, pagedResult.pageSize)
        assertEquals(5, pagedResult.totalPages)
        assertTrue(pagedResult.hasNext)
        assertFalse(pagedResult.hasPrevious)
    }
    
    @Test
    fun testPaginateInvalidPageSize() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 1, -1)
        
        // 验证结果
        assertEquals(1, pagedResult.items.size) // 应该使用有效的页大小
        assertEquals(25, pagedResult.totalItems)
        assertEquals(1, pagedResult.pageNumber)
        assertEquals(1, pagedResult.pageSize) // 应该使用有效的页大小
        assertEquals(25, pagedResult.totalPages)
        assertTrue(pagedResult.hasNext)
        assertFalse(pagedResult.hasPrevious)
    }
    
    @Test
    fun testPaginateExceedMaxPageSize() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 1, 30)
        
        // 验证结果
        assertEquals(20, pagedResult.items.size) // 应该限制为最大页大小
        assertEquals(25, pagedResult.totalItems)
        assertEquals(1, pagedResult.pageNumber)
        assertEquals(20, pagedResult.pageSize) // 应该限制为最大页大小
        assertEquals(2, pagedResult.totalPages)
        assertTrue(pagedResult.hasNext)
        assertFalse(pagedResult.hasPrevious)
    }
    
    @Test
    fun testGeneratePaginationInfo() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 2, 5)
        
        // 生成分页信息
        val paginationInfo = paginator.generatePaginationInfo(pagedResult)
        
        // 验证结果
        assertTrue(paginationInfo.contains("页码: 2/5"))
        assertTrue(paginationInfo.contains("项目: 5/25"))
        assertTrue(paginationInfo.contains("页大小: 5"))
        assertTrue(paginationInfo.contains("[上一页(1)]"))
        assertTrue(paginationInfo.contains("[下一页(3)]"))
    }
    
    @Test
    fun testGenerateHtmlPaginationInfo() {
        // 执行分页
        val pagedResult = paginator.paginate(testResults, 2, 5)
        
        // 生成HTML分页信息
        val htmlPaginationInfo = paginator.generateHtmlPaginationInfo(pagedResult, "/search?q=test")
        
        // 验证结果
        assertTrue(htmlPaginationInfo.contains("<div class=\"pagination\">"))
        assertTrue(htmlPaginationInfo.contains("页码: 2/5"))
        assertTrue(htmlPaginationInfo.contains("项目: 5/25"))
        assertTrue(htmlPaginationInfo.contains("页大小: 5"))
        assertTrue(htmlPaginationInfo.contains("<a href=\"/search?q=test&page=1&pageSize=5\""))
        assertTrue(htmlPaginationInfo.contains("<a href=\"/search?q=test&page=3&pageSize=5\""))
    }
}
