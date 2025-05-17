package ai.kastrax.code.agent

import ai.kastrax.code.model.DetailLevel
import ai.kastrax.core.agent.AgentStreamOptions
import kotlinx.coroutines.flow.Flow

/**
 * 代码智能体接口
 *
 * 提供代码生成、解释、重构和测试生成等核心功能
 */
interface CodeAgent {
    /**
     * 生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @return 生成的代码
     */
    suspend fun generateCode(prompt: String, language: String): String = ""

    /**
     * 流式生成代码
     *
     * @param prompt 提示文本
     * @param language 编程语言
     * @param options 流式选项
     * @return 生成的代码流
     */
    suspend fun streamGenerateCode(prompt: String, language: String, options: AgentStreamOptions = AgentStreamOptions()): Flow<String> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * 解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @return 代码解释
     */
    suspend fun explainCode(code: String, detailLevel: DetailLevel): String = ""

    /**
     * 流式解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @param options 流式选项
     * @return 代码解释流
     */
    suspend fun streamExplainCode(code: String, detailLevel: DetailLevel, options: AgentStreamOptions = AgentStreamOptions()): Flow<String> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * 重构代码
     *
     * @param code 代码文本
     * @param instructions 重构指令
     * @return 重构后的代码
     */
    suspend fun refactorCode(code: String, instructions: String): String = ""

    /**
     * 流式重构代码
     *
     * @param code 代码文本
     * @param instructions 重构指令
     * @param options 流式选项
     * @return 重构后的代码流
     */
    suspend fun streamRefactorCode(code: String, instructions: String, options: AgentStreamOptions = AgentStreamOptions()): Flow<String> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * 生成测试
     *
     * @param code 代码文本
     * @param framework 测试框架
     * @return 生成的测试代码
     */
    suspend fun generateTest(code: String, framework: String): String = ""

    /**
     * 流式生成测试
     *
     * @param code 代码文本
     * @param framework 测试框架
     * @param options 流式选项
     * @return 生成的测试代码流
     */
    suspend fun streamGenerateTest(code: String, framework: String, options: AgentStreamOptions = AgentStreamOptions()): Flow<String> = kotlinx.coroutines.flow.emptyFlow()

    /**
     * 补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @return 补全的代码
     */
    suspend fun complete(code: String, language: String, maxTokens: Int = 100): String = ""

    /**
     * 流式补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @param options 流式选项
     * @return 补全的代码流
     */
    suspend fun streamComplete(code: String, language: String, maxTokens: Int = 100, options: AgentStreamOptions = AgentStreamOptions()): Flow<String> = kotlinx.coroutines.flow.emptyFlow()
}
