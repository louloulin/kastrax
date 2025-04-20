package ai.kastrax.fastembed

/**
 * Enumeration of supported embedding models.
 */
enum class EmbeddingModel(internal val id: Int) {
    /**
     * BGE Small English v1.5 model.
     * Dimensions: 384
     */
    BGE_SMALL_EN(0),

    /**
     * BGE Base English v1.5 model.
     * Dimensions: 768
     */
    BGE_BASE_EN(1),

    /**
     * BGE Small Chinese v1.5 model.
     * Dimensions: 384
     */
    BGE_SMALL_ZH(2),

    /**
     * BGE Base Chinese v1.5 model.
     * Dimensions: 768
     */
    BGE_BASE_ZH(3),

    /**
     * All-MiniLM-L6-v2 model.
     * Dimensions: 384
     */
    ALL_MINILM_L6_V2(4),

    /**
     * All-MiniLM-L6-v2 Quantized model.
     * Dimensions: 384
     */
    ALL_MINILM_L6_V2_Q(5),

    /**
     * Multilingual E5 Small model.
     * Dimensions: 384
     */
    E5_SMALL(6),

    /**
     * Multilingual E5 Large model.
     * Dimensions: 1024
     */
    E5_LARGE(7);

    companion object {
        /**
         * Get the default model.
         */
        val DEFAULT = BGE_SMALL_EN
    }
}
