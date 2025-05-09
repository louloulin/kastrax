package ai.kastrax.rag.query

/**
 * 同义词查询转换器，将查询文本中的词替换为同义词。
 *
 * @property synonymMap 同义词映射
 * @property expandQuery 是否扩展查询
 */
class SynonymQueryTransformer(
    private val synonymMap: Map<String, List<String>>,
    private val expandQuery: Boolean = true
) : QueryTransformer {
    /**
     * 转换查询。
     *
     * @param query 查询文本
     * @return 转换后的查询文本
     */
    override fun transform(query: String): String {
        val words = query.split(Regex("\\s+"))
        val result = StringBuilder()
        
        for (word in words) {
            val lowerWord = word.lowercase()
            
            if (synonymMap.containsKey(lowerWord)) {
                val synonyms = synonymMap[lowerWord]!!
                
                if (expandQuery) {
                    // 扩展查询：添加原词和同义词
                    result.append(word)
                    result.append(" ")
                    
                    for (synonym in synonyms) {
                        result.append(synonym)
                        result.append(" ")
                    }
                } else {
                    // 替换查询：使用第一个同义词替换原词
                    if (synonyms.isNotEmpty()) {
                        result.append(synonyms[0])
                        result.append(" ")
                    } else {
                        result.append(word)
                        result.append(" ")
                    }
                }
            } else {
                result.append(word)
                result.append(" ")
            }
        }
        
        return result.toString().trim()
    }
}
