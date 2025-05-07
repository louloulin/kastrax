package ai.kastrax.examples.agent

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.autonomy.AgentAutonomy
import ai.kastrax.core.agent.autonomy.AutonomyCapability
import ai.kastrax.core.agent.autonomy.AutonomyConfig
import ai.kastrax.core.agent.autonomy.AutonomyLevel
import ai.kastrax.core.agent.autonomy.AutonomyMode
import ai.kastrax.core.agent.autonomy.CreativeAgent
import ai.kastrax.core.agent.autonomy.CreativityType
import ai.kastrax.core.agent.autonomy.GoalStatus
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

/**
 * 创造性Agent示例
 */
fun main() = runBlocking {
    println("KastraX 创造性Agent示例")
    println("-------------------")

    // 创建基础Agent
    val baseAgent = agent {
        name = "CreativeAssistant"
        instructions = "You are a helpful and creative assistant. You excel at generating creative ideas, exploring new concepts, and providing innovative solutions."
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 使用API密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
    }

    // 创建自主性配置
    val autonomyConfig = AutonomyConfig(
        mode = AutonomyMode.CREATIVE,
        level = AutonomyLevel.HIGH,
        creativityLevel = 0.8,
        explorationRate = 0.6,
        enabledCapabilities = setOf(
            AutonomyCapability.SELF_REFLECTION,
            AutonomyCapability.IDEA_GENERATION,
            AutonomyCapability.CREATIVITY,
            AutonomyCapability.EXPLORATION,
            AutonomyCapability.GOAL_SETTING
        ),
        constraints = listOf(
            "生成的内容必须是原创的",
            "生成的内容必须是有用的",
            "生成的内容必须是适合所有年龄段的"
        )
    )

    // 创建创造性Agent
    val creativeAgent = CreativeAgent(baseAgent, autonomyConfig)

    // 获取自主性管理器
    val autonomy = creativeAgent.getAutonomy()

    println("\n1. 标准响应生成")
    println("-------------------")
    println("响应: ")
    // 生成标准响应
    val response = creativeAgent.generate("What is creativity?")
    println(response.text)

    println("\n2. 创意内容生成")
    println("-------------------")
    println("组合式创意（结合现有概念）:")
    // 生成组合式创意内容
    val combinationalPrompt = "Create a new sport that combines elements of chess and basketball"
    val combinationalContent = autonomy.generateCreativeContent(combinationalPrompt, CreativityType.COMBINATIONAL)
    println(combinationalContent.content)

    println("\n探索式创意（探索现有概念空间）:")
    // 生成探索式创意内容
    val exploratoryPrompt = "Reimagine how education could work in the digital age"
    val exploratoryContent = autonomy.generateCreativeContent(exploratoryPrompt, CreativityType.EXPLORATORY)
    println(exploratoryContent.content)

    println("\n转换式创意（改变概念空间）:")
    // 生成转换式创意内容
    val transformationalPrompt = "Invent a completely new form of art that doesn't exist today"
    val transformationalContent = autonomy.generateCreativeContent(transformationalPrompt, CreativityType.TRANSFORMATIONAL)
    println(transformationalContent.content)

    println("\n3. 自主探索")
    println("-------------------")
    println("探索主题: 'The future of work'")
    println("探索结果:")

    // 进行自主探索
    val explorationResults = autonomy.exploreAutonomously("The future of work", 2)
    explorationResults.forEach { result ->
        println("- $result")
    }

    println("\n4. 自主目标设定")
    println("-------------------")
    // 设置自主目标
    val goal1 = autonomy.setAutonomousGoal("Generate innovative solutions for climate change", 3)
    val goal2 = autonomy.setAutonomousGoal("Explore new storytelling techniques", 2)
    val goal3 = autonomy.setAutonomousGoal("Learn about emerging technologies", 1)

    // 显示目标
    println("设定的目标:")
    autonomy.getGoals().forEach { goal ->
        println("- ${goal.description} (优先级: ${goal.priority})")
    }

    // 完成一个目标
    autonomy.updateGoalStatus(goal2.id, GoalStatus.COMPLETED)
    println("\n更新后的活跃目标:")
    autonomy.getGoals().filter { it.status != GoalStatus.COMPLETED }.forEach { goal ->
        println("- ${goal.description} (优先级: ${goal.priority})")
    }

    println("\n5. 自主行为历史")
    println("-------------------")
    println("Agent的自主行为历史:")
    autonomy.getBehaviorHistory().forEach { behavior ->
        println("- ${behavior.timestamp}: ${behavior.type} - ${behavior.description}")
    }

    println("\n6. 创意内容历史")
    println("-------------------")
    println("Agent的创意内容历史:")
    autonomy.getCreativeHistory().forEach { content ->
        println("- ${content.timestamp}: ${content.type} - ${content.content.take(50)}...")
    }
}


