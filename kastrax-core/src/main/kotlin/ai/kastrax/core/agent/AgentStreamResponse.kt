package ai.kastrax.core.agent

/**
 * Agent流式响应
 */
data class AgentStreamResponse(
    val text: String,
    val metadata: Map<String, String>? = null
)
