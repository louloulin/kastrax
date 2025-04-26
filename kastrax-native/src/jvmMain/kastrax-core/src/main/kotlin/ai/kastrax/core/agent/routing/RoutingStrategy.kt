package ai.kastrax.core.agent.routing

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentInteraction
import ai.kastrax.core.llm.LlmMessage

/**
 * 路由策略接口，定义了如何在代理网络中路由消息
 */
interface RoutingStrategy {
    /**
     * 获取路由指令
     *
     * @param agents 可用的代理列表
     * @param instructions 网络指令
     * @return 路由指令
     */
    fun getInstructions(agents: List<Agent>, instructions: String): String

    /**
     * 格式化代理ID
     *
     * @param name 代理名称
     * @return 格式化后的代理ID
     */
    fun formatAgentId(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }

    /**
     * 准备代理执行的上下文
     *
     * @param agentId 代理ID
     * @param input 输入消息
     * @param includeHistory 是否包含历史记录
     * @param agentHistory 代理历史记录
     * @return 准备好的消息列表
     */
    fun prepareAgentContext(
        agentId: String,
        input: String,
        includeHistory: Boolean,
        agentHistory: Map<String, List<AgentInteraction>>
    ): List<LlmMessage>
}
