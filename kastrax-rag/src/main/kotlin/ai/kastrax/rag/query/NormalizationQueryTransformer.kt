package ai.kastrax.rag.query

import java.text.Normalizer
import java.util.Locale

/**
 * 归一化查询转换器，对查询文本进行归一化处理。
 *
 * @property lowercase 是否转换为小写
 * @property removeAccents 是否移除重音符号
 * @property removeExtraWhitespace 是否移除多余的空白字符
 * @property removeSpecialChars 是否移除特殊字符
 */
class NormalizationQueryTransformer(
    private val lowercase: Boolean = true,
    private val removeAccents: Boolean = true,
    private val removeExtraWhitespace: Boolean = true,
    private val removeSpecialChars: Boolean = false
) : QueryTransformer {
    /**
     * 转换查询。
     *
     * @param query 查询文本
     * @return 转换后的查询文本
     */
    override fun transform(query: String): String {
        var result = query
        
        // 转换为小写
        if (lowercase) {
            result = result.lowercase(Locale.getDefault())
        }
        
        // 移除重音符号
        if (removeAccents) {
            result = Normalizer.normalize(result, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        }
        
        // 移除特殊字符
        if (removeSpecialChars) {
            result = result.replace(Regex("[^\\w\\s]"), "")
        }
        
        // 移除多余的空白字符
        if (removeExtraWhitespace) {
            result = result.replace(Regex("\\s+"), " ").trim()
        }
        
        return result
    }
}
