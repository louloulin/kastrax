package ai.kastrax.rag.query

/**
 * 分解查询转换器，将复杂查询分解为多个简单查询。
 *
 * @property maxQueries 最大查询数量
 */
class DecompositionQueryTransformer(
    private val maxQueries: Int = 3
) : QueryTransformer {
    /**
     * 转换查询。
     *
     * @param query 查询文本
     * @return 转换后的查询文本
     */
    override fun transform(query: String): String {
        // 如果查询很短，直接返回
        if (query.length < 50) {
            return query
        }
        
        // 尝试按句子分割
        val sentences = query.split(Regex("[.!?]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (sentences.size <= 1) {
            return query
        }
        
        // 选择最重要的句子
        val importantSentences = selectImportantSentences(sentences, maxQueries)
        
        // 组合成新的查询
        return importantSentences.joinToString(" ")
    }
    
    /**
     * 选择重要的句子。
     *
     * @param sentences 句子列表
     * @param maxCount 最大数量
     * @return 重要的句子列表
     */
    private fun selectImportantSentences(sentences: List<String>, maxCount: Int): List<String> {
        // 如果句子数量小于等于最大数量，直接返回所有句子
        if (sentences.size <= maxCount) {
            return sentences
        }
        
        // 计算每个句子的重要性分数
        val scores = sentences.map { calculateImportanceScore(it) }
        
        // 选择分数最高的句子
        return sentences.zip(scores)
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
    }
    
    /**
     * 计算句子的重要性分数。
     *
     * @param sentence 句子
     * @return 重要性分数
     */
    private fun calculateImportanceScore(sentence: String): Double {
        // 这里使用一个简单的启发式方法：句子长度和关键词出现次数
        val length = sentence.length
        val keywordCount = countKeywords(sentence)
        
        // 长度得分：中等长度的句子得分较高
        val lengthScore = if (length < 10) {
            length / 10.0
        } else if (length < 100) {
            1.0
        } else {
            100.0 / length
        }
        
        // 关键词得分
        val keywordScore = keywordCount * 0.2
        
        return lengthScore + keywordScore
    }
    
    /**
     * 计算句子中关键词的出现次数。
     *
     * @param sentence 句子
     * @return 关键词出现次数
     */
    private fun countKeywords(sentence: String): Int {
        val keywords = listOf(
            "what", "how", "why", "when", "where", "who",
            "explain", "describe", "define", "compare", "analyze",
            "important", "significant", "key", "main", "primary",
            "need", "want", "require", "must", "should"
        )
        
        val lowerSentence = sentence.lowercase()
        return keywords.count { lowerSentence.contains(it) }
    }
}
