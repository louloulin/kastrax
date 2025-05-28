package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.architecture.HierarchicalAgent
import ai.kastrax.core.agent.architecture.hierarchicalAgent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

/**
 * 分层Agent示例
 */
fun main() = runBlocking {
    // 创建基础LLM
    val llm = deepSeek {
        // 直接设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)
    }

    // 使用DSL创建协调器Agent
    val coordinator = agent {
        name = "协调器Agent"
        model = llm
        instructions = """
            你是一个协调器Agent，负责分析用户请求并决定如何处理。
            你可以自己回答问题，或者将任务委派给专业的子Agent。
            当面对复杂问题时，你应该将任务分解为多个子任务，并委派给适当的子Agent。
        """.trimIndent()
    }

    // 使用DSL创建技术专家Agent
    val techExpert = agent {
        name = "技术专家Agent"
        model = llm
        instructions = """
            你是一个技术专家Agent，专注于回答技术相关的问题。
            你对编程语言、软件开发、人工智能、数据科学等技术领域有深入的了解。
            提供准确、详细且实用的技术信息。
        """.trimIndent()
    }

    // 使用DSL创建创意专家Agent
    val creativeExpert = agent {
        name = "创意专家Agent"
        model = llm
        instructions = """
            你是一个创意专家Agent，专注于创意和设计相关的问题。
            你对艺术、设计、创意写作、内容创作等领域有丰富的经验。
            提供有创意、有灵感且实用的建议。
        """.trimIndent()
    }

    // 使用DSL创建教育专家Agent
    val educationExpert = agent {
        name = "教育专家Agent"
        model = llm
        instructions = """
            你是一个教育专家Agent，专注于教育和学习相关的问题。
            你对教育理论、学习方法、课程设计等领域有专业知识。
            提供有效的学习建议和教育资源。
        """.trimIndent()
    }

    // 方法1：使用构造函数创建分层Agent
    val hierarchicalAgent1 = HierarchicalAgent(
        coordinator = coordinator,
        subAgents = mapOf(
            "tech" to techExpert,
            "creative" to creativeExpert,
            "education" to educationExpert
        )
    )

    // 方法2：使用DSL创建分层Agent
    val hierarchicalAgent2 = hierarchicalAgent {
        coordinator(coordinator)
        addSubAgent("tech", techExpert)
        addSubAgent("creative", creativeExpert)
        addSubAgent("education", educationExpert)
    }

    println("=== 分层Agent示例 ===")

    // 技术问题示例
    val techPrompt = "请解释Kotlin协程与Java线程的区别，并给出一些实际应用场景。"
    println("\n技术问题: $techPrompt")

    try {
        val techResponse = hierarchicalAgent1.stream(
            prompt = techPrompt,
            options = AgentStreamOptions()
        )

        println("回答:")
        
        // 处理流式文本响应
        techResponse.textStream?.collect { chunk ->
            print(chunk)
            kotlinx.coroutines.delay(30)
        } ?: run {
            print(techResponse.text)
        }
        
        println() // 换行
        
    } catch (e: Exception) {
        println("生成响应时发生错误: ${e.message}")
    }

    // 创意问题示例
    val creativePrompt = "我需要为一个科技初创公司设计logo，有什么创意建议？"
    println("\n创意问题: $creativePrompt")

    try {
        val creativeResponse = hierarchicalAgent1.stream(
            prompt = creativePrompt,
            options = AgentStreamOptions()
        )

        println("回答:")
        
        // 处理流式文本响应
        creativeResponse.textStream?.collect { chunk ->
            print(chunk)
            kotlinx.coroutines.delay(30)
        } ?: run {
            print(creativeResponse.text)
        }
        
        println() // 换行
        
    } catch (e: Exception) {
        println("生成响应时发生错误: ${e.message}")
    }

    // 复杂问题示例（需要多个专家协作）
    val complexPrompt = "我想开发一个编程学习平台，需要考虑技术实现、创意设计和教育内容。请给我一些建议。"
    println("\n复杂问题: $complexPrompt")

    try {
        val complexResponse = hierarchicalAgent1.stream(
            prompt = complexPrompt,
            options = AgentStreamOptions()
        )

        println("回答:")
        
        // 处理流式文本响应
        complexResponse.textStream?.collect { chunk ->
            print(chunk)
            kotlinx.coroutines.delay(30)
        } ?: run {
            print(complexResponse.text)
        }
        
        println() // 换行
        
    } catch (e: Exception) {
        println("生成响应时发生错误: ${e.message}")
    }
}
