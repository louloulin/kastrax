package ai.kastrax.core.tools.datetime

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ZodDateTimeToolTest {

    private val dateTimeTool = ZodDateTimeTool.createZodTool()

    @Test
    fun `test current operation`() = runBlocking {
        // 测试获取当前时间
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "current"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertNotNull(result.result)
        assertTrue(result.description.contains("当前日期时间"))
    }

    @Test
    fun `test current operation with format`() = runBlocking {
        // 测试获取当前时间并格式化
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "current",
            format = "yyyy-MM-dd"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertTrue(result.result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(result.description.contains("当前日期时间"))
    }

    @Test
    fun `test current operation with timezone`() = runBlocking {
        // 测试获取指定时区的当前时间
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "current",
            timezone = "UTC"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertNotNull(result.result)
        assertTrue(result.description.contains("UTC时区"))
    }

    @Test
    fun `test format operation`() = runBlocking {
        // 测试格式化日期时间
        val now = LocalDateTime.now()
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "format",
            datetime = now.toString(),
            format = "yyyy-MM-dd HH:mm"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertEquals(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), result.result)
        assertEquals("格式化后的日期时间", result.description)
    }

    @Test
    fun `test parse operation`() = runBlocking {
        // 测试解析日期时间
        val dateStr = "2023-05-15 14:30"
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "parse",
            datetime = dateStr,
            format = "yyyy-MM-dd HH:mm"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertTrue(result.result.startsWith("2023-05-15T14:30"))
        assertEquals("解析后的日期时间", result.description)
    }

    @Test
    fun `test add operation`() = runBlocking {
        // 测试日期时间加法
        val now = LocalDateTime.now()
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "add",
            datetime = now.toString(),
            amount = 5,
            unit = "days"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertNotNull(result.result)
        assertEquals("添加 5 days 后的日期时间", result.description)
    }

    @Test
    fun `test subtract operation`() = runBlocking {
        // 测试日期时间减法
        val now = LocalDateTime.now()
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "subtract",
            datetime = now.toString(),
            amount = 3,
            unit = "hours"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertNotNull(result.result)
        assertEquals("减去 3 hours 后的日期时间", result.description)
    }

    @Test
    fun `test difference operation`() = runBlocking {
        // 测试计算日期时间差异
        val date1 = LocalDateTime.of(2023, 5, 15, 14, 30)
        val date2 = LocalDateTime.of(2023, 5, 20, 10, 15)

        val input = ZodDateTimeTool.DateTimeInput(
            operation = "difference",
            datetime1 = date1.toString(),
            datetime2 = date2.toString(),
            unit = "days"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertEquals("4", result.result)
        assertEquals("两个日期时间之间相差的 days 数", result.description)
    }

    @Test
    fun `test convert operation`() = runBlocking {
        // 测试时区转换
        val now = LocalDateTime.now()
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "convert",
            datetime = now.toString(),
            timezone = "UTC",
            targetTimezone = "America/New_York"
        )

        val result = dateTimeTool.execute(input)

        assertNotNull(result)
        assertNotNull(result.result)
        assertTrue(result.description.contains("从 UTC 转换到 America/New_York"))
    }

    @Test
    fun `test invalid operation`() = runBlocking {
        // 测试无效操作
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "invalid_operation"
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                dateTimeTool.execute(input)
            }
        }

        assertTrue(exception.message?.contains("不支持的操作") ?: false)
    }

    @Test
    fun `test missing required parameters`() = runBlocking {
        // 测试缺少必需参数
        val input = ZodDateTimeTool.DateTimeInput(
            operation = "format"
        )

        val exception = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                dateTimeTool.execute(input)
            }
        }

        assertTrue(exception.message?.contains("需要") ?: false)
    }
}
