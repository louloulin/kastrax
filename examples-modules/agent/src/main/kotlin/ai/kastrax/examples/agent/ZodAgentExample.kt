package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.memory.MemoryFactory
import ai.kastrax.core.tools.zodTool
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.zod.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ZodTool Agent 示例
 * 
 * 这个示例展示了如何创建一个使用 ZodTool 的 Agent
 */
fun main() = runBlocking {
    println("ZodTool Agent 示例")
    println("------------------")
    
    // 创建计算器工具
    val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "calculator"
        name = "计算器"
        description = "执行基本的数学运算（加、减、乘、除）"
        
        // 定义输入模式
        inputSchema = objectInput("计算器输入") {
            stringField("operation", "要执行的运算") {
                enum("add", "subtract", "multiply", "divide")
                description = "支持的运算：add（加法）、subtract（减法）、multiply（乘法）、divide（除法）"
            }
            numberField("a", "第一个操作数") {
                description = "运算的第一个数字"
            }
            numberField("b", "第二个操作数") {
                description = "运算的第二个数字"
            }
        }
        
        // 定义输出模式
        outputSchema = objectOutput("计算器输出") {
            numberField("result", "运算结果") {
                description = "数学运算的结果"
            }
            stringField("expression", "运算表达式") {
                description = "格式化的运算表达式"
            }
        }
        
        // 实现执行逻辑
        execute = { input ->
            val operation = input["operation"] as String
            val a = (input["a"] as Number).toDouble()
            val b = (input["b"] as Number).toDouble()
            
            val result = when (operation) {
                "add" -> a + b
                "subtract" -> a - b
                "multiply" -> a * b
                "divide" -> a / b
                else -> throw IllegalArgumentException("不支持的运算: $operation")
            }
            
            val expressionSymbol = when (operation) {
                "add" -> "+"
                "subtract" -> "-"
                "multiply" -> "*"
                "divide" -> "/"
                else -> "?"
            }
            
            val expression = "$a $expressionSymbol $b = $result"
            
            mapOf(
                "result" to result,
                "expression" to expression
            )
        }
    }
    
    // 创建日期时间工具
    val dateTimeTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "datetime"
        name = "日期时间工具"
        description = "获取当前日期和时间，或者执行日期时间格式化"
        
        // 定义输入模式
        inputSchema = objectInput("日期时间输入") {
            stringField("action", "要执行的操作") {
                enum("current", "format", "add", "subtract")
                description = "支持的操作：current（获取当前时间）、format（格式化日期时间）、add（添加时间）、subtract（减去时间）"
            }
            stringField("datetime", "日期时间字符串") {
                description = "ISO 格式的日期时间字符串，例如 2023-01-01T12:00:00"
                optional = true
            }
            stringField("format", "格式化模式") {
                description = "日期时间格式化模式，例如 yyyy-MM-dd HH:mm:ss"
                optional = true
            }
            numberField("amount", "时间量") {
                description = "要添加或减去的时间量"
                optional = true
            }
            stringField("unit", "时间单位") {
                enum("days", "hours", "minutes", "seconds")
                description = "时间单位：days（天）、hours（小时）、minutes（分钟）、seconds（秒）"
                optional = true
            }
        }
        
        // 定义输出模式
        outputSchema = objectOutput("日期时间输出") {
            stringField("datetime", "日期时间") {
                description = "ISO 格式的日期时间"
            }
            stringField("formatted", "格式化的日期时间") {
                description = "按指定格式格式化的日期时间"
                optional = true
            }
        }
        
        // 实现执行逻辑
        execute = { input ->
            val action = input["action"] as String
            
            when (action) {
                "current" -> {
                    val now = LocalDateTime.now()
                    val format = input["format"] as? String
                    
                    val result = mutableMapOf<String, Any?>(
                        "datetime" to now.format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                    
                    if (format != null) {
                        result["formatted"] = now.format(DateTimeFormatter.ofPattern(format))
                    }
                    
                    result
                }
                "format" -> {
                    val datetimeStr = input["datetime"] as? String
                        ?: throw IllegalArgumentException("format 操作需要提供 datetime 参数")
                    val format = input["format"] as? String
                        ?: throw IllegalArgumentException("format 操作需要提供 format 参数")
                    
                    val datetime = LocalDateTime.parse(datetimeStr, DateTimeFormatter.ISO_DATE_TIME)
                    
                    mapOf(
                        "datetime" to datetime.format(DateTimeFormatter.ISO_DATE_TIME),
                        "formatted" to datetime.format(DateTimeFormatter.ofPattern(format))
                    )
                }
                "add" -> {
                    val datetimeStr = input["datetime"] as? String
                        ?: throw IllegalArgumentException("add 操作需要提供 datetime 参数")
                    val amount = (input["amount"] as? Number)?.toLong()
                        ?: throw IllegalArgumentException("add 操作需要提供 amount 参数")
                    val unit = input["unit"] as? String
                        ?: throw IllegalArgumentException("add 操作需要提供 unit 参数")
                    
                    val datetime = LocalDateTime.parse(datetimeStr, DateTimeFormatter.ISO_DATE_TIME)
                    
                    val result = when (unit) {
                        "days" -> datetime.plusDays(amount)
                        "hours" -> datetime.plusHours(amount)
                        "minutes" -> datetime.plusMinutes(amount)
                        "seconds" -> datetime.plusSeconds(amount)
                        else -> throw IllegalArgumentException("不支持的时间单位: $unit")
                    }
                    
                    val format = input["format"] as? String
                    val resultMap = mutableMapOf<String, Any?>(
                        "datetime" to result.format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                    
                    if (format != null) {
                        resultMap["formatted"] = result.format(DateTimeFormatter.ofPattern(format))
                    }
                    
                    resultMap
                }
                "subtract" -> {
                    val datetimeStr = input["datetime"] as? String
                        ?: throw IllegalArgumentException("subtract 操作需要提供 datetime 参数")
                    val amount = (input["amount"] as? Number)?.toLong()
                        ?: throw IllegalArgumentException("subtract 操作需要提供 amount 参数")
                    val unit = input["unit"] as? String
                        ?: throw IllegalArgumentException("subtract 操作需要提供 unit 参数")
                    
                    val datetime = LocalDateTime.parse(datetimeStr, DateTimeFormatter.ISO_DATE_TIME)
                    
                    val result = when (unit) {
                        "days" -> datetime.minusDays(amount)
                        "hours" -> datetime.minusHours(amount)
                        "minutes" -> datetime.minusMinutes(amount)
                        "seconds" -> datetime.minusSeconds(amount)
                        else -> throw IllegalArgumentException("不支持的时间单位: $unit")
                    }
                    
                    val format = input["format"] as? String
                    val resultMap = mutableMapOf<String, Any?>(
                        "datetime" to result.format(DateTimeFormatter.ISO_DATE_TIME)
                    )
                    
                    if (format != null) {
                        resultMap["formatted"] = result.format(DateTimeFormatter.ofPattern(format))
                    }
                    
                    resultMap
                }
                else -> throw IllegalArgumentException("不支持的操作: $action")
            }
        }
    }
    
    // 创建内存
    val memory = MemoryFactory.createMemory {
        storage(MemoryFactory.createInMemoryStorage())
        lastMessages(10)
    }
    
    // 创建代理
    val assistant = agent {
        name = "助手"
        instructions = """
            你是一个有用的助手，可以帮助用户执行数学计算和处理日期时间。
            
            你可以使用以下工具：
            1. 计算器工具 - 执行基本的数学运算（加、减、乘、除）
            2. 日期时间工具 - 获取当前日期和时间，或者执行日期时间格式化
            
            请根据用户的请求使用适当的工具，并以友好、专业的方式回应。
            
            当用户请求数学计算时，使用计算器工具。
            当用户询问日期或时间时，使用日期时间工具。
            
            始终以中文回应用户。
        """.trimIndent()
        
        model = openAi("gpt-4o")
        
        tools {
            add(calculatorTool.toTool())
            add(dateTimeTool.toTool())
        }
        
        this.memory = memory
    }
    
    // 创建线程
    val threadId = memory.createThread("ZodTool Agent 示例")
    
    // 测试代理
    val testQueries = listOf(
        "你好，请帮我计算 15 + 27 是多少？",
        "现在的日期和时间是什么？",
        "请帮我计算 45 乘以 12",
        "如果现在加上 3 天是什么时候？请以 yyyy年MM月dd日 格式显示",
        "谢谢你的帮助！"
    )
    
    for (query in testQueries) {
        println("\n用户: $query")
        
        // 生成回复
        val response = assistant.generate(query, threadId = threadId)
        
        // 打印回复
        println("助手: ${response.content}")
    }
    
    println("\nZodTool Agent 示例完成")
}
