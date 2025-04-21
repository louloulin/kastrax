package ai.kastrax.rag.context

/**
 * 令牌计数器接口，用于计算文本的令牌数。
 */
interface TokenCounter {
    /**
     * 计算文本的令牌数。
     *
     * @param text 文本
     * @return 令牌数
     */
    fun countTokens(text: String): Int
}

/**
 * 简单的令牌计数器，使用空格分割文本并计算单词数。
 */
class SimpleTokenCounter : TokenCounter {
    override fun countTokens(text: String): Int {
        if (text.isEmpty()) {
            return 0
        }
        
        // 简单的启发式方法：每个单词约 1.3 个令牌
        val wordCount = text.split(Regex("\\s+")).size
        return (wordCount * 1.3).toInt()
    }
}
