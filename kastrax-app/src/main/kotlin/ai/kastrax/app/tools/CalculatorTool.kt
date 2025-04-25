package ai.kastrax.app.tools

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.script.ScriptEngineManager

private val logger = KotlinLogging.logger {}

/**
 * 计算器工具。
 * 用于执行数学计算。
 */
val calculatorTool = tool {
    // 设置 ID 和名称
    id = "calculator"
    name = "计算器"

    // 设置描述
    description = "一个简单的计算器工具，可以执行基本的数学计算"

    // 定义输入模式
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("expression") {
                put("type", "string")
                put("description", "要计算的数学表达式")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("expression"))
        }
    }

    // 执行计算
    execute = { input ->
        try {
            val inputStr = input.toString()
            val expression = if (inputStr.contains("expression")) {
                inputStr.substringAfter("expression").substringAfter(":").substringAfter("\"").substringBefore("\"")
            } else {
                "0"
            }
            logger.info { "计算表达式: $expression" }

            val result = evaluateExpression(expression)
            logger.info { "计算结果: $result" }

            buildJsonObject {
                put("result", result)
            }
        } catch (e: Exception) {
            logger.error(e) { "计算表达式时发生错误" }
            buildJsonObject {
                put("error", "计算错误: ${e.message}")
            }
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
