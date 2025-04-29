package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import ai.kastrax.actor.network.*
import ai.kastrax.actor.network.protocols.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

/**
 * Agent 网络示例
 */
object AgentNetworkExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) {
        // 创建 Actor 系统
        val system = ActorSystem("agent-network-example")

        // 创建 Agent 网络
        val network = AgentNetwork(system)

        try {
            // 创建并添加 Agent
            val managerAgent = SpecializedAgent("manager", "我是一个管理者，负责分解任务和整合结果。")
            val researcherAgent = SpecializedAgent("researcher", "我是一个研究者，擅长收集和分析信息。")
            val writerAgent = SpecializedAgent("writer", "我是一个作家，擅长创作和表达。")
            val criticAgent = SpecializedAgent("critic", "我是一个评论家，擅长评估和提出改进建议。")

            // 添加 Agent 到网络
            network.addAgent(managerAgent)
            network.addAgent(researcherAgent)
            network.addAgent(writerAgent)
            network.addAgent(criticAgent)

            // 建立 Agent 之间的关系
            network.connectAgents("manager", "researcher", AgentRelation.MASTER)
            network.connectAgents("manager", "writer", AgentRelation.MASTER)
            network.connectAgents("manager", "critic", AgentRelation.PEER)
            network.connectAgents("researcher", "writer", AgentRelation.PEER)
            network.connectAgents("writer", "critic", AgentRelation.PEER)

            // 运行示例
            runBlocking {
                // 顺序协作示例
                println("=== 顺序协作示例 ===")
                val sequentialProtocol = SequentialProtocol(
                    agentSequence = listOf("researcher", "writer", "critic", "manager")
                )
                val sequentialResult = network.collaborate(
                    protocol = sequentialProtocol,
                    initiatorId = "manager",
                    task = "撰写一篇关于人工智能在医疗领域应用的文章"
                )

                println("顺序协作结果:")
                println("成功: ${sequentialResult.success}")
                println("参与者: ${sequentialResult.participants.joinToString()}")
                println("步骤数: ${sequentialResult.steps.size}")
                println("最终结果:")
                println(sequentialResult.result)
                println()

                // 层次协作示例
                println("=== 层次协作示例 ===")
                val hierarchicalProtocol = HierarchicalProtocol()
                val hierarchicalResult = network.collaborate(
                    protocol = hierarchicalProtocol,
                    initiatorId = "manager",
                    task = "撰写一篇关于人工智能在教育领域应用的文章"
                )

                println("层次协作结果:")
                println("成功: ${hierarchicalResult.success}")
                println("参与者: ${hierarchicalResult.participants.joinToString()}")
                println("步骤数: ${hierarchicalResult.steps.size}")
                println("最终结果:")
                println(hierarchicalResult.result)
                println()

                // 共识协作示例
                println("=== 共识协作示例 ===")
                val consensusProtocol = ConsensusProtocol()
                val consensusResult = network.collaborate(
                    protocol = consensusProtocol,
                    initiatorId = "manager",
                    task = "提出一个解决气候变化的创新方案"
                )

                println("共识协作结果:")
                println("成功: ${consensusResult.success}")
                println("参与者: ${consensusResult.participants.joinToString()}")
                println("步骤数: ${consensusResult.steps.size}")
                println("最终结果:")
                println(consensusResult.result)
            }
        } finally {
            // 关闭网络和系统
            network.shutdown()
            system.shutdown()
        }
    }

    /**
     * 专业化 Agent 实现
     */
    private class SpecializedAgent(
        override val name: String,
        private val specialization: String
    ) : Agent {
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
            // 模拟 Agent 响应
            val sender = options.metadata?.get("sender") ?: "unknown"
            val response = when {
                prompt.contains("分析并分解任务") -> {
                    "1. 收集相关领域的最新研究和应用\n" +
                    "2. 撰写文章的主体内容\n" +
                    "3. 评估文章质量并提出改进建议"
                }
                prompt.contains("提出解决方案") -> {
                    "作为$name，$specialization 我的解决方案是：\n" +
                    "我们可以通过结合人工智能、可再生能源和社区参与来解决气候变化问题。具体来说，我们可以开发智能电网系统，优化能源使用；推广太阳能和风能等可再生能源；建立社区参与机制，提高公众意识。"
                }
                prompt.contains("评估以下解决方案") -> {
                    "评估结果：\n" +
                    "manager: 8分\n" +
                    "researcher: 7分\n" +
                    "writer: 9分\n" +
                    "critic: 6分"
                }
                prompt.contains("整合以下改进建议") -> {
                    "最终解决方案：\n" +
                    "我们提出一个综合性的气候变化解决方案，结合了技术创新、政策支持和社区参与三个方面。在技术方面，我们将开发智能电网和能源管理系统，优化能源使用效率；在政策方面，我们建议制定碳税和补贴政策，鼓励绿色技术发展；在社区参与方面，我们将建立教育项目和激励机制，提高公众参与度。这个方案不仅考虑了短期减排目标，也关注长期可持续发展。"
                }
                else -> {
                    "作为$name，$specialization 我的回应是：\n" +
                    "关于\"$prompt\"，我认为这是一个重要的话题。从我的专业角度来看，我们需要综合考虑各种因素，包括技术可行性、经济效益和社会影响。我建议采取系统性的方法，结合多方面的专业知识来解决这个问题。"
                }
            }

            return AgentResponse(
                text = response,
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            val response = generate(prompt, AgentGenerateOptions())
            return AgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(response.text)
            )
        }

        override suspend fun reset() {
            // 不做任何操作
        }

        override suspend fun getState(): AgentState? {
            return null
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            return null
        }

        override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun createVersion(
            instructions: String,
            name: String?,
            description: String?,
            metadata: Map<String, String>,
            activateImmediately: Boolean
        ): AgentVersion? {
            return null
        }

        override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
            return emptyList()
        }

        override suspend fun getActiveVersion(): AgentVersion? {
            return null
        }

        override suspend fun activateVersion(versionId: String): AgentVersion? {
            return null
        }

        override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
            return null
        }
    }
}
