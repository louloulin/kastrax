package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.datetime.DateTimeToolFactory
import ai.kastrax.core.tools.datetime.ZodDateTimeTool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 日期时间工具示例，展示如何使用日期时间工具执行日期时间操作。
 */
fun main() = runBlocking {
    println("KastraX 日期时间工具示例")
    println("-------------------")

    // 创建日期时间工具
    val dateTimeTool = DateTimeToolFactory.createZodTool()

    println("\n直接使用 ZodTool 接口:")

    // 获取当前时间
    val currentInput = ZodDateTimeTool.DateTimeInput(
        operation = "current",
        format = "yyyy年MM月dd日 HH时mm分ss秒"
    )
    val currentResult = dateTimeTool.execute(currentInput)
    println("当前时间: ${currentResult.result}")

    // 获取指定时区的当前时间
    val timezoneInput = ZodDateTimeTool.DateTimeInput(
        operation = "current",
        timezone = "America/New_York",
        format = "yyyy-MM-dd HH:mm:ss z"
    )
    val timezoneResult = dateTimeTool.execute(timezoneInput)
    println("纽约时间: ${timezoneResult.result}")

    // 日期时间加法
    val now = LocalDateTime.now()
    val addInput = ZodDateTimeTool.DateTimeInput(
        operation = "add",
        datetime = now.toString(),
        amount = 7,
        unit = "days",
        format = "yyyy-MM-dd"
    )
    val addResult = dateTimeTool.execute(addInput)
    println("7天后: ${addResult.result}")

    // 日期时间减法
    val subtractInput = ZodDateTimeTool.DateTimeInput(
        operation = "subtract",
        datetime = now.toString(),
        amount = 3,
        unit = "months",
        format = "yyyy年MM月"
    )
    val subtractResult = dateTimeTool.execute(subtractInput)
    println("3个月前: ${subtractResult.result}")

    // 日期时间差异
    val date1 = LocalDateTime.of(2023, 1, 1, 0, 0)
    val date2 = LocalDateTime.of(2023, 12, 31, 23, 59)
    val differenceInput = ZodDateTimeTool.DateTimeInput(
        operation = "difference",
        datetime = date1.toString(),
        targetDatetime = date2.toString(),
        unit = "days"
    )
    val differenceResult = dateTimeTool.execute(differenceInput)
    println("2023年1月1日到12月31日相差: ${differenceResult.result} 天")

    // 时区转换
    val convertInput = ZodDateTimeTool.DateTimeInput(
        operation = "convert",
        datetime = now.toString(),
        timezone = "UTC",
        targetTimezone = "Asia/Tokyo",
        format = "yyyy-MM-dd HH:mm:ss z"
    )
    val convertResult = dateTimeTool.execute(convertInput)
    println("从UTC转换到东京时间: ${convertResult.result}")

    // 创建代理
    val dateTimeAgent = agent {
        name = "日期时间助手"
        instructions = """
            你是一个日期时间助手，可以帮助用户处理各种日期时间相关的操作。
            
            你可以使用日期时间工具执行以下操作：
            - 获取当前日期时间（current）
            - 格式化日期时间（format）
            - 解析日期时间（parse）
            - 日期时间加法（add）
            - 日期时间减法（subtract）
            - 计算日期时间差异（difference）
            - 时区转换（convert）
            
            当用户要求你执行日期时间操作时，使用日期时间工具。
            始终以友好、专业的方式回应用户。
            解释你的操作过程，并提供清晰的结果。
        """.trimIndent()

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 添加工具
        tools {
            tool(DateTimeToolFactory.createTool())
        }
    }

    // 使用代理
    println("\n开始与代理交互...")

    // 定义示例问题列表
    val exampleQuestions = listOf(
        "现在的日期和时间是什么？",
        "请以'yyyy年MM月dd日'格式显示当前日期",
        "纽约现在是几点？",
        "今天加上30天是什么日期？",
        "2023年1月1日和2023年12月31日相差多少天？",
        "北京时间下午3点在伦敦是几点？"
    )

    println("\n正在使用示例问题进行演示...")

    // 使用示例问题
    for (question in exampleQuestions) {
        println("\n示例问题: $question")
        println("思考中...")

        try {
            val response = dateTimeAgent.generate(question)
            println("代理: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }
}
