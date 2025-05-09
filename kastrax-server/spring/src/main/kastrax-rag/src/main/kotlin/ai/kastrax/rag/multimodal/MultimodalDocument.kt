package ai.kastrax.rag.multimodal

import ai.kastrax.store.document.Document

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
            val mediaUrls = document.metadata["mediaUrls"] as? List<String> ?: emptyList()
            val mediaTypeName = document.metadata["mediaType"] as? String ?: MultimodalDocumentType.TEXT.name
            val mediaType = try {
                MultimodalDocumentType.valueOf(mediaTypeName)
            } catch (e: IllegalArgumentException) {
                MultimodalDocumentType.TEXT
            }
            
            val filteredMetadata = document.metadata.filterKeys { it != "mediaUrls" && it != "mediaType" }
            
            return MultimodalDocument(
                id = document.id,
                content = document.content,
                mediaUrls = mediaUrls,
                mediaType = mediaType,
                metadata = filteredMetadata
            )
        }
        
        /**
         * 创建文本文档。
         *
         * @param id 文档 ID
         * @param content 文本内容
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun text(
            id: String,
            content: String,
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = content,
                mediaUrls = emptyList(),
                mediaType = MultimodalDocumentType.TEXT,
                metadata = metadata
            )
        }
        
        /**
         * 创建图像文档。
         *
         * @param id 文档 ID
         * @param imageUrl 图像 URL
         * @param caption 图像描述
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun image(
            id: String,
            imageUrl: String,
            caption: String = "",
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = caption,
                mediaUrls = listOf(imageUrl),
                mediaType = MultimodalDocumentType.IMAGE,
                metadata = metadata
            )
        }
        
        /**
         * 创建音频文档。
         *
         * @param id 文档 ID
         * @param audioUrl 音频 URL
         * @param transcript 音频转录
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun audio(
            id: String,
            audioUrl: String,
            transcript: String = "",
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = transcript,
                mediaUrls = listOf(audioUrl),
                mediaType = MultimodalDocumentType.AUDIO,
                metadata = metadata
            )
        }
        
        /**
         * 创建视频文档。
         *
         * @param id 文档 ID
         * @param videoUrl 视频 URL
         * @param description 视频描述
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun video(
            id: String,
            videoUrl: String,
            description: String = "",
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = description,
                mediaUrls = listOf(videoUrl),
                mediaType = MultimodalDocumentType.VIDEO,
                metadata = metadata
            )
        }
        
        /**
         * 创建文本和图像混合文档。
         *
         * @param id 文档 ID
         * @param content 文本内容
         * @param imageUrl 图像 URL
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun textAndImage(
            id: String,
            content: String,
            imageUrl: String,
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = content,
                mediaUrls = listOf(imageUrl),
                mediaType = MultimodalDocumentType.TEXT_IMAGE,
                metadata = metadata
            )
        }
        
        /**
         * 创建文本和音频混合文档。
         *
         * @param id 文档 ID
         * @param content 文本内容
         * @param audioUrl 音频 URL
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun textAndAudio(
            id: String,
            content: String,
            audioUrl: String,
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = content,
                mediaUrls = listOf(audioUrl),
                mediaType = MultimodalDocumentType.TEXT_AUDIO,
                metadata = metadata
            )
        }
        
        /**
         * 创建文本和视频混合文档。
         *
         * @param id 文档 ID
         * @param content 文本内容
         * @param videoUrl 视频 URL
         * @param metadata 元数据
         * @return 多模态文档
         */
        fun textAndVideo(
            id: String,
            content: String,
            videoUrl: String,
            metadata: Map<String, Any> = emptyMap()
        ): MultimodalDocument {
            return MultimodalDocument(
                id = id,
                content = content,
                mediaUrls = listOf(videoUrl),
                mediaType = MultimodalDocumentType.TEXT_VIDEO,
                metadata = metadata
            )
        }
    }
}
