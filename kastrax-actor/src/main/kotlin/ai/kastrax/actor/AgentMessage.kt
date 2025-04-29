package ai.kastrax.actor

import actor.proto.PID
import ai.kastrax.agent.AgentGenerateOptions
import ai.kastrax.agent.AgentStreamOptions
import ai.kastrax.agent.ToolCall
import kotlinx.serialization.json.JsonObject

/**
 * 基础消息接口，定义 Agent 之间通信的消息格式
 */
sealed interface AgentMessage

/**
 * Agent 请求，用于向 Agent 发送生成请求
 *
 * @property prompt 提示文本
 * @property options 生成选项
 */
data class AgentRequest(
    val prompt: String,
    val options: AgentGenerateOptions = AgentGenerateOptions()
) : AgentMessage

/**
 * Agent 响应，包含生成的文本和可能的工具调用
 *
 * @property text 生成的文本
 * @property toolCalls 工具调用列表
 */
data class AgentResponse(
    val text: String,
    val toolCalls: List<ToolCall> = emptyList()
) : AgentMessage

/**
 * 流式请求，用于向 Agent 发送流式生成请求
 *
 * @property prompt 提示文本
 * @property options 流式生成选项
 * @property sender 发送者的 PID，用于接收流式响应
 */
data class AgentStreamRequest(
    val prompt: String,
    val options: AgentStreamOptions = AgentStreamOptions(),
    val sender: PID
) : AgentMessage

/**
 * 流式响应块，包含生成的文本块
 *
 * @property chunk 生成的文本块
 */
data class AgentStreamChunk(val chunk: String) : AgentMessage

/**
 * 流式响应完成，表示流式生成已完成
 */
object AgentStreamComplete : AgentMessage

/**
 * 取消请求，用于取消正在进行的流式生成
 */
object CancelRequest : AgentMessage

/**
 * 取消响应，表示取消操作的结果
 *
 * @property success 是否成功取消
 */
data class CancelResponse(val success: Boolean) : AgentMessage

/**
 * 工具调用请求，用于调用 Agent 的工具
 *
 * @property toolName 工具名称
 * @property input 工具输入参数
 */
data class ToolCallRequest(
    val toolName: String,
    val input: JsonObject
) : AgentMessage

/**
 * 工具调用响应，包含工具调用的结果
 *
 * @property result 工具调用结果
 */
data class ToolCallResponse(val result: JsonObject) : AgentMessage

/**
 * 协作请求，用于 Agent 之间的协作
 *
 * @property task 任务描述
 * @property sender 发送者标识
 * @property metadata 元数据
 */
data class CollaborationRequest(
    val task: String,
    val sender: String,
    val metadata: Map<String, String> = emptyMap()
) : AgentMessage

/**
 * 协作响应，包含协作的结果
 *
 * @property result 协作结果
 */
data class CollaborationResponse(val result: String) : AgentMessage
