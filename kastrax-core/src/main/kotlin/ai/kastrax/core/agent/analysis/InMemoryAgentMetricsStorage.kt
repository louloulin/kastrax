package ai.kastrax.core.agent.analysis

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 内存中的代理指标存储实现
 */
class InMemoryAgentMetricsStorage : AgentMetricsStorage {
    private val agentMetrics = mutableMapOf<Pair<String, String?>, AgentMetrics>()
    private val stepMetrics = mutableMapOf<Triple<String, String?, String>, AgentStepMetrics>()
    private val toolCallMetrics = mutableMapOf<String, ToolCallMetrics>()
    private val mutex = Mutex()

    override suspend fun saveAgentMetrics(metrics: AgentMetrics) = mutex.withLock {
        agentMetrics[Pair(metrics.agentId, metrics.sessionId)] = metrics
    }

    override suspend fun saveStepMetrics(metrics: AgentStepMetrics): Unit = mutex.withLock {
        stepMetrics[Triple(metrics.agentId, metrics.sessionId, metrics.stepId)] = metrics

        // 更新代理指标中的步骤指标
        val agentMetricsKey = Pair(metrics.agentId, metrics.sessionId)
        agentMetrics[agentMetricsKey]?.let { agentMetric ->
            agentMetric.addStepMetric(metrics)
        }
    }

    override suspend fun saveToolCallMetrics(metrics: ToolCallMetrics) = mutex.withLock {
        val key = "${metrics.toolId}-${metrics.startTime.toEpochMilliseconds()}"
        toolCallMetrics[key] = metrics
    }

    override suspend fun getAgentMetrics(agentId: String, sessionId: String?): AgentMetrics? = mutex.withLock {
        return agentMetrics[Pair(agentId, sessionId)]
    }

    override suspend fun getAgentMetricsForAgent(agentId: String, limit: Int, offset: Int): List<AgentMetrics> = mutex.withLock {
        return agentMetrics.values
            .filter { it.agentId == agentId }
            .sortedByDescending { it.startTime }
            .drop(offset)
            .take(limit)
    }

    override suspend fun getStepMetricsForSession(agentId: String, sessionId: String?): List<AgentStepMetrics> = mutex.withLock {
        return stepMetrics.values
            .filter { it.agentId == agentId && it.sessionId == sessionId }
            .sortedByDescending { it.startTime }
    }

    override suspend fun getToolCallMetricsForStep(agentId: String, sessionId: String?, stepId: String): List<ToolCallMetrics> = mutex.withLock {
        val step = stepMetrics[Triple(agentId, sessionId, stepId)] ?: return emptyList()
        return step.toolCalls
    }
}
