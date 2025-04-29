package ai.kastrax.actor.multimodal

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 简单的多模态消息测试
 */
class SimpleMultimodalTest {

    @Test
    fun `should create text message`() {
        // 创建文本消息
        val textMessage = text("Hello, multimodal world!")

        // 验证消息
        assertNotNull(textMessage)
        assertEquals(MultimodalType.TEXT, textMessage.type)
        assertEquals("Hello, multimodal world!", textMessage.content)
    }

    @Test
    fun `should create image message`() {
        // 创建临时图像文件
        val tempFile = File.createTempFile("test-image", ".png")
        tempFile.writeBytes(ByteArray(10))
        tempFile.deleteOnExit()

        // 创建图像消息
        val imageMessage = imageFromFile(tempFile.absolutePath)

        // 验证消息
        assertNotNull(imageMessage)
        assertEquals(MultimodalType.IMAGE, imageMessage.type)
        val image = imageMessage.content as MultimodalContent.Image
        assertEquals("png", image.format)
        assertNotNull(image.file)
    }

    @Test
    fun `should create mixed message`() {
        // 创建文本内容
        val textContent = MultimodalContent.Text("Hello, mixed content!")

        // 创建音频内容
        val audioContent = MultimodalContent.Audio(
            data = ByteArray(10),
            format = "mp3"
        )

        // 创建混合消息
        val mixedMessage = mixed(textContent, audioContent)

        // 验证消息
        assertNotNull(mixedMessage)
        assertEquals(MultimodalType.MIXED, mixedMessage.type)
        val mixed = mixedMessage.content as MultimodalContent.Mixed
        assertEquals(2, mixed.contents.size)
        assertEquals("Hello, mixed content!", (mixed.contents[0] as MultimodalContent.Text).text)
        assertEquals("mp3", (mixed.contents[1] as MultimodalContent.Audio).format)
    }
}
