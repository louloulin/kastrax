package ai.kastrax.core.tools.datetime

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateTimeToolTest {

    private val dateTimeTool = DateTimeTool.create()

    @Test
    fun `test current operation`() = runBlocking {
        // 测试获取当前时间
        val input = buildJsonObject {
            put("operation", JsonPrimitive("current"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertTrue(description?.contains("当前日期时间") ?: false)
    }

    @Test
    fun `test current operation with format`() = runBlocking {
        // 测试获取当前时间并格式化
        val input = buildJsonObject {
            put("operation", JsonPrimitive("current"))
            put("format", JsonPrimitive("yyyy-MM-dd"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertTrue(resultValue?.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ?: false)
        assertTrue(description?.contains("当前日期时间") ?: false)
    }

    @Test
    fun `test current operation with timezone`() = runBlocking {
        // 测试获取指定时区的当前时间
        val input = buildJsonObject {
            put("operation", JsonPrimitive("current"))
            put("timezone", JsonPrimitive("UTC"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertTrue(description?.contains("UTC时区") ?: false)
    }

    @Test
    fun `test format operation`() = runBlocking {
        // 测试格式化日期时间
        val now = LocalDateTime.now()
        val input = buildJsonObject {
            put("operation", JsonPrimitive("format"))
            put("datetime", JsonPrimitive(now.toString()))
            put("format", JsonPrimitive("yyyy-MM-dd HH:mm"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertEquals(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), resultValue)
        assertEquals("格式化后的日期时间", description)
    }

    @Test
    fun `test parse operation`() = runBlocking {
        // 测试解析日期时间
        val dateStr = "2023-05-15 14:30"
        val input = buildJsonObject {
            put("operation", JsonPrimitive("parse"))
            put("datetime", JsonPrimitive(dateStr))
            put("format", JsonPrimitive("yyyy-MM-dd HH:mm"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertTrue(resultValue?.startsWith("2023-05-15T14:30") ?: false)
        assertEquals("解析后的日期时间", description)
    }

    @Test
    fun `test add operation`() = runBlocking {
        // 测试日期时间加法
        val now = LocalDateTime.now()
        val input = buildJsonObject {
            put("operation", JsonPrimitive("add"))
            put("datetime", JsonPrimitive(now.toString()))
            put("amount", JsonPrimitive(5))
            put("unit", JsonPrimitive("days"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertEquals("添加 5 days 后的日期时间", description)
    }

    @Test
    fun `test subtract operation`() = runBlocking {
        // 测试日期时间减法
        val now = LocalDateTime.now()
        val input = buildJsonObject {
            put("operation", JsonPrimitive("subtract"))
            put("datetime", JsonPrimitive(now.toString()))
            put("amount", JsonPrimitive(3))
            put("unit", JsonPrimitive("hours"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertEquals("减去 3 hours 后的日期时间", description)
    }

    @Test
    fun `test difference operation`() = runBlocking {
        // 测试计算日期时间差异
        val date1 = LocalDateTime.of(2023, 5, 15, 14, 30)
        val date2 = LocalDateTime.of(2023, 5, 20, 10, 15)
        
        val input = buildJsonObject {
            put("operation", JsonPrimitive("difference"))
            put("datetime1", JsonPrimitive(date1.toString()))
            put("datetime2", JsonPrimitive(date2.toString()))
            put("unit", JsonPrimitive("days"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertEquals("4", resultValue)
        assertEquals("两个日期时间之间相差的 days 数", description)
    }

    @Test
    fun `test convert operation`() = runBlocking {
        // 测试时区转换
        val now = LocalDateTime.now()
        val input = buildJsonObject {
            put("operation", JsonPrimitive("convert"))
            put("datetime", JsonPrimitive(now.toString()))
            put("timezone", JsonPrimitive("UTC"))
            put("targetTimezone", JsonPrimitive("America/New_York"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val resultValue = result["result"]?.jsonPrimitive?.content
        val description = result["description"]?.jsonPrimitive?.content

        assertNotNull(resultValue)
        assertTrue(description?.contains("从 UTC 转换到 America/New_York") ?: false)
    }

    @Test
    fun `test invalid operation`() = runBlocking {
        // 测试无效操作
        val input = buildJsonObject {
            put("operation", JsonPrimitive("invalid_operation"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val description = result["description"]?.jsonPrimitive?.content

        assertTrue(description?.contains("错误") ?: false)
        assertTrue(description?.contains("不支持的操作") ?: false)
    }

    @Test
    fun `test missing required parameters`() = runBlocking {
        // 测试缺少必需参数
        val input = buildJsonObject {
            put("operation", JsonPrimitive("format"))
        }

        val result = dateTimeTool.execute(input).jsonObject
        val description = result["description"]?.jsonPrimitive?.content

        assertTrue(description?.contains("错误") ?: false)
        assertTrue(description?.contains("需要") ?: false)
    }
}
