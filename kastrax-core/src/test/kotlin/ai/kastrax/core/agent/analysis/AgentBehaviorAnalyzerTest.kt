package ai.kastrax.core.agent.analysis

import ai.kastrax.core.agent.AgentStatus
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled
import kotlin.test.assertNotNull

class AgentBehaviorAnalyzerTest {

    private lateinit var metricsStorage: InMemoryAgentMetricsStorage
    private lateinit var metricsCollector: AgentMetricsCollector
    private lateinit var behaviorAnalyzer: AgentBehaviorAnalyzer

    @BeforeEach
    fun setup() {
        metricsStorage = InMemoryAgentMetricsStorage()
        metricsCollector = AgentMetricsCollector(metricsStorage)
        behaviorAnalyzer = AgentBehaviorAnalyzer(metricsStorage)
    }

    @Test
    fun `test analyze agent behavior with no steps`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        val agentMetrics = AgentMetrics(
            agentId = agentId,
            sessionId = sessionId,
            startTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 完成代理执行
        agentMetrics.status = AgentStatus.COMPLETED
        agentMetrics.endTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis() + 1000)
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 分析代理行为
        val analysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId)

        // 由于没有步骤，应该返回null
        assertNull(analysis)
    }

    @Test
    fun `test analyze agent behavior with steps`() = runBlocking {
        // 开始代理执行
        val agentId = "test-agent"
        val sessionId = "test-session"
        val agentMetrics = AgentMetrics(
            agentId = agentId,
            sessionId = sessionId,
            startTime = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
        metricsStorage.saveAgentMetrics(agentMetrics)

        // 添加步骤
        val step1 = metricsCollector.startStep(agentId, sessionId, "Step 1", "test")
        Thread.sleep(100) // 模拟步骤执行时间
        metricsCollector.completeStep(agentId, sessionId, step1.stepId, AgentStepStatus.COMPLETED)

        val step2 = metricsCollector.startStep(agentId, sessionId, "Step 2", "test")
        Thread.sleep(200) // 模拟步骤执行时间
        metricsCollector.completeStep(agentId, sessionId, step2.stepId, AgentStepStatus.COMPLETED)

        val step3 = metricsCollector.startStep(agentId, sessionId, "Step 3", "test")
        Thread.sleep(50) // 模拟步骤执行时间
        metricsCollector.completeStep(agentId, sessionId, step3.stepId, AgentStepStatus.FAILED, "Test error")

        // 更新Token计数
        metricsCollector.updateTokenCounts(agentId, sessionId, step1.stepId, 100, 50)
        metricsCollector.updateTokenCounts(agentId, sessionId, step2.stepId, 200, 100)
        metricsCollector.updateTokenCounts(agentId, sessionId, step3.stepId, 150, 75)

        // 添加工具调用
        val toolCall1 = metricsCollector.startToolCall(
            agentId, sessionId, step1.stepId, "tool1", "Tool 1", buildJsonObject { put("param", "value") }
        )
        if (toolCall1 != null) {
            metricsCollector.completeToolCall(toolCall1, ToolCallStatus.COMPLETED)
        }

        val toolCall2 = metricsCollector.startToolCall(
            agentId, sessionId, step2.stepId, "tool2", "Tool 2", buildJsonObject { put("param", "value") }
        )
        if (toolCall2 != null) {
            metricsCollector.completeToolCall(toolCall2, ToolCallStatus.FAILED, null, "Tool error")
        }

        // 记录错误和重试
        metricsCollector.recordError(agentId, sessionId, step3.stepId, "Test error")
        metricsCollector.recordStepRetry(agentId, sessionId, step3.stepId)

        // 完成代理执行
        metricsCollector.completeAgentExecution(agentId, sessionId, AgentStatus.COMPLETED)

        // 分析代理行为
        val analysis = behaviorAnalyzer.analyzeAgentBehavior(agentId, sessionId)

        // 验证分析结果
        assertNotNull(analysis)
        assertEquals(agentId, analysis?.agentId)
        assertEquals(sessionId, analysis?.sessionId)

        // 验证性能指标
        assertTrue(analysis?.totalDuration!! > 0)
        assertTrue(analysis.averageStepDuration > 0)
        assertTrue(analysis.maxStepDuration > 0)
        assertTrue(analysis.minStepDuration > 0)

        // 验证Token使用情况
        assertEquals(450, analysis.totalPromptTokens)
        assertEquals(225, analysis.totalCompletionTokens)
        assertEquals(675, analysis.totalTokens)

        // 验证工具调用情况
        assertEquals(2, analysis.totalToolCalls)
        assertEquals(2.0 / 3.0, analysis.toolCallsPerStep, 0.01)

        // 验证错误和重试情况
        assertEquals(1, analysis.totalErrors)
        assertEquals(1, analysis.totalRetries)
        assertEquals(1.0 / 3.0, analysis.errorRate, 0.01)
        assertEquals(1.0 / 3.0, analysis.retryRate, 0.01)

        // 验证瓶颈步骤
        assertTrue(analysis.bottleneckSteps.isNotEmpty())

        // 验证高错误率步骤
        assertEquals(1, analysis.highErrorSteps.size)
        assertEquals("Step 3", analysis.highErrorSteps[0].stepName)
        assertEquals("Test error", analysis.highErrorSteps[0].errorMessage)
        assertEquals(1, analysis.highErrorSteps[0].retryCount)

        // 验证优化建议
        assertTrue(analysis.optimizationSuggestions.isNotEmpty())
    }
}
