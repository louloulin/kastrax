package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.core.tools.ToolFactory
import ai.kastrax.core.tools.math.ZodCalculatorTool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * ZodCalculatorTool 示例，展示如何使用基于 ZodTool 实现的计算器工具执行数学运算。
 */
fun main() = runBlocking {
    println("KastraX ZodCalculatorTool 示例")
    println("----------------------------")

    // 创建计算器工具
    val calculatorTool = ToolFactory.createCalculatorTool(useZod = true)
    
    // 直接使用 ZodTool 接口
    val zodCalculatorTool = ZodCalculatorTool.createZodTool()
    
    println("\n直接使用 ZodTool 接口:")
    
    // 加法示例
    val addInput = ZodCalculatorTool.CalculatorInput(
        operation = "add",
        a = 25.0,
        b = 17.0
    )
    val addResult = zodCalculatorTool.execute(addInput)
    println("加法结果: ${addResult.expression}")
    
    // 乘法示例
    val multiplyInput = ZodCalculatorTool.CalculatorInput(
        operation = "multiply",
        a = 12.0,
        b = 8.0
    )
    val multiplyResult = zodCalculatorTool.execute(multiplyInput)
    println("乘法结果: ${multiplyResult.expression}")
    
    // 平方根示例
    val sqrtInput = ZodCalculatorTool.CalculatorInput(
        operation = "sqrt",
        a = 144.0
    )
    val sqrtResult = zodCalculatorTool.execute(sqrtInput)
    println("平方根结果: ${sqrtResult.expression}")
    
    // 使用传统 Tool 接口
    println("\n使用传统 Tool 接口:")
    
    // 加法示例
    val addJsonInput = buildJsonObject {
        put("operation", "add")
        put("a", 25.0)
        put("b", 17.0)
    }
    val addJsonResult = calculatorTool.execute(addJsonInput)
    println("加法结果: $addJsonResult")
    
    // 创建代理
    val mathAgent = agent {
        name = "数学助手"
        instructions = """
            你是一个数学助手，可以帮助用户执行各种数学计算。
            
            你可以使用计算器工具执行以下操作：
            - 加法 (add)
            - 减法 (subtract)
            - 乘法 (multiply)
            - 除法 (divide)
            - 幂运算 (power)
            - 平方根 (sqrt)
            - 三角函数 (sin, cos, tan)
            - 对数 (log10, ln)
            
            当用户要求你执行计算时，使用计算器工具。
            始终以友好、专业的方式回应用户。
            解释你的计算过程，并提供清晰的结果。
        """.trimIndent()

        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 添加工具
        tools {
            tool(calculatorTool)
        }
    }

    // 使用代理
    println("\n开始与代理交互...")

    // 定义示例问题列表
    val exampleQuestions = listOf(
        "计算 25 + 17 的结果",
        "计算 100 - 45 的结果",
        "计算 12 乘以 8 的结果",
        "计算 144 除以 12 的结果",
        "计算 2 的 10 次方",
        "计算 144 的平方根",
        "计算 sin(90) 的值",
        "计算 log10(1000) 的值"
    )

    println("\n正在使用示例问题进行演示...")

    // 使用示例问题
    for (question in exampleQuestions) {
        println("\n示例问题: $question")
        println("思考中...")

        try {
            val response = mathAgent.generate(question)
            println("代理: ${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }
}
