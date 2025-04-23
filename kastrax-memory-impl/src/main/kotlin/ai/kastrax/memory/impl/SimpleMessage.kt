package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.ToolCall

/**
 * 简单消息实现，用于测试和示例。
 *
 * @property role 消息角色
 * @property content 消息内容
 * @property name 消息名称（可选）
 * @property toolCalls 工具调用列表
 * @property toolCallId 工具调用ID
 */
data class SimpleMessage(
    override val role: MessageRole,
    override val content: String,
    override val name: String? = null,
    override val toolCalls: List<ToolCall> = emptyList(),
    override val toolCallId: String? = null
) : Message
