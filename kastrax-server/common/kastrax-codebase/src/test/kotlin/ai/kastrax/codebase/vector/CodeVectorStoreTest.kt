package ai.kastrax.codebase.vector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.Location
import ai.kastrax.codebase.semantic.model.Visibility
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import ai.kastrax.store.memory.InMemoryVectorStore
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID

class CodeVectorStoreTest {

    private lateinit var vectorStore: VectorStore
    private lateinit var codeVectorStore: CodeVectorStore

    @BeforeEach
    fun setUp() {
        vectorStore = InMemoryVectorStore(1536)
        codeVectorStore = CodeVectorStore(
            baseVectorStore = vectorStore,
            indexName = "test-code-elements",
            dimension = 1536,
            metric = SimilarityMetric.COSINE
        )
    }

    @Test
    fun `test initialize`() = runBlocking {
        // 初始化向量存储
        codeVectorStore.initialize()
        
        // 验证初始化成功
        val elementCount = codeVectorStore.getElementCount()
        assertEquals(0, elementCount)
    }

    @Test
    fun `test add element`() = runBlocking {
        // 创建代码元素
        val element = createTestElement()
        
        // 创建向量
        val vector = FloatArray(1536) { 0.1f }
        
        // 添加元素
        val result = codeVectorStore.addElement(element, vector)
        
        // 验证添加成功
        assertTrue(result)
        
        // 验证元素已添加
        val retrievedElement = codeVectorStore.getElement(element.id)
        assertNotNull(retrievedElement)
        assertEquals(element.id, retrievedElement?.id)
    }

    @Test
    fun `test similarity search`() = runBlocking {
        // 创建代码元素
        val element1 = createTestElement(name = "TestClass1")
        val element2 = createTestElement(name = "TestClass2")
        
        // 创建向量
        val vector1 = FloatArray(1536) { 0.1f }
        val vector2 = FloatArray(1536) { 0.2f }
        
        // 添加元素
        codeVectorStore.addElement(element1, vector1)
        codeVectorStore.addElement(element2, vector2)
        
        // 执行相似度搜索
        val queryVector = List(1536) { 0.15f }
        val results = codeVectorStore.similaritySearch(queryVector, limit = 2)
        
        // 验证搜索结果
        assertEquals(2, results.size)
    }

    @Test
    fun `test delete element`() = runBlocking {
        // 创建代码元素
        val element = createTestElement()
        
        // 创建向量
        val vector = FloatArray(1536) { 0.1f }
        
        // 添加元素
        codeVectorStore.addElement(element, vector)
        
        // 删除元素
        val result = codeVectorStore.deleteElement(element.id)
        
        // 验证删除成功
        assertTrue(result)
        
        // 验证元素已删除
        val retrievedElement = codeVectorStore.getElement(element.id)
        assertEquals(null, retrievedElement)
    }

    @Test
    fun `test clear`() = runBlocking {
        // 创建代码元素
        val element = createTestElement()
        
        // 创建向量
        val vector = FloatArray(1536) { 0.1f }
        
        // 添加元素
        codeVectorStore.addElement(element, vector)
        
        // 清空向量存储
        val result = codeVectorStore.clear()
        
        // 验证清空成功
        assertTrue(result)
        
        // 验证元素已清空
        val elementCount = codeVectorStore.getElementCount()
        assertEquals(0, elementCount)
    }

    private fun createTestElement(
        id: String = UUID.randomUUID().toString(),
        name: String = "TestClass",
        type: CodeElementType = CodeElementType.CLASS
    ): CodeElement {
        return CodeElement(
            id = id,
            name = name,
            qualifiedName = "com.example.$name",
            type = type,
            location = Location(
                filePath = Path.of("$name.java"),
                startLine = 1,
                startColumn = 1,
                endLine = 10,
                endColumn = 1
            ),
            visibility = Visibility.PUBLIC,
            documentation = "This is a test class."
        )
    }
}
