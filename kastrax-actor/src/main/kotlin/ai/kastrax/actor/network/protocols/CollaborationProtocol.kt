package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork

/**
 * 协作协议，定义 Agent 之间的协作方式
 * 
 * 协作协议定义了多个 Agent 如何共同完成一个任务
 */
interface CollaborationProtocol {
    /**
     * 执行协作任务
     *
     * @param network Agent 网络
     * @param initiatorId 发起者 Agent ID
     * @param task 任务描述
     * @return 协作结果
     */
    suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult
}
