package ai.kastrax.code.memory

import ai.kastrax.code.model.CodeElement
import ai.kastrax.code.model.CodeElementType
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.ContextLevel
import ai.kastrax.code.model.ContextRelevance
import ai.kastrax.code.model.Location
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk
import java.nio.file.Paths
import java.time.Instant

/**
 * CodeMemorySystemImpl 测试类
 */
class CodeMemorySystemImplTest {

    private lateinit var memorySystem: CodeMemorySystemImpl
    private lateinit var project: Project

    @Before
    fun setUp() {
        project = mockk<Project>()
        memorySystem = CodeMemorySystemImpl(project)
    }

    @After
    fun tearDown() = runBlocking {
        memorySystem.close()
    }

    @Test
    fun testStoreAndRetrieveConversationMemory() = runBlocking {
        // 创建记忆
        val memory = SimpleMemory(
            content = "Hello, world!",
            metadata = mapOf("role" to "user"),
            timestamp = Instant.now()
        )

        // 存储记忆
        val result = memorySystem.storeConversationMemory("test-conversation", memory)
        assertTrue(result)

        // 检索记忆
        val memories = memorySystem.retrieveConversationMemory("test-conversation", 10)
        assertEquals(1, memories.size)
        assertEquals("Hello, world!", memories[0].content)
        assertEquals("user", memories[0].metadata["role"])
    }

    @Test
    fun testClearConversationMemory() = runBlocking {
        // 创建记忆
        val memory = SimpleMemory(
            content = "Hello, world!",
            metadata = mapOf("role" to "user"),
            timestamp = Instant.now()
        )

        // 存储记忆
        memorySystem.storeConversationMemory("test-conversation", memory)

        // 清除记忆
        val result = memorySystem.clearConversationMemory("test-conversation")
        assertTrue(result)

        // 检索记忆
        val memories = memorySystem.retrieveConversationMemory("test-conversation", 10)
        assertEquals(0, memories.size)
    }

    @Test
    fun testStoreAndRetrieveCodeContextMemory() = runBlocking {
        // 创建代码元素
        val codeElement = CodeElement(
            id = "test-id",
            name = "TestClass",
            type = CodeElementType.CLASS,
            content = "public class TestClass {}",
            path = "test/TestClass.java",
            location = Location(1, 1, 10, 1)
        )

        // 创建上下文元素
        val contextElement = ContextElement(
            element = codeElement,
            level = ContextLevel.CLASS,
            relevance = ContextRelevance.HIGH,
            content = "public class TestClass {}",
            score = 0.9f
        )

        // 创建上下文
        val context = Context(
            query = "TestClass",
            elements = listOf(contextElement)
        )

        // 存储上下文
        val result = memorySystem.storeCodeContextMemory(context)
        assertTrue(result)

        // 检索上下文
        val elements = memorySystem.retrieveCodeContextMemory("TestClass", 10, 0.5)
        assertEquals(1, elements.size)
        assertEquals("TestClass", elements[0].element.name)
        assertEquals(CodeElementType.CLASS, elements[0].element.type)
        assertEquals("public class TestClass {}", elements[0].content)
    }

    @Test
    fun testClearCodeContextMemory() = runBlocking {
        // 创建代码元素
        val codeElement = CodeElement(
            id = "test-id",
            name = "TestClass",
            type = CodeElementType.CLASS,
            content = "public class TestClass {}",
            path = "test/TestClass.java",
            location = Location(1, 1, 10, 1)
        )

        // 创建上下文元素
        val contextElement = ContextElement(
            element = codeElement,
            level = ContextLevel.CLASS,
            relevance = ContextRelevance.HIGH,
            content = "public class TestClass {}",
            score = 0.9f
        )

        // 创建上下文
        val context = Context(
            query = "TestClass",
            elements = listOf(contextElement)
        )

        // 存储上下文
        memorySystem.storeCodeContextMemory(context)

        // 清除上下文
        val result = memorySystem.clearCodeContextMemory()
        assertTrue(result)

        // 检索上下文
        val elements = memorySystem.retrieveCodeContextMemory("TestClass", 10, 0.5)
        assertEquals(0, elements.size)
    }

    @Test
    fun testStoreAndRetrieveProjectMemory() = runBlocking {
        // 创建记忆
        val memory = SimpleMemory(
            content = "Project information",
            metadata = mapOf("type" to "PROJECT"),
            timestamp = Instant.now()
        )

        // 存储记忆
        val result = memorySystem.storeProjectMemory("test-project", memory)
        assertTrue(result)

        // 检索记忆
        val memories = memorySystem.retrieveProjectMemory("test-project", null, 10)
        assertEquals(1, memories.size)
        assertEquals("Project information", memories[0].content)
        assertEquals("PROJECT", memories[0].metadata["type"])
    }

    @Test
    fun testClearProjectMemory() = runBlocking {
        // 创建记忆
        val memory = SimpleMemory(
            content = "Project information",
            metadata = mapOf("type" to "PROJECT"),
            timestamp = Instant.now()
        )

        // 存储记忆
        memorySystem.storeProjectMemory("test-project", memory)

        // 清除记忆
        val result = memorySystem.clearProjectMemory("test-project")
        assertTrue(result)

        // 检索记忆
        val memories = memorySystem.retrieveProjectMemory("test-project", null, 10)
        assertEquals(0, memories.size)
    }

    @Test
    fun testStoreAndRetrieveUserPreferenceMemory() = runBlocking {
        // 存储偏好
        val result = memorySystem.storeUserPreferenceMemory("test-user", "theme", "dark")
        assertTrue(result)

        // 检索偏好
        val value = memorySystem.retrieveUserPreferenceMemory("test-user", "theme")
        assertEquals("dark", value)
    }

    @Test
    fun testClearUserPreferenceMemory() = runBlocking {
        // 存储偏好
        memorySystem.storeUserPreferenceMemory("test-user", "theme", "dark")

        // 清除偏好
        val result = memorySystem.clearUserPreferenceMemory("test-user")
        assertTrue(result)

        // 检索偏好
        val value = memorySystem.retrieveUserPreferenceMemory("test-user", "theme")
        assertNull(value)
    }
}
