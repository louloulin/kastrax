package ai.kastrax.a2a.examples

import ai.kastrax.a2a.a2a
import ai.kastrax.a2a.model.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * A2A 任务示例
 */
fun main() = runBlocking {
    // 创建模拟的 kastrax 代理
    val mockAgent = mockk<Agent>()
    
    // 配置模拟代理的行为
    coEvery { mockAgent.name } returns "助手代理"
    coEvery { mockAgent.generate(any<String>()) } returns AgentResponse(
        text = "这是一个模拟响应",
        toolCalls = emptyList()
    )
    
    // 创建 A2A 实例
    val a2aInstance = a2a {
        // 注册 kastrax 代理
        agent(mockAgent)
        
        // 配置服务器
        server {
            port = 8080
            enableCors = true
        }
    }
    
    // 创建任务消息
    val message = Message(
        role = "user",
        parts = listOf(
            TextPart(
                text = "你好，请帮我分析这些数据"
            )
        )
    )
    
    // 创建任务
    val task = a2aInstance.createTask(message)
    println("创建任务: ${task.id}")
    
    // 处理任务
    a2aInstance.processTask("助手代理", task.id)
    
    // 等待任务处理完成
    delay(2000)
    
    // 获取任务状态
    val updatedTask = a2aInstance.getTask(task.id)
    println("任务状态: ${updatedTask?.status?.state}")
    println("任务产物: ${updatedTask?.artifacts?.firstOrNull()?.parts?.filterIsInstance<TextPart>()?.firstOrNull()?.text}")
    
    // 取消任务（已完成的任务无法取消）
    val cancelResult = a2aInstance.cancelTask(task.id)
    println("取消任务结果: ${cancelResult?.status?.state}")
    
    // 停止服务器
    a2aInstance.stopServer()
}
