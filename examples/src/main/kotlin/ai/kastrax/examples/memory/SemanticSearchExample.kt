package ai.kastrax.examples.memory

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.SemanticRecallConfig
import ai.kastrax.memory.impl.InMemoryVectorStorage
import ai.kastrax.memory.impl.SimpleEmbeddingGenerator
import ai.kastrax.memory.impl.SemanticSearchFactory
import ai.kastrax.memory.impl.SimpleMessage
import ai.kastrax.memory.impl.enhancedMemory
import kotlinx.coroutines.runBlocking

/**
 * 语义搜索示例，展示如何使用语义搜索和相关性排序功能。
 */
fun main(): kotlin.Unit = runBlocking {
    // 替换为你的Deepseek API密钥
    val apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "test-api-key"

    println("KastraX 语义搜索示例 (Deepseek版)")
    println("-------------------")

    // 创建简单嵌入生成器（不需要API密钥）
    val embeddingGenerator = SimpleEmbeddingGenerator()

    // 创建向量存储
    val vectorStorage = InMemoryVectorStorage()

    // 创建标准重排序器
    val reranker = SemanticSearchFactory.createStandardReranker()

    // 创建增强型内存，启用语义搜索和混合搜索
    var memory = enhancedMemory {
        lastMessages(10)
        semanticRecall(true)
        embeddingGenerator(embeddingGenerator)
        vectorStorage(vectorStorage)
        hybridSearch(true)
        searchWeights(0.3f, 0.7f)
        reranker(reranker)
    }

    // 创建使用语义搜索的Agent
    val agent = agent {
        name = "SemanticSearchAgent"
        instructions = """
            你是一个具有语义搜索能力的助手。你可以记住对话历史，并使用语义搜索找到相关信息。

            当用户询问之前讨论过的内容时，你可以使用语义搜索找到相关的历史消息，并基于这些信息回答问题。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(apiKey)
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }
        memory = memory
    }

    // 创建一个新的对话线程
    val threadId = memory.createThread("语义搜索示例对话")
    println("创建了新的对话线程: $threadId")

    // 模拟一个包含多个主题的对话
    val conversation = listOf(
        "你好，我想了解一下人工智能的基础知识。",
        "人工智能有哪些主要的应用领域？",
        "机器学习和深度学习有什么区别？",
        "我最近在学习Python，有什么好的学习资源推荐吗？",
        "Python中的列表和元组有什么区别？",
        "我对自然语言处理很感兴趣，它是如何工作的？",
        "BERT和GPT这两种模型有什么不同？",
        "我想了解一下区块链技术的基本原理。",
        "区块链和传统数据库有什么区别？",
        "智能合约是什么，它有哪些应用？",
        "我想回到之前的话题，你能告诉我更多关于深度学习的知识吗？",
        "卷积神经网络适合解决什么类型的问题？",
        "我们之前讨论过Python，你能再给我一些进阶的Python学习建议吗？"
    )

    // 模拟对话
    for (userMessage in conversation) {
        println("\n用户: $userMessage")

        // 保存用户消息
        memory.saveMessage(
            SimpleMessage(
                role = MessageRole.USER,
                content = userMessage
            ),
            threadId
        )

        // 生成回复
        val response = agent.generate(userMessage, AgentGenerateOptions(threadId = threadId))
        println("助手: ${response.text}")
    }

    // 测试语义搜索
    println("\n\n测试语义搜索功能")
    println("-------------------")

    val queries = listOf(
        "我们讨论过哪些关于Python的内容？",
        "深度学习和机器学习的区别是什么？",
        "区块链技术的主要特点是什么？"
    )

    for (query in queries) {
        println("\n搜索查询: $query")

        // 执行语义搜索
        val results = memory.semanticSearch(
            query = query,
            threadId = threadId,
            config = SemanticRecallConfig(topK = 3, minScore = 0.5f)
        )

        println("找到 ${results.size} 条相关消息:")
        results.forEachIndexed { index, result ->
            println("${index + 1}. 相似度: ${result.score}")
            println("   角色: ${result.message.message.role}")
            println("   内容: ${result.message.message.content}")
            println()
        }
    }

    // 关闭嵌入生成器
    // SimpleEmbeddingGenerator不需要关闭
}
