package ai.kastrax.core.agent.routing

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentInteraction
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole

/**
 * 上下文感知路由策略，提供更丰富的上下文共享和代理协作
 */
class ContextAwareRoutingStrategy : RoutingStrategy {
    override fun getInstructions(agents: List<Agent>, instructions: String): String {
        // 创建可用代理列表，包含更详细的描述
        val agentList = agents.joinToString("\n") { agent ->
            val id = formatAgentId(agent.name)
            " - **$id**: ${agent.name}"
        }

        return """
            你是一个高级代理网络中的智能路由器。
            你的工作是协调多个专业代理，确保它们高效协作以解决复杂问题。

            ## 系统指令
            $instructions

            ## 可用的专业代理
            你可以使用"transmit"工具调用这些代理：
            $agentList

            ## 如何使用"transmit"工具

            "transmit"工具允许你调用一个或多个专业代理，并在它们之间共享上下文。

            ### 单个代理调用
            要调用单个代理，使用以下格式：
            ```json
            {
              "actions": [
                {
                  "agent": "agent_name",
                  "input": "给代理的详细指令",
                  "includeHistory": true,
                  "contextType": "full" 
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
                  "includeHistory": true,
                  "contextType": "full"
                },
                {
                  "agent": "agent_name_2",
                  "input": "给代理2的详细指令",
                  "includeHistory": false,
                  "contextType": "summary"
                }
              ]
            }
            ```

            ### 上下文类型选项
            - "full": 提供完整的历史记录和上下文
            - "summary": 提供历史记录的摘要
            - "none": 不提供额外上下文

            ## 协作模式
            你可以使用以下协作模式：

            1. **顺序协作**: 一个代理接一个代理工作，每个代理使用前一个代理的输出
            2. **并行协作**: 多个代理同时工作，然后你综合它们的结果
            3. **专家小组**: 让多个代理评估同一问题，然后综合他们的见解
            4. **迭代改进**: 让一个代理工作，然后让另一个代理改进结果

            ## 最佳实践
            1. 将复杂任务分解为更小的步骤
            2. 为每个步骤选择最合适的代理
            3. 为每个代理提供清晰、详细的指令
            4. 在代理之间共享相关上下文
            5. 在需要时综合多个代理的结果
            6. 为用户提供最终摘要或答案

            ## 工作流程
            1. 分析用户的请求
            2. 设计最佳的代理协作策略
            3. 使用transmit工具调用适当的代理
            4. 管理代理之间的上下文共享
            5. 综合结果并提供连贯的响应
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
            // 添加当前代理的历史
            val agentSpecificHistory = agentHistory[agentId] ?: emptyList()
            if (agentSpecificHistory.isNotEmpty()) {
                messages.add(
                    LlmMessage(
                        role = LlmMessageRole.SYSTEM,
                        content = "以下是你之前的交互历史，可能对当前任务有帮助:\n" +
                                agentSpecificHistory.joinToString("\n\n") { "用户: ${it.input}\n你: ${it.output}" }
                    )
                )
            }

            // 添加其他代理的相关历史摘要
            val otherAgentsHistory = agentHistory.filter { it.key != agentId }
            if (otherAgentsHistory.isNotEmpty()) {
                val historySummary = otherAgentsHistory.map { (otherAgentId, interactions) ->
                    if (interactions.isNotEmpty()) {
                        "代理 $otherAgentId 的最新交互:\n" +
                                "- 输入: ${interactions.last().input}\n" +
                                "- 输出: ${interactions.last().output}"
                    } else {
                        ""
                    }
                }.filter { it.isNotEmpty() }.joinToString("\n\n")

                if (historySummary.isNotEmpty()) {
                    messages.add(
                        LlmMessage(
                            role = LlmMessageRole.SYSTEM,
                            content = "以下是其他代理的相关信息，可能对当前任务有帮助:\n$historySummary"
                        )
                    )
                }
            }
        }

        // 添加用户消息
        messages.add(LlmMessage(role = LlmMessageRole.USER, content = input))

        return messages
    }
}
