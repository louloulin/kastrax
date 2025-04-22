package ai.kastrax.examples.memory

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.TokenLimiter
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import ai.kastrax.memory.impl.SimpleEmbeddingGenerator
import ai.kastrax.memory.impl.InMemoryVectorStorage
import ai.kastrax.memory.impl.enhancedMemory
import kotlinx.coroutines.runBlocking

/**
 * 增强型内存系统示例
 */
fun main() = runBlocking {
    // 创建增强型内存
    val memory = enhancedMemory {
        // 设置最近消息数量
        lastMessages(10)
        
        // 启用语义召回
        semanticRecall(true)
        embeddingGenerator(SimpleEmbeddingGenerator())
        vectorStorage(InMemoryVectorStorage())
        
        // 添加令牌限制器
        processor(TokenLimiter(2000))
        
        // 启用工作内存
        workingMemory(
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
                    
                    # 重要信息
                    - 用户对机器学习感兴趣
                """.trimIndent()
            )
        )
    }
    
    // 创建使用增强型内存的Agent
    val agent = agent {
        name = "EnhancedMemoryAgent"
        instructions = """
            你是一个具有增强记忆能力的助手。你可以记住对话历史，并使用工作内存来跟踪重要信息。
            
            请注意工作内存中的信息，并在回答时参考这些信息。
        """.trimIndent()
        model = openAi("gpt-4o")
        memory = memory
    }
    
    // 创建对话线程
    println("=== 创建对话线程 ===")
    val threadId = memory.createThread("增强型内存示例")
    println("线程ID: $threadId")
    println()
    
    // 模拟对话
    println("=== 对话开始 ===")
    
    // 第一轮对话
    println("用户: 你好，我叫张三，我对机器学习很感兴趣。")
    
    // 保存用户消息
    memory.saveMessage(
        Message(role = "user", content = "你好，我叫张三，我对机器学习很感兴趣。"),
        threadId
    )
    
    // 更新工作内存
    (memory as ai.kastrax.memory.impl.EnhancedMemory).workingMemory?.updateWorkingMemory(
        threadId,
        """
        # 用户信息
        - 姓名: 张三
        - 位置: 未知
        - 偏好: 机器学习
        
        # 对话上下文
        - 主题: 机器学习
        - 目标: 学习
        
        # 重要信息
        - 用户对机器学习感兴趣
        """.trimIndent()
    )
    
    // 生成回复
    val response1 = agent.generate(
        "你好，我叫张三，我对机器学习很感兴趣。",
        ai.kastrax.core.agent.AgentGenerateOptions(threadId = threadId)
    )
    
    println("助手: ${response1.text}")
    println()
    
    // 第二轮对话
    println("用户: 我住在北京，想了解一下深度学习。")
    
    // 保存用户消息
    memory.saveMessage(
        Message(role = "user", content = "我住在北京，想了解一下深度学习。"),
        threadId
    )
    
    // 更新工作内存
    (memory as ai.kastrax.memory.impl.EnhancedMemory).workingMemory?.updateWorkingMemory(
        threadId,
        """
        # 用户信息
        - 姓名: 张三
        - 位置: 北京
        - 偏好: 机器学习, 深度学习
        
        # 对话上下文
        - 主题: 深度学习
        - 目标: 学习
        
        # 重要信息
        - 用户对机器学习和深度学习感兴趣
        - 用户居住在北京
        """.trimIndent()
    )
    
    // 生成回复
    val response2 = agent.generate(
        "我住在北京，想了解一下深度学习。",
        ai.kastrax.core.agent.AgentGenerateOptions(threadId = threadId)
    )
    
    println("助手: ${response2.text}")
    println()
    
    // 第三轮对话 - 测试语义搜索
    println("用户: 你还记得我的名字和我住在哪里吗？")
    
    // 保存用户消息
    memory.saveMessage(
        Message(role = "user", content = "你还记得我的名字和我住在哪里吗？"),
        threadId
    )
    
    // 生成回复
    val response3 = agent.generate(
        "你还记得我的名字和我住在哪里吗？",
        ai.kastrax.core.agent.AgentGenerateOptions(threadId = threadId)
    )
    
    println("助手: ${response3.text}")
    println()
    
    // 展示语义搜索功能
    println("=== 语义搜索 ===")
    val searchResults = memory.semanticSearch(
        query = "用户的个人信息",
        threadId = threadId,
        ai.kastrax.memory.api.SemanticRecallConfig(topK = 2)
    )
    
    println("搜索结果数量: ${searchResults.size}")
    searchResults.forEachIndexed { index, result ->
        println("结果 ${index + 1}:")
        println("角色: ${result.message.message.role}")
        println("内容: ${result.message.message.content}")
        println("相似度: ${result.score}")
        println()
    }
    
    // 展示工作内存
    println("=== 工作内存 ===")
    val workingMemory = (memory as ai.kastrax.memory.impl.EnhancedMemory).workingMemory?.getWorkingMemory(threadId)
    println(workingMemory)
}
