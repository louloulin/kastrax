package ai.kastrax.rag.retriever

/**
 * 关键词提取器接口，定义了提取关键词的方法。
 */
interface KeywordExtractor {
    /**
     * 提取关键词。
     *
     * @param text 文本
     * @return 关键词列表
     */
    fun extract(text: String): List<String>
}
