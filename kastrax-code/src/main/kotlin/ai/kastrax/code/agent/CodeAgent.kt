package ai.kastrax.code.agent

import ai.kastrax.code.model.DetailLevel

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
     * 解释代码
     *
     * @param code 代码文本
     * @param detailLevel 详细程度
     * @return 代码解释
     */
    suspend fun explainCode(code: String, detailLevel: DetailLevel): String = ""

    /**
     * 重构代码
     *
     * @param code 代码文本
     * @param instructions 重构指令
     * @return 重构后的代码
     */
    suspend fun refactorCode(code: String, instructions: String): String = ""

    /**
     * 生成测试
     *
     * @param code 代码文本
     * @param framework 测试框架
     * @return 生成的测试代码
     */
    suspend fun generateTest(code: String, framework: String): String = ""

    /**
     * 补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @return 补全的代码
     */
    suspend fun complete(code: String, language: String, maxTokens: Int = 100): String = ""
}
