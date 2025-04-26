package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.datetime.DateTimeToolFactory
import ai.kastrax.core.tools.datetime.ZodDateTimeTool
import ai.kastrax.core.tools.math.ZodCalculatorTool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

/**
 * ZodTools 综合示例，展示如何使用多个 ZodTools。
 */
fun main() = runBlocking {
    println("KastraX ZodTools 综合示例")
    println("------------------------")

    // 创建计算器工具
    val calculatorTool = ZodCalculatorTool.createZodTool()
    
    // 创建日期时间工具
    val dateTimeTool = DateTimeToolFactory.createZodTool()

    println("\n直接使用 ZodTools:")
    
    // 使用计算器工具
    val calcInput = ZodCalculatorTool.CalculatorInput(
        operation = "add",
        a = 25.0,
        b = 17.0
    )
    val calcResult = calculatorTool.execute(calcInput)
    println("计算结果: ${calcResult.expression}")
    
    // 使用日期时间工具
    val now = LocalDateTime.now()
    val dateTimeInput = ZodDateTimeTool.DateTimeInput(
        operation = "add",
        datetime = now.toString(),
        amount = 7,
        unit = "days",
        format = "yyyy-MM-dd"
    )
    val dateTimeResult = dateTimeTool.execute(dateTimeInput)
    println("日期时间结果: ${dateTimeResult.result}")

    // 创建多工具代理
    val multiToolAgent = agent {
        name = "多功能助手"
        instructions = """
            你是一个多功能助手，可以帮助用户执行各种操作，包括数学计算和日期时间处理。
            
            你可以使用以下工具：
            1. 计算器工具 - 执行各种数学运算，包括基本运算、三角函数和对数
            2. 日期时间工具 - 处理日期时间相关操作，包括获取当前时间、格式化、计算等
            
            当用户要求你执行计算时，使用计算器工具。
            当用户要求你处理日期时间时，使用日期时间工具。
            始终以友好、专业的方式回应用户。
        """.trimIndent()

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 添加工具
        tools {
            tool(calculatorTool.toTool())
            tool(dateTimeTool.toTool())
        }
    }

    // 使用代理
    println("\n开始与多功能代理交互...")

    // 定义示例问题列表
    val exampleQuestions = listOf(
        "计算 25 + 17 的结果",
        "现在的日期和时间是什么？",
        "计算 144 的平方根",
        "今天加上30天是什么日期？",
        "计算 sin(90) 的值",
        "北京时间下午3点在伦敦是几点？"
    )

    println("\n正在使用示例问题进行演示...")

    // 使用示例问题
    for (question in exampleQuestions) {
        println("\n示例问题: $question")
        println("思考中...")

        try {
            val response = multiToolAgent.generate(question)
            println("代理: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }
}
