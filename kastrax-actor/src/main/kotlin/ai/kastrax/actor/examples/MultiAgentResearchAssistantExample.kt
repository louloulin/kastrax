package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.AgentRequest
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.network.AgentNetwork
import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import ai.kastrax.actor.network.protocols.ParallelProtocol
import ai.kastrax.actor.network.protocols.SequentialProtocol
import ai.kastrax.actor.network.topology.RoleBasedTopology
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

/**
 * 多智能体研究助手系统示例
 *
 * 本示例展示了如何使用 kastrax-actor 构建一个多智能体研究助手系统，
 * 其中多个专业化的 Agent 协作完成研究任务
 */
object MultiAgentResearchAssistantExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("multi-agent-research-assistant")

        try {
            // 创建研究助手系统
            val researchSystem = createResearchAssistantSystem(system)

            // 执行研究任务
            val topic = if (args.isNotEmpty()) args[0] else "量子计算在密码学中的应用"
            val depth = if (args.size > 1) args[1].toIntOrNull() ?: 2 else 2

            println("执行研究任务，主题: $topic，深度: $depth")
            val result = researchSystem.conductResearch(topic, depth)

            // 保存研究结果到文件
            val outputFile = File("research_result.md")
            outputFile.writeText(result)
            println("研究结果已保存到: ${outputFile.absolutePath}")

            // 关闭系统
            researchSystem.shutdown()
        } finally {
            // 关闭 Actor 系统
            system.shutdown()
        }
    }

    /**
     * 创建研究助手系统
     */
    private fun createResearchAssistantSystem(system: ActorSystem): ResearchAssistantSystem {
        // 创建基于角色的拓扑
        val topology = RoleBasedTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建研究助手系统
        return ResearchAssistantSystem(network, topology)
    }

    /**
     * 研究助手系统
     */
    private class ResearchAssistantSystem(
        private val network: AgentNetwork,
        private val topology: RoleBasedTopology
    ) {
        init {
            setupAgents()
        }

        /**
         * 设置 Agent
         */
        private fun setupAgents() {
            // 定义角色
            val coordinatorRole = RoleBasedTopology.Role(
                name = "coordinator",
                description = "研究协调员，负责分配任务和整合结果",
                capabilities = setOf("task_planning", "result_integration", "quality_control")
            )

            val researcherRole = RoleBasedTopology.Role(
                name = "researcher",
                description = "研究员，负责收集和分析信息",
                capabilities = setOf("information_gathering", "data_analysis", "fact_checking")
            )

            val expertRole = RoleBasedTopology.Role(
                name = "expert",
                description = "领域专家，提供专业知识和见解",
                capabilities = setOf("domain_knowledge", "critical_analysis", "insight_generation")
            )

            val writerRole = RoleBasedTopology.Role(
                name = "writer",
                description = "撰写者，负责整理和撰写研究内容",
                capabilities = setOf("content_creation", "summarization", "technical_writing")
            )

            val reviewerRole = RoleBasedTopology.Role(
                name = "reviewer",
                description = "审核员，负责审核内容质量和准确性",
                capabilities = setOf("quality_assessment", "fact_verification", "feedback_provision")
            )

            // 添加角色
            topology.addRole(coordinatorRole)
            topology.addRole(researcherRole)
            topology.addRole(expertRole)
            topology.addRole(writerRole)
            topology.addRole(reviewerRole)

            // 创建 Agent
            val coordinator = ResearchAgent(
                name = "coordinator",
                displayName = "研究协调员",
                expertise = "任务规划、协调和质量控制",
                skills = listOf("任务分解", "资源分配", "进度跟踪", "结果整合")
            )

            val generalResearcher = ResearchAgent(
                name = "general_researcher",
                displayName = "通用研究员",
                expertise = "广泛的信息收集和初步分析",
                skills = listOf("信息检索", "数据收集", "初步分析", "研究方法")
            )

            val specialistResearcher = ResearchAgent(
                name = "specialist_researcher",
                displayName = "专业研究员",
                expertise = "深入的专业领域研究",
                skills = listOf("深度分析", "专业文献研究", "数据挖掘", "趋势识别")
            )

            val techExpert = ResearchAgent(
                name = "tech_expert",
                displayName = "技术专家",
                expertise = "技术领域的专业知识",
                skills = listOf("技术评估", "技术趋势分析", "技术可行性分析", "技术比较")
            )

            val academicExpert = ResearchAgent(
                name = "academic_expert",
                displayName = "学术专家",
                expertise = "学术研究和理论知识",
                skills = listOf("理论分析", "学术文献评估", "研究方法论", "学术写作")
            )

            val technicalWriter = ResearchAgent(
                name = "technical_writer",
                displayName = "技术写作者",
                expertise = "技术内容的清晰表达",
                skills = listOf("技术写作", "内容组织", "技术术语使用", "图表说明")
            )

            val contentEditor = ResearchAgent(
                name = "content_editor",
                displayName = "内容编辑",
                expertise = "内容编辑和改进",
                skills = listOf("内容编辑", "结构优化", "风格统一", "可读性提升")
            )

            val factChecker = ResearchAgent(
                name = "fact_checker",
                displayName = "事实核查员",
                expertise = "信息准确性验证",
                skills = listOf("事实核查", "来源验证", "准确性评估", "错误识别")
            )

            // 添加 Agent 到网络
            network.addAgent(coordinator)
            network.addAgent(generalResearcher)
            network.addAgent(specialistResearcher)
            network.addAgent(techExpert)
            network.addAgent(academicExpert)
            network.addAgent(technicalWriter)
            network.addAgent(contentEditor)
            network.addAgent(factChecker)

            // 分配角色
            topology.assignRole("coordinator", "coordinator")
            topology.assignRole("general_researcher", "researcher")
            topology.assignRole("specialist_researcher", "researcher")
            topology.assignRole("tech_expert", "expert")
            topology.assignRole("academic_expert", "expert")
            topology.assignRole("technical_writer", "writer")
            topology.assignRole("content_editor", "writer")
            topology.assignRole("fact_checker", "reviewer")

            // 创建团队结构
            topology.createTeamStructure(
                leaderRole = "coordinator",
                memberRoles = listOf("researcher", "expert", "writer", "reviewer")
            )

            // 更新节点关系
            topology.updateNodeRelations()
        }

        /**
         * 执行研究任务
         *
         * @param topic 研究主题
         * @param depth 研究深度（1-3，数字越大研究越深入）
         * @return 研究结果
         */
        suspend fun conductResearch(topic: String, depth: Int = 2): String {
            println("开始研究任务: $topic (深度: $depth)")

            // 第一阶段：任务规划和初步研究
            val planningResult = planResearch(topic, depth)

            // 第二阶段：深入研究
            val researchResult = conductDeepResearch(topic, planningResult, depth)

            // 第三阶段：专家分析
            val expertAnalysisResult = analyzeResearch(topic, researchResult, depth)

            // 第四阶段：内容创作
            val draftResult = createResearchDraft(topic, researchResult, expertAnalysisResult, depth)

            // 第五阶段：审核和完善
            val finalResult = reviewAndFinalize(topic, draftResult, depth)

            return finalResult
        }

        /**
         * 第一阶段：任务规划和初步研究
         */
        private suspend fun planResearch(topic: String, depth: Int): String {
            println("阶段 1: 任务规划和初步研究")

            // 协调员制定研究计划
            val planningTask = """
            |# 研究任务规划
            |
            |## 研究主题
            |$topic
            |
            |## 研究深度
            |$depth (1-3，数字越大研究越深入)
            |
            |## 要求
            |请制定一个详细的研究计划，包括：
            |1. 研究目标和范围
            |2. 关键研究问题
            |3. 需要探索的主要领域
            |4. 研究方法和资源
            |5. 任务分配建议
            |
            |提供一个结构化的研究计划，以指导后续研究工作。
            """.trimMargin()

            val planningResponse = network.askAgent("system", "coordinator", planningTask)

            // 通用研究员进行初步研究
            val initialResearchTask = """
            |# 初步研究任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究计划
            |${planningResponse.text}
            |
            |## 要求
            |请进行初步研究，收集关于该主题的基本信息，包括：
            |1. 主题背景和概述
            |2. 关键概念和定义
            |3. 主要参与者和组织
            |4. 当前发展状态
            |5. 主要挑战和机遇
            |
            |提供全面但简洁的初步研究结果，为后续深入研究奠定基础。
            """.trimMargin()

            val initialResearchResponse = network.askAgent("coordinator", "general_researcher", initialResearchTask)

            // 整合计划和初步研究
            val planningResult = """
            |# 研究计划和初步发现
            |
            |## 研究计划
            |${planningResponse.text}
            |
            |## 初步研究结果
            |${initialResearchResponse.text}
            """.trimMargin()

            return planningResult
        }

        /**
         * 第二阶段：深入研究
         */
        private suspend fun conductDeepResearch(topic: String, planningResult: String, depth: Int): String {
            println("阶段 2: 深入研究")

            // 使用并行协议让多个研究员同时进行研究
            val researchProtocol = ParallelProtocol(
                agentIds = listOf("general_researcher", "specialist_researcher"),
                aggregator = { results ->
                    """
                    |# 综合研究结果
                    |
                    |## 通用研究
                    |${results[0]}
                    |
                    |## 专业研究
                    |${results[1]}
                    """.trimMargin()
                }
            )

            // 研究任务
            val researchTask = """
            |# 深入研究任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究深度
            |$depth (1-3，数字越大研究越深入)
            |
            |## 前期工作
            |$planningResult
            |
            |## 要求
            |请进行深入研究，探索该主题的详细方面，包括：
            |1. 历史发展和演变
            |2. 技术细节和工作原理
            |3. 最新研究进展和突破
            |4. 应用案例和实际影响
            |5. 未来趋势和发展方向
            |
            |根据研究深度 $depth，调整研究的深度和广度，提供详实的研究结果。
            """.trimMargin()

            // 执行研究任务
            val researchResult = network.collaborate(researchProtocol, "coordinator", researchTask)

            return researchResult.result
        }

        /**
         * 第三阶段：专家分析
         */
        private suspend fun analyzeResearch(topic: String, researchResult: String, depth: Int): String {
            println("阶段 3: 专家分析")

            // 使用并行协议让多个专家同时进行分析
            val analysisProtocol = ParallelProtocol(
                agentIds = listOf("tech_expert", "academic_expert"),
                aggregator = { results ->
                    """
                    |# 专家分析结果
                    |
                    |## 技术专家分析
                    |${results[0]}
                    |
                    |## 学术专家分析
                    |${results[1]}
                    """.trimMargin()
                }
            )

            // 分析任务
            val analysisTask = """
            |# 专家分析任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究深度
            |$depth (1-3，数字越大研究越深入)
            |
            |## 研究结果
            |$researchResult
            |
            |## 要求
            |请从您的专业角度对研究结果进行深入分析，包括：
            |1. 关键发现的评估和解读
            |2. 技术/理论的优势和局限性
            |3. 与相关领域的关联和影响
            |4. 潜在的创新点和突破口
            |5. 实际应用的可行性和挑战
            |
            |提供专业、深入的分析见解，突出您专业领域的独特视角。
            """.trimMargin()

            // 执行分析任务
            val analysisResult = network.collaborate(analysisProtocol, "coordinator", analysisTask)

            return analysisResult.result
        }

        /**
         * 第四阶段：内容创作
         */
        private suspend fun createResearchDraft(
            topic: String,
            researchResult: String,
            expertAnalysisResult: String,
            depth: Int
        ): String {
            println("阶段 4: 内容创作")

            // 使用顺序协议让写作者和编辑依次工作
            val writingProtocol = SequentialProtocol(
                agentSequence = listOf("technical_writer", "content_editor")
            )

            // 写作任务
            val writingTask = """
            |# 研究报告撰写任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究深度
            |$depth (1-3，数字越大研究越深入)
            |
            |## 研究结果
            |$researchResult
            |
            |## 专家分析
            |$expertAnalysisResult
            |
            |## 要求
            |请根据研究结果和专家分析，撰写一份完整的研究报告，包括：
            |1. 执行摘要
            |2. 引言和背景
            |3. 研究方法
            |4. 主要发现（按主题组织）
            |5. 专家分析和见解
            |6. 应用和影响
            |7. 未来展望
            |8. 结论
            |9. 参考资料
            |
            |技术写作者负责初稿撰写，内容编辑负责优化结构、提升可读性和确保一致性。
            |根据研究深度 $depth，调整内容的深度和技术细节水平。
            |使用 Markdown 格式，确保结构清晰，内容专业。
            """.trimMargin()

            // 执行写作任务
            val writingResult = network.collaborate(writingProtocol, "coordinator", writingTask)

            return writingResult.result
        }

        /**
         * 第五阶段：审核和完善
         */
        private suspend fun reviewAndFinalize(topic: String, draftResult: String, depth: Int): String {
            println("阶段 5: 审核和完善")

            // 事实核查
            val factCheckTask = """
            |# 事实核查任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究报告草稿
            |$draftResult
            |
            |## 要求
            |请对研究报告进行全面的事实核查，包括：
            |1. 验证关键事实和数据的准确性
            |2. 检查引用和参考的正确性
            |3. 识别任何不准确、误导或需要澄清的内容
            |4. 提供具体的修改建议
            |
            |提供详细的事实核查报告，确保研究内容的准确性和可靠性。
            """.trimMargin()

            val factCheckResponse = network.askAgent("coordinator", "fact_checker", factCheckTask)

            // 最终整合
            val finalizationTask = """
            |# 研究报告最终整合任务
            |
            |## 研究主题
            |$topic
            |
            |## 研究报告草稿
            |$draftResult
            |
            |## 事实核查结果
            |${factCheckResponse.text}
            |
            |## 要求
            |请根据事实核查结果，对研究报告进行最终整合和完善，包括：
            |1. 修正任何不准确或有误的内容
            |2. 补充必要的信息和解释
            |3. 确保内容的一致性和连贯性
            |4. 优化整体结构和格式
            |5. 添加适当的图表说明（如适用）
            |
            |生成最终的研究报告，确保内容准确、全面、专业。
            """.trimMargin()

            val finalizationResponse = network.askAgent("coordinator", "coordinator", finalizationTask)

            return finalizationResponse.text
        }

        /**
         * 关闭系统
         */
        fun shutdown() {
            network.shutdown()
        }
    }

    /**
     * 研究 Agent 实现
     */
    private class ResearchAgent(
        override val name: String,
        private val displayName: String,
        private val expertise: String,
        private val skills: List<String>
    ) : Agent {
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): CoreAgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): CoreAgentResponse {
            // 构建响应
            val response = """
            |# ${displayName}的响应
            |
            |## 专业领域
            |$expertise
            |
            |## 技能
            |${skills.joinToString(", ")}
            |
            |## 回应
            |作为一名专业的${displayName}，我将根据我的专业知识和技能来完成这个任务。
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
                status = AgentStatus.IDLE,
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
         * 生成专业化响应
         */
        private fun generateSpecializedResponse(prompt: String): String {
            // 这里简化处理，实际应用中应该使用真实的 LLM 生成
            return when (name) {
                "coordinator" -> "我已经分析了任务需求，并制定了详细的研究计划，包括任务分解、资源分配和时间安排...[协调内容]"
                "general_researcher" -> "根据我的初步研究，以下是关于该主题的基本信息和背景...[通用研究内容]"
                "specialist_researcher" -> "通过深入研究，我发现了以下专业领域的关键信息和见解...[专业研究内容]"
                "tech_expert" -> "从技术角度分析，该主题涉及的关键技术包括...[技术分析内容]"
                "academic_expert" -> "从学术理论角度看，该研究领域的主要范式和方法论包括...[学术分析内容]"
                "technical_writer" -> "我已经整理了研究发现，并撰写了结构清晰的技术内容...[技术写作内容]"
                "content_editor" -> "我已经审阅并优化了内容结构和表达，提高了可读性和连贯性...[编辑内容]"
                "fact_checker" -> "我已经验证了报告中的关键事实和数据，以下是我的核查结果...[事实核查内容]"
                else -> "这是我的专业回应...[通用内容]"
            }
        }
    }
}
