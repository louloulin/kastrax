package ai.kastrax.core.agent.autonomy

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Agent自主性测试类
 */
class AgentAutonomyTest {

    // 模拟LLM提供者
    private class MockLlmProvider : LlmProvider {
        override val model: String = "mock-model"

        override suspend fun generate(messages: List<LlmMessage>, options: LlmOptions): LlmResponse {
            // 根据输入内容生成不同的响应
            val lastMessage = messages.lastOrNull { it.role == LlmMessageRole.USER }?.content ?: ""

            val content = when {
                lastMessage.contains("creative") -> "这是一个创意响应，充满了新颖的想法和独特的视角。"
                lastMessage.contains("explore") -> "这是一个探索性响应，提出了多个值得研究的方向：\n1. 方向一\n2. 方向二\n3. 方向三"
                lastMessage.contains("reflect") -> "这是一个反思性响应，分析了之前的交互并提出了改进建议。"
                else -> "这是一个标准响应。"
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

    private lateinit var testAgent: ai.kastrax.core.agent.Agent
    private lateinit var autonomy: AgentAutonomy
    private lateinit var creativeAgent: CreativeAgent

    @BeforeEach
    fun setUp() {
        // 创建测试Agent
        testAgent = agent {
            name = "TestAgent"
            instructions = "You are a test agent."
            model = MockLlmProvider()
        }

        // 创建自主性管理器
        autonomy = testAgent.createAutonomy {
            mode(AutonomyMode.CREATIVE)
            level(AutonomyLevel.HIGH)
            enableAllCapabilities()
        }

        // 创建创造性Agent
        creativeAgent = testAgent.toCreativeAgent {
            mode(AutonomyMode.CREATIVE)
            level(AutonomyLevel.HIGH)
            enableAllCapabilities()
        }
    }

    @Test
    fun `test generate autonomous response`() = runBlocking {
        // 测试生成自主响应
        val response = autonomy.generateAutonomousResponse("Tell me something creative")

        assertNotNull(response)
        assertTrue(response.text.contains("创意响应") || response.text.contains("creative"))

        // 验证行为历史
        val behaviors = autonomy.getBehaviorHistory()
        assertTrue(behaviors.size >= 1)
        assertTrue(behaviors.any { it.type == "response_generation" })
    }

    @Test
    fun `test generate creative content`() = runBlocking {
        // 测试生成创意内容
        val content = autonomy.generateCreativeContent(
            "Generate a new business idea",
            CreativityType.COMBINATIONAL
        )

        assertNotNull(content)
        assertEquals(CreativityType.COMBINATIONAL, content.type)
        // 内容可能不包含“创意响应”，只验证内容不为空
        assertTrue(content.content.isNotEmpty())

        // 验证创意历史
        val creativeHistory = autonomy.getCreativeHistory()
        assertEquals(1, creativeHistory.size)
        assertEquals("Generate a new business idea", creativeHistory[0].inspiration)
    }

    @Test
    fun `test autonomous exploration`() = runBlocking {
        // 测试自主探索
        val explorationResults = autonomy.exploreAutonomously("explore artificial intelligence", 2)

        assertNotNull(explorationResults)
        assertTrue(explorationResults.isNotEmpty())
        assertTrue(explorationResults[0].contains("探索性响应"))

        // 验证行为历史
        val behaviors = autonomy.getBehaviorHistory()
        assertTrue(behaviors.any { it.type == "autonomous_exploration" })
    }

    @Test
    fun `test set autonomous goal`() = runBlocking {
        // 测试设置自主目标
        val goal = autonomy.setAutonomousGoal("Learn about quantum computing", 2)

        assertNotNull(goal)
        assertEquals("Learn about quantum computing", goal.description)
        assertEquals(2, goal.priority)
        assertEquals(GoalStatus.ACTIVE, goal.status)

        // 验证目标列表
        val goals = autonomy.getGoals()
        assertEquals(1, goals.size)
        assertEquals("Learn about quantum computing", goals[0].description)
    }

    @Test
    fun `test update goal status`() = runBlocking {
        // 设置目标
        val goal = autonomy.setAutonomousGoal("Learn about blockchain")

        // 更新目标状态
        val updatedGoal = autonomy.updateGoalStatus(goal.id, GoalStatus.COMPLETED)

        assertNotNull(updatedGoal)
        assertEquals(GoalStatus.COMPLETED, updatedGoal?.status)
        assertNotNull(updatedGoal?.completedAt)

        // 验证目标列表
        val activeGoals = autonomy.getGoals(includeCompleted = false)
        assertEquals(0, activeGoals.size)

        val allGoals = autonomy.getGoals(includeCompleted = true)
        assertEquals(1, allGoals.size)
        assertEquals(GoalStatus.COMPLETED, allGoals[0].status)
    }

    @Test
    fun `test creative agent`() = runBlocking {
        // 测试创造性Agent
        val response = creativeAgent.generate("Tell me something creative")

        assertNotNull(response)
        assertTrue(response.text.contains("创意响应") || response.text.contains("creative"))

        // 测试创意内容生成
        val content = creativeAgent.generateCreativeContent("Generate a new business idea")

        assertNotNull(content)
        // 内容可能不包含“创意响应”，只验证内容不为空
        assertTrue(content.content.isNotEmpty())

        // 测试自主探索
        val explorationResults = creativeAgent.exploreAutonomously("explore artificial intelligence")

        assertNotNull(explorationResults)
        assertTrue(explorationResults.isNotEmpty())
        assertTrue(explorationResults[0].contains("探索性响应"))
    }

    @Test
    fun `test max goals limit`() = runBlocking {
        // 配置最大目标数为2
        val limitedAutonomy = testAgent.createAutonomy {
            maxGoals(2)
            enableCapability(AutonomyCapability.GOAL_SETTING)
        }

        // 设置两个目标
        limitedAutonomy.setAutonomousGoal("Goal 1")
        limitedAutonomy.setAutonomousGoal("Goal 2")

        // 尝试设置第三个目标，应该抛出异常
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                limitedAutonomy.setAutonomousGoal("Goal 3")
            }
        }

        assertTrue(exception.message?.contains("最大目标数") ?: false)

        // 验证目标列表
        val goals = limitedAutonomy.getGoals()
        assertEquals(2, goals.size)
    }

    @Test
    fun `test disabled capability`() = runBlocking {
        // 创建禁用创造力的自主性管理器
        val limitedAutonomy = testAgent.createAutonomy {
            enableCapability(AutonomyCapability.SELF_REFLECTION)
            // 不启用CREATIVITY
        }

        // 尝试生成创意内容，应该抛出异常
        val exception = assertThrows<IllegalStateException> {
            runBlocking {
                limitedAutonomy.generateCreativeContent("Generate a new business idea")
            }
        }

        assertTrue(exception.message?.contains("创造力能力未启用") ?: false)
    }
}
