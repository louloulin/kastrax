package ai.kastrax.codebase.search

import ai.kastrax.codebase.indexing.CodeIndexer
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths

class StructureAwareSearcherTest {

    private lateinit var codeIndexer: CodeIndexer
    private lateinit var structureAwareSearcher: StructureAwareSearcher
    private lateinit var testElements: List<CodeElement>

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        // 创建测试元素
        val testFilePath = tempDir.resolve("TestClass.kt")

        // 创建父类
        val parentClass = CodeElement(
            id = "1",
            name = "ParentClass",
            qualifiedName = "com.example.ParentClass",
            type = CodeElementType.CLASS,
            visibility = Visibility.PUBLIC,
            location = Location(
                filePath = testFilePath.toString(),
                startLine = 1,
                endLine = 20,
                startColumn = 0,
                endColumn = 0
            ),
            content = "public class ParentClass { ... }",
            documentation = "This is a parent class",
            metadata = mutableMapOf()
        )

        // 创建子类
        val childClass = CodeElement(
            id = "2",
            name = "ChildClass",
            qualifiedName = "com.example.ChildClass",
            type = CodeElementType.CLASS,
            visibility = Visibility.PUBLIC,
            location = Location(
                filePath = testFilePath.toString(),
                startLine = 22,
                endLine = 40,
                startColumn = 0,
                endColumn = 0
            ),
            content = "public class ChildClass extends ParentClass { ... }",
            documentation = "This is a child class",
            metadata = mutableMapOf(
                "implements" to listOf(parentClass)
            ),
            parent = parentClass
        )

        // 创建方法
        val method = CodeElement(
            id = "3",
            name = "testMethod",
            qualifiedName = "com.example.ChildClass.testMethod",
            type = CodeElementType.METHOD,
            visibility = Visibility.PUBLIC,
            location = Location(
                filePath = testFilePath.toString(),
                startLine = 25,
                endLine = 30,
                startColumn = 0,
                endColumn = 0
            ),
            content = "public void testMethod() { ... }",
            documentation = "This is a test method",
            metadata = mutableMapOf(),
            parent = childClass
        )

        // 设置关系
        parentClass.children.add(childClass)
        childClass.children.add(method)

        testElements = listOf(parentClass, childClass, method)

        // 创建模拟对象
        codeIndexer = mockk()
        coEvery { codeIndexer.getAllElements() } returns testElements

        // 创建结构感知搜索器
        structureAwareSearcher = StructureAwareSearcher(codeIndexer)
    }

    @Test
    fun testSearch() = runBlocking {
        // 执行搜索
        val results = structureAwareSearcher.search(
            query = "test method",
            limit = 5,
            minScore = 0.5f
        )

        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("testMethod", results.first().element.name)

        // 验证相关元素
        val firstResult = results.first()
        assertTrue(firstResult.relatedElements.isNotEmpty())
        assertTrue(firstResult.relatedElements.any { it.name == "ChildClass" })
    }

    @Test
    fun testSearchWithTypeFilter() = runBlocking {
        // 执行搜索
        val results = structureAwareSearcher.search(
            query = "class",
            limit = 5,
            minScore = 0.5f,
            types = setOf(CodeElementType.CLASS)
        )

        // 验证结果
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.element.type == CodeElementType.CLASS })
    }

    @Test
    fun testConvertToRetrievalResult() = runBlocking {
        // 执行搜索
        val results = structureAwareSearcher.search(
            query = "test method",
            limit = 1,
            minScore = 0.5f
        )

        // 转换为检索结果
        val retrievalResult = structureAwareSearcher.convertToRetrievalResult(results.first())

        // 验证结果
        assertEquals(results.first().element.id, retrievalResult.element.id)
        assertEquals(results.first().score.toDouble(), retrievalResult.score)
    }
}
