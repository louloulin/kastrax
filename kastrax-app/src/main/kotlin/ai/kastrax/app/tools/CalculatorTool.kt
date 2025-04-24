package ai.kastrax.app.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.script.ScriptEngineManager

private val logger = KotlinLogging.logger {}

/**
 * 计算器工具。
 * 用于执行数学计算。
 */
val calculatorTool = tool("calculator") {
    description = "一个简单的计算器工具，可以执行基本的数学计算"
    
    // 定义输入参数
    parameters {
        parameter("expression", "string", "要计算的数学表达式", true)
    }
    
    // 执行计算
    execute { input ->
        try {
            val expression = input.get("expression").asString()
            logger.info { "计算表达式: $expression" }
            
            val result = evaluateExpression(expression)
            logger.info { "计算结果: $result" }
            
            mapOf("result" to result)
        } catch (e: Exception) {
            logger.error(e) { "计算表达式时发生错误" }
            mapOf("error" to "计算错误: ${e.message}")
        }
    }
}

/**
 * 计算数学表达式。
 */
private fun evaluateExpression(expression: String): Double {
    // 清理表达式，移除不安全的代码
    val cleanExpression = expression
        .replace(Regex("[a-zA-Z]"), "") // 移除字母
        .replace(Regex("\\s+"), "") // 移除空白字符
    
    // 使用 JavaScript 引擎计算表达式
    val engine = ScriptEngineManager().getEngineByName("JavaScript")
    return engine.eval(cleanExpression).toString().toDouble()
}
