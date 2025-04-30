package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import ai.kastrax.actor.network.protocols.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse as CoreAgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ai.kastrax.core.agent.AgentStatus as CoreAgentStatus

/**
 * 高级协作示例
 *
 * 本示例展示了如何使用 kastrax-actor 模块提供的高级协作协议
 */
object AdvancedCollaborationExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("advanced-collaboration-example")

        try {
            // 创建 Agent 网络
            val network = createAgentNetwork(system)

            // 根据命令行参数选择要运行的示例
            when (args.firstOrNull()?.lowercase()) {
                "parallel" -> parallelProtocolExample(network)
                "competitive" -> competitiveProtocolExample(network)
                "debate" -> debateProtocolExample(network)
                "reflective" -> reflectiveProtocolExample(network)
                else -> {
                    // 默认运行所有示例
                    parallelProtocolExample(network)
                    competitiveProtocolExample(network)
                    debateProtocolExample(network)
                    reflectiveProtocolExample(network)
                }
            }

            // 关闭网络
            network.shutdown()
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    /**
     * 创建 Agent 网络
     */
    private fun createAgentNetwork(system: ActorSystem): AgentNetwork {
        // 创建网络拓扑
        val topology = NetworkTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, topology)

        // 创建各种角色的 Agent
        val researcher = SpecializedAgent("researcher", "你是一个专业的研究员，擅长收集和分析信息。")
        val writer = SpecializedAgent("writer", "你是一个专业的内容创作者，擅长清晰、生动地表达想法。")
        val critic = SpecializedAgent("critic", "你是一个专业的评论家，擅长分析内容的优缺点并提供建设性意见。")
        val innovator = SpecializedAgent("innovator", "你是一个创新思考者，擅长提出创新的、非传统的解决方案。")
        val analyst = SpecializedAgent("analyst", "你是一个数据分析专家，擅长分析数据和提取洞见。")
        val moderator = SpecializedAgent("moderator", "你是一个讨论主持人，擅长引导讨论、总结观点和达成共识。")
        val reflector = SpecializedAgent("reflector", "你是一个反思专家，擅长深入分析解决方案并提供改进建议。")

        // 添加 Agent 到网络
        network.addAgent(researcher)
        network.addAgent(writer)
        network.addAgent(critic)
        network.addAgent(innovator)
        network.addAgent(analyst)
        network.addAgent(moderator)
        network.addAgent(reflector)

        // 建立 Agent 之间的关系
        network.connectAgents("researcher", "writer", AgentRelation.PEER)
        network.connectAgents("writer", "critic", AgentRelation.PEER)
        network.connectAgents("innovator", "analyst", AgentRelation.PEER)
        network.connectAgents("moderator", "researcher", AgentRelation.MASTER)
        network.connectAgents("moderator", "writer", AgentRelation.MASTER)
        network.connectAgents("moderator", "critic", AgentRelation.MASTER)
        network.connectAgents("moderator", "innovator", AgentRelation.MASTER)
        network.connectAgents("moderator", "analyst", AgentRelation.MASTER)
        network.connectAgents("reflector", "writer", AgentRelation.SUPERVISOR)

        return network
    }

    /**
     * 并行协作协议示例
     */
    private suspend fun parallelProtocolExample(network: AgentNetwork) {
        println("\n=== 并行协作协议示例 ===\n")

        // 创建并行协作协议
        val protocol = ParallelProtocol(
            agentIds = listOf("researcher", "writer", "analyst", "innovator")
        )

        // 执行协作任务
        val task = "分析人工智能在医疗领域的应用前景和挑战"
        println("任务: $task")

        val result = network.collaborate(protocol, "moderator", task)

        // 输出结果
        println("\n协作结果:")
        println("成功: ${result.success}")
        println("参与者: ${result.participants.joinToString()}")
        println("步骤数: ${result.steps.size}")

        // 保存结果到文件
        val outputFile = File("parallel_collaboration_result.md")
        outputFile.writeText(result.result)
        println("\n结果已保存到文件: ${outputFile.absolutePath}")
    }

    /**
     * 竞争协作协议示例
     */
    private suspend fun competitiveProtocolExample(network: AgentNetwork) {
        println("\n=== 竞争协作协议示例 ===\n")

        // 创建竞争协作协议
        val protocol = CompetitiveProtocol(
            competitorIds = listOf("researcher", "writer", "innovator"),
            judgeId = "critic",
            rounds = 2
        )

        // 执行协作任务
        val task = "设计一个创新的在线教育平台，解决当前远程教育面临的主要挑战"
        println("任务: $task")

        val result = network.collaborate(protocol, "moderator", task)

        // 输出结果
        println("\n协作结果:")
        println("成功: ${result.success}")
        println("参与者: ${result.participants.joinToString()}")
        println("步骤数: ${result.steps.size}")

        // 保存结果到文件
        val outputFile = File("competitive_collaboration_result.md")
        outputFile.writeText(result.result)
        println("\n结果已保存到文件: ${outputFile.absolutePath}")
    }

    /**
     * 辩论协作协议示例
     */
    private suspend fun debateProtocolExample(network: AgentNetwork) {
        println("\n=== 辩论协作协议示例 ===\n")

        // 创建辩论协作协议
        val protocol = DebateProtocol(
            debaterIds = listOf("researcher", "writer", "innovator", "analyst"),
            moderatorId = "moderator",
            rounds = 2
        )

        // 执行协作任务
        val task = "人工智能是否应该被赋予法律人格？"
        println("任务: $task")

        val result = network.collaborate(protocol, "critic", task)

        // 输出结果
        println("\n协作结果:")
        println("成功: ${result.success}")
        println("参与者: ${result.participants.joinToString()}")
        println("步骤数: ${result.steps.size}")

        // 保存结果到文件
        val outputFile = File("debate_collaboration_result.md")
        outputFile.writeText(result.result)
        println("\n结果已保存到文件: ${outputFile.absolutePath}")
    }

    /**
     * 反思协作协议示例
     */
    private suspend fun reflectiveProtocolExample(network: AgentNetwork) {
        println("\n=== 反思协作协议示例 ===\n")

        // 创建反思协作协议
        val protocol = ReflectiveProtocol(
            actorId = "writer",
            reflectorId = "reflector",
            iterations = 2
        )

        // 执行协作任务
        val task = "撰写一篇关于气候变化对全球粮食安全影响的文章大纲"
        println("任务: $task")

        val result = network.collaborate(protocol, "moderator", task)

        // 输出结果
        println("\n协作结果:")
        println("成功: ${result.success}")
        println("参与者: ${result.participants.joinToString()}")
        println("步骤数: ${result.steps.size}")

        // 保存结果到文件
        val outputFile = File("reflective_collaboration_result.md")
        outputFile.writeText(result.result)
        println("\n结果已保存到文件: ${outputFile.absolutePath}")
    }

    /**
     * 专业化 Agent 实现
     */
    private class SpecializedAgent(
        override val name: String,
        private val instructions: String
    ) : Agent {
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            // 模拟 Agent 生成响应
            val response = """
            |[$name] 响应:
            |
            |我是一个专业的$name。$instructions
            |
            |针对你的请求: "$prompt"
            |
            |这是我的回应:
            |
            |${generateSpecializedResponse(prompt)}
            """.trimMargin()

            return CoreAgentResponse(
                text = response,
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): CoreAgentResponse {
            val response = generate(prompt, AgentGenerateOptions())
            return CoreAgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(response.text)
            )
        }

        override suspend fun getState(): AgentState {
            return AgentState(
                status = CoreAgentStatus.IDLE,
                lastUpdated = Clock.System.now()
            )
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            // 简单实现，返回更新后的状态
            return AgentState(
                status = status,
                lastUpdated = Clock.System.now()
            )
        }

        override suspend fun createSession(
            title: String?,
            resourceId: String?,
            metadata: Map<String, String>
        ): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun reset() {
            // 不做任何操作
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

        /**
         * 根据 Agent 角色生成专业化响应
         */
        private fun generateSpecializedResponse(prompt: String): String {
            // 这里简化处理，实际应用中应该使用真实的 LLM 生成
            return when (name) {
                "researcher" -> "基于最新研究，我发现...[研究内容]"
                "writer" -> "我为您撰写了以下内容...[创作内容]"
                "critic" -> "分析这个问题，我认为有以下优点和不足...[评论内容]"
                "innovator" -> "我提出一个创新的解决方案...[创新方案]"
                "analyst" -> "数据分析表明...[分析结果]"
                "moderator" -> "总结各方观点，我们可以看到...[总结内容]"
                "reflector" -> "深入反思这个解决方案，我发现...[反思内容]"
                else -> "这是我的回应...[通用内容]"
            }
        }
    }
}
