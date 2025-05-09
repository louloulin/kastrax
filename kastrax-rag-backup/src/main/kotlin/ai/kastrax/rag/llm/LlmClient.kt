package ai.kastrax.rag.llm

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider

/**
 * LLM 客户端接口，用于 RAG 评估工具。
 */
interface LlmClient {
    /**
     * 生成文本。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @param options 生成选项
     * @return 生成的文本
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        options: Map<String, Any> = emptyMap()
    ): String
}

/**
 * 基于 LlmProvider 的 LlmClient 实现。
 *
 * @property provider LLM 提供商
 */
class LlmProviderClient(
    private val provider: LlmProvider
) : LlmClient {
    /**
     * 生成文本。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @param options 生成选项
     * @return 生成的文本
     */
    override suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        options: Map<String, Any>
    ): String {
        val messages = listOf(
            LlmMessage(role = LlmMessageRole.SYSTEM, content = systemPrompt),
            LlmMessage(role = LlmMessageRole.USER, content = userPrompt)
        )

        val llmOptions = LlmOptions(
            temperature = options["temperature"] as? Double ?: 0.0,
            maxTokens = options["maxTokens"] as? Int,
            topP = options["topP"] as? Double
        )

        val response = provider.generate(messages, llmOptions)
        return response.content
    }
}
