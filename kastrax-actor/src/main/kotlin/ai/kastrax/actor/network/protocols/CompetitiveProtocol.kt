package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork

/**
 * 竞争协作协议，多个 Agent 竞争解决同一任务，选择最佳解决方案
 *
 * 竞争协作协议让多个 Agent 同时解决同一问题，然后通过评估选择最佳解决方案，
 * 适用于需要多种思路和创新解决方案的任务
 *
 * @property competitorIds 参与竞争的 Agent ID 列表
 * @property judgeId 评判 Agent 的 ID，用于评估解决方案
 * @property rounds 竞争轮数，默认为 1
 */
class CompetitiveProtocol(
    private val competitorIds: List<String>,
    private val judgeId: String,
    private val rounds: Int = 1
) : CollaborationProtocol {

    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 验证所有 Agent 都存在
        val allAgentIds = competitorIds + judgeId
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
        
        // 当前最佳解决方案
        var bestSolution = ""
        var bestCompetitor = ""
        
        // 进行多轮竞争
        for (round in 1..rounds) {
            println("开始第 $round 轮竞争")
            
            // 收集所有竞争者的解决方案
            val solutions = mutableMapOf<String, String>()
            
            // 每个竞争者提出解决方案
            for (competitorId in competitorIds) {
                // 构建提示，包含之前的最佳解决方案（如果有）
                val prompt = if (round > 1 && bestSolution.isNotEmpty()) {
                    """
                    |任务: $task
                    |
                    |上一轮最佳解决方案 (由 $bestCompetitor 提出):
                    |$bestSolution
                    |
                    |请提出你的解决方案，尝试超越上一轮的最佳方案。
                    """.trimMargin()
                } else {
                    "任务: $task\n\n请提出你的解决方案。"
                }
                
                // 向竞争者发送请求
                val response = network.askAgent(initiatorId, competitorId, prompt)
                
                // 记录步骤
                val step = CollaborationStep(
                    agentId = competitorId,
                    input = prompt,
                    output = response.text
                )
                steps.add(step)
                
                // 保存解决方案
                solutions[competitorId] = response.text
            }
            
            // 评判解决方案
            val evaluationPrompt = """
            |任务: $task
            |
            |请评估以下解决方案，并选择最佳方案:
            |
            |${solutions.entries.joinToString("\n\n") { (id, solution) ->
                "## $id 的解决方案:\n$solution"
            }}
            |
            |请分析每个解决方案的优缺点，并明确指出最佳方案是哪一个。格式为:
            |最佳方案: [Agent ID]
            |理由: [详细说明为什么这是最佳方案]
            """.trimMargin()
            
            // 向评判发送请求
            val evaluationResponse = network.askAgent(initiatorId, judgeId, evaluationPrompt)
            
            // 记录步骤
            val evaluationStep = CollaborationStep(
                agentId = judgeId,
                input = evaluationPrompt,
                output = evaluationResponse.text
            )
            steps.add(evaluationStep)
            
            // 解析评判结果，找出最佳方案
            val bestAgentPattern = "最佳方案:\\s*([\\w-]+)".toRegex()
            val matchResult = bestAgentPattern.find(evaluationResponse.text)
            
            if (matchResult != null) {
                val winnerAgentId = matchResult.groupValues[1]
                if (solutions.containsKey(winnerAgentId)) {
                    bestSolution = solutions[winnerAgentId] ?: ""
                    bestCompetitor = winnerAgentId
                    
                    println("第 $round 轮最佳解决方案: $bestCompetitor")
                }
            }
        }
        
        // 构建最终结果
        val finalResult = """
        |# 竞争协作结果
        |
        |## 任务
        |$task
        |
        |## 最佳解决方案 (由 $bestCompetitor 提出)
        |$bestSolution
        |
        |## 评判过程
        |${steps.filter { it.agentId == judgeId }.joinToString("\n\n") { it.output }}
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
