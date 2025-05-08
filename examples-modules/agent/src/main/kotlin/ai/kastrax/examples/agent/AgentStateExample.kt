package ai.kastrax.examples.agent

import ai.kastrax.core.agent.*
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * Agent状态管理和会话控制示例
 */
fun agentStateExample() = runBlocking {
    // 创建会话管理器和状态管理器
    val sessionManager = InMemorySessionManager()
    val stateManager = InMemoryStateManager()

    // 创建计算器工具
    val calculatorTool = tool {
        id = "calculator"
        name = "Calculator"
        description = "Perform mathematical calculations"
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("expression") {
                    put("type", "string")
                    put("description", "The mathematical expression to evaluate")
                }
            }
            putJsonArray("required") {
                add("expression")
            }
        }
        execute = { input ->
            val expression = input.toString()
            val result = evaluateExpression(expression)
            buildJsonObject {
                put("result", result)
            }
        }
    }

    // 创建Agent
    val agent = agent {
        name = "StateAwareAgent"
        instructions = "You are a helpful assistant with state management capabilities."
        model = deepSeek {
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
            model(DeepSeekModel.DEEPSEEK_CHAT)
            temperature(0.7)
            maxTokens(2000)
        }

        // 添加工具
        tools {
            tool(calculatorTool)
        }

        // 添加会话管理器和状态管理器
        sessionManager(sessionManager)
        stateManager(stateManager)
    }

    // 创建会话
    println("=== 创建会话 ===")
    val session = agent.createSession(
        title = "数学问题会话",
        resourceId = "user-123",
        metadata = mapOf(
            "category" to "math",
            "difficulty" to "easy"
        )
    )
    println("会话ID: ${session?.id}")
    println("会话标题: ${session?.title}")
    println("会话元数据: ${session?.metadata}")
    println()

    // 获取当前状态
    println("=== 初始状态 ===")
    val initialState = agent.getState()
    println("初始状态: ${initialState?.status ?: "无状态"}")
    println()

    // 生成响应
    println("=== 生成响应 ===")
    println("用户: 计算 25 + 17 的结果")

    // 手动更新状态为思考中
    val thinkingState = agent.updateState(AgentStatus.THINKING)
    println("状态更新为: ${thinkingState?.status}")

    // 生成响应
    val response = agent.generate(
        "计算 25 + 17 的结果",
        AgentGenerateOptions(threadId = session?.id)
    )

    // 打印响应
    println("助手: ${response.text}")
    println("状态: ${response.state?.status}")
    println("工具调用: ${response.toolCalls.size}")
    if (response.toolCalls.isNotEmpty()) {
        println("工具名称: ${response.toolCalls[0].name}")
        println("工具结果: ${response.toolResults[response.toolCalls[0].id]?.result}")
    }
    println()

    // 获取会话消息
    println("=== 会话消息 ===")
    val messages = agent.getSessionMessages(session?.id ?: "")
    println("消息数量: ${messages?.size ?: 0}")
    messages?.forEach { message ->
        println("角色: ${message.message.role}")
        println("内容: ${message.message.content}")
        println("时间: ${message.createdAt}")
        println()
    }

    // 流式生成响应
    println("=== 流式生成响应 ===")
    println("用户: 计算 125 * 37 的结果")

    // 流式生成
    val streamResponse = agent.stream(
        "计算 125 * 37 的结果",
        AgentStreamOptions(threadId = session?.id)
    )

    // 处理流式响应
    print("助手: ")
    streamResponse.textStream?.collect { chunk ->
        print(chunk)
    }
    println("\n")

    // 获取最终状态
    println("=== 最终状态 ===")
    val finalState = agent.getState()
    println("最终状态: ${finalState?.status}")
    println()

    // 重置状态
    println("=== 重置状态 ===")
    agent.reset()
    val resetState = agent.getState()
    println("重置后状态: ${resetState?.status ?: "无状态"}")
}

/**
 * 简单的表达式计算函数
 */
private fun evaluateExpression(expression: String): Int {
    // 这是一个非常简化的计算器，仅用于演示
    val cleanExpr = expression.replace("\"", "").replace("expression=", "")
    val parts = cleanExpr.split("+", "-", "*", "/")
    if (parts.size != 2) return 0

    val a = parts[0].trim().toIntOrNull() ?: 0
    val b = parts[1].trim().toIntOrNull() ?: 0

    return when {
        "+" in cleanExpr -> a + b
        "-" in cleanExpr -> a - b
        "*" in cleanExpr -> a * b
        "/" in cleanExpr -> if (b != 0) a / b else 0
        else -> 0
    }
}
