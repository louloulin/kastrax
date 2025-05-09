package ai.kastrax.rag.retriever

/**
 * 简单关键词提取器，基于分词和停用词过滤提取关键词。
 *
 * @property stopWords 停用词集合
 * @property minWordLength 最小词长度
 */
class SimpleKeywordExtractor(
    private val stopWords: Set<String> = DEFAULT_STOP_WORDS,
    private val minWordLength: Int = 3
) : KeywordExtractor {
    /**
     * 提取关键词。
     *
     * @param text 文本
     * @return 关键词列表
     */
    override fun extract(text: String): List<String> {
        // 分词
        val words = text.split(Regex("\\s+|[,.;:!?()\\[\\]{}'\"]"))
        
        // 过滤停用词和短词，转换为小写
        return words
            .map { it.lowercase() }
            .filter { it.length >= minWordLength && it !in stopWords }
    }
    
    companion object {
        /**
         * 默认停用词集合。
         */
        val DEFAULT_STOP_WORDS = setOf(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "by", "for", "with", "about", "against", "between", "into",
            "through", "during", "before", "after", "above", "below", "to",
            "from", "up", "down", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when",
            "where", "why", "how", "all", "any", "both", "each", "few", "more",
            "most", "other", "some", "such", "no", "nor", "not", "only", "own",
            "same", "so", "than", "too", "very", "s", "t", "can", "will",
            "just", "don", "don't", "should", "should've", "now", "d", "ll",
            "m", "o", "re", "ve", "y", "ain", "aren", "aren't", "couldn",
            "couldn't", "didn", "didn't", "doesn", "doesn't", "hadn", "hadn't",
            "hasn", "hasn't", "haven", "haven't", "isn", "isn't", "ma",
            "mightn", "mightn't", "mustn", "mustn't", "needn", "needn't",
            "shan", "shan't", "shouldn", "shouldn't", "wasn", "wasn't",
            "weren", "weren't", "won", "won't", "wouldn", "wouldn't"
        )
    }
}
