package ai.kastrax.actor.multimodal

import java.io.File
import java.net.URL

/**
 * 多模态消息，支持不同类型的数据
 *
 * @property content 消息内容，可以是文本、图像、音频等
 * @property type 消息类型
 * @property metadata 元数据
 */
data class MultimodalMessage(
    val content: Any,
    val type: MultimodalType,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 多模态类型
 */
enum class MultimodalType {
    TEXT,       // 文本
    IMAGE,      // 图像
    AUDIO,      // 音频
    VIDEO,      // 视频
    FILE,       // 文件
    MIXED       // 混合类型
}

/**
 * 多模态内容，表示不同类型的数据
 */
sealed class MultimodalContent {
    /**
     * 文本内容
     *
     * @property text 文本内容
     */
    data class Text(val text: String) : MultimodalContent()

    /**
     * 图像内容
     *
     * @property data 图像数据
     * @property format 图像格式
     * @property url 图像 URL
     * @property file 图像文件
     */
    data class Image(
        val data: ByteArray? = null,
        val format: String? = null,
        val url: URL? = null,
        val file: File? = null
    ) : MultimodalContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false
            if (format != other.format) return false
            if (url != other.url) return false
            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data?.contentHashCode() ?: 0
            result = 31 * result + (format?.hashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + (file?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 音频内容
     *
     * @property data 音频数据
     * @property format 音频格式
     * @property url 音频 URL
     * @property file 音频文件
     */
    data class Audio(
        val data: ByteArray? = null,
        val format: String? = null,
        val url: URL? = null,
        val file: File? = null
    ) : MultimodalContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Audio

            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false
            if (format != other.format) return false
            if (url != other.url) return false
            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data?.contentHashCode() ?: 0
            result = 31 * result + (format?.hashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + (file?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 视频内容
     *
     * @property data 视频数据
     * @property format 视频格式
     * @property url 视频 URL
     * @property file 视频文件
     */
    data class Video(
        val data: ByteArray? = null,
        val format: String? = null,
        val url: URL? = null,
        val file: File? = null
    ) : MultimodalContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Video

            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false
            if (format != other.format) return false
            if (url != other.url) return false
            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data?.contentHashCode() ?: 0
            result = 31 * result + (format?.hashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + (file?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 文件内容
     *
     * @property data 文件数据
     * @property name 文件名
     * @property type 文件类型
     * @property url 文件 URL
     * @property file 文件对象
     */
    data class FileContent(
        val data: ByteArray? = null,
        val name: String? = null,
        val type: String? = null,
        val url: URL? = null,
        val file: File? = null
    ) : MultimodalContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FileContent

            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false
            if (name != other.name) return false
            if (type != other.type) return false
            if (url != other.url) return false
            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data?.contentHashCode() ?: 0
            result = 31 * result + (name?.hashCode() ?: 0)
            result = 31 * result + (type?.hashCode() ?: 0)
            result = 31 * result + (url?.hashCode() ?: 0)
            result = 31 * result + (file?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * 混合内容，包含多种类型的数据
     *
     * @property contents 内容列表
     */
    data class Mixed(val contents: List<MultimodalContent>) : MultimodalContent()
}


