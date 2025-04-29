package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import ai.kastrax.actor.actorAgent
import ai.kastrax.actor.askMessage
import ai.kastrax.actor.sendMessage
import ai.kastrax.actor.streamMessage
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes

/**
 * 基本使用示例
 */
fun main() = runBlocking {
    // 创建 Actor 系统
    val system = ActorSystem("kastrax-system")
    
    // 创建 Actor 化的 Agent，直接复用现有的 agent DSL
    val agentPid = system.actorAgent {
        // 这部分是现有的 kastrax agent DSL
        agent {
            name = "助手"
            instructions = "你是一个有帮助的助手。"
            model = ai.kastrax.integrations.deepseek.deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            }
            tools {
                tool("calculator") {
                    description = "执行数学计算"
                    input {
                        field("expression", string()) {
                            description = "要计算的表达式"
                        }
                    }
                    output {
                        field("result", number()) {
                            description = "计算结果"
                        }
                    }
                    execute { input ->
                        val expression = input.getString("expression")
                        val result = evaluateExpression(expression)
                        jsonObject {
                            "result" to result
                        }
                    }
                }
            }
        }
        
        // 这部分是 actor 特有的配置
        actor {
            // actor 特有的配置，如监督策略、邮箱类型等
            oneForOneStrategy {
                maxRetries = 3
                withinTimeRange = 1.minutes
            }
            unboundedMailbox()
        }
    }
    
    // 发送消息
    system.sendMessage(agentPid, "你能帮我计算 2 + 2 吗？")
    
    // 请求-响应模式
    val response = system.askMessage(agentPid, "巴黎的人口是多少？")
    println("回答: $response")
    
    // 流式请求
    system.streamMessage(agentPid, "讲个故事") { chunk ->
        print(chunk)
    }
    
    // 关闭系统
    system.shutdown()
}

/**
 * 简单的表达式计算函数
 */
private fun evaluateExpression(expression: String): Double {
    return try {
        val sanitized = expression.replace("[^0-9+\\-*/().\\s]".toRegex(), "")
        javax.script.ScriptEngineManager().getEngineByName("JavaScript").eval(sanitized).toString().toDouble()
    } catch (e: Exception) {
        throw IllegalArgumentException("无法计算表达式: $expression", e)
    }
}
