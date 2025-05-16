package ai.kastrax.code.mock

import ai.kastrax.code.model.DetailLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// 导入MockDeepSeekProvider中的类
import ai.kastrax.code.mock.LlmToolCall
import ai.kastrax.code.mock.LlmUsage

/**
 * 模拟Agent类
 *
 * 用于解决编译错误
 */
class Agent(
    val id: String,
    val config: AgentConfig
) {
    /**
     * 生成文本
     *
     * @param prompt 提示文本
     * @param options 生成选项
     * @return 生成的响应
     */
    suspend fun generate(prompt: String, options: AgentGenerateOptions = AgentGenerateOptions()): AgentResponse = withContext(Dispatchers.IO) {
        return@withContext AgentResponse(
            text = "模拟响应: $prompt",
            usage = ai.kastrax.code.mock.LlmUsage(
                promptTokens = prompt.length,
                completionTokens = 100,
                totalTokens = prompt.length + 100
            )
        )
    }
}

/**
 * 模拟Agent配置类
 */
data class AgentConfig(
    val name: String,
    val description: String,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000
)

/**
 * 模拟Agent生成选项
 */
data class AgentGenerateOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000
)

/**
 * 模拟Agent响应
 */
data class AgentResponse(
    val text: String,
    val toolCalls: List<ai.kastrax.code.mock.LlmToolCall> = emptyList(),
    val toolResults: Map<String, ToolCallResult> = emptyMap(),
    val usage: ai.kastrax.code.mock.LlmUsage? = null
)

/**
 * 模拟工具调用结果
 */
data class ToolCallResult(
    val output: String
)

/**
 * 模拟Agent上下文
 */
class AgentContext {
    /**
     * 获取内容
     */
    fun getContent(): String {
        return "模拟上下文内容"
    }

    /**
     * 处理请求
     */
    suspend fun process(request: String): String {
        return "处理结果: $request"
    }
}
