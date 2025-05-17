package ai.kastrax.code.agent

import ai.kastrax.code.agent.specialized.CodeExplanationAgent
import ai.kastrax.code.context.Context
import ai.kastrax.code.context.ContextElement
import ai.kastrax.code.model.CodeElement
import ai.kastrax.code.model.CodeElementType
import ai.kastrax.code.model.DetailLevel
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock
import java.nio.file.Paths

/**
 * JsonPrimitive修复测试
 */
class JsonPrimitiveFixTest : LightPlatformTestCase() {

    private lateinit var codeAgent: CodeAgent
    private lateinit var project: Project

    override fun setUp() {
        super.setUp()
        project = mock(Project::class.java)
        codeAgent = CodeExplanationAgent(project)
    }

    /**
     * 测试Context.toString()方法
     */
    @Test
    fun testContextToString() {
        // 创建上下文元素
        val element = CodeElement(
            id = "test-id",
            type = CodeElementType.CLASS,
            name = "TestClass",
            path = "src/test/kotlin/TestClass.kt",
            content = "class TestClass { }",
            location = null
        )

        // 创建上下文
        val context = Context(
            elements = listOf(
                ContextElement(
                    element = element,
                    content = "class TestClass { }",
                    level = ai.kastrax.code.model.ContextLevel.CLASS,
                    relevance = 0.9f
                )
            ),
            query = "test query"
        )

        // 测试toString()方法
        val contextString = context.toString()
        
        // 验证结果
        assertNotNull("上下文字符串不应为空", contextString)
        assertTrue("上下文字符串应包含元素类型", contextString.contains("CLASS"))
        assertTrue("上下文字符串应包含元素名称", contextString.contains("TestClass"))
        assertTrue("上下文字符串应包含元素内容", contextString.contains("class TestClass { }"))
    }

    /**
     * 测试解释代码方法
     */
    @Test
    fun testExplainCode() = runBlocking {
        // 创建上下文元素
        val element = CodeElement(
            id = "test-id",
            type = CodeElementType.CLASS,
            name = "TestClass",
            path = "src/test/kotlin/TestClass.kt",
            content = "class TestClass { }",
            location = null
        )

        // 创建上下文
        val context = Context(
            elements = listOf(
                ContextElement(
                    element = element,
                    content = "class TestClass { }",
                    level = ai.kastrax.code.model.ContextLevel.CLASS,
                    relevance = 0.9f
                )
            ),
            query = "test query"
        )

        // 测试解释代码方法
        try {
            val result = codeAgent.explainCode("class TestClass { }", DetailLevel.NORMAL, context)
            // 由于没有实际的LLM，这里可能会抛出异常，我们只需要验证代码不会因为getContent()问题而崩溃
            println("解释结果: $result")
        } catch (e: Exception) {
            // 验证异常不是因为getContent()方法未找到
            assertFalse("异常不应该是因为getContent()方法未找到", 
                e.message?.contains("getContent") ?: false)
            assertFalse("异常不应该是因为Unresolved reference", 
                e.message?.contains("Unresolved reference") ?: false)
        }
    }
}
