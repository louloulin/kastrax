package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.AgentResponse
import ai.kastrax.actor.network.AgentNetwork
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 并行协作协议，多个 Agent 并行执行任务，然后聚合结果
 *
 * 并行协作协议允许多个 Agent 同时处理同一任务，然后将结果聚合起来，
 * 适用于可以并行处理的任务，如数据收集、多角度分析等
 *
 * @property agentIds 参与协作的 Agent ID 列表
 * @property aggregator 结果聚合函数，默认为简单拼接
 */
class ParallelProtocol(
    private val agentIds: List<String>,
    private val aggregator: (List<String>) -> String = { results ->
        """
        |# 并行任务结果汇总
        |
        |${results.mapIndexed { index, result -> "## Agent ${agentIds[index]} 的结果\n\n$result" }.joinToString("\n\n")}
        """.trimMargin()
    }
) : CollaborationProtocol {

    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 验证所有 Agent 都存在
        val invalidAgents = agentIds.filter { network.getAgentPid(it) == null }
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
        
        // 并行执行任务
        val results = coroutineScope {
            agentIds.map { agentId ->
                async {
                    try {
                        // 向 Agent 发送请求
                        val response = network.askAgent(initiatorId, agentId, task)
                        
                        // 记录步骤
                        val step = CollaborationStep(
                            agentId = agentId,
                            input = task,
                            output = response.text
                        )
                        synchronized(steps) {
                            steps.add(step)
                        }
                        
                        // 返回结果
                        response.text
                    } catch (e: Exception) {
                        // 如果发生异常，记录错误
                        val errorMessage = "[错误] 在调用 $agentId 时发生异常: ${e.message}"
                        val step = CollaborationStep(
                            agentId = agentId,
                            input = task,
                            output = errorMessage
                        )
                        synchronized(steps) {
                            steps.add(step)
                        }
                        
                        // 返回错误消息
                        errorMessage
                    }
                }
            }.awaitAll()
        }
        
        // 聚合结果
        val aggregatedResult = aggregator(results)
        
        // 返回结果
        return CollaborationResult(
            success = true,
            result = aggregatedResult,
            participants = agentIds,
            steps = steps
        )
    }
}
