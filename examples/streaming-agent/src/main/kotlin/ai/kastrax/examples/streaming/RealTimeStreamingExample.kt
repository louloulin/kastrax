package ai.kastrax.examples.streaming

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.random.Random

/**
 * 实时流式处理示例
 * 展示带工具调用的流式生成
 */
fun main() = runBlocking {
    println("KastraX 实时流式处理示例")
    println("========================")

    // 创建带工具的代理
    val agent = agent {
        name = "Real-time Assistant"
        instructions = """
            你是一个实时助手，可以获取当前时间、天气信息和执行计算。
            请根据用户的请求使用相应的工具，并以流式方式返回结果。
        """.trimIndent()
        
        // 注意：实际使用时需要配置正确的模型
        // model = openAi("gpt-4")
        
        // 添加获取当前时间的工具
        tool {
            name = "getCurrentTime"
            description = "获取当前时间"
            
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            }
            
            execute = { input ->
                val currentTime = System.currentTimeMillis()
                val dateTime = java.time.Instant.ofEpochMilli(currentTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toString()
                
                buildJsonObject {
                    put("timestamp", currentTime)
                    put("datetime", dateTime)
                    put("formatted", java.time.LocalDateTime.now().toString())
                }
            }
        }
        
        // 添加模拟天气查询工具
        tool {
            name = "getWeather"
            description = "获取指定城市的天气信息"
            
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("city", buildJsonObject {
                        put("type", "string")
                        put("description", "城市名称")
                    })
                })
                put("required", buildJsonArray {
                    add("city")
                })
            }
            
            execute = { input ->
                val city = input.jsonObject["city"]?.jsonPrimitive?.content ?: "未知城市"
                
                // 模拟网络延迟
                delay(1000)
                
                // 模拟天气数据
                val temperatures = listOf(15, 18, 22, 25, 28, 30, 32)
                val conditions = listOf("晴朗", "多云", "小雨", "阴天", "雷阵雨")
                
                buildJsonObject {
                    put("city", city)
                    put("temperature", temperatures.random())
                    put("condition", conditions.random())
                    put("humidity", Random.nextInt(30, 90))
                    put("windSpeed", Random.nextInt(5, 25))
                }
            }
        }
        
        // 添加计算工具
        tool {
            name = "calculate"
            description = "执行数学计算"
            
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("expression", buildJsonObject {
                        put("type", "string")
                        put("description", "数学表达式")
                    })
                })
                put("required", buildJsonArray {
                    add("expression")
                })
            }
            
            execute = { input ->
                val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
                
                try {
                    // 简单的计算逻辑（实际应用中应使用更安全的表达式解析器）
                    val result = when {
                        expression.contains("+") -> {
                            val parts = expression.split("+")
                            parts.sumOf { it.trim().toDoubleOrNull() ?: 0.0 }
                        }
                        expression.contains("-") -> {
                            val parts = expression.split("-")
                            (parts.firstOrNull()?.trim()?.toDoubleOrNull() ?: 0.0) - 
                            (parts.drop(1).sumOf { it.trim().toDoubleOrNull() ?: 0.0 })
                        }
                        expression.contains("*") -> {
                            val parts = expression.split("*")
                            parts.fold(1.0) { acc, part -> acc * (part.trim().toDoubleOrNull() ?: 1.0) }
                        }
                        expression.contains("/") -> {
                            val parts = expression.split("/")
                            val first = parts.firstOrNull()?.trim()?.toDoubleOrNull() ?: 0.0
                            val second = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 1.0
                            if (second != 0.0) first / second else Double.NaN
                        }
                        else -> expression.toDoubleOrNull() ?: 0.0
                    }
                    
                    buildJsonObject {
                        put("expression", expression)
                        put("result", result)
                        put("success", true)
                    }
                } catch (e: Exception) {
                    buildJsonObject {
                        put("expression", expression)
                        put("error", e.message ?: "Unknown error")
                        put("success", false)
                    }
                }
            }
        }
    }

    // 创建会话
    val session = agent.createSession()
    println("会话ID: ${session?.id}\n")

    // 示例1：获取当前时间
    println("=== 示例1：获取当前时间 ===")
    println("用户: 现在几点了？")
    
    val timeResponse = agent.stream(
        "现在几点了？",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    timeResponse.textStream?.collect { chunk ->
        print(chunk)
        delay(50)
    }
    println("\n")
    
    // 检查工具调用
    val timeState = agent.getState()
    println("工具调用数: ${timeResponse.toolCalls?.size ?: 0}")
    timeResponse.toolCalls?.forEach { toolCall ->
        println("调用工具: ${toolCall.name}")
        println("工具结果: ${timeResponse.toolResults?.get(toolCall.id)?.result}")
    }
    println()

    // 示例2：查询天气
    println("=== 示例2：查询天气 ===")
    println("用户: 北京今天的天气怎么样？")
    
    val weatherResponse = agent.stream(
        "北京今天的天气怎么样？",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    weatherResponse.textStream?.collect { chunk ->
        print(chunk)
        delay(40)
    }
    println("\n")
    
    // 显示工具调用信息
    println("工具调用数: ${weatherResponse.toolCalls?.size ?: 0}")
    weatherResponse.toolCalls?.forEach { toolCall ->
        println("调用工具: ${toolCall.name}")
        println("工具参数: ${toolCall.arguments}")
        println("工具结果: ${weatherResponse.toolResults?.get(toolCall.id)?.result}")
    }
    println()

    // 示例3：执行计算
    println("=== 示例3：执行计算 ===")
    println("用户: 帮我计算 125 * 37 的结果")
    
    val calcResponse = agent.stream(
        "帮我计算 125 * 37 的结果",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    calcResponse.textStream?.collect { chunk ->
        print(chunk)
        delay(30)
    }
    println("\n")
    
    // 显示计算结果
    println("工具调用数: ${calcResponse.toolCalls?.size ?: 0}")
    calcResponse.toolCalls?.forEach { toolCall ->
        println("调用工具: ${toolCall.name}")
        println("计算表达式: ${toolCall.arguments}")
        println("计算结果: ${calcResponse.toolResults?.get(toolCall.id)?.result}")
    }
    println()

    // 示例4：复合查询
    println("=== 示例4：复合查询 ===")
    println("用户: 现在几点了？上海的天气如何？顺便帮我算一下 50 + 30 * 2")
    
    val complexResponse = agent.stream(
        "现在几点了？上海的天气如何？顺便帮我算一下 50 + 30 * 2",
        AgentStreamOptions(threadId = session?.id)
    )
    
    print("助手: ")
    var chunkCount = 0
    complexResponse.textStream?.collect { chunk ->
        print(chunk)
        chunkCount++
        
        // 每20个chunk显示进度
        if (chunkCount % 20 == 0) {
            print(" [处理中...] ")
        }
        
        delay(35)
    }
    println("\n")
    
    // 显示所有工具调用
    println("总工具调用数: ${complexResponse.toolCalls?.size ?: 0}")
    complexResponse.toolCalls?.forEachIndexed { index, toolCall ->
        println("工具${index + 1}: ${toolCall.name}")
        println("参数: ${toolCall.arguments}")
        println("结果: ${complexResponse.toolResults?.get(toolCall.id)?.result}")
        println()
    }

    // 示例5：实时状态监控
    println("=== 示例5：实时状态监控 ===")
    println("用户: 请告诉我当前时间，然后每隔几秒更新一次天气信息")
    
    // 模拟实时更新
    repeat(3) { round ->
        println("\n--- 第${round + 1}次更新 ---")
        
        val updateResponse = agent.stream(
            "获取当前时间和随机城市的天气",
            AgentStreamOptions(threadId = session?.id)
        )
        
        print("助手: ")
        updateResponse.textStream?.collect { chunk ->
            print(chunk)
            delay(25)
        }
        println("\n")
        
        // 显示更新信息
        updateResponse.toolCalls?.forEach { toolCall ->
            println("更新: ${toolCall.name} -> ${updateResponse.toolResults?.get(toolCall.id)?.result}")
        }
        
        if (round < 2) {
            println("等待下次更新...")
            delay(3000)
        }
    }

    // 获取完整会话历史
    println("\n=== 会话总结 ===")
    val allMessages = agent.getSessionMessages(session?.id ?: "")
    println("总消息数: ${allMessages?.size ?: 0}")
    
    val finalState = agent.getState()
    println("最终状态: ${finalState?.status}")
    
    println("\n实时流式处理示例完成！")
}