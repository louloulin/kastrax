package ai.kastrax.core.agent.routing

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentInteraction
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole

/**
 * 默认路由策略实现
 */
class DefaultRoutingStrategy : RoutingStrategy {
    override fun getInstructions(agents: List<Agent>, instructions: String): String {
        // 创建可用代理列表
        val agentList = agents.joinToString("\n") { agent ->
            val id = formatAgentId(agent.name)
            " - **$id**: ${agent.name}"
        }

        return """
            你是一个专业代理网络中的路由器。
            你的工作是决定哪个代理应该处理任务的每个步骤。

            ## 系统指令
            $instructions

            ## 可用的专业代理
            你可以使用"transmit"工具调用这些代理：
            $agentList

            ## 如何使用"transmit"工具

            "transmit"工具允许你调用一个或多个专业代理。

            ### 单个代理调用
            要调用单个代理，使用以下格式：
            ```json
            {
              "actions": [
                {
                  "agent": "agent_name",
                  "input": "给代理的详细指令",
                  "includeHistory": true
                }
              ]
            }
            ```

            ### 多个代理调用
            要调用多个代理，使用以下格式：
            ```json
            {
              "actions": [
                {
                  "agent": "agent_name_1",
                  "input": "给代理1的详细指令",
                  "includeHistory": true
                },
                {
                  "agent": "agent_name_2",
                  "input": "给代理2的详细指令",
                  "includeHistory": false
                }
              ]
            }
            ```

            ## 最佳实践
            1. 将复杂任务分解为更小的步骤
            2. 为每个步骤选择最合适的代理
            3. 为每个代理提供清晰、详细的指令
            4. 在需要时综合多个代理的结果
            5. 为用户提供最终摘要或答案

            ## 工作流程
            1. 分析用户的请求
            2. 确定哪些专业代理可以提供帮助
            3. 使用transmit工具调用适当的代理
            4. 审查代理的响应
            5. 调用更多代理或提供最终答案
        """.trimIndent()
    }

    override fun prepareAgentContext(
        agentId: String,
        input: String,
        includeHistory: Boolean,
        agentHistory: Map<String, List<AgentInteraction>>
    ): List<LlmMessage> {
        val messages = mutableListOf<LlmMessage>()

        // 如果请求，包含相关历史
        if (includeHistory) {
            val history = agentHistory[agentId] ?: emptyList()
            if (history.isNotEmpty()) {
                messages.add(
                    LlmMessage(
                        role = LlmMessageRole.SYSTEM,
                        content = "以下是之前与你的交互历史，可能对当前任务有帮助:\n" +
                                history.joinToString("\n\n") { "用户: ${it.input}\n你: ${it.output}" }
                    )
                )
            }
        }

        // 添加用户消息
        messages.add(LlmMessage(role = LlmMessageRole.USER, content = input))

        return messages
    }
}
