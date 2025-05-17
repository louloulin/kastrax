package ai.kastrax.code.agent

import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 基于 kastrax-core 的代码智能体实现
 *
 * 支持流式生成和标准生成模式
 */
class KastraxCodeAgent(
    private val agent: Agent,
    private val contextEngine: CodeContextEngine,
    private val toolRegistry: CodeToolRegistry,
    private val config: CodeAgentConfig = CodeAgentConfig()
) : KastraXCodeBase("CODE_AGENT"), CodeAgent {

    // 使用父类的logger

    /**
     * 生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @return 生成的代码
     */
    override suspend fun generateCode(prompt: String, language: String): String {
        logger.info("开始生成代码，提示：$prompt，语言：$language")

        val enhancedPrompt = """
            请根据以下描述生成 $language 代码：

            $prompt

            请只返回代码，不要包含解释或其他文本。
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.codeGenerationTemperature,
            maxTokens = config.codeGenerationMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码生成助手，擅长编写高质量的代码。请根据用户的描述生成代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        logger.info("调用 DeepSeek LLM 生成代码，消息数：${messages.size}")
        val startTime = System.currentTimeMillis()
        val response = agent.generate(messages, options)
        val endTime = System.currentTimeMillis()
        logger.info("收到 DeepSeek LLM 响应，耗时：${endTime - startTime}ms，原始响应：${response.text}")

        val extractedCode = extractCodeFromResponse(response.text, language)
        logger.info("提取的代码：$extractedCode")
        return extractedCode
    }

    /**
     * 流式生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @param options 流式选项
     * @return 生成的代码流
     */
    override suspend fun streamGenerateCode(prompt: String, language: String, options: AgentStreamOptions): Flow<String> {
        logger.info("开始流式生成代码，提示：$prompt，语言：$language")

        val enhancedPrompt = """
            请根据以下描述生成 $language 代码：

            $prompt

            请只返回代码，不要包含解释或其他文本。
        """.trimIndent()

        val streamOptions = AgentStreamOptions(
            temperature = config.codeGenerationTemperature,
            maxTokens = config.codeGenerationMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码生成助手，擅长编写高质量的代码。请根据用户的描述生成代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        logger.info("调用 DeepSeek LLM 流式生成代码，消息数：${messages.size}")
        val startTime = System.currentTimeMillis()

        try {
            // 使用 agent 的 generate 方法获取响应，因为 stream 方法不支持消息列表
            // 将消息列表转换为单个提示字符串
            val prompt = buildPromptFromMessages(messages)
            val response = agent.stream(prompt, streamOptions)

            // 收集完整的代码，用于提取
            val codeBuilder = StringBuilder()

            // 返回流
            return response.textStream?.map { chunk ->
                codeBuilder.append(chunk)
                chunk
            } ?: flow {
                // 如果没有流，返回空流
                logger.warn("没有收到流式响应")
            }
        } catch (e: Exception) {
            logger.error("流式生成代码失败", e)
            // 如果流式生成失败，返回空流
            return flow { }
        }
    }

    /**
     * 解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @return 代码解释
     */
    override suspend fun explainCode(code: String, detailLevel: DetailLevel): String {
        logger.debug("解释代码, 详细程度: $detailLevel")

        val detailLevelText = when (detailLevel) {
            DetailLevel.BRIEF -> "提供基本概述，简要解释代码的功能和目的"
            DetailLevel.NORMAL -> "提供标准解释，包括代码的功能和主要部分的说明"
            DetailLevel.DETAILED -> "提供详细解释，包括代码的功能、实现细节、算法原理、性能考虑和可能的边界情况"
        }

        val enhancedPrompt = """
            请解释以下代码：

            ```
            $code
            ```

            详细程度: $detailLevelText
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.codeExplanationTemperature,
            maxTokens = config.codeExplanationMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码解释助手，擅长解释代码的功能和实现。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        val response = agent.generate(messages, options)
        return response.text
    }

    /**
     * 流式解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @param options 流式选项
     * @return 代码解释流
     */
    override suspend fun streamExplainCode(code: String, detailLevel: DetailLevel, options: AgentStreamOptions): Flow<String> {
        logger.debug("流式解释代码, 详细程度: $detailLevel")

        val detailLevelText = when (detailLevel) {
            DetailLevel.BRIEF -> "提供基本概述，简要解释代码的功能和目的"
            DetailLevel.NORMAL -> "提供标准解释，包括代码的功能和主要部分的说明"
            DetailLevel.DETAILED -> "提供详细解释，包括代码的功能、实现细节、算法原理、性能考虑和可能的边界情况"
        }

        val enhancedPrompt = """
            请解释以下代码：

            ```
            $code
            ```

            详细程度: $detailLevelText
        """.trimIndent()

        val streamOptions = AgentStreamOptions(
            temperature = config.codeExplanationTemperature,
            maxTokens = config.codeExplanationMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码解释助手，擅长解释代码的功能和实现。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        try {
            // 使用 agent 的 generate 方法获取响应，因为 stream 方法不支持消息列表
            // 将消息列表转换为单个提示字符串
            val prompt = buildPromptFromMessages(messages)
            val response = agent.stream(prompt, streamOptions)

            // 返回流
            return response.textStream ?: flow {
                // 如果没有流，返回空流
                logger.warn("没有收到流式响应")
            }
        } catch (e: Exception) {
            logger.error("流式解释代码失败", e)
            // 如果流式生成失败，返回空流
            return flow { }
        }
    }

    /**
     * 重构代码
     *
     * @param code 代码文本
     * @param instructions 重构指令
     * @return 重构后的代码
     */
    override suspend fun refactorCode(code: String, instructions: String): String {
        logger.debug("重构代码, 指令: $instructions")

        val enhancedPrompt = """
            请根据以下指令重构代码：

            原始代码:
            ```
            $code
            ```

            重构指令:
            $instructions

            请只返回重构后的代码，不要包含解释或其他文本。
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.codeRefactoringTemperature,
            maxTokens = config.codeRefactoringMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码重构助手，擅长优化和重构代码以提高其质量。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        val response = agent.generate(messages, options)
        return extractCodeFromResponse(response.text)
    }

    /**
     * 生成测试
     *
     * @param code 代码文本
     * @param framework 测试框架
     * @return 生成的测试代码
     */
    override suspend fun generateTest(code: String, framework: String): String {
        logger.debug("生成测试, 框架: $framework")

        val enhancedPrompt = """
            请为以下代码生成 $framework 测试：

            ```
            $code
            ```

            请只返回测试代码，不要包含解释或其他文本。
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.testGenerationTemperature,
            maxTokens = config.testGenerationMaxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的测试生成助手，擅长为代码生成高质量的测试用例。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        val response = agent.generate(messages, options)
        return extractCodeFromResponse(response.text)
    }

    /**
     * 补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @return 补全的代码
     */
    override suspend fun complete(code: String, language: String, maxTokens: Int): String {
        logger.debug("补全代码, 语言: $language, 最大令牌数: $maxTokens")

        val enhancedPrompt = """
            请补全以下 $language 代码：

            ```
            $code
            ```

            请只返回补全的部分，不要重复已有代码，也不要包含解释或其他文本。
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.codeCompletionTemperature,
            maxTokens = maxTokens
        )

        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码补全助手，擅长根据上下文补全代码。请只返回补全的部分，不要重复已有代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = enhancedPrompt
            )
        )

        val response = agent.generate(messages, options)
        return extractCodeFromResponse(response.text)
    }

    /**
     * 从响应中提取代码
     *
     * @param response 响应文本
     * @param language 可选的编程语言
     * @return 提取的代码
     */
    private fun extractCodeFromResponse(response: String, language: String? = null): String {
        // 尝试提取代码块
        val codeBlockRegex = if (language != null) {
            Regex("```(?:$language)?\\s*\\n([\\s\\S]*?)```")
        } else {
            Regex("```(?:\\w*)?\\s*\\n([\\s\\S]*?)```")
        }

        val codeBlockMatch = codeBlockRegex.find(response)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // 如果没有代码块，返回整个响应
        return response.trim()
    }

    /**
     * 将消息列表构建为单个提示字符串
     *
     * @param messages 消息列表
     * @return 构建的提示字符串
     */
    private fun buildPromptFromMessages(messages: List<LlmMessage>): String {
        val promptBuilder = StringBuilder()

        // 添加系统消息作为指令
        val systemMessage = messages.find { it.role == LlmMessageRole.SYSTEM }
        if (systemMessage != null) {
            promptBuilder.append("指令：\n${systemMessage.content}\n\n")
        }

        // 添加用户消息作为主要提示
        val userMessage = messages.find { it.role == LlmMessageRole.USER }
        if (userMessage != null) {
            promptBuilder.append(userMessage.content)
        }

        return promptBuilder.toString()
    }
}


