package ai.kastrax.core.agent.analysis

/**
 * 代理指标存储接口，用于存储和检索代理行为指标
 */
interface AgentMetricsStorage {
    /**
     * 保存代理指标
     *
     * @param metrics 代理指标
     */
    suspend fun saveAgentMetrics(metrics: AgentMetrics)

    /**
     * 保存代理步骤指标
     *
     * @param metrics 代理步骤指标
     */
    suspend fun saveStepMetrics(metrics: AgentStepMetrics)

    /**
     * 保存工具调用指标
     *
     * @param metrics 工具调用指标
     */
    suspend fun saveToolCallMetrics(metrics: ToolCallMetrics)

    /**
     * 获取代理指标
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID
     * @return 代理指标，如果不存在则返回null
     */
    suspend fun getAgentMetrics(agentId: String, sessionId: String?): AgentMetrics?

    /**
     * 获取代理的所有指标
     *
     * @param agentId 代理ID
     * @param limit 限制返回的记录数
     * @param offset 偏移量
     * @return 代理指标列表
     */
    suspend fun getAgentMetricsForAgent(agentId: String, limit: Int = 100, offset: Int = 0): List<AgentMetrics>

    /**
     * 获取会话的所有代理步骤指标
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID
     * @return 代理步骤指标列表
     */
    suspend fun getStepMetricsForSession(agentId: String, sessionId: String?): List<AgentStepMetrics>

    /**
     * 获取步骤的所有工具调用指标
     *
     * @param agentId 代理ID
     * @param sessionId 会话ID
     * @param stepId 步骤ID
     * @return 工具调用指标列表
     */
    suspend fun getToolCallMetricsForStep(agentId: String, sessionId: String?, stepId: String): List<ToolCallMetrics>
}
