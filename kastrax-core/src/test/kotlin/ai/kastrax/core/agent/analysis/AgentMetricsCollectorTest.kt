package ai.kastrax.core.agent.analysis

import ai.kastrax.core.agent.AgentStatus
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled

class AgentMetricsCollectorTest {

    private lateinit var metricsStorage: InMemoryAgentMetricsStorage
    private lateinit var metricsCollector: AgentMetricsCollector

    @BeforeEach
    fun setup() {
        metricsStorage = InMemoryAgentMetricsStorage()
        metricsCollector = AgentMetricsCollector(metricsStorage)
    }

    @Test
    fun `test start and complete agent execution`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        val tags = mapOf("test" to "value")

        val metrics = metricsCollector.startAgentExecution(agentId, sessionId, tags)

        // 验证指标
        assertEquals(agentId, metrics.agentId)
        assertEquals(sessionId, metrics.sessionId)
        assertEquals(tags, metrics.tags)
        assertEquals(AgentStatus.IDLE, metrics.status)
        assertNull(metrics.endTime)

        // 完成代理执行
        val updatedMetrics = metricsCollector.completeAgentExecution(agentId, sessionId, AgentStatus.COMPLETED)

        // 验证更新后的指标
        assertNotNull(updatedMetrics)
        assertEquals(AgentStatus.COMPLETED, updatedMetrics?.status)
        assertNotNull(updatedMetrics?.endTime)
        // 注意：在测试环境中，代理执行时间可能非常短，所以latency可能为0
        assertTrue(updatedMetrics?.latency!! >= 0)
    }

    @Test
    fun `test start and complete step`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        metricsCollector.startAgentExecution(agentId, sessionId)

        // 开始步骤
        val stepName = "Test Step"
        val stepType = "test"
        val stepMetrics = metricsCollector.startStep(agentId, sessionId, stepName, stepType)

        // 验证步骤指标
        assertEquals(agentId, stepMetrics.agentId)
        assertEquals(sessionId, stepMetrics.sessionId)
        assertEquals(stepName, stepMetrics.stepName)
        assertEquals(stepType, stepMetrics.stepType)
        assertEquals(AgentStepStatus.RUNNING, stepMetrics.status)
        assertNull(stepMetrics.endTime)

        // 完成步骤
        val updatedStepMetrics = metricsCollector.completeStep(
            agentId,
            sessionId,
            stepMetrics.stepId,
            AgentStepStatus.COMPLETED,
            null,
            1024L,
            mapOf("test" to "value")
        )

        // 验证更新后的步骤指标
        assertNotNull(updatedStepMetrics)
        assertEquals(AgentStepStatus.COMPLETED, updatedStepMetrics?.status)
        assertNotNull(updatedStepMetrics?.endTime)
        assertEquals(1024L, updatedStepMetrics?.memoryUsage)
        assertEquals("value", updatedStepMetrics?.customMetrics?.get("test"))
        // 注意：在测试环境中，步骤执行时间可能非常短，所以latency可能为0
        assertTrue(updatedStepMetrics?.latency!! >= 0)
    }

    @Test
    fun `test record step retry`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        metricsCollector.startAgentExecution(agentId, sessionId)

        // 开始步骤
        val stepMetrics = metricsCollector.startStep(agentId, sessionId, "Test Step", "test")

        // 记录重试
        val updatedStepMetrics = metricsCollector.recordStepRetry(agentId, sessionId, stepMetrics.stepId)

        // 验证重试计数
        assertNotNull(updatedStepMetrics)
        assertEquals(1, updatedStepMetrics?.retryCount)

        // 验证代理指标中的重试计数
        val agentMetrics = metricsCollector.getAgentMetrics(agentId, sessionId)
        assertEquals(1, agentMetrics?.retryCount)
    }

    @Test
    fun `test start and complete tool call`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        metricsCollector.startAgentExecution(agentId, sessionId)

        // 开始步骤
        val stepMetrics = metricsCollector.startStep(agentId, sessionId, "Test Step", "test")

        // 开始工具调用
        val toolId = "test-tool"
        val toolName = "Test Tool"
        val arguments = buildJsonObject { put("param", "value") }

        val toolCallMetrics = metricsCollector.startToolCall(
            agentId,
            sessionId,
            stepMetrics.stepId,
            toolId,
            toolName,
            arguments
        )

        // 验证工具调用指标
        assertNotNull(toolCallMetrics)
        assertEquals(toolId, toolCallMetrics?.toolId)
        assertEquals(toolName, toolCallMetrics?.toolName)
        assertEquals(arguments, toolCallMetrics?.arguments)
        assertEquals(ToolCallStatus.RUNNING, toolCallMetrics?.status)
        assertNull(toolCallMetrics?.endTime)

        // 完成工具调用
        val result = buildJsonObject { put("result", "success") }
        val updatedToolCallMetrics = metricsCollector.completeToolCall(
            toolCallMetrics!!,
            ToolCallStatus.COMPLETED,
            result,
            null
        )

        // 验证更新后的工具调用指标
        assertEquals(ToolCallStatus.COMPLETED, updatedToolCallMetrics.status)
        assertNotNull(updatedToolCallMetrics.endTime)
        assertEquals(result, updatedToolCallMetrics.result)
        assertNull(updatedToolCallMetrics.errorMessage)
        // 注意：在测试环境中，工具调用时间可能非常短，所以latency可能为0
        assertTrue(updatedToolCallMetrics.latency >= 0)

        // 验证代理指标中的工具调用次数
        val agentMetrics = metricsCollector.getAgentMetrics(agentId, sessionId)
        assertEquals(1, agentMetrics?.toolCalls)
    }

    @Test
    fun `test update token counts`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        metricsCollector.startAgentExecution(agentId, sessionId)

        // 开始步骤
        val stepMetrics = metricsCollector.startStep(agentId, sessionId, "Test Step", "test")

        // 更新Token计数
        metricsCollector.updateTokenCounts(
            agentId,
            sessionId,
            stepMetrics.stepId,
            100,
            50
        )

        // 验证代理指标中的Token计数
        val agentMetrics = metricsCollector.getAgentMetrics(agentId, sessionId)
        assertEquals(100, agentMetrics?.promptTokens)
        assertEquals(50, agentMetrics?.completionTokens)
        assertEquals(150, agentMetrics?.totalTokens)

        // 验证步骤指标中的Token计数
        val updatedStepMetrics = metricsCollector.getStepMetricsForSession(agentId, sessionId)
            .find { it.stepId == stepMetrics.stepId }
        assertEquals(100, updatedStepMetrics?.promptTokens)
        assertEquals(50, updatedStepMetrics?.completionTokens)
        assertEquals(150, updatedStepMetrics?.totalTokens)
    }

    @Test
    fun `test record error`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        metricsCollector.startAgentExecution(agentId, sessionId)

        // 开始步骤
        val stepMetrics = metricsCollector.startStep(agentId, sessionId, "Test Step", "test")

        // 记录错误
        val errorMessage = "Test error message"
        metricsCollector.recordError(
            agentId,
            sessionId,
            stepMetrics.stepId,
            errorMessage
        )

        // 验证代理指标中的错误次数
        val agentMetrics = metricsCollector.getAgentMetrics(agentId, sessionId)
        assertEquals(1, agentMetrics?.errorCount)

        // 验证步骤指标中的错误信息
        val updatedStepMetrics = metricsCollector.getStepMetricsForSession(agentId, sessionId)
            .find { it.stepId == stepMetrics.stepId }
        assertEquals(errorMessage, updatedStepMetrics?.errorMessage)
        assertEquals(AgentStepStatus.FAILED, updatedStepMetrics?.status)
    }
}
