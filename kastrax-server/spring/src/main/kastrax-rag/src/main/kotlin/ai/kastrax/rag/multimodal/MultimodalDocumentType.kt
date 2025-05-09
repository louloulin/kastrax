package ai.kastrax.rag.multimodal

/**
 * 多模态文档类型。
 */
enum class MultimodalDocumentType {
    /**
     * 纯文本文档。
     */
    TEXT,

    /**
     * 图像文档。
     */
    IMAGE,

    /**
     * 音频文档。
     */
    AUDIO,

    /**
     * 视频文档。
     */
    VIDEO,

    /**
     * 文本和图像混合文档。
     */
    TEXT_IMAGE,

    /**
     * 文本和音频混合文档。
     */
    TEXT_AUDIO,

    /**
     * 文本和视频混合文档。
     */
    TEXT_VIDEO,

    /**
     * 多模态混合文档。
     */
    MULTIMODAL
}
