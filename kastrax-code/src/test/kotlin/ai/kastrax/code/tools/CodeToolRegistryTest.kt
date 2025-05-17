package ai.kastrax.code.tools

import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import io.mockk.mockk

/**
 * 代码工具注册表测试
 */
class CodeToolRegistryTest : LightPlatformTestCase() {

    private lateinit var toolRegistry: CodeToolRegistry
    private lateinit var project: Project

    override fun setUp() {
        super.setUp()

        // 使用模拟对象
        project = mockk<Project>(relaxed = true)

        // 创建工具注册表
        toolRegistry = CodeToolRegistry(project)
        toolRegistry.initializeDefaultTools()

        // 注意：在实际测试中，可以使用真实的 Project 对象
        // 但这需要在 IntelliJ IDEA 环境中运行，而不是在普通的 JUnit 测试中
    }

    /**
     * 测试获取所有工具
     */
    @Test
    fun testGetTools() {
        val tools = toolRegistry.getTools()

        // 验证工具数量
        assertEquals("应该有2个工具", 2, tools.size)

        // 验证工具名称
        val toolNames = tools.map { it.name }
        assertTrue("应该包含代码格式化工具", toolNames.contains("代码格式化"))
        assertTrue("应该包含代码分析工具", toolNames.contains("代码分析"))
    }

    /**
     * 测试代码格式化工具
     */
    @Test
    fun testFormatCodeTool() = runBlocking {
        // 获取格式化工具
        val formatTool = toolRegistry.getToolByName("代码格式化")
        assertNotNull("格式化工具不应为空", formatTool)

        // 创建输入参数
        val input = buildJsonObject {
            put("code", JsonPrimitive("function test() { return 1; }"))
            put("language", JsonPrimitive("javascript"))
        }

        // 执行工具
        val result = formatTool?.execute(input)

        // 验证结果
        assertNotNull("结果不应为空", result)
        assertTrue("结果应该是JsonObject", result is JsonObject)

        val resultObj = result as JsonObject
        assertTrue("结果应该包含formattedCode字段", resultObj.containsKey("formattedCode"))
        assertTrue("结果应该包含success字段", resultObj.containsKey("success"))
        assertEquals("success应该为true", true, resultObj["success"]?.jsonPrimitive?.content?.toBoolean())
    }

    /**
     * 测试代码分析工具
     */
    @Test
    fun testAnalyzeCodeTool() = runBlocking {
        // 获取分析工具
        val analyzeTool = toolRegistry.getToolByName("代码分析")
        assertNotNull("分析工具不应为空", analyzeTool)

        // 创建输入参数
        val input = buildJsonObject {
            put("code", JsonPrimitive("function test() { return 1; }"))
            put("language", JsonPrimitive("javascript"))
        }

        // 执行工具
        val result = analyzeTool?.execute(input)

        // 验证结果
        assertNotNull("结果不应为空", result)
        assertTrue("结果应该是JsonObject", result is JsonObject)

        val resultObj = result as JsonObject
        assertTrue("结果应该包含suggestions字段", resultObj.containsKey("suggestions"))
        assertTrue("结果应该包含success字段", resultObj.containsKey("success"))
        assertEquals("success应该为true", true, resultObj["success"]?.jsonPrimitive?.content?.toBoolean())
    }

    /**
     * 测试错误处理
     */
    @Test
    fun testErrorHandling() = runBlocking {
        // 获取格式化工具
        val formatTool = toolRegistry.getToolByName("代码格式化")
        assertNotNull("格式化工具不应为空", formatTool)

        // 创建缺少必要参数的输入
        val input = buildJsonObject {
            put("code", JsonPrimitive("function test() { return 1; }"))
            // 缺少language参数
        }

        // 执行工具
        val result = formatTool?.execute(input)

        // 验证结果
        assertNotNull("结果不应为空", result)
        assertTrue("结果应该是JsonObject", result is JsonObject)

        val resultObj = result as JsonObject
        assertTrue("结果应该包含error字段", resultObj.containsKey("error"))
        assertTrue("结果应该包含success字段", resultObj.containsKey("success"))
        assertEquals("success应该为false", false, resultObj["success"]?.jsonPrimitive?.content?.toBoolean())
    }
}
