package ai.kastrax.codebase.context

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.codebase.semantic.relation.CodeRelation
import ai.kastrax.codebase.semantic.relation.CodeRelationAnalyzer
import ai.kastrax.codebase.semantic.relation.RelationType
import ai.kastrax.codebase.vector.CodeSearchResult
import ai.kastrax.codebase.vector.CodeVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class ContextBuilderTest {
    private lateinit var vectorStore: CodeVectorStore
    private lateinit var embeddingService: EmbeddingService
    private lateinit var relationAnalyzer: CodeRelationAnalyzer
    private lateinit var contextBuilder: ContextBuilder

    private val testFilePath = Paths.get("/test/path/TestFile.kt")

    @BeforeEach
    fun setUp() {
        vectorStore = mockk(relaxed = true)
        embeddingService = mockk(relaxed = true)
        relationAnalyzer = mockk(relaxed = true)

        // 创建上下文构建器
        contextBuilder = ContextBuilder(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            config = ContextBuilderConfig(
                includeRelatedElements = true,
                maxRelatedElements = 5
            ),
            relationAnalyzer = relationAnalyzer
        )
    }

    @Test
    fun `test buildContext with related elements`() = runBlocking {
        // 创建测试元素
        val classElement = createClassElement("TestClass")
        val methodElement = createMethodElement("testMethod", classElement)
        val fieldElement = createFieldElement("testField", classElement)

        // 创建相关元素
        val relatedClass = createClassElement("RelatedClass")
        val relatedMethod = createMethodElement("relatedMethod", relatedClass)

        // 设置向量存储的搜索结果
        val searchResults = listOf(
            CodeSearchResult(classElement, 0.9),
            CodeSearchResult(methodElement, 0.8)
        )

        coEvery {
            vectorStore.similaritySearch(any(), any(), any())
        } returns searchResults

        // 设置关系分析器的结果
        val classRelation = CodeRelation(
            id = "relation1",
            sourceId = classElement.id,
            targetId = relatedClass.id,
            type = RelationType.INHERITANCE,
            metadata = mutableMapOf("description" to "TestClass extends RelatedClass")
        )

        val methodRelation = CodeRelation(
            id = "relation2",
            sourceId = methodElement.id,
            targetId = relatedMethod.id,
            type = RelationType.OVERRIDE,
            metadata = mutableMapOf("description" to "testMethod overrides relatedMethod")
        )

        every {
            relationAnalyzer.getElementRelations(classElement.id)
        } returns listOf(classRelation)

        every {
            relationAnalyzer.getElementRelations(methodElement.id)
        } returns listOf(methodRelation)

        every {
            vectorStore.getElement(relatedClass.id)
        } returns relatedClass

        every {
            vectorStore.getElement(relatedMethod.id)
        } returns relatedMethod

        // 执行测试
        val context = contextBuilder.buildContext(
            query = "test query",
            maxElements = 10,
            minScore = 0.5
        )

        // 验证结果
        assertEquals(4, context.elements.size, "应该包含原始元素和相关元素")

        // 验证原始元素
        assertTrue(context.elements.any { it.element.id == classElement.id })
        assertTrue(context.elements.any { it.element.id == methodElement.id })

        // 验证相关元素
        assertTrue(context.elements.any { it.element.id == relatedClass.id })
        assertTrue(context.elements.any { it.element.id == relatedMethod.id })

        // 验证相关元素的元数据
        val relatedClassElement = context.elements.first { it.element.id == relatedClass.id }
        assertEquals(RelationType.INHERITANCE.name, relatedClassElement.metadata["relationType"])
        assertEquals("TestClass extends RelatedClass", relatedClassElement.metadata["relationDescription"])

        val relatedMethodElement = context.elements.first { it.element.id == relatedMethod.id }
        assertEquals(RelationType.OVERRIDE.name, relatedMethodElement.metadata["relationType"])
        assertEquals("testMethod overrides relatedMethod", relatedMethodElement.metadata["relationDescription"])
    }

    @Test
    fun `test buildContext without related elements`() = runBlocking {
        // 创建测试元素
        val classElement = createClassElement("TestClass")
        val methodElement = createMethodElement("testMethod", classElement)

        // 设置向量存储的搜索结果
        val searchResults = listOf(
            CodeSearchResult(classElement, 0.9),
            CodeSearchResult(methodElement, 0.8)
        )

        coEvery {
            vectorStore.similaritySearch(any(), any(), any())
        } returns searchResults

        // 创建不包含相关元素的上下文构建器
        val contextBuilderWithoutRelated = ContextBuilder(
            vectorStore = vectorStore,
            embeddingService = embeddingService,
            config = ContextBuilderConfig(
                includeRelatedElements = false
            ),
            relationAnalyzer = relationAnalyzer
        )

        // 执行测试
        val context = contextBuilderWithoutRelated.buildContext(
            query = "test query",
            maxElements = 10,
            minScore = 0.5
        )

        // 验证结果
        assertEquals(2, context.elements.size, "应该只包含原始元素")

        // 验证原始元素
        assertTrue(context.elements.any { it.element.id == classElement.id })
        assertTrue(context.elements.any { it.element.id == methodElement.id })

        // 验证没有调用关系分析器
        verify(exactly = 0) { relationAnalyzer.getElementRelations(any()) }
    }

    // 辅助方法：创建类元素
    private fun createClassElement(name: String): CodeElement {
        return CodeElement(
            id = "class:$name",
            name = name,
            qualifiedName = "com.example.$name",
            type = CodeElementType.CLASS,
            location = Location(
                filePath = testFilePath,
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            language = "kotlin"
        )
    }

    // 辅助方法：创建方法元素
    private fun createMethodElement(name: String, parent: CodeElement): CodeElement {
        return CodeElement(
            id = "${parent.id}:method:$name",
            name = name,
            qualifiedName = "${parent.qualifiedName}.$name",
            type = CodeElementType.METHOD,
            location = Location(
                filePath = testFilePath,
                startLine = 2,
                startColumn = 1,
                endLine = 5,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            parent = parent,
            language = "kotlin"
        )
    }

    // 辅助方法：创建字段元素
    private fun createFieldElement(name: String, parent: CodeElement): CodeElement {
        return CodeElement(
            id = "${parent.id}:field:$name",
            name = name,
            qualifiedName = "${parent.qualifiedName}.$name",
            type = CodeElementType.FIELD,
            location = Location(
                filePath = testFilePath,
                startLine = 6,
                startColumn = 1,
                endLine = 6,
                endColumn = 20
            ),
            visibility = Visibility.PRIVATE,
            parent = parent,
            language = "kotlin"
        )
    }
}
