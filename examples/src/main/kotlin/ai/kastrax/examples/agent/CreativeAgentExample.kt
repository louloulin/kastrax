package ai.kastrax.examples.agent

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.autonomy.*
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

/**
 * 创造性Agent示例
 */
fun main() = runBlocking {
    println("KastraX 创造性Agent示例")
    println("-------------------")

    // 创建模拟LLM提供者
    val mockLlmProvider = MockLlmProvider()

    // 创建基础Agent
    val baseAgent = agent {
        name = "CreativeAssistant"
        instructions = "You are a helpful and creative assistant."
        model = mockLlmProvider
    }

    // 创建自主性管理器
    val autonomy = AgentAutonomy(baseAgent, AutonomyConfig(
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
    ))

    // 创建创造性Agent
    val creativeAgent = CreativeAgent(baseAgent, AutonomyConfig(
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
    ))

    println("\n1. 标准响应生成")
    println("-------------------")
    val standardResponse = creativeAgent.generate("What is creativity?")
    println("响应: ${standardResponse.text}")

    println("\n2. 创意内容生成")
    println("-------------------")
    println("组合式创意（结合现有概念）:")
    val combinationalContent = autonomy.generateCreativeContent(
        "Create a new sport that combines elements of chess and basketball",
        CreativityType.COMBINATIONAL
    )
    println("内容: ${combinationalContent.content}")

    println("\n探索式创意（探索现有概念空间）:")
    val exploratoryContent = autonomy.generateCreativeContent(
        "Reimagine how education could work in the digital age",
        CreativityType.EXPLORATORY
    )
    println("内容: ${exploratoryContent.content}")

    println("\n转换式创意（改变概念空间）:")
    val transformationalContent = autonomy.generateCreativeContent(
        "Invent a completely new form of art that doesn't exist today",
        CreativityType.TRANSFORMATIONAL
    )
    println("内容: ${transformationalContent.content}")

    println("\n3. 自主探索")
    println("-------------------")
    println("探索主题: 'The future of work'")
    val explorationResults = autonomy.exploreAutonomously("The future of work", 2)
    println("探索结果:")
    explorationResults.forEachIndexed { index, result ->
        println("\n探索 ${index + 1}:")
        println(result)
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
    autonomy.getGoals().forEach { goal ->
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

/**
 * 模拟LLM提供者
 */
class MockLlmProvider : LlmProvider {
    override val model: String = "mock-model"

    override suspend fun generate(messages: List<LlmMessage>, options: LlmOptions): LlmResponse {
        // 根据输入内容生成不同的响应
        val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }?.content ?: ""

        val content = when {
            lastMessage.contains("creativity") -> "创造力是产生新颖、有用和有价值的想法或解决方案的能力。它涉及到打破常规思维模式，探索新的可能性，并将不同的概念结合起来。"
            lastMessage.contains("sport") -> "国际象棋篮球：这是一项结合了国际象棋策略和篮球运动的新型运动。球员在一个特殊设计的篮球场上移动，每个位置对应棋盘上的一个格子。球员的移动必须遵循国际象棋规则，同时他们需要投篮得分。"
            lastMessage.contains("education") -> "在数字时代，教育可以转变为个性化学习旅程，学生通过虚拟现实环境探索不同学科，AI导师根据每个学生的学习风格和进度提供指导，全球教育资源共享平台使知识民主化。"
            lastMessage.contains("art") -> "量子情感艺术：这是一种利用量子计算和脑机接口的新艺术形式。艺术家通过思想和情感直接影响量子粒子的行为，创造出随观众情绪变化而变化的动态视觉和听觉体验。"
            lastMessage.contains("future of work") -> "未来工作的探索：\n1. 远程协作将成为主流，虚拟办公室技术将模拟真实办公环境\n2. AI助手将处理日常任务，人类专注于创造性和战略性工作\n3. 工作将更加灵活，基于项目而非固定时间\n4. 终身学习将成为职业发展的核心"
            else -> "这是一个模拟响应。在实际应用中，这里会返回真实的AI生成内容。"
        }

        return LlmResponse(content = content)
    }

    override suspend fun streamGenerate(messages: List<LlmMessage>, options: LlmOptions): Flow<String> {
        throw UnsupportedOperationException("Stream generation not supported in mock")
    }

    override suspend fun embedText(text: String): List<Float> {
        return List(10) { 0.1f * it }
    }
}
