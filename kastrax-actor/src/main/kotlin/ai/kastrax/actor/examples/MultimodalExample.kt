package ai.kastrax.actor.examples

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.MultimodalRequest
import ai.kastrax.actor.MultimodalResponse
import ai.kastrax.actor.multimodal.*
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 多模态数据传输示例
 *
 * 本示例展示了如何使用多模态数据传输功能，包括：
 * 1. 创建多模态消息
 * 2. 发送多模态消息
 * 3. 处理多模态响应
 */
object MultimodalExample {

    /**
     * 主函数
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建 Actor 系统
        val system = ActorSystem("multimodal-example")

        try {
            // 创建模拟 Agent
            val mockAgent = MultimodalMockAgent()

            // 创建 Actor
            val props = fromProducer { KastraxActor(mockAgent) }
            val agentPid = system.root.spawn(props)

            // 1. 发送文本消息
            println("=== 发送文本消息 ===")
            system.sendTextMessage(agentPid, "你好，我是用户")

            // 2. 请求-响应模式，发送文本请求并等待响应
            println("=== 请求-响应模式 ===")
            val response = system.askTextMessage(agentPid, "巴黎的人口是多少？")
            println("回答: ${(response.message.content as String)}")

            // 3. 发送图像消息
            println("=== 发送图像消息 ===")
            // 创建临时图像文件
            val tempImageFile = File.createTempFile("example", ".jpg")
            tempImageFile.deleteOnExit()

            // 从文件创建图像内容
            val image = MultimodalProcessor.createImageFromFile(tempImageFile.absolutePath)
            system.sendImageMessage(agentPid, image)

            // 4. 使用 DSL 创建和发送多模态消息
            println("=== 使用 DSL 创建和发送多模态消息 ===")

            // 创建文本消息
            val textMessage = text("这是一段文本")
            println("文本消息: ${textMessage.content}")

            // 创建图像消息
            val imageMessage = imageFromFile(tempImageFile.absolutePath)
            println("图像消息类型: ${imageMessage.type}")

            // 创建混合消息
            val textContent = MultimodalProcessor.createTextContent("这是文本部分")
            val imageContent = MultimodalProcessor.createImageFromFile(tempImageFile.absolutePath)
            val mixedMessage = mixed(textContent, imageContent)
            println("混合消息类型: ${mixedMessage.type}")

            // 发送混合消息
            val mixedResponse = system.askMultimodalMessage(
                agentPid,
                mixedMessage.content,
                mixedMessage.type
            )
            println("混合消息响应: ${(mixedResponse.message.content as String)}")
        } finally {
            // 关闭系统
            system.shutdown()
        }
    }

    /**
     * 多模态模拟 Agent 实现
     */
    private class MultimodalMockAgent : Agent {
        override val name: String = "多模态模拟助手"
        override val versionManager: AgentVersionManager? = null

        override suspend fun generate(messages: List<LlmMessage>, options: AgentGenerateOptions): AgentResponse {
            val lastMessage = messages.lastOrNull { it.role.name.equals("USER", ignoreCase = true) }?.content ?: ""
            return generate(lastMessage, options)
        }

        override suspend fun generate(prompt: String, options: AgentGenerateOptions): AgentResponse {
            // 模拟响应
            val sender = options.metadata?.get("sender") ?: "unknown"

            // 如果发送者是自己，返回简单响应以避免递归
            if (sender == name) {
                return AgentResponse(
                    text = "[自我处理] $prompt",
                    toolCalls = emptyList()
                )
            }

            return AgentResponse(
                text = "[$name] 回复: $prompt",
                toolCalls = emptyList()
            )
        }

        override suspend fun stream(prompt: String, options: AgentStreamOptions): AgentResponse {
            // 创建一个安全的选项对象，避免递归
            val safeOptions = AgentGenerateOptions(metadata = null)
            val response = generate(prompt, safeOptions)

            // 模拟流式响应
            val chunks = response.text.chunked(10)

            return AgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(*chunks.toTypedArray())
            )
        }

        override suspend fun reset() {
            // 不做任何操作
        }

        override suspend fun getState(): AgentState? {
            return null
        }

        override suspend fun updateState(status: AgentStatus): AgentState? {
            return null
        }

        override suspend fun createSession(title: String?, resourceId: String?, metadata: Map<String, String>): SessionInfo? {
            return null
        }

        override suspend fun getSession(sessionId: String): SessionInfo? {
            return null
        }

        override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
            return emptyList()
        }

        override suspend fun createVersion(
            instructions: String,
            name: String?,
            description: String?,
            metadata: Map<String, String>,
            activateImmediately: Boolean
        ): AgentVersion? {
            return null
        }

        override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
            return emptyList()
        }

        override suspend fun getActiveVersion(): AgentVersion? {
            return null
        }

        override suspend fun activateVersion(versionId: String): AgentVersion? {
            return null
        }

        override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
            return null
        }
    }
}
