package ai.kastrax.examples

import ai.kastrax.core.tools.datetime.DateTimeToolFactory
import ai.kastrax.core.tools.datetime.ZodDateTimeTool
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 日期时间工具示例。
 */
fun main() {
    // 创建日期时间工具
    val dateTimeTool = DateTimeToolFactory.createZodTool()
    
    println("=== 日期时间工具示例 ===")
    
    // 获取当前日期时间
    val currentResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "current"
            )
        )
    }
    println("当前日期时间: ${currentResult.result}")
    
    // 获取格式化的当前日期时间
    val formattedCurrentResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "current",
                format = "yyyy年MM月dd日 HH时mm分ss秒"
            )
        )
    }
    println("格式化的当前日期时间: ${formattedCurrentResult.result}")
    
    // 获取指定时区的当前日期时间
    val timezoneCurrentResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "current",
                timezone = "America/New_York",
                format = "yyyy-MM-dd HH:mm:ss z"
            )
        )
    }
    println("纽约时间: ${timezoneCurrentResult.result}")
    
    // 日期时间加法
    val now = LocalDateTime.now()
    val addResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "add",
                datetime = now.toString(),
                amount = 7,
                unit = "days",
                format = "yyyy-MM-dd"
            )
        )
    }
    println("7天后的日期: ${addResult.result}")
    
    // 日期时间减法
    val subtractResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "subtract",
                datetime = now.toString(),
                amount = 3,
                unit = "months",
                format = "yyyy年MM月"
            )
        )
    }
    println("3个月前: ${subtractResult.result}")
    
    // 日期时间解析
    val parseResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "parse",
                datetime = "2023-12-25 08:30",
                format = "yyyy-MM-dd HH:mm"
            )
        )
    }
    println("解析的日期时间: ${parseResult.result}")
    
    // 日期时间格式化
    val formatResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "format",
                datetime = "2023-12-25T08:30:00",
                format = "yyyy年MM月dd日 HH时mm分"
            )
        )
    }
    println("格式化的日期时间: ${formatResult.result}")
    
    // 日期时间差异
    val date1 = LocalDateTime.of(2023, 1, 1, 0, 0)
    val date2 = LocalDateTime.of(2023, 12, 31, 23, 59)
    val differenceResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "difference",
                datetime = date1.toString(),
                targetTimezone = date2.toString(),
                unit = "days"
            )
        )
    }
    println("2023年1月1日到12月31日相差: ${differenceResult.result} 天")
    
    // 时区转换
    val convertResult = runBlocking {
        dateTimeTool.execute(
            ZodDateTimeTool.DateTimeInput(
                operation = "convert",
                datetime = now.toString(),
                timezone = "UTC",
                targetTimezone = "Asia/Tokyo",
                format = "yyyy-MM-dd HH:mm:ss z"
            )
        )
    }
    println("从UTC转换到东京时间: ${convertResult.result}")
}
