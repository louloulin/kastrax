package ai.kastrax.actor.network.protocols

import ai.kastrax.actor.network.AgentNetwork

/**
 * 辩论协作协议，Agent 之间进行结构化辩论以精炼解决方案
 *
 * 辩论协作协议让多个 Agent 通过结构化辩论的方式讨论问题，提出不同观点，
 * 并通过辩论过程逐步精炼解决方案，适用于复杂问题和需要多角度思考的任务
 *
 * @property debaterIds 参与辩论的 Agent ID 列表
 * @property moderatorId 主持人 Agent 的 ID，用于引导辩论和总结
 * @property rounds 辩论轮数
 * @property includeConclusion 是否由主持人给出结论
 */
class DebateProtocol(
    private val debaterIds: List<String>,
    private val moderatorId: String,
    private val rounds: Int = 3,
    private val includeConclusion: Boolean = true
) : CollaborationProtocol {

    override suspend fun execute(
        network: AgentNetwork,
        initiatorId: String,
        task: String
    ): CollaborationResult {
        // 验证所有 Agent 都存在
        val allAgentIds = debaterIds + moderatorId
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
        
        // 辩论历史
        val debateHistory = StringBuilder()
        
        // 主持人开场白
        val introPrompt = """
        |# 辩论主题
        |$task
        |
        |作为主持人，请介绍辩论主题并邀请参与者发表初步观点。
        """.trimMargin()
        
        val introResponse = network.askAgent(initiatorId, moderatorId, introPrompt)
        
        // 记录步骤
        val introStep = CollaborationStep(
            agentId = moderatorId,
            input = introPrompt,
            output = introResponse.text
        )
        steps.add(introStep)
        
        // 添加到辩论历史
        debateHistory.append("## 主持人开场白\n\n")
        debateHistory.append(introResponse.text)
        debateHistory.append("\n\n")
        
        // 初始观点阶段
        debateHistory.append("## 初始观点\n\n")
        
        for (debaterId in debaterIds) {
            val initialPrompt = """
            |# 辩论主题
            |$task
            |
            |# 主持人开场白
            |${introResponse.text}
            |
            |请提出你对这个主题的初步观点和立场。
            """.trimMargin()
            
            val initialResponse = network.askAgent(initiatorId, debaterId, initialPrompt)
            
            // 记录步骤
            val initialStep = CollaborationStep(
                agentId = debaterId,
                input = initialPrompt,
                output = initialResponse.text
            )
            steps.add(initialStep)
            
            // 添加到辩论历史
            debateHistory.append("### $debaterId 的初始观点\n\n")
            debateHistory.append(initialResponse.text)
            debateHistory.append("\n\n")
        }
        
        // 多轮辩论
        for (round in 1..rounds) {
            debateHistory.append("## 第 $round 轮辩论\n\n")
            
            // 主持人引导本轮辩论
            val roundIntroPrompt = """
            |# 辩论历史
            |$debateHistory
            |
            |作为主持人，请总结目前的辩论进展，指出分歧点和共识点，并引导第 $round 轮辩论。
            """.trimMargin()
            
            val roundIntroResponse = network.askAgent(initiatorId, moderatorId, roundIntroPrompt)
            
            // 记录步骤
            val roundIntroStep = CollaborationStep(
                agentId = moderatorId,
                input = roundIntroPrompt,
                output = roundIntroResponse.text
            )
            steps.add(roundIntroStep)
            
            // 添加到辩论历史
            debateHistory.append("### 主持人引导\n\n")
            debateHistory.append(roundIntroResponse.text)
            debateHistory.append("\n\n")
            
            // 每个辩论者发言
            for (debaterId in debaterIds) {
                val debatePrompt = """
                |# 辩论历史
                |$debateHistory
                |
                |请针对之前的讨论和主持人的引导，提出你的观点、反驳其他辩论者的观点，或者深化你之前的论点。
                """.trimMargin()
                
                val debateResponse = network.askAgent(initiatorId, debaterId, debatePrompt)
                
                // 记录步骤
                val debateStep = CollaborationStep(
                    agentId = debaterId,
                    input = debatePrompt,
                    output = debateResponse.text
                )
                steps.add(debateStep)
                
                // 添加到辩论历史
                debateHistory.append("### $debaterId 的发言\n\n")
                debateHistory.append(debateResponse.text)
                debateHistory.append("\n\n")
            }
        }
        
        // 主持人总结
        var conclusion = ""
        
        if (includeConclusion) {
            val conclusionPrompt = """
            |# 完整辩论记录
            |$debateHistory
            |
            |作为主持人，请总结整个辩论过程，包括各方观点、达成的共识和仍存在的分歧。
            |然后，基于辩论内容，提出一个综合性的解决方案或结论。
            """.trimMargin()
            
            val conclusionResponse = network.askAgent(initiatorId, moderatorId, conclusionPrompt)
            
            // 记录步骤
            val conclusionStep = CollaborationStep(
                agentId = moderatorId,
                input = conclusionPrompt,
                output = conclusionResponse.text
            )
            steps.add(conclusionStep)
            
            // 添加到辩论历史
            debateHistory.append("## 主持人总结与结论\n\n")
            debateHistory.append(conclusionResponse.text)
            
            conclusion = conclusionResponse.text
        }
        
        // 构建最终结果
        val finalResult = if (includeConclusion) {
            """
            |# 辩论协作结果
            |
            |## 辩论主题
            |$task
            |
            |## 主持人结论
            |$conclusion
            |
            |## 完整辩论记录
            |$debateHistory
            """.trimMargin()
        } else {
            """
            |# 辩论协作结果
            |
            |## 辩论主题
            |$task
            |
            |## 完整辩论记录
            |$debateHistory
            """.trimMargin()
        }
        
        // 返回结果
        return CollaborationResult(
            success = true,
            result = finalResult,
            participants = allAgentIds,
            steps = steps
        )
    }
}
