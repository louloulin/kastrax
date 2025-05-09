package ai.kastrax.rag.query

/**
 * 组合查询转换器，将多个查询转换器组合在一起。
 *
 * @property transformers 查询转换器列表
 */
class CompositeQueryTransformer(
    private val transformers: List<QueryTransformer>
) : QueryTransformer {
    /**
     * 构造函数。
     *
     * @param vararg transformers 查询转换器
     */
    constructor(vararg transformers: QueryTransformer) : this(transformers.toList())
    
    /**
     * 转换查询。
     *
     * @param query 查询文本
     * @return 转换后的查询文本
     */
    override fun transform(query: String): String {
        var result = query
        for (transformer in transformers) {
            result = transformer.transform(result)
        }
        return result
    }
}
