package ai.kastrax.actor

import ai.kastrax.actor.multimodal.MultimodalMessage
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.llm.LlmToolCall

/**
 * 多模态请求，用于向 Agent 发送多模态数据
 *
 * @property message 多模态消息
 * @property options 生成选项
 */
data class MultimodalRequest(
    val message: MultimodalMessage,
    val options: AgentGenerateOptions = AgentGenerateOptions()
) : AgentMessage

/**
 * 多模态响应，包含 Agent 生成的多模态数据
 *
 * @property message 多模态消息
 * @property toolCalls 工具调用列表
 */
data class MultimodalResponse(
    val message: MultimodalMessage,
    val toolCalls: List<LlmToolCall> = emptyList()
) : AgentMessage
