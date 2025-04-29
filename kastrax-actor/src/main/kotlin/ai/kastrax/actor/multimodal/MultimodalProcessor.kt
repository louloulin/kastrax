package ai.kastrax.actor.multimodal

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 多模态处理器，用于处理多模态数据
 */
object MultimodalProcessor {
    /**
     * 从文件创建图像内容
     *
     * @param filePath 文件路径
     * @return 图像内容
     */
    fun createImageFromFile(filePath: String): MultimodalContent.Image {
        val file = File(filePath)
        val format = filePath.substringAfterLast('.', "")
        return MultimodalContent.Image(
            file = file,
            format = format
        )
    }

    /**
     * 从 URL 创建图像内容
     *
     * @param url 图像 URL
     * @return 图像内容
     */
    fun createImageFromUrl(url: String): MultimodalContent.Image {
        val imageUrl = URL(url)
        val format = url.substringAfterLast('.', "").substringBefore('?')
        return MultimodalContent.Image(
            url = imageUrl,
            format = format
        )
    }

    /**
     * 从字节数组创建图像内容
     *
     * @param data 图像数据
     * @param format 图像格式
     * @return 图像内容
     */
    fun createImageFromBytes(data: ByteArray, format: String): MultimodalContent.Image {
        return MultimodalContent.Image(
            data = data,
            format = format
        )
    }

    /**
     * 从文件创建音频内容
     *
     * @param filePath 文件路径
     * @return 音频内容
     */
    fun createAudioFromFile(filePath: String): MultimodalContent.Audio {
        val file = File(filePath)
        val format = filePath.substringAfterLast('.', "")
        return MultimodalContent.Audio(
            file = file,
            format = format
        )
    }

    /**
     * 从 URL 创建音频内容
     *
     * @param url 音频 URL
     * @return 音频内容
     */
    fun createAudioFromUrl(url: String): MultimodalContent.Audio {
        val audioUrl = URL(url)
        val format = url.substringAfterLast('.', "").substringBefore('?')
        return MultimodalContent.Audio(
            url = audioUrl,
            format = format
        )
    }

    /**
     * 从字节数组创建音频内容
     *
     * @param data 音频数据
     * @param format 音频格式
     * @return 音频内容
     */
    fun createAudioFromBytes(data: ByteArray, format: String): MultimodalContent.Audio {
        return MultimodalContent.Audio(
            data = data,
            format = format
        )
    }

    /**
     * 从文件创建视频内容
     *
     * @param filePath 文件路径
     * @return 视频内容
     */
    fun createVideoFromFile(filePath: String): MultimodalContent.Video {
        val file = File(filePath)
        val format = filePath.substringAfterLast('.', "")
        return MultimodalContent.Video(
            file = file,
            format = format
        )
    }

    /**
     * 从 URL 创建视频内容
     *
     * @param url 视频 URL
     * @return 视频内容
     */
    fun createVideoFromUrl(url: String): MultimodalContent.Video {
        val videoUrl = URL(url)
        val format = url.substringAfterLast('.', "").substringBefore('?')
        return MultimodalContent.Video(
            url = videoUrl,
            format = format
        )
    }

    /**
     * 从字节数组创建视频内容
     *
     * @param data 视频数据
     * @param format 视频格式
     * @return 视频内容
     */
    fun createVideoFromBytes(data: ByteArray, format: String): MultimodalContent.Video {
        return MultimodalContent.Video(
            data = data,
            format = format
        )
    }

    /**
     * 从文件创建文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    fun createFileContent(filePath: String): MultimodalContent.FileContent {
        val file = File(filePath)
        val name = file.name
        val type = Files.probeContentType(Paths.get(filePath)) ?: ""
        return MultimodalContent.FileContent(
            file = file,
            name = name,
            type = type
        )
    }

    /**
     * 从 URL 创建文件内容
     *
     * @param url 文件 URL
     * @param name 文件名
     * @param type 文件类型
     * @return 文件内容
     */
    fun createFileContentFromUrl(url: String, name: String, type: String): MultimodalContent.FileContent {
        val fileUrl = URL(url)
        return MultimodalContent.FileContent(
            url = fileUrl,
            name = name,
            type = type
        )
    }

    /**
     * 从字节数组创建文件内容
     *
     * @param data 文件数据
     * @param name 文件名
     * @param type 文件类型
     * @return 文件内容
     */
    fun createFileContentFromBytes(data: ByteArray, name: String, type: String): MultimodalContent.FileContent {
        return MultimodalContent.FileContent(
            data = data,
            name = name,
            type = type
        )
    }

    /**
     * 创建混合内容
     *
     * @param contents 内容列表
     * @return 混合内容
     */
    fun createMixedContent(vararg contents: MultimodalContent): MultimodalContent.Mixed {
        return MultimodalContent.Mixed(contents.toList())
    }

    /**
     * 创建文本内容
     *
     * @param text 文本内容
     * @return 文本内容
     */
    fun createTextContent(text: String): MultimodalContent.Text {
        return MultimodalContent.Text(text)
    }

    /**
     * 创建多模态消息
     *
     * @param content 消息内容
     * @param type 消息类型
     * @param metadata 元数据
     * @return 多模态消息
     */
    fun createMultimodalMessage(
        content: Any,
        type: MultimodalType,
        metadata: Map<String, String> = emptyMap()
    ): MultimodalMessage {
        return MultimodalMessage(content, type, metadata)
    }
}
