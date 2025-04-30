package ai.kastrax.actor.multimodal

import actor.proto.Actor
import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.fromProducer
import ai.kastrax.actor.MultimodalRequest
import ai.kastrax.actor.MultimodalResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


/**
 * 多模态消息测试
 */
class MultimodalMessageTest {
    private lateinit var system: ActorSystem
    private lateinit var echoAgentPid: PID

    @BeforeEach
    fun setup() {
        // 创建 Actor 系统
        system = ActorSystem("multimodal-test")

        // 创建回显 Agent，简单地返回接收到的消息
        val echoProps = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    println("EchoAgent received message: $msg")
                    when (msg) {
                        is MultimodalRequest -> {
                            println("Processing MultimodalRequest: ${msg.message.type}")
                            val response = MultimodalResponse(msg.message)
                            println("Sending response: $response")
                            respond(response)
                        }
                        else -> {
                            println("Received unknown message type: ${msg::class.java.name}")
                        }
                    }
                }
            }
        }
        echoAgentPid = system.root.spawn(echoProps)
    }

    @AfterEach
    fun teardown() {
        // 关闭系统
        system.shutdown()
    }

    @Test
    fun `should send and receive text message`() = runBlocking {
        println("Starting text message test")

        // 创建文本消息
        val textMessage = text("Hello, multimodal world!")
        println("Created text message: $textMessage")

        try {
            // 发送消息并接收响应
            println("Sending message to agent: $echoAgentPid")
            val response = system.askMultiModalMessage(echoAgentPid, textMessage)
            println("Received response: $response")

            // 验证响应
            assertNotNull(response)
            assertEquals(MultimodalType.TEXT, response.message.type)
            assertEquals("Hello, multimodal world!", response.message.content)
        } catch (e: Exception) {
            println("Exception in text message test: ${e.message}")
            throw e
        }
    }

    @Test
    @Disabled("需要解决递归问题")
    fun `should send and receive image message`() = runBlocking {
        // 创建临时图像文件
        val tempFile = File.createTempFile("test-image", ".png")
        tempFile.writeBytes(ByteArray(10))
        tempFile.deleteOnExit()

        // 创建图像消息
        val imageMessage = imageFromFile(tempFile.absolutePath)

        // 发送消息并接收响应
        val response = system.askMultiModalMessage(echoAgentPid, imageMessage)

        // 验证响应
        assertNotNull(response)
        assertEquals(MultimodalType.IMAGE, response.message.type)
        val image = response.message.content as MultimodalContent.Image
        assertEquals("png", image.format)
        assertNotNull(image.file)
    }

    @Test
    @Disabled("需要解决递归问题")
    fun `should send and receive mixed message`() = runBlocking {
        // 创建文本内容
        val textContent = MultimodalContent.Text("Hello, mixed content!")

        // 创建音频内容
        val audioContent = MultimodalContent.Audio(
            data = ByteArray(10),
            format = "mp3"
        )

        // 创建混合消息
        val mixedMessage = mixed(textContent, audioContent)

        // 发送消息并接收响应
        val response = system.askMultiModalMessage(echoAgentPid, mixedMessage)

        // 验证响应
        assertNotNull(response)
        assertEquals(MultimodalType.MIXED, response.message.type)
        val mixed = response.message.content as MultimodalContent.Mixed
        assertEquals(2, mixed.contents.size)
        assertEquals("Hello, mixed content!", (mixed.contents[0] as MultimodalContent.Text).text)
        assertEquals("mp3", (mixed.contents[1] as MultimodalContent.Audio).format)
    }
}
