package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation

/**
 * 层次协作协议，由主 Agent 分配任务给从 Agent，然后整合结果
 *
 * 层次协作协议适用于主从关系的 Agent 网络，主 Agent 负责分解任务、分配子任务和整合结果
 */
class HierarchicalProtocol : CollaborationProtocol {
    
    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 获取主 Agent 的从 Agent
        val slaves = network.getAgentsByRelation(initiatorId, AgentRelation.MASTER)
        if (slaves.isEmpty()) {
            return CollaborationResult(
                success = false,
                result = "没有可用的从 Agent",
                participants = listOf(initiatorId),
                steps = emptyList()
            )
        }
        
        // 执行步骤
        val steps = mutableListOf<CollaborationStep>()
        
        // 主 Agent 分析任务
        val masterResponse = network.askAgent(initiatorId, initiatorId, "分析并分解任务: $task")
        steps.add(
            CollaborationStep(
                agentId = initiatorId,
                input = "分析并分解任务: $task",
                output = masterResponse.text
            )
        )
        
        // 向从 Agent 分配子任务
        val subTasks = masterResponse.text.split("\n").filter { it.isNotEmpty() }
        val subResults = mutableListOf<String>()
        
        for ((index, subTask) in subTasks.withIndex()) {
            // 选择从 Agent
            val slaveId = slaves[index % slaves.size]
            
            // 向从 Agent 发送子任务
            val slaveResponse = network.askAgent(initiatorId, slaveId, subTask)
            
            // 记录步骤
            steps.add(
                CollaborationStep(
                    agentId = slaveId,
                    input = subTask,
                    output = slaveResponse.text
                )
            )
            
            // 收集结果
            subResults.add(slaveResponse.text)
        }
        
        // 主 Agent 整合结果
        val finalInput = "整合以下结果:\n${subResults.joinToString("\n")}"
        val finalResponse = network.askAgent(initiatorId, initiatorId, finalInput)
        
        // 记录最终步骤
        steps.add(
            CollaborationStep(
                agentId = initiatorId,
                input = finalInput,
                output = finalResponse.text
            )
        )
        
        // 返回结果
        return CollaborationResult(
            success = true,
            result = finalResponse.text,
            participants = listOf(initiatorId) + slaves,
            steps = steps
        )
    }
}
