package ai.kastrax.actor.multimodal

import actor.proto.ActorSystem
import actor.proto.fromProducer
import ai.kastrax.actor.KastraxActor
import ai.kastrax.actor.MultimodalRequest
import ai.kastrax.actor.MultimodalResponse
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File

class MultimodalTest {

    private lateinit var system: ActorSystem
    private lateinit var tempImageFile: File

    @BeforeEach
    fun setup() {
        system = ActorSystem("test-system")
        tempImageFile = File.createTempFile("test", ".jpg")
        tempImageFile.deleteOnExit()
    }

    @AfterEach
    fun teardown() {
        system.shutdown()
    }

    @Test
    fun `test create multimodal message`() {
        // 创建文本消息
        val textMessage = text("这是一段文本")
        assertEquals("这是一段文本", textMessage.content)
        assertEquals(MultimodalType.TEXT, textMessage.type)

        // 创建图像消息
        val imageMessage = imageFromFile(tempImageFile.absolutePath)
        assertEquals(MultimodalType.IMAGE, imageMessage.type)
        assertTrue(imageMessage.content is MultimodalContent.Image)

        // 创建混合消息
        val textContent = MultimodalProcessor.createTextContent("这是文本部分")
        val imageContent = MultimodalProcessor.createImageFromFile(tempImageFile.absolutePath)
        val mixedMessage = mixed(textContent, imageContent)
        assertEquals(MultimodalType.MIXED, mixedMessage.type)
        assertTrue(mixedMessage.content is MultimodalContent.Mixed)
    }

    @Test
    fun `test multimodal processor`() {
        // 测试创建图像内容
        val image = MultimodalProcessor.createImageFromFile(tempImageFile.absolutePath)
        assertNotNull(image.file)
        assertEquals(tempImageFile.absolutePath, image.file?.absolutePath)

        // 测试创建文本内容
        val text = MultimodalProcessor.createTextContent("这是一段文本")
        assertEquals("这是一段文本", text.text)

        // 测试创建混合内容
        val mixed = MultimodalProcessor.createMixedContent(text, image)
        assertEquals(2, mixed.contents.size)
        assertTrue(mixed.contents[0] is MultimodalContent.Text)
        assertTrue(mixed.contents[1] is MultimodalContent.Image)
    }

    @Test
    @Disabled("需要实际的 Agent 实现")
    fun `test send multimodal message`() = runBlocking {
        // 创建模拟 Agent
        val mockAgent = TestAgent("test-agent")

        // 创建 Actor
        val props = fromProducer { KastraxActor(mockAgent) }
        val agentPid = system.root.spawn(props)

        // 发送文本消息
        system.sendTextMessage(agentPid, "你好，我是用户")

        // 请求-响应模式，发送文本请求并等待响应
        val response = system.askTextMessage(agentPid, "测试请求")
        assertEquals(MultimodalType.TEXT, response.message.type)
        assertEquals("[test-agent] 回复: 测试请求", response.message.content)
    }

    /**
     * 测试用 Agent 实现
     */
    private class TestAgent(override val name: String) : Agent {
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
            return AgentResponse(
                text = response.text,
                toolCalls = emptyList(),
                textStream = flowOf(response.text)
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
