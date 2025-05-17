package ai.kastrax.code.agent

import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基于 kastrax-core 的代码智能体实现
 */
class KastraxCodeAgentImpl(
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
    override suspend fun generateCode(prompt: String, language: String): String = withContext(Dispatchers.IO) {
        logger.debug("生成代码: $prompt, 语言: $language")

        val enhancedPrompt = """
            请根据以下描述生成 $language 代码：

            $prompt

            请只返回代码，不要包含解释或其他文本。
        """.trimIndent()

        // 获取上下文
        val context = contextEngine.getQueryContext(prompt, 5, 0.0, true)

        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码生成助手，擅长编写高质量的代码。请根据用户的描述生成代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "$enhancedPrompt\n\n上下文信息：\n${context.toString()}"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = config.codeGenerationTemperature,
            maxTokens = config.codeGenerationMaxTokens
        )

        val response = agent.generate(messages, options)
        return@withContext extractCodeFromResponse(response.text, language)
    }

    /**
     * 解释代码
     *
     * @param code 代码
     * @param detailLevel 详细程度
     * @return 解释
     */
    override suspend fun explainCode(code: String, detailLevel: DetailLevel): String = withContext(Dispatchers.IO) {
        logger.debug("解释代码, 详细程度: $detailLevel")

        val detailPrompt = when (detailLevel) {
            DetailLevel.BRIEF -> "请简要解释这段代码的功能，不需要详细分析每一行。"
            DetailLevel.NORMAL -> "请解释这段代码的功能和主要实现方式。"
            DetailLevel.DETAILED -> "请详细解释这段代码的功能、实现方式和每个关键部分的作用。"
        }

        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码解释助手，擅长解释代码的功能和实现。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "$detailPrompt\n\n```\n$code\n```"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = config.codeExplanationTemperature,
            maxTokens = config.codeExplanationMaxTokens
        )

        val response = agent.generate(messages, options)
        return@withContext response.text
    }

    /**
     * 重构代码
     *
     * @param code 代码
     * @param instructions 重构指令
     * @return 重构后的代码
     */
    override suspend fun refactorCode(code: String, instructions: String): String = withContext(Dispatchers.IO) {
        logger.debug("重构代码: $instructions")

        // 获取上下文
        val context = contextEngine.getQueryContext(instructions, 5, 0.0, true)

        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码重构助手，擅长根据指令优化和重构代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "请根据以下指令重构代码：\n\n$instructions\n\n代码：\n```\n$code\n```\n\n上下文信息：\n${context.toString()}"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = config.codeRefactoringTemperature,
            maxTokens = config.codeRefactoringMaxTokens
        )

        val response = agent.generate(messages, options)
        return@withContext extractCodeFromResponse(response.text)
    }

    /**
     * 生成测试
     *
     * @param code 代码
     * @param framework 测试框架
     * @return 生成的测试代码
     */
    override suspend fun generateTest(code: String, framework: String): String = withContext(Dispatchers.IO) {
        logger.debug("生成测试: $framework")

        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的测试生成助手，擅长为代码编写高质量的测试。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "请为以下代码生成使用 $framework 框架的测试：\n\n```\n$code\n```"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = config.testGenerationTemperature,
            maxTokens = config.testGenerationMaxTokens
        )

        val response = agent.generate(messages, options)
        return@withContext extractCodeFromResponse(response.text)
    }

    /**
     * 补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @return 补全的代码
     */
    override suspend fun complete(code: String, language: String, maxTokens: Int): String = withContext(Dispatchers.IO) {
        logger.debug("补全代码, 语言: $language, 最大令牌数: $maxTokens")

        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个专业的代码补全助手，擅长根据上下文补全代码。请只返回补全的部分，不要重复已有代码。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "请补全以下 $language 代码：\n\n```\n$code\n```"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = config.codeCompletionTemperature,
            maxTokens = maxTokens
        )

        val response = agent.generate(messages, options)
        return@withContext extractCodeFromResponse(response.text)
    }

    /**
     * 从响应中提取代码
     *
     * @param response 响应文本
     * @param language 编程语言（可选）
     * @return 提取的代码
     */
    private fun extractCodeFromResponse(response: String, language: String? = null): String {
        // 尝试提取代码块
        val codeBlockRegex = if (language != null) {
            "```(?:$language)?\\s*([\\s\\S]*?)```".toRegex()
        } else {
            "```(?:\\w*)?\\s*([\\s\\S]*?)```".toRegex()
        }

        val codeBlockMatch = codeBlockRegex.find(response)
        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        // 如果没有找到代码块，返回整个响应
        return response.trim()
    }
}
