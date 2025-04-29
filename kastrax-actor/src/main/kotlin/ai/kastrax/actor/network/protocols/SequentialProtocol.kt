package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork

/**
 * 顺序协作协议，按照预定义的顺序依次执行任务
 *
 * 顺序协作协议将任务按照预定义的顺序依次传递给不同的 Agent，每个 Agent 的输出作为下一个 Agent 的输入
 *
 * @property agentSequence Agent 执行顺序
 */
class SequentialProtocol(
    private val agentSequence: List<String>
) : CollaborationProtocol {
    
    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 验证所有 Agent 都存在
        val invalidAgents = agentSequence.filter { network.getAgentPid(it) == null }
        if (invalidAgents.isNotEmpty()) {
            return CollaborationResult(
                success = false,
                result = "无效的 Agent: ${invalidAgents.joinToString()}",
                participants = emptyList(),
                steps = emptyList()
            )
        }
        
        // 执行步骤
        val steps = mutableListOf<CollaborationStep>()
        var currentInput = task
        
        // 按顺序执行
        for (agentId in agentSequence) {
            // 向当前 Agent 发送请求
            val response = network.askAgent(initiatorId, agentId, currentInput)
            
            // 记录步骤
            val step = CollaborationStep(
                agentId = agentId,
                input = currentInput,
                output = response.text
            )
            steps.add(step)
            
            // 更新输入
            currentInput = response.text
        }
        
        // 返回结果
        return CollaborationResult(
            success = true,
            result = currentInput,
            participants = agentSequence,
            steps = steps
        )
    }
}
