package ai.kastrax.core.tools

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.util.Base64

/**
 * SimpleToolFactory 测试类。
 */
class ZodToolFactoryTest {

    @Test
    fun testCalculatorTool() = runBlocking {
        // 创建计算器工具
        val tool = SimpleToolFactory.createCalculatorTool()

        // 测试加法
        val inputObj = buildJsonObject {
            put("operation", "add")
            put("a", 5)
            put("b", 3)
        }

        val result = tool.execute(inputObj)

        val resultObj = result.jsonObject
        assertTrue(resultObj["success"]?.toString()?.toBoolean() ?: false)
        assertEquals("8.0", resultObj["result"]?.toString()?.trim('"'))
    }

    @Test
    fun testTextProcessingTool() = runBlocking {
        // 创建文本处理工具
        val tool = SimpleToolFactory.createTextProcessingTool()

        // 测试大写转换
        val inputObj = buildJsonObject {
            put("operation", "uppercase")
            put("text", "hello world")
        }

        val result = tool.execute(inputObj)

        val resultObj = result.jsonObject
        assertTrue(resultObj["success"]?.toString()?.toBoolean() ?: false)
        assertEquals("HELLO WORLD", resultObj["result"]?.toString()?.trim('"'))
    }

    @Test
    fun testDateTimeTool() = runBlocking {
        // 创建日期时间工具
        val tool = SimpleToolFactory.createDateTimeTool()

        // 测试获取当前时间
        val inputObj = buildJsonObject {
            put("operation", "current_time")
        }

        val result = tool.execute(inputObj)

        val resultObj = result.jsonObject
        assertTrue(resultObj["success"]?.toString()?.toBoolean() ?: false)
        assertNotNull(resultObj["result"])
    }
}
