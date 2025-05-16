package ai.kastrax.code.memory

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.ToolCall
import java.time.Instant

/**
 * 简单记忆实现
 *
 * 用于代码记忆系统
 */
class SimpleMemory(
    val content: String,
    val metadata: Map<String, Any> = emptyMap(),
    val timestamp: Instant = Instant.now()
) {
    /**
     * 转换为消息
     *
     * @param role 角色
     * @return 消息
     */
    fun toMessage(role: String): SimpleMessage {
        return SimpleMessage(
            role = when (role.lowercase()) {
                "user" -> MessageRole.USER
                "assistant" -> MessageRole.ASSISTANT
                "system" -> MessageRole.SYSTEM
                "tool" -> MessageRole.TOOL
                else -> MessageRole.USER
            },
            content = content
        )
    }
}

/**
 * 简单消息实现
 *
 * @property role 角色
 * @property content 内容
 * @property name 名称
 * @property toolCalls 工具调用
 * @property toolCallId 工具调用ID
 */
class SimpleMessage(
    override val role: MessageRole,
    override val content: String,
    override val name: String? = null,
    override val toolCalls: List<ToolCall> = emptyList(),
    override val toolCallId: String? = null
) : Message
