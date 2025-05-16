package ai.kastrax.code.agent

import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.common.KastraXBase
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 基于 kastrax-core 的代码智能体实现
 */
class KastraxCodeAgent(
    private val agent: Agent,
    private val contextEngine: CodeContextEngine,
    private val toolRegistry: CodeToolRegistry,
    private val config: CodeAgentConfig = CodeAgentConfig()
) : KastraXBase(component = "CODE_AGENT", name = agent.name), CodeAgent {

    override val logger = KotlinLogging.logger {}

    /**
     * 生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @return 生成的代码
     */
    override suspend fun generateCode(prompt: String, language: String): String {
        logger.debug { "生成代码: $prompt, 语言: $language" }

        val enhancedPrompt = """
            请根据以下描述生成 $language 代码：

            $prompt

            请只返回代码，不要包含解释或其他文本。
        """.trimIndent()

        val options = AgentGenerateOptions(
            temperature = config.codeGenerationTemperature,
            maxTokens = config.codeGenerationMaxTokens
        )

        val response = agent.generate(enhancedPrompt, options)
        return extractCodeFromResponse(response.text, language)
    }

    /**
     * 解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @return 代码解释
     */
    override suspend fun explainCode(code: String, detailLevel: DetailLevel): String {
        logger.debug { "解释代码, 详细程度: $detailLevel" }

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

        val response = agent.generate(enhancedPrompt, options)
        return response.text
    }

    /**
     * 重构代码
     *
     * @param code 代码文本
     * @param instructions 重构指令
     * @return 重构后的代码
     */
    override suspend fun refactorCode(code: String, instructions: String): String {
        logger.debug { "重构代码, 指令: $instructions" }

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

        val response = agent.generate(enhancedPrompt, options)
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
        logger.debug { "生成测试, 框架: $framework" }

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

        val response = agent.generate(enhancedPrompt, options)
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
        logger.debug { "补全代码, 语言: $language, 最大令牌数: $maxTokens" }

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

        val response = agent.generate(enhancedPrompt, options)
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
}

/**
 * 代码智能体配置
 */
data class CodeAgentConfig(
    // 代码生成配置
    val codeGenerationTemperature: Double = 0.3,
    val codeGenerationMaxTokens: Int = 2000,

    // 代码解释配置
    val codeExplanationTemperature: Double = 0.7,
    val codeExplanationMaxTokens: Int = 2000,

    // 代码重构配置
    val codeRefactoringTemperature: Double = 0.3,
    val codeRefactoringMaxTokens: Int = 2000,

    // 测试生成配置
    val testGenerationTemperature: Double = 0.3,
    val testGenerationMaxTokens: Int = 2000,

    // 代码补全配置
    val codeCompletionTemperature: Double = 0.2,
    val codeCompletionMaxTokens: Int = 500
)
