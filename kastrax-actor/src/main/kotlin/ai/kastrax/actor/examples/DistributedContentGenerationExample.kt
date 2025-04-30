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
import ai.kastrax.actor.network.topology.HierarchicalTopology
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
 * 分布式内容生成系统示例
 *
 * 本示例展示了如何使用 kastrax-actor 构建一个分布式内容生成系统，
 * 其中多个专业化的 Agent 协作生成内容
 */
object DistributedContentGenerationExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("distributed-content-generation")

        try {
            // 创建内容生成系统
            val contentSystem = createContentGenerationSystem(system)

            // 生成内容
            val topic = if (args.isNotEmpty()) args[0] else "人工智能在医疗领域的应用"
            val format = if (args.size > 1) args[1] else "article"

            println("生成内容，主题: $topic，格式: $format")
            val content = contentSystem.generateContent(topic, format)

            // 保存内容到文件
            val outputFile = File("generated_content.md")
            outputFile.writeText(content)
            println("内容已保存到: ${outputFile.absolutePath}")

            // 关闭系统
            contentSystem.shutdown()
        } finally {
            // 关闭 Actor 系统
            system.shutdown()
        }
    }

    /**
     * 创建内容生成系统
     */
    private fun createContentGenerationSystem(system: ActorSystem): ContentGenerationSystem {
        // 创建层次拓扑
        val topology = HierarchicalTopology()

        // 创建 Agent 网络
        val network = AgentNetwork(system, NetworkTopology())

        // 创建内容生成系统
        return ContentGenerationSystem(network, topology)
    }

    /**
     * 内容生成系统
     */
    private class ContentGenerationSystem(
        private val network: AgentNetwork,
        private val topology: HierarchicalTopology
    ) {
        init {
            setupAgents()
        }

        /**
         * 设置 Agent
         */
        private fun setupAgents() {
            // 创建编辑 Agent
            val editor = ContentAgent(
                name = "editor",
                role = "编辑",
                expertise = "负责协调内容生成过程，分配任务，审核和整合内容",
                skills = listOf("内容规划", "质量控制", "编辑", "内容整合")
            )

            // 创建研究员 Agent
            val researcher = ContentAgent(
                name = "researcher",
                role = "研究员",
                expertise = "负责收集和分析相关信息，提供事实依据和数据支持",
                skills = listOf("信息收集", "数据分析", "研究", "事实核查")
            )

            // 创建作家 Agent
            val writer = ContentAgent(
                name = "writer",
                role = "作家",
                expertise = "负责撰写内容，确保内容流畅、有吸引力",
                skills = listOf("写作", "叙事", "内容创作", "风格调整")
            )

            // 创建设计师 Agent
            val designer = ContentAgent(
                name = "designer",
                role = "设计师",
                expertise = "负责内容的视觉呈现，提供设计建议",
                skills = listOf("视觉设计", "排版", "图表设计", "美学")
            )

            // 创建审校员 Agent
            val proofreader = ContentAgent(
                name = "proofreader",
                role = "审校员",
                expertise = "负责检查内容的准确性、一致性和语法",
                skills = listOf("校对", "语法检查", "一致性检查", "质量控制")
            )

            // 添加 Agent 到网络
            network.addAgent(editor)
            network.addAgent(researcher)
            network.addAgent(writer)
            network.addAgent(designer)
            network.addAgent(proofreader)

            // 构建层次结构
            topology.addRootNode("editor")
            topology.addChildNode("editor", "researcher")
            topology.addChildNode("editor", "writer")
            topology.addChildNode("editor", "designer")
            topology.addChildNode("editor", "proofreader")

            // 建立 Agent 之间的关系
            network.connectAgents("researcher", "writer", AgentRelation.PEER)
            network.connectAgents("writer", "designer", AgentRelation.PEER)
            network.connectAgents("designer", "proofreader", AgentRelation.PEER)
        }

        /**
         * 生成内容
         *
         * @param topic 主题
         * @param format 格式（article, blog, report, presentation）
         * @return 生成的内容
         */
        suspend fun generateContent(topic: String, format: String): String {
            // 根据格式选择不同的生成流程
            return when (format.lowercase()) {
                "article" -> generateArticle(topic)
                "blog" -> generateBlogPost(topic)
                "report" -> generateReport(topic)
                "presentation" -> generatePresentation(topic)
                else -> generateArticle(topic) // 默认生成文章
            }
        }

        /**
         * 生成文章
         */
        private suspend fun generateArticle(topic: String): String {
            println("生成文章，主题: $topic")

            // 使用顺序协议生成文章
            val protocol = SequentialProtocol(
                agentSequence = listOf("researcher", "writer", "proofreader")
            )

            // 编辑分配任务
            val task = """
            |# 文章生成任务
            |
            |## 主题
            |$topic
            |
            |## 要求
            |1. 研究员：收集相关信息，提供事实依据和数据支持
            |2. 作家：基于研究员的信息撰写一篇完整的文章，包括引言、正文和结论
            |3. 审校员：检查文章的准确性、一致性和语法，提供修改建议
            |
            |## 格式
            |- 文章应包含标题、引言、正文（2-3个小节）和结论
            |- 使用 Markdown 格式
            |- 文章长度约 800-1200 字
            """.trimMargin()

            // 执行协作任务
            val result = network.collaborate(protocol, "editor", task)

            // 编辑整合内容
            val finalTask = """
            |# 文章整合任务
            |
            |## 原始内容
            |${result.result}
            |
            |## 要求
            |请整合上述内容，形成一篇完整、连贯的文章。确保文章结构清晰，内容准确，语言流畅。
            """.trimMargin()

            val finalResponse = network.askAgent("system", "editor", finalTask)

            return finalResponse.text
        }

        /**
         * 生成博客文章
         */
        private suspend fun generateBlogPost(topic: String): String {
            println("生成博客文章，主题: $topic")

            // 使用并行协议生成博客文章
            val protocol = ParallelProtocol(
                agentIds = listOf("researcher", "writer", "designer"),
                aggregator = { results ->
                    """
                    |# 博客文章素材
                    |
                    |## 研究内容
                    |${results[0]}
                    |
                    |## 初稿
                    |${results[1]}
                    |
                    |## 设计建议
                    |${results[2]}
                    """.trimMargin()
                }
            )

            // 编辑分配任务
            val task = """
            |# 博客文章生成任务
            |
            |## 主题
            |$topic
            |
            |## 要求
            |1. 研究员：收集相关信息，提供事实依据、数据支持和有趣的观点
            |2. 作家：撰写一篇有吸引力的博客文章，语言生动，风格轻松
            |3. 设计师：提供视觉呈现建议，包括图片、排版和视觉元素建议
            |
            |## 格式
            |- 博客文章应包含引人注目的标题、引言、正文和结论
            |- 使用 Markdown 格式
            |- 文章长度约 600-800 字
            |- 应当包含小标题、列表和其他视觉元素
            """.trimMargin()

            // 执行协作任务
            val result = network.collaborate(protocol, "editor", task)

            // 编辑和审校员整合内容
            val finalTask = """
            |# 博客文章整合任务
            |
            |## 素材
            |${result.result}
            |
            |## 要求
            |请整合上述素材，形成一篇完整、有吸引力的博客文章。确保文章风格轻松生动，
            |结构清晰，并融入设计建议中的视觉元素。最后进行审校，确保内容准确、语法正确。
            """.trimMargin()

            // 编辑和审校员协作完成最终内容
            val finalProtocol = SequentialProtocol(
                agentSequence = listOf("editor", "proofreader")
            )

            val finalResult = network.collaborate(finalProtocol, "system", finalTask)

            return finalResult.result
        }

        /**
         * 生成报告
         */
        private suspend fun generateReport(topic: String): String {
            println("生成报告，主题: $topic")

            // 使用层次协议生成报告
            // 编辑作为协调者，分配任务给各个专家

            // 第一阶段：研究和数据收集
            val researchTask = """
            |# 报告研究任务
            |
            |## 主题
            |$topic
            |
            |## 要求
            |请进行深入研究，收集相关信息、数据和事实依据。包括：
            |1. 背景信息
            |2. 关键数据和统计
            |3. 当前趋势和发展
            |4. 相关案例
            |
            |提供全面、准确的研究结果，为报告提供坚实的基础。
            """.trimMargin()

            val researchResponse = network.askAgent("editor", "researcher", researchTask)

            // 第二阶段：内容撰写
            val writingTask = """
            |# 报告撰写任务
            |
            |## 主题
            |$topic
            |
            |## 研究结果
            |${researchResponse.text}
            |
            |## 要求
            |基于上述研究结果，撰写一份专业、全面的报告。包括：
            |1. 执行摘要
            |2. 引言
            |3. 方法论
            |4. 发现和分析
            |5. 结论和建议
            |
            |使用正式、专业的语言，确保内容逻辑清晰，论证有力。
            """.trimMargin()

            val writingResponse = network.askAgent("editor", "writer", writingTask)

            // 第三阶段：设计和排版
            val designTask = """
            |# 报告设计任务
            |
            |## 报告内容
            |${writingResponse.text}
            |
            |## 要求
            |为上述报告提供设计和排版建议，包括：
            |1. 整体布局和结构
            |2. 图表和可视化建议
            |3. 排版和格式化建议
            |4. 视觉元素建议
            |
            |确保设计专业、清晰，增强报告的可读性和视觉吸引力。
            """.trimMargin()

            val designResponse = network.askAgent("editor", "designer", designTask)

            // 第四阶段：审校和质量控制
            val proofreadingTask = """
            |# 报告审校任务
            |
            |## 报告内容
            |${writingResponse.text}
            |
            |## 设计建议
            |${designResponse.text}
            |
            |## 要求
            |请审校上述报告内容，检查：
            |1. 事实准确性
            |2. 语法和拼写
            |3. 一致性和连贯性
            |4. 格式和引用
            |
            |提供详细的修改建议，确保报告质量达到专业标准。
            """.trimMargin()

            val proofreadingResponse = network.askAgent("editor", "proofreader", proofreadingTask)

            // 最终阶段：整合和完善
            val finalTask = """
            |# 报告整合任务
            |
            |## 报告内容
            |${writingResponse.text}
            |
            |## 设计建议
            |${designResponse.text}
            |
            |## 审校意见
            |${proofreadingResponse.text}
            |
            |## 要求
            |请整合上述内容，形成最终报告。应用设计建议和审校意见，确保报告：
            |1. 内容准确、全面
            |2. 结构清晰、逻辑连贯
            |3. 格式专业、一致
            |4. 视觉呈现专业、有效
            |
            |生成一份可直接使用的完整报告。
            """.trimMargin()

            val finalResponse = network.askAgent("system", "editor", finalTask)

            return finalResponse.text
        }

        /**
         * 生成演示文稿
         */
        private suspend fun generatePresentation(topic: String): String {
            println("生成演示文稿，主题: $topic")

            // 使用并行协议生成演示文稿内容
            val protocol = ParallelProtocol(
                agentIds = listOf("researcher", "writer", "designer")
            )

            // 编辑分配任务
            val task = """
            |# 演示文稿生成任务
            |
            |## 主题
            |$topic
            |
            |## 要求
            |1. 研究员：收集关键信息、数据和事实，提供演示文稿的核心内容
            |2. 作家：创建简洁、有力的演示文稿文本，包括关键信息点和叙述
            |3. 设计师：提供演示文稿的视觉设计、布局和结构建议
            |
            |## 格式
            |- 演示文稿应包含 10-15 张幻灯片
            |- 每张幻灯片应有标题和简洁的内容
            |- 使用 Markdown 格式描述每张幻灯片
            |- 包含引言、主要内容和结论部分
            """.trimMargin()

            // 执行协作任务
            val result = network.collaborate(protocol, "editor", task)

            // 编辑整合内容
            val finalTask = """
            |# 演示文稿整合任务
            |
            |## 素材
            |${result.result}
            |
            |## 要求
            |请整合上述素材，创建一份完整的演示文稿。每张幻灯片应包含：
            |1. 幻灯片编号和标题
            |2. 内容（简洁的要点或文本）
            |3. 视觉元素描述（如适用）
            |4. 演讲者注释（可选）
            |
            |确保演示文稿结构清晰，内容简洁有力，视觉设计专业。
            """.trimMargin()

            // 编辑和审校员协作完成最终内容
            val finalProtocol = SequentialProtocol(
                agentSequence = listOf("editor", "proofreader")
            )

            val finalResult = network.collaborate(finalProtocol, "system", finalTask)

            return finalResult.result
        }

        /**
         * 关闭系统
         */
        fun shutdown() {
            network.shutdown()
        }
    }

    /**
     * 内容 Agent 实现
     */
    private class ContentAgent(
        override val name: String,
        private val role: String,
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
            |# ${role}的响应
            |
            |## 任务
            |$prompt
            |
            |## 专业领域
            |$expertise
            |
            |## 技能
            |${skills.joinToString(", ")}
            |
            |## 回应
            |作为一名专业的${role}，我将根据我的专业知识和技能来完成这个任务。
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
            return when (role) {
                "编辑" -> "我已经审核了内容，并进行了必要的编辑和整合，确保内容质量和一致性。"
                "研究员" -> "根据我的研究，以下是相关的关键信息和数据...[研究内容]"
                "作家" -> "我已经撰写了内容，确保语言流畅、结构清晰...[创作内容]"
                "设计师" -> "我提供了视觉设计建议，包括布局、颜色方案和图表设计...[设计建议]"
                "审校员" -> "我已经检查了内容的准确性、一致性和语法，以下是我的修改建议...[审校意见]"
                else -> "这是我的专业回应...[通用内容]"
            }
        }
    }
}
