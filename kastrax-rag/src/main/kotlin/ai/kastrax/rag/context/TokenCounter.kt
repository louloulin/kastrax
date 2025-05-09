package ai.kastrax.rag.context

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingRegistry
import com.knuddels.jtokkit.api.EncodingType

/**
 * 令牌计数器，用于计算文本的令牌数量。
 *
 * @property encodingType 编码类型
 */
class TokenCounter(
    private val encodingType: EncodingType = EncodingType.CL100K_BASE
) {
    private val registry: EncodingRegistry = Encodings.newDefaultEncodingRegistry()
    private val encoding: Encoding = registry.getEncoding(encodingType)

    /**
     * 计算文本的令牌数量。
     *
     * @param text 文本
     * @return 令牌数量
     */
    fun countTokens(text: String): Int {
        return encoding.countTokens(text)
    }

    /**
     * 截断文本，使其令牌数量不超过指定值。
     *
     * @param text 文本
     * @param maxTokens 最大令牌数量
     * @return 截断后的文本
     */
    fun truncateText(text: String, maxTokens: Int): String {
        val tokens = encoding.encode(text)

        if (tokens.size <= maxTokens) {
            return text
        }

        val truncatedTokens = IntArray(maxTokens) { i -> tokens[i] }
        return encoding.decode(truncatedTokens.toList())
    }

    /**
     * 截断文本列表，使其总令牌数量不超过指定值。
     *
     * @param texts 文本列表
     * @param maxTokens 最大令牌数量
     * @return 截断后的文本列表
     */
    fun truncateTexts(texts: List<String>, maxTokens: Int): List<String> {
        val result = mutableListOf<String>()
        var remainingTokens = maxTokens

        for (text in texts) {
            val tokenCount = countTokens(text)

            if (tokenCount <= remainingTokens) {
                result.add(text)
                remainingTokens -= tokenCount
            } else if (remainingTokens > 0) {
                result.add(truncateText(text, remainingTokens))
                remainingTokens = 0
            } else {
                break
            }
        }

        return result
    }
}
