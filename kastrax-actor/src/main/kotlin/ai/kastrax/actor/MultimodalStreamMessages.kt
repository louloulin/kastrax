package ai.kastrax.actor

import actor.proto.PID
import ai.kastrax.actor.multimodal.MultimodalMessage
import ai.kastrax.core.agent.AgentStreamOptions

/**
 * 多模态流式请求，用于向 Agent 发送多模态流式生成请求
 *
 * @property message 多模态消息
 * @property options 流式生成选项
 * @property sender 发送者的 PID，用于接收流式响应
 */
data class MultimodalStreamRequest(
    val message: MultimodalMessage,
    val options: AgentStreamOptions = AgentStreamOptions(),
    val sender: PID
) : AgentMessage

/**
 * 多模态流式响应块，包含生成的多模态消息块
 *
 * @property chunk 多模态消息块
 */
data class MultimodalStreamChunk(val chunk: MultimodalMessage) : AgentMessage

/**
 * 多模态流式完成，表示多模态流式生成已完成
 */
object MultimodalStreamComplete : AgentMessage
