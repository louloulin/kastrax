package ai.kastrax.examples.memory

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import ai.kastrax.memory.impl.EnhancedMemoryBuilder
import ai.kastrax.memory.impl.InMemoryWorkingMemory
import ai.kastrax.memory.impl.SimpleMessage
import kotlinx.coroutines.runBlocking

/**
 * 工作内存示例，展示如何使用工作内存功能。
 */
fun main() = runBlocking {
    println("KastraX 工作内存示例")
    println("-------------------")

    // 创建增强型内存，启用工作内存
    val enhancedMemory = EnhancedMemoryBuilder()
        .lastMessages(10)
        .workingMemory(
            WorkingMemoryConfig(
                enabled = true,
                mode = WorkingMemoryMode.TEXT_STREAM,
                template = """
                    # 用户信息
                    - 姓名: 未知
                    - 位置: 未知
                    - 偏好: 未知

                    # 对话上下文
                    - 主题: 人工智能
                    - 目标: 学习
                """.trimIndent()
            )
        )
        .build()

    // 创建使用工作内存的Agent
    val agent = agent {
        name = "WorkingMemoryAgent"
        instructions = """
            你是一个具有工作内存能力的助手。你可以记住用户的信息和对话上下文。

            请注意工作内存中的信息，并在回答时参考这些信息。如果你了解到用户的新信息，
            请更新工作内存中的相应部分。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
        memory = enhancedMemory
    }

    // 创建一个新的对话线程
    val threadId = enhancedMemory.createThread("工作内存示例对话")
    println("创建了新的对话线程: $threadId")

    // 模拟对话
    val conversation = listOf(
        "你好，我叫张三。",
        "我住在北京，我对人工智能和机器学习很感兴趣。",
        "你能推荐一些关于深度学习的入门资料吗？",
        "谢谢你的推荐。我还对自然语言处理特别感兴趣。"
    )

    for (userMessage in conversation) {
        println("\n用户: $userMessage")

        // 保存用户消息
        enhancedMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = userMessage
            ),
            threadId
        )

        // 生成回复
        val response = agent.generate(userMessage, threadId = threadId)
        println("助手: ${response.content}")

        // 查看当前工作内存
        val workingMemory = enhancedMemory.getWorkingMemorySystemMessage(threadId)
        println("\n当前工作内存:")
        println(workingMemory?.substringAfter("# 工作内存\n以下是你的工作内存，包含关于用户和对话的重要信息。请在回答时参考这些信息。\n\n") ?: "无工作内存")
    }

    // 使用工具调用模式
    println("\n\n工具调用模式示例")
    println("-------------------")

    // 创建使用工具调用模式的工作内存
    val toolCallMemory = EnhancedMemoryBuilder()
        .lastMessages(10)
        .workingMemory(
            WorkingMemoryConfig(
                enabled = true,
                mode = WorkingMemoryMode.TOOL_CALL,
                template = """
                    # 用户信息
                    - 姓名: 未知
                    - 位置: 未知
                    - 偏好: 未知
                """.trimIndent()
            )
        )
        .build()

    // 创建使用工具调用模式的Agent
    val toolCallAgent = agent {
        name = "ToolCallMemoryAgent"
        instructions = """
            你是一个具有工作内存能力的助手。你可以使用update_working_memory工具来更新工作内存。

            当你了解到用户的新信息时，请使用update_working_memory工具更新工作内存。
            确保保留工作内存的结构，只更新相关信息。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
        memory = toolCallMemory

        // 添加工作内存工具
        tools {
            val workingMemoryTools = toolCallMemory.getWorkingMemoryTools()
            workingMemoryTools.forEach { (name, tool) ->
                tool(name, tool)
            }
        }
    }

    // 创建一个新的对话线程
    val toolCallThreadId = toolCallMemory.createThread("工具调用工作内存示例对话")
    println("创建了新的对话线程: $toolCallThreadId")

    // 模拟对话
    val toolCallConversation = listOf(
        "你好，我叫李四。",
        "我住在上海，我喜欢阅读科幻小说。",
        "我最近在学习编程，特别是Python和机器学习。"
    )

    for (userMessage in toolCallConversation) {
        println("\n用户: $userMessage")

        // 保存用户消息
        toolCallMemory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = userMessage
            ),
            toolCallThreadId
        )

        // 生成回复
        val response = toolCallAgent.generate(userMessage, threadId = toolCallThreadId)
        println("助手: ${response.content}")

        // 查看当前工作内存
        val workingMemory = toolCallMemory.getWorkingMemorySystemMessage(toolCallThreadId)
        println("\n当前工作内存:")
        println(workingMemory?.substringAfter("# 工作内存\n你可以使用update_working_memory工具来更新工作内存。当前工作内存内容：\n\n") ?: "无工作内存")
    }
}
