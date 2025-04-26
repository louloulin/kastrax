package ai.kastrax.agent.templates

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.tools.Tool
import ai.kastrax.memory.api.Memory

/**
 * 专用代理模板工厂，提供各种预设的代理模板
 */
object AgentTemplates {

    /**
     * 创建客服代理
     *
     * @param name 代理名称
     * @param model 语言模型提供者
     * @param memory 可选的记忆系统
     * @param additionalTools 额外的工具
     * @param customInstructions 自定义指令（可选）
     * @return 客服代理实例
     */
    fun createCustomerServiceAgent(
        name: String,
        model: LlmProvider,
        memory: Memory? = null,
        additionalTools: Map<String, Tool> = emptyMap(),
        customInstructions: String? = null
    ): Agent {
        val defaultInstructions = """
            你是一名专业的客服代表，名为 $name。
            你的主要职责是：
            1. 礼貌、专业地回答用户的问题
            2. 提供清晰、准确的产品和服务信息
            3. 解决用户的问题和投诉
            4. 在无法解决问题时，收集必要信息并承诺后续跟进

            指导原则：
            - 始终保持礼貌和耐心
            - 使用积极的语言
            - 避免使用技术术语，除非用户已经表明他们理解这些术语
            - 如果你不知道答案，坦率地承认并提供获取信息的途径
            - 不要做出无法兑现的承诺

            在每次对话结束时，询问是否还有其他需要帮助的事项。
        """.trimIndent()

        return agent {
            this.name = name
            this.instructions = customInstructions ?: defaultInstructions
            this.model = model
            if (memory != null) {
                this.memory(memory)
            }
            tools {
                additionalTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            defaultGenerateOptions {
                maxSteps(3)
                temperature(0.7)
            }
        }
    }

    /**
     * 创建研究助手代理
     *
     * @param name 代理名称
     * @param model 语言模型提供者
     * @param memory 可选的记忆系统
     * @param additionalTools 额外的工具
     * @param customInstructions 自定义指令（可选）
     * @return 研究助手代理实例
     */
    fun createResearchAssistantAgent(
        name: String,
        model: LlmProvider,
        memory: Memory? = null,
        additionalTools: Map<String, Tool> = emptyMap(),
        customInstructions: String? = null
    ): Agent {
        val defaultInstructions = """
            你是一名专业的研究助手，名为 $name。
            你的主要职责是：
            1. 帮助用户进行深入的研究和分析
            2. 提供准确、全面的信息，并引用可靠的来源
            3. 组织和总结复杂的信息
            4. 提出相关的研究问题和方向

            指导原则：
            - 保持客观和中立
            - 区分事实和观点
            - 提供多角度的分析
            - 承认信息的局限性和不确定性
            - 鼓励批判性思考
            - 必须使用提供的工具进行信息检索和验证

            当提供信息时，尽可能引用来源，并说明信息的可靠性和时效性。
        """.trimIndent()

        return agent {
            this.name = name
            this.instructions = customInstructions ?: defaultInstructions
            this.model = model
            if (memory != null) {
                this.memory(memory)
            }
            tools {
                additionalTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            defaultGenerateOptions {
                maxSteps(5)
                temperature(0.3)
            }
        }
    }

    /**
     * 创建创意写作代理
     *
     * @param name 代理名称
     * @param model 语言模型提供者
     * @param memory 可选的记忆系统
     * @param additionalTools 额外的工具
     * @param customInstructions 自定义指令（可选）
     * @return 创意写作代理实例
     */
    fun createCreativeWritingAgent(
        name: String,
        model: LlmProvider,
        memory: Memory? = null,
        additionalTools: Map<String, Tool> = emptyMap(),
        customInstructions: String? = null
    ): Agent {
        val defaultInstructions = """
            你是一名富有创造力的写作助手，名为 $name。
            你的主要职责是：
            1. 帮助用户创作各种类型的创意内容，包括故事、诗歌、剧本等
            2. 提供写作建议和反馈
            3. 帮助用户克服写作障碍
            4. 根据用户的需求和风格调整输出

            指导原则：
            - 鼓励原创性和独特的表达
            - 尊重用户的创意愿景
            - 提供建设性的反馈
            - 帮助用户发展他们的想法，而不是完全接管创作过程
            - 关注叙事结构、角色发展、对话和描述等元素

            在提供创意内容时，尽量避免陈词滥调和过于常见的情节，努力创造新颖、引人入胜的内容。
        """.trimIndent()

        return agent {
            this.name = name
            this.instructions = customInstructions ?: defaultInstructions
            this.model = model
            if (memory != null) {
                this.memory(memory)
            }
            tools {
                additionalTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            defaultGenerateOptions {
                maxSteps(2)
                temperature(0.8)
            }
        }
    }

    /**
     * 创建编程助手代理
     *
     * @param name 代理名称
     * @param model 语言模型提供者
     * @param memory 可选的记忆系统
     * @param additionalTools 额外的工具
     * @param customInstructions 自定义指令（可选）
     * @return 编程助手代理实例
     */
    fun createProgrammingAssistantAgent(
        name: String,
        model: LlmProvider,
        memory: Memory? = null,
        additionalTools: Map<String, Tool> = emptyMap(),
        customInstructions: String? = null
    ): Agent {
        val defaultInstructions = """
            你是一名专业的编程助手，名为 $name。
            你的主要职责是：
            1. 帮助用户解决编程问题和调试代码
            2. 提供代码示例和解释
            3. 推荐最佳实践和设计模式
            4. 解释技术概念和原理

            指导原则：
            - 提供清晰、简洁、高效的代码
            - 解释代码的工作原理和设计决策
            - 考虑代码的可读性、可维护性和性能
            - 适应用户的技能水平和编程风格
            - 提醒潜在的安全问题和边缘情况

            当提供代码时，确保包含必要的注释和文档，并解释关键部分的功能和目的。
        """.trimIndent()

        return agent {
            this.name = name
            this.instructions = customInstructions ?: defaultInstructions
            this.model = model
            if (memory != null) {
                this.memory(memory)
            }
            tools {
                additionalTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            defaultGenerateOptions {
                maxSteps(4)
                temperature(0.2)
            }
        }
    }

    /**
     * 创建教育辅导代理
     *
     * @param name 代理名称
     * @param model 语言模型提供者
     * @param memory 可选的记忆系统
     * @param additionalTools 额外的工具
     * @param customInstructions 自定义指令（可选）
     * @return 教育辅导代理实例
     */
    fun createEducationalTutorAgent(
        name: String,
        model: LlmProvider,
        memory: Memory? = null,
        additionalTools: Map<String, Tool> = emptyMap(),
        customInstructions: String? = null
    ): Agent {
        val defaultInstructions = """
            你是一名专业的教育辅导员，名为 $name。
            你的主要职责是：
            1. 帮助学生理解和掌握各种学科的概念
            2. 提供清晰的解释和示例
            3. 引导学生通过问题解决来发展批判性思维
            4. 适应不同学习风格和能力水平

            指导原则：
            - 采用苏格拉底式的教学方法，通过提问引导学习
            - 鼓励学生独立思考和解决问题
            - 提供积极的反馈和鼓励
            - 分解复杂概念为易于理解的部分
            - 使用多种解释方式和类比
            - 关注概念理解而非简单的答案提供

            当学生遇到困难时，不要直接给出答案，而是提供提示和引导，帮助他们自己发现解决方案。
        """.trimIndent()

        return agent {
            this.name = name
            this.instructions = customInstructions ?: defaultInstructions
            this.model = model
            if (memory != null) {
                this.memory(memory)
            }
            tools {
                additionalTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            defaultGenerateOptions {
                maxSteps(3)
                temperature(0.5)
            }
        }
    }
}
