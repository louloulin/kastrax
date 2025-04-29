package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation

/**
 * 共识协作协议，所有 Agent 对同一任务提出解决方案，然后达成共识
 *
 * 共识协作协议适用于对等关系的 Agent 网络，每个 Agent 都提出自己的解决方案，
 * 然后通过评估和改进达成最终共识
 */
class ConsensusProtocol : CollaborationProtocol {
    
    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 获取所有对等 Agent
        val peers = network.getAgentsByRelation(initiatorId, AgentRelation.PEER)
        val participants = listOf(initiatorId) + peers
        
        if (peers.isEmpty()) {
            return CollaborationResult(
                success = false,
                result = "没有可用的对等 Agent",
                participants = listOf(initiatorId),
                steps = emptyList()
            )
        }
        
        // 执行步骤
        val steps = mutableListOf<CollaborationStep>()
        
        // 第一轮：每个 Agent 提出解决方案
        val solutions = mutableMapOf<String, String>()
        
        // 发起者提出解决方案
        val initiatorResponse = network.askAgent(initiatorId, initiatorId, "提出解决方案: $task")
        steps.add(
            CollaborationStep(
                agentId = initiatorId,
                input = "提出解决方案: $task",
                output = initiatorResponse.text
            )
        )
        solutions[initiatorId] = initiatorResponse.text
        
        // 对等 Agent 提出解决方案
        for (peerId in peers) {
            val peerResponse = network.askAgent(initiatorId, peerId, "提出解决方案: $task")
            steps.add(
                CollaborationStep(
                    agentId = peerId,
                    input = "提出解决方案: $task",
                    output = peerResponse.text
                )
            )
            solutions[peerId] = peerResponse.text
        }
        
        // 第二轮：每个 Agent 评估所有解决方案
        val evaluations = mutableMapOf<String, Map<String, Int>>()
        
        // 构建评估输入
        val evaluationInput = buildString {
            append("评估以下解决方案（1-10分）:\n")
            solutions.forEach { (agentId, solution) ->
                append("$agentId 的解决方案: $solution\n")
            }
        }
        
        // 发起者评估
        val initiatorEvaluation = network.askAgent(initiatorId, initiatorId, evaluationInput)
        steps.add(
            CollaborationStep(
                agentId = initiatorId,
                input = evaluationInput,
                output = initiatorEvaluation.text
            )
        )
        evaluations[initiatorId] = parseEvaluation(initiatorEvaluation.text, participants)
        
        // 对等 Agent 评估
        for (peerId in peers) {
            val peerEvaluation = network.askAgent(initiatorId, peerId, evaluationInput)
            steps.add(
                CollaborationStep(
                    agentId = peerId,
                    input = evaluationInput,
                    output = peerEvaluation.text
                )
            )
            evaluations[peerId] = parseEvaluation(peerEvaluation.text, participants)
        }
        
        // 计算总分
        val totalScores = mutableMapOf<String, Int>()
        participants.forEach { agentId ->
            var totalScore = 0
            evaluations.forEach { (_, scores) ->
                totalScore += scores[agentId] ?: 0
            }
            totalScores[agentId] = totalScore
        }
        
        // 找出得分最高的解决方案
        val bestAgentId = totalScores.maxByOrNull { it.value }?.key ?: initiatorId
        val bestSolution = solutions[bestAgentId] ?: ""
        
        // 第三轮：达成共识
        val consensusInput = buildString {
            append("基于评估，最佳解决方案是 $bestAgentId 的方案:\n")
            append(bestSolution)
            append("\n\n请对此解决方案提出改进建议。")
        }
        
        // 收集改进建议
        val improvements = mutableListOf<String>()
        
        // 发起者提出改进建议
        val initiatorImprovement = network.askAgent(initiatorId, initiatorId, consensusInput)
        steps.add(
            CollaborationStep(
                agentId = initiatorId,
                input = consensusInput,
                output = initiatorImprovement.text
            )
        )
        improvements.add(initiatorImprovement.text)
        
        // 对等 Agent 提出改进建议
        for (peerId in peers) {
            val peerImprovement = network.askAgent(initiatorId, peerId, consensusInput)
            steps.add(
                CollaborationStep(
                    agentId = peerId,
                    input = consensusInput,
                    output = peerImprovement.text
                )
            )
            improvements.add(peerImprovement.text)
        }
        
        // 最终轮：整合改进建议
        val finalInput = buildString {
            append("整合以下改进建议，形成最终解决方案:\n")
            append("原始解决方案: $bestSolution\n\n")
            append("改进建议:\n")
            improvements.forEachIndexed { index, improvement ->
                append("${index + 1}. $improvement\n")
            }
        }
        
        // 发起者整合改进建议
        val finalResponse = network.askAgent(initiatorId, initiatorId, finalInput)
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
            participants = participants,
            steps = steps
        )
    }
    
    /**
     * 解析评估结果
     *
     * @param evaluationText 评估文本
     * @param agentIds Agent ID 列表
     * @return Agent ID 到评分的映射
     */
    private fun parseEvaluation(evaluationText: String, agentIds: List<String>): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        
        // 为每个 Agent 设置默认分数
        agentIds.forEach { agentId ->
            scores[agentId] = 5 // 默认中等分数
        }
        
        // 尝试从文本中提取分数
        agentIds.forEach { agentId ->
            val regex = "$agentId.*?(\\d+)".toRegex()
            val matchResult = regex.find(evaluationText)
            if (matchResult != null) {
                val score = matchResult.groupValues[1].toIntOrNull()
                if (score != null && score in 1..10) {
                    scores[agentId] = score
                }
            }
        }
        
        return scores
    }
}
