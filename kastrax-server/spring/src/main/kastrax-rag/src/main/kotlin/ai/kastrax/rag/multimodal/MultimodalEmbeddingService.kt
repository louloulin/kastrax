package ai.kastrax.rag.multimodal

import ai.kastrax.store.embedding.EmbeddingService

/**
 * 多模态嵌入服务接口，支持文本、图像、音频和视频等多种模态的嵌入。
 */
interface MultimodalEmbeddingService : EmbeddingService {
    /**
     * 嵌入多模态文档。
     *
     * @param document 多模态文档
     * @return 嵌入向量
     */
    suspend fun embedMultimodalDocument(document: MultimodalDocument): FloatArray

    /**
     * 批量嵌入多模态文档。
     *
     * @param documents 多模态文档列表
     * @return 嵌入向量列表
     */
    suspend fun embedMultimodalDocuments(documents: List<MultimodalDocument>): List<FloatArray>

    /**
     * 嵌入图像。
     *
     * @param imageUrl 图像 URL
     * @return 嵌入向量
     */
    suspend fun embedImage(imageUrl: String): FloatArray

    /**
     * 嵌入音频。
     *
     * @param audioUrl 音频 URL
     * @return 嵌入向量
     */
    suspend fun embedAudio(audioUrl: String): FloatArray

    /**
     * 嵌入视频。
     *
     * @param videoUrl 视频 URL
     * @return 嵌入向量
     */
    suspend fun embedVideo(videoUrl: String): FloatArray

    /**
     * 嵌入文本和图像。
     *
     * @param text 文本
     * @param imageUrl 图像 URL
     * @return 嵌入向量
     */
    suspend fun embedTextAndImage(text: String, imageUrl: String): FloatArray

    /**
     * 嵌入文本和音频。
     *
     * @param text 文本
     * @param audioUrl 音频 URL
     * @return 嵌入向量
     */
    suspend fun embedTextAndAudio(text: String, audioUrl: String): FloatArray

    /**
     * 嵌入文本和视频。
     *
     * @param text 文本
     * @param videoUrl 视频 URL
     * @return 嵌入向量
     */
    suspend fun embedTextAndVideo(text: String, videoUrl: String): FloatArray
}
