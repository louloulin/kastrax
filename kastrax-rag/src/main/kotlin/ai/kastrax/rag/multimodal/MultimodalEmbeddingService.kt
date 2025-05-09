package ai.kastrax.rag.multimodal

import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 多模态嵌入服务接口，支持文本、图像、音频和视频等多种模态的嵌入。
 */
interface MultimodalEmbeddingService : EmbeddingService {
    /**
     * 生成图像的嵌入向量。
     *
     * @param imageUrl 图像 URL
     * @return 嵌入向量
     */
    suspend fun embedImage(imageUrl: String): FloatArray
    
    /**
     * 生成音频的嵌入向量。
     *
     * @param audioUrl 音频 URL
     * @return 嵌入向量
     */
    suspend fun embedAudio(audioUrl: String): FloatArray
    
    /**
     * 生成视频的嵌入向量。
     *
     * @param videoUrl 视频 URL
     * @return 嵌入向量
     */
    suspend fun embedVideo(videoUrl: String): FloatArray
    
    /**
     * 生成多模态文档的嵌入向量。
     *
     * @param document 多模态文档
     * @return 嵌入向量
     */
    suspend fun embedMultimodalDocument(document: MultimodalDocument): FloatArray {
        return when (document.mediaType) {
            MultimodalDocumentType.TEXT -> embed(document.content)
            MultimodalDocumentType.IMAGE -> {
                if (document.mediaUrls.isEmpty()) {
                    logger.warn { "No image URL found in document ${document.id}" }
                    embed(document.content)
                } else {
                    val textEmbedding = embed(document.content)
                    val imageEmbedding = embedImage(document.mediaUrls.first())
                    combineEmbeddings(textEmbedding, imageEmbedding)
                }
            }
            MultimodalDocumentType.AUDIO -> {
                if (document.mediaUrls.isEmpty()) {
                    logger.warn { "No audio URL found in document ${document.id}" }
                    embed(document.content)
                } else {
                    val textEmbedding = embed(document.content)
                    val audioEmbedding = embedAudio(document.mediaUrls.first())
                    combineEmbeddings(textEmbedding, audioEmbedding)
                }
            }
            MultimodalDocumentType.VIDEO -> {
                if (document.mediaUrls.isEmpty()) {
                    logger.warn { "No video URL found in document ${document.id}" }
                    embed(document.content)
                } else {
                    val textEmbedding = embed(document.content)
                    val videoEmbedding = embedVideo(document.mediaUrls.first())
                    combineEmbeddings(textEmbedding, videoEmbedding)
                }
            }
            MultimodalDocumentType.PDF -> {
                // PDF 处理为文本
                embed(document.content)
            }
            MultimodalDocumentType.MIXED -> {
                // 混合模态处理
                val textEmbedding = embed(document.content)
                val mediaEmbeddings = document.mediaUrls.map { url ->
                    when {
                        isImageUrl(url) -> embedImage(url)
                        isAudioUrl(url) -> embedAudio(url)
                        isVideoUrl(url) -> embedVideo(url)
                        else -> FloatArray(0)
                    }
                }.filter { it.isNotEmpty() }
                
                if (mediaEmbeddings.isEmpty()) {
                    textEmbedding
                } else {
                    val combinedMediaEmbedding = combineMultipleEmbeddings(mediaEmbeddings)
                    combineEmbeddings(textEmbedding, combinedMediaEmbedding)
                }
            }
        }
    }
    
    /**
     * 生成多个多模态文档的嵌入向量。
     *
     * @param documents 多模态文档列表
     * @return 嵌入向量列表
     */
    suspend fun embedMultimodalDocuments(documents: List<MultimodalDocument>): List<FloatArray> {
        return documents.map { embedMultimodalDocument(it) }
    }
    
    /**
     * 组合两个嵌入向量。
     *
     * @param embedding1 嵌入向量 1
     * @param embedding2 嵌入向量 2
     * @return 组合后的嵌入向量
     */
    fun combineEmbeddings(embedding1: FloatArray, embedding2: FloatArray): FloatArray {
        // 如果维度不同，需要进行调整
        val dim1 = embedding1.size
        val dim2 = embedding2.size
        
        if (dim1 == dim2) {
            // 简单平均
            val result = FloatArray(dim1) { i ->
                (embedding1[i] + embedding2[i]) / 2
            }
            
            // 归一化
            val norm = sqrt(result.sumOf { it * it.toDouble() })
            if (norm > 0) {
                for (i in result.indices) {
                    result[i] = (result[i] / norm).toFloat()
                }
            }
            
            return result
        } else {
            // 维度不同，使用截断或填充
            val maxDim = maxOf(dim1, dim2)
            val result = FloatArray(maxDim)
            
            // 复制第一个嵌入向量
            for (i in 0 until minOf(dim1, maxDim)) {
                result[i] = embedding1[i]
            }
            
            // 组合第二个嵌入向量
            for (i in 0 until minOf(dim2, maxDim)) {
                result[i] = (result[i] + embedding2[i]) / 2
            }
            
            // 归一化
            val norm = sqrt(result.sumOf { it * it.toDouble() })
            if (norm > 0) {
                for (i in result.indices) {
                    result[i] = (result[i] / norm).toFloat()
                }
            }
            
            return result
        }
    }
    
    /**
     * 组合多个嵌入向量。
     *
     * @param embeddings 嵌入向量列表
     * @return 组合后的嵌入向量
     */
    fun combineMultipleEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) {
            return FloatArray(0)
        }
        
        if (embeddings.size == 1) {
            return embeddings[0]
        }
        
        var result = embeddings[0]
        for (i in 1 until embeddings.size) {
            result = combineEmbeddings(result, embeddings[i])
        }
        
        return result
    }
    
    /**
     * 判断 URL 是否为图像 URL。
     *
     * @param url URL
     * @return 是否为图像 URL
     */
    fun isImageUrl(url: String): Boolean {
        val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
        return imageExtensions.any { url.lowercase().endsWith(it) }
    }
    
    /**
     * 判断 URL 是否为音频 URL。
     *
     * @param url URL
     * @return 是否为音频 URL
     */
    fun isAudioUrl(url: String): Boolean {
        val audioExtensions = listOf(".mp3", ".wav", ".ogg", ".flac", ".aac", ".m4a")
        return audioExtensions.any { url.lowercase().endsWith(it) }
    }
    
    /**
     * 判断 URL 是否为视频 URL。
     *
     * @param url URL
     * @return 是否为视频 URL
     */
    fun isVideoUrl(url: String): Boolean {
        val videoExtensions = listOf(".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".webm")
        return videoExtensions.any { url.lowercase().endsWith(it) }
    }
}
