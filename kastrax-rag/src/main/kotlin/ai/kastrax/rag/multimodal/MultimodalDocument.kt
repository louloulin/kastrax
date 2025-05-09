package ai.kastrax.rag.multimodal

import ai.kastrax.store.document.Document

/**
 * 多模态文档类型枚举。
 */
enum class MultimodalDocumentType {
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    PDF,
    MIXED
}

/**
 * 多模态文档，支持文本、图像、音频和视频等多种模态。
 *
 * @property id 文档 ID
 * @property content 文本内容
 * @property mediaUrls 媒体 URL 列表
 * @property mediaType 媒体类型
 * @property metadata 元数据
 */
data class MultimodalDocument(
    val id: String,
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val mediaType: MultimodalDocumentType = MultimodalDocumentType.TEXT,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 转换为普通文档。
     *
     * @return 普通文档
     */
    fun toDocument(): Document {
        val updatedMetadata = metadata.toMutableMap()
        updatedMetadata["mediaUrls"] = mediaUrls
        updatedMetadata["mediaType"] = mediaType.name
        
        return Document(
            id = id,
            content = content,
            metadata = updatedMetadata
        )
    }
    
    companion object {
        /**
         * 从普通文档创建多模态文档。
         *
         * @param document 普通文档
         * @return 多模态文档
         */
        fun fromDocument(document: Document): MultimodalDocument {
            val mediaUrls = document.metadata["mediaUrls"]?.let {
                when (it) {
                    is List<*> -> it.filterIsInstance<String>()
                    is String -> listOf(it)
                    else -> emptyList()
                }
            } ?: emptyList()
            
            val mediaType = document.metadata["mediaType"]?.let {
                when (it) {
                    is String -> try {
                        MultimodalDocumentType.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        MultimodalDocumentType.TEXT
                    }
                    else -> MultimodalDocumentType.TEXT
                }
            } ?: MultimodalDocumentType.TEXT
            
            val filteredMetadata = document.metadata.filterKeys { it != "mediaUrls" && it != "mediaType" }
            
            return MultimodalDocument(
                id = document.id,
                content = document.content,
                mediaUrls = mediaUrls,
                mediaType = mediaType,
                metadata = filteredMetadata
            )
        }
    }
}
