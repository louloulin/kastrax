package ai.kastrax.memory.impl

import ai.kastrax.memory.api.ToolCall

/**
 * 简单工具调用实现，用于测试和示例。
 *
 * @property id 工具调用ID
 * @property name 工具名称
 * @property arguments 工具参数
 */
data class SimpleToolCall(
    override val id: String,
    override val name: String,
    override val arguments: String = "{}"
) : ToolCall
