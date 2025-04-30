package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork

/**
 * 反思协作协议，实现行动、反思和改进的循环
 *
 * 反思协作协议让 Agent 在执行任务后进行反思，然后基于反思改进解决方案，
 * 适用于需要迭代改进和自我完善的任务
 *
 * @property actorId 执行任务的 Agent ID
 * @property reflectorId 反思者 Agent 的 ID，用于评估和提供反馈
 * @property iterations 迭代次数
 */
class ReflectiveProtocol(
    private val actorId: String,
    private val reflectorId: String,
    private val iterations: Int = 3
) : CollaborationProtocol {

    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 验证所有 Agent 都存在
        val allAgentIds = listOf(actorId, reflectorId)
        val invalidAgents = allAgentIds.filter { network.getAgentPid(it) == null }
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
        
        // 当前解决方案
        var currentSolution = ""
        
        // 反思历史
        val reflectionHistory = StringBuilder()
        reflectionHistory.append("# 反思协作过程\n\n")
        reflectionHistory.append("## 任务\n$task\n\n")
        
        // 迭代执行-反思-改进循环
        for (iteration in 1..iterations) {
            reflectionHistory.append("## 迭代 $iteration\n\n")
            
            // 1. 执行阶段
            val actionPrompt = if (iteration == 1) {
                "任务: $task\n\n请提出解决方案。"
            } else {
                """
                |任务: $task
                |
                |你之前的解决方案:
                |$currentSolution
                |
                |反思者的反馈:
                |${steps.last().output}
                |
                |请基于反馈改进你的解决方案。
                """.trimMargin()
            }
            
            val actionResponse = network.askAgent(initiatorId, actorId, actionPrompt)
            
            // 记录步骤
            val actionStep = CollaborationStep(
                agentId = actorId,
                input = actionPrompt,
                output = actionResponse.text
            )
            steps.add(actionStep)
            
            // 更新当前解决方案
            currentSolution = actionResponse.text
            
            // 添加到反思历史
            reflectionHistory.append("### 执行者的解决方案\n\n")
            reflectionHistory.append(currentSolution)
            reflectionHistory.append("\n\n")
            
            // 如果是最后一次迭代，不需要反思
            if (iteration == iterations) {
                break
            }
            
            // 2. 反思阶段
            val reflectionPrompt = """
            |任务: $task
            |
            |执行者的解决方案:
            |$currentSolution
            |
            |请对这个解决方案进行深入反思，评估其优缺点，并提供具体的改进建议。
            |分析以下几个方面:
            |1. 解决方案的优点
            |2. 解决方案的不足之处
            |3. 具体的改进建议
            |4. 可能被忽视的重要考虑因素
            """.trimMargin()
            
            val reflectionResponse = network.askAgent(initiatorId, reflectorId, reflectionPrompt)
            
            // 记录步骤
            val reflectionStep = CollaborationStep(
                agentId = reflectorId,
                input = reflectionPrompt,
                output = reflectionResponse.text
            )
            steps.add(reflectionStep)
            
            // 添加到反思历史
            reflectionHistory.append("### 反思者的反馈\n\n")
            reflectionHistory.append(reflectionResponse.text)
            reflectionHistory.append("\n\n")
        }
        
        // 构建最终结果
        val finalResult = """
        |# 反思协作结果
        |
        |## 任务
        |$task
        |
        |## 最终解决方案
        |$currentSolution
        |
        |## 完整反思过程
        |$reflectionHistory
        """.trimMargin()
        
        // 返回结果
        return CollaborationResult(
            success = true,
            result = finalResult,
            participants = allAgentIds,
            steps = steps
        )
    }
}
