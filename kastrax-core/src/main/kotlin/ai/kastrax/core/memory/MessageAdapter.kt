package ai.kastrax.core.memory

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.ToolCall

/**
 * 将 LlmMessage 转换为 Message 的扩展函数。
 */
fun LlmMessage.toMessage(): Message {
    return LlmMessageAdapter(this)
}

/**
 * 将 Message 转换为 LlmMessage 的扩展函数。
 */
fun Message.toLlmMessage(): LlmMessage {
    return LlmMessage(
        role = when (this.role) {
            MessageRole.SYSTEM -> LlmMessageRole.SYSTEM
            MessageRole.USER -> LlmMessageRole.USER
            MessageRole.ASSISTANT -> LlmMessageRole.ASSISTANT
            MessageRole.TOOL -> LlmMessageRole.TOOL
        },
        content = this.content,
        name = this.name,
        toolCalls = this.toolCalls.map {
            LlmToolCall(
                id = it.id,
                name = it.name,
                arguments = it.arguments
            )
        },
        toolCallId = this.toolCallId
    )
}

/**
 * 将 LlmMessage 转换为 Message 的适配器类。
 */
class LlmMessageAdapter(private val llmMessage: LlmMessage) : Message {
    override val role: MessageRole = when (llmMessage.role) {
        LlmMessageRole.SYSTEM -> MessageRole.SYSTEM
        LlmMessageRole.USER -> MessageRole.USER
        LlmMessageRole.ASSISTANT -> MessageRole.ASSISTANT
        LlmMessageRole.TOOL -> MessageRole.TOOL
    }

    override val content: String = llmMessage.content

    override val name: String? = llmMessage.name

    override val toolCalls: List<ToolCall> = llmMessage.toolCalls.map { LlmToolCallAdapter(it) }

    override val toolCallId: String? = llmMessage.toolCallId
}

/**
 * 将 LlmToolCall 转换为 ToolCall 的适配器类。
 */
class LlmToolCallAdapter(private val llmToolCall: LlmToolCall) : ToolCall {
    override val id: String = llmToolCall.id

    override val name: String = llmToolCall.name

    override val arguments: String = llmToolCall.arguments
}


