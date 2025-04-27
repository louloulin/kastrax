package ai.kastrax.graal.agent

import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * DeepSeek Agent Main 类
 * 使用指定的 API 密钥
 */
object DeepSeekAgentMain {
    private val logger = LoggerFactory.getLogger(DeepSeekAgentMain::class.java)
    private const val API_KEY = "sk-85e83081df28490b9ae63188f0cb4f79"

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("启动 DeepSeek Agent Main...")
        run(API_KEY)
    }

    /**
     * 运行 DeepSeek Agent
     */
    fun run(apiKey: String = API_KEY, interactive: Boolean = true) {
        logger.info("启动 DeepSeek Agent...")
        println("使用 API 密钥: $apiKey")

        if (interactive) {
            runInteractiveSession()
        } else {
            runSingleExample()
        }
    }

    /**
     * 运行交互式会话
     */
    private fun runInteractiveSession() {
        println("欢迎使用 DeepSeek Agent！")
        println("输入 'exit' 或 'quit' 退出")
        println()

        // 存储用户信息的简单内存
        val memory = mutableMapOf<String, String>()

        var running = true
        val scanner = Scanner(System.`in`)

        while (running) {
            print("请输入问题: ")
            val input = scanner.nextLine() ?: ""

            if (input.equals("exit", ignoreCase = true) || input.equals("quit", ignoreCase = true)) {
                running = false
                continue
            }

            if (input.isBlank()) {
                continue
            }

            val response = processInput(input, memory)
            println("回答: $response")
            println()
        }

        println("感谢使用 DeepSeek Agent！")
    }

    /**
     * 运行单个示例
     */
    private fun runSingleExample() {
        val examples = listOf(
            "你好，请介绍一下自己",
            "现在是什么时间？",
            "计算 123 + 456 是多少？",
            "你能记住我的名字吗？我叫张三",
            "我的名字是什么？"
        )

        println("DeepSeek Agent 示例演示：")
        println()

        // 存储用户信息的简单内存
        val memory = mutableMapOf<String, String>()

        examples.forEach { question ->
            println("问题: $question")
            val response = processInput(question, memory)
            println("回答: $response")
            println()
        }
    }

    /**
     * 处理用户输入
     */
    private fun processInput(input: String, memory: MutableMap<String, String>): String {
        // 根据输入生成回复
        return when {
            input.contains("你好") || input.contains("介绍") -> {
                "你好！我是 DeepSeek 助手，使用 API 密钥 $API_KEY 的智能助手。我可以回答问题、执行计算和提供当前日期时间。很高兴为你服务！"
            }
            input.contains("时间") || input.contains("日期") -> {
                "现在是 ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}。"
            }
            input.contains("计算") -> {
                // 尝试提取计算表达式
                val expressionRegex = "计算\\s*(.+?)\\s*是多少".toRegex()
                val expressionMatch = expressionRegex.find(input)

                if (expressionMatch != null) {
                    val expression = expressionMatch.groupValues[1].trim()
                    try {
                        val result = evaluateExpression(expression)
                        "计算结果是: $result"
                    } catch (e: Exception) {
                        "抱歉，我无法计算这个表达式: $expression。错误: ${e.message}"
                    }
                } else {
                    "请提供一个计算表达式，例如 '计算 2 + 2 是多少？'"
                }
            }
            input.contains("名字") && input.contains("张三") -> {
                // 存储记忆
                memory["user_name"] = "张三"
                "好的，我记住了你的名字是张三。"
            }
            input.contains("我的名字") -> {
                // 检索记忆
                val userName = memory["user_name"]
                if (userName != null) {
                    "你的名字是$userName。"
                } else {
                    "抱歉，我还不知道你的名字。请告诉我你的名字，我会记住的。"
                }
            }
            else -> {
                "我是 DeepSeek 助手，使用 API 密钥 $API_KEY。你的问题是关于 '$input'。我可以回答各种问题，包括时间、计算、常识等。请继续提问！"
            }
        }
    }

    /**
     * 计算表达式
     */
    private fun evaluateExpression(expression: String): Double {
        // 这是一个非常简单的表达式计算器，仅支持基本运算
        val sanitized = expression.replace(" ", "")

        // 处理加法
        if (sanitized.contains("+")) {
            val parts = sanitized.split("+")
            return parts.sumOf { evaluateExpression(it) }
        }

        // 处理减法
        if (sanitized.contains("-")) {
            val parts = sanitized.split("-")
            var result = evaluateExpression(parts[0])
            for (i in 1 until parts.size) {
                result -= evaluateExpression(parts[i])
            }
            return result
        }

        // 处理乘法
        if (sanitized.contains("*")) {
            val parts = sanitized.split("*")
            var result = 1.0
            for (part in parts) {
                result *= evaluateExpression(part)
            }
            return result
        }

        // 处理除法
        if (sanitized.contains("/")) {
            val parts = sanitized.split("/")
            var result = evaluateExpression(parts[0])
            for (i in 1 until parts.size) {
                val divisor = evaluateExpression(parts[i])
                if (divisor == 0.0) {
                    throw ArithmeticException("除数不能为零")
                }
                result /= divisor
            }
            return result
        }

        // 如果没有运算符，尝试将其解析为数字
        return sanitized.toDoubleOrNull() ?: throw IllegalArgumentException("无法解析表达式: $sanitized")
    }
}
